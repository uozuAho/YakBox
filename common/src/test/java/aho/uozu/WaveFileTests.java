package aho.uozu;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import wav.WaveFile;

public class WaveFileTests {

    private static final String TEST_FILE_PATH = "/tmp/asdf.wav";

    @Test
    public void toAndFromFile() throws IOException {
        int len_s = 2;
        int sampleRate = 22050;
        int numSamples = len_s * sampleRate;
        int bitDepth = 16;
        int numChannels = 1;
        short[] audio = sineWave(len_s, 440, sampleRate);

        WaveFile wavOut = new WaveFile.Builder()
                .data(audio)
                .sampleRate(sampleRate)
                .bitDepth(bitDepth)
                .channels(numChannels)
                .build();
        String path = TEST_FILE_PATH;
        wavOut.writeToFile(path);

        WaveFile wavIn;
        wavIn = WaveFile.fromFile(TEST_FILE_PATH);
        Assert.assertEquals(sampleRate, wavIn.getSampleRate());
        Assert.assertEquals(numSamples, wavIn.getNumSamples());
        Assert.assertEquals(bitDepth, wavIn.getBitsPerSample());
        Assert.assertEquals(numChannels, wavIn.getNumChannels());
        Assert.assertArrayEquals(sineWave(len_s, 440, sampleRate), wavIn.getAudioData());
    }

    /**
     * Create a 16-bit PCM sine wave of desired length and frequency.
     * Amplitude is 1 (-0dB?).
     */
    private static short[] sineWave(int len_s, int freq, int sampleRate) {
        int amplitude = Short.MAX_VALUE;
        int num_samples = sampleRate * len_s;
        short[] samples = new short[num_samples];

        for (int i = 0; i < num_samples; i++) {
            double time_s = (double) i * len_s / num_samples;
            samples[i] = (short) (amplitude * Math.sin(2 * Math.PI * freq * time_s));
        }
        return samples;
    }

}
