package talkbox.lib;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.*;

/**
 * Reads data from the input channel and writes to the output stream
 */
public class CaptureAudio implements Runnable {

    TargetDataLine line;
    volatile Thread thread;
    AudioInputStream audioInputStream;
    ByteArrayOutputStream out;
    byte[] audioBytes;
    String errStr;
    AudioFormat format;
    boolean running = true;
    private CompletableFuture<byte[]> cfBytes = null;

    public void start() {
        running = true;
        new Thread(this).start();
    }
    public void stop() {
        running = false;
    }

    private void shutDown(String message) {
        if ((errStr = message) != null && thread != null) {
            thread = null;
            System.err.println(errStr);
        }
    }

    public synchronized CompletableFuture<byte[]> getAudioBytes() {
        if(cfBytes == null)
            cfBytes = new CompletableFuture<>();
        return cfBytes;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public void setFormat() {
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        float rate = 8000.0f;
        int channels = 1;
        int frameSize = 4;
        int sampleSize = 16;
        boolean bigEndian = true;

        format = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8)
                * channels, rate, bigEndian);
    }

    public void run() {
        audioInputStream = null;

        // define the required attributes for our line,
        // and make sure a compatible line is supported.
        setFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            shutDown("Line matching " + info + " not supported.");
            return;
        }

        // get and open the target data line for capture.
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format, line.getBufferSize());
        } catch (LineUnavailableException ex) {
            shutDown("Unable to open the line: " + ex);
            return;
        } catch (SecurityException ex) {
            shutDown(ex.toString());
            return;
        } catch (Exception ex) {
            shutDown(ex.toString());
            return;
        }

        // play back the captured audio data
        out = new ByteArrayOutputStream();
        int frameSizeInBytes = format.getFrameSize();
        int bufferLengthInFrames = line.getBufferSize() / 8;
        int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
        byte[] data = new byte[bufferLengthInBytes];
        int numBytesRead;

        line.start();
        
        while (running) {
            try {
                if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1)
                    break;
                
                out.write(data, 0, numBytesRead);
            } catch (Exception ie) {                
            shutDown(ie.toString());
            }
        }
        audioBytes = out.toByteArray();
        // load bytes into the audio input stream for playback
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        if((frameSizeInBytes = format.getFrameSize()) < 1)
            frameSizeInBytes = 1;
        
        audioInputStream = new AudioInputStream(bais, format, audioBytes.length/frameSizeInBytes);
        out.reset();
        try {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
        } catch (IOException ex) {
            Logger.getLogger(CaptureAudio.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        getAudioBytes().complete(out.toByteArray());
        // we reached the end of the stream.
        // stop and close the line.
        line.stop();
        line.close();
        line = null;
		cfBytes = null;
    }
} // End class Capture