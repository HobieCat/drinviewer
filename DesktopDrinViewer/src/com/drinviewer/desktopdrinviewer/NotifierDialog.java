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
 * THIS IS AN IMPLEMENTATION (almost copy & paste)
 * OF THE POPUP NOTIFICATION WIDGET PROPOSED AT:
 * http://hexapixel.com/2009/06/30/creating-a-notification-popup-widget
 * 
 */
package com.drinviewer.desktopdrinviewer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.drinviewer.common.Constants;

/**
 * Class to build dialog notification popup,
 * implementation of http://hexapixel.com/2009/06/30/creating-a-notification-popup-widget
 * 
 * @author giorgio
 *
 */
public class NotifierDialog {
    // how long the tray popup is displayed after fading in (in milliseconds)
    private final int   DISPLAY_TIME  = 60000;
 // how long the tray TEST popup is displayed after fading in (in milliseconds)
    private final int   TEST_DISPLAY_TIME  = 4500;    
    // how long each tick is when fading in (in ms)
    private final int   FADE_TIMER    = 50;
    // how long each tick is when fading out (in ms)
    private final int   FADE_IN_STEP  = 30;
    // how many tick steps we use when fading out 
    private final int   FADE_OUT_STEP = 28;

    // how high the alpha value is when we have finished fading in 
    private final int   FINAL_ALPHA   = 225;
    // default minimum popup width
    private final int   WIDTH = 250;
    // popup border
    private final int   BORDER = 2;
    // popup inner margin
    private final int   MARGIN = 5;

    // title foreground color
    private Color       _titleFgColor = new Color(Display.getCurrent(), new RGB(40, 73, 97)); 
    // text foreground color
    private Color       _fgColor      = _titleFgColor;

    // shell gradient background color - top
    private Color       _bgFgGradient = new Color(Display.getCurrent(), new RGB(226, 239, 249));
    // shell gradient background color - bottom    
    private Color       _bgBgGradient = new Color(Display.getCurrent(), new RGB(177, 211, 243));
    // shell border color
    private Color       _borderColor  = new Color(Display.getCurrent(), new RGB(40, 73, 97));
    
    // contains list of all active popup shells
    private List<Shell> _activeShells;

    private Shell   _shell;
    private Display _display;
    
    public NotifierDialog(Display _display) {
		this._display = _display;
		_activeShells = new ArrayList<Shell>();
	}
    
    /**
     * @return number of attached monitors
     */
    private int getMonitorCount() {
    	return _display.getMonitors().length-1;
    }
    
    /**
     * builds the popup to be displayed
     * 
     * @param title title of the popup
     * @param message message of the popup
     * @param imageData image of the popup as a byte array
     * 
     * @return the builded popup, just set its position and display it
     */
    
    private Shell buildPopUp(final String title, final String message, byte[] imageData) {
    	// hidden shell used to hide the popup to appear as a desktop window
		Shell _hiddenShell = new Shell(_display, SWT.NO_FOCUS | SWT.NO_TRIM);
		
        final Shell popUpShell = new Shell(_hiddenShell, SWT.NO_FOCUS | SWT.NO_TRIM | SWT.ON_TOP);    	        
        popUpShell.setLayout(new FillLayout());
        popUpShell.setForeground(_fgColor);
        popUpShell.setBackgroundMode(SWT.INHERIT_DEFAULT);
        
        popUpShell.addListener(SWT.Dispose, new Listener() {
            @Override
            public void handleEvent(Event event) {
                _activeShells.remove(popUpShell);
            }
        });

        final Composite inner = new Composite(popUpShell, SWT.NONE);

        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = MARGIN;
        gl.marginWidth = MARGIN;

        inner.setLayout(gl);
        popUpShell.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event e) {
                try {
                    // get the size of the drawing area
                    Rectangle rect = popUpShell.getClientArea();
                    // create a new image with that size
                    Image newImage = new Image(Display.getDefault(), Math.max(1, rect.width), rect.height);
                    // create a GC object we can use to draw with
                    GC gc = new GC(newImage);
                    gc.setAdvanced(true);
                    gc.setAntialias(SWT.ON);
                    // fill background
                    gc.setForeground(_bgFgGradient);
                    gc.setBackground(_bgBgGradient);
                    gc.fillGradientRectangle(rect.x, rect.y, rect.width, rect.height, true);
                    // draw shell edge
                    gc.setLineWidth(BORDER);
                    gc.setForeground(_borderColor);
                    gc.drawRectangle(rect.x + 1, rect.y + 1, rect.width - BORDER, rect.height - BORDER);
                    // remember to dispose the GC object!
                    gc.dispose();
                    // now set the background image on the shell
                    popUpShell.setBackgroundImage(newImage);
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        });

