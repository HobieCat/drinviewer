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

import java.net.BindException;

import com.drinviewer.common.Constants;

/**
 * This is the DesktopServer test application
 * 
 * @author giorgio
 * 
 */
public class DesktopServer {
	// port used for communication, if not passed from the command line, use the default
	protected int port;
	// the broadcast listener object
	private BroadCastListener broadcastListener;
	// the TCP listener object
	protected ServerListener serverListener;
	
	// the thread running the broadcastListener
	private Thread broadcastListenerThread;
	// the thread running the TCP listener
	private Thread listenerThread;
	
	/**
	 * instantiate the class setting a port to listen to
	 * 
	 * @param port
	 */
	public DesktopServer(int port) {
		this.port = port;		
		// get the Singleton instance of the BroadCastListener class
		broadcastListener = BroadCastListener.getInstance();
		// set the port (unnecessary if using the default)
		if (Constants.PORT != this.port) broadcastListener.listenOnPort(this.port);
		// instantiate a new TCP listener
		serverListener = new ServerListener();
		// set the port (unnecessary if using the default)
		if (Constants.PORT != this.port) serverListener.listenOnPort(port);
	}

	/**
	 * instantiate the class using the default listening port
	 */
	public DesktopServer() {
		this (Constants.PORT);
	}
	
	/**
	 * tells if the server is running
	 * 
	 * @return true if the broadcast server is running and the TCP is listening
	 */
	public boolean isRunning() {
		return broadcastListener.isRunning() && serverListener.isListening();
	}
	
	/**
	 * starts the server
	 * Throws the exception if cannot bind so
	 * that the UI class can display appropriate
	 * message to the user
	 * 
	 * @throws BindException 
	 */
	public void startServer() throws BindException {
		/**
		 * Initializes the broadcast socket and then runs the thread
		 */
		broadcastListener.initDatagramSocket();
		
		if (broadcastListenerThread == null) {
			broadcastListenerThread = new Thread(broadcastListener);
			broadcastListenerThread.setDaemon(true);
			broadcastListenerThread.start();
		}
		
		if (listenerThread == null) {
			listenerThread = new Thread(serverListener);
			listenerThread.setDaemon(true);
			listenerThread.start();	
		}
	}
	
	/**
	 * stops the server
	 */
	public void stopServer() {
		try {
			if (isRunning())
			{
				if (broadcastListenerThread != null) {
					// terminate the broadcast listener
					broadcastListener.terminate();
			    	// join the broadcast listener thread
					broadcastListenerThread.join();
					broadcastListenerThread = null;
				}
				
				if (listenerThread != null) {
					// terminate the TCP listener
					serverListener.terminate();
					// terminate and join the TCP listener thread
					listenerThread.join();
					listenerThread = null;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * sets the do not disturb mode on the broadcast listener,
	 * to force it to not respond to broadcast request coming
	 * from device that are unpaired
	 * 
	 * @param mode true to not respond
	 */
	public void setDoNotDisturbMode(boolean mode) {
		broadcastListener.setDoNotDisturbMode(mode);
	}
	
	/**
	 * adds a listener for the custom IncomingDrin event
	 * 
	 * @param listener the IncomingDrinListener to be added
	 */
	public void addListener (IncomingDrinListener listener) {
		serverListener.addIncomingDrinListener(listener);
	}
}