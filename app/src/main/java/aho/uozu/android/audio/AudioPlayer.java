package aho.uozu.android.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import aho.uozu.android.audio.AudioBuffer;

public class AudioPlayer {
    private final int mSampleRate;
    private final int mBufferSizeSamples;
    private final int mBufferSizeBytes;
    private int mLastClipLengthSamples;
    private AudioTrack mAudioTrack;

    private static final String TAG = "YakBox-AudioPlayer";
    private static final double PLAYBACK_RATE_MIN = 0.333;
    private static final double PLAYBACK_RATE_MAX = 3.0;

    /**
     * Initialise a new AudioPlayer
     *
     * @param sample_rate Audio sample rate in Hertz
     * @param buffer_size Audio buffer size in samples
     * @throws IllegalArgumentException if initialisation parameters are bad
     * @throws IllegalStateException If audio system initialisation fails.
     */
    public AudioPlayer(int sample_rate, int buffer_size)
            throws IllegalArgumentException, IllegalStateException {
        this.mSampleRate = sample_rate;
        this.mBufferSizeSamples = buffer_size;
        // assume 16 bit samples
        this.mBufferSizeBytes = buffer_size * 2;
        this.mLastClipLengthSamples = 0;
        this.mAudioTrack = initAudioTrack();
    }

    /**
     * Play an audio clip
     * @param buf Audio buffer
     * @param rate Playback rate
     */
    public void play(AudioBuffer buf, double rate) throws IllegalArgumentException {
        if (buf == null) throw new NullPointerException();
        if (rate < PLAYBACK_RATE_MIN || rate > PLAYBACK_RATE_MAX) {
            throw new IllegalArgumentException("playback rate out of bounds");
        }
        if (buf.getIdx() > 0) {
            if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                mAudioTrack.stop();
            }
            // Clear mAudioTrack's internal buffer
            // so the end of the last clip is not played.
            if (buf.getIdx() < mLastClipLengthSamples) {
                flush();
            }
            mLastClipLengthSamples = buf.getIdx();
            int rate_hz = getPlaybackSamplingRate(rate);
            Log.d(TAG, String.format("Playing sample at %d hz", rate_hz));
            mAudioTrack.write(buf.getBuffer(), 0, buf.getIdx());
            mAudioTrack.reloadStaticData();
            mAudioTrack.setPlaybackRate(rate_hz);
            mAudioTrack.play();
        }
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

    public int getPlaybackSamplingRate(double rate) {
        return (int) (mSampleRate * rate);
    }

    /**
     * Initialise audio track
     *
     * @return initialised AudioTrack
     * @throws IllegalArgumentException if initialisation parameters are bad
     * @throws IllegalStateException if failed to initialise
     */
    private AudioTrack initAudioTrack() throws IllegalStateException {
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                mBufferSizeBytes, AudioTrack.MODE_STATIC);
        if (track.getState() == AudioTrack.STATE_UNINITIALIZED) {
            track.release();
            throw new IllegalStateException("Failed to initialise AudioTrack");
        }
        return track;
    }
}
