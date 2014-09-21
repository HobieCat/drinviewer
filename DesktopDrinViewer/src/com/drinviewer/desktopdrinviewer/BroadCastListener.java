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

package com.drinviewer.desktopdrinviewer;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.drinviewer.common.Constants;
import com.drinviewer.common.HostData;

/**
 * Main class for the server
 * This is a singleton, this is why it has a holder class below
 * 
 * @author giorgio
 * 
 */
public class BroadCastListener implements Runnable {

	/**
	 * socket used for listening
	 * 
	 */
	private DatagramSocket socket = null;
	
	/**
	 * port number to open
	 * 
	 */
	private int port = Constants.PORT;
	
	/**
	 * tells if the thread is running
	 * 
	 */ 
	private volatile boolean running = false;
	
	/**
	 * local host name to be sent back to the client
	 * 
	 */
	private String localHostName = null;

	/**
	 * Class constructor:
	 * 
	 * gets the local host name only once, and will be 
	 * sent out together with the DISCOVER_RESPONSE message
	 * @throws SocketException 
	 */
	public BroadCastListener() {		
		try {
			localHostName = InetAddress.getLocalHost().getHostName();			
		} catch (UnknownHostException e) {
			localHostName = null;
		}		
	}
	
	/**
	 * init the datagram socket
	 * The exception is thrown if the port has
	 * already been taken (server already running
	 * or port in use by someone else)
	 * 
	 * @throws BindException
	 */
	public void initDatagramSocket() throws BindException {
		try {
			socket = new DatagramSocket(port, InetAddress.getByName(Constants.ZERO_ADDRESS));
			socket.setBroadcast(true);
			socket.setSoTimeout(500);
			port = socket.getLocalPort();
			// print a message to the user
			// System.out.println(getClass().getName() + ">>> " + socket.getClass().getName() + " socket opened on port " + port);
			running = true;
		} catch (BindException e) {
			throw e;
		} catch (SocketException e) {
			e.printStackTrace();
			// System.err.println("Could not open datagaram socket");
			System.exit(-1);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * run method implementation:
	 * while we're running, receive a packet and pass it to the handler
	 */
	@Override
	public void run() {
		try {
			// note: running is set to true in initDatagramSocket
			while (running) {
				try {	
					// Receive a packet, the timeout is there to properly stop the Runnable
					byte[] recvBuf = new byte[Constants.BUFLEN];
					DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
					socket.receive(packet);
					// a packet has arrived!! print a message to the user
					// System.out.println(getClass().getName() + ">>>Packet received; data: " + new String(packet.getData()));
					// handle the incoming message
					handleMessage(packet);
				} catch (SocketTimeoutException e) {
					// it's safe to do nothing and restart if the socket times out.
					// the timeout is there only to terminate suddenly 
					// when terminate() is called.
					// This catch must be inside the loop
				}			
			} // ends while
			if (socket != null) socket.close();
		} catch (IOException e) {
			terminate();
			e.printStackTrace();
		}
	}
	
	/**
	 * handles the incoming packet, checking its validity,
	 * produces and sends out appropriate responses in a
	 * new thread
	 * 
	 * @param packet
	 */
	private void handleMessage(final DatagramPacket packet) {
		// temporarily store the uuid
		final String receivedUUID;
		
		// extract the response to a string and check if it's what we'd expected
        final String message = new String(packet.getData()).trim();
        
	        // so, if it's a DISCOVER_REQUEST, do something with the received data	
	        if (message.startsWith(Constants.DISCOVER_REQUEST)) {
	        	
	        	// look for a received uuid and place it as first message parametr
	        	String temp[] = message.split(Constants.MESSAGE_CHAR_SEPARATOR);
	        	
				// if the splitted array has more than one element, there's the uuid in the received message
				if (temp.length>1) receivedUUID = temp[1];
				else receivedUUID = null;
				
				// do the real message handling in a new thread
	        	new Thread (new Runnable() 
	        	{
	        		@Override
					public void run() {
			        	// prepare the message to send out
			        	String sendMessage = Constants.DISCOVER_RESPONSE;
			        	
			        	// check if the receivedUUID owns to a paired device
						if (receivedUUID!=null && new ServerDBManager().isPaired( new HostData(receivedUUID, packet.getAddress().getHostAddress()))) {
							sendMessage += Constants.MESSAGE_CHAR_SEPARATOR+Constants.MESSAGE_DEVICE_IS_PAIRED;
						} else {
							sendMessage += Constants.MESSAGE_CHAR_SEPARATOR+Constants.MESSAGE_DEVICE_IS_UNPAIRED;
						}
			        	
						// if there's a localHostName, put it as second optional message parameter
			        	if (localHostName!=null) sendMessage += Constants.MESSAGE_CHAR_SEPARATOR + localHostName;
			        	
			        	// sendMessage ends here
			        	sendMessage += Constants.MESSAGE_CHAR_SEPARATOR + Constants.DISCOVER_RESPONSE_END;
			        	
			            byte[] sendData = sendMessage.getBytes();
			 
			            // send out the response
			            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
			            try {
			            	socket.send(sendPacket);
			            	// print a message to the user
			            	// System.out.println(this.getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
			            } catch (IOException e) {
			            	// System.out.println(this.getClass().getName() + ">>>ERROR SENDING PACKET: " + e.getMessage() );
			            }			            
	        		} // ends run method
	        	}).start();
	        } // ends if message.startsWith
	}
	
	/**
	 * tells if the runnable is running
	 * 
	 * @return boolean
	 */
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * terminates the runnable
	 * 
	 */
	public void terminate() {
		running = false;
	}

	/**
	 * set the listening port
	 * 
	 * @param port
	 */
	public void listenOnPort(int port) {
		this.port = port;
	}

	/**
	 * gets the singleton instance
	 * 
	 * @return BroadCastListener
	 */
	public static BroadCastListener getInstance() {
		return listenerThreadHolder.INSTANCE;
	}

	/**
	 * holder class for the singleton implementation
	 * 
	 * @author giorgio
	 *
	 */
	private static class listenerThreadHolder {
		private static final BroadCastListener INSTANCE = new BroadCastListener();
	}
}