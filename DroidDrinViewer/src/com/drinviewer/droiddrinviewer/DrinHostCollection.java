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

import android.os.Parcel;
import android.os.Parcelable;

import com.drinviewer.common.HostCollection;
import com.drinviewer.common.HostData;

/**
 * Host collection class for Android app
 * 
 * Implements the Parcelable interface to
 * properly communicate with the background service
 * 
 * @author giorgio
 *
 */
public class DrinHostCollection extends HostCollection implements Parcelable {
	
	/**
	 * Creator Object to used to implement the Parcelable interface
	 */
	public static final Creator<DrinHostCollection> CREATOR = new Creator<DrinHostCollection>() {

		@Override
		public DrinHostCollection createFromParcel(Parcel parcel) {
			return new DrinHostCollection(parcel);
		}

		@Override
		public DrinHostCollection[] newArray(int size) {
			return new DrinHostCollection[size];
		}
	};
	
	/**
	 * constructor, just calls the super
	 */
	public DrinHostCollection() {
		super();
	}
	
	/**
	 * Overridden getLast method to return a DrinHostData Object
	 */
	@Override
	public DrinHostData getLast() {
		HostData hs = super.getLast();
		if (hs != null) return new DrinHostData(hs.hostname, hs.address, hs.isPaired);		
		else return null;
	}
	
	/**
	 * Overridden setPaired method to set a DrinHostData Object from a HostData
	 * 
	 * @param element the HostData Object to be paired or unpaired
	 * @param isPaired the value to be set
	 */
	@Override
	public void setPaired (HostData element, boolean isPaired)
	{
		if (isInList(element))
		{
			int index = hostList.indexOf(element);
			hostList.set(index, new DrinHostData(element.hostname, element.address, isPaired));
		}
	}

	/**
	 * constructor from a parcel Object
	 * 
	 * @param parcel the received Parcel
	 */
	@SuppressWarnings("unchecked")
	public DrinHostCollection(Parcel parcel) {
		setHostList(parcel.readArrayList(DrinHostCollection.class.getClassLoader()));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(hostList);
	}
}
