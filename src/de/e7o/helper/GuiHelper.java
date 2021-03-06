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

import android.app.Activity;
import android.content.Context;
import android.text.ClipboardManager;
import android.widget.Toast;

public class GuiHelper {
	// TestActivity.showToast(this, "Hello World");
	public static void showToast(Activity a, CharSequence message) {
		Toast.makeText(a.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
	
	public static void copyToClipboard(Activity a, CharSequence message) {
		((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setText(message);
	}
}