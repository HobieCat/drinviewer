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


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

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
	 * The DiscoverServerAPI
	 */
	private DiscoverServerApi discoverServerApi;
	
	/**
	 * the activity this fragment is attached to
	 */
	private DrinViewerActivity mActivity;
	
	/**
	 * true when this Fragment is bound to DiscoverServerService
	 */
	private boolean isBound = false;

	/**
	 * True if the UI must be updated
	 */
	private boolean mustUpdateUI = false;
	
	private DiscoverServerListener.Stub hostUpdatedListener = new DiscoverServerListener.Stub() {
		@Override
		public void onHostDiscoveryStarted() throws RemoteException {			
			getAdapter().initHostCollection();
			 if (mustUpdateUI) {
				 mActivity.getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_DISCOVER_START);
			 }
		}

		@Override
		public void onHostDiscoveryDone() throws RemoteException {
			/**
			 *  getMostUpToDateCollection may cause can ANR (application not responding) issue
			 *  because it must wait for a lock to be released, so the following code is executed
			 *  in a separate thread. Remember we are running on the UI thread here.
			 */
			new Thread(new Runnable(){
				@Override
				public void run() {
					DrinHostCollection newHostCollection;
					try {
						newHostCollection = discoverServerApi.getMostUpToDateCollection();
						if (newHostCollection != null) {
							getAdapter().setHostCollection(newHostCollection);
						}
						if (mustUpdateUI) {
							mActivity.getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_DISCOVER_DONE);
						}
					} catch (RemoteException e) {
						// ignore
						e.printStackTrace();
					}
				}
			}).start();
		}

		@Override
		public void onHostDiscovered(DrinHostData hostData) throws RemoteException {
			if (mustUpdateUI) {
				getAdapter().getHostCollection().add(hostData);
				if (mActivity != null) mActivity.getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_SERVER_FOUND);
			}
		}

		@Override
		public void onHostCollectionInit() throws RemoteException {
			getAdapter().initHostCollection();
			if (mustUpdateUI) {
				if (mActivity != null) mActivity.getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_COLLECTION_INIT);
			}
		}
	}; 
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the view
		View view = inflater.inflate(R.layout.serverlist, container, false);
		// Get the list viewer
		ListView serverListView = (ListView) view.findViewById(R.id.serverListView);
		// Get the progress bar
		discoverServerProgress = (ProgressBar) view.findViewById(R.id.discoverServerProgress);
		
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
		
		// Display this installation generated UUID
		((TextView) view.findViewById(R.id.deviceUUID)).setText(
				getString(R.string.deviceUUIDlabel) + DrinViewerApplication.getInstallationUUID());
		
		// Sets fragment to be retained		
		setRetainInstance(true);
		return view;
	}
	
	/**
	 * onResume Fragment method
	 */
	@Override
	public void onResume() {
		super.onResume();
		mustUpdateUI = true;
		
		try {
			if (discoverServerApi != null) {
				/**
				 *  if the service is doing a discovery, just set the
				 *  discoverServerProgress visibility to visible. 
				 *  The service will call the listener when it has finished
				 */
				if (discoverServerApi.isRunning()) {
					if (mActivity != null) {
						mActivity.getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_DISCOVER_START);
					}
				} else {
					/**
					 * If the service is not doing a discovery,
					 * ask him for the most up-to-date host collection
					 * set it in the adapter and display it
					 */
						final DrinHostCollection newCollection = discoverServerApi.getMostUpToDateCollection();
						if (mActivity != null) {
							mActivity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (newCollection != null) {
										getAdapter().setHostCollection(newCollection);												
									} else {
										getAdapter().initHostCollection();
									}	
									getAdapter().notifyDataSetChanged();
								}
							});
							/**
							 * either the hostcollection has been set or voided,
							 * turn off the discoverServerProgress
							 */
							mActivity.getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_DISCOVER_DONE);
						}
				}
			} else {
		        /**
		         * Bind the DiscoverServerService if it is not, note that the onServiceConnected
		         * method will send a startdiscovery message to the DrinViewerBroadcastReceiver
		         * as soon as the service gets connected at startup
		         */
				if (!isBound) {
					Intent intent = new Intent(mActivity.getApplication(),DiscoverServerService.class);
					intent.setAction(DiscoverServerService.class.getName());
					isBound = mActivity.getApplication().bindService(intent, this, Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);
				}
			}
		} catch (RemoteException e) {
			// ignore
			e.printStackTrace();
		}
	}


	/**
	 * onAttach Fragment method
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (DrinViewerActivity) activity;
	}
	
	/**
	 * onDetach Fragment method
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		mActivity = null;
	}

	/**
	 * onPause Fragment method
	 */
	@Override
	public void onPause() {
		super.onPause();
		try {
			// tell the activity to hide the ProgressBar
			if (mustUpdateUI && mActivity != null && discoverServerApi != null && discoverServerApi.isRunning()) {
				mActivity.getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_DISCOVER_DONE);
			}
		} catch (RemoteException e) {
			// ignore
		} finally {
			mustUpdateUI = false;
		}		
	}

	/**
	 * onDestroy Fragment method
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			// remove the listener and unbind the DiscoverServerService
			if (discoverServerApi!=null)
				discoverServerApi.removeListener(hostUpdatedListener);
			if (mActivity!=null)
				mActivity.getApplication().unbindService(this);
			isBound = false;
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
	 * service is not doing an update by itself.
	 * Called by the Refresh List menu item
	 * 
	 */
	public void doDiscover() {
		
		boolean doDiscover = true;
		
		try {
			if (discoverServerApi != null) {
				// force a discover only if the service is not doing it already
				doDiscover = !discoverServerApi.isRunning();
			} else {
				doDiscover = true;
			}
		} catch (RemoteException e) {
			// on remote error, don't run the discover process
			doDiscover = false;
			Toast.makeText(mActivity.getBaseContext(), e.getMessage() , Toast.LENGTH_LONG).show();
		} finally {
			if (doDiscover) {
				// run the discover by sending a message broadcast_startdiscovery to the DrinViewerBroadcastReceiver
				Intent intent = new Intent(mActivity, DrinViewerBroadcastReceiver.class);
				intent.setAction(getResources().getString(R.string.broadcast_startdiscovery));
				mActivity.sendBroadcast(intent);		
			}
		}
	}
	
	/**
	 * Sets the progress bar visibility
	 * 
	 * @param v either View.GONE or View.Visible
	 */
	public void setProgressServerVisibility (int v) {
		if ((discoverServerProgress != null) &&  (v == View.VISIBLE || v == View.GONE)) {
			discoverServerProgress.setVisibility(v);
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
				new PairProtocolTask(mActivity.getMessageHandler(), position).execute(pm);
				pm = null;
			}
			toPair = null;
		}
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
		if (discoverServerApi == null) {
			discoverServerApi = DiscoverServerApi.Stub.asInterface(service);			

			try {
				if (discoverServerApi.isRunning()) {
					/**
					 *  if the service is doing a discovery, just set the
					 *  discoverServerProgress visibility to visible. 
					 *  The service will call the listener when it has finished
					 */
					if (mustUpdateUI && mActivity != null) {
						mActivity.getMessageHandler().sendEmptyMessage(DroidDrinViewerConstants.MSG_DISCOVER_START);
					}
				} else {
			        /**
			         *  sends a message to the DrinViewerBroadcastReceiver to start the discovery repeat
			         *  this fires an immediate discovery, so there's no need to call the doDiscover method
			         */
			        Intent i = new Intent(mActivity.getBaseContext(), DrinViewerBroadcastReceiver.class);
			        i.setAction(getResources().getString(R.string.broadcast_startalarmrepeater));
			        i.putExtra("forcegetbroadcast", true);
			        mActivity.sendBroadcast(i);
				}
				
		        discoverServerApi.addListener(hostUpdatedListener);
				
			} catch (RemoteException e) {
				// ignore
				e.printStackTrace();
			}
		}
	}
}
