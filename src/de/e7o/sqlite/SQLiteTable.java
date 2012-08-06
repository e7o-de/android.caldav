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

import java.util.LinkedList;
import java.util.Iterator;

public class SQLiteTable {
	LinkedList<SQLiteField> fields;
	String fieldsString = "";
	String myName;
	SQLiteManager sm = null;
	
	public SQLiteTable(String tableName) {
		myName = tableName;
		fields = new LinkedList<SQLiteField>();
	}
	
	public String createQuery() {
		String s = "";
		Iterator<SQLiteField> i;
		SQLiteField f;
		for (i = fields.iterator(); i.hasNext(); ) {
			f = i.next();
			s += ", "+f.fieldName+(f.isNumeric ? " INTEGER" : "")+(f.isPrimary ? " PRIMARY KEY" : "");
		}
		return "CREATE TABLE IF NOT EXISTS "+myName+" ("+s.substring(1)+");";
	}
	
	public void addField(SQLiteField f) {
		fields.add(f);
		if (!f.isPrimary) {
			if (fieldsString.length() > 0)
				fieldsString += ", "+f.fieldName;
			else
				fieldsString += f.fieldName;
		}
	}
	
	public void addRow(String... values) {
		// ACHTUNG: Eingaben werden nicht escaped!
		StringBuilder sb = new StringBuilder();
		int i;
		sb.append("INSERT INTO "+myName+"("+fieldsString+") VALUES (");
		for (i = 0; i < values.length; i++) {
			if (i > 0) sb.append(',');
			sb.append('"'+values[i]+'"');
		}
		sb.append(");");
		sm.execute(sb.toString());
	}
}