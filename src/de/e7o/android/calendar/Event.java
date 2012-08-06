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

import de.e7o.helper.TimeHelper;

import java.security.MessageDigest;
import java.util.*;

import android.content.ContentValues;
import android.database.Cursor;

//TODO: ("rrule", "FREQ=YEARLY");
//      (“rrule”, “FREQ=MONTHLY;UTIL=20200624T080000Z;INTERVAL=1;BYMONTHDAY=24;WKST=SU”);
//      http://www.ietf.org/rfc/rfc2445.txt

// For iCalendar-Format:
//  - RFC: http://www.ietf.org/rfc/rfc2445.txt
//  - Validator: http://severinghaus.org/projects/icv/

public class Event {
	private static boolean colIndexInitialised = false;
	private static final String PRODUCER_LINE = "-//e7o.de//NONSGML CalDAV Sync//EN";
	private static final String ICS_VERSION = "2.0";
	/*
	 * following constants are meant final but can't because of silly Java:
	 * "The blank final field IDX_* may not have been initialized"
	*/
	private static int IDX_DTSTART = 0;
	private static int IDX_DTEND = 0;
	private static int IDX_ALLDAY = 0;
	private static int IDX_TIMEZONE = 0;
	private static int IDX_EVENTTIMEZONE = 0;
	private static int IDX_TITLE = 0;
	private static int IDX_DESCRIPTION = 0;
	private static int IDX_EVENTLOCATION = 0;
	private static int IDX_RRULE = 0;
	private static int IDX_HASALARM = 0;
	private static int IDX_IMPORTANCE = 0;
	private static int IDX_VISIBILITY = 0;
	private static int IDX_TRANSPARENCY = 0;
	private static int IDX_GUID = 0;
	private static int IDX_EVENTSTATUS = 0;
	private static int IDX_ID = 0;
	
	public Calendar myCal;
	
	public Date val_dtstart;
	public Date val_dtend;
	public Date val_created = null;
	public Date val_lastmodified = null;
	public int val_allday;
	public String val_timezone;
	public String val_eventtimezone;
	public String val_title;
	public String val_description;
	public String val_eventlocation;
	public String val_rrule;
	public int val_hasalarm;
	public int val_importance;
	// VISIBILITY: default=0, confidential=1, private=2, public=3
	public int val_visibility;
	// TRANSPARENCY: opaque=0, transparent=1
	public int val_transparency;
	public String val_guid;
	// STATUS: tentative=0, confirmed=1, canceled=2
	public int val_eventstatus;
	public int val_id;
	
	public String etag = null;
	
	public boolean isExistingInContentProvider = false;
	public boolean isExistingOnServerSite = false;
	public boolean isSynced = false;
	
