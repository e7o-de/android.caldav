/*
    e7o.de CalDAV project
    Copyright (C) 2010 sven herzky

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.e7o.caldav.sync;

import java.util.HashMap;

import de.e7o.android.calendar.CalendarManager;
import de.e7o.android.calendar.Event;
import de.e7o.helper.ErrorHelper;
import de.e7o.helper.TimeHelper;
import de.e7o.sqlite.*;

import android.app.*;
import android.content.*;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class StartScreen extends Activity implements SyncObserver, SyncProvider {
	private SharedPreferences pref;
	private SQLiteManager sm;
	private SQLiteTable tblAppdata, tblSyncdata;
	private boolean syncActive = false;
	private TextView tvStatus, tvLastSync, tvResult;
	
	private final int ecs_version = 1;
	
	public void onCreate(Bundle savedInstanceState) {
		SQLiteResult sr;
		String s[];
		int i;
		
		// Constructor
		super.onCreate(savedInstanceState);
		
		// Init screen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		tvStatus = (TextView)findViewById(R.id.tvStatus);
		tvLastSync = (TextView)findViewById(R.id.tvLast);
		tvResult = (TextView)findViewById(R.id.tvResult);
		
		// Init fields
		tvStatus.setText("");
		tvResult.setText("");
		
		// Init preferences
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Init SQLite
		sm = new SQLiteManager(this, "e7o_syncdb");
		
		tblAppdata = new SQLiteTable("appdata");
		tblAppdata.addField(new SQLiteField("param", false)); // Parameter-Name
		tblAppdata.addField(new SQLiteField("value", false)); // Value of param
		sm.addTable(tblAppdata);
		
		tblSyncdata = new SQLiteTable("syncdata");
		tblSyncdata.addField(new SQLiteField("syncid", true, true)); // PRIMARY KEY
		tblSyncdata.addField(new SQLiteField("icalguid", false)); // UID of event
		tblSyncdata.addField(new SQLiteField("androidid", false)); // ID of event
		tblSyncdata.addField(new SQLiteField("androidcalendar", false)); // Calendar of event
		tblSyncdata.addField(new SQLiteField("etag", false)); // Last known etag
		tblSyncdata.addField(new SQLiteField("androidhash", false)); // Last known local hash (see Event.java)
		tblSyncdata.addField(new SQLiteField("status", true)); // Status of event (0=normal, 1=deleted)
		sm.addTable(tblSyncdata);
		
		// Check for version & insert values if neccessary
		sr = sm.query("SELECT value FROM appdata WHERE param='ecs_version'");
		
		if (sr.length() > 0) {
			s = sr.next();
			i = Integer.parseInt(s[0]);
			if (i < ecs_version) {
				sm.query("UPDATE appdata SET value='"+ecs_version+"' WHERE param='ecs_version'");
				tvStatus.setText("[Updated db from "+i+" to "+ecs_version+"]");
			}
		} else {
			i = -1;
		}
		
		// No breaks in switch!
		switch (i) {
			case -1:
				tblAppdata.addRow("ecs_version", ""+ecs_version);
			case 0:
				tblAppdata.addRow("sync_lastctag", "never_synced");
				tblAppdata.addRow("sync_lastsync", "?");
		}
		
		tvLastSync.setText(getSavedAppdata("sync_lastsync"));
		
		ErrorHelper.addErrMessage("Started");
		
		//startSync();
    }
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.mainmenu_syncnow:
				startSync();
				break;
			case R.id.mainmenu_settings:
				startActivityForResult(new Intent(this, Settings.class), 0);
				break;
			case R.id.mainmenu_export:
				// TODO
				break;
			case R.id.mainmenu_errors:
				startActivityForResult(new Intent(this, ViewLog.class), 0);
				break;
			case R.id.mainmenu_removeentries:
				deleteAllLocalEvents();
				break;
			case R.id.mainmenu_about:
				showDialog("(c) 2010 by e7o.de/Sven Herzky (android@e7o.de)\nReleased under the terms of GnuGPLv3.\n\nProduct makes use of Apache HttpCore, HttpClient, Commons.\n\nSee:\nhttp://www.e7o.de/\nhttps://sourceforge.net/projects/e7cal/");
				break;
		}
		return false;
	}
	
	public void deleteAllLocalEvents() {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		DialogInterface.OnClickListener ocl;
		
		ocl = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						deleteAllLocalEvents_do();
						break;
				}
			}
		};
		
		b.setMessage(R.string.dialog_reallydeleteallevents);
		b.setPositiveButton(R.string.dialog_yes, ocl);
		b.setNegativeButton(R.string.dialog_no, ocl);
		b.show();
	}
	
	private void deleteAllLocalEvents_do() {
		CalendarManager cm = new CalendarManager(this, this);
		HashMap<String,Event> hm = cm.getEvents(TimeHelper.mkDate(1900, 0, 1, 0, 0, 0), TimeHelper.mkDate(2300, 0, 1, 0, 0, 0));
		for (Event e : hm.values()) {
			e.delete();
		}
		sm.query("DELETE FROM syncdata");
		
		showDialog(R.string.dialog_deleteallevents_done);
		ErrorHelper.addErrMessage("All events deleted");
	}
	
	public void showDialog(String message) {
		AlertDialog.Builder b;
		b = new AlertDialog.Builder(this);
		b.setMessage(message);
		b.setNeutralButton(R.string.dialog_ok, null);
		b.show();
	}
	
	public void startSync() {
		Syncing s;
		if (syncActive) return;
		s = new Syncing(this, this, this);
		syncActive = true;
		s.startSync();
	}
	
	public void syncDone(String result) {
		String ls;
		syncActive = false;
		tvStatus.setText("Sync done");
		tvResult.setText(result);
		ls = java.util.Calendar.getInstance().getTime().toLocaleString();
		tvLastSync.setText(ls);
		writeAppdata("sync_lastsync", ls);
		ErrorHelper.addErrMessage("Sync done\n"+result);
	}
	
	public void syncError(String result) {
		syncActive = false;
		tvStatus.setText("Error occured");
		tvResult.setText(result);
		ErrorHelper.addErrMessage("Sync error\n"+result);
	}
	
	public void syncState(String state) {
		ErrorHelper.addErrMessage(state);
		tvStatus.setText(state);
	}
	
	public SharedPreferences getPreferences() { return pref; }
	public SQLiteManager getSQLiteManager() { return sm; }
	public SQLiteTable getTableAppdata() { return tblAppdata; }
	public SQLiteTable getTableSyncdata() { return tblSyncdata; }
	
	public String getSavedAppdata(String param) {
		SQLiteResult sr;
		String s[];
		sr = sm.query("SELECT value FROM appdata WHERE param='"+param+"'");
		if (sr.length() > 0) {
			s = sr.next();
			return s[0];
		} else {
			return null;
		}
	}
	
	public void writeAppdata(String param, String newValue) {
		if (newValue.indexOf('"') > -1) newValue = newValue.replace("\"", "\\\"");
		sm.query("UPDATE appdata SET param='"+newValue+"' WHERE param='"+param+"'");
	}
}