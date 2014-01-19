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


import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.drinviewer.common.HostData;

/**
 * Main ServerList view class, displays the found host
 * list and manages user interactions
 * 
 * @author giorgio
 *
 */
public class ServerListFragment extends Fragment implements ServiceConnection {

	/**
	 * The DrinHostAdapter to display the list
	 * 
	 */
	private DrinHostAdapter adapter;
	
	/**
	 * ProgressBar to be shown when updating the UI
	 * while server discovery is running
	 * 
	 */
	private ProgressBar discoverServerProgress;
	
	/**
	 * ProgressBar visibility status
	 * can be View.GONE or View.VISIBLE
	 * Must be Integer Object to be properly retained
	 * 
	 */
	private Integer discoverServerVisibility;
	
	/**
	 * The DiscoverServerAPI
	 */
	private DiscoverServerApi discoverServerApi;
	
	private DiscoverServerListener.Stub hostUpdatedListener = new DiscoverServerListener.Stub() {
		@Override
		public void onHostDiscoveryStarted() throws RemoteException {			
			getAdapter().initHostCollection();
			
			 if (mustUpdateUI) {
				 ((DrinViewerActivity) getActivity()).getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_DISCOVER_START);
			 }
			 else {
				 discoverServerVisibility = View.VISIBLE;
			 }
		}

