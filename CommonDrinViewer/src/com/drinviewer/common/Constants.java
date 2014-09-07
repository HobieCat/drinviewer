/**
 * Copyright 2013 Giorgio Consorti <giorgio.consorti@gmail.com>
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

package com.drinviewer.common;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Constants {
	// application name
	public static final String APPNAME = "DrinViewer";
	// application version number
	public static final String APPVERSION = "0.0.1";
	
	// default port for communication
	public static final int    PORT=50001;
	// how many time to run the test server application
	public static final int    SECONDS_TO_RUN_SERVER = 600;
	// default length of the buffer used for UDP communication while in DISCOVER phase
	public static final int    BUFLEN = 150;
	
	// broadcast address for sending UDP packets while in DISCOVER phase
	public static final String BROADCAST_ADDRESS = Constants.getFirstBroadcastAddressAsString();
	// broadcast address for receiving UDP packets while in DISCOVER phase
	public static final String ZERO_ADDRESS = "0.0.0.0";
	// broadcast group
	public static final String BROADCAST_GROUP = "230.0.0.1";
	
	// text message for initializing the DISCOVER protcol
	public static final String DISCOVER_REQUEST = "DISCOVER";
	// text message valid as a response to a DISCOVER request
	public static final String DISCOVER_RESPONSE = "DISCOVER_RESPONSE";
	// character to separate the text messages to the (small) data attached
	public static final String MESSAGE_CHAR_SEPARATOR = ":"; // this is a char indeed, but is more useful as a String
	// append to the server broadcast sent message if the device is already paired in the DISCOVERY phase
	public static final String MESSAGE_DEVICE_IS_PAIRED = "ISPAIRED";
	// append to the server broadcast sent message if the device is *NOT* already paired in the DISCOVERY phase
	public static final String MESSAGE_DEVICE_IS_UNPAIRED = "ISUNPAIRED";
	// the pairing request message sent from device to PC
	public static final String MESSAGE_PAIRME = "PAIRME";
	// the unpairing request message sent from device to PC
	public static final String MESSAGE_UNPAIRME = "UNPAIRME";
	// the pairing response message
	public static final String MESSAGE_PAIRED = "PAIRED";
	// the unpairing response message
	public static final String MESSAGE_UNPAIRED = "UNPAIRED";
	// the incoming drin event message
	public static final String INCOMING_DRIN = "INCOMING_DRIN";
	// a generic OK message
	public static final String MESSAGE_OK = "OK";
	// a generic ERROR message
	public static final String MESSAGE_ERROR = "ERROR";
	
	// how many times to send out the broadcast during DISCOVERY phase.
	public static final int    DISCOVERY_BROADCAST_COUNT = 3;
	// timeout for the client DISCOVER phase. The client will keep waiting for responses for this amount of time
	// single receive time out is: desired total timeout/number of time to send the broadcast	
	public static final int    DISCOVER_TIMEOUT = 12*1000 / Constants.DISCOVERY_BROADCAST_COUNT; // 10 seconds
	// timeout when waiting for a pairing request response
	public static final int	   PAIRING_TIMEOUT = DISCOVER_TIMEOUT;
	
	// size of the popup icon longest side
	public static final int    ICON_SIZE = 96;
	
	// action codes
	public static final int	   NO_ACTION = 0;
	public static final int	   SHOW_POPUP = 1;
	public static final int	   REMOVE_POPUP = 2;
	public static final int	   SHOW_PAIRED = 3;
	
	/**
	 * gets the first available broadcast address for the local network
	 * 
	 * @return the broadcast address as a string
	 */
	private static String getFirstBroadcastAddressAsString() {
	    String found_bcast_address=null;
	     System.setProperty("java.net.preferIPv4Stack", "true"); 
	        try
	        {
	          Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
	          while (niEnum.hasMoreElements())
	          {
	            NetworkInterface ni = niEnum.nextElement();
	            if(!ni.isLoopback()){
	                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses())
	                {
	                  if (interfaceAddress.getBroadcast() == null) continue;
	                  found_bcast_address = interfaceAddress.getBroadcast().toString();
	                  found_bcast_address = found_bcast_address.substring(1);

	                }
	            }
	          }
	        }
	        catch (SocketException e)
	        {
	          e.printStackTrace();
	        }

	        return found_bcast_address;
	}
	
	public Constants() {
		throw new AssertionError();
	}
}
