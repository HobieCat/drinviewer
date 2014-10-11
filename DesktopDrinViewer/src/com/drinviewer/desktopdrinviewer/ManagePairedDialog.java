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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * Class that implements the Manage Paired Dialog
 * shown on menu item selection
 * 
 * @author giorgio
 *
 */
public class ManagePairedDialog extends Dialog {
	
	/**
	 * The table element shown
	 */
	private Table table;

	public ManagePairedDialog(Shell owner) {
		this(owner, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	public ManagePairedDialog(Shell owner, int style) {
		super(owner, style);
	}
	
	public void open() {
	    Shell shell = new Shell(getParent(), getStyle());
	    this.setText(DesktopDrinViewerConstants.i18nMessages.getString("managepaired"));
	    shell.setText(getText());
	    createContents(shell);
	    shell.pack();
	    shell.open();
	  }
	
	private void createContents(final Shell shell) {
		
		final ServerDBManager db = new ServerDBManager();
		
		GridLayout gridLayout = new GridLayout();
		GridData gridData;
		
		gridLayout.numColumns = 2;
		shell.setLayout(gridLayout);
		
		gridData = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
		gridData.horizontalSpan = gridLayout.numColumns;
		Label label = new Label(shell, SWT.NULL);
		label.setText(DesktopDrinViewerConstants.i18nMessages.getString("selectpaireddevices"));
		label.setLayoutData(gridData);
		    
		table = new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		
		gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
		gridData.horizontalSpan = gridLayout.numColumns;
		
		table.setLayoutData(gridData);
		
		ArrayList<String>uuids = db.getPairedDevices();
		
		if (uuids!=null) {
			for (int i = 0; i < uuids.size(); i++) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(uuids.get(i));
				item.setChecked(true);
			}			
		}
		
		table.setSize(800, 150);
		
		gridData = new GridData(GridData.END, GridData.CENTER, true, false);
		gridData.horizontalSpan = 1;
		Button ok = new Button(shell, SWT.PUSH);
		ok.setText(DesktopDrinViewerConstants.i18nMessages.getString("ok"));		
		ok.setLayoutData(gridData);
		
		gridData = new GridData(GridData.END, GridData.CENTER, false, false);
		gridData.horizontalSpan = 1;
		Button cancel = new Button(shell, SWT.PUSH);
		cancel.setText(DesktopDrinViewerConstants.i18nMessages.getString("cancel"));
		cancel.setLayoutData(gridData);
		
	    ok.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent event) {
	    	  if (table.getItemCount()>0) {
	    		  TableItem[] items = table.getItems();
	    		  for (int i = 0; i<items.length ; i++) {
	    			  if (!items[i].getChecked()) {
	    				  db.unpairHost(items[i].getText());
	    			  }
	    		  }
	    	  }
	    	  
	    	  shell.close();
	      }
	    });

	    cancel.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent event) {
	    	  shell.close();
	      }
	    });

	    shell.setDefaultButton(ok);
	}
}
