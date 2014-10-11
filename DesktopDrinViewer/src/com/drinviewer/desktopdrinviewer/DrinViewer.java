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
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FontDialog;
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
	private final Preferences prefs = DrinViewer.getPreferences();
	
	/**
	 * SWT display object
	 */
	private Display display;
	
	/**
	 * SWT shell object
	 */
	private Shell shell;

	public void runApplication(String[] args) {
		// if an argument is passed, it's the comm port
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (i == 0)
					port = Integer.parseInt(args[i]);
			}
		}
		
		boolean serverWasRunning = prefs.getBoolean(DesktopDrinViewerConstants.PREFS_SERVER, true);
		boolean doNotDisturbMode = prefs.getBoolean(DesktopDrinViewerConstants.PREFS_DONOTDISTURB, false);
		
		Display.setAppName(Constants.APPNAME);
		Display.setAppVersion(Constants.APPVERSION);		
		
		this.display = Display.getDefault();
		this.shell = new Shell(display);
		final Menu menu = new Menu(shell, SWT.POP_UP);

		// icon image for the system tray
		final Image normalImage = new Image(display, new DrinImageLoader("icon.png").data[0]);
		final Image disabledImage = new Image(display, new DrinImageLoader("icon-off.png").data[0]);
		final Image highlightImage = normalImage;

		final Tray tray = display.getSystemTray();
		
		/**
		 * OSX platform specific initialization for the about menu
		 */
        if (System.getProperty("os.name").toUpperCase().contains("MAC")) {
        	/**
        	 * Listener for quit action
        	 */
        	Listener quitListener = new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					shell.dispose();
				}
			};
			/**
			 * Listener for about action
			 */
			Listener aboutAction = new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					// create dialog with OK button and information icon
					MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK );
					dialog.setText(Constants.APPNAME+" v"+Constants.APPVERSION);
					dialog.setMessage(DesktopDrinViewerConstants.i18nMessages.getString("abouttext"));
					dialog.open(); 
				}
			};
			/**
			 * Listener for preferences action
			 */
			Listener preferencesAction = new Listener() {
				@Override
				public void handleEvent(Event arg0) {
				}
			};
			
			// pass it all to the CocoaUIEnhancer
        	new CocoaUIEnhancer(Constants.APPNAME).hookApplicationMenu(display, quitListener, aboutAction, preferencesAction);			
        } // end osx platform specific stuff
		
		/**
		 * THE SERVER OBJECT
		 */
		final DesktopServer ds = new DesktopServer();
		try {
			if (serverWasRunning) ds.startServer();
		} catch (BindException e) {
			alreadyRunningError(shell);
		}
		
		/**
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
				    	if (event.action == Constants.SHOW_POPUP) {
				    		ndlg.notify(event.title, event.message, event.imageData, false);
				    	} else if (event.action == Constants.SHOW_PAIRED) {
				    		ndlg.notify(event.title, event.message, event.imageData, true);
				    	} else if (event.action == Constants.REMOVE_POPUP) {
				    		ndlg.removeNotify(event.title, event.message);
				    	}
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
			 *  start server, stop server and donotdisturb menu item definition
			 */
			final MenuItem startServer = new MenuItem(menu, SWT.PUSH);
			final MenuItem stopServer = new MenuItem(menu, SWT.PUSH);
			final MenuItem doNotDisturb = new MenuItem(menu, SWT.CHECK);
			
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
						try {
							prefs.flush();
						} catch (BackingStoreException e) {
							e.printStackTrace();
						}
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
					try {
						prefs.flush();
					} catch (BackingStoreException e) {
						e.printStackTrace();
					}
					item.setImage(disabledImage);
					item.setToolTipText(baseToolTipText+" - "+DesktopDrinViewerConstants.i18nMessages.getString("disabled"));
					stopServer.setEnabled(false);
					startServer.setEnabled(true);
				}
			});
			
			/**
			 * do not disturb item implementation
			 */
			doNotDisturb.setText(DesktopDrinViewerConstants.i18nMessages.getString("donotdisturb"));
			doNotDisturb.setSelection(doNotDisturbMode);
			ds.setDoNotDisturbMode(doNotDisturb.getSelection());
			doNotDisturb.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					prefs.putBoolean(DesktopDrinViewerConstants.PREFS_DONOTDISTURB, doNotDisturb.getSelection());
					ds.setDoNotDisturbMode(doNotDisturb.getSelection());
				}
			});
			
			/**
			 * settings menu item
			 */
			MenuItem settingsMenu = new MenuItem(menu, SWT.CASCADE);
			settingsMenu.setText(DesktopDrinViewerConstants.i18nMessages.getString("settings"));
			
			/**
			 * settings submenu
			 */
			Menu settingsSubMenu = new Menu (menu);
			settingsMenu.setMenu (settingsSubMenu);
			
			/**
			 * settings/popupposition menu item 
			 */
			MenuItem popupPosition = new MenuItem (settingsSubMenu, SWT.PUSH);
			popupPosition.setText(DesktopDrinViewerConstants.i18nMessages.getString("setpopupposition"));
			popupPosition.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					display.asyncExec(new Runnable() {
						@Override						
					    public void run() {
					    	ndlg.showToSetPosition(DesktopDrinViewerConstants.i18nMessages.getString("popuptitle"),
					    			DesktopDrinViewerConstants.i18nMessages.getString("popuptext"), null);
					    }
					});
				}
			});
			
			/**
			 * settings/manage paired devices menu item
			 */
			MenuItem managePaired = new MenuItem(settingsSubMenu, SWT.PUSH);
			managePaired.setText(DesktopDrinViewerConstants.i18nMessages.getString("managepaired"));
			managePaired.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					ManagePairedDialog mpd = new ManagePairedDialog(shell);
					mpd.open();
				}
			});
			
			/**
			 * settings/select font menu item
			 */
			MenuItem fontMenu = new MenuItem(settingsSubMenu, SWT.CASCADE);
			fontMenu.setText(DesktopDrinViewerConstants.i18nMessages.getString("selectfont"));
			
			/**
			 * settings/select font submenu
			 */
			Menu fontSubMenu = new Menu (settingsSubMenu);
			fontMenu.setMenu (fontSubMenu);
			
			/**
			 * settings/select font/select title font menu item 
			 */
			MenuItem selectTitleFont = new MenuItem (fontSubMenu, SWT.PUSH);
			selectTitleFont.setText(DesktopDrinViewerConstants.i18nMessages.getString("selecttitlefont"));
			selectTitleFont.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					selectAndSaveFont(true);
				}
			});
			
			/**
			 * settings/select font/select text title font menu item 
			 */
			MenuItem selectTextFont = new MenuItem(fontSubMenu, SWT.PUSH);
			selectTextFont.setText(DesktopDrinViewerConstants.i18nMessages.getString("selecttextfont"));
			selectTextFont.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					selectAndSaveFont(false);
				}
			});
			
			/**
			 * test notification menu item
			 */
			MenuItem testNotify = new MenuItem(menu, SWT.PUSH);
			testNotify.setText(DesktopDrinViewerConstants.i18nMessages.getString("testnotify"));
			testNotify.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					display.asyncExec(new Runnable() {
					    public void run() {
					    	 ndlg.notify(DesktopDrinViewerConstants.i18nMessages.getString("testnotify"), "+39-666-666-66-66",null, true);
					    }
					});
				}
			});
			
			// separator before website and exit menu item
			new MenuItem(menu, SWT.SEPARATOR);
			
			/**
			 * openwebsite menu item
			 */
			MenuItem openwebsite = new MenuItem(menu, SWT.PUSH);
			openwebsite.setText(DesktopDrinViewerConstants.i18nMessages.getString("openwebsite"));
			openwebsite.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					org.eclipse.swt.program.Program.launch(Constants.APPURL);
				}
			});
			
			
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
	 * shows font dialog and save user choice to preferences
	 * 
	 * @param isTitle true if must set font for title, false for text
	 */
	private void selectAndSaveFont (final boolean isTitle) {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				FontDialog fd = new FontDialog(shell);
		        fd.setText(DesktopDrinViewerConstants.i18nMessages.getString("selectfont"));
		        fd.setRGB(new RGB(0, 0, 0));
		        fd.setFontList(DrinViewer.getNotificationFont(display, isTitle));
		        if (fd.open() != null) {
		        	// save font
		    		String namePref  = (isTitle) ? DesktopDrinViewerConstants.PREFS_TITLEFONTNAME : 
						   DesktopDrinViewerConstants.PREFS_TEXTFONTNAME;
		    		String sizePref  = (isTitle) ? DesktopDrinViewerConstants.PREFS_TITLEFONTSIZE :
						   DesktopDrinViewerConstants.PREFS_TEXTFONTSIZE;
		    		String stylePref = (isTitle) ? DesktopDrinViewerConstants.PREFS_TITLEFONTSTYLE :
						   DesktopDrinViewerConstants.PREFS_TEXTFONTSTYLE;
		    		
		    		prefs.put(namePref, fd.getFontList()[0].getName());
		    		prefs.putInt(sizePref, fd.getFontList()[0].getHeight());
		    		prefs.putInt(stylePref, fd.getFontList()[0].getStyle());
		    		try {
						prefs.flush();
					} catch (BackingStoreException e) {
						e.printStackTrace();
					}
		        }
			}
		});		
	}

	/**
	 * get font for notification
	 * 
	 * @param display display in use
	 * @param isTitle true if must get font for title, false for text
	 * 
	 * @return FontData
	 */
	public static FontData[] getNotificationFont(Display display, boolean isTitle) {
		Preferences prefs = DrinViewer.getPreferences();
		FontData fd = display.getSystemFont().getFontData()[0];
		
		String namePref  = (isTitle) ? DesktopDrinViewerConstants.PREFS_TITLEFONTNAME : 
									   DesktopDrinViewerConstants.PREFS_TEXTFONTNAME;
		String sizePref  = (isTitle) ? DesktopDrinViewerConstants.PREFS_TITLEFONTSIZE :
									   DesktopDrinViewerConstants.PREFS_TEXTFONTSIZE;
		String stylePref = (isTitle) ? DesktopDrinViewerConstants.PREFS_TITLEFONTSTYLE :
									   DesktopDrinViewerConstants.PREFS_TEXTFONTSTYLE;
		
		String fontName = prefs.get(namePref, fd.getName());
		int fontSize = prefs.getInt(sizePref, fd.getHeight());
		int fontStyle = prefs.getInt(stylePref, (isTitle) ? SWT.BOLD : fd.getStyle());

		return (new Font(display, fontName, fontSize, fontStyle)).getFontData(); 
	}
	
	/**
	 * get application Preferences object
	 * 
	 * @return Preferences
	 */
	public static Preferences getPreferences() {		
		// be a nice guy and save preferences to a nice location:
		// com/drinviewer/desktopdrinviewer/DrinViewer
		Preferences prefs = Preferences.userRoot();		
		StringTokenizer strTkn = new StringTokenizer(DrinViewer.class.getName(), ".");
		while(strTkn.hasMoreTokens()) {
			prefs = prefs.node(strTkn.nextToken());
		}		
		return prefs;
	}
	
	/**
	 * handle server is already running or port in
	 * use error by displaying a dialog to the user
	 */
	private static void alreadyRunningError(Shell shell) {
		
		MessageFormat formatter = new MessageFormat(DesktopDrinViewerConstants.i18nMessages.getString("alreadyrunning"));
		
		String output = formatter.format(
				new Object[] {
						Integer.toString(port)
				});
		
		// create dialog with OK button and error icon
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
