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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import com.drinviewer.common.Constants;
import com.drinviewer.common.HostData;
import com.drinviewer.droiddrinviewer.DrinViewerActivity.DrinViewerHandler;


/**
 * Class to discover the available servers using and UDP broadcast
 * and has the producer role for the serverCollection Object
 * 
 * @author giorgio
 *
 */
public class DiscoverServer implements Runnable {
	
	/**
	 * port number to open
	 * 
	 * @var int
	 */
	int port = Constants.PORT;
	
	/**
	 * tells if the thread is running
	 * 
	 * @var boolean
	 */ 
	private volatile boolean running = false;
	
	/**
	 * this Runnable has the producer role for the serverCollection Object
	 */
	private DrinHostCollection serverCollection;
	
	/**
	 * the android device calculated uuid
	 */
	private String uuid = null;
	
	/**
	 * the handler to send messages to
	 */
	private DrinViewerHandler handler;
	
	/**
	 * true if must send the message to update the UI
	 */
	private boolean sendUpdateUIMessage;
	
	/**
	 * constructor, just sets the serverCollection
	 * 
	 * @param serverCollection the HostCollection object to be filled
	 * 
	 */
	public DiscoverServer(DrinHostCollection serverCollection) {
		this.serverCollection = serverCollection;
		handler = null;
		setSendUpdateUIMessage(false);
	}
	
	/**
	 * constructor, sets the serverCollection and the handler
	 * 
	 * @param serverCollection the HostCollection object to be filled
	 * @param DrinViewerHandler the handler to set
	 * 
	 */
	public DiscoverServer(DrinHostCollection serverCollection, DrinViewerHandler handler) {
		this(serverCollection);
		this.handler = handler;
	}
	
	/**
	 * run implementation
	 */
	@Override
	public void run() {
		try {
			// build the datas to be sent	
			byte[] sendData = (Constants.DISCOVER_REQUEST+Constants.MESSAGE_CHAR_SEPARATOR+uuid).getBytes();
			// build the packet to be sent
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(Constants.BROADCAST_ADDRESS), port);
			// build the socket to be used for sending what's been built
			DatagramSocket socket = new DatagramSocket();
			socket.setBroadcast(true);
			
			// declare the receive buffer
			byte[] recvBuf = null;
			// declare the receiver DatagramPacket
			DatagramPacket receivePacket = null;
			
			running = uuid!=null;
			
			int loopNumber = 0;
			boolean packetSentInThisLoop = false;
			
			if (running) {
				sendMessage(DroidDrinViewerConstants.MSG_DISCOVER_START);
				recvBuf = new byte[Constants.BUFLEN];				
			}
			
			while (running) {
				try {
					// send the broadcast out if needed
					if (!packetSentInThisLoop) {
						socket.send(sendPacket);
						packetSentInThisLoop = true;
						// print a message to the user
						System.out.println(getClass().getName()+">>> Request packet sent to: " + Constants.BROADCAST_ADDRESS);
					}
					
					// setup stuff and wait for a response
					if (recvBuf!=null) {
						if (receivePacket==null) receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
						else receivePacket.setData(recvBuf);
						
						socket.setSoTimeout(Constants.DISCOVER_TIMEOUT);
						socket.receive(receivePacket);
					}
					
					// we have a response here, since receive is blocking
					// extract the response to a string and check if it's what we'd expected
					String message = new String(receivePacket.getData()).trim();
					
					// so, if it's a DISCOVER_RESPONSE, do something with the received data			
				    if (message.indexOf(Constants.DISCOVER_RESPONSE)==0) {
				    	// initialize the responding server as unpaired
				    	boolean isPaired = false;
				    	// get the server host address
						String serverHostAddress = receivePacket.getAddress().getHostAddress();
						// for time being the server host name is its address
						String serverHostName = serverHostAddress;
						// split the received string using MESSAGE_CHAR_SEPARATOR to look for the server name
						String temp[] = message.split(Constants.MESSAGE_CHAR_SEPARATOR);
						// if the splitted array has more than two elements, there's the host name in the received message
						// the paired parameter is (should be) always there, but double check it						
						if (temp.length>2) serverHostName = temp[2];
						if (temp.length>1) isPaired = temp[1].equals(Constants.MESSAGE_DEVICE_IS_PAIRED);
						// add the the hostname and IP address to the list of discovered servers
						serverCollection.put(new HostData(serverHostName,serverHostAddress,isPaired));
						sendMessage(DroidDrinViewerConstants.MSG_SERVER_FOUND);
					  }
				    
				    message = null;
				    
					/**
					 *  USE THE FOLLOWING CODE IF AN ARRAY OF DATA IS TO BE RECEIVED IN A SUBSEQUENT PACKET
					 */
				    
				    /*
	 			    c.receive(receivePacket);
					  
					// deserialize
					ByteArrayInputStream in = new ByteArrayInputStream(receivePacket.getData());
					    
				    String[] mydata = null;
					    
					try {
						mydata = (String[]) new ObjectInputStream(in).readObject();
					} catch (ClassNotFoundException e) {					
						e.printStackTrace();
					}
					    
					if (mydata!=null) {
						for (String mystr : mydata)
						{
							System.out.println(mystr);
						}
					} else System.out.println("mydata is null :(");
	 			    */
				    
				} catch (SocketTimeoutException e) {
					if (++loopNumber >= Constants.DISCOVERY_BROADCAST_COUNT) {
						// print a message to the user
						System.out.println(getClass().getName()+">>> DONE DISCOVERY.");
						// self terminate
						terminate();
					} else packetSentInThisLoop = false;
				}
			}
			// we're terminating here and out of the while loop, close the socket and terminate
			if (socket!=null) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends messages to the handler
	 * 
	 * @param what the type of message to send
	 */
	private void sendMessage(int what)
	{
		if (isSendUpdateUIMessage() && handler != null) {
			switch (what) {
			case DroidDrinViewerConstants.MSG_SERVER_FOUND:
			case DroidDrinViewerConstants.MSG_DISCOVER_START:
			case DroidDrinViewerConstants.MSG_DISCOVER_DONE:
				handler.sendEmptyMessage(what);
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Tells if the runnable is running
	 * 
	 * @return boolean true if the discover process is running
	 * @access public
	 */
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Terminates the runnable
	 * 
	 * @access public
	 */
	public void terminate() {
		serverCollection.notifyProducerHasStopped();
		running = false;
		sendMessage(DroidDrinViewerConstants.MSG_DISCOVER_DONE);
	}
	
	/**
	 * uuid setter 
	 * @param uuid the device uuid to set
	 */
	public void setUUID (String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * Set the port to connect to
	 * 
	 * @param port the UDP port to set
	 */
	public void connectOnPort(int port) {
		this.port = port;
	}

	/**
	 * sendUpdateUIMessage getter
	 * 
	 * @return the sendUpdateUIMessage
	 */
	public boolean isSendUpdateUIMessage() {
		return sendUpdateUIMessage;
	}

	/**
	 * sendUpdateUIMessage setter
	 * 
	 * @param sendUpdateUIMessage the sendUpdateUIMessage to set
	 */
	public void setSendUpdateUIMessage(boolean sendUpdateUIMessage) {
		this.sendUpdateUIMessage = sendUpdateUIMessage;
	}
}
