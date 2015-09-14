package aho.uozu;

import org.junit.Test;

import wav.WaveFile;

public class WaveFileTests {

    private static final String TEST_FILE_PATH = "/tmp/asdf.wav";

    @Test
    public void writeToFile() {
        // create a sine wave, write to file
        int sampleRate = 22050;
        short[] audio = sineWave(2, 440, sampleRate);
        WaveFile wav = new WaveFile.Builder()
                .data(audio)
                .sampleRate(sampleRate)
                .bitDepth(16)
                .channels(1)
                .build();
        String path = TEST_FILE_PATH;
        wav.writeToFile(path);
    }

    @Test
    public void fromFile() {

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
