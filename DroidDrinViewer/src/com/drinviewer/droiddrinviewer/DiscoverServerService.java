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
	
	private final Object discoverLock = new Object();
	
	private DrinHostCollection hostCollection = new DrinHostCollection(); // search results
	
	private RemoteCallbackList<DiscoverServerListener> listeners = new RemoteCallbackList<DiscoverServerListener>();
	
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
	};
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG,"action="+intent.getAction());
		if (intent.getAction().equals(getResources().getString(R.string.broadcast_startdiscovery))) {
			Log.i(TAG,"Starting discoverTask");
			// discoverTask.start();
			new Thread () {
				@Override
				public void run() {
					runDiscover();
				}
			}.start();			
		} else if (intent.getAction().equals(getResources().getString(R.string.broadcast_cleanhostcollection))) {
			hostCollection.init();
		}
		return Service.START_NOT_STICKY;
	}
	
	private void runDiscover () {
		try {
			/**
			 * Sends a (sort of) discovery started event to all listeners
			 */
			
			int N  = listeners.beginBroadcast();
			for (int i=0; i<N; i++)
			{
				listeners.getBroadcastItem(i).onHostDiscoveryStarted();
			}
			listeners.finishBroadcast();
			
			hostCollection.init();
			DiscoverServer ds = new DiscoverServer(hostCollection);
			ds.setUUID(DrinViewerApplication.getInstallationUUID());

			synchronized (discoverLock) {
				hostCollection = ds.doDiscover();
			}
			
			N  = listeners.beginBroadcast();
			for (int i=0; i<N; i++)
			{
				listeners.getBroadcastItem(i).onHostDiscoveryDone();
			}
			listeners.finishBroadcast();
			
			Log.d(TAG, "size=" + hostCollection.size());
		} catch (Throwable t) {
			Log.e(TAG, "Failed to retrieve the hostCollection:", t);
		}		
	}

//	@Override
//	public void onCreate() {
//		super.onCreate();
//		Log.i(TAG, "Service creating");
//	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Service destroying");
		listeners.kill(); // TODO check this
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (DiscoverServerService.class.getName().equals(intent.getAction())) {
			Log.d(TAG, "Bound by intent " + intent);
			return discoverAPI;
		} else {
			return null;
		}
	}

}
