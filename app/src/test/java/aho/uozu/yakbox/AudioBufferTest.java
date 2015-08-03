package aho.uozu.yakbox;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    public void testNothing() {

    }
}
