package aho.uozu.yakbox;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

public class LoadActivity extends AppCompatActivity {

    /** All recordings at the time this activity was created */
    private List<String> mAllRecordings;
    /** Recordings to give to the ListView */
    private List<String> mViewRecordings;
    private ArrayAdapter<String> mAdapter;
    private Storage mStorage;

    private static final String TAG = "Yakbox-Load";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.ic_launcher_sml);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mStorage = Storage.getInstance(this);
        try {
            mAllRecordings = mStorage.getSavedRecordingNames();
        } catch (Storage.StorageUnavailableException e) {
            String msg = "Error: Can't access storage";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            mAllRecordings = new ArrayList<>();
        }
        Collections.sort(mAllRecordings);

        mViewRecordings = new ArrayList<>(mAllRecordings);

        // Configure list view
        ListView mListView = (ListView) findViewById(R.id.list);
        mAdapter = new ArrayAdapter<>(this, R.layout.load_list_item, mViewRecordings);
        mListView.setAdapter(mAdapter);

        // short taps
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        mListView.setLongClickable(true);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDeleteDialog(position);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_load, menu);

        // configure search
        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterViewList(newText);
                mAdapter.notifyDataSetChanged();
                return true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                filterViewList("");
                return false;
            }
        });

        return true;
    }

    /**
     * Filter the view list for items containing the given pattern
     */
    private void filterViewList(String pattern) {
        if (pattern.isEmpty()) {
            mViewRecordings.clear();
            mViewRecordings.addAll(mAllRecordings);
        }
        else {
            mViewRecordings.clear();
            for (String item : mAllRecordings) {
                if (item.contains(pattern)) {
                    mViewRecordings.add(item);
                }
            }
        }
    }

    private void showDeleteDialog(final int itemIdx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String name = mAllRecordings.get(itemIdx);

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
        String name = mAllRecordings.get(itemIdx);
        mAllRecordings.remove(itemIdx);
        mAdapter.notifyDataSetChanged();
        try {
            mStorage.deleteRecording(name);
        }
        catch (IOException e) {
            Log.e(TAG, "Error deleting " + name, e);
        }
    }
}
