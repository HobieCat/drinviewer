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

import com.drinviewer.droiddrinviewer.DrinViewerActivity.DrinViewerHandler;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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
	 * The hostCollection used by the service
	 * 
	 * Shared between the UI list and the background running
	 * process. This is the most up-to-date host list available
	 * ready to use whenever it's needed
	 */
	private DrinHostCollection hostCollection;
	
	/**
	 * The DiscoverServer Runnable to be run
	 */
	private DiscoverServer discoverServer;
	
	/**
	 * Service binder
	 */
	private final IBinder mBinder = new DiscoverServerBinder();
	
	@Override
	public void onCreate() {
		super.onCreate();
		hostCollection = new DrinHostCollection();
	}

	/**
	 * When service gets started it looks if the passed intent action is a request to start
	 * a discovery or a reqeust to clean the stored hosCollection (used e.g. when the deivce
	 * is disconnected to a WiFi network to display an empty list) 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (intent.getAction().equals(getResources().getString(R.string.broadcast_startdiscovery))) {
			// Instantiates the DiscoverServerRunnable
			discoverServer = new DiscoverServer(hostCollection);
			// Forces a hostCollection update without updating the UI
			forceUpdate(false);
		} else if (intent.getAction().equals(getResources().getString(R.string.broadcast_cleanhostcollection))) {
			// Clears the hostCollection
			hostCollection.init();
		}
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	/**
	 * Forces a hostCollection update by actually running the DiscoverServer Runnable in a new Thread
	 * 
	 * @param updateUI true if an update to the UI is required
	 */
	private void forceUpdate (boolean updateUI) {
		// Sets the UUID to be used
		discoverServer.setUUID( DrinViewerApplication.getInstallationUUID());
		// Tells the DiscoverServer if it must send messages to the DrinViewerActivity handler
		discoverServer.setSendUpdateUIMessage(updateUI);
		// Inits the collectiom
		hostCollection.init();
		// Runs the Runnable
		new Thread(discoverServer).start();	
	}
	
	/**
	 * Forces a hostCollection from the UI
	 * 
	 * @param handler the DrinViewerHandler to use in order to handle messages to update the UI
	 * @param passedCollection the DrinHostCollection that's being used when drawing the UI
	 */
	public void forceUpdateWithUIMessage (DrinViewerHandler handler, DrinHostCollection passedCollection) {
		// Sets the hostCollection to the passed one, so the listview gets properly updated
		hostCollection = passedCollection;
		// Tells the DiscoverServer which hostCollection and handler to use
		discoverServer = new DiscoverServer(hostCollection, handler);
		// Runs the collection+UI update
		forceUpdate(true);
	}
	
	/*
	 * Get the most up-to-date hostCollection
	 */
	public DrinHostCollection getHostCollection() {
		return hostCollection;
	}
	
	/**
	 * DiscoverServerBinder to bind the DiscoverServerService
	 * 
	 * @author giorgio
	 *
	 */
	public class DiscoverServerBinder extends Binder {
		public DiscoverServerService getService() {
			return DiscoverServerService.this;
		}
	}
}
