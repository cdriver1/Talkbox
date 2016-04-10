/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package talkbox.lib;

import java.io.*;
import java.net.Socket;
import javax.sound.sampled.*;

/**
 * Reads data from the input channel and writes to the output stream
 */
public class CaptureAudio {

    // path of the wav file
    private File wavFile = new File("C:\\Users\\Charles\\Desktop\\Talkbox-cjdeakin\\AudioData\\wavFile.wav");
 
    // format of audio file
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
 
    // the line from which audio data is captured
    private TargetDataLine line;
 
    /**
     * Defines an audio format
     */
    private AudioFormat getAudioFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 8;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits,
                                             channels, signed, bigEndian);
        return format;
    }
 
    /**
     * Captures the sound and record into a WAV file
     */
    public void start() {
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
 
            // checks if system supports the data line
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line not supported");
                System.exit(0);
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();   // start capturing
 
            System.out.println("Start capturing...");
 
            AudioInputStream ais = new AudioInputStream(line);
 
            System.out.println("Start recording...");
 
            // start recording
            AudioSystem.write(ais, fileType, wavFile);
 
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e){
			System.out.println(e);
		}
    }
 
    /**
     * Closes the target data line to finish capturing and recording
     */
    public void stop() {
        line.stop();
        line.close();
        System.out.println("Finished");
    }
} // End class Capture

