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

import com.drinviewer.droiddrinviewer.DrinViewerActivity.DrinViewerHandler;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;

/**
 * Pairing protocl async task class
 * 
 * @author giorgio
 *
 */
public class PairProtocolTask extends AsyncTask<ClientConnectionManager, Void, Object> {
	
	/**
	 * The app handler to which send messages
	 * 
	 */
	private DrinViewerHandler handler;
	
	/**
	 * The position  in the collection of the host to run the pairing process with
	 * 
	 */
	private int hostPositionInCollection;
	
	public PairProtocolTask(DrinViewerHandler handler, int hostPositionInCollection) {
		super();
		this.handler = handler;
		this.hostPositionInCollection = hostPositionInCollection;
	}

	@Override
	protected Object doInBackground(ClientConnectionManager... params) {
		PairProtocolResult returnMe = new PairProtocolResult(false, null);

		// there must be only one ClientConnectionManager passed
		if (params.length == 1 && hostPositionInCollection>=0) {
			try {
				returnMe.isPaired = params[0].runPairProtocol();
			} catch (Exception thrown) {
				returnMe.isPaired = false;
				returnMe.e = thrown;
			}
		} 
		return returnMe;
	}

	@Override
	protected void onPostExecute(Object obj) {
		/**
		 * sends a message to the handler only if something has happened
		 */
		if (hostPositionInCollection>=0) {
			PairProtocolResult result = (PairProtocolResult) obj;
			Message msg = handler.obtainMessage(DroidDrinViewerConstants.MSG_SERVER_TOGGLEPAIRED);
			Bundle b = new Bundle();
			
			b.putBoolean("isPaired", result.isPaired);
			b.putInt("position", hostPositionInCollection);
			
			if (result.e==null) {
				// there was no error
				b.putBoolean("error", false);
				
			} else {
				// return the exception getMessage
				b.putBoolean("error", true);
				b.putCharSequence("errorMessage", result.e.getMessage());
			}
			msg.setData(b);
			handler.sendMessage(msg);
		}
	}
	
	/**
	 * Object to store returned pair of pairing result
	 * and exception to return if an error occoured
	 * 
	 * @author giorgio
	 *
	 */
	private class PairProtocolResult {
		/**
		 * The pairing protocol result
		 * true if device has been paired
		 */
		public boolean isPaired;
		
		/**
		 * The exception raised during the protocol execution
		 * null if there was no exception
		 * 
		 */
		public Exception e;
		
		public PairProtocolResult(boolean result, Exception e) {
			this.isPaired = result;
			this.e = e;
		}
	}
	
}
