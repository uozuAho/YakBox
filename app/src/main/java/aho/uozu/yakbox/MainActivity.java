package aho.uozu.yakbox;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.acra.ACRA;

import java.io.File;
import java.io.IOException;

import aho.uozu.android.audio.AudioBuffer;
import aho.uozu.android.audio.AudioPlayer;
import aho.uozu.android.audio.AudioRecorder;

public class MainActivity extends AppCompatActivity {

    // UI elements
    private Button mBtnSay = null;
    private Button mBtnPlay = null;
    private Button mBtnYalp = null;
    private SeekBar mSkbSpeed = null;

    // sound recorder, buffer & player
    private AudioRecorder mRecorder = null;
    private AudioBuffer mBuffer = null;
    private AudioPlayer mPlayer = null;

    // storage
    private Storage mStorage;

    // recording delay data
    private long mLastRecordEndMillis = 0;
    private boolean mIsRecording = false;

    private boolean mShowedVolumeWarningOnPlay = false;

    // constants
    private static final String TAG = "YakBox";
    /**
     * Used for sharing
     */
    private static final String TEMP_WAV_FILENAME = "yak";
    private static final int MAX_RECORD_TIME_S = 20;
    // Delay between end of last recording and next recording
    private static final int RECORD_WAIT_MS = 300;
    private static final double PLAYBACK_SPEED_MIN = 0.333;
    private static final double PLAYBACK_SPEED_MAX = 3.0;
    private static final double LOW_VOLUME = 0.33;

    private static final int LOAD_RECORDING_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(false);
        }

        mStorage = Storage.getInstance(this);
        mStorage.deleteTempRecordings();

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

            // set 'say' button back to grey if record buffer is full
            mRecorder.setOnBufferFullListener(new AudioRecorder.OnBufferFullListener() {
                @Override
                public void onBufferFull() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBtnSay.setBackgroundResource(R.drawable.round_button_grey);
                        }
                    });
                }
            });
        }
        catch (Exception e) {
            releaseAudioResources();
            ACRA.getErrorReporter().handleException(e, true);
            // application ends here (true parameter)
        }

        // init audio buffer
        if (mBuffer == null) {
            mBuffer = new AudioBuffer(mRecorder.getBufferSizeSamples());
            mStorage.loadBuffer(mBuffer);
        }

        // make volume buttons adjust music stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        lowVolumeWarningIfNecessary();
    }

    /**
     * Notify user if volume is lower than LOW_VOLUME
     */
    private void lowVolumeWarningIfNecessary() {
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
            mBuffer.resetIdx();
            mRecorder.startRecording();
            mBtnSay.setBackgroundResource(R.drawable.round_button_red);
            mIsRecording = true;
        }
    }

    private void stopRecording() {
        if (mIsRecording) {
            Log.d(TAG, "recording STOP");
            // set 'say' button back to grey
            mBtnSay.setBackgroundResource(R.drawable.round_button_grey);
            mRecorder.stopRecording();

            // move recording from recorder to audio buffer
            int numSamples = mRecorder.read(mBuffer);
            Log.d(TAG, String.format("%d samples copied to buffer", numSamples));

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

    private void showSaveDialog(String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_save, null);
        final TextView filenameTextView =
                (EditText) dialogView.findViewById(R.id.save_filename);
        filenameTextView.setText(name);

        builder
            .setTitle(R.string.save_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save_dialog_positive,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            saveWithOverwriteCheck(filenameTextView.getText().toString());
                        }
                    })
            .setNegativeButton(R.string.save_dialog_negative,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }

    private void saveWithOverwriteCheck(String name) {
        try {
            if (mStorage.exists(name)) {
                showConfirmOverwriteDialog(name);
            } else {
                saveRecording(name);
            }
        }
        catch (Storage.StorageUnavailableException e) {
            String msg = "Error: Can't access storage";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    }

    private void showConfirmOverwriteDialog(final String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(getString(R.string.save_confirm_overwrite_message)
                        + " " + name + "?")
                .setPositiveButton(R.string.save_dialog_positive,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                saveRecording(name);
                            }
                        })
                .setNegativeButton(R.string.save_dialog_negative,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showSaveDialog(name);
                            }
                        });
        builder.show();
    }

    private void saveRecording(String name) {
        try {
            mStorage.saveRecording(mBuffer, name, mRecorder.getSampleRate());
            String msg = "Saved: " + name;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
        catch (Storage.StorageUnavailableException e) {
            String msg = "Error: Can't access storage";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
        catch (IOException e) {
            Log.e(TAG, "Error saving recording", e);
            String msg = "Error saving file";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    }

    private File saveTempWav()
            throws Storage.StorageUnavailableException, IOException {
        if (mBuffer != null && mRecorder != null) {
            return mStorage.saveTempRecording(mBuffer, TEMP_WAV_FILENAME, mRecorder.getSampleRate());
        }
        return null;
    }

    private void startLoadActivity() {
        Intent i = new Intent(this, LoadActivity.class);
        startActivityForResult(i, LOAD_RECORDING_REQUEST);
    }

    private void startHelpActivity() {
        Intent i = new Intent(this, AboutHelpActivity.class);
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOAD_RECORDING_REQUEST:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    loadRecording(name);
                }
                break;
            default:
                Log.e(TAG, "Shouldn't get here!");
                break;
        }
    }

    private void loadRecording(String name) {
        try {
            mStorage.loadRecordingToBuffer(mBuffer, name);
            // Show saved toast to user
            String msg = "Loaded: " + name;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error loading recording", e);
            String msg = "Error loading file!";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBuffer != null) {
            mStorage.saveBuffer(mBuffer);
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
        try {
            File tempWav = saveTempWav();
            if (tempWav != null) {
                MenuItem shareItem = menu.findItem(R.id.action_share);
                ShareActionProvider myShareActionProvider =
                        (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
                Intent myShareIntent = new Intent(Intent.ACTION_SEND);
                myShareIntent.setType("audio/wav");
                myShareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempWav));
                myShareActionProvider.setShareIntent(myShareIntent);
            }
        }
        catch (Storage.StorageUnavailableException e) {
            String msg = "Error: Can't access storage";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
        catch (IOException e) {
            Log.e(TAG, "Error saving recording", e);
            String msg = "Error preparing yak for sharing";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_save:
                showSaveDialog("");
                return true;
            case R.id.action_load:
                startLoadActivity();
                return true;
            case R.id.action_about:
                startHelpActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
