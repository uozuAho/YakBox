package aho.uozu.yakbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends Activity {

    // UI elements
    private Button mBtnSay = null;
    private Button mBtnPlay = null;
    private Button mBtnYalp = null;
    private SeekBar mSkbSpeed = null;

    // sound recorder & player
    private AudioRecord mRecorder = null;
    private AudioTrack mPlayer = null;

    private AudioBuffer mBuffer = null;

    // constants
    private static final String TAG = "YakBox";
    private static final String BUFFER_FILEPATH = Environment
            .getExternalStorageDirectory().getAbsolutePath() + "/yakbox-sound.bin";
    private static final int MAX_RECORD_TIME_S = 5;
    private static final int SAMPLE_RATE_HZ_MAX =
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) * 2;
    // TODO: try a bunch of sample rates to increase chances of working
    // Sample rate of 8000 works on 4.0 emulator, 22050 doesn't
    private static final int RECORD_SAMPLE_RATE_HZ = SAMPLE_RATE_HZ_MAX / 4;
//    private static final int RECORD_SAMPLE_RATE_HZ = 8000;
    private static final int BUFFER_SIZE_SAMPLES =
            MAX_RECORD_TIME_S * RECORD_SAMPLE_RATE_HZ;
    private static final int BUFFER_SIZE_BYTES = BUFFER_SIZE_SAMPLES * 2;
    private static final double PLAYBACK_SPEED_MIN = 0.333;
    private static final double PLAYBACK_SPEED_MAX = 3.0;

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

        // audio mBuffer
        mBuffer = new AudioBuffer(BUFFER_SIZE_SAMPLES);

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
            initRecorder();
            initPlayer();
        }
        catch (Exception e) {
            Log.e(TAG, "init error", e);
            reportErrorAndClose("Error: couldn't initialise audio. Sorry!");
            // TODO: send error report
        }

        // init audio buffer
        try {
            mBuffer.loadFromFile(BUFFER_FILEPATH);
        }
        catch (FileNotFoundException e) {
            // do nothing - it's OK if there's no existing sound file
        }
        catch (IOException e) {
            Log.e(TAG, "Error loading saved buffer", e);
        }
    }

    private void initRecorder() throws Exception {
        // Not all devices may support the given sampling rate.
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORD_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_BYTES);
        int state = mRecorder.getState();
        if (state == AudioRecord.STATE_INITIALIZED) {
            // Change 'say' button colour to grey if buffer full
            mRecorder.setRecordPositionUpdateListener(
                    new AudioRecord.OnRecordPositionUpdateListener() {
                        @Override
                        public void onMarkerReached(AudioRecord recorder) {
                            mBtnSay.setBackgroundResource(R.drawable.round_button_grey);
                        }

                        @Override
                        public void onPeriodicNotification(AudioRecord recorder) {

                        }
                    });
            mRecorder.setNotificationMarkerPosition(BUFFER_SIZE_SAMPLES);
        }
        else {
            throw new Exception(String.format(
                    "Failed to initialise AudioRecord. State: %d", state));
        }
    }

    private void initPlayer() throws Exception {
        mPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, RECORD_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_BYTES, AudioTrack.MODE_STATIC);
        int state = mPlayer.getState();
        if (state == AudioTrack.STATE_UNINITIALIZED) {
            throw new Exception(String.format(
                    "Failed to initialise AudioTrack. State: %d", state));
        }
    }

    /**
     * Report error to the user (& devs?) & close the app.
     * @param message
     *      Message displayed to the user.
     * @param btnText
     *      Text on the button that closes the report (and app).
     */
    private void reportErrorAndClose(String message, String btnText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(message)
                .setCancelable(false)
                .setNeutralButton(btnText, new DialogInterface.OnClickListener() {
                    public void onClick (DialogInterface dialog, int which) {
                        finish();
                    }
                });
        AlertDialog error = builder.create();
        error.show();
    }

    private void reportErrorAndClose(String message) {
        reportErrorAndClose(message, "Close app");
    }

    private void startRecording() {
        Log.d(TAG, "recording START");
        mRecorder.startRecording();
        mBtnSay.setBackgroundResource(R.drawable.round_button_red);
    }

    private void stopRecording() {
        Log.d(TAG, "recording STOP");
        mBtnSay.setBackgroundResource(R.drawable.round_button_grey);
        mRecorder.stop();

        // move recording from recorder to audio mBuffer
        flush();
        mBuffer.mNumSamples = mRecorder.read(mBuffer.mBuffer, 0, BUFFER_SIZE_SAMPLES);
        Log.d(TAG, String.format("%d samples copied to buffer", mBuffer.mNumSamples));
    }

    private void playForward() {
        play(false);
    }

    private void playReverse() {
        play(true);
    }

    private void play(boolean reverse) {
        int playback_rate_hz = getPlaybackSamplingRate();
        if (mPlayer.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mPlayer.stop();
        }
        if (mBuffer.mNumSamples > 0) {
            Log.d(TAG, String.format("Playing sample at %d hz", playback_rate_hz));
            if (reverse) mBuffer.reverse();
            mPlayer.write(mBuffer.mBuffer, 0, mBuffer.mNumSamples);
            if (reverse) mBuffer.reverse();
            mPlayer.reloadStaticData();
            mPlayer.setPlaybackRate(playback_rate_hz);
            mPlayer.play();
        }
        else {
            Log.d(TAG, "Can't play - buffer empty");
        }
    }

    /**
     * Write zeroes to the output buffer.
     * Use this to delete the current sample - otherwise if a newly
     * recorded sample is shorter than the current one, you'll hear
     * the end of the current one.
     */
    private void flush() {
        mBuffer.clear();
        mPlayer.write(mBuffer.mBuffer, 0, BUFFER_SIZE_SAMPLES);
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

    /**
     * Get the playback sampling rate based on the slider position.
     * @return Sampling rate in Hertz
     */
    private int getPlaybackSamplingRate() {
        return (int) (RECORD_SAMPLE_RATE_HZ * getPlaybackSpeed());
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mBuffer.saveToFile(BUFFER_FILEPATH);
        }
        catch (IOException e) {
            Log.e(TAG, "Error saving buffer to file", e);
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
