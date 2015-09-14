package wav;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
        // TODO: defensive copy for immutability???? mehg.....
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

    public void writeToFile(String path) throws IOException {
        WaveFileHeader header = new WaveFileHeader.Builder()
                .sampleRate(sampleRate)
                .dataSize(audioDataSize)
                .channels(numChannels)
                .bitDepth(bitsPerSample)
                .format(Format.PCM)
                .build();
        ByteBuffer headerBytes = header.asByteBuffer();

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

    public static WaveFile fromFile(String path) throws IOException {
        FileInputStream is = new FileInputStream(new File(path));
        FileChannel fc = is.getChannel();

        // get header
        ByteBuffer headerBytes = ByteBuffer.allocate(WaveFileHeader.HEADER_LEN);
        while (headerBytes.hasRemaining()) {
            fc.read(headerBytes);
        }
        headerBytes.flip();
        WaveFileHeader header = WaveFileHeader.read(headerBytes);

        // get audio data
        ByteBuffer audioBytes = ByteBuffer.allocate(header.getAudioDataSize());
        while (audioBytes.hasRemaining()) {
            fc.read(audioBytes);
        }
        fc.close();
        audioBytes.flip();

        audioBytes.order(ByteOrder.LITTLE_ENDIAN);
        short[] audioData = new short[header.getNumSamples()];
        for (int i = 0; i < header.getNumSamples(); i++) {
            audioData[i] = audioBytes.getShort();
        }

        return new Builder()
                .bitDepth(header.getBitDepth())
                .channels(header.getNumChannels())
                .sampleRate(header.getSampleRate())
                .data(audioData)
                .build();
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Copy internal audio data to the given buffer.
     */
    public void getAudioData(short[] buffer) {
        if (buffer.length < numSamples)
            throw new IllegalArgumentException("Output buffer too small");
        System.arraycopy(audioData, 0, buffer, 0, audioData.length);
    }

    public int getNumSamples() {
        return numSamples;
    }

    private ByteBuffer audioAsByteBuffer() {
        ByteBuffer audio = ByteBuffer.allocate(audioDataSize);
        audio.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : audioData) { audio.putShort(s); }
        audio.flip();
        return audio;
    }

}
