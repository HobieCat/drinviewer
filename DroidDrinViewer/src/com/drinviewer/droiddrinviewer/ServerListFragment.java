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

import com.drinviewer.common.HostData;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Main ServerList view class, displays the found host
 * list and manages user interactions
 * 
 * @author giorgio
 *
 */
public class ServerListFragment extends Fragment {

	/**
	 * The DrinHostAdapter to display the list
	 * 
	 * @var DrinHostAdapter
	 */
	private DrinHostAdapter adapter;
	
	/**
	 * The DrinHostCollection to display
	 * 
	 * @var DrinHostCollection
	 */
	private DrinHostCollection hostCollection;
	
	/**
	 * The Runnable to execute when discoverying hosts
	 * 
	 * @var DiscoverServer
	 */
	private DiscoverServer discoverServer;
	
	/**
	 * ProgressBar to be shown when updating the UI
	 * while server discovery is running
	 * 
	 * @var ProgressBar
	 */
	private ProgressBar discoverServerProgress;
	
	/**
	 * ProgressBar visibility status
	 * can be View.GONE or View.VISIBLE
	 * Must be Integer Object to be properly retained
	 * 
	 * @var Integer
	 */
	private Integer discoverServerVisibility;
	
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

		// If the collection is not null, it was retained by setRetainInstance call
		if (hostCollection==null) {
			// Instantiate the collection to be passed to the producer and list adapter			
			hostCollection = new DrinHostCollection();
			// Instantiate the list adapter
			adapter = new DrinHostAdapter(view.getContext(), hostCollection);
		}
		
		// set the list adapter and the OnItemClickListener
		serverListView.setAdapter(adapter);
		serverListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				doPairingToggle(position);
			}
		});
		// Sets fragment to be reained		
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
		if (discoverServer == null) {
			// Instantiate the discoverServer class, passing the list of servers to be populated
			discoverServer = new DiscoverServer(hostCollection, ((DrinViewerActivity) getActivity()).getMessageHandler());
			discoverServer.setUUID( DrinViewerApplication.getInstallationUUID() );
			doDiscover(true);
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
		hostCollection.setPaired(hostCollection.get(position), isPaired);
		getAdapter().notifyDataSetChanged();
	}

	/**
	 * Runs the server discovery protocol
	 * 
	 * @param updateUI true if the UI must be updated while discovering
	 */
	public void doDiscover(boolean updateUI) {
		hostCollection.init();
		discoverServer.setSendUpdateUIMessage(updateUI);
		new Thread(discoverServer).start();
	}
	
	/**
	 * Sets the progress bar visibility
	 * 
	 * @param v either View.GONE or View.Visible
	 */
	public void setProgressServerVisibility (int v) {
		discoverServerVisibility = Integer.valueOf(v);
		discoverServerProgress.setVisibility(discoverServerVisibility.intValue());
	}

	/**
	 * Runs the pair or unpair protocol of the device to a server
	 * 
	 * @param position the position of the server to pair or unpair
	 */
	private void doPairingToggle(int position) {
		if (position < hostCollection.size()) {
			HostData toPair = hostCollection.get(position);
			if (toPair != null) {
				ClientConnectionManager pm = new ClientConnectionManager(toPair);
				pm.setUUID(DrinViewerApplication.getInstallationUUID());
				new PairProtocolTask(((DrinViewerActivity) getActivity()).getMessageHandler(), position).execute(pm);
				pm = null;
			}
			toPair = null;
		} else
			System.err.println("Position "+position + " is invalid");
	}
}
