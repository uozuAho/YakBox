package aho.uozu.yakbox;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

public class AudioRecorder {
    private int mSampleRate;
    private int mBufferSizeSamples;
    private int mBufferSizeBytes;
    private AudioRecord mAudioRecord;
    private OnBufferFullListener mBufListener;

    // constants
    private static final String TAG = "YakBox-AudioRecorder";
    private static final int SAMPLE_RATE_HZ_MAX =
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) * 2;
    // Sample rates to attempt when initialising
    private static final int[] SAMPLE_RATES = {44100, 22050, 16000, 8000};

    public AudioRecorder(int record_time_s) throws Exception {
        this.mSampleRate = SAMPLE_RATE_HZ_MAX / 4;
        this.mBufferSizeSamples = record_time_s * this.mSampleRate;
        // assume 16 bit samples
        this.mBufferSizeBytes = this.mBufferSizeSamples * 2;
        this.mAudioRecord = initAudioRecord();
    }

    public void startRecording() {
        mAudioRecord.startRecording();
    }

    public void stopRecording() {
        mAudioRecord.stop();
    }

    public int read(short[] buffer) {
        return mAudioRecord.read(buffer, 0, mBufferSizeSamples);
    }

    public void release() {
        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord.stop();
        }
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

    public int getBufferSizeBytes() {
        return mBufferSizeBytes;
    }

    private AudioRecord initAudioRecord() throws Exception {
        // TODO: try multiple sampling rates / encodings etc.
        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, mBufferSizeBytes);
        int state = record.getState();
        if (state == AudioRecord.STATE_UNINITIALIZED) {
            throw new Exception(String.format(
                    "Failed to initialise AudioRecord. State: %d", state));
        }
        return record;
    }
}
