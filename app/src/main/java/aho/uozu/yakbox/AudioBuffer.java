package aho.uozu.yakbox;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class AudioBuffer {
    // audio mBuffer - short for 16 bit PCM
    public short[] mBuffer;
    public int mNumSamples;

    public AudioBuffer(int sample_capacity) {
        mBuffer = new short[sample_capacity];
        mNumSamples = 0;
    }

    public void reverse() {
        short temp;
        for (int i = 0; i < mNumSamples / 2; i++) {
            temp = mBuffer[i];
            mBuffer[i] = mBuffer[mNumSamples - i];
            mBuffer[mNumSamples - i] = temp;
        }
    }

    public void clear() {
        for (int i = 0; i < mBuffer.length; i++) {
            mBuffer[i] = 0;
        }
    }

    public void saveToFile(String path) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
        oos.writeInt(mNumSamples);
        for (int i = 0; i < mNumSamples; i++) {
            oos.writeShort(mBuffer[i]);
        }
        oos.flush();
        oos.close();
    }

    public void loadFromFile(String path) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
        mNumSamples = ois.readInt();
        for (int i = 0; i < mNumSamples; i++) {
            mBuffer[i] = ois.readShort();
        }
    }
}
