package aho.uozu.yakbak;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;


public class MainActivity extends Activity {

    // UI elements
    private Button mBtnSay = null;
    private Button mBtnPlay = null;
    private Button mBtnYalp = null;
    private SeekBar mSkbSpeed = null;

    // sound recorder & player
    private AudioRecord mRecorder = null;
    private AudioTrack mPlayer = null;

    // audio buffer - short for 16 bit PCM
    private short[] mBuffer = null;
    private int mSamplesInBuffer = 0;

    // constants
    private static final String TAG = "YakBak";
    private static final int MAX_RECORD_TIME_S = 2;
    private static final int SAMPLE_RATE_HZ_MAX =
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) * 2;
    private static final int BUFFER_SIZE_SAMPLES =
            MAX_RECORD_TIME_S * SAMPLE_RATE_HZ_MAX;
    private static final int RECORD_SAMPLE_RATE_HZ = SAMPLE_RATE_HZ_MAX / 4;
    private static final double PLAYBACK_SPEED_MIN = 0.3;
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

        // audio buffer
        mBuffer = new short[BUFFER_SIZE_SAMPLES];

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
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playForward();
            }
        });

        mBtnYalp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playReverse();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: check recorder and player state after initialisation.
        // Not all devices may support the given sampling rate.
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORD_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_SAMPLES);
        mPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, RECORD_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_SAMPLES, AudioTrack.MODE_STATIC);
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

        // move recording from recorder to audio buffer
        flush();
        mSamplesInBuffer = mRecorder.read(mBuffer, 0, BUFFER_SIZE_SAMPLES);
        Log.d(TAG, String.format("%d samples copied to buffer", mSamplesInBuffer));
    }

    // TODO:
    private void playForward() {
        int playback_rate_hz = getPlaybackSamplingRate();
        Log.d(TAG, String.format("playing sample at %d hz", playback_rate_hz));
        mPlayer.stop();
        mPlayer.write(mBuffer, 0, mSamplesInBuffer);
        mPlayer.reloadStaticData();
        mPlayer.setPlaybackRate(playback_rate_hz);
        mPlayer.play();
    }

    private void playReverse() {
        int playback_rate_hz = getPlaybackSamplingRate();
        Log.d(TAG, String.format("playing sample at %d hz", playback_rate_hz));
        mPlayer.stop();
        reverseBuffer();
        mPlayer.write(mBuffer, 0, mSamplesInBuffer);
        reverseBuffer();
        mPlayer.reloadStaticData();
        mPlayer.setPlaybackRate(playback_rate_hz);
        mPlayer.play();
    }

    /**
     * Write zeroes to the output buffer.
     * Use this to delete the current sample - otherwise if a newly
     * recorded sample is shorter than the current one, you'll hear
     * the end of the current one.
     */
    private void flush() {
        for (int i = 0; i < BUFFER_SIZE_SAMPLES; i++) {
            mBuffer[i] = 0;
        }
        mPlayer.write(mBuffer, 0, BUFFER_SIZE_SAMPLES);
    }

    /**
     * In-place reverse the local audio buffer
     */
    private void reverseBuffer() {
        short temp;
        for (int i = 0; i < mSamplesInBuffer / 2; i++) {
            temp = mBuffer[i];
            mBuffer[i] = mBuffer[mSamplesInBuffer - i];
            mBuffer[mSamplesInBuffer - i] = temp;
        }
    }

    /**
     * Get the playback speed based on the slider position.
     * @return A double in the range [PLAYBACK_SPEED_MIN, PLAYBACK_SPEED_MAX]
     */
    private double getPlaybackSpeed() {
        double range = PLAYBACK_SPEED_MAX - PLAYBACK_SPEED_MIN;
        double slider_frac = ((double) mSkbSpeed.getProgress()) / mSkbSpeed.getMax();
        return slider_frac * range + PLAYBACK_SPEED_MIN;
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
        // release resources
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
