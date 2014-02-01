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

import java.util.ArrayList;

/**
 * collection of the discovered hosts,
 * as an ArrayList of HostData Objects
 * 
 * @author giorgio
 *
 */
public class HostCollection {
	
	/**
	 * the array of the discovered hosts
	 * 
	 */
	protected ArrayList<HostData> hostList;
	
	/**
	 * true  if consumer should wait for producer to send message,
	 * false if producer should wait for consumer to retrieve message.
	 *  
	 */
    private boolean elementReady = true;
    
    /**
     * true if the producer runnable is running
     * false if the producer runnable is stopped
     * 
     */
    private boolean producerRunning  = true;

    /**
     * constructor, allocates a new ArrayList
     */
	public HostCollection() {
		setHostList(new ArrayList<HostData>());
	}
	
	/**
	 * return the last discovered host to the consumer
	 * in a synchronized way, so that the consumer keeps
	 * waiting till a new host is found or the producer
	 * is stopped
	 * 
	 * @return HostData
	 */
	public synchronized HostData getLast() {
		// wait until an element (i.e. a found host) is available.
		while (elementReady) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// toggle status
		elementReady = true;
		// notify producer that status has changed
        notifyAll();
        
        if (producerRunning && !hostList.isEmpty()) return get(hostList.size()-1);
        else return null;
	}
	
	/**
	 * adds a new found host to the array in a synchronized way
	 * 
	 * @param element the new discovered host
	 */
	public synchronized void put (HostData element) {
		// wait until an element (i.e. a found host) has been retrieved.
		while (!elementReady) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// store element if not already in the array	
        if (!isInList(element)) {
        	hostList.add(element);
    		// toggle status
    		elementReady = false;
            // notify consumer that status has changed
            notifyAll();
        }
	}
	
	public void add (HostData element) {
		// store element if not already in the array	
        if (!isInList(element)) hostList.add(element);
	}
	
	/**
	 * gets the HostData object at the specified index
	 * 
	 * @param index the index of the array to get
	 * @return the selected HostData Object or null
	 */
	public HostData get(int index)
	{
		if (index < hostList.size())
		{
			return hostList.get(index);
		} else return null;
	}
	
	/**
	 * sets the paired value of the passed element
	 * 
	 * @param element the HostData Object to be paired or unpaired
	 * @param isPaired the value to be set
	 */
	public void setPaired (HostData element, boolean isPaired)
	{
		if (isInList(element))
		{
			int index = hostList.indexOf(element);
			hostList.set(index, new HostData(element.hostname, element.address, isPaired ));
		}
	}
	
	/**
	 * initialize the list to an empty ArrayList
	 * sets the boolean as appropriate and notifies all
	 */
	public synchronized void init()	
	{
		hostList.clear();
		elementReady = true;
		producerRunning = true;
		notifyAll();
	}
	
	/**
	 * gets the size of the hostList ArrayList
	 * @return hostList size
	 */
	public int size()
	{
		return hostList.size();
	}
	
	/**
	 * checks if the given element is in the host list
	 * 
	 * @param element the element to check
	 * @return true if the element is in the list
	 */
	public boolean isInList (HostData element)
	{
		return hostList.indexOf(element) != -1;
	}
	
	/**
	 * tells if the producer is running
	 * 
	 * @return boolean
	 */
	public boolean isProducerRunning() {
		return producerRunning;
	}
	
	/**
	 * used by the producer to tell the class that
	 * it has been stopped by some one, unlocks 
	 * the getLast method so that it can return for the
	 * last time if the consumer is still waiting
	 * 
	 */	
	public synchronized void notifyProducerHasStopped() {
		producerRunning = false;
		elementReady = false;
		// notify consumer so that getLast shall return and it will stop waiting
		notifyAll();
	}

	/**
	 * @param hostList the hostList to set
	 */
	public void setHostList(ArrayList<HostData> hostList) {
		this.hostList = hostList;
	}
}
