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


    /**
     * Initialise the audio recorder.
     *
     * @param record_time_s Maximum recording length in seconds
     * @throws UnsupportedOperationException if hardware not supported
     * @throws IllegalStateException if error initialising audio recorder
     */
    public AudioRecorder(int record_time_s)
            throws UnsupportedOperationException, IllegalStateException {
        mRecordTimeS = record_time_s;
        mAudioRecord = initAudioRecord();
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
     *
     * @return Initialised AudioRecord object.
     * @throws UnsupportedOperationException if audio hardware is unsupported
     * @throws IllegalStateException if audio recorder could not be initialised
     */
    private AudioRecord initAudioRecord()
            throws UnsupportedOperationException, IllegalStateException {
        mSampleRate = findRecordingSampleRate();
        int buffer_size_bytes = mSampleRate * mRecordTimeS * 2;
        mBufferSizeSamples = buffer_size_bytes / 2;
        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, buffer_size_bytes);
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            record.release();
            throw new IllegalStateException("Error initialising audio recorder");
        }
        return record;
    }

    /**
     * Get a supported sampling rate for Android's AudioRecord.
     *
     * @return Supported sampling rate in hertz
     * @throws UnsupportedOperationException if no supported sampling rates
     */
    private int findRecordingSampleRate() throws UnsupportedOperationException {
        for (int rate : new int[] {22050, 16000, 11025, 8000}) {
            int bufferSize = AudioRecord.getMinBufferSize(rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0)
                return rate;
        }
        throw new UnsupportedOperationException("Unsupported audio hardware");
    }
}
