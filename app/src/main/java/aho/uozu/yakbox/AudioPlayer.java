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
    private AudioTrack mAudioTrack;

    private static final String TAG = "YakBox-AudioPlayer";
    private static final double PLAYBACK_RATE_MIN = 0.333;
    private static final double PLAYBACK_RATE_MAX = 3.0;

    /**
     * Initialise a new AudioPlayer
     * @param sample_rate Audio sample rate in Hertz
     * @param buffer_size Audio buffer size in samples
     * @throws Exception If audio system initialisation fails.
     */
    public AudioPlayer(int sample_rate, int buffer_size) throws Exception {
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
        if (buf.mNumSamples > 0) {
            if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                mAudioTrack.stop();
            }
            // Clear mAudioTrack's internal buffer
            // so the end of the last clip is not played.
            if (buf.mNumSamples < mLastClipLengthSamples) {
                flush();
            }
            mLastClipLengthSamples = buf.mNumSamples;
            int rate_hz = getPlaybackSamplingRate(rate);
            Log.d(TAG, String.format("Playing sample at %d hz", rate_hz));
            mAudioTrack.write(buf.mBuffer, 0, buf.mNumSamples);
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

    private int getPlaybackSamplingRate(double rate) {
        return (int) (mSampleRate * rate);
    }

    private AudioTrack initAudioTrack() throws Exception {
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                mBufferSizeBytes, AudioTrack.MODE_STATIC);
        int state = track.getState();
        if (state == AudioTrack.STATE_UNINITIALIZED) {
            track.release();
            throw new Exception(String.format(
                    "Failed to initialise AudioTrack. State: %d", state));
        }
        return track;
    }
}
