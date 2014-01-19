/**
 * Copyright 2014 Giorgio Consorti <giorgio.consorti@gmail.com>
 *
 * This file is part of DrinViewer.
 *
 * DrinViewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DrinViewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrinViewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.drinviewer.droiddrinviewer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
/**
 * Discover DrinViewer Hosts available for pairing or already paired
 * 
 * Basically runs the DiscoverServer Runnable in a new Thread
 * 
 * @author giorgio
 *
 */
public class DiscoverServerService extends Service {

	//TODO Remove this
	private final static String TAG = DiscoverServerService.class.getSimpleName();
	
	/**
	 * Object used to synchronize when clients request the hostCollection
	 */
	private final Object discoverLock = new Object();
	
	/**
	 * The stored, most up-to-dated DrinHostCollection
	 */
	private DrinHostCollection hostCollection = new DrinHostCollection();
	
	/**
	 * A list of remote callbacks to communicate with the service
	 */
	private RemoteCallbackList<DiscoverServerListener> listeners = new RemoteCallbackList<DiscoverServerListener>();
	
	/**
	 * true if a discoery process is running
	 */
	private boolean isRunning = false;
	
	private DiscoverServerApi.Stub discoverAPI = new DiscoverServerApi.Stub() {

		@Override
		public DrinHostCollection getMostUpToDateCollection() throws RemoteException {
			synchronized (discoverLock) {
				return hostCollection;				
			}
		}
		
		@Override
		public void addListener(DiscoverServerListener listener) throws RemoteException {
			if (listener != null) listeners.register(listener);
		}
		
		@Override
		public void removeListener(DiscoverServerListener listener) throws RemoteException {
			if (listener != null) listeners.unregister(listener);
		}

		@Override
		public boolean isRunning() throws RemoteException {
			return isRunning;
		}

		@Override
		public void updatePairState(int position, boolean pairState) throws RemoteException {
				Log.d(TAG,"Setting paired# "+position+"to: "+pairState);
				hostCollection.setPaired(hostCollection.get(position), pairState);
		}
	};
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals(getResources().getString(R.string.broadcast_startdiscovery))) {
			runDiscover();		
		} else if (intent.getAction().equals(getResources().getString(R.string.broadcast_cleanhostcollection))) {
			hostCollection.init();
		}
		return Service.START_NOT_STICKY;
	}
	
	private void runDiscover () {
		try {
			isRunning = true;

			/**
			 * Sends a discovery started event to all listeners
			 */
			int N  = listeners.beginBroadcast();
			for (int i=0; i<N; i++) {
				listeners.getBroadcastItem(i).onHostDiscoveryStarted();
			}
			listeners.finishBroadcast();
			
			hostCollection.init();
			DiscoverServer ds = new DiscoverServer(hostCollection);
			ds.setUUID(DrinViewerApplication.getInstallationUUID());

			synchronized (discoverLock) {
				new Thread(ds).start();
				/*
				 * following called methods are both synchronized
				 * so that they should return as soon as an host
				 * is found and added to the hostCollection or
				 * the timeout is reached
				 */
				while (hostCollection.isProducerRunning()) {
					DrinHostData hs = hostCollection.getLast();
					
					/**
					 * Sends a host discovered event to all listeners
					 */
					N  = listeners.beginBroadcast();
					for (int i=0; i<N; i++) {
						if (hs != null) listeners.getBroadcastItem(i).onHostDiscovered(hs);
					}
					listeners.finishBroadcast();
				}
			}
			
			/**
			 * Sends a discovery done event to all listeners
			 */
			N  = listeners.beginBroadcast();
			for (int i=0; i<N; i++) {
				listeners.getBroadcastItem(i).onHostDiscoveryDone();
			}
			listeners.finishBroadcast();
			
			Log.d(TAG, "size=" + hostCollection.size());
		} catch (Throwable t) {
			Log.e(TAG, "Failed to retrieve the hostCollection:", t);
		} finally {
			isRunning = false;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		listeners.kill(); // TODO check this
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (DiscoverServerService.class.getName().equals(intent.getAction())) {
			return discoverAPI;
		} else {
			return null;
		}
	}
}
