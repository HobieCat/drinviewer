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
package com.drinviewer.desktopdrinviewer;

import java.util.ResourceBundle;

import com.drinviewer.common.Constants;

/**
 * Desktop app own constants
 * @author giorgio
 *
 */
public class DesktopDrinViewerConstants extends Constants {
	// the application ResourceBundle messages, for i18n purposes
	// Locale.getDefault() is implicitly used by the ResourceBundle
	 public static final ResourceBundle i18nMessages = ResourceBundle.getBundle("DrinViewerUI");
	 
	 // preferences string to tell if the server is running
	 public static final String PREFS_SERVER = "serverWasRunning";
	 // preferences string to get and set popup coordinates on screen
	 public static final String PREFS_POPUP_X = "popUpX";
	 public static final String PREFS_POPUP_Y = "popUpY";
}
