package talkbox.lib;

/**
 * This is a convenience class for passing files to Clients.
 */
public class FilePacket extends FileMessage implements DataPacket {
	public static final int preferredSize = 1024;
	public final byte[] bytes;
	public final int start;
	public final int sequence;
	public final long totalBytes;

	/**
	 * Create a new FilePacket.
	 * @param   fm The FileMessage this FilePacket is related to.
	 * @param   bytes The bytes to encapsulate in this FilePacket.
	 * @param   start The index of the first byte, in the context of the entire FilePacket sequence.
	 * @param   sequence The next packet is sequence + 1, the previous is sequence - 1.
	 * @param   totalBytes The total number of bytes contained by the entire sequence.
	 */
	public FilePacket(FileMessage fm, byte[] bytes, int start, int sequence, long totalBytes) {
		super(fm);
		this.bytes = bytes;
		this.start = start;
		this.sequence = sequence;
		this.totalBytes = totalBytes;
	}

	/**
	 * Create a new FilePacket that is the first in a sequence.
	 * @param   fm The FileMessage this FilePacket is related to.
	 * @param   bytes The bytes to encapsulate in this FilePacket.
	 * @param   totalBytes The total number of bytes contained by the entire sequence.
	 */
	public FilePacket(FileMessage fm, byte[] bytes, long totalBytes) {
		super(fm);
		this.bytes = bytes;
		this.start = 0;
		this.sequence = 0;
		this.totalBytes = totalBytes;
	}

	/**
	 * Create a new FilePacket that is the first in a sequence.
	 * This constructor should be used with {@link #nextPacket} methods to break
	 * the byte array into several FilePackets.
	 * @param   fm The FileMessage this FilePacket is related to.
	 * @param   bytes The entire array of bytes to be sent by this sequence.
	 * @param   size The size of the byte array encapsulated by this FilePacket.
	 */
	public FilePacket(FileMessage fm, byte[] bytes, int size) {
		super(fm);
		if(size > bytes.length) {
			size = bytes.length;
		}
		byte[] b = new byte[size];
		System.arraycopy(bytes, 0, b, 0, size);
		this.bytes = b;
		start = 0;
		sequence = 0;
		totalBytes = bytes.length;
	}

	/**
	 * Create a new FilePacket that encapsulates all the bytes to be transmitted.
	 * @param   fm The FileMessage this FilePacket is related to.
	 * @param   bytes The byte array to encapsulate.
	 */
	public FilePacket(FileMessage fm, byte[] bytes) {
		super(fm);
		this.bytes = bytes;
		this.start = 0;
		this.sequence = 0;
		this.totalBytes = bytes.length;
	}

	/**
	 * Generate the next FilePacket in this sequence.
	 * If this FilePacket is the last one, return null.
	 * @param  bytes The byte array to be used in the next packet.
	 * @return The next FilePacket, or null if this is the last one.
	 */
	public FilePacket nextPacket(byte[] bytes) {
		if(isLastPacket()) {
			return null;
		}
		return new FilePacket(this, bytes, start + this.bytes.length, sequence + 1, totalBytes);
	}

	/**
	 * Generate the next FilePacket of a certain size.
	 * This method can break a large byte array into several FilePackets.
	 * @param   bytes The byte array containing the data to be copied.
	 * @param   size The size of the byte array of the generated FilePacket.
	 * @return  The next FilePacket or null if this is the last FilePacket.
	 */
	public FilePacket nextPacket(byte[] bytes, int size) {
		if(isLastPacket()) {
			return null;
		}
		int s = start + this.bytes.length;
		if(s + size >= bytes.length) {
			size = bytes.length - s;
		}
		byte[] b = new byte[size];
		System.arraycopy(bytes, s, b, 0, size);
		return nextPacket(b);
	}

	/**
	 * Convenience function to generate an array of FilePackets from a byte array.
	 * This function makes FilePackets of 512 kB.
	 * @param fm The FileMessage the FilePackets is related to.
	 * @param bytes All of the bytes to be encapsulated.
	 */
	public static FilePacket[] generatePackets(FileMessage fm, byte[] bytes) {
		return generatePackets(fm, bytes, preferredSize);
	}

	/**
	 * Convenience function to generate an array of FilePackets from a byte array.
	 * @param fm The FileMessage the FilePackets are related to.
	 * @param bytes All of the bytes to be encapsulated.
	 * @param size How many bytes each FilePacket should contain.
	 */
	public static FilePacket[] generatePackets(FileMessage fm, byte[] bytes, int size) {
		int packets = bytes.length / size;
		FilePacket[] packet = new FilePacket[packets];
		packet[0] = new FilePacket(fm, bytes, size);
		for(int i = 1; i < packets; i++) {
			packet[i] = packet[i - 1].nextPacket(bytes, size);
		}
		return packet;
	}

	/**
	 * @return True if this is the last packet in a sequence, false otherwise.
	 */
	public boolean isLastPacket() {
		return totalBytes == start + bytes.length;
	}

	/**
	 * @return The bytes encapsulated by this FilePacket.
	 */
	@Override
	public byte[] getBytes() {
		return bytes;
	}
}
