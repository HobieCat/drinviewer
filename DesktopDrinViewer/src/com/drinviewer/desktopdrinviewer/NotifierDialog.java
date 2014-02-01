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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
    // how long the the tray popup is displayed after fading in (in milliseconds)
    private final int   DISPLAY_TIME  = 4500;
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

	public void notify(String title, String message, byte[] imageData) {

		// hidden shell used to hide the popup to appear as a desktop window
		Shell _hiddenShell = new Shell(_display, SWT.NO_FOCUS | SWT.NO_TRIM);
		
        _shell = new Shell(_hiddenShell, SWT.NO_FOCUS | SWT.NO_TRIM | SWT.ON_TOP);
        _shell.setLayout(new FillLayout());
        _shell.setForeground(_fgColor);
        _shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
        
        _shell.addListener(SWT.Dispose, new Listener() {
            @Override
            public void handleEvent(Event event) {
                _activeShells.remove(_shell);
            }
        });

        final Composite inner = new Composite(_shell, SWT.NONE);

        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = MARGIN;
        gl.marginWidth = MARGIN;

        inner.setLayout(gl);
        _shell.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event e) {
                try {
                    // get the size of the drawing area
                    Rectangle rect = _shell.getClientArea();
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
                    _shell.setBackgroundImage(newImage);
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
        FontData fd = titleLabel.getFont().getFontData()[0];
        fd.setStyle(SWT.BOLD);
        fd.height = 11;
        titleLabel.setFont(new Font(Display.getDefault(), fd));
        
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
        FontData tfd = text.getFont().getFontData()[0];
        tfd.setStyle(SWT.BOLD);
        tfd.height = 8;
        text.setFont(new Font(Display.getDefault(), tfd));
        GridData gd = new GridData(GridData.FILL_BOTH);
        text.setLayoutData(gd);
        text.setForeground(_fgColor);
        text.setText(message);

        if (_display == null) { return; }
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
        _shell.setSize(popupWidth, minHeight);

        //TODO: if no popup coordinates are set in the preferences
        // set popup start coordinates
        int startX = clientArea.x + clientArea.width - popupWidth - 1;
        // 650 is an arbitrary Y
        int startY = 650 + (_activeShells.size() * minHeight);
        
        boolean forceMoveUp = false;
        
        // if the bottom of the popup will fall below the screen...
        if (startY + minHeight >= clientArea.height) {
        	if (!_activeShells.isEmpty()) {
        		// if there is at least one poup, place the new one
        		// at the same y of the last popup
        		startY = _activeShells.get(_activeShells.size()-1).getLocation().y;
            	forceMoveUp = true;
        	} else {
        		// if there are no popups, place the
        		// new one at the very bottom of the screen
        		startY = clientArea.y + clientArea.height - minHeight;
        	}
        	
        }
        
        // move other shells up
        if (!_activeShells.isEmpty() && forceMoveUp) {
            List<Shell> modifiable = new ArrayList<Shell>(_activeShells);
            Collections.reverse(modifiable);
            for (Shell shell : modifiable) {
                Point curLoc = shell.getLocation();
                shell.setLocation(curLoc.x, curLoc.y - minHeight - BORDER);
                if (curLoc.y - minHeight - BORDER < 0) {
                    _activeShells.remove(shell);
                    shell.dispose();
                }
            }
        }

        _shell.setLocation(startX, startY);
        _shell.setAlpha(0);
        _shell.setVisible(true);
        _activeShells.add(_shell);
        fadeIn(_shell);
    }
    
    private void fadeIn(final Shell _shell) {
        Runnable run = new Runnable() {

            @Override
            public void run() {
                try {
                    if (_shell == null || _shell.isDisposed()) { return; }

                    int cur = _shell.getAlpha();
                    cur += FADE_IN_STEP;

                    if (cur > FINAL_ALPHA) {
                        _shell.setAlpha(FINAL_ALPHA);
                        startTimer(_shell);
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

    private void startTimer(final Shell _shell) {
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
        Display.getDefault().timerExec(DISPLAY_TIME, run);

    }

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
                        
                        System.out.println("Removing active shell");
                        _activeShells.remove(_shell);
                        _shell.dispose();
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
}
