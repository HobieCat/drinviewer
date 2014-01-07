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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;
import java.util.UUID;

import com.drinviewer.common.Constants;

import android.content.Context;

/**
 * Generates a random UUID that identifies the user installation
 * and *NOT* the device. Writes and reads it to and from a file
 * 
 * This is from: http://android-developers.blogspot.it/2011/03/identifying-app-installations.html
 * 
 * @author giorgio
 *
 */
public class InstallationUUID {
	/**
	 * The String that will be returned
	 */
	private static String sID = null;
	
	/**
	 * Saved file name
	 */
    private static final String INSTALLATION = Constants.APPNAME+Constants.APPVERSION;

    /**
     * Gets the stored UUID if the file is there, else generates it
     * 
     * @param context Application context
     * @return the generated UUID
     */
    public synchronized static String id(Context context) {

        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);

            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID.toUpperCase(Locale.getDefault());
    }

    /**
     * Reads the file if there
     * 
     * @param installation filename to read
     * @return the read string
     * @throws IOException
     */
    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    /**
     * Writes the generated UUID to the file
     * 
     * @param installation file name to write
     * @throws IOException
     */
    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }
}
