package wav;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WaveFile {
    private void properWAV(short[] data, String path) {

        int numSamples = data.length;
        int numChannels = 1;
        int bitsPerSample = 16;
        long sampleRate = 22100;

        long mySubChunk1Size = 16; // wtf is this? sample size in bits?
        int FORMAT_CODE_PCM = 1;
        long myChannels = 1;
        long byteRate = sampleRate * myChannels * bitsPerSample / 8;
        int myBlockAlign = (int) (myChannels * bitsPerSample / 8);

        long audioDataSize =  numSamples * numChannels * bitsPerSample / 8;
        // Not sure if this is the total file size,
        // but it's what goes into bytes 4-7 of the wave file
        long totalFileSize = 36 + audioDataSize;

        OutputStream os;
        try {
            os = new FileOutputStream(new File(path));
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);

            // TODO: endian-ness correct?
            // maybe use ByteBuffer here, which allows setting of byte order with order()
            outFile.writeBytes("RIFF");                                 // 00 - RIFF
            outFile.writeInt((int) totalFileSize);      // 04 - how big is the rest of this file?
            outFile.writeBytes("WAVE");                                 // 08 - WAVE
            outFile.writeBytes("fmt ");                                 // 12 - fmt
            outFile.writeInt((int) mySubChunk1Size);  // 16 - size of this chunk
            outFile.writeShort((short) FORMAT_CODE_PCM);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
            outFile.writeShort((short) numChannels);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
            outFile.writeInt((int) sampleRate);     // 24 - samples per second (numbers per second)
            outFile.writeInt((int) byteRate);       // 28 - bytes per second
            outFile.writeShort((short) myBlockAlign); // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray((short) bitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                                 // 36 - data
            outFile.write(intToByteArray((int) myDataSize), 0, 4);       // 40 - how big is this data chunk
            outFile.write(clipData);                                    // 44 - the actual data itself - just a long string of numbers

            outFile.flush();
            outFile.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