	public static final String COLUMS[] = {
		"dtstart", "dtend", "allDay", "timezone", "eventTimezone",
		"title", "description", "eventLocation",
		"rrule", "hasAlarm", "importance", "visibility", "transparency",
		"iCalGUID", "eventStatus", "_id"
	};
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[EVENT id="+val_id+", start="+val_dtstart.toString()+", title="+val_title+"]");
		return sb.toString();
	}
	
	public Event(Calendar cal, Cursor c) {
		myCal = cal;
		
		if (colIndexInitialised == false) {
			IDX_DTSTART = c.getColumnIndex("dtstart");
			IDX_DTEND = c.getColumnIndex("dtend");
			IDX_ALLDAY = c.getColumnIndex("allDay");
			IDX_TIMEZONE = c.getColumnIndex("timezone");
			IDX_EVENTTIMEZONE = c.getColumnIndex("eventTimezone");
			IDX_TITLE = c.getColumnIndex("title");
			IDX_DESCRIPTION = c.getColumnIndex("description");
			IDX_EVENTLOCATION = c.getColumnIndex("eventLocation");
			IDX_RRULE = c.getColumnIndex("rrule");
			IDX_HASALARM = c.getColumnIndex("hasAlarm");
			IDX_IMPORTANCE = c.getColumnIndex("importance");
			IDX_VISIBILITY = c.getColumnIndex("visibility");
			IDX_TRANSPARENCY = c.getColumnIndex("transparency");
			IDX_GUID = c.getColumnIndex("iCalGUID");
			IDX_EVENTSTATUS = c.getColumnIndex("eventStatus");
			IDX_ID = c.getColumnIndex("_id");
			colIndexInitialised = true;
		}
		
		//val_timezone = c.getString(IDX_TIMEZONE);
		// Android saves UTC-times, so we use UTC, too
		val_timezone = "UTC";
		val_eventtimezone = c.getString(IDX_EVENTTIMEZONE);
		
		val_dtstart = TimeHelper.mkDate(c.getLong(IDX_DTSTART), /*val_timezone*/null);
		val_dtend = TimeHelper.mkDate(c.getLong(IDX_DTEND), /*val_timezone*/null);
		
		val_allday = c.getInt(IDX_ALLDAY);
		val_title = c.getString(IDX_TITLE);
		val_description = c.getString(IDX_DESCRIPTION);
		val_eventlocation = c.getString(IDX_EVENTLOCATION);
		val_rrule = c.getString(IDX_RRULE);
		val_hasalarm = c.getInt(IDX_HASALARM);
		val_importance = c.getInt(IDX_IMPORTANCE);
		val_visibility = c.getInt(IDX_VISIBILITY);
		val_transparency = c.getInt(IDX_TRANSPARENCY);
		val_guid = c.getString(IDX_GUID);
		val_eventstatus = c.getInt(IDX_EVENTSTATUS);
		val_id = c.getInt(IDX_ID);
		
		isExistingInContentProvider = true;
	}
	
	public Event(Calendar cal) {
		myCal = cal;
		isExistingInContentProvider = false;
		val_guid = myCal.cm.nextGUID();
	}
	
	public Event(Calendar cal, String parseICal) {
		myCal = cal;
		isExistingInContentProvider = false;
		parseICal(parseICal);
	}
	
	public Event() {
		// Nothing
		// Don't use this constructor unless you are sure what you're doing :)
	}
	
	public void parseICal(String parseICal) {
		String lines[];
		String l[], l2[];
		String p1, p2, v;
		int i;
		boolean isInEvent = false;
		boolean forceAllday = false;
		
		lines = parseICal.split("\n");
		for (i = 0; i < lines.length; i++) {
			l = lines[i].split(":", 2);
			if (l.length <= 1) {
				// ERROR - ignoring line
			} else {
				// Parse values
				// ... before ':'
				if (l[0].indexOf(';') > 0) {
					// e.g. DTSTART;TZID=...:...
					l2 = l[0].split(";");
					p1 = l2[0];
					p2 = l2[1];
				} else {
					p1 = l[0];
					p2 = "";
				}
				v = l[1];
				// Where we are?
				if (p1.equals("BEGIN") && v.equals("VEVENT")) {
					isInEvent = true;
				} else if (p1.equals("END") && v.equals("VEVENT")) {
					isInEvent = false;
					break;
				} else if (isInEvent) {
					// TODO: range between BEGIN:VALARM and END:VALARM - filter or collect...
					// Read properties
					if (p1.equals("CREATED")) {
						val_created = TimeHelper.mkCaldavDateString(v);
					} else if (p1.equals("LAST-MODIFIED")) {
						val_lastmodified = TimeHelper.mkCaldavDateString(v);
					} else if (p1.equals("DTSTAMP") && val_created == null) {
						val_created = TimeHelper.mkCaldavDateString(v);
					} else if (p1.equals("UID")) {
						val_guid = v;
					} else if (p1.equals("SUMMARY")) {
						val_title = v;
					} else if (p1.equals("DTSTART")) {
						if (p2.length() > 6 && p2.substring(0, 5).equalsIgnoreCase("TZID=")) val_timezone = p2.substring(5);
						if (v.length() == 8) {
							forceAllday = true;
							// Using timezone give bad results
							val_dtstart = TimeHelper.mkCaldavDateString(v, null);
						} else {
							val_dtstart = TimeHelper.mkCaldavDateString(v, val_timezone);
						}
					} else if (p1.equals("DTEND")) {
						val_dtend = TimeHelper.mkCaldavDateString(v, forceAllday ? null : val_timezone);
					} else if (p1.equals("TRANSP")) {
						val_transparency = v.equalsIgnoreCase("TRANSPARENT") ? 1 : 0;
					} else if (p1.equals("X-MICROSOFT-CDO-ALLDAYEVENT")) {
						val_allday = v.equalsIgnoreCase("TRUE") ? 1 : 0;
					}
				}
			}
		}
		if (forceAllday) val_allday = 1;
		// We use UTC.
		val_timezone = "UTC";
	}
	
	public void save() {
		ContentValues cv = new ContentValues();
		
		cv.put("calendar_id", myCal.getId());
		cv.put("iCalGUID", val_guid);
		
		cv.put("dtstart", val_dtstart.getTime());
		cv.put("dtend", val_dtend.getTime());
		cv.put("allDay", val_allday);
		// TODO really include timezone? It's always UTC I hope...
		//cv.put("timezone", val_timezone);
		//cv.put("eventTimezone", val_eventtimezone);
		
		cv.put("title", val_title);
		cv.put("description", val_description);
		cv.put("eventLocation", val_eventlocation);
		
		cv.put("eventStatus", val_eventstatus);
		cv.put("rrule", val_rrule);
		cv.put("hasAlarm", val_hasalarm);
		cv.put("importance", val_importance);
		cv.put("visibility", val_visibility);
		cv.put("transparency", val_transparency);
		
		if (isExistingInContentProvider) {
			saveAsModified(cv);
		} else {
			saveAsNew(cv);
		}
		
		updateSyncDB();
	}
	
	public void updateSyncDB() {
		myCal.cm.updateSyncDB(this);
	}
	
	private void saveAsModified(ContentValues cv) {
		myCal.cm.update("/events/"+val_id, cv, val_id);
	}
	
	private void saveAsNew(ContentValues cv) {
		myCal.cm.insert("/events", cv);
		// TODO: id zurückgeben lassen und setzen
		// TODO: etag?
		isExistingInContentProvider = true;
	}
	
	public void delete() {
		myCal.cm.delete("/events/"+val_id);
		myCal.cm.deleteSyncDB(this);
	}
	
	public String getICal() {
		StringBuilder sb = new StringBuilder();
		
		val_lastmodified = java.util.Calendar.getInstance().getTime();
		if (val_created == null) val_created = val_lastmodified;
		
		sb.append("BEGIN:VCALENDAR\nPRODID:"+PRODUCER_LINE+"\nVERSION:"+ICS_VERSION+"\nCALSCALE:GREGORIAN\nBEGIN:VEVENT");
		sb.append("\nCREATED:"+TimeHelper.mkCaldavDateString(val_created));
		sb.append("\nLAST-MODIFIED:"+TimeHelper.mkCaldavDateString(val_lastmodified));
		sb.append("\nDTSTAMP:"+TimeHelper.mkCaldavDateString(val_created));
		sb.append("\nUID:"+val_guid);
		sb.append("\nSUMMARY:"+val_title.replace("\n", "\\n"));
		if (val_allday == 1) {
			sb.append("\nDTSTART;VALUE=DATE:"+TimeHelper.mkCaldavDateStringShort(val_dtstart));
			sb.append("\nDTEND;VALUE=DATE:"+TimeHelper.mkCaldavDateStringShort(val_dtend));
			sb.append("\nX-MICROSOFT-CDO-ALLDAYEVENT:TRUE");
		} else {
			//sb.append("\nDTSTART;TZID="+val_timezone+":"+TimeHelper.mkCaldavDateString(val_dtstart));
			//sb.append("\nDTEND;TZID="+val_timezone+":"+TimeHelper.mkCaldavDateString(val_dtend));
			// It's UTC:
			sb.append("\nDTSTART:"+TimeHelper.mkCaldavDateString(val_dtstart));
			sb.append("\nDTEND:"+TimeHelper.mkCaldavDateString(val_dtend));
		}
		sb.append("\nTRANSP:"+(val_transparency==1 ? "TRANSPARENT" : "OPAQUE"));
		sb.append("\nEND:VEVENT\nEND:VCALENDAR\n");
		return sb.toString();
	}
	
	public void setTimezone(String tzName) {
		val_timezone = val_eventtimezone = tzName;
	}
	
	public String getHash() {
		MessageDigest md;
		StringBuffer sb = new StringBuffer();
		int i;
		byte b[];
		
		try {
			md = MessageDigest.getInstance("SHA-512");
		} catch (Exception e) {
			// If it doesn't work use beginning and end of event
			return "error-"+val_dtstart.toString()+"-"+val_dtend.toString();
		}
		
		md.reset();
		
		sb.append("lh-");
		
		if (val_dtstart != null) md.update(val_dtstart.toString().getBytes());
		if (val_dtend != null) md.update(val_dtend.toString().getBytes());
		//Ignore timezone changes...
		//if (val_timezone != null) md.update(val_timezone.toString().getBytes());
		//if (val_eventtimezone != null) md.update(val_eventtimezone.toString().getBytes());
		if (val_title != null) md.update(val_title.toString().getBytes());
		if (val_description != null) md.update(val_description.toString().getBytes());
		if (val_rrule != null) md.update(val_rrule.toString().getBytes());
		if (val_guid != null) md.update(val_guid.toString().getBytes());
		md.update((byte)val_hasalarm);
		md.update((byte)val_importance);
		md.update((byte)val_visibility);
		md.update((byte)val_transparency);
		md.update((byte)val_eventstatus);
		// Not: md.update((byte)val_id);
		
		b = md.digest();
		for (i = 0; i < 15; i++) {
			sb.append(Integer.toHexString(0xff & b[i]));
		}
		
		return sb.toString();
	}
}
