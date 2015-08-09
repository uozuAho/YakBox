package aho.uozu.yakbox;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecorder {
    private final int mRecordTimeS;
    private int mSampleRate;
    private int mBufferSizeSamples;
    private AudioRecord mAudioRecord;
    private OnBufferFullListener mBufListener;

    // constants
    private static final String TAG = "YakBox-AudioRecorder";


    public AudioRecorder(int record_time_s) throws Exception {
        mRecordTimeS = record_time_s;
        mAudioRecord = initAudioRecord();
        if (mAudioRecord == null)
            throw new Exception("Error initialising audio recorder");
        Log.d(TAG, "AudioRecorder initialised. Sample rate: " +
                Integer.toString(mSampleRate));
    }

    public void startRecording() {
        mAudioRecord.startRecording();
    }

    public void stopRecording() {
        mAudioRecord.stop();
    }

    public int read(AudioBuffer buf) {
        if (BuildConfig.DEBUG && buf.mBuffer.length != mBufferSizeSamples)
            throw new AssertionError();
        return mAudioRecord.read(buf.mBuffer, 0, mBufferSizeSamples);
    }

    public void release() {
        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord.stop();
        }
        mAudioRecord.release();
        mAudioRecord = null;
    }

    /**
     * Interface definition for callback to be invoked when the
     * audio recording buffer is full.
     */
    public interface OnBufferFullListener {
        void onBufferFull();
    }

    public void setOnBufferFullListener(OnBufferFullListener l) {
        mBufListener = l;
        mAudioRecord.setRecordPositionUpdateListener(
                new AudioRecord.OnRecordPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioRecord recorder) {
                        mBufListener.onBufferFull();
                    }

                    @Override
                    public void onPeriodicNotification(AudioRecord recorder) {
                    }
                });
        mAudioRecord.setNotificationMarkerPosition(mBufferSizeSamples);
    }

    /**
     * Get the sample rate used for recording.
     * @return Sample rate in Hertz.
     */
    public int getSampleRate() {
        return mSampleRate;
    }

    public int getBufferSizeSamples() {
        return mBufferSizeSamples;
    }

    /**
     * Initialise AudioRecord object.
     * @return Initialised AudioRecord object, or null.
     */
    private AudioRecord initAudioRecord() {
        AudioRecord record = null;
        mSampleRate = findRecordingSampleRate();
        if (mSampleRate > 0) {
            int buffer_size_bytes = mSampleRate * mRecordTimeS * 2;
            mBufferSizeSamples = buffer_size_bytes / 2;
            record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, buffer_size_bytes);
            int state = record.getState();
            if (state != AudioRecord.STATE_INITIALIZED) {
                record.release();
                record = null;
            }
        }
        return record;
    }

    /**
     * Get a supported sample rate for Android's AudioRecord.
     * @return Valid sampling rate, or -1 if none found.
     */
    private int findRecordingSampleRate() {
        for (int rate : new int[] {22050, 16000, 11025, 8000}) {
            int bufferSize = AudioRecord.getMinBufferSize(rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0)
                return rate;
        }
        return -1;
    }
}
