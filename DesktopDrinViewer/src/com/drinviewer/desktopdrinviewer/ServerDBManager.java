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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;

import com.drinviewer.common.HostData;

/**
 * class for managing the Server SQLiteDB 
 * 
 * @author giorgio
 */
public class ServerDBManager {
	
	/**
	 * the connection to the database
	 */
	private Connection c;
	
	/**
	 * the connection string to the database
	 * 
	 */
	private String connectionString;
	
	/**
	 * application path where to save the DB file
	 */
	private String appSavePath;
	
	/**
	 * the filename of the database
	 */
	public static final String DB_FILENAME = "drinViewer.db";
	
	/**
	 * database version
	 */
	private static final String DB_VERSION = "0.0.1";

	public ServerDBManager() {
		appSavePath = DesktopDrinViewerConstants.getAppSaveDir();
		connectionString = "jdbc:sqlite:"+ appSavePath + "/" + DB_FILENAME;
		createIfNotExists();		
		if (!checkDBVersion()) updateDB();		
	}
	
	/**
	 * updates the db when a new version is set.
	 * Checks if the version strored in the version table
	 * equals the class constant DB_VERSION and take appropriate
	 * actions if a database update is needed
	 */
	private void updateDB() {
		// TODO: Implement db update logic when it shall be needed
		c = getConnection();
		Statement stmt = null;
		try {
			c.setAutoCommit(false);
			stmt = c.createStatement();
			stmt.executeUpdate("UPDATE version SET version='" + DB_VERSION + "'");
			stmt.close();
			c.commit();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();			
		}
	}

	/**
	 * checks if the database version equals the class constant
	 * 
	 * @return true if the versions match
	 */
	private boolean checkDBVersion()
	{
		String  version = "0.0.0";
		
		try {
			c = getConnection();
			PreparedStatement prep = c.prepareStatement("SELECT * FROM version");

			ResultSet rs = prep.executeQuery();
			while (rs.next()) {
				version = rs.getString("version");
			}
			rs.close();
			prep.close();
			c.close();
			
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(-1);
		}		
		return version.equalsIgnoreCase(DB_VERSION);
	}
	
	/**
	 * creates the databases if it does not exists
	 * copies the file from the resources folder
	 */
	private void createIfNotExists ()
	{
		try {
			File f = new File (appSavePath+"/"+DB_FILENAME);
			if (!f.exists() && !f.isFile())
			{
				// copy the DB from the resources
				InputStream is = ServerDBManager.class.getClassLoader().getResourceAsStream(DB_FILENAME);
				FileOutputStream fos = new FileOutputStream( f );

				byte[] buffer = new byte[1024];
				int read = -1;
				while ((read = is.read(buffer)) != -1) {
					fos.write(buffer, 0, read);
				}
				fos.flush();
				fos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * connects to the database
	 * 
	 * @return the connection to be used
	 */
	private Connection getConnection ()
	{
		Connection conn = null;
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection(connectionString);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
		return conn;
	}
	
	/**
	 * checks if the passed HostData uuid is found in the database
	 * 
	 * @param hostData the host to be checked
	 * @return true if found
	 */
	public boolean isPaired (HostData hostData)
	{
		boolean isPaired = false;
		
		try {
			c = getConnection();
			
			PreparedStatement prep = c.prepareStatement("SELECT id FROM hosts WHERE uuid=?");
			prep.setString(1, hostData.hostname);
			
			ResultSet rs = prep.executeQuery(); 
			
			while (rs.next()) {
				isPaired = (rs.getInt("id") > 0);
			}
			rs.close();
			prep.close();
			c.close();
		} catch (SQLException e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(-1);
		}
		return isPaired;
	}
	
	/**
	 * unpairs a host by removing its uuid from the database
	 * 
	 * @param uuid
	 */
	public void unpairHost (String uuid)
	{
		if (isPaired(new HostData (uuid, "")))
		{
			c = getConnection();
	
			if (c != null) {
				try {
					c.setAutoCommit(false);
					PreparedStatement prep = c.prepareStatement("DELETE FROM hosts WHERE uuid=?");
					prep.setString(1, uuid);
					
					prep.executeUpdate(); 
	
					prep.close();
					c.commit();
					c.close();
				} catch (Exception e) {
					System.err.println(e.getClass().getName() + ": " + e.getMessage());
					System.exit(-1);
				}
			}
		}
		
	}
	
	/**
	 * pairs a host by adding its uuid to the database
	 * 
	 * @param uuid
	 */
	public void pairHost (String uuid)
	{
		if (!isPaired(new HostData (uuid, "")))
		{
			c = getConnection();
	
			if (c != null) {
				try {
					c.setAutoCommit(false);
					PreparedStatement prep = c.prepareStatement("INSERT INTO hosts (uuid) VALUES (?)");
					prep.setString(1, uuid);
					
					prep.executeUpdate(); 
	
					prep.close();
					c.commit();
					c.close();
				} catch (Exception e) {
					System.err.println(e.getClass().getName() + ": " + e.getMessage());
					System.exit(-1);
				}
			}
		}
	}
}
