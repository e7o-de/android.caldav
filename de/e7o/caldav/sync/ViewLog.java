package de.e7o.caldav.sync;

import de.e7o.helper.ErrorHelper;
import de.e7o.helper.GuiHelper;

import android.app.*;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;

public class ViewLog extends ListActivity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Activity baseActivity = this;
		
		ListAdapter la = new ArrayAdapter<String>(this, R.layout.listviewitem, ErrorHelper.errMessages);
		setListAdapter(la);
		
		ListView lv = getListView();
		
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				GuiHelper.copyToClipboard(baseActivity, ((TextView)view).getText());
				GuiHelper.showToast(baseActivity, baseActivity.getString(R.string.log_infocopy));
				return false;
			}
		});
		
		// TODO: Menu for "copy all to clipboard" or something like this
		// TODO: Menu "clear log"
	}
}
