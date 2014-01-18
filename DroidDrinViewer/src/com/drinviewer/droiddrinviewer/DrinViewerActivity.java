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

import java.lang.ref.WeakReference;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * Main DrinViewr activity class
 * 
 * @author giorgio
 *
 */
public class DrinViewerActivity extends FragmentActivity {
	/**
	 * message handler for the whole application
	 * 
	 */
	private DrinViewerHandler messageHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_drin_viewer);
		
		messageHandler = new DrinViewerHandler(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.drin_viewer, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_refresh:
			ServerListFragment slFragment = (ServerListFragment) getSupportFragmentManager().findFragmentById(R.id.serverlistfragment);
 			slFragment.doDiscover(true);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Message handler getter
	 * 
	 * @return messageHandler
	 */
	public DrinViewerHandler getMessageHandler() {
		return this.messageHandler;
	}
	
	/**
	 * DrinViewerHandler class for handling application
	 * messages, basically receives messages from the
	 * running thread, runnable and async task and
	 * updates the UI accordingly 
	 * 
	 * @author giorgio
	 *
	 */
	static class DrinViewerHandler extends Handler {
		/**
		 * weak reference to the containing activity
		 * 
		 * @var WeakReference<DrinViewerActivity>
		 */
		private final WeakReference<DrinViewerActivity> mActivity;

		public DrinViewerHandler(DrinViewerActivity a) {
			super();
			mActivity = new WeakReference<DrinViewerActivity>(a);
		}

		@Override
		public void handleMessage(Message msg) {
			
			DrinViewerActivity activity = mActivity.get();
			ServerListFragment slFragment = (ServerListFragment) activity.getSupportFragmentManager().findFragmentById(R.id.serverlistfragment);

			// process the message only if ServerListFragment is there
			if (slFragment != null && slFragment.isInLayout()) {
				switch (msg.what) {
				case DroidDrinViewerConstants.MSG_SERVER_TOGGLEPAIRED:
					Bundle bundle = msg.getData();
					if (bundle.containsKey("isPaired") && bundle.containsKey("position") && bundle.containsKey("error"))
					{
						if (!bundle.getBoolean("error")) {
							slFragment.updateIsPaired (bundle.getInt("position"), bundle.getBoolean("isPaired"));
						} else {
							Toast.makeText(activity.getBaseContext(), bundle.getCharSequence("errorMessage") , Toast.LENGTH_LONG).show();
						}
					}
					break;
				case DroidDrinViewerConstants.MSG_SERVER_FOUND:
					slFragment.getAdapter().notifyDataSetChanged();
					break;
				case DroidDrinViewerConstants.MSG_DISCOVER_START:
					slFragment.getAdapter().notifyDataSetChanged();
					slFragment.setProgressServerVisibility(View.VISIBLE);
					break;
				case DroidDrinViewerConstants.MSG_DISCOVER_DONE:
					slFragment.getAdapter().notifyDataSetChanged();
					slFragment.setProgressServerVisibility(View.GONE);
					break;
				}
			}
		}
	}
}
