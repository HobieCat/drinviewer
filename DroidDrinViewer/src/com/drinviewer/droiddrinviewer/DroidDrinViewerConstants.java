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

import com.drinviewer.common.Constants;

/**
 * Android app own constants
 * @author giorgio
 *
 */
public class DroidDrinViewerConstants extends Constants {

	// Server found message sent to the app handler
	public static final int    MSG_SERVER_FOUND  = 666;
	// Discovery process started message sent to the app handler	
	public static final int    MSG_DISCOVER_START = 667;
	// Discovery process ended message sent to the app handler
	public static final int    MSG_DISCOVER_DONE = 668;
	// Toggle pairing message sent to the app handler	
	public static final int    MSG_SERVER_TOGGLEPAIRED = 669;
	// Collection init sent to the app handler
	public static final int    MSG_COLLECTION_INIT = 700;
	
	// triggers onHostCollectionInit
	public static final int COLLECTION_INIT = 1;
	// triggers onHostDiscoveryStarted
	public static final int DISCOVERY_STARTED = 2;
	// triggers onHostDiscovered
	public static final int HOST_DISCOVERED = 3;
	// triggers onHostDiscoveryDone
	public static final int DISCOVERY_DONE = 4;
	
	// restart discover service every 10 minutes
	public static final long   DISCOVER_REPEAT_TIME = 1000 * 10 * 60;
	// discover maximum timeout
	public static final long   DISCOVERY_MAX_TIMEOUT = 30000; // 30 seconds
}
