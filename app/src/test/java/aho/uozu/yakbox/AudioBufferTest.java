package aho.uozu.yakbox;

import static junit.framework.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import aho.uozu.android.audio.AudioBuffer;

public class AudioBufferTest {

    private AudioBuffer mAudioBuffer;

    private int TEST_BUFFER_SIZE = 100;

    @Before
    public void setUp() {
        mAudioBuffer = new AudioBuffer(TEST_BUFFER_SIZE);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testInit() {
        assertEquals(0, mAudioBuffer.getIdx());
        assertEquals(TEST_BUFFER_SIZE, mAudioBuffer.capacity());
        assertEquals(TEST_BUFFER_SIZE, mAudioBuffer.remaining());
        assertEquals(false, mAudioBuffer.isFull());
    }

    @Test
    public void testWrite() {
        mAudioBuffer.write(new short[] {1}, 1);
        assertEquals(1, mAudioBuffer.getIdx());
        assertEquals(TEST_BUFFER_SIZE - 1, mAudioBuffer.remaining());
    }
}
