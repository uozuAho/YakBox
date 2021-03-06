package aho.uozu.yakbox.tests;

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import java.util.Random;

import aho.uozu.android.audio.AudioBuffer;
import aho.uozu.android.audio.AudioRecorder;
import aho.uozu.yakbox.LoadActivity;

/**
 * Try to cause audio errors by thrashing the audio system.
 * Required since there is at least one hard to reproduce bug
 * that sometimes causes AudioRecord to fail to initialise,
 * and then continue to fail until an OS restart.
 *
 * LoadActivity is used, since it doesn't grab audio resources
 * that I want to test.
 */
public class AudioBreaker
        extends ActivityInstrumentationTestCase2<LoadActivity> {

    private AudioRecorder mAudioRecorder;
    private AudioBuffer mAudioBuffer;
    private Random mRandom;

    private static final int MAX_RECORD_TIME_S = 20;
    private static final int TEST_ITERATIONS = 100;
    private static final String TAG = "AudioBreaker";

    public AudioBreaker() {
        super(LoadActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        mAudioRecorder = new AudioRecorder(MAX_RECORD_TIME_S);
        mAudioBuffer = new AudioBuffer(mAudioRecorder.getBufferSizeSamples());
        mRandom = new Random();
        mRandom.setSeed(SystemClock.uptimeMillis());
    }

    @Override
    public void tearDown() throws Exception {
        if (mAudioRecorder != null) {
            mAudioRecorder.release();
        }
    }

    public void testRecordAndRead() {
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Log.d(TAG, "-------------------------------------------------");
            Log.d(TAG, "testRecordAndRead " + i);
            mAudioRecorder.startRecording();
            randomSleep(5, 50);
            mAudioRecorder.stopRecording();
            mAudioBuffer.resetIdx();
            mAudioRecorder.read(mAudioBuffer);
        }
    }

    public void testCreateAndDestroy() {
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Log.d(TAG, "-------------------------------------------------");
            Log.d(TAG, "testCreateAndDestroy " + i);
            mAudioRecorder.release();
            mAudioRecorder = null;
            try {
                mAudioRecorder = new AudioRecorder(MAX_RECORD_TIME_S);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail("Interrupted");
            }
            mAudioRecorder.startRecording();
            randomSleep(5, 50);
            mAudioRecorder.stopRecording();
            mAudioBuffer.resetIdx();
            mAudioRecorder.read(mAudioBuffer);
        }
    }

    private void randomSleep(int minMillis, int maxMillis) {
        sleep(mRandom.nextInt(maxMillis - minMillis) + minMillis);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
