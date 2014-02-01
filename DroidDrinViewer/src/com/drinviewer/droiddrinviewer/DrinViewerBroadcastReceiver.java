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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * BroadcastReceiver class for the following actions:
 * - WifiManager.NETWORK_STATE_CHANGED_ACTION
 * - ConnectivityManager.CONNECTIVITY_ACTION
 * - broadcast_startdiscovery      (DrinViewer custom)
 * - broadcast_cleanhostcollection (DrinViewer custom)
 * - broadcast_startalarmrepeater  (DrinViewer custom)
 * - broadcast_stopalarmrepeater   (DrinViewer custom)
 * 
 * @author giorgio
 *
 */
public class DrinViewerBroadcastReceiver extends BroadcastReceiver {

	/**
	 * used to repeat the discovery at fixed time intervals
	 */
	private AlarmManager alarmManager;
	
	/**
	 * broadcast IP as a string, as returned from the DHCP
	 */
	private String wifiBroadcastAddress = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
	        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	        if (networkInfo.isConnected()) {
	        	/**
	        	 * WiFi is connected, get its broadcast 
	        	 * address and start the discovery process
	        	 * repeated at a fixed time interval
	        	 */
	        	wifiBroadcastAddress = getWiFiBroadcastAddress(context);
	            startAlarmRepeater(context);
	        } else {
	        	wifiBroadcastAddress = null;
	        }
	    } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
	        NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
	        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && !networkInfo.isConnected()) {
	            /**
	             * WiFi is disconnected, stop the discovery
	             * process repeating, it would be a waste of resources
	             */
	        	wifiBroadcastAddress = null;
	            stopAlarmRepeater(context);
	        }
	    }
	    else if (intent.getAction().equals(context.getResources().getString(R.string.broadcast_startdiscovery)) || 
	    		 intent.getAction().equals(context.getResources().getString(R.string.broadcast_cleanhostcollection))) {
	    	/**
	    	 * Calls the DiscoverServerService asking to do a discovery
	    	 * or a clean host collection by simply forwarding the received action
	    	 */
    		Intent service = new Intent(context, DiscoverServerService.class);
    		service.setAction(intent.getAction());
    		
    		if (intent.getAction().equals(context.getResources().getString(R.string.broadcast_startdiscovery))) {
    			
    			ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	    	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    		wifiBroadcastAddress = (mWifi.isConnected()) ? getWiFiBroadcastAddress(context) : null;	    					    			
    			service.putExtra("wifiBroadcastAddress", wifiBroadcastAddress);
    		}
    		
    		context.startService(service);
	    } else if (intent.getAction().equals(context.getResources().getString(R.string.broadcast_startalarmrepeater))) {
	    	/**
	    	 * start the alarm repeater only if WiFi is connected already
	    	 * used by ServerListFragment.onServiceConnected method to start the discovery
	    	 * if the application is launched being already connected to a WiFi network
	    	 */
	    	ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    	
	    	if (mWifi.isConnected()) {
	    		// if we're called from the activity, try to get a broadcast address
	    		if (intent.getBooleanExtra("forcegetbroadcast", false)) wifiBroadcastAddress = getWiFiBroadcastAddress(context);	    		
	    		startAlarmRepeater(context);
	    	} else {
	    		wifiBroadcastAddress = null;
	    	}
	    } else if (intent.getAction().equals(context.getResources().getString(R.string.broadcast_stopalarmrepeater))) {
	    	/**
	    	 *  stop the alarm repeater. period.
	    	 *  used by DrinViewerApplication.onTerminate method
	    	 */
	    	stopAlarmRepeater(context);
	    }
	}
	
	/**
	 * Starts the alarm repeater after a disconnection from a WiFi network.
	 * The alarm is firing request to discover server at regular time interval
	 * 
	 * @param context The context to use
	 * @return true on success
	 */
	private boolean startAlarmRepeater(Context context) {
		
		boolean returnValue = false;
		
		// Gets the Binder to the DiscoverServerService
		IBinder b = peekService(context, new Intent(context, DiscoverServerService.class));
		DiscoverServerApi discoverServerApi = DiscoverServerApi.Stub.asInterface(b);			
		
		// start the alarm repeater only if the api exists and the discover process in not running
		try {
			if (b != null && discoverServerApi != null && !discoverServerApi.isRunning()) { 			
				// Get the alarm manager
				if (alarmManager == null) alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				// Instantiate the intent and set its action
				Intent i = new Intent(context, this.getClass());
				i.setAction(context.getResources().getString(R.string.broadcast_startdiscovery));
				// send the wifiBroadcastAddress together with the intent
				i.putExtra("wifiBroadcastAddress", wifiBroadcastAddress);
				// Get the broadcast
				PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
				if (pending!=null) {
					Calendar cal = Calendar.getInstance();
					// cancel the alarm
					alarmManager.cancel(pending);
					// Run the intent at fixed time intervals
					alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
							cal.getTimeInMillis(), DroidDrinViewerConstants.DISCOVER_REPEAT_TIME, pending);
					returnValue = true;
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			returnValue = false;
		} 
		return returnValue;
	}
	
	/**
	 * Stops the alarm repeater after a disconnection from a WiFi network
	 * 
	 * @param context  The context to use
	 */
	private void stopAlarmRepeater(Context context) {
		/**
		 * Sends a message to clean the host collection,
		 * should be safe because this method gets called on WiFi disconnect
		 */
		Intent i = new Intent(context, this.getClass());
		i.setAction(context.getResources().getString(R.string.broadcast_cleanhostcollection));
		context.sendBroadcast(i);
		
		/**
		 * Cancel the pending intent from the AlarmManager, reusing the same Intent i
		 * NOTE: This does not fires a start discover, just setting the same action
		 * used in startAlarmRepeater
		 */
		i.setAction(context.getResources().getString(R.string.broadcast_startdiscovery));
		PendingIntent senderstop = PendingIntent.getBroadcast(context, 0, i, 0);
		// Get the alarm manager
		if (alarmManager == null) alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(senderstop);
	}
	
	private String getWiFiBroadcastAddress (Context context) {
		String bcastaddr = null;
    	WifiManager mWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = mWifi.getDhcpInfo();
        	            
		if (mWifi.isWifiEnabled() && dhcp != null) {
			int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
			byte[] quads = new byte[4];
			for (int k = 0; k < 4; k++)
				quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
			
			try {
				bcastaddr = InetAddress.getByAddress(quads).getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return bcastaddr;
	}
}
