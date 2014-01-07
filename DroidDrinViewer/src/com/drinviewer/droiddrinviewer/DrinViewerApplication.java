package com.drinviewer.droiddrinviewer;

import android.app.Application;

public class DrinViewerApplication extends Application {
	private static String installationUUID;

	@Override
	public void onCreate() {
		super.onCreate();
		// sets installation uuid
		DrinViewerApplication.installationUUID = InstallationUUID.id(getApplicationContext());
	}

	/**
	 * @return the installationUUID
	 */
	public static String getInstallationUUID() {
		return installationUUID;
	}
}