		@Override
		public void onHostDiscoveryDone() throws RemoteException {			
			DrinHostCollection newHostCollection = discoverServerApi.getMostUpToDateCollection();
			if (newHostCollection != null) {
				getAdapter().setHostCollection(newHostCollection);
			}
			
			if (mustUpdateUI) {
				((DrinViewerActivity) getActivity()).getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_DISCOVER_DONE);
			} else {
				discoverServerVisibility = View.GONE;
			}
		}

		@Override
		public void onHostDiscovered(DrinHostData hostData) throws RemoteException {
			if (mustUpdateUI) {
				getAdapter().getHostCollection().add(hostData);
				((DrinViewerActivity) getActivity()).getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_SERVER_FOUND);
			}
		}

		@Override
		public void onHostCollectionInit() throws RemoteException {
			getAdapter().initHostCollection();
			
			if (mustUpdateUI) {
				((DrinViewerActivity) getActivity()).getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_SERVER_FOUND);
			}
		}
	}; 
	
	/**
	 * True if the UI must be updated
	 */
	private boolean mustUpdateUI = false;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the view
		View view = inflater.inflate(R.layout.serverlist, container, false);
		// Get the list viewer
		ListView serverListView = (ListView) view.findViewById(R.id.serverListView);
		// Get the progress bar
		discoverServerProgress = (ProgressBar) view.findViewById(R.id.discoverServerProgress);
		
		// Set visibility is its value was retained
		if (discoverServerVisibility!=null) discoverServerProgress.setVisibility(discoverServerVisibility);
		
		// Instantiate the list adapter	if it was not retained	
		if (adapter == null) adapter = new DrinHostAdapter(view.getContext());
		
		// set the list adapter and the OnItemClickListener
		serverListView.setAdapter(adapter);
		serverListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				doPairingToggle(position);
			}
		});
		// Sets fragment to be retained		
		setRetainInstance(true);
		return view;
	}
	
	/**
	 * If the DiscoverServer Object was not retained,
	 * instantiate it and run a server discovery
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	    // bind to the DiscoverServerService
		Intent intent = new Intent(DiscoverServerService.class.getName());
		//TODO check Context.BIND_AUTO_CREATE
	    getActivity().getApplication().bindService(intent, this, 0);
	}
	
	/**
	 * onResume Fragment method
	 */
	@Override
	public void onResume() {
		super.onResume();
		mustUpdateUI = true;
		
		if (discoverServerApi != null) {
			try {
				boolean isUpdating = discoverServerApi.isRunning();
				
				discoverServerVisibility = (isUpdating) ? View.VISIBLE : View.GONE;
				discoverServerProgress.setVisibility(discoverServerVisibility);
				/*
				 * If the service is not updating and has a hostCollection
				 * ready, use that one that is the most up-to-date
				 */
				if (!isUpdating) {
					DrinHostCollection newCollection = discoverServerApi.getMostUpToDateCollection();
					if (newCollection != null) {
						getAdapter().setHostCollection(newCollection);
						getAdapter().notifyDataSetChanged();
					}
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * onPause Fragment method
	 */
	@Override
	public void onPause() {
		super.onPause();
		mustUpdateUI = false;
	}

	/**
	 * onDestroy Fragment method
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			// remove the listener and unbind the DiscoverServerService
			discoverServerApi.removeListener(hostUpdatedListener);
			getActivity().getApplication().unbindService(this);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * List adapter getter to be used in main Activity
	 * 
	 * @return the adapter
	 */
	public DrinHostAdapter getAdapter() {
		return adapter;
	}

	/**
	 * Updates isPaired status on an Host and redraws the
	 * list to set the appropriate icon
	 * 
	 * @param position the position of the host to be paired in the list
	 * @param isPaired true if the host is paired
	 */
	public void updateIsPaired(int position, boolean isPaired) {
		getAdapter().updateIsPaired(position, isPaired);
		getAdapter().notifyDataSetChanged();
		
		// notify the DiscoverServerService of the change
		try {
			if (discoverServerApi != null) {
				discoverServerApi.updatePairState(position, isPaired);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Runs the server discovery protocol, if the
	 * service is not doing an update by itself
	 * 
	 * @param updateUI true if the UI must be updated while discovering
	 */
	public void doDiscover() {
		boolean doDiscover = true;
		
		try {
			if (discoverServerApi != null) {
				// force a discover only if the service is not doing it already
				doDiscover = !discoverServerApi.isRunning();
			}
		} catch (RemoteException e) {
			// on remote error, don't run the discover process
			doDiscover = false;
		} finally {
			if (doDiscover) {
				// run the discover by sending a message broadcast_startdiscovery to the DrinViewerBroadcastReceiver
				Intent intent = new Intent(getActivity().getBaseContext(), DrinViewerBroadcastReceiver.class);
				intent.setAction(getResources().getString(R.string.broadcast_startdiscovery));
				getActivity().sendBroadcast(intent);		
			}
		}
	}
	
	/**
	 * Sets the progress bar visibility
	 * 
	 * @param v either View.GONE or View.Visible
	 */
	public void setProgressServerVisibility (int v) {
		if (v == View.VISIBLE || v == View.GONE) {
			discoverServerVisibility = Integer.valueOf(v);
			discoverServerProgress.setVisibility(discoverServerVisibility.intValue());
		}
	}

	/**
	 * Runs the pair or unpair protocol of the device to a server
	 * 
	 * @param position the position of the server to pair or unpair
	 */
	private void doPairingToggle(int position) {
		if (position < getAdapter().getHostCollection().size()) {
			HostData toPair = getAdapter().getHostCollection().get(position);
			if (toPair != null) {
				// prepares the data and runs the pair protocol async task
				ClientConnectionManager pm = new ClientConnectionManager(toPair);
				pm.setUUID(DrinViewerApplication.getInstallationUUID());
				new PairProtocolTask(((DrinViewerActivity) getActivity()).getMessageHandler(), position).execute(pm);
				pm = null;
			}
			toPair = null;
		} else
			System.err.println("Position "+position + " is invalid");
	}
	
	/**
	 * Disconnection to the DiscoverServerService
	 * Method that implements the ServiceConnection
	 */
	@Override
	public void onServiceDisconnected(ComponentName name) {
		discoverServerApi = null;
	}

	/**
	 * Connection to the DiscoverServerService
	 * Method that implements the ServiceConnection
	 */
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		// that's how we get the client side of the IPC connection
		discoverServerApi = DiscoverServerApi.Stub.asInterface(service);
		
		try {
			discoverServerApi.addListener(hostUpdatedListener);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
        // sends a message to the DrinViewerBroadcastReceiver to start the discovery repeat
		// this fires an immediate discovery, so there's no need to call the doDiscover method
        Intent i = new Intent(((DrinViewerActivity) getActivity()).getBaseContext(),DrinViewerBroadcastReceiver.class);
        i.setAction(getResources().getString(R.string.broadcast_startalarmrepeater));
        ((DrinViewerActivity) getActivity()).getBaseContext().sendBroadcast(i);
	}
}
