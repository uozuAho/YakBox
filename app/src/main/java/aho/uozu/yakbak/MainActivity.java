package aho.uozu.yakbak;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;


public class MainActivity extends Activity {

    // UI elements
    private Button mBtnSay = null;
    private Button mBtnPlay = null;
    private CheckBox mCbReverse = null;
    private SeekBar mSkbSpeed = null;
    private ImageView mImgRecordIndicator = null;

    // sound recorder & player
    private AudioRecord mRecorder = null;
    private AudioTrack mPlayer = null;

    // audio buffer - short for 16 bit PCM
    private short[] mBuffer = null;
    private int mSamplesInBuffer = 0;

    // constants
    private static final String TAG = "YakBak";
    private static final String FILENAME = Environment
            .getExternalStorageDirectory().getAbsolutePath()
            + "/yakbak-sound.3gp";

    private static final int MAX_RECORD_TIME_S = 2;
    private static final int SAMPLE_RATE_HZ_MAX =
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
    private static final int BUFFER_SIZE_SAMPLES =
            MAX_RECORD_TIME_S * SAMPLE_RATE_HZ_MAX;
    private static final double PLAYBACK_SPEED_MIN = 0.3;
    private static final double PLAYBACK_SPEED_MAX = 3.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI controls
        mBtnPlay = (Button) findViewById(R.id.button_play);
        mBtnSay = (Button) findViewById(R.id.button_say);
        mCbReverse = (CheckBox) findViewById(R.id.cb_reverse);
        mSkbSpeed = (SeekBar) findViewById(R.id.skb_speed);
        mImgRecordIndicator = (ImageView) findViewById(R.id.color_rec);

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
                startPlayback();
            }
        });

        // reverse checkbox listener - reverse existing sample if
        // reverse is checked / unchecked
        mCbReverse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                reverseBuffer();
                mPlayer.write(mBuffer, 0, mSamplesInBuffer);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_HZ_MAX / 2,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_SAMPLES);
        mPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE_HZ_MAX / 2,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE_SAMPLES, AudioTrack.MODE_STATIC);
    }

    private void startRecording() {
        Log.d(TAG, "recording START");

        mRecorder.startRecording();

        // indicate recording with red rectangle
        mImgRecordIndicator.setImageResource(R.drawable.red_rect);
    }

    private void stopRecording() {
        Log.d(TAG, "recording STOP");

        // indicate NOT recording with blue rectangle
        mImgRecordIndicator.setImageResource(R.drawable.blue_rect);
        mRecorder.stop();

        // move recording from recorder to player
        flush();
        mSamplesInBuffer = mRecorder.read(mBuffer, 0, BUFFER_SIZE_SAMPLES);
        if (mCbReverse.isChecked()) {
            reverseBuffer();
        }
        mPlayer.write(mBuffer, 0, mSamplesInBuffer);
        Log.d(TAG, String.format("%d samples copied to buffer", mSamplesInBuffer));
    }

    private void startPlayback() {
        double speed = getPlaybackSpeed();
        int playback_rate_hz = (int) (speed * SAMPLE_RATE_HZ_MAX / 2);
        Log.d(TAG, String.format("playing sample at %d hz", playback_rate_hz));
        mPlayer.stop();
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

    private void reverseBuffer() {
        short temp;
        for (int i = 0; i < mSamplesInBuffer / 2; i++) {
            temp = mBuffer[i];
            mBuffer[i] = mBuffer[mSamplesInBuffer - i];
            mBuffer[mSamplesInBuffer - i] = temp;
        }
    }

    private double getPlaybackSpeed() {
        double range = PLAYBACK_SPEED_MAX - PLAYBACK_SPEED_MIN;
        double slider_frac = ((double) mSkbSpeed.getProgress()) / mSkbSpeed.getMax();
        return slider_frac * range + PLAYBACK_SPEED_MIN;
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
