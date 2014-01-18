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

/**
 * class for holding the discovered server data
 * 
 * @author giorgio
 *
 */
public class HostData {
	
	/**
	 * the server host name
	 * 
	 */
	public String hostname;
	
	/**
	 * the server IP address
	 * 
	 */
	public String address;
	
	/**
	 * if the host is a server, true if it's paired with the device
	 */
	public boolean isPaired;
	
	/**
	 * constructor, set all properties
	 * to empty string and isPaired to false
	 */
	public HostData() {
		this.hostname = "";
		this.address = "";
		this.isPaired = false;
	}
	
	/**
	 * constructor, sets hostname and IP address
	 * 
	 * @param hostname the server hostname to be set
	 * @param address the server IP address to be set
	 */
	public HostData(String hostname, String address) {
		this.hostname = hostname;
		this.address = address;
		this.isPaired = false;
	}

	/**
	 * constructor, sets hostname and IP address and isPaired
	 * 
	 * @param hostname the server hostname to be set
	 * @param address the server IP address to be set
	 * @param isPaired true if the server is paired with the device
	 */
	public HostData(String hostname, String address, boolean isPaired) {
		this (hostname, address);
		this.isPaired = isPaired;
	}

	/**
	 * calculates the Object hashCode
	 * used by the indexOf method of the ArrayList
	 * 
	 * @return int the calculated hasCode
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
		result = prime * result + (isPaired ? 1231 : 1237);
		return result;
	}

	/**
	 * tells if two Objects are equal
	 * used by the indexOf method of the ArrayList
	 * 
	 * @param obj the object to be checked
	 * @return boolean true if the this is equal to obj
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof HostData)) return false;
		
		HostData other = (HostData) obj;
		
		if (address == null) {
			if (other.address != null) return false;
		} else if (!address.equals(other.address)) return false;
		
		if (hostname == null) {
			if (other.hostname != null) return false;
		} else if (!hostname.equals(other.hostname)) return false;
		
		if (isPaired != other.isPaired) return false;
		
		return true;
	}
}
