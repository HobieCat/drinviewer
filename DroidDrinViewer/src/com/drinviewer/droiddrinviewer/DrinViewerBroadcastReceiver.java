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

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;

public class DrinViewerBroadcastReceiver extends BroadcastReceiver {
	/**
	 * Notification counter, for debugging purposes
	 */
	// TODO: remove it in final version
	private static int count=1;
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		/*
		 * The text and two booleans used for notification, for debugging purposes
		 */
		// TODO: remove these 3 in final version 
		StringBuilder notificationText = new StringBuilder();
		boolean sendNotify = false;
		boolean isStarted = false;
		
		if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
	        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	        if (networkInfo.isConnected()) {
	            // Wifi is connected
	            isStarted = startAlarmRepeater(context);
	            
	            // sets text of the notification
	    		// TODO: remove these 2 in final version	            
	            notificationText.append( "Wifi YES, started="+isStarted );
	            sendNotify = true;
	        }
	    } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
	        NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
	        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && !networkInfo.isConnected()) {
	            // Wifi is disconnected
	            stopAlarmRepeater(context);
	            
	            // sets text of the notification
	    		// TODO: remove these 2 in final version    
	            notificationText.append( "Wifi NO, started="+isStarted );
	            sendNotify = true;
	        }
	    } else if (intent.getAction().equals(context.getResources().getString(R.string.broadcast_startdiscovery)) || 
	    		   intent.getAction().equals(context.getResources().getString(R.string.broadcast_cleanhostcollection))) {
	    	// Calls the DiscoverServerService asking to do a discovery or a clean host collection
    		Intent service = new Intent(context, DiscoverServerService.class);
    		service.setAction(intent.getAction());
    		context.startService(service);
    		
            // sets text of the notification
    		// TODO: remove these 2 in final version    		
    		notificationText.append("Started discovery or clean");
    		sendNotify = true;
	    }
		
		// Handles the notification for debugging purposes
		// TODO: remove this if block in final version
		if (sendNotify) {
			
	        Intent contentIntent = new Intent(context, DrinViewerActivity.class);
	        PendingIntent contentPendingIntent = PendingIntent.getActivity(context,0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	
	        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
	
	        builder.setContentIntent(contentPendingIntent)
	        .setSmallIcon(R.drawable.ic_launcher)
	        .setTicker("Broadcast DrinViewer n. " + String.valueOf(++count))
	        .setWhen(System.currentTimeMillis())
	        .setContentTitle(notificationText.toString());
	
	         Notification notification = builder.build();
	        
	        // Fires the notification
	        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(count, notification);
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
		
		// Get the alarm manager
		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		// Instantiate the intent and set its action
		Intent i = new Intent(context, this.getClass());
		i.setAction(context.getResources().getString(R.string.broadcast_startdiscovery));
		// Get the broadcast
		// TODO: consider using FLAG_UPDATE_CURRENT
		PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		if (pending!=null) {
			Calendar cal = Calendar.getInstance();
			// Run the intent at fixed time intervals
			service.setInexactRepeating(AlarmManager.RTC_WAKEUP,
					cal.getTimeInMillis(), DroidDrinViewerConstants.DISCOVER_REPEAT_TIME, pending);
			returnValue = true;
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
		
		// Cancel the pending intent from the AlarmManager, reusing the same Intent i		
		i.setAction(context.getResources().getString(R.string.broadcast_startdiscovery));
		PendingIntent senderstop = PendingIntent.getBroadcast(context, 0, i, 0);
		AlarmManager alarmManagerstop = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManagerstop.cancel(senderstop);
	}
}
