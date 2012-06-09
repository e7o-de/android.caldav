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

import android.database.Cursor;

import java.util.*;

import de.e7o.helper.ErrorHelper;

public class Calendar {
	private int calId;
	private String calName;
	public CalendarManager cm;
	
	protected Calendar(CalendarManager cm, int calId, String calName) {
		this.cm = cm;
		this.calId = calId;
		this.calName = calName;
	}
	
	public int getId() {
		return calId;
	}
	
	public String getName() {
		return calName;
	}
	
	/*
	public LinkedList<Event> getEvents() {
		return getEvents(null, null);
	}
	*/
	
	public HashMap<String, Event> getEvents(Date from, Date to) {
		HashMap<String, Event> hm = new HashMap<String, Event>();
		Cursor c;
		Event ev;
		
		try {
			c = cm.getCursor(
				"/instances/when/"+from.getTime()+"/"+to.getTime(),
				"Calendars._id="+calId,
				Event.COLUMS
			);
		} catch (Exception e) {
			ErrorHelper.addErrMessage(e);
			String s = e.getClass().getName() + ": " + e.getLocalizedMessage();
			System.out.println(s);
			return null;
		}
		
		if (c.moveToFirst()) {
			do {
				ev = new Event(this, c);
				hm.put(ev.val_guid, ev);
			} while (c.moveToNext());
		}
		
		return hm;
	}
	
	public Event getEvent(int eventID) {
		Cursor c;
		c = cm.getCursor("/events/"+eventID, null, Event.COLUMS);
		if (c.moveToFirst()) {
			return new Event(this, c);
		} else {
			return null;
		}
	}
	
	public Event addEvent() {
		return new Event(this);
	}
}
