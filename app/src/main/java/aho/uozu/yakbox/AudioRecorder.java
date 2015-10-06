package aho.uozu.yakbox;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


public class AudioRecorder {
    private int mSampleRate;
    private int mBufferSizeSamples;
    private AudioBuffer2 mAudioBuffer;
    private AudioRecord mAudioRecord;
    private OnBufferFullListener mBufListener;
    private boolean mIsRecording;

    // ---------------------------------------------------------------------
    // constants
    private static final int READ_CHUNK_SIZE_SAMPLES = 256;
    private static final String TAG = "YakBox-AudioRecorder";

    // ---------------------------------------------------------------------
    // data types, interfaces

    /** Buffer for storing audio */
    private class AudioBuffer2 {
        public short[] buffer;
        public int idx;

        public AudioBuffer2(int capacity) {
            buffer = new short[capacity];
            idx = 0;
        }

        /** Number of samples remaining */
        public int remaining() { return buffer.length - idx; }
    }

    /** Reads bytes from mAudioRecord into mAudioBuffer */
    private class AudioReader implements Runnable {
        @Override
        public void run() {
            while (mIsRecording) {
                int bytesToRead = Math.min(READ_CHUNK_SIZE_SAMPLES, mAudioBuffer.remaining());
                mAudioBuffer.idx +=
                        mAudioRecord.read(mAudioBuffer.buffer, mAudioBuffer.idx, bytesToRead);
                // if buffer full, stop recording and call the buffer full listener
                if (bytesToRead < READ_CHUNK_SIZE_SAMPLES) {
                    mIsRecording = false;
                    mBufListener.onBufferFull();
                }
            }
        }
    }

    /**
     * Interface definition for callback to be invoked when the
     * audio recording buffer is full.
     */
    public interface OnBufferFullListener {
        void onBufferFull();
    }


    // ---------------------------------------------------------------------
    // constructors, methods

    /**
     * Initialise the audio recorder.
     *
     * @param record_time_s Maximum recording length in seconds
     * @throws UnsupportedOperationException if hardware not supported
     * @throws IllegalStateException if error initialising audio recorder
     */
    public AudioRecorder(int record_time_s)
            throws UnsupportedOperationException, IllegalStateException {
        mAudioRecord = initAudioRecord(record_time_s);
        mAudioBuffer = new AudioBuffer2(mBufferSizeSamples);
        Log.d(TAG, "AudioRecorder initialised. Sample rate: " + mSampleRate);
    }

    public void startRecording() {
        mAudioBuffer.idx = 0;
        mAudioRecord.startRecording();
        mIsRecording = true;
        Thread t = new Thread(new AudioReader());
        t.start();
    }

    public void stopRecording() {
        mAudioRecord.stop();
        mIsRecording = false;
    }

    /** Reads internally stored audio into the given buffer. */
    public int read(AudioBuffer buf) {
        if (BuildConfig.DEBUG && buf.mBuffer.length != mBufferSizeSamples)
            throw new AssertionError();
        System.arraycopy(mAudioBuffer.buffer, 0, buf.mBuffer, 0, mAudioBuffer.idx);
        return mAudioBuffer.idx;
    }

    /**
     * Releases internal resources. Must be called when you are finished
     * with this object, otherwise other applications will be blocked from
     * using audio resources.
     *
     * Subsequent calls on the same object have no effect.
     */
    public void release() {
        if (mAudioRecord != null) {
            if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                mAudioRecord.stop();
            }
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    public void setOnBufferFullListener(OnBufferFullListener l) {
        mBufListener = l;
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
     * @throws IllegalArgumentException if initialisation parameters are bad
     */
    private AudioRecord initAudioRecord(int recordTimeS)
            throws UnsupportedOperationException, IllegalStateException {
        mSampleRate = findRecordingSampleRate();
        int buffer_size_bytes = mSampleRate * recordTimeS * 2;
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
        for (int rate : new int[] { 22050, 16000, 11025, 8000 }) {
            int bufferSize = AudioRecord.getMinBufferSize(rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0)
                return rate;
        }
        throw new UnsupportedOperationException("Unsupported audio hardware");
    }


}
