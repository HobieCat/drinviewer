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

import java.util.EventObject;
/**
 * class implementing the custom IncomingDrinEvent that is fired
 * when we receive a drin message from the client
 * 
 * @author giorgio
 *
 */
public class IncomingDrinEvent extends EventObject {
	/**
	 * serial version ID
	 */
	private static final long serialVersionUID = 1213150563549939051L;

	/**
	 * the title to be displayed in the popup
	 * 
	 */
	public String title;
	
	/**
	 * the message to be displayed in the popup
	 * 
	 */
	public String message;

	/**
	 * the image to be displayed in the popup
	 */
	public byte[] imageData;
	
	/**
	 * Instantiate the class
	 * 
	 * @param source the source generator object
	 * @param title the title to be displayed in the popup
	 * @param message the message to be displayed in the popup
	 * @param imageData the imagedata to be displayed in the popup
	 */
	public IncomingDrinEvent(Object source, String title, String message, byte[] imageData) {
		super(source);
		this.title = title;
		this.message = message;
		this.imageData = imageData;
	}
	
	public IncomingDrinEvent(Object source, String title, String message) {
		this(source, title, message, null);
	}
	
	
}
