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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.drinviewer.common.HostData;
/**
 * Adapter class for the ListView in the ServerListFragment
 * 
 * @author giorgio
 *
 */
public class DrinHostAdapter extends BaseAdapter {
	
	/**
	 * Main activity context
	 * 
	 */
	private Context context;
	
	/**
	 * DrinHostCollection to be displayed
	 * 
	 */
	private DrinHostCollection hostCollection;

	public DrinHostAdapter(Context context) {
		this.context = context;
		setHostCollection(new DrinHostCollection());
	}

	/**
	 * Gets the HostAdapter view
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.serverlistrow, parent, false);
		TextView serverNameLine = (TextView) rowView.findViewById(R.id.serverNameLine);
		TextView serverIPLine = (TextView) rowView.findViewById(R.id.serverIPLine);
		ImageView iconPaired = (ImageView) rowView.findViewById(R.id.iconPaired);
		
		HostData data = hostCollection.get(position);
		if (data != null) {
			serverNameLine.setText(data.hostname);
			serverIPLine.setText(data.address);	
			
			iconPaired.setImageDrawable(context.getResources().getDrawable(
					(data.isPaired) ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off ));
		}
		return rowView;		
	}
	
	/**
	 * Sets the DrinHostCollection to be displayed
	 * 
	 * @param c the collection to be displayed
	 */
	public void setHostCollection (DrinHostCollection c) {
		hostCollection = c;
	}
	
	/**
	 * hostCollection getter
	 * 
	 * @return the hostCollection
	 */
	public DrinHostCollection getHostCollection() {
		return hostCollection;
	}

	/**
	 * clears the DrinHostCollection
	 */
	public void initHostCollection () {
		hostCollection.init();
	}
	
	/**
	 * Updates isPaired status on an Host and redraws the
	 * list to set the appropriate icon
	 * 
	 * @param position the position of the host to be paired in the list
	 * @param isPaired true if the host is paired
	 */
	public void updateIsPaired(int position, boolean isPaired) {
		hostCollection.setPaired(hostCollection.get(position), isPaired);
	}
	
	/**
	 * Counts the item in the collection
	 */
	@Override
	public int getCount() {
		return hostCollection.size();
	}

	/**
	 * Gets the item in the collection at the passed position
	 */
	@Override
	public HostData getItem(int position) {
		return hostCollection.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}
