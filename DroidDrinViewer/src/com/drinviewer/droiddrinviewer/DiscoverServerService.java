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
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
/**
 * Discover DrinViewer Hosts available for pairing or already paired
 * 
 * Basically runs the DiscoverServer Runnable in a new Thread
 * 
 * @author giorgio
 *
 */
public class DiscoverServerService extends Service {

	/**
	 * Object used to synchronize when clients request the hostCollection
	 */
	private final Object discoverLock = new Object();
	
	/**
	 * The stored, most up-to-dated DrinHostCollection
	 */
	private DrinHostCollection hostCollection = new DrinHostCollection();
	
	/**
	 * A list of remote callback to communicate with the service
	 */
	private RemoteCallbackList<DiscoverServerListener> listeners = new RemoteCallbackList<DiscoverServerListener>();
	
	/**
	 * true if a discovery process is running
	 */
	private boolean isRunning = false;
	
	private String wifiBroadcastAddress = null;
	
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
				hostCollection.setPaired(hostCollection.get(position), pairState);
		}
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction()!=null) {
			if (intent.getAction().equals(getResources().getString(R.string.broadcast_startdiscovery))) {
				// get the wifiBroadcastAddress from intent extra
				wifiBroadcastAddress = null;
				Bundle b = intent.getExtras();
				if (b != null) {
					wifiBroadcastAddress = b.getString("wifiBroadcastAddress");				
				}
				if (wifiBroadcastAddress != null) {
					/**
					 * the runDiscover can take some time to complete, it spawns
					 * its own thread for the DiscoverServer to run but it also
					 * puts a lock on the discoverLock Object and executes a couple
					 * of synchronized methods of the DrinHostCollection class.
					 * Remember that the service always run on the UI thread, so
					 * to avoid ANR let's run it in a separate thread as well.
					 */
					new Thread(new Runnable(){
						@Override
						public void run() {
							runDiscover(wifiBroadcastAddress);
						}
					}).start();
				}
			} else if (intent.getAction().equals(getResources().getString(R.string.broadcast_cleanhostcollection))) {
				hostCollection.init();
				/**
				 * Sends a host init event to all listeners
				 */
				sendBroadcastToListeners(DroidDrinViewerConstants.COLLECTION_INIT);
			}
		}
		DrinViewerBroadcastReceiver.completeWakefulIntent(intent);
		return Service.START_NOT_STICKY;
	}
	
	private void runDiscover (String wifiBroadcastAddress) {
		try {
			isRunning = true;
			/**
			 * Sends a discovery started event to all listeners
			 */
			sendBroadcastToListeners(DroidDrinViewerConstants.DISCOVERY_STARTED);
			
			hostCollection.init();
			
			DiscoverServer ds = new DiscoverServer(hostCollection);
			ds.setUUID(DrinViewerApplication.getInstallationUUID());
			
			if (wifiBroadcastAddress != null) ds.setBroadcastAddress (wifiBroadcastAddress);

			synchronized (discoverLock) {
				// start the discoverServer thread
				new Thread(ds).start();
				/**
				 * following called methods:
				 * - isProducerRunning
				 * - getLast
				 * are both synchronized, so that they should return as soon as an
				 * host is found and added to the hostCollection or the timeout is reached
				 */
				while (hostCollection.isProducerRunning()) {
					DrinHostData hs = hostCollection.getLast();					
					/**
					 * Sends a host discovered event to all listeners
					 */
					if (hs!=null) sendBroadcastToListeners(DroidDrinViewerConstants.HOST_DISCOVERED, hs);
				}
			}
			/**
			 * Sends a discovery done event to all listeners
			 */
			sendBroadcastToListeners(DroidDrinViewerConstants.DISCOVERY_DONE);
			
		} catch (Throwable t) {
			// ignore
			t.printStackTrace();
		} finally {
			isRunning = false;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		listeners.kill();
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (DiscoverServerService.class.getName().equals(intent.getAction())) {
			return discoverAPI;
		} else {
			return null;
		}
	}
	
	/**
	 * send the broadcast to all registered listeners
	 * 
	 * @param what int representing which broadcast to send
	 * @param params the DrinHostData associated to a HOST_DISCOVERED event
	 */
	private void sendBroadcastToListeners (int what, Object...params) {
		try {
			int N = listeners.beginBroadcast();
			for (int i=0; i<N; i++) {
				switch (what) {
				case DroidDrinViewerConstants.COLLECTION_INIT:
					listeners.getBroadcastItem(i).onHostCollectionInit();
					break;
				case DroidDrinViewerConstants.DISCOVERY_STARTED:
					listeners.getBroadcastItem(i).onHostDiscoveryStarted();
					break;
				case DroidDrinViewerConstants.HOST_DISCOVERED:
					if (params.length == 1)
					{
						listeners.getBroadcastItem(i).onHostDiscovered((DrinHostData) params[0]);
					}
					break;
				case DroidDrinViewerConstants.DISCOVERY_DONE:
					listeners.getBroadcastItem(i).onHostDiscoveryDone();
					break;				
				}
			}
		} catch (Throwable t) {
			// ignore
			t.printStackTrace();
		} finally {
			listeners.finishBroadcast();
		}
	}
}
