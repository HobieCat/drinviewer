DrinViewer
==========

DrinViewer is a free software to let you receive notifications on your desktop when your android device is ringing.
It is designed to be as lightweight and fast as possible, goal is: install one app in your desktop OS (be it Linux, Mac or Windows) and one in your Android device (OS version 2.3.3 and up) pair the devices and as long as you have them connected to the same WiFi network you'll receive a popup notification on your desktop showing caller's name, phone and picture whenever you receive an incoming call.
That's it for time being.

The project is structured in two main parts: 

- a desktop Java application that is responsible of listening to any incoming connection and keep an SQLite database on your pc with the list of paired device.
- an Android app responsible of scanning for listening PCs, sending them a pairing request, and sending the needed data to build up the popup notification to the paired PCs. Note that these data will **not** be sent to anybody else and that the Android device does **not** store any persistent data by any means.

********************** 
Directories and Projects Structure
----------------------------------
Each directory of the repository contains an Eclipse project that you can import into your workspace or where you feel more comfortable.

Directories and their contents:

1. **`CommonDrinViewer`** contains the Eclipse project with the common Java classes used by the desktop and Android apps.
2. **`DesktopDrinViewer`** contains the Eclipse project with the needed Java classes for the Desktop application. SWT Library (http://www.eclipse.org/swt/), SQLite-jdbc (https://bitbucket.org/xerial/sqlite-jdbc) and SWTJar (http://mchr3k.github.io/swtjar/) for easy SWT multiplatform deployment are include in the lib folder.
3. **`DroidDrinViewer`** contains the Eclipse project with the needed Java classes for the Android app. android-support-v4.jar for backward Android OS support is included in libs folder for convenience.

**********************
Notices
-------
DrinViewer is still under heavy developement, the source code is just a skeleton for further imporvements but still usable.

You'll always have the source code: I'm not asking to trust me, but look at the source to realize that nothing evil will be done with your precious contact informations. Every permission asked to the Android device is strictly related to the implementation.

No binaries are provided for time being, but you can clone the repository and compile it yourself.

**********************
DrinViewer is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
