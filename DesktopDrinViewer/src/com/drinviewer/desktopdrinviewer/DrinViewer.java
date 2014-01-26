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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
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

	public static void main(String[] args) {
		// if an argument is passed, it's the comm port
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (i == 0)
					port = Integer.parseInt(args[i]);
			}
		}
		
		final Display display = new Display();
		final Shell shell = new Shell(display);
		final Menu menu = new Menu(shell, SWT.POP_UP);

		// icon image for the system tray
		Image highlightImage = new Image(display, 16, 16);
		Image normalImage = new Image(display, 16, 16);
		GC gc = new GC(normalImage);
		gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		gc.fillRectangle(normalImage.getBounds());
		gc.dispose();
		final Tray tray = display.getSystemTray();
		
		/**
		 * THE SERVER OBJECT
		 */
		final DesktopServer ds = new DesktopServer();
		try {
			ds.startServer();
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
			item.setToolTipText(Constants.APPNAME+" "+Constants.APPVERSION);

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
			startServer.setEnabled(false);
			startServer.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					try {
						ds.startServer();
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
			stopServer.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					ds.stopServer();
					stopServer.setEnabled(false);
					startServer.setEnabled(true);
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
			item.setImage(normalImage);
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
		display.dispose();
	}
	
	/**
	 * handle server is already running or port in
	 * use error by displaying a dialog to the user
	 * 
	 * @param shell 
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
}
