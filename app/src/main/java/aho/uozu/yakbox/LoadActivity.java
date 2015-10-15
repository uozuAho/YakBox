package aho.uozu.yakbox;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import aho.uozu.yakbox.R;
import aho.uozu.yakbox.Storage;

public class LoadActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a new Adapter containing a list of colors
        // Set the adapter on this ListActivity's built-in ListView
        setListAdapter(new ArrayAdapter<>(this, R.layout.load_list_item,
                Storage.getSavedRecordingNames(this)));

        ListView lv = getListView();

        // Enable filtering when the user types in the virtual keyboard
        lv.setTextFilterEnabled(true);

        // Set an setOnItemClickListener on the ListView
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String recordingName = ((TextView) view).getText().toString();
                Intent i = new Intent();
                i.putExtra("name", recordingName);
                setResult(RESULT_OK, i);
                finish();
            }
        });
    }
}
