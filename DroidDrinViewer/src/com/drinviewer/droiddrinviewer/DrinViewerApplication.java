package com.drinviewer.droiddrinviewer;

import android.app.Application;
import android.content.Intent;

public class DrinViewerApplication extends Application {
	private static String installationUUID;

	@Override
	public void onCreate() {
		super.onCreate();
		// sets installation uuid
		DrinViewerApplication.installationUUID = InstallationUUID.id(getApplicationContext());
		startService(new Intent(DiscoverServerService.class.getName()));
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		stopService(new Intent(DiscoverServerService.class.getName()));
	}

	/**
	 * @return the installationUUID
	 */
	public static String getInstallationUUID() {
		return installationUUID;
	}
}
