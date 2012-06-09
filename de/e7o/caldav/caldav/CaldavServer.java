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
package de.e7o.caldav.caldav;

import javax.xml.parsers.*;

import org.apache.http.Header;
import org.w3c.dom.*;

import java.io.*;

import de.e7o.helper.*;

public class CaldavServer {
	public String baseUri;
	public String username, password;
	
	public DocumentBuilderFactory dbf;
	public DocumentBuilder db;
	
	public static final String depth0[] = {"Depth", "0"};
	public static final String depth1[] = {"Depth", "1"};
	public static final String noFurtherHeader[] = {"X-No-Further-Header", "1"};
	
	public String lastEtag = null;
	
	public CaldavServer(String baseUri) {
		this(baseUri, null, null);
	}
	
	public CaldavServer(String baseUri, String username, String password) {
		// Check
		if (username != null && username.length() == 0) username = null;
		if (password != null && password.length() == 0) password = null;
		// Remeber values
		this.baseUri = baseUri;
		this.username = username;
		this.password = password;
		// XML creation
		dbf = DocumentBuilderFactory.newInstance();
		try {
			db = dbf.newDocumentBuilder();
		} catch (Exception e) {
		}
	}
	
	public Connection newRawConnection() {
		return new Connection(this);
	}
	
	public Document newDocument() {
		return db.newDocument();
	}
	
	public void prepareRootElement(Element root) {
		root.setAttribute("xmlns:D", "DAV:");
		root.setAttribute("xmlns:CS", "http://calendarserver.org/ns/");
		root.setAttribute("xmlns:C", "urn:ietf:params:xml:ns:caldav");
	}
	
	public String documentToString(Node n) {
		return XmlHelper.documentToString(n);
	}
	
	public NodeList xpath(String expr, Node n) {
		// Not available (missing support on Android plattform)
		// Using findNodes instead
		ErrorHelper.addErrMessage("Error: Someone is using CaldavServer.xpath(\""+expr+"\")!");
		return null;
	}
	
	public static NodeList findNodes(String tagName, Node n) {
		return XmlHelper.findNodes(tagName, n);
	}
	
	public String getNodeContent(Node n, boolean stripQuotes) {
		String s;
		s = XmlHelper.getTextContent(n);
		if (s.charAt(0) == '"') s = s.substring(1, s.length() - 1);
		return s;
	}
	
	public String getNodeContent(Node n) {
		return getNodeContent(n, false);
	}
	
	public String getNodeContent(Node n, String xpath) {
		//NodeList nl = xpath(xpath, n);
		NodeList nl = findNodes(xpath, n);
		if (nl.getLength() == 0) return null;
		return getNodeContent(nl.item(0), true);
	}
	
	// System.out.println(cs.requestCTag());
	public String requestCTag() {
		Document d = newDocument();
		Element root = d.createElement("D:propfind");
		Element p;
		
		Connection c;
		NodeList nl;
		
		prepareRootElement(root);
		d.appendChild(root);
		
		p = d.createElement("D:prop");
		root.appendChild(p);
		p.appendChild(d.createElement("CS:getctag"));
		
		try {
			c = newRawConnection();
			d = db.parse(c.request("", "PROPFIND", documentToString(d), depth0));
			//nl = xpath("//getctag", d);
			nl = findNodes("getctag", d);
			if (nl.getLength() == 0) {
				ErrorHelper.addErrMessage("No ctag specified");
				return null;
			} else {
				return getNodeContent(nl.item(0));
			}
		} catch (Exception e) {
			ErrorHelper.addErrMessage(e);
			return null;
		}
	}
	
	/*
	String s[][] = requestEntriesByTime("20100701T000001Z", "20100731T235959Z", "VEVENT");
	for (int i = 0; i < s.length; i++) {
		System.out.println(s[i][2]);
	}
	*/
	public String[][] requestEntriesByTime(String startTime, String endTime, String filterType) {
		Document d = newDocument();
		Element root = d.createElement("C:calendar-query");
		Element p, q;
		
		Connection c;
		
		prepareRootElement(root);
		d.appendChild(root);
		
		p = d.createElement("D:prop");
		root.appendChild(p);
		p.appendChild(d.createElement("D:getetag"));
		p.appendChild(d.createElement("C:calendar-data"));
		
		p = d.createElement("C:filter");
		root.appendChild(p);
		q = d.createElement("C:comp-filter");
		q.setAttribute("name", "VCALENDAR");
		p.appendChild(q);
		p = d.createElement("C:comp-filter");
		p.setAttribute("name", filterType);
		q.appendChild(p);
		q = d.createElement("C:time-range");
		q.setAttribute("start", startTime);
		q.setAttribute("end", endTime);
		p.appendChild(q);
		
		try {
			c = newRawConnection();
			return parseMultigetEntries(c.request("", "REPORT", documentToString(d), depth1));
		} catch (Exception e) {
			ErrorHelper.addErrMessage(e);
			return null;
		}
	}
	
