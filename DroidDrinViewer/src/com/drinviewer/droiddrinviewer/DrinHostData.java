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

import com.drinviewer.common.HostData;

/**
 * HostData class for Android app
 * 
 * Implements the Parcelable interface to
 * properly communicate with the background service
 * 
 * @author giorgio
 *
 */
public class DrinHostData extends HostData implements Parcelable {
	
	/**
	 * Creator Object to used to implement the Parcelable interface
	 */
	public static final Creator<DrinHostData> CREATOR = new Creator<DrinHostData>() {

		@Override
		public DrinHostData createFromParcel(Parcel parcel) {
			return new DrinHostData(parcel);
		}

		@Override
		public DrinHostData[] newArray(int size) {
			return new DrinHostData[size];
		}
	};
	
	/**
	 * constructor, sets hostname and IP address and isPaired
	 * 
	 * @param hostname the server hostname to be set
	 * @param address the server IP address to be set
	 * @param isPaired true if the server is paired with the device
	 */
	public DrinHostData(String hostname, String address, boolean isPaired) {
		super(hostname, address, isPaired);
	}
	
	/**
	 * constructor from a parcel Object
	 * 
	 * @param parcel the received Parcel
	 */
	private DrinHostData(Parcel parcel) {
		hostname = parcel.readString();
		address = parcel.readString();
		boolean[] tmpBooleanArr = new boolean[1];
		parcel.readBooleanArray(tmpBooleanArr);
		isPaired = tmpBooleanArr[0];
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(hostname);
		dest.writeString(address);
		dest.writeBooleanArray(new boolean[] {isPaired});
	}
}
