package aho.uozu.yakbox.tests;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;

import com.robotium.solo.Solo;

import aho.uozu.yakbox.MainActivity;
import aho.uozu.yakbox.R;

public class MainActivityTest
        extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private MainActivity mMainActivity;
    private Instrumentation mInst;
    private Button mBtnSay;
    private int mOrientation;

    private static final String TAG = "MainActTest";

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
        mMainActivity = getActivity();
        mInst = getInstrumentation();
        mBtnSay = (Button) mMainActivity.findViewById(R.id.button_say);
        mOrientation = Solo.PORTRAIT;
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void test1sRecordAndPlay() {
        sendSayButtonEvent(MotionEvent.ACTION_DOWN);
        solo.sleep(1000);
        sendSayButtonEvent(MotionEvent.ACTION_UP);
        solo.sleep(100);
        solo.clickOnText("Play");
        solo.sleep(1000);
        solo.clickOnText("yalP");
    }

    /**
     * There was a bug once that caused crashes when recording
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
}
