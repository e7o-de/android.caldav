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
package de.e7o.android.calendar;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;

import java.util.*;
import java.security.MessageDigest;

import de.e7o.caldav.sync.SyncProvider;
import de.e7o.sqlite.*;

/*
Remember: Before doing actions like adding an event you have to
          request at least one Cursor (because it sets MY_PROVIDER)
          or call initProvider();
*/
public class CalendarManager {
	private Activity baseActivity;
	private SyncProvider syncPro;
	private SQLiteManager sm;
	private ContentResolver cr;
	private final String POSSIBLE_PROVIDERS[] = {
		"content://calendar" /* up to 2.1 */,
		"content://com.android.calendar", /* since 2.2 */
		"content://calendarEx" /* Motorola */
	};
	private String MY_PROVIDER = null;
	
	private MessageDigest md;
	
	public CalendarManager(Activity a, SyncProvider sp) {
		byte rndkey[];
		
		baseActivity = a;
		syncPro = sp;
		sm = syncPro.getSQLiteManager();
		cr = a.getContentResolver();
		
		rndkey = new byte[35];
		new Random().nextBytes(rndkey);
		try {
			md = MessageDigest.getInstance("SHA-512");
			md.reset();
			md.update(rndkey);
		} catch (Exception e) {
		}
	}
	
	public void initProvider() {
		// Only initialising with a free choosen uri
		getCursor("/calendars", null, null);
	}
	
	public String nextGUID() {
		StringBuffer sb = new StringBuffer();
		byte b[] = new byte[5];
		int i;
		
		sb.append("e70de");
		new Random().nextBytes(b);
		md.update(b);
		b = md.digest();
		for (i = 0; i < 12; i++) {
			if (i % 3 == 0) sb.append('-');
			sb.append(Integer.toHexString(0xff & b[i]));
		}
		
		return sb.toString();
	}
	
	public void insert(String uri, ContentValues cv) {
		baseActivity.getContentResolver().insert(Uri.parse(MY_PROVIDER+uri), cv);
	}
	
	public void delete(String uri) {
		baseActivity.getContentResolver().delete(Uri.parse(MY_PROVIDER+uri), null, null);
	}
	
	public void update(String uri, ContentValues cv, int id) {
		baseActivity.getContentResolver().update(Uri.parse(MY_PROVIDER+uri), cv, null, null);
	}
	
	public void updateSyncDB(Event e) {
		SQLiteResult sr;
		String s[];
		sr = sm.query("SELECT syncid FROM syncdata WHERE icalguid=\""+e.val_guid+"\"");
		if (sr.length() > 0) {
			s = sr.next();
			sm.query("UPDATE syncdata SET etag=\""+e.etag+"\", androidhash=\""+e.getHash()+"\" WHERE syncid="+s[0]);
		} else {
			syncPro.getTableSyncdata().addRow(e.val_guid, ""+e.val_id, ""+e.myCal.getId(), e.etag, e.getHash(), "0");
		}
	}
	
	public void deleteSyncDB(Event e) {
		sm.query("DELETE FROM syncdata WHERE icalguid=\""+e.val_guid+"\"");
	}
	
	// e.g. getCursor("/events");
	// e.g. getCursor("/calendars", "selected=1");
	public Cursor getCursor(String uri) {
		return getCursor(uri, null, null);
	}
	
	public Cursor getCursor(String uri, String projection[]) {
		return getCursor(uri, null, projection);
	}
	
	public Cursor getCursor(String uri, String params) {
		return getCursor(uri, params, null);
	}
	
	public Cursor getCursor(String uri, String params, String columns[]) {
		int i;
		Cursor cursor = null;
		
		if (MY_PROVIDER == null) {
			for (i = 0; i < POSSIBLE_PROVIDERS.length; i++) {
				cursor = cr.query(Uri.parse(POSSIBLE_PROVIDERS[i]+uri), columns, params, null, null);
				if (cursor != null) {
					MY_PROVIDER = POSSIBLE_PROVIDERS[i];
					break;
				}
			}
			if (MY_PROVIDER == null) {
				// TODO: ErrorHelper oder anderes weil kein Provider da ist
				return null;
			} else {
				return cursor;
			}
		} else {
			return cr.query(Uri.parse(MY_PROVIDER+uri), null, null, null, null);
		}
	}
	
	public Calendar getCalendar(int calID) {
		return new Calendar(this, calID, "[not resolved]");
	}
	
	public LinkedList<Calendar> getCalendars() {
		LinkedList<Calendar> ll = new LinkedList<Calendar>();
		Cursor c;
		int col_id, col_name;
		
		c = getCursor("/calendars", "selected=1", new String[]{"_id", "displayname"});
		
		if (c.moveToFirst()) {
			col_id = c.getColumnIndex("_id");
			col_name = c.getColumnIndex("displayname");
			do {
				ll.add(new Calendar(this, c.getInt(col_id), c.getString(col_name)));
			} while (c.moveToNext());
		}
		
		return ll;
	}
	
	public HashMap<String, Event> getEvents(Date from, Date to) {
		HashMap<String, Event> hme = new HashMap<String, Event>();
		LinkedList<Calendar> llc;
		
		llc = getCalendars();
		for (Calendar c : llc) {
			hme.putAll(c.getEvents(from, to));
		}
		
		return hme;
	}
}