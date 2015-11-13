package aho.uozu.android.audio;


import android.media.AudioRecord;
import android.util.Log;

import aho.uozu.yakbox.BuildConfig;


public class AudioRecorder {
    private AudioBuffer mAudioBuffer;
    private AudioRecordThreadSafe mAudioRecord;
    private OnBufferFullListener mBufListener;
    private Thread mAudioReader;

    // ---------------------------------------------------------------------
    // constants
    private static final int READ_CHUNK_SIZE_SAMPLES = 256;
    private static final String TAG = "YakBox-AudioRecorder";

    // ---------------------------------------------------------------------
    // data types, interfaces

    /** Reads bytes from mAudioRecord into mAudioBuffer */
    private class AudioReader implements Runnable {
        private final short[] readBuf = new short[READ_CHUNK_SIZE_SAMPLES];
        @Override
        public void run() {
            while (isRecording() && !Thread.interrupted()) {
                int samplesToRead = Math.min(READ_CHUNK_SIZE_SAMPLES, mAudioBuffer.remaining());
                // read to a temporary buffer, then write to audio buffer.
                // This gets around a bug in Android 5.0's AudioRecord.read(short[]...)
                // See https://code.google.com/p/android/issues/detail?id=81953
                int result = 0;
                try {
                    result = mAudioRecord.read(readBuf, 0, samplesToRead);
                } catch (InterruptedException e) {
                    Log.d(TAG, "AudioReader interrupted");
                }
                if (result >= 0) {
                    mAudioBuffer.write(readBuf, result);
                }
                else {
                    Log.e(TAG, "mAudioRecord.read error: " + result);
                    // stop recording if any problems reading from AudioRecord
                    stopRecording();
                    // also call onBufferFull(). Could rename this to onRecordingStopped().
                    onBufferFullCallback();
                }

                // if buffer is (or is nearly) full, stop recording
                // and call the buffer full listener
                if (mAudioBuffer.isFull() || samplesToRead < READ_CHUNK_SIZE_SAMPLES) {
                    stopRecording();
                    onBufferFullCallback();
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
     * @param recordTimeS Maximum recording length in seconds
     * @throws UnsupportedOperationException if hardware not supported
     * @throws IllegalStateException if error initialising audio recorder
     */
    public AudioRecorder(int recordTimeS)
            throws UnsupportedOperationException, IllegalStateException, InterruptedException {
        mAudioRecord = AudioRecordThreadSafe.getInstance(recordTimeS);
        mAudioBuffer = new AudioBuffer(mAudioRecord.getSamplingRate() * recordTimeS);
    }

    public void startRecording() {
        Log.d(TAG, "startRecording");
        mAudioBuffer.resetIdx();
        try {
            mAudioRecord.startRecording();
            mAudioReader = new Thread(new AudioReader());
            mAudioReader.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops recording audio. Has no effect if not recording.
     */
    public void stopRecording() {
        Log.d(TAG, "stopRecording");
        if (isRecording()) {
            try {
                mAudioRecord.stop();
                mAudioReader.interrupt();
                mAudioReader.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads internally stored audio into the given buffer.
     *  @return number of samples read
     */
    public int read(AudioBuffer buf) {
        Log.d(TAG, "read");
        if (BuildConfig.DEBUG && buf.capacity() < mAudioBuffer.capacity())
            throw new AssertionError();
        // copy to dst buffer
        System.arraycopy(mAudioBuffer.getBuffer(), 0, buf.getBuffer(), buf.getIdx(),
                mAudioBuffer.getIdx());
        // increment dst buffer index by number of samples in this buffer
        buf.incrementIdx(mAudioBuffer.getIdx());
        return mAudioBuffer.getIdx();
    }

    /**
     * Releases internal resources. Must be called when you are finished
     * with this object, otherwise other applications will be blocked from
     * using audio resources.
     *
     * Subsequent calls on the same object have no effect.
     */
    public void release() {
        Log.d(TAG, "release");
        if (mAudioRecord != null) {
            stopRecording();
            try {
                mAudioRecord.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        return mAudioRecord.getSamplingRate();
    }

    public int getBufferSizeSamples() {
        return mAudioBuffer.capacity();
    }

    /**
     * Returns true if mAudioRecord object exists and is recording, otherwise returns false.
     */
    private boolean isRecording() {
        if (mAudioRecord != null) {
            try {
                if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    return true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void onBufferFullCallback() {
        if (mBufListener != null) {
            mBufListener.onBufferFull();
        }
    }
}
