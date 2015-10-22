package aho.uozu.yakbox;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LoadActivity extends ListActivity {

    private List<String> mItems;
    private ArrayAdapter<String> mAdapter;
    private static final String TAG = "Yakbox-Load";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mItems = Storage.getSavedRecordingNames(this);
        Collections.sort(mItems);

        // Create a new Adapter containing a list of colors
        // Set the adapter on this ListActivity's built-in ListView
        mAdapter = new ArrayAdapter<>(this, R.layout.load_list_item, mItems);
        setListAdapter(mAdapter);

        ListView lv = getListView();

        // Enable filtering when the user types in the virtual keyboard
        lv.setTextFilterEnabled(true);

        // short taps
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String recordingName = ((TextView) view).getText().toString();
                Intent i = new Intent();
                i.putExtra("name", recordingName);
                setResult(RESULT_OK, i);
                finish();
            }
        });

        // long taps
        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDeleteDialog(position);
                return true;
            }
        });
    }

    private void showDeleteDialog(final int itemIdx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String name = mItems.get(itemIdx);

        builder .setMessage(getString(R.string.delete_dialog_msg) + " " + name + "?")
                .setPositiveButton(R.string.delete_dialog_positive,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                deleteRecording(itemIdx);
                            }
                        })
                .setNegativeButton(R.string.delete_dialog_negative,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
        builder.show();
    }

    private void deleteRecording(int itemIdx) {
        String name = mItems.get(itemIdx);
        mItems.remove(itemIdx);
        mAdapter.notifyDataSetChanged();
        try {
            Storage.deleteRecording(this, name);
        }
        catch (IOException e) {
            Log.e(TAG, "Error deleting " + name, e);
        }
    }
}
