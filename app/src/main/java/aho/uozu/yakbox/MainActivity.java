package aho.uozu.yakbox;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import org.acra.ACRA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends Activity {

    // UI elements
    private Button mBtnSay = null;
    private Button mBtnPlay = null;
    private Button mBtnYalp = null;
    private SeekBar mSkbSpeed = null;

    // sound recorder, buffer & player
    private AudioRecorder mRecorder = null;
    private AudioBuffer mBuffer = null;
    private AudioPlayer mPlayer = null;

    // recording delay data
    private long mLastRecordEndMillis = 0;
    private boolean mIsRecording = false;

    private boolean mShowedVolumeWarningOnPlay = false;

    // constants
    private static final String TAG = "YakBox";
    private static final String BUFFER_FILENAME = "yakbox-sound.bin";
    private static final int MAX_RECORD_TIME_S = 5;
    // Delay between end of last recording and next recording
    private static final int RECORD_WAIT_MS = 300;
    private static final double PLAYBACK_SPEED_MIN = 0.333;
    private static final double PLAYBACK_SPEED_MAX = 3.0;
    private static final double LOW_VOLUME = 0.33;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI controls
        mBtnSay = (Button) findViewById(R.id.button_say);
        mBtnPlay = (Button) findViewById(R.id.button_play);
        mBtnYalp = (Button) findViewById(R.id.button_yalp);
        mSkbSpeed = (SeekBar) findViewById(R.id.skb_speed);

        // set speed slider to half-way
        mSkbSpeed.setProgress(mSkbSpeed.getMax() / 2);

        // record ('say') button listener
        mBtnSay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRecording();
                        break;
                    case MotionEvent.ACTION_UP:
                        stopRecording();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        // play button listener
        mBtnPlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        playForward();
                        mBtnPlay.setBackgroundResource(R.drawable.round_button_grey_dark);
                        break;
                    case MotionEvent.ACTION_UP:
                        mBtnPlay.setBackgroundResource(R.drawable.round_button_grey);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        // 'yalp' button listener
        mBtnYalp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        playReverse();
                        mBtnYalp.setBackgroundResource(R.drawable.round_button_grey_dark);
                        break;
                    case MotionEvent.ACTION_UP:
                        mBtnYalp.setBackgroundResource(R.drawable.round_button_grey);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // init audio recorder and player
        try {
            mRecorder = new AudioRecorder(MAX_RECORD_TIME_S);
            mPlayer = new AudioPlayer(mRecorder.getSampleRate(),
                    mRecorder.getBufferSizeSamples());
            mBuffer = new AudioBuffer(mRecorder.getBufferSizeSamples());

            // set 'say' button back to grey if record buffer is full
            mRecorder.setOnBufferFullListener(new AudioRecorder.OnBufferFullListener() {
                @Override
                public void onBufferFull() {
                    mBtnSay.setBackgroundResource(R.drawable.round_button_grey);
                }
            });
        }
        catch (Exception e) {
            releaseAudioResources();
            // TODO: read logcat here for audio problems.
            //       Looks like reading the log is not allowed since android 4.1
            //       Any way around this...?
            ACRA.getErrorReporter().handleException(e, true);
        }

        // init audio buffer
        if (mBuffer != null) {
            try {
                File f = new File(getFilesDir(), BUFFER_FILENAME);
                mBuffer.loadFromFile(f);
            } catch (FileNotFoundException e) {
                // do nothing - it's OK if there's no existing sound file
            } catch (IOException e) {
                Log.e(TAG, "Error loading saved buffer", e);
            }
        }

        // make volume buttons adjust music stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        lowVolumeWarningIfNecessary();
    }

    /**
     * Notify user if volume is lower than LOW_VOLUME
     */
    private void lowVolumeWarningIfNecessary() {
        //getApplicationContext();
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max_vol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int min_vol = (int) (LOW_VOLUME * max_vol);
        String warn_txt = getString(R.string.low_volume_warning);
        if (vol < min_vol) {
            Toast.makeText(getApplicationContext(), warn_txt,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void startRecording() {
        long interval = SystemClock.elapsedRealtime() - mLastRecordEndMillis;
        if (!mIsRecording && interval > RECORD_WAIT_MS) {
            Log.d(TAG, "recording START");
            mRecorder.startRecording();
            mBtnSay.setBackgroundResource(R.drawable.round_button_red);
            mIsRecording = true;
        }
    }

    private void stopRecording() {
        if (mIsRecording) {
            Log.d(TAG, "recording STOP");
            mBtnSay.setBackgroundResource(R.drawable.round_button_grey);
            mRecorder.stopRecording();

            // move recording from recorder to audio mBuffer
            mBuffer.mNumSamples = mRecorder.read(mBuffer);
            Log.d(TAG, String.format("%d samples copied to buffer", mBuffer.mNumSamples));

            mLastRecordEndMillis = SystemClock.elapsedRealtime();
            mIsRecording = false;
        }
    }

    private void playForward() {
        if (!mShowedVolumeWarningOnPlay) {
            lowVolumeWarningIfNecessary();
            mShowedVolumeWarningOnPlay = true;
        }
        mPlayer.play(mBuffer, getPlaybackSpeed());
    }

    private void playReverse() {
        if (!mShowedVolumeWarningOnPlay) {
            lowVolumeWarningIfNecessary();
            mShowedVolumeWarningOnPlay = true;
        }
        mBuffer.reverse();
        mPlayer.play(mBuffer, getPlaybackSpeed());
        mBuffer.reverse();
    }

    /**
     * Get the playback speed based on the slider position.
     * @return A double in the range [PLAYBACK_SPEED_MIN, PLAYBACK_SPEED_MAX]
     */
    private double getPlaybackSpeed() {
        // Different scales for (min, 1.0) and (1.0, max).
        // This keeps 1.0x speed at the middle of the slider.
        double range_lo = 1.0 - PLAYBACK_SPEED_MIN;
        double range_hi = PLAYBACK_SPEED_MAX - 1.0;
        double slider_pos = getSliderPos();
        if (slider_pos < 0.5) {
            return slider_pos * 2 * range_lo + PLAYBACK_SPEED_MIN;
        }
        else {
            return (slider_pos - 0.5) * 2 * range_hi + 1.0;
        }
    }

    /**
     * Get position of the slider as a fraction.
     * @return Slider position, within range [0, 1.0]
     */
    private double getSliderPos() {
        return ((double) mSkbSpeed.getProgress()) / mSkbSpeed.getMax();
    }

    private void saveRecording() {

    }

    private void loadRecording() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBuffer != null) {
            try {
                File f = new File(getFilesDir(), BUFFER_FILENAME);
                mBuffer.saveToFile(f);
            } catch (IOException e) {
                Log.e(TAG, "Error saving buffer to file", e);
            }
        }
        releaseAudioResources();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseAudioResources();
    }

    private void releaseAudioResources() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_save:
                saveRecording();
                return true;
            case R.id.action_load:
                loadRecording();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
