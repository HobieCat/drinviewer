package com.drinviewer.droiddrinviewer;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Intent;

@ReportsCrashes(formKey="",
mode = ReportingInteractionMode.TOAST,
mailTo = "drinviewer@gmail.com",
resToastText = R.string.crash_toast_text // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
)

public class DrinViewerApplication extends Application {
	private static String installationUUID;

	@Override
	public void onCreate() {
		super.onCreate();
		// sets installation uuid
		DrinViewerApplication.installationUUID = InstallationUUID.id(getApplicationContext());
		startService(new Intent(DiscoverServerService.class.getName()));
			
		ACRA.init(this);
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
