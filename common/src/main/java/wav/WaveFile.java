package wav;

import java.io.File;
import java.io.FileNotFoundException;
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

    private final int audioDataSize;
    private final short[] audioData;

    private WaveFile(Builder builder) {
        numSamples = builder.audioData.length;
        numChannels = builder.numChannels;
        bitsPerSample = builder.bitsPerSample;
        sampleRate = builder.sampleRate;

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
        WaveFileHeader header = new WaveFileHeader.Builder()
                .sampleRate(sampleRate)
                .dataSize(audioDataSize)
                .channels(numChannels)
                .bitDepth(bitsPerSample)
                .format(Format.PCM)
                .build();
        ByteBuffer headerBytes = header.asByteBuffer();

        try {
            FileOutputStream os = new FileOutputStream(new File(path));
            FileChannel fc = os.getChannel();
            ByteBuffer audio = audioAsByteBuffer();
            while (headerBytes.hasRemaining())
                fc.write(headerBytes);
            while (audio.hasRemaining())
                fc.write(audio);
            fc.close();
            os.flush();
            os.close();
        }
        // TODO: don't catch everything here
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ByteBuffer audioAsByteBuffer() {
        ByteBuffer audio = ByteBuffer.allocate(audioDataSize);
        audio.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : audioData) { audio.putShort(s); }
        audio.flip();
        return audio;
    }

    public static WaveFile fromFile(String path) throws FileNotFoundException {
        return null;
    }
}
