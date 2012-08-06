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
package de.e7o.sqlite;

import java.util.HashMap;
import android.app.Activity;
import android.database.sqlite.*;

public class SQLiteManager {
	SQLiteDatabase db;
	Activity baseActivity;
	HashMap<String, SQLiteTable> tables;
	
	public SQLiteManager(Activity a, String dbName) {
		db = a.openOrCreateDatabase(dbName, a.MODE_PRIVATE, null);
		baseActivity = a;
		tables = new HashMap<String, SQLiteTable>();
	}
	
	public void addTable(SQLiteTable tbl) {
		tbl.sm = this;
		tables.put(tbl.myName, tbl);
		execute(tbl.createQuery());
	}
	
	public void addRow(SQLiteTable toTable, String... values) {
		toTable.addRow(values);
	}
	
	public void addRow(String toTable, String... values) {
		addRow(tables.get(toTable), values);
	}
	
	public void execute(String sql) {
		db.execSQL(sql);
	}
	
	public SQLiteResult query(String query) {
		return new SQLiteResult(db, query);
	}
	
	protected void finalize() throws Throwable {
		db.close();
	}
}