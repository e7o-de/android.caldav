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
package de.e7o.helper;

import java.util.*;
import java.text.*;

public class TimeHelper {
	static DateFormat caldavDateformatLong;
	static DateFormat caldavDateformatShort;
	static TimeZone tz_utc;
	
	static {
		tz_utc = TimeZone.getTimeZone("UTC");
		TimeZone.setDefault(tz_utc);
		// 20110203T000001Z
		caldavDateformatLong = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		// 20110203
		caldavDateformatShort = new SimpleDateFormat("yyyyMMdd");
	}
	
	public static Date mkDate(long inputTime, String tzName) {
		TimeZone tz;
		Date d;
		
		if (tzName == null) {
			d = new Date(inputTime);
		} else {
			// TODO: Zeitzonen?!?
			tz = TimeZone.getTimeZone(tzName);
			d = new Date(inputTime + tz.getOffset(inputTime));
		}
		
		return d;
	}
	
	public static Date mkDate(int year, int month, int day, int hour, int minute, int second) {
		Calendar c = Calendar.getInstance();
		c.set(year, month, day, hour, minute, second);
		return c.getTime();
	}
	
	public static Date currentTime() {
		return Calendar.getInstance().getTime();
	}
	
	public static Date addTime(Date d, int days) {
		return new Date(d.getTime() + days * 86400000l);
	}
	
	public static String mkCaldavDateString(Date d) {
		return caldavDateformatLong.format(d);
	}
	
	public static String mkCaldavDateStringShort(Date d) {
		return caldavDateformatShort.format(d);
	}
	
	public static Date mkCaldavDateString(String s, String tzName) {
		TimeZone tz;
		Date d;
		long l;
		if (tzName == null) {
			d = mkCaldavDateString(s);
		} else {
			// TODO: Very sad code. Maybee there's a better way...
			tz = TimeZone.getTimeZone(tzName);
			TimeZone.setDefault(tz);
			d = mkCaldavDateString(s);
			l = d.getTime();
			TimeZone.setDefault(tz_utc);
			d = new Date(l - tz.getOffset(d.getTime()));
		}
		return d;
	}
	
	public static Date mkCaldavDateString(String s) {
		// TODO: Z is marker for UTC time
		if (s.length() == 15) s += "Z";
		try {
			// Try long format
			return caldavDateformatLong.parse(s);
		} catch (Exception e) {
			try {
				// Try short format
				return caldavDateformatShort.parse(s);
			} catch (Exception e2) {
				// Don't know...
				return null;
			}
		}
	}
}
