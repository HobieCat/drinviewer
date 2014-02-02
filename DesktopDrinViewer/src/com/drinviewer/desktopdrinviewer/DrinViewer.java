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
import java.text.MessageFormat;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import com.drinviewer.common.Constants;
import com.drinviewer.common.IncomingDrinEvent;

/**
 * DrinViewer main method
 * 
 * @author giorgio
 *
 */
public class DrinViewer {
	/**
	 * port used for communication, if not passed from the command line, use the default
	 */
	private static int port = Constants.PORT;
	
	/**
	 * Preferences object used to store:
	 * - status of the server (running or not)
	 * - position of the popup defined by user
	 * 
	 */
	private Preferences prefs;

	public void runApplication(String[] args) {
		// if an argument is passed, it's the comm port
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (i == 0)
					port = Integer.parseInt(args[i]);
			}
		}
		
		prefs = Preferences.userRoot().node(this.getClass().getName());
		boolean serverWasRunning = prefs.getBoolean(DesktopDrinViewerConstants.PREFS_SERVER, true);
		
		final Display display = new Display();
		final Shell shell = new Shell(display);
		final Menu menu = new Menu(shell, SWT.POP_UP);

		// icon image for the system tray
		final Image normalImage = new Image(display, new DrinImageLoader("icon.png").data[0]);
		final Image disabledImage = new Image(display, new DrinImageLoader("icon-off.png").data[0]);
		final Image highlightImage = normalImage;

		final Tray tray = display.getSystemTray();
		
		/**
		 * THE SERVER OBJECT
		 */
		final DesktopServer ds = new DesktopServer();
		try {
			if (serverWasRunning) ds.startServer();
		} catch (BindException e) {
			alreadyRunningError(shell);
		}
		
		/*
		 * THE NOTIFIER (aka POPUP) DIALOG
		 */
		final NotifierDialog ndlg = new NotifierDialog(display);
		
		/**
		 * add the DesktopServer event listener, this
		 * actually cause the popup to be displayed
		 */
		ds.addListener(new IncomingDrinListener() {
			@Override
			public void handleDrin(final IncomingDrinEvent event) {
				display.asyncExec(new Runnable() {
				    public void run() {
				    	ndlg.notify(event.title, event.message, event.imageData);
				    }
				});
			}
		});
		
		if (tray == null) {
			System.out.println(DesktopDrinViewerConstants.i18nMessages.getString("trayerror"));
		} else {
			final TrayItem item = new TrayItem(tray, SWT.NONE);
			final String baseToolTipText = Constants.APPNAME+" "+Constants.APPVERSION; 
			item.setToolTipText(baseToolTipText+" - " + 
					((serverWasRunning) ? 
							DesktopDrinViewerConstants.i18nMessages.getString("enabled") : 
							DesktopDrinViewerConstants.i18nMessages.getString("disabled")));

			/**
			 * selection event listener
			 */
			item.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					menu.setVisible(true);
				}
			});

			/**
			 * menudetect event listener
			 */
			item.addListener(SWT.MenuDetect, new Listener() {
				@Override
				public void handleEvent(Event event) {
					menu.setVisible(true);
				}
			});

			/**
			 *  start server and stop server menu item definition
			 */
			final MenuItem startServer = new MenuItem(menu, SWT.PUSH);
			final MenuItem stopServer = new MenuItem(menu, SWT.PUSH);
			
			/**
			 * start server menu item implementation
			 */
			startServer.setText(DesktopDrinViewerConstants.i18nMessages.getString("startserver"));
			startServer.setEnabled(!serverWasRunning);
			startServer.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					try {						
						ds.startServer();
						prefs.putBoolean(DesktopDrinViewerConstants.PREFS_SERVER, true);
						item.setImage(normalImage);
						item.setToolTipText(baseToolTipText+" - "+DesktopDrinViewerConstants.i18nMessages.getString("enabled"));
					} catch (BindException e) {
						alreadyRunningError(shell);
					}
					startServer.setEnabled(false);
					stopServer.setEnabled(true);
				}
			});
			
			/**
			 * stop server menu item implementation
			 */
			stopServer.setText(DesktopDrinViewerConstants.i18nMessages.getString("stopserver"));
			stopServer.setEnabled(serverWasRunning);			
			stopServer.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					ds.stopServer();
					prefs.putBoolean(DesktopDrinViewerConstants.PREFS_SERVER, false);
					item.setImage(disabledImage);
					item.setToolTipText(baseToolTipText+" - "+DesktopDrinViewerConstants.i18nMessages.getString("disabled"));
					stopServer.setEnabled(false);
					startServer.setEnabled(true);
				}
			});
			
			/**
			 * set popup position menu item
			 */
			MenuItem popupPosition = new MenuItem(menu, SWT.PUSH);
			popupPosition.setText(DesktopDrinViewerConstants.i18nMessages.getString("setpopupposition"));
			popupPosition.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					display.asyncExec(new Runnable() {
					    public void run() {
					    	ndlg.showToSetPosition(DesktopDrinViewerConstants.i18nMessages.getString("popuptitle"),
					    			DesktopDrinViewerConstants.i18nMessages.getString("popuptext"), null);					    	
					    }
					});
				}
			});
			
			/**
			 * test notification menu item
			 */
			MenuItem testNotify = new MenuItem(menu, SWT.PUSH);
			testNotify.setText("Test Notification");
			testNotify.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					display.asyncExec(new Runnable() {
					    public void run() {
					    	 ndlg.notify("Incoming Call", "Test Number:\n+39-666-666-66-66",null);
					    }
					});
				}
			});
			
			// separator before exit
			new MenuItem(menu, SWT.SEPARATOR);
			
			/**
			 * exit menu item
			 */
			MenuItem exit = new MenuItem(menu, SWT.PUSH);
			exit.setText(DesktopDrinViewerConstants.i18nMessages.getString("exit"));
			exit.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					shell.dispose();
				}
			});

			/**
			 * set the icon images for the systray item
			 */
			if (serverWasRunning) item.setImage(normalImage);
			else item.setImage(disabledImage);
			
			item.setHighlightImage(highlightImage);
		}

		/**
		 * wait for something to happen till the shell is there
		 */
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		
		/**
		 * dispose unneeded stuff
		 */
		highlightImage.dispose();
		normalImage.dispose();
		disabledImage.dispose();
		display.dispose();
	}
	
	/**
	 * handle server is already running or port in
	 * use error by displaying a dialog to the user
	 * 
	 */
	private static void alreadyRunningError(Shell shell) {
		
		MessageFormat formatter = new MessageFormat(DesktopDrinViewerConstants.i18nMessages.getString("alreadyrunning"));
		
		String output = formatter.format(
				new Object[] {
						Integer.toString(port)
				});
		
		// create dialog with ok button and error icon
		MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK );
		dialog.setText(Constants.APPNAME+" v"+Constants.APPVERSION);
		dialog.setMessage(output);
		dialog.open(); 
		System.exit(-1);		
	}
	
	public static void main(String[] args) {
		new DrinViewer().runApplication(args);		
	}
}
