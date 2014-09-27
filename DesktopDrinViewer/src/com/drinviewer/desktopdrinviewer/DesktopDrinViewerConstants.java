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

import java.io.File;
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
	 // preferences string to get and set notification font for popup title
	 public static final String PREFS_TITLEFONTNAME = "TitleFontName";
	 public static final String PREFS_TITLEFONTSIZE = "TitleFontSize";
	 public static final String PREFS_TITLEFONTSTYLE = "TitleFontStyle";
	 // preferences string to get and set notification font for popup text
	 public static final String PREFS_TEXTFONTNAME = "TextFontName";
	 public static final String PREFS_TEXTFONTSIZE = "TextFontSize";
	 public static final String PREFS_TEXTFONTSTYLE = "TextFontStyle";
	 // preferences string to tell if donotditrub mode is on or off
	 public static final String PREFS_DONOTDISTURB = "donotdisturb";
	 
	 public static String getAppSaveDir()
	 {
		 String workingDirectory = null;
		 String OS = (System.getProperty("os.name")).toUpperCase();
		//to determine what the workingDirectory is.
		//if it is some version of Windows
		if (OS.contains("WIN"))
		{
		    //it is simply the location of the "AppData" folder
		    workingDirectory = System.getenv("AppData");
		}
		//Otherwise, we assume Linux or Mac
		else
		{
		    //in either case, we would start in the user's home directory
		    workingDirectory = System.getProperty("user.home");
		    if (OS.contains("MAC")) {
			    //if we are on a Mac, we are not done, we look for "Application Support"
			    workingDirectory += "/Library/Application Support";	
		    } else if (OS.contains("LINUX")) {
		    	workingDirectory += "/.local/share";
		    }
		}
		
		if (workingDirectory!=null)
		{
			workingDirectory += "/" + Constants.APPNAME;
			File f = new File(workingDirectory);
			if (!f.exists() && !f.isDirectory()) {
			    if (!new File(workingDirectory).mkdirs()) {
			    	return ".";
			    }
			}
		}		
		return workingDirectory;
	 }
}
