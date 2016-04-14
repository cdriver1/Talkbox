package talkbox.lib;

import java.io.*;
import java.net.Socket;
import javax.sound.sampled.*;
  /**
   * Reads data from the input channel and writes to the output stream
   */
  public class CaptureAudio implements Runnable {

    TargetDataLine line;
    volatile Thread thread;
	double duration, seconds;
	final int bufSize = 16384;
	AudioInputStream audioInputStream;
	ByteArrayOutputStream out;
	byte[] audioBytes;
	String errStr;	
	AudioFormat format;
	boolean running = true;

    // public void start() {
      // errStr = null;
      // thread = new Thread(this);
      // thread.setName("Capture");
      // thread.start();
    // }
	
	public synchronized void waitOrNotify(){
		running = !running;
		System.out.println("running = " + running);
		if(!running)
			notify();
	}

    public void stop() {		
		System.out.println("In capture.stop()");
      running = false;
    }

    private void shutDown(String message) {
      if ((errStr = message) != null && thread != null) {
        thread = null;
        System.err.println(errStr);
      }
    }
	
	public byte[] getAudioBytes(){
		System.out.println("In capture.getaudioBytes()");
		System.out.println(" " + audioBytes.length);
		System.out.println("In capture before accessing audioBytes");
		return audioBytes;
	}
	
	public AudioFormat getFormat(){
		return format;
	}
	public void setFormat(){
		AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
      float rate = 44100.0f;
      int channels = 2;
      int frameSize = 4;
      int sampleSize = 16;
      boolean bigEndian = true;

      format = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8)
          * channels, rate, bigEndian);
	}

    public void run() {
	  System.out.println("In capture run()");
      duration = 0;
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
	  System.out.println("In capture before at line 78");
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format, line.getBufferSize());
      } catch (LineUnavailableException ex) {
        shutDown("Unable to open the line: " + ex);
        return;
      } catch (SecurityException ex) {
        shutDown(ex.toString());
        //JavaSound.showInfoDialog();
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
	  System.out.println("In capture after line.start()");
		//Thread curThread = Thread.currentThread();
      while (running) {
		  try{
			    if (running) {
                    synchronized(this) {
                        while (running)
                            wait();
                    }
                }
				if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
					System.out.println("In capture.run() while.if");
					break;
				}
				out.write(data, 0, numBytesRead);
				System.out.println("In capture.run.while after out.write");
				
			} catch(InterruptedException ie){ }
		}
        
      
	  
		System.out.println("In capture.run() after while loop");
	  // stop and close the output stream
      try {
        out.flush();
        out.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
		audioBytes = out.toByteArray();
		System.out.println("in capture.run() after audiobytes = out.toByteArray " + audioBytes.length);
      // we reached the end of the stream.
      // stop and close the line.
      line.stop();
      line.close();
      line = null;
	  System.out.println("In capture after line.stop()");

      

      // load bytes into the audio input stream for playback

      
	  System.out.println("In capture: audioBytes.length = " + audioBytes.length);
      ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
      audioInputStream = new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);

      long milliseconds = (long) ((audioInputStream.getFrameLength() * 1000) / format
          .getFrameRate());
      duration = milliseconds / 1000.0;

      try {
        audioInputStream.reset();
      } catch (Exception ex) {
        ex.printStackTrace();
        return;
      }

    }
  } // End class Capture