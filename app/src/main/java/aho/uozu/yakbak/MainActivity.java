package aho.uozu.yakbak;

import android.app.Activity;
import android.app.FragmentManager;
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

    // data to retain on config changes
    private DataFragment mDataFragment;

    // constants
    private static final String TAG = "YakBak";
    private static final int MAX_RECORD_TIME_S = 2;
    private static final int SAMPLE_RATE_HZ_MAX =
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) * 2;
    private static final int BUFFER_SIZE_SAMPLES =
            MAX_RECORD_TIME_S * SAMPLE_RATE_HZ_MAX;
    private static final int RECORD_SAMPLE_RATE_HZ = SAMPLE_RATE_HZ_MAX / 4;
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

        // load audio buffer from fragment, if available
        FragmentManager fm = getFragmentManager();
        mDataFragment = (DataFragment) fm.findFragmentByTag("data");
        if (mDataFragment == null) {
            // no fragment/audio buffer, create both
            mDataFragment = new DataFragment();
            fm.beginTransaction().add(mDataFragment, "data").commit();
            mBuffer = new short[BUFFER_SIZE_SAMPLES];
            Log.d(TAG, "New audio buffer created");
        }
        else {
            mBuffer = mDataFragment.getAudioBuffer();
            mSamplesInBuffer = mDataFragment.getSamplesInBuffer();
            Log.d(TAG, String.format("Audio buffer recovered: %d samples",
                    mSamplesInBuffer));
        }

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
        if (mSamplesInBuffer > 0) {
            Log.d(TAG, String.format("Playing sample at %d hz", playback_rate_hz));
            if (reverse) reverseBuffer();
            mPlayer.write(mBuffer, 0, mSamplesInBuffer);
            if (reverse) reverseBuffer();
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
    protected void onDestroy() {
        super.onDestroy();
        mDataFragment.setAudioBuffer(mBuffer);
        mDataFragment.setSamplesInBuffer(mSamplesInBuffer);
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
