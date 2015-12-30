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
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadActivity extends ListActivity {

    private List<String> mItems;
    private ArrayAdapter<String> mAdapter;
    private Storage mStorage;

    private static final String TAG = "Yakbox-Load";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorage = Storage.getInstance(this);
        try {
            mItems = mStorage.getSavedRecordingNames();
        } catch (Storage.StorageUnavailableException e) {
            String msg = "Error: Can't access storage";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            mItems = new ArrayList<>();
        }
        Collections.sort(mItems);

        mAdapter = new ArrayAdapter<>(this, R.layout.load_list_item, mItems);
        setListAdapter(mAdapter);

        ListView lv = getListView();

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
            mStorage.deleteRecording(name);
        }
        catch (IOException e) {
            Log.e(TAG, "Error deleting " + name, e);
        }
    }
}
