package aho.uozu.yakbox;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayer {
    private int mSampleRate;
    private int mBufferSizeSamples;
    private int mBufferSizeBytes;
    private int mLastClipLengthSamples;
    private String mFilepath;
    private AudioTrack mAudioTrack;

    private static final String TAG = "YakBox-AudioPlayer";
    private static final double PLAYBACK_RATE_MIN = 0.333;
    private static final double PLAYBACK_RATE_MAX = 3.0;

    public static class Builder {
        private int mSampleRate;
        private int mBufferSizeSamples;
        private String mFilepath;

        public Builder sample_rate(int rate) {
            mSampleRate = rate;
            return this;
        }

        public Builder filepath(String path) {
            mFilepath = path;
            return this;
        }

        /**
         * Set the audio player buffer size (in number of samples)
         * @param num_samples
         *      Buffer size in samples.
         */
        public Builder buffersize(int num_samples) {
            mBufferSizeSamples = num_samples;
            return this;
        }

        /**
         * Build the AudioPlayer.
         * @throws Exception
         *      If initialisation fails.
         */
        public AudioPlayer build() throws Exception {
            return new AudioPlayer(this);
        }
    }

    /**
     * Play an audio clip
     * @param buf Audio buffer
     * @param len Number of useful samples in the buffer
     * @param rate Playback rate
     */
    public void play(short[] buf, int len, double rate) {
        if (buf == null)                    throw new NullPointerException();
        if (len <= 0 || len > buf.length)   throw new IllegalArgumentException();
        if (rate < PLAYBACK_RATE_MIN || rate > PLAYBACK_RATE_MAX) {
            throw new IllegalArgumentException("playback rate out of bounds");
        }

        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.stop();
        }

        // Clear mAudioTrack's internal buffer
        // so the end of the last clip is not played.
        if (len < mLastClipLengthSamples) {
            flush();
        }
        mLastClipLengthSamples = len;
        int rate_hz = getPlaybackSamplingRate(rate);
        Log.d(TAG, String.format("Playing sample at %d hz", rate_hz));
        mAudioTrack.write(buf, 0, len);
        mAudioTrack.reloadStaticData();
        mAudioTrack.setPlaybackRate(rate_hz);
        mAudioTrack.play();
    }

    /**
     * Clear mAudioTrack's internal buffer.
     */
    private void flush() {
        short[] empty = new short[mBufferSizeSamples];
        mAudioTrack.write(empty, 0, mBufferSizeSamples);
    }

    /**
     * Release internal resources.
     */
    public void release() {
        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.stop();
        }
        mAudioTrack.release();
        mAudioTrack = null;
    }

    private int getPlaybackSamplingRate(double rate) {
        return (int) (mSampleRate * rate);
    }

    /**
     * Private constructor
     */
    private AudioPlayer(Builder b) throws Exception {
        this.mSampleRate = b.mSampleRate;
        this.mBufferSizeSamples = b.mBufferSizeSamples;
        // assume 16 bit samples
        this.mBufferSizeBytes = b.mBufferSizeSamples * 2;
        this.mLastClipLengthSamples = 0;
        this.mFilepath = b.mFilepath;
        this.mAudioTrack = initAudioTrack();
    }

    private AudioTrack initAudioTrack() throws Exception {
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                mBufferSizeBytes, AudioTrack.MODE_STATIC);
        int state = track.getState();
        if (state == AudioTrack.STATE_UNINITIALIZED) {
            throw new Exception(String.format(
                    "Failed to initialise AudioTrack. State: %d", state));
        }
        return track;
    }
}