	/*
	String s[][] = cs.requestEntriesByUID("008b-3f8f", "33536-ad62d");
	for (int i = 0; i < s.length; i++) {
		System.out.println(s[i][0]+" "+s[i][2]);
	}
	*/
	public String[][] requestEntriesByUID(String... uid) {
		Document d = newDocument();
		Element root = d.createElement("calendar-multiget");
		Element p;
		
		Connection c;
		
		int i;
		
		prepareRootElement(root);
		d.appendChild(root);
		
		p = d.createElement("D:prop");
		root.appendChild(p);
		p.appendChild(d.createElement("D:getetag"));
		p.appendChild(d.createElement("C:calendar-data"));
		
		for (i = 0; i < uid.length; i++) {
			p = d.createElement("D:href");
			//p.setTextContent(baseUri+uid[i]+".ics");
			XmlHelper.setTextContent(p, baseUri+uid[i]+".ics");
			root.appendChild(p);
		}
		
		try {
			c = newRawConnection();
			return parseMultigetEntries(c.request("", "REPORT", documentToString(d), depth1));
		} catch (Exception e) {
			ErrorHelper.addErrMessage(e);
			return null;
		}
	}
	
	public String requestEtag(String uid) {
		String s[][];
		s = requestEntriesByUID(uid);
		if (s != null && s.length > 0 && s[0].length > 1) {
			return s[0][2];
		} else {
			return null;
		}
	}
	
	private String[][] parseMultigetEntries(InputStream is) throws Exception {
		Document d;
		NodeList nl;
		Node n;
		String returnArray[][];
		int i;
		
		d = db.parse(is);
		//nl = xpath("//multistatus/response", d);
		nl = findNodes("response", d);
		if (nl.getLength() == 0) {
			return null;
		} else {
			returnArray = new String[nl.getLength()][];
			for (i = 0; i < nl.getLength(); i++) {
				n = nl.item(i);
				returnArray[i] = new String[3];
				/*
				//todo: check why xpath on nl.item(i) doesn't work like expected
				//      --> at least in original java dom implementation
				//      not todo in current non-xpath-implementation
				j = i+1;
				returnArray[i][0] = getNodeContent(d, "//response["+i+"]/href");
				returnArray[i][1] = getNodeContent(d, "//response["+i+"]/propstat/prop/calendar-data");
				returnArray[i][2] = getNodeContent(d, "//response["+i+"]/propstat/prop/getetag");
				*/
				returnArray[i][0] = getNodeContent(n, "href");
				returnArray[i][1] = getNodeContent(n, "calendar-data");
				returnArray[i][2] = getNodeContent(n, "getetag");
			}
			return returnArray;
		}
	}
	
	/*
	String newIcsEntry = "BEGIN:VCALENDAR\nPRODID:-//e7.de//caldav sync//EN\nVERSION:2.0\nCALSCALE:GREGORIAN\nBEGIN:VEVENT\nUID:abc123-abaa-bccd-2288\nSUMMARY:Testeintrag\nDTSTART:20100814T090000Z\nDTEND:20100814T100000Z\nDTSTAMP:20100713T11000Z\nEND:VEVENT\nEND:VCALENDAR";
	System.out.println(cs.addEntry(newIcsEntry, "abc123-abaa-bccd-2288") ? "true" : "false");
	*/
	public boolean updateEntry(String ics, String uid) {
		return addEntry(ics, uid);
	}
	
	public boolean addEntry(String ics, String uid) {
		Connection c;
		try {
			c = newRawConnection();
			c.request(uid+".ics", "PUT", ics, noFurtherHeader);
			if (c.lastCode == 201) {
				lastEtag = null;
				for (Header h : c.lastHeaders) {
					if (h.getName().equalsIgnoreCase("Etag")) {
						lastEtag = h.getValue();
						if (lastEtag.contains("\"")) lastEtag = lastEtag.replace("\"", "");
						break;
					}
				}
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			ErrorHelper.addErrMessage(e);
			return false;
		}
	}
	
	// cs.deleteEntry("abc123-abaa-bccd-2288", "cf820e100f4d5af26fda34f0d8d99ffdc042ebc2");
	public boolean deleteEntry(String uid, String etag) {
		Connection c;
		try {
			c = newRawConnection();
			c.request(uid+".ics", "DELETE", "", new String[]{"If-Match", etag});
			if (c.lastCode == 204) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			ErrorHelper.addErrMessage(e);
			return false;
		}
	}
}