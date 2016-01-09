package aho.uozu.yakbox.tests;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import com.robotium.solo.Solo;

import aho.uozu.yakbox.LoadActivity;
import aho.uozu.yakbox.R;

public class LoadActivityTest
    extends ActivityInstrumentationTestCase2<LoadActivity> {

    private static final String TAG = "LoadActTest";

    private Solo solo;

    public LoadActivityTest() {
        super(LoadActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
        Utils.deleteAllRecordings(getActivity());
        Utils.saveDummyRecording(getActivity(), "a");
        Utils.saveDummyRecording(getActivity(), "aa");
        Utils.saveDummyRecording(getActivity(), "ab");
        Utils.saveDummyRecording(getActivity(), "ac");
        Utils.saveDummyRecording(getActivity(), "b");
        Utils.saveDummyRecording(getActivity(), "bb");
        Utils.saveDummyRecording(getActivity(), "bba");
        // recreate to populate the listview
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().recreate();
            }
        });
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testSearchFilter() {
        clickSearch();
        solo.enterText(0, "zzz");
        assertFalse(solo.searchText("aa"));
        assertFalse(solo.searchText("bb"));
    }

    public void testDeleteSingle() {
        final String DELETE_ITEM = "bba";
        // select item
        solo.clickLongOnText(DELETE_ITEM);
        // click delete
        clickDelete();
        // assert dialog has appeared
        assertTrue(solo.searchText("Delete", 2));
        assertTrue(solo.searchText("Cancel"));
        // confirm delete
        solo.clickOnText("Delete", 2);
        // assert it's gone
        assertFalse(solo.searchText(DELETE_ITEM));
    }

    public void testDeleteMultiple() {
        final String[] DELETE_ITEMS = new String[] {"ab", "bb", "bba"};
        // select items
        boolean first = true;
        for (String item : DELETE_ITEMS) {
            if (first) {
                solo.clickLongOnText(item);
                first = false;
            } else {
                solo.clickOnText(item);
            }
        }
        // click delete
        clickDelete();
        // assert dialog has appeared
        assertTrue(solo.searchText("Delete", 2));
        assertTrue(solo.searchText("Cancel"));
        // confirm delete
        solo.clickOnText("Delete", 2);
        // assert it's gone
        for (String item : DELETE_ITEMS) {
            assertFalse(solo.searchText(item));
        }
    }

    private void clickSearch() {
        View search = solo.getView(R.id.action_search);
        solo.clickOnView(search);
    }

    /**
     * Click on the delete/trash button that appears when
     * any recording is selected.
     */
    private void clickDelete() {
        View delete = solo.getView(R.id.action_delete);
        solo.clickOnView(delete);
    }
}
