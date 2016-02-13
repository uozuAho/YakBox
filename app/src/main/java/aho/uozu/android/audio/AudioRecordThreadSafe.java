package aho.uozu.android.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.Semaphore;

/**
 * Thread-safe, managed instance of AudioRecord, with a few nice
 * utilities thrown in.
 *
 * Only one thread at a time can access the internal AudioRecord object.
 *
 * I don't know if AudioRecord is thread safe. Can't find any mention
 * of thread safety in the docs. It would have been nice to know before
 * I attempted this.
 */
class AudioRecordThreadSafe {

    private static final int INIT_TRIES = 3;
    private static final int INIT_RETRY_DELAY_MS = 20;
    private static final String TAG = "AudioRecordThreadSafe";

    private int mSamplingRate;

    /** Single instance of this object */
    private static AudioRecordThreadSafe mInstance;

    /** Managed instance of AudioRecord */
    private AudioRecord mAudioRecord;

    /** Guards access to the AudioRecord instance */
    private final Semaphore mAudioRecordSem;

    /** Private constructor to prevent accidental instantiation */
    private AudioRecordThreadSafe(int recordTimeS) throws InterruptedException {
        mAudioRecordSem = new Semaphore(1, true);
        initAudioRecordWithRetry(recordTimeS);
    }

    public static AudioRecordThreadSafe getInstance(int recordTimeS)
            throws UnsupportedOperationException, IllegalStateException, InterruptedException {
        if (mInstance == null) {
            mInstance = new AudioRecordThreadSafe(recordTimeS);
        }
        else if (mInstance.mAudioRecord == null) {
            mInstance.initAudioRecordWithRetry(recordTimeS);
        }
        return mInstance;
    }

    public int getSamplingRate() {
        return mSamplingRate;
    }

    public void startRecording() throws IllegalStateException, InterruptedException {
        mAudioRecordSem.acquire();
        mAudioRecord.startRecording();
        mAudioRecordSem.release();
    }

    public void stop() throws IllegalStateException, InterruptedException {
        mAudioRecordSem.acquire();
        mAudioRecord.stop();
        mAudioRecordSem.release();
    }

    @SuppressWarnings("unused")
    public int getRecordingState() throws InterruptedException {
        mAudioRecordSem.acquire();
        int state = mAudioRecord.getRecordingState();
        mAudioRecordSem.release();
        return state;
    }

    public int read(short[] audioData, int offsetInShorts, int sizeInShorts)
            throws InterruptedException {
        mAudioRecordSem.acquire();
        int numSamples = mAudioRecord.read(audioData, offsetInShorts, sizeInShorts);
        mAudioRecordSem.release();
        return numSamples;
    }

    public void release() throws InterruptedException {
        mAudioRecordSem.acquire();
        mAudioRecord.release();
        mAudioRecord = null;
        mAudioRecordSem.release();
    }

    /**
     * Attempt a number of times to initialise AudioRecord. Blocks until
     * initialised or an exception is thrown.
     *
     * @param recordTimeS size of recording storage, in seconds
     * @throws UnsupportedOperationException
     * @throws IllegalStateException
     */
    private void initAudioRecordWithRetry(int recordTimeS)
            throws UnsupportedOperationException, IllegalStateException, InterruptedException {

        if (mAudioRecord != null) {
            throw new IllegalStateException("mAudioRecord should be null here");
        }
        mAudioRecordSem.acquire();
        for (int i = 0; i < INIT_TRIES; i++) {
            try {
                Log.d(TAG, "Attempting audio init");
                mAudioRecord = initAudioRecord();
                // if we get here, mAudioRecord was initialised, so stop further attempts
                break;
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(INIT_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    throw new IllegalStateException("Interrupted during recorder init");
                }
            }
        }
        if (mAudioRecord == null) {
            throw new IllegalStateException("Failed to initialise AudioRecord");
        }
        mAudioRecordSem.release();
        Log.d(TAG, "AudioRecorder initialised. Sample rate: " + mSamplingRate);
    }

    /**
     * Initialise AudioRecord object.
     *
     * @return Initialised AudioRecord object.
     * @throws UnsupportedOperationException if audio hardware is unsupported
     * @throws IllegalStateException if audio recorder could not be initialised
     * @throws IllegalArgumentException if initialisation parameters are bad
     */
    private AudioRecord initAudioRecord()
            throws UnsupportedOperationException, IllegalStateException {
        mSamplingRate = findRecordingSampleRate();
        int bufferSize = AudioRecord.getMinBufferSize(mSamplingRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                mSamplingRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            record.release();
            // Not sure if this is necessary, but I'm still getting
            // audio init errors so I'm getting desperate.
            //noinspection UnusedAssignment
            record = null;
            throw new IllegalStateException("Error initialising audio recorder");
        }
        return record;
    }

    /**
     * Get a supported sampling rate for Android's AudioRecord.
     *
     * @return Supported sampling rate in hertz
     * @throws UnsupportedOperationException if no supported sampling rates
     *
     * TODO: This can return a rate that doesn't work (fails on initialise).
     *       Seen on emulators (which tend to like 8000Hz), not sure on real hardware.
     */
    private int findRecordingSampleRate() throws UnsupportedOperationException {
        // Workaround: If returning bad rates on emulators:
         for (int rate : new int[] { 44100, 22050, 16000, 11025, 8000 }) {
//        for (int rate : new int[] { 22050, 16000, 11025, 8000 }) {
            int bufferSize = AudioRecord.getMinBufferSize(rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                Log.d(TAG, "Supported sample rate found: " + rate);
                return rate;
            }
        }
        throw new UnsupportedOperationException("Unsupported audio hardware");
    }

}
