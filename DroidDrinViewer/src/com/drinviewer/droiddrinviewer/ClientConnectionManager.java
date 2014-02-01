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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.drinviewer.common.Constants;
import com.drinviewer.common.HostData;
import com.drinviewer.common.IncomingDrinEvent;

/**
 * pairing manager class, handles the pairing communication
 * between the android application and the desktop one
 * 
 * @author giorgio
 *
 */

public class ClientConnectionManager {
	/**
	 * port number to open
	 * 
	 */
	private int port = Constants.PORT;
	
	/**
	 * the host to which the pairing request is sent
	 */
	private HostData hostToPair;
	
	/**
	 * the device UUID to be paired
	 */
	private String uuid;
	
	/**
	 * socket for communication
	 */
	private Socket socket;
	
	/**
	 * write to
	 */
	private ObjectOutputStream out;
	
	/**
	 * read from
	 */
	private BufferedReader in;
	
	
	/**
	 * instantiates the class by passing the host to which
	 * you want to connect
	 * 
	 * @param hostToPair the host to connect to
	 */
	public ClientConnectionManager(HostData hostToPair) {
		this.hostToPair = hostToPair;
	}
	
	/**
	 * Initializes the connection by opening the socket and
	 * instantiating in and out objects for reading and writing
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void initConnection() throws UnknownHostException, IOException {
		try {
			socket = new Socket(hostToPair.address, port);
	        out = new ObjectOutputStream(socket.getOutputStream());
	        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostToPair.address);
            throw e;
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostToPair.address);
            throw e;
        }
	}
	
	/**
	 * Closes the connection by closing the socket
	 * together with in and out objects
	 */
	private void closeConnection() {
        try {
        	if (out!=null) {
        		out.flush();
        		out.close();
        	}        	
            if (in != null) in.close();
			if (socket != null) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends out the incoming drin event to the host
	 * passed when calling the constructor
	 * 
	 * @param event IncomingDrinEvent Object to send
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void sendDrinEvent (IncomingDrinEvent event) throws UnknownHostException, IOException {
		initConnection();
		try {
			out.writeUTF(Constants.INCOMING_DRIN);
			out.writeObject(event);
		} finally {
			closeConnection();
		}
	}

	/**
	 * Issues a pair request to the server and waits for a response
	 * 
	 * this has to wait for the server to respond, so it's not put
	 * into a thread. Who sends out a pair/unpair request must wait
	 * for the server response anyway...
	 * 
	 * @return true if success
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public boolean runPairProtocol() throws UnknownHostException, IOException {
		boolean isPaired = false;
		initConnection();
		try {			
			String fromServer;
			if (uuid != null) {
				String sendMessage = null; // the message to send to the server
				String expectedMessage = null; // the message that is expected to come from the server

				// prepares the message to send and to expect to and from the server
				if (!hostToPair.isPaired) {
					sendMessage = Constants.MESSAGE_PAIRME;
					expectedMessage = Constants.MESSAGE_PAIRED;
				} else {
					sendMessage = Constants.MESSAGE_UNPAIRME;
					expectedMessage = Constants.MESSAGE_UNPAIRED;
				}

				if (sendMessage != null && expectedMessage != null) {
					// sends out the pairing request
					out.writeUTF(sendMessage + Constants.MESSAGE_CHAR_SEPARATOR + uuid);
					out.flush();
					// set a timeout
					socket.setSoTimeout(Constants.PAIRING_TIMEOUT);
					// wait for a pairing request response message
					if ((fromServer = in.readLine()) != null) {
						if (fromServer.startsWith(expectedMessage)) {
							if (fromServer.contains(Constants.MESSAGE_OK)) {
								// the device has been successfully paired, do something
								isPaired = !hostToPair.isPaired;
							}
						}
					}
				}
			}
		} catch (SocketTimeoutException e) {
			System.err.println("Connection has timed out to host " + hostToPair.address);
			throw e;
		} finally {
			closeConnection();
		}
        return isPaired;
	}
	
	/**
	 * uuid setter 
	 * @param uuid the device uuid to set
	 */
	public void setUUID (String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * Sets the port to connect to
	 * 
	 * @param port
	 */
	public void connectOnPort(int port) {
		this.port = port;
	}
}
