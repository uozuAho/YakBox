package wav;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * A quick and dirty wave file class for saving and
 * loading audio data to/from 'WAVE' files. Only supports
 * 16 bit, 1 channel PCM wave files at the moment.
 */
public class WaveFile {

    private final int numSamples;
    private final short numChannels;
    private final short bitsPerSample;
    private final int sampleRate;

    private final int byteRate;
    private final short frameSize;
    private final int audioDataSize;
    private final short[] audioData;

    private static final short FORMAT_CODE_PCM = 1;
    private static final int HEADER_LEN = 44;

    private WaveFile(Builder builder) {
        numSamples = builder.audioData.length;
        numChannels = builder.numChannels;
        bitsPerSample = builder.bitsPerSample;
        sampleRate = builder.sampleRate;

        byteRate = sampleRate * numChannels * bitsPerSample / 8;
        frameSize = (short) (numChannels * bitsPerSample / 8);
        audioDataSize =  numSamples * numChannels * bitsPerSample / 8;
        audioData = builder.audioData;
    }

    public static class Builder {
        private short numChannels;
        private short bitsPerSample;
        private int sampleRate;
        private short[] audioData;

        public Builder() {}

        public Builder channels(int numChannels) {
            this.numChannels = (short) numChannels;
            return this;
        }

        // This could be inferred from audio data
        public Builder bitDepth(int bitsPerSample) {
            this.bitsPerSample = (short) bitsPerSample;
            return this;
        }

        public Builder sampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder data(short[] audioData) {
            this.audioData = audioData;
            return this;
        }

        public WaveFile build() { return new WaveFile(this); }
    }

    public void writeToFile(String path) {
        // This is actually total file size - 8 bytes,
        // couldn't think of a better variable name
        // 44 byte header + audio data - 8
        int totalFileSize = HEADER_LEN + audioDataSize - 8;

        // Create the header
        ByteBuffer header = ByteBuffer.allocate(HEADER_LEN);
        header.order(ByteOrder.LITTLE_ENDIAN);
        for (byte b : "RIFF".getBytes()) { header.put(b); }      // 00: RIFF
        header.putInt(totalFileSize);                            // 04: total size - 8
        for (byte b : "WAVE".getBytes()) { header.put(b); }      // 08: WAVE
        for (byte b : "fmt ".getBytes()) { header.put(b); }      // 12: fmt
        header.putInt(16);                                       // 16: length of 'fmt' section - always 16
        header.putShort(FORMAT_CODE_PCM);                        // 20: format
        header.putShort(numChannels);                            // 22: num channels
        header.putInt(sampleRate);                               // 24: sample rate
        header.putInt(byteRate);                                 // 28: byte rate
        header.putShort(frameSize);                              // 32: frame size
        header.putShort(bitsPerSample);                          // 34: bit depth
        for (byte b : "data".getBytes()) { header.put(b); }      // 36: data
        header.putInt(audioDataSize);                            // 40: audio data size
        header.flip();

        try {
            FileOutputStream os = new FileOutputStream(new File(path));
            FileChannel fc = os.getChannel();
            ByteBuffer audio = ByteBuffer.allocate(audioDataSize);
            audio.order(ByteOrder.LITTLE_ENDIAN);
            for (short s : audioData) { audio.putShort(s); }
            audio.flip();
            while (header.hasRemaining())
                fc.write(header);
            while (audio.hasRemaining())
                fc.write(audio);
            fc.close();
            os.flush();
            os.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: fromFile

    // for testing... TODO: move this
    private static short[] sineWave(int len_s, int freq, int sampleRate) {
        int amplitude = Short.MAX_VALUE;  // crank it up!
        int num_samples = sampleRate * len_s;
        short[] samples = new short[num_samples];

        for (int i = 0; i < num_samples; i++) {
            double time_s = (double) i * len_s / num_samples;
            samples[i] = (short) (amplitude * Math.sin(2 * Math.PI * freq * time_s));
        }
        return samples;
    }

    // cheap unit test
    public static void main(String[] args) {
        // create a sine wave, write to file
        int sampleRate = 22050;
        short[] audio = sineWave(2, 440, sampleRate);
        WaveFile wav = new Builder()
                .data(audio)
                .sampleRate(sampleRate)
                .bitDepth(16)
                .channels(1)
                .build();
        String path = "/tmp/asdf.wav";
        wav.writeToFile(path);
    }
}
