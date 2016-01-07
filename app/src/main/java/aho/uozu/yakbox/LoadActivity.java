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
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
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

    private ListView mListView;
    /** All saved recordings. Used as a cache to reduce file system usage */
    private List<String> mAllRecordings;
    /** Recordings to show in the list view (some may be filtered out by search term) */
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
        updateAllRecordingsList();

        mViewRecordings = new ArrayList<>(mAllRecordings);

        // Configure list view
        mListView = (ListView) findViewById(R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
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

        // multi-select
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {

            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.menu_load_context, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_delete:
                        showDeleteDialog(getSelectedViewIds(), new Runnable() {
                            @Override
                            public void run() {
                                actionMode.finish();
                            }
                        });
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
    }

    /**
     * Update {@link #mAllRecordings}
     */
    private void updateAllRecordingsList() {
        try {
            mAllRecordings = mStorage.getSavedRecordingNames();
        } catch (Storage.StorageUnavailableException e) {
            String msg = "Error: Can't access storage";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            mAllRecordings = new ArrayList<>();
        }
        Collections.sort(mAllRecordings);
    }

    /**
     * Get the currently selected item indices in {@link #mViewRecordings}.
     *
     * This list only remains valid while the view list remains unchanged.
     */
    private List<Integer> getSelectedViewIds() {
        List<Integer> selectedList = new ArrayList<>();
        SparseBooleanArray selected = mListView.getCheckedItemPositions();
        for (int i = 0; i < mViewRecordings.size(); i++) {
            if (selected.get(i)) {
                selectedList.add(i);
            }
        }
        return selectedList;
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

    private void showDeleteDialog(final List<Integer> idxs, final Runnable onDeleteConfirm) {
        Log.d(TAG, idxs.toString());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String msg;
        if (idxs.size() == 1) {
            String name = mViewRecordings.get(idxs.get(0));
            msg = getString(R.string.delete_dialog_msg) + " " + name + "?";
        } else {
            // TODO: string resource
            msg = getString(R.string.delete_dialog_msg) + " selected yaks?";
        }

        builder .setMessage(msg)
                .setPositiveButton(R.string.delete_dialog_positive,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                deleteRecordings(idxs);
                                onDeleteConfirm.run();
                            }
                        })
                .setNegativeButton(R.string.delete_dialog_negative,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
        builder.show();
    }

    private boolean deleteRecording(int itemIdx) {
        boolean deleted = false;
        String name = mViewRecordings.get(itemIdx);
        try {
            deleted = mStorage.deleteRecording(name);
        }
        catch (IOException e) {
            Log.e(TAG, "Error deleting " + name, e);
        }
        return deleted;
    }

    /**
     * Delete recordings by index in {@link #mViewRecordings}
     */
    private void deleteRecordings(List<Integer> idxs) {
        List<String> removeFromView = new ArrayList<>();
        for (int idx : idxs) {
            if (deleteRecording(idx)) {
                removeFromView.add(mViewRecordings.get(idx));
            }
        }
        mViewRecordings.removeAll(removeFromView);
        mAdapter.notifyDataSetChanged();
        updateAllRecordingsList();
    }
}