        // add the image to the popup
        CLabel imgLabel = new CLabel(inner, SWT.NONE);
        GridData glimg = new GridData(GridData.VERTICAL_ALIGN_CENTER);
        glimg.verticalSpan = 2;
        imgLabel.setLayoutData(glimg);

        // loads the scaled version of the default icon image if the passed one is null
        if (imageData == null)
        { 
        	imageData = new DrinImageLoader("icon.png").getScaled(Constants.ICON_SIZE);
        }        
        // and put it in the popup
        imgLabel.setImage( new Image (_display, new ImageData(new ByteArrayInputStream(imageData))) );
        
        // minimum popup height is the height of the image
        int minHeight = Constants.ICON_SIZE+(MARGIN*2)+(BORDER*2); // img. height + margin *2 + border *2

        // add title to the popup
        CLabel titleLabel = new CLabel(inner, SWT.NONE);
        titleLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        titleLabel.setText(title);
        titleLabel.setForeground(_titleFgColor);
        titleLabel.setTopMargin(15);
        // Set title font
        titleLabel.setFont(new Font(_display, DrinViewer.getNotificationFont(_display, true)));
        
        // compute popup width basing on how long the title is
        // adding an extra "X" char for safety...
        GC labelGC = new GC(titleLabel);
        int popupWidth = labelGC.textExtent(title+"X").x +
        				 Constants.ICON_SIZE+(MARGIN*2)+(BORDER*2)+
        				 titleLabel.getRightMargin()+titleLabel.getLeftMargin()+
        				 imgLabel.getLeftMargin()+imgLabel.getRightMargin();
        labelGC.dispose();

        // add text to the popup
        Label text = new Label(inner, SWT.WRAP);
        // set text font
        text.setFont(new Font(_display, DrinViewer.getNotificationFont(_display, false)));
        GridData gd = new GridData(GridData.FILL_BOTH);
        text.setLayoutData(gd);
        text.setForeground(_fgColor);
        text.setText(message);

        if (_display == null) { return null; }
        Rectangle clientArea = _display.getClientArea();

        // fix the popup width
        if (popupWidth < WIDTH) popupWidth = WIDTH;
        else if (popupWidth > clientArea.x + clientArea.width) popupWidth = clientArea.x + clientArea.width - 1 ;
        
        // fix popup height
        int titleHeight = titleLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        int messageHeight = text.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;        
        if (titleHeight + messageHeight > minHeight) {
        	minHeight = titleHeight + messageHeight +(MARGIN*2)+(BORDER*2)*4;
        }
        
        // set popup size
        popUpShell.setSize(popupWidth, minHeight);
        // set popup text hidden because of the layout but useful
        // to discover the popup to remove in removeNotify method
        popUpShell.setText(message);
        
