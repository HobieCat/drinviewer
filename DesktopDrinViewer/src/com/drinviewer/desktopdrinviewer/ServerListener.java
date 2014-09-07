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
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import com.drinviewer.common.Constants;
import com.drinviewer.common.IncomingDrinEvent;

/**
 * Implements the TCP server listener in a runnable, executing
 * the listening actions in separate threads, supporting
 * multiple clients connection at the same time.
 * 
 * @author giorgio
 *
 */
public class ServerListener implements Runnable {
	/**
	 * port number to open
	 * 
	 */
	private int port = Constants.PORT;
	
	/**
	 * list of listeners to be called when the IncomingDrin custom event is fired
	 * 
	 */
	private ArrayList<IncomingDrinListener> drinListeners = new ArrayList<IncomingDrinListener>();
	
	/**
	 * tells if the server is listening
	 * 
	 */ 
	private volatile boolean listening = false;
	
	/**
	 * the socket used for the communication
	 * 
	 */
	private ServerSocket serverSocket = null;	
	
	/**
	 * starts listening on a new thread, so multiple clients could be accepted
	 */
	@Override
	public void run() {
		listening = true;
        try { 
        	serverSocket = new ServerSocket(port);
            while (listening) {
            	System.out.println("Starting an accept thread");
	            new Thread(new ServerListenerRunnable(serverSocket.accept())).start();
	        }
	    } catch (SocketException e) {
	    	// nothing to do when the close() method is called on the serverSocket
	    } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            System.exit(-1);
        }
	}
	
	/**
	 * adds a new listener to the listeners list
	 * 
	 * @param listener the IncomingDrinListener to be added
	 */
	public void addIncomingDrinListener(IncomingDrinListener listener) {
		drinListeners.add(listener);
	}
	
	/**
	 * terminates by closing the socket
	 */
	public void terminate()
	{
		try {
			serverSocket.close();
		} catch (IOException e) {}
		
		serverSocket = null;
		listening = false;
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
	 * gets the listening status
	 * 
	 * @return true if the server is listening
	 */
	public boolean isListening() {
		return listening;
	}
	
	/**
	 * runnable used by ServerListener to make its own listening threads
	 * this is were the actual communication takes place
	 * 
	 * @author giorgio
	 *
	 */
	private class ServerListenerRunnable implements Runnable {
		/**
		 * the socket used for the communication as passed by ServerListener
		 * 
		 */
		private Socket socket = null;
		
		/**
		 * instantiate the class by setting the passed socket
		 * 
		 * @param socket
		 */
		public ServerListenerRunnable(Socket socket) {
			this.socket = socket;
		}
		
		/**
		 * fires and IncomingDrin custom Event
		 * 
		 * @param e the IncomingDrinEvent to be fired
		 */
		private void fireIncomingDrinEvent(IncomingDrinEvent e)
		{
			int size = drinListeners.size();
			if (size>0)
			{
				for (int i=0; i<size; i++) {
					// simply calls the handleDrin method for every listener
					drinListeners.get(i).handleDrin(e);
				}
			}
		}

		/**
		 * actual protocol implementation
		 */
		@Override
		public void run() {
	        try {
	        	PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	        	ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				String inputLine, outputLine;

				if ((inputLine = in.readUTF()) != null) {
					
					String incomingMessage = null;		// message to check that's coming from the device
					String outgoingMessage = null; 	 	// message to send out as per protocol
					String responseParam   = "";        // response parameter to be appended to the response
					Boolean actionIsPair   = null;      // true if it's a pair request
					
					if (inputLine.startsWith(Constants.MESSAGE_PAIRME)) {
						// it's a pair request
						incomingMessage = Constants.MESSAGE_PAIRME;
						outgoingMessage = Constants.MESSAGE_PAIRED;
						actionIsPair = true;
						fireIncomingDrinEvent(new IncomingDrinEvent(this, Constants.APPNAME, DesktopDrinViewerConstants.i18nMessages.getString("paired"), Constants.SHOW_PAIRED));
					} else if (inputLine.startsWith(Constants.MESSAGE_UNPAIRME)) {
						// it's an unpair request
						incomingMessage = Constants.MESSAGE_UNPAIRME;
						outgoingMessage = Constants.MESSAGE_UNPAIRED;
						actionIsPair = false;
						fireIncomingDrinEvent(new IncomingDrinEvent(this, Constants.APPNAME, DesktopDrinViewerConstants.i18nMessages.getString("unpaired"), Constants.SHOW_PAIRED));
					} else if (inputLine.startsWith(Constants.INCOMING_DRIN)) {
						fireIncomingDrinEvent((IncomingDrinEvent) in.readObject());
					} else { 					        // handle other kind of messages here
						outgoingMessage = Constants.MESSAGE_ERROR;
					}
					
					if (incomingMessage!=null && actionIsPair!=null)
					{
						// is a pairing message, extract the uuid to be paired
						String temp[] = inputLine.split(Constants.MESSAGE_CHAR_SEPARATOR);

						if (temp.length > 1) {
							// the uuid is there, take the required action and respond OK
							ServerDBManager db = new ServerDBManager();
							if (actionIsPair) db.pairHost(temp[1]);
							else db.unpairHost(temp[1]);
							
							responseParam = Constants.MESSAGE_OK;
							
						} else {
							// the uuid isn't there, respond ERORR
							responseParam = Constants.MESSAGE_ERROR;
						}						
					}
					
					// prepare the paired message response
					outputLine = outgoingMessage + Constants.MESSAGE_CHAR_SEPARATOR + responseParam;
					// and send it
					out.println(outputLine);
				}
				out.close();
				in.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (ClassNotFoundException e){
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
