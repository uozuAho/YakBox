package aho.uozu.yakbak;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Hold data to retain through configuration changes.
 */
public class DataFragment extends Fragment {
    private short[] mAudioBuffer;
    private int mSamplesInBuffer = 0;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setAudioBuffer(short[] buffer) {
        mAudioBuffer = buffer;
    }

    public short[] getAudioBuffer() {
        return mAudioBuffer;
    }

    public void setSamplesInBuffer(int num_samples) {
        mSamplesInBuffer = num_samples;
    }

    public int getSamplesInBuffer() {
        return mSamplesInBuffer;
    }
}
