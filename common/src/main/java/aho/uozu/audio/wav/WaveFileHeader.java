package aho.uozu.audio.wav;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


class WaveFileHeader {
    private final Format format;
    private final short numChannels;
    private final int sampleRate;
    private final short bitDepth;
    private final int audioDataSize;
    private final int numSamples;

    public static final int HEADER_LEN = 44;

    private WaveFileHeader(Builder b) {
        format = b.format;
        numChannels = b.numChannels;
        sampleRate = b.sampleRate;
        bitDepth = b.bitDepth;
        audioDataSize = b.audioDataSize;
        numSamples = b.audioDataSize / getFrameSize();
    }

    public static class Builder {
        private Format format;
        private short numChannels;
        private int sampleRate;
        private short bitDepth;
        private int audioDataSize;

        public Builder() {}

        public Builder format(Format f) {
            this.format = f;
            return this;
        }

        public Builder channels(int numChannels) {
            this.numChannels = (short) numChannels;
            return this;
        }

        public Builder sampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder bitDepth(int bitDepth) {
            this.bitDepth = (short) bitDepth;
            return this;
        }

        public Builder dataSize(int size) {
            this.audioDataSize = size;
            return this;
        }

        public WaveFileHeader build() {
            return new WaveFileHeader(this);
        }
    }

    public static WaveFileHeader read(ByteBuffer buffer) {
        Builder builder = new Builder();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        // skip past unnecessary data
        buffer.getInt(); // riff
        buffer.getInt(); // size
        buffer.getInt(); // wave
        buffer.getInt(); // fmt
        buffer.getInt(); // fmt length
        builder.format(Format.fromShort(buffer.getShort()));
        builder.channels(buffer.getShort());
        builder.sampleRate(buffer.getInt());
        buffer.getInt(); // byte rate
        buffer.getShort(); // frame size
        builder.bitDepth(buffer.getShort());
        buffer.getInt(); // 'data'
        builder.dataSize(buffer.getInt());
        return builder.build();
    }

    public ByteBuffer asByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_LEN);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (byte b : "RIFF".getBytes()) { buffer.put(b); }      // 00: RIFF
        buffer.putInt(HEADER_LEN + audioDataSize - 8);           // 04: total size - 8
        for (byte b : "WAVE".getBytes()) { buffer.put(b); }      // 08: WAVE
        for (byte b : "fmt ".getBytes()) { buffer.put(b); }      // 12: fmt
        buffer.putInt(16);                                       // 16: length of 'fmt' section - always 16
        buffer.putShort((short) format.code());                  // 20: format
        buffer.putShort(numChannels);                            // 22: num channels
        buffer.putInt(sampleRate);                               // 24: sample rate
        buffer.putInt(getByteRate());                            // 28: byte rate
        buffer.putShort(getFrameSize());                         // 32: frame size
        buffer.putShort(bitDepth);                               // 34: bit depth
        for (byte b : "data".getBytes()) { buffer.put(b); }      // 36: data
        buffer.putInt(audioDataSize);                            // 40: audio data size
        // flip buffer - now ready for reading (?)
        buffer.flip();
        return buffer;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public int getNumSamples() {
        return numSamples;
    }

    /**
     * Get the size of the audio data, in bytes
     */
    public int getAudioDataSize() {
        return audioDataSize;
    }

    private int getByteRate() {
        return sampleRate * numChannels * bitDepth / 8;
    }

    private short getFrameSize() {
        return (short) (numChannels * bitDepth / 8);
    }

    // -------------------------------------------------------------
    // private tests

    private static WaveFileHeader buildTestHeader() {
        return new Builder()
                .format(Format.PCM)
                .bitDepth(16)
                .dataSize(10)
                .sampleRate(22050)
                .channels(2)
                .build();
    }

    private static void myAssert(boolean condition) {
        if (!condition)
            throw new AssertionError();
    }

    private static void assertIsTestHeader(WaveFileHeader header) {
        myAssert(header.format == Format.PCM);
        myAssert(header.bitDepth == 16);
        myAssert(header.audioDataSize == 10);
        myAssert(header.sampleRate == 22050);
        myAssert(header.numChannels == 2);
    }

    private static void buildTest() {
        WaveFileHeader header = buildTestHeader();
        assertIsTestHeader(header);
    }

    private static void readSelfTest() {
        WaveFileHeader header = buildTestHeader();
        WaveFileHeader header2 = WaveFileHeader.read(header.asByteBuffer());
        assertIsTestHeader(header2);
    }

    public static void main(String[] args) {
        buildTest();
        readSelfTest();
        System.out.println("tests passed");
    }
}
