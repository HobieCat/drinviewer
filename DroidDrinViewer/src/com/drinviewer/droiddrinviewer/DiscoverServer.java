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
	 */
	int port = Constants.PORT;
	
	/**
	 * tells if the thread is running
	 * 
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
	 * the wifiBroadcastAddress
	 */
	private String wifiBroadcastAddress;
	
	/**
	 * timeout to set when discovering, as follows:
	 * - start from Constants.DISCOVER_TIMEOUT
	 * - if the timeout is reached, double it for the next packet
	 * - if a packet is received, reset it to Constants.DISCOVER_TIMEOUT
	 * - if the timeout exceeds DroidDrinViewerConstants.DISCOVERY_MAX_TIMEOUT stop discovering
	 */	
	private int currentTimeOut;
	
	/**
	 * constructor, just sets the serverCollection
	 * 
	 * @param serverCollection the HostCollection object to be filled
	 * 
	 */
	public DiscoverServer(DrinHostCollection serverCollection) {
		this.serverCollection = serverCollection;
		this.wifiBroadcastAddress = Constants.BROADCAST_ADDRESS;
		this.currentTimeOut = Constants.DISCOVER_TIMEOUT;
	}

	/**
	 * runs the actual discovery protocol
	 */
	@Override
	public void run() {
		if (wifiBroadcastAddress != null) {
			try {
				// build the data to be sent	
				byte[] sendData = (Constants.DISCOVER_REQUEST+Constants.MESSAGE_CHAR_SEPARATOR+uuid).getBytes();
				// build the packet to be sent
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(wifiBroadcastAddress), port);
				// build the socket to be used for sending what's been built
				DatagramSocket socket = new DatagramSocket();
				socket.setBroadcast(true);
				
				// declare the receive buffer
				byte[] recvBuf = null;
				// declare the receiver DatagramPacket
				DatagramPacket receivePacket = null;
				
				running = uuid!=null;
				
				int loopNumber = 1;
				boolean packetSentInThisLoop = false;
				
				if (running) {
					recvBuf = new byte[Constants.BUFLEN];				
				}
				
				while (running) {
					try {
						// send the broadcast out if needed
						if (!packetSentInThisLoop) {
							socket.send(sendPacket);
							packetSentInThisLoop = true;
							// print a message to the user
							System.out.println(getClass().getName()+">>> Request packet sent to: " + wifiBroadcastAddress);
						}
						
						// setup stuff and wait for a response
						if (recvBuf!=null) {
							if (receivePacket==null) receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
							else receivePacket.setData(recvBuf);
							System.out.println(getClass().getName()+">>> Setting timeout to: " + currentTimeOut);
							// if for some reason the currentTimeOut becomes too big, terminate
							if (currentTimeOut > DroidDrinViewerConstants.DISCOVERY_MAX_TIMEOUT) {
								terminate();
								break;
							}
							socket.setSoTimeout(currentTimeOut);
							System.out.println(getClass().getName()+">>> loop:"+loopNumber+" Waiting for a packet...");
							socket.receive(receivePacket);
						}
						
						// we have a response here, since receive is blocking
						System.err.println(getClass().getName()+">>> ...got it! :)");
						
						// looks like network is responding, reset the socket timeout if it was increased
						if (currentTimeOut > Constants.DISCOVER_TIMEOUT) currentTimeOut = Constants.DISCOVER_TIMEOUT;
						
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
							DrinHostData foundHost = new DrinHostData(serverHostName,serverHostAddress,isPaired);
							// if found host is not in the collection, add it and decrease the loop
							// count so that this host does not count in the total timeout count
							if (!serverCollection.isInList(foundHost)) {
								System.err.println(getClass().getName()+">>> ADDING HOST");
								serverCollection.put(foundHost);
								loopNumber--;
							}
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
						// looks like network is slow on responding,
						// increase the timeout for the next iterations
							currentTimeOut += currentTimeOut;
							System.err.println(getClass().getName()+">>> NEW TIME OUT: "+currentTimeOut);
					} finally {
						if (loopNumber >= Constants.DISCOVERY_BROADCAST_COUNT) {
							// print a message to the user
							System.err.println(getClass().getName()+">>> DONE DISCOVERY.");
							// self terminate, sets running to false
							terminate();
						} else {
							// send another broadcast and increase the loop counter
							packetSentInThisLoop = false;
							loopNumber++;
						}
					}
				}
				// we're terminating here and out of the while loop, close the socket and terminate
				if (socket!=null) socket.close();
			} catch (IOException e) {
				e.printStackTrace();
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
	 * wifiBroadcastAddress setter
	 * 
	 * @param wifiBroadcastAddress the address to set
	 */
	public void setBroadcastAddress(String wifiBroadcastAddress) {
		this.wifiBroadcastAddress = wifiBroadcastAddress;
	}
}
