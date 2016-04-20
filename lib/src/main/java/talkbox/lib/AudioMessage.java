package talkbox.lib;

import javax.sound.sampled.AudioFormat;

public class AudioMessage extends Message {
	private static final long serialVersionUID = -2015517444054302221L;
	private byte[] audioBytes;
	
	public AudioMessage(Client sender, byte[] audioBytes){
		super(sender, "audioMessage");
		this.audioBytes = audioBytes;
	}
	
	public byte[] getAudioBytes(){
		return audioBytes;
	}
	
	
}