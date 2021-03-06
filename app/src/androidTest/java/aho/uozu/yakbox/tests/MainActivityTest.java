package aho.uozu.yakbox.tests;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.robotium.solo.Solo;

import java.io.IOException;
import java.util.Random;

import aho.uozu.yakbox.MainActivity;
import aho.uozu.yakbox.R;

public class MainActivityTest
        extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private Instrumentation mInst;
    private int mOrientation;

    private static final String TAG = "MainActTest";
    private static final int MAX_RECORD_MS = 20000;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
        mInst = getInstrumentation();
        mOrientation = Solo.PORTRAIT;
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testRecordAndPlayLots() {
        Random r = new Random();
        // TODO:
        // This test takes a while to run. Slow after stop recording.
        // What's causing slowness? I commented out button background
        // update, still took same time (but no skipped frame warning).
        // Possibly IO in UI thread is the cause
        for (int i = 0; i < 5; i++) {
            sendSayButtonEvent(MotionEvent.ACTION_DOWN);
            solo.sleep(100 + r.nextInt(500));
            sendSayButtonEvent(MotionEvent.ACTION_UP);
            solo.sleep(100);
            pressPlay();
            solo.sleep(250 + r.nextInt(200));
            pressYalp();
        }
        Log.d(TAG, "record to full buffer");
        sendSayButtonEvent(MotionEvent.ACTION_DOWN);
        solo.sleep(MAX_RECORD_MS);
        sendSayButtonEvent(MotionEvent.ACTION_UP);
        pressPlay();
        solo.sleep(500);
        pressYalp();
    }

    public void testManyPlaySpeeds() {
        // record something to play
        sendSayButtonEvent(MotionEvent.ACTION_DOWN);
        solo.sleep(500);
        sendSayButtonEvent(MotionEvent.ACTION_UP);

        for (int percent = 0; percent < 100; percent += 10) {
            solo.setProgressBar(0, percent);
            pressPlay();
            solo.sleep(100);
            pressYalp();
            solo.sleep(100);
        }
    }

    /**
     * There was a bug that caused crashes when recording
     * and rotating the screen. Hopefully this test will catch
     * it if comes back.
     */
    public void testRecordRotateLots() {
        for (int i = 0; i < 10; i++) {
            sendSayButtonEvent(MotionEvent.ACTION_DOWN);
            solo.sleep(100);
            toggleOrientation();
            solo.sleep(1000);
            sendSayButtonEvent(MotionEvent.ACTION_UP);
            solo.sleep(500);
        }
    }

    public void testSaveAndLoad() {
        final String TEST_FILENAME = "asdf";

        try {
            Utils.deleteAllRecordings(getActivity());
        } catch (IOException e) {
            Log.e(TAG, "", e);
            fail();
        }

        clickToolbarSave();
        solo.enterText(0, TEST_FILENAME);
        solo.clickOnView(solo.getButton("Save"));

        // check that overwrite warning appears
        clickToolbarSave();
        solo.enterText(0, TEST_FILENAME);
        solo.clickOnView(solo.getButton("Save"));
        String overwriteText = getActivity()
                .getString(R.string.save_confirm_overwrite_message);
        assertTrue(solo.searchText(overwriteText));
        solo.clickOnText("Cancel");
        // check that entered text is retained
        assertTrue(solo.searchText(TEST_FILENAME));
        solo.clickOnText("Cancel");

        // check that saved item appears in load screen
        clickToolbarLoad();
        assertTrue(solo.searchText(TEST_FILENAME));
    }

    /**
     * Click on the save toolbar icon/text (in main activity)
     */
    private void clickToolbarSave() {
        View toolbar = solo.getView(R.id.toolbar);
        View saveView = toolbar.findViewById(R.id.action_save);
        if (saveView != null)
            solo.clickOnView(saveView);
        else
            solo.clickOnMenuItem("Save");
    }

    private void clickToolbarLoad() {
        View toolbar = solo.getView(R.id.toolbar);
        View loadView = toolbar.findViewById(R.id.action_load);
        if (loadView != null)
            solo.clickOnView(loadView);
        else
        solo.clickOnMenuItem("Load");
    }

    private void sendSayButtonEvent(int event) {
        int[] xy = new int[2];
        Button sayButton = solo.getButton("Say");
        sayButton.getLocationOnScreen(xy);
        long t = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(t, t, event, xy[0], xy[1], 0);
        mInst.sendPointerSync(e);
    }

    private void toggleOrientation() {
        if (mOrientation == Solo.PORTRAIT) {
            mOrientation = Solo.LANDSCAPE;
        } else {
            mOrientation = Solo.PORTRAIT;
        }
        solo.setActivityOrientation(mOrientation);
    }

    private void pressPlay() {
        solo.clickOnText("Play");
    }

    private void pressYalp() {
        solo.clickOnText("yalP");
    }
}
