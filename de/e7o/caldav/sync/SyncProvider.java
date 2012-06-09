package de.e7o.caldav.sync;

import de.e7o.sqlite.*;
import android.content.SharedPreferences;

public interface SyncProvider {
	public SharedPreferences getPreferences();
	public SQLiteManager getSQLiteManager();
	public SQLiteTable getTableAppdata();
	public SQLiteTable getTableSyncdata();
	
	public String getSavedAppdata(String param);
	public void writeAppdata(String param, String newValue);
}