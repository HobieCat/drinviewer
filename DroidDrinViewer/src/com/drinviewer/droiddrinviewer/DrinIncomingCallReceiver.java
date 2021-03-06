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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.drinviewer.common.Constants;
import com.drinviewer.common.HostData;
import com.drinviewer.common.IncomingDrinEvent;

/**
 * BroadcastReceiver to receive the PHONE_STATE action.
 * 
 * Shall connect to the running DiscoverServerService if any,
 * and handle the incoming call event through a PhoneStateListener subclass
 * 
 * Keep it separate for readability, could have easily be in the DrinViewerBroadcastReceiver
 * 
 * @author giorgio
 *
 */
public class DrinIncomingCallReceiver extends BroadcastReceiver {
	
	/**
	 * The TelephonyManager to listen 
	 */
	private TelephonyManager tManager = null;
	
	/**
	 * The PhoneStateListener subclass instance
	 */
	private DrinPhoneStateListener listener;
	
	@Override   
    public void onReceive(Context context, Intent intent) {
		// Gets the Binder to the DiscoverServerService
		IBinder b = peekService(context, new Intent(context, DiscoverServerService.class));
		DiscoverServerApi discoverServerApi = DiscoverServerApi.Stub.asInterface(b);
		
		if (b != null) {
			// Get the hostCollection
			DrinHostCollection hostCollection = null;
			try {
				hostCollection = discoverServerApi.getMostUpToDateCollection();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			// Do send the IncomingDrinEvent only if there's someone willing to receiving it
			if (hostCollection != null && hostCollection.size()>0) {
				// Get The TelephonyManager
				tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
				// Instantiates the listener
				listener = new DrinPhoneStateListener(context, hostCollection);
				// Register listener for LISTEN_CALL_STATE
				tManager.listen(listener,PhoneStateListener.LISTEN_CALL_STATE);
			}
		}
    }
	
	/**
	 * Stops the listener from listening
	 * 
	 * @param tManager
	 */
	private void deregisterTelephonyManager(TelephonyManager tManager) {
		if (tManager!=null) tManager.listen(listener,PhoneStateListener.LISTEN_NONE);
	}

	/**
	 * Private subclass of PhoneStateListener used to listen for incoming
	 * call, to build up the IncomingDrinEvent and fire it to all the paired
	 * servers found in the hostCollection
	 * 
	 * @author giorgio
	 *
	 */
    private class DrinPhoneStateListener extends PhoneStateListener {
    	/**
    	 * The context used to query the addressbook
    	 */
    	private Context context;
    	
    	/**
    	 * The hostCollection as given by the DiscoverServerService
    	 */
    	private DrinHostCollection hostCollection;
    	
    	/**
    	 * true if the phone was ringing
    	 */
    	private boolean wasRinging = false;

        public DrinPhoneStateListener(Context context, DrinHostCollection hostCollection) {
			super();
			this.context = context;
			this.hostCollection = hostCollection;
			// get wasRinging state from SharedPreferences
	        wasRinging = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.APPNAME+"wasRinging", false);
		}

        /**
         * The onCallStateChanged listener, where stuff happens
         * 
         */
        @Override
		public void onCallStateChanged(int state, String incomingNumber) {
        	
        	/**
        	 * contactId used to query for contact image
        	 */
			long contactId = 0L;
			
			/**
			 * Caller's name
			 */
			String name = null;
			
			/**
			 * Caller's photo InputStream to build the ByteArray
			 */
			InputStream photoIS = null;
			
			/**
			 * The image ByteArray sent in the IncomingDrinEvent
			 */
			byte[] imgData = null;
			
            if ( state == TelephonyManager.CALL_STATE_RINGING || 
            	 (wasRinging && 
                 (state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_IDLE))
               ) {
            	/**
            	 * Lookup caller's Name and Picture from the AddressBook
            	 */
            	// 0. Get a content resolver
            	ContentResolver cr = context.getContentResolver();
            	
            	// 1. Define the needed columns
            	String[] projection = new String[] {
            	        ContactsContract.PhoneLookup.DISPLAY_NAME,
            	        ContactsContract.Contacts._ID};
            	
            	// 2. Encode phone number and build filter URI
            	// set incomingNumber to null if it's a call being answered,
            	// it looks like there's no way of getting the incomingNumber in this case,
            	// and send the message to the server anyway. He'll know what to do.
            	if (state==TelephonyManager.CALL_STATE_OFFHOOK) incomingNumber = null;
            	
            	Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(incomingNumber));
            	// 3. Perform the query
            	Cursor cursor = cr.query(contactUri, projection, null, null, null);
            	
            	// If a phone number is found
            	if (cursor.moveToFirst()) {
            		
            	    // Get values from contacts database:
            	    contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            	    name =      cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));

            	    // Get photo of contactId as input stream:
            	    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
            	    photoIS = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
            	    // If a photo is found, get it as a PNG and build its ByteArray
            	    if (photoIS!=null) {
            	    	Bitmap photoBMP = BitmapFactory.decodeStream(photoIS);
            	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
            	    	photoBMP.compress(Bitmap.CompressFormat.PNG, 90, baos);
            	    	imgData = baos.toByteArray();
            	    }
            	}
            	cursor.close();
            	
            	// If name it's null set it to the unknown string
            	if (name==null) name = context.getResources().getString(R.string.unknownnametitle);
            	
            	/**
            	 * Build the IncomingDrinEvent and send it to all paired servers
            	 */
            	int action = Constants.NO_ACTION;
            	
            	if (state == TelephonyManager.CALL_STATE_RINGING) {
            		wasRinging = true;
            		action = Constants.SHOW_POPUP;
            	} else if (state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_IDLE) {
            		wasRinging = false;
            		action = Constants.REMOVE_POPUP;
            	}
            	
            	final IncomingDrinEvent event = new IncomingDrinEvent(this, name, incomingNumber, imgData, action);
            	
            	for (int i=0; i<hostCollection.size();i++) {
            		final HostData currentHost = hostCollection.get(i);
            		if (currentHost.isPaired) {
            			/**
            			 * No need to check for a WiFi connection, if there's a paired
            			 * host we can safely assume that the connection is there, because
            			 * when a WiFi is disconnected the DrinViewerBroadCastReceiver cleans the hostCollection
            			 */
        				new Thread(new Runnable() { public void run() {
            					try {
            						new ClientConnectionManager(currentHost).sendDrinEvent(event);
								} catch (Exception e) {
									e.printStackTrace();
								}
        					}
        				}).start();
            		}
            	}
            } else if (wasRinging) {
            	wasRinging = false;
            }
            // Save wasRinging state to SharedPreferences
            Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putBoolean(Constants.APPNAME+"wasRinging", wasRinging);
            editor.commit();

            // Once the event has been sent, deregister the TelephonyManager
            deregisterTelephonyManager (tManager);
        }
    }
}
