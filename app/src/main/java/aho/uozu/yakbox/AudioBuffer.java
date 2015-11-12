package aho.uozu.yakbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Single channel 16-bit audio sample buffer
 */
public class AudioBuffer {

    /** audio sample storage */
    private final short[] mBuffer;

    /** current read/write idx */
    private int mIdx;

    /** Creates new AudioBuffer with the given sample capacity */
    public AudioBuffer(int sample_capacity) {
        mBuffer = new short[sample_capacity];
        mIdx = 0;
    }

    /**
     * Reverses contents in buffer up to read/write index.
     *
     * Read/write index is unchanged.
     */
    public void reverse() {
        short temp;
        for (int i = 0; i < mIdx / 2; i++) {
            int i_opposite = mIdx - 1 - i;
            temp = mBuffer[i];
            mBuffer[i] = mBuffer[i_opposite];
            mBuffer[i_opposite] = temp;
        }
    }

    /** Get a reference to the internal storage. */
    public short[] getBuffer() {
        return mBuffer;
    }

    /** Get current read/write index */
    public int getIdx() {
        return mIdx;
    }

    /** Get the buffer's storage capacity / length */
    public int capacity() {
        return mBuffer.length;
    }

    /** Returns true if the internal storage is full. */
    public boolean isFull() {
        return mIdx == mBuffer.length;
    }

    /**
     * Increment the read/write index by the given value.
     *
     * If end of storage is reached/exceeded, index remains at
     * end of storage.
     *
     * @param val positive value to increment by. If negative, idx is unchanged.
     */
    public void incrementIdx(int val) {
        if (val >= 0)
            mIdx += val;
        if (mIdx > mBuffer.length)
            mIdx = mBuffer.length;
    }

    /**
     * Write the data in buf to this buffer.
     *
     * Buffer overflows are silently ignored - use getIdx() or isFull().
     *
     * @param buf Buffer to copy data from.
     * @param len Number of shorts to write.
     */
    public void write(short[] buf, int len) {
        int writeLen = Math.min(remaining(), len);
        System.arraycopy(buf, 0, mBuffer, mIdx, writeLen);
        mIdx += writeLen;
    }

    /** Reset read/write index to zero */
    public void resetIdx() {
        mIdx = 0;
    }

    /** Returns the space remaining in the buffer.
     *
     * Ie. buffer length - read/write index.
     *
     * @return Integer in range [0, capacity]
     */
    public int remaining() {
        int rem = mBuffer.length - mIdx;
        if (rem < 0)
            rem = 0;
        return rem;
    }

    public void saveToFile(File fp) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fp));
        oos.writeInt(mIdx);
        for (int i = 0; i < mIdx; i++) {
            oos.writeShort(mBuffer[i]);
        }
        oos.flush();
        oos.close();
    }

    public void loadFromFile(File fp) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fp));
        int num_samples_in_file = ois.readInt();
        // ensure not to overflow buffer, if saved sound is too big
        mIdx = Math.min(num_samples_in_file, mBuffer.length);
        for (int i = 0; i < mIdx; i++) {
            mBuffer[i] = ois.readShort();
        }
    }
}