        // add onClick listener to all children to
        // remove notification on single mouse click
        propagateListenerToChildren(popUpShell, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.type == SWT.MouseUp) {
					removeNotify(title, message);
				}
			}        	
        });
        
        return popUpShell;
    	
    }

    /**
     * gets the popup coordinates stored in the shared preferences
     * 
     * @return the retrieved point or null if none is found
     */
    private Point getPositionFromPrefs() {
    	Preferences prefs = DrinViewer.getPreferences();
    	int x=-1, y=-1;
    	
    	x = prefs.getInt(DesktopDrinViewerConstants.PREFS_POPUP_X+getMonitorCount(), -1);
    	y = prefs.getInt(DesktopDrinViewerConstants.PREFS_POPUP_Y+getMonitorCount(), -1);
    	
    	// if still no x and y, get default (aka primary) monitor ones
    	if (x==-1 && y==-1) {
        	x = prefs.getInt(DesktopDrinViewerConstants.PREFS_POPUP_X, -1);
        	y = prefs.getInt(DesktopDrinViewerConstants.PREFS_POPUP_Y, -1);
    	}
    	
    	if (x!=-1 && y!=-1) {
    		return new Point(x,y);
    	} else {
    		return null;
    	}
    }

    /**
     * sets the popup coordinates in the shared preferences
     * @param popup 
     * 
     * @param position the point to set
     */
    private void setPositionInPrefs(Shell popup, Point position) {
    	Preferences prefs = DrinViewer.getPreferences();
    	
    	if (position.x < 0) position.x = 0;
    	if (position.y < 0) position.y = 0;
    	prefs.putInt(DesktopDrinViewerConstants.PREFS_POPUP_X+getMonitorCount(), position.x);
    	prefs.putInt(DesktopDrinViewerConstants.PREFS_POPUP_Y+getMonitorCount(), position.y);
    	try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * shows a popup to let the user choose its position
     * 
     * @param title title of the popup
     * @param message message of the popup
     * @param imageData image of the popup as a byte array
     */
    public void showToSetPosition (String title, String message, byte[] imageData) {
		// get a popup
		final Shell popup = buildPopUp(title, message, imageData);
		// this popup goes at bottom right of primary monitor if no preferences
		Rectangle clientArea = _display.getMonitors()[getMonitorCount()].getClientArea();
		
        // set popup start coordinates
		int startX;
		int startY;
		Point popUpPos = getPositionFromPrefs();
		
		if (popUpPos != null) {
			startX = popUpPos.x;
			startY = popUpPos.y;
		} else {
			startX = clientArea.x + clientArea.width - popup.getSize().x - 1;
	        startY = (clientArea.height - popup.getSize().y - 1) + (_activeShells.size() * popup.getSize().y);			
		}
		popup.setLocation(startX, startY);

		// add move shell around and save position on double click
		Listener l = new Listener() {
			Point origin;
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.MouseDown:
					origin = new Point(e.x, e.y);
					break;
				case SWT.MouseUp:
					origin = null;
					break;
				case SWT.MouseMove:
					if (origin != null) {
						Point p = _display.map(popup, null, e.x, e.y);
						popup.setLocation(p.x - origin.x, p.y - origin.y);
					}
					break;
				case SWT.MouseDoubleClick:
					Point p = _display.map(popup, null, e.x, e.y);
					setPositionInPrefs(popup, new Point(p.x - origin.x, p.y - origin.y));
					popup.close();				
					break;
				}
			}
		};

		propagateListenerToChildren(popup, l);
		popup.open();
		popup.setAlpha(FINAL_ALPHA);
		popup.setVisible(true);
    }
    
    /**
     * show the DrinViewer popup, disappearing after
     * DISPLAY_TIME secs with fadeIn and fadeOut effect
     * 
     * @param title title of the popup
     * @param message message of the popup
     * @param imageData image of the popup as a byte array
     * @param isTestNotify true if is the test notification popup
     * 
     */
	public void notify(String title, String message, byte[] imageData, boolean isTestNotify) {

		// get the popup
		_shell = buildPopUp(title, message, imageData);
		
		if (_display == null || _shell == null) { return ; }
		
        Rectangle clientArea = _display.getClientArea();	
		int popUpHeight = _shell.getSize().y;
		int popUpWidth  = _shell.getSize().x;
		
        // set popup start coordinates
		int startX;
		int startY;
		Point popUpPos = getPositionFromPrefs();
		
		if (popUpPos != null) {
			startX = popUpPos.x;
			startY = popUpPos.y;
		} else {
			startX = clientArea.x + clientArea.width - popUpWidth - 1;
	        startY = (clientArea.height - popUpHeight - 1);			
		}
		
		startY += (_activeShells.size() * popUpHeight);
        
        boolean forceMoveUp = false;

        // if the bottom of the popup will fall outside the screen...
        // fix it so that it'll be fully visible
        if (startY + popUpHeight >= clientArea.height) {
        	if (!_activeShells.isEmpty()) {
        		// if there is at least one popup, place the new one
        		// at the same y of the last popup
        		startY = _activeShells.get(_activeShells.size()-1).getLocation().y;
            	forceMoveUp = true;
        	} else {
        		// if there are no popups, place the
        		// new one at the very bottom of the screen
        		startY = clientArea.y + clientArea.height - popUpHeight;
        	}
        }
        
        // if the right of the popup will fall outside the screen...
        // fix it so that it'll be fully visible
        if (startX + popUpWidth >= clientArea.width) {
        	startX = clientArea.x + clientArea.width - popUpWidth - 1;
        }
        
        // move other shells up
        if (!_activeShells.isEmpty() && forceMoveUp) {
            List<Shell> modifiable = new ArrayList<Shell>(_activeShells);
            Collections.reverse(modifiable);
            for (Shell shell : modifiable) {
                Point curLoc = shell.getLocation();
                shell.setLocation(curLoc.x, curLoc.y - popUpHeight - BORDER);
                if (curLoc.y - popUpHeight - BORDER < 0) {
                    _activeShells.remove(shell);
                    shell.dispose();
                }
            }
        }

        _shell.setLocation(startX, startY);
        _shell.setAlpha(0);
        _shell.setVisible(true);
        _activeShells.add(_shell);
        fadeIn(_shell, isTestNotify);
    }
    
	/**
	 * fades in the popup notificaition
	 * 
	 * @param _shell the popup to be faded
	 * @param isTestNotify true if is the test notification popup
	 */
    private void fadeIn(final Shell _shell, final boolean isTestNotify) {
        Runnable run = new Runnable() {

            @Override
            public void run() {
                try {
                    if (_shell == null || _shell.isDisposed()) { return; }

                    int cur = _shell.getAlpha();
                    cur += FADE_IN_STEP;

                    if (cur > FINAL_ALPHA) {
                        _shell.setAlpha(FINAL_ALPHA);
                        startTimer(_shell, isTestNotify);
                        return;
                    }

                    _shell.setAlpha(cur);
                    Display.getDefault().timerExec(FADE_TIMER, this);
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

        };
        Display.getDefault().timerExec(FADE_TIMER, run);
    }

    /**
     * delays the fadeout on the popup to be removed
     * 
     * @param _shell the popup to be faded
	 * @param isTestNotify true if is the test notification popup
     */
    private void startTimer(final Shell _shell, boolean isTestNotify) {
        Runnable run = new Runnable() {

            @Override
            public void run() {
                try {
                    if (_shell == null || _shell.isDisposed()) { return; }

                    fadeOut(_shell);
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

        };
        Display.getDefault().timerExec(isTestNotify ? TEST_DISPLAY_TIME : DISPLAY_TIME, run);

    }

    /**
     * fades out the popup notification
     * 
     * @param _shell the popup to be faded
     */
    private void fadeOut(final Shell _shell) {
    	
        final Runnable run = new Runnable() {
        	
            @Override
            public void run() {
                try {
                    if (_shell == null || _shell.isDisposed()) { return; }

                    int cur = _shell.getAlpha();
                    cur -= FADE_OUT_STEP;

                    if (cur <= 0) {
                    	_shell.setVisible(false);
                        _shell.setAlpha(0);
                        
                        removeShell(_shell);
                        return;
                    }

                    _shell.setAlpha(cur);
                    
                    if (cur != _shell.getAlpha()) {
                    	/**
                    	 * shell alpha did not change, remove it without fadeOut
                    	 */
                    	removeShell(_shell);
                    } else {
                    	/**
                    	 * go on with fadeOut effect
                    	 */
                    	Display.getDefault().timerExec(FADE_TIMER, this);
                    }

                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

        };
        Display.getDefault().timerExec(FADE_TIMER, run);
    }
    
    /**
     * removes the popup shell from the list of the active shells
     * 
     * @param _shell the shell to be removed
     */
    private void removeShell(final Shell _shell) {
        _activeShells.remove(_shell);
        _shell.dispose();
        return;
    }
    
    /**
     * looks like SWT does not propagate the events,
     * so event listener must be added for each Composite child
     * 
     * @param popup parent popup to receive propagated events
     * @param l Listener to be propagated
     */
    private void propagateListenerToChildren(Shell popup, Listener l) {
		for (Object cc : popup.getChildren()) {
			if (cc instanceof Composite) {
				((Composite) cc).addListener(SWT.MouseDown, l);
				((Composite) cc).addListener(SWT.MouseUp, l);
				((Composite) cc).addListener(SWT.MouseMove, l);
				((Composite) cc).addListener(SWT.MouseDoubleClick, l);
				for (Control child : ((Composite) cc).getChildren()) {
					child.addListener(SWT.MouseDown, l);
					child.addListener(SWT.MouseUp, l);
					child.addListener(SWT.MouseMove, l);
					child.addListener(SWT.MouseDoubleClick, l);
				}
			}
		}
    }

    /**
     * removes the DrinViewer popup, when user answers
     * or rejects the call on the mobile counterpart
     * 
     * @param title title of the popup
     * @param message message of the popup
     * 
     */
	public void removeNotify(String title, String message) {
		if (!_activeShells.isEmpty()) {
			if (message!=null) {
				// look for active shell to be removed
				for (Shell shell : _activeShells) {
					if (shell.getText().equalsIgnoreCase(message)) {
						removeShell (shell);
						break;
					}
				}				
			} else {
				// remove last active shell
	            removeShell(_activeShells.get(_activeShells.size()-1));
			}
		}
	}
}
