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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

/**
 * SWT ImageLoader extension to load the scaled image
 * either from a resource file or from an InputStream
 * 
 * @author giorgio
 *
 */
public class DrinImageLoader extends ImageLoader {
	
    public DrinImageLoader(String resourceName) {
    	
    	super();
    	ClassLoader classLoader = getClass().getClassLoader();
		InputStream is = classLoader.getResourceAsStream(resourceName);
		if (is == null) {
			// the old way didn't have leading slash, so if we can't find the
			// image stream, let's see if the old way works.
			is = classLoader.getResourceAsStream(resourceName.substring(1));

			if (is == null) {
				is = classLoader.getResourceAsStream(resourceName);
				if (is == null) {
					is = classLoader.getResourceAsStream(resourceName.substring(1));
				}
			}
		}
		
		if (is!=null) load(is);
	}

	public DrinImageLoader(InputStream is) {
		super();
		load (is);
	}
	
	public byte[] getScaled (int longestSidePx) {
		
		ImageData[] imageData = null;
		
		if (data.length > 0) {
			imageData = new ImageData[data.length];
			for (int i=0; i < data.length; i++) {
				
			    // return the image proportionally scaled, having the longest side to longestSidePx
			    if (data[i].height>=data[i].width)
			    	imageData[i] = data[i].scaledTo((int) ((float)data[i].width / (float)data[i].height * longestSidePx), longestSidePx);
			    else
			    	imageData[i] = data[i].scaledTo(longestSidePx,(int) ((float)data[i].height / (float)data[i].width * longestSidePx));
			}
		}
		
		ImageLoader temp = new ImageLoader();
		temp.data = imageData;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		temp.save(out, SWT.IMAGE_PNG);
		return out.toByteArray();
	}
}
