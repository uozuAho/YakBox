package aho.uozu.yakbox.tests;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.SeekBar;

import com.robotium.solo.Solo;

import java.util.Random;

import aho.uozu.yakbox.MainActivity;
import aho.uozu.yakbox.R;

public class MainActivityTest
        extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private MainActivity mMainActivity;
    private Instrumentation mInst;
    private Button mBtnSay;
    private Button mBtnPlay;
    private Button mBtnYalp;
    private SeekBar mSkbSpeed;
    private int mOrientation;

    private static final String TAG = "MainActTest";

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
        mInst = getInstrumentation();
        mOrientation = Solo.PORTRAIT;
        initActivityAndButtonVars();
    }

    /** Set activity and button variables */
    private void initActivityAndButtonVars() {
        mMainActivity = getActivity();
        mBtnSay = (Button) mMainActivity.findViewById(R.id.button_say);
        mBtnPlay = (Button) mMainActivity.findViewById(R.id.button_play);
        mBtnYalp = (Button) mMainActivity.findViewById(R.id.button_yalp);
        mSkbSpeed = (SeekBar) mMainActivity.findViewById(R.id.skb_speed);
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
        solo.sleep(5000);
        sendSayButtonEvent(MotionEvent.ACTION_UP);
        pressPlay();
        solo.sleep(500);
        pressYalp();
    }

    public void testManyPlaySpeeds() {
        Random r = new Random();
        int increment = mSkbSpeed.getMax() / 10;

        // record something to play
        sendSayButtonEvent(MotionEvent.ACTION_DOWN);
        solo.sleep(500);
        sendSayButtonEvent(MotionEvent.ACTION_UP);

        for (int i = 0; i < mSkbSpeed.getMax(); i += increment) {
            pressPlay();
            solo.sleep(100 + r.nextInt(100));
            pressYalp();
            mSkbSpeed.setProgress(i);
            solo.sleep(100 + r.nextInt(100));
        }
        // just in case max was missed
        mSkbSpeed.setProgress(mSkbSpeed.getMax());
        pressPlay();
        solo.sleep(100 + r.nextInt(100));
        pressYalp();
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
            // refresh activity and button variables after
            // configuration change
            // TODO: this doesn't work - button coords are still incorrect
            initActivityAndButtonVars();
            sendSayButtonEvent(MotionEvent.ACTION_UP);
            solo.sleep(500);
        }
    }

    private void sendSayButtonEvent(int event) {
        // say button coords. Add a bit since clicking on the
        // returned coordinates doesn't actually trigger
        // the touch listener.
        float x = mBtnSay.getX() + 100;
        float y = mBtnSay.getY() + 100;
        long t = SystemClock.uptimeMillis();
        MotionEvent e = MotionEvent.obtain(t, t, event, x, y, 0);
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
        solo.clickOnView(mBtnPlay);
    }

    private void pressYalp() {
        solo.clickOnView(mBtnYalp);
    }
}
