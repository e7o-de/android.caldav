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

import java.util.*;

import android.app.Activity;
import android.content.SharedPreferences;

import de.e7o.caldav.caldav.*;
import de.e7o.android.calendar.*;
import de.e7o.helper.*;
import de.e7o.sqlite.*;

/*
sp.getString("cdpass", "default-pass")
sp.getBoolean("sytest", false)
*/

/*
 * TODO
 * - https
 * - ganztägig: werden an zwei tagen angezeigt
 * - zeitzone (+2)
 * - sync an sich
 * - ErrorHelper anzeigen
*/

public class Syncing {
	Activity baseActivity;
	SyncObserver syncObs;
	SyncProvider syncPro;
	SharedPreferences prefs;
	
	public Syncing(Activity base, SyncObserver so, SyncProvider pr) {
		baseActivity = base;
		syncObs = so;
		syncPro = pr;
		prefs = pr.getPreferences();
	}
	
	public void startSync() {
		try {
			startSyncIntern();
		} catch (Exception e) {
			ErrorHelper.addErrMessage(e);
			syncObs.syncError(ErrorHelper.lastErr);
		}
	}
	
	private void startSyncIntern() throws Exception {
		CaldavServer cs;
		CalendarManager cm;
		HashMap<String, Event> hml, hms;
		String serverEvents[][];
		Event e = null;
		LinkedList<Event> writeToServer, writeToClient, removeFromServer, removeFromClient;
		String ctag;
		String s[];
		String sh;
		Date d, dto, dfrom;
		SQLiteTable slt = syncPro.getTableSyncdata();
		SQLiteManager sm = syncPro.getSQLiteManager();
		SQLiteResult sr;
		
		int i;
		boolean bo, onlyClient;
		
		// Connect to server
		// TODO: catch "no connection to server" etc.
		try {
			cs = new CaldavServer(
				prefs.getString("cduri", null),
				prefs.getString("cduser", null),
				prefs.getString("cdpass", null)
			);
		} catch (Exception ex) {
			ErrorHelper.addErrMessage(ex);
			syncObs.syncError(ex.getClass().getSimpleName());
			return;
		}
		
		// Check ctag
		ctag = cs.requestCTag();
		
		if (ctag == null) {
			syncObs.syncError("Can't fetch ctag");
			return;
		}
		
		if (ctag.equals(syncPro.getSavedAppdata("sync_lastctag"))) {
			syncObs.syncState("No changes on server (same ctag)");
			onlyClient = true;
		} else {
			syncObs.syncState("ctag changed to "+ctag);
			onlyClient = false;
		}
		
		// Prepare
		d = TimeHelper.currentTime();
		i = Integer.parseInt(prefs.getString("syrange", "0"));
		switch (i) {
			case 1: // Week
				dto = TimeHelper.addTime(d, 7);
				break;
			case 2: // Month
				dto = TimeHelper.addTime(d, 31);
				break;
			case 4: // Half Year
				dto = TimeHelper.addTime(d, 182);
				break;
			case 5: // Year
				dto = TimeHelper.addTime(d, 365);
				break;
			case 6: // All
				//dto = TimeHelper.mkDate(2345, 1, 23, 4, 5, 6);
				// Use max 32 bit int to avoid server problems...
				dto = TimeHelper.mkDate(2037, 11, 24, 4, 5, 6);
				break;
			case 3: // Quarter Year
			default:
				dto = TimeHelper.addTime(d, 91);
				break;
		}
		
		dfrom = TimeHelper.addTime(d, -21);
		
		syncObs.syncState("Syncing from "+dfrom.toString()+" to "+dto.toString());
		
		// Read local entries
		cm = new CalendarManager(baseActivity, syncPro);
		
		de.e7o.android.calendar.Calendar c = cm.getCalendar(1);
		
		if (prefs.getBoolean("caall", false)) {
			hml = cm.getEvents(dfrom, dto);
			syncObs.syncState("Syncing all calendars");
		} else {
			hml = c.getEvents(dfrom, dto);
		}
		
		// Prepare compare
		writeToServer = new LinkedList<Event>();
		writeToClient = new LinkedList<Event>();
		removeFromServer = new LinkedList<Event>();
		removeFromClient = new LinkedList<Event>();
		
		// Full sync?
		if (!onlyClient) {
		////////////////////////////////////////////////////////////////////////////
			// Read server entries
			serverEvents = cs.requestEntriesByTime(
				TimeHelper.mkCaldavDateString(dfrom),
				TimeHelper.mkCaldavDateString(dto),
				"VEVENT"
			);
			
			hms = new HashMap<String, Event>();
			if (serverEvents == null) {
				syncObs.syncState("No events on server");
			} else {
				for (i = 0; i < serverEvents.length; i++) {
					e = new Event(c, serverEvents[i][1]);
					e.etag = serverEvents[i][2];
					hms.put(e.val_guid, e);
				}
			}
			// Screen output
			if (serverEvents == null) {
				syncObs.syncState("Events: "+hml.size()+" C");
			} else {
				syncObs.syncState("Events: "+serverEvents.length+" S, "+hml.size()+" C");
			}
			
			// Compare: Server to local
			for (Event eServer : hms.values()) {
				e = hml.get(eServer.val_guid);
				
				if (e == null) {
					// Isn't local - why?
					sr = sm.query("SELECT syncid, etag FROM syncdata WHERE icalguid=\""+eServer.val_guid+"\"");
					if (sr.length() > 0) {
						// Known, deleted locally
						// --> delete on server
						s = sr.next();
						eServer.etag = s[1];
						eServer.isSynced = true;
						removeFromServer.add(eServer);
					} else {
						// Unknown - new on server
						// --> add to client as new
						eServer.isExistingInContentProvider = false;
						eServer.isSynced = true;
						writeToClient.add(eServer);
					}
					
				} else {
					// There is a local equivalent
					sr = sm.query("SELECT syncid, etag, androidhash FROM syncdata WHERE icalguid=\""+eServer.val_guid+"\"");
					if (sr.length() > 0) {
						// Well-known
						s = sr.next();
						if (s[1].equalsIgnoreCase(eServer.etag)) {
							// Same etag as previous sync
							// --> No changes on server
							if (s[2].equalsIgnoreCase(e.getHash())) {
								// No changes on client since last sync
								// --> Nothing todo :)
								e.isSynced = true;
								eServer.isSynced = true;
							} else {
								// Changes on client
								// --> update on server
								e.isExistingOnServerSite = true;
								e.isSynced = true;
								eServer.isSynced = true;
								writeToServer.add(e);
							}
						} else {
							// Server has changed
							eServer.isExistingOnServerSite = true;
							eServer.isSynced = true;
							eServer.val_id = e.val_id;
							writeToClient.add(e);
							// Conflict: no client sync
							e.isSynced = true;
							ErrorHelper.addErrMessage("SYNC conflict between "+e.toString()+" and "+eServer.toString());
						}
					} else {
						// Same UID, but not known...
						// --> Bug
						// --> Set result from server to local event
						ErrorHelper.addErrMessage("Maybee a bug: No local information about existing events");
						eServer.isExistingInContentProvider = true;
						e.isSynced = true;
						eServer.isSynced = true;
						writeToClient.add(eServer);
					}
				}
			}
			
			// Compare: Local to server
			for (Event eClient : hml.values()) {
				// Maybee already done...
				if (eClient.isSynced) continue;
				// No, search for server equivalent
				e = hms.get(eClient.val_guid);
				// Found?
				if (e == null) {
					// Isn't on server - why?
					sr = sm.query("SELECT syncid, androidhash FROM syncdata WHERE icalguid=\""+eClient.val_guid+"\"");
					if (sr.length() > 0) {
						// Known UID, but not on server anymore
						// --> remove from client
						removeFromClient.add(eClient);
					} else {
						// Unknown entry, never seen, not on server
						// --> upload to server
						eClient.isExistingOnServerSite = false;
						writeToServer.add(eClient);
					}
				} else if (e.isSynced == false) {
					// Found something on client with an UID which is known by server
					// but hasn't synced in other comparing part
					// --> maybee a bug
					// --> do nothing with it
					ErrorHelper.addErrMessage("Maybee a bug: Conflict between local and remote information (event on "+e.val_dtstart.toGMTString()+")");
				}
			}
		} else { // if (!onlyClient) {
		////////////////////////////////////////////////////////////////////////////
			// onlyClient --> only from client to server
			
			// Looking for new local events
			for (Event eClient : hml.values()) {
				// Is this event known?
				sr = sm.query("SELECT syncid, androidhash FROM syncdata WHERE icalguid=\""+eClient.val_guid+"\"");
				if (sr.length() > 0) {
					// Known UID
					s = sr.next();
					// Changed?
					if (s[1].equalsIgnoreCase(eClient.getHash())) {
						// No.
					} else {
						// Yes
						// --> upload to server
						eClient.isExistingOnServerSite = false;
						writeToServer.add(eClient);
					}
				} else {
					// Unknown UID
					// --> upload to server
					eClient.isExistingOnServerSite = false;
					writeToServer.add(eClient);
				}
			}
			
			// Looking for changed and deleted events
			sr = sm.query("SELECT syncid, icalguid, androidhash, etag FROM syncdata WHERE status<>1");
			if (sr.length() > 0) {
				// There are known events
				for (i = 0; i < sr.length(); i++) {
					s = sr.next();
					// Find in current events' list equivalent
					e = hml.get(s[1]);
					if (e == null) {
						// There's none... maybee deleted locally :)
						// --> remove it on server, too
						e = new Event();
						e.etag = s[3];
						e.val_guid = s[1];
						e.isExistingInContentProvider = false;
						e.isExistingOnServerSite = true;
						removeFromServer.add(e);
					} else {
						// It's there. Check if changed...
						if (s[2].equalsIgnoreCase(e.getHash())) {
							// No changes
						} else {
							// Changed
							// --> update on server
							writeToServer.add(e);
						}
					}
				}
			}
		} // if (!onlyClient) {
		////////////////////////////////////////////////////////////////////////////
		
		// Do sync to server
		syncObs.syncState("Server: Write");
		for (Event ev : writeToServer) {
			bo = cs.addEntry(ev.getICal(), ev.val_guid);
			if (bo) {
				// Get etag
				sh = cs.lastEtag;
				if (sh == null) {
					// Not saved...
					ErrorHelper.addErrMessage("Server doesn't provide etag in response");
					// Ask server
					sh = cs.requestEtag(ev.val_guid);
					if (sh == null) {
						// Server doesn't say
						ErrorHelper.addErrMessage("Can't fetch etag from server");
						sh = "err";
					}
				}
				ev.etag = sh;
				ev.updateSyncDB();
				//bad idea:
				//sm.query("UPDATE syncdata SET etag=\""+sh+"\", androidhash=\""+ev.getHash()+"\" WHERE icalguid=\""+ev.val_guid+"\"");
			} else {
				// Err while uploading
				// --> don't change androidhash so next time we can try it again
				ErrorHelper.addErrMessage("Can't update "+ev.val_guid+" on server");
			}
		}
		
		syncObs.syncState("Server: Delete");
		for (Event ev : removeFromServer) {
			if (cs.deleteEntry(ev.val_guid, ev.etag)) sm.query("UPDATE syncdata SET status=1 WHERE icalguid=\""+ev.val_guid+"\"");
		}
		
		// Do sync to local
		syncObs.syncState("Client: Write");
		for (Event ev : writeToClient) {
			ev.save();
			// No sm.query needed because cm.updateSyncDB() is called by Event.java
		}
		
		syncObs.syncState("Client: Delete");
		for (Event ev : removeFromClient) {
			ev.delete();
			sm.query("UPDATE syncdata SET status=1 WHERE icalguid=\""+ev.val_guid+"\"");
		}
		
		// Changed? Request new ctag from server
		if (onlyClient && (writeToServer.size() > 0 || removeFromServer.size() > 0)) {
			ctag = cs.requestCTag();
		}
		// Remeber last ctag (if changed)
		syncPro.writeAppdata("sync_lastctag", ctag);
		
		// Done
		GuiHelper.showToast(baseActivity, (writeToServer.size()+removeFromServer.size()+writeToClient.size()+removeFromClient.size())
			+" "+R.string.main_toast_syncresult);
		syncObs.syncDone("Done: "+
			"S +"+writeToServer.size()+" -"+removeFromServer.size()+
			" C +"+writeToClient.size()+" -"+removeFromClient.size()+"\nSync up to "+dto.toString()
		);
	}
}