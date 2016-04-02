package talkbox.lib;

import java.util.Arrays;

/**
 * This is a convenience class for passing files to Clients.
 */
public class FilePacket extends FileMessage implements DataPacket, Comparable<FilePacket> {
	private static final long serialVersionUID = -6321865498170925123L;
	public static final int preferredSize = 1024 * 512;
	public static final int minimumSize = 1024;
	public final byte[] bytes;
	public final int start;
	public final int sequence;
	public final long totalBytes;

	/**
	 * Create a new FilePacket.
	 *
	 * @param fm The FileMessage this FilePacket is related to.
	 * @param bytes The bytes to encapsulate in this FilePacket.
	 * @param start The index of the first byte, in the context of the entire
	 * FilePacket sequence.
	 * @param sequence The next packet is sequence + 1, the previous is sequence
	 * - 1.
	 * @param totalBytes The total number of bytes contained by the entire
	 * sequence.
	 */
	public FilePacket(FileMessage fm, byte[] bytes, int start, int sequence, long totalBytes) {
		super(fm.sender, fm);
		this.bytes = bytes;
		this.start = start;
		this.sequence = sequence;
		this.totalBytes = totalBytes;
	}

	/**
	 * Create a new FilePacket that is the first in a sequence.
	 *
	 * @param fm The FileMessage this FilePacket is related to.
	 * @param bytes The bytes to encapsulate in this FilePacket.
	 * @param totalBytes The total number of bytes contained by the entire
	 * sequence.
	 */
	public FilePacket(FileMessage fm, byte[] bytes, long totalBytes) {
		super(fm.sender, fm);
		this.bytes = bytes;
		this.start = 0;
		this.sequence = 0;
		this.totalBytes = totalBytes;
	}

	/**
	 * Create a new FilePacket that is the first in a sequence. This constructor
	 * should be used with {@link #nextPacket} methods to break the byte array
	 * into several FilePackets.
	 *
	 * @param fm The FileMessage this FilePacket is related to.
	 * @param bytes The entire array of bytes to be sent by this sequence.
	 * @param size The size of the byte array encapsulated by this FilePacket.
	 */
	public FilePacket(FileMessage fm, byte[] bytes, int size) {
		super(fm.sender, fm);
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
	 * Create a new FilePacket that encapsulates all the bytes to be
	 * transmitted.
	 *
	 * @param fm The FileMessage this FilePacket is related to.
	 * @param bytes The byte array to encapsulate.
	 */
	public FilePacket(FileMessage fm, byte[] bytes) {
		super(fm.sender, fm);
		this.bytes = bytes;
		this.start = 0;
		this.sequence = 0;
		this.totalBytes = bytes.length;
	}

	/**
	 * Generate the next FilePacket in this sequence. If this FilePacket is the
	 * last one, return null.
	 *
	 * @param bytes The byte array to be used in the next packet.
	 * @return The next FilePacket, or null if this is the last one.
	 */
	public FilePacket nextPacket(byte[] bytes) {
		if(isLastPacket()) {
			return null;
		}
		return new FilePacket(this, bytes, start + this.bytes.length, sequence + 1, totalBytes);
	}

	/**
	 * Generate the next FilePacket of a certain size. This method can break a
	 * large byte array into several FilePackets.
	 *
	 * @param bytes The byte array containing the data to be copied.
	 * @param size The size of the byte array of the generated FilePacket.
	 * @return The next FilePacket or null if this is the last FilePacket.
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
	 * Convenience function to generate an array of FilePackets from a byte
	 * array. This function makes FilePackets of size FilePacket.preferredSize.
	 *
	 * @param fm The FileMessage the FilePackets is related to.
	 * @param bytes All of the bytes to be encapsulated.
	 * @return The entire sequence of FilePackets stores in an array.
	 */
	public static FilePacket[] generatePackets(FileMessage fm, byte[] bytes) {
		return generatePackets(fm, bytes, preferredSize);
	}

	/**
	 * Convenience function to generate an array of FilePackets from a byte
	 * array.
	 *
	 * @param fm The FileMessage the FilePackets are related to.
	 * @param bytes All of the bytes to be encapsulated.
	 * @param size How many bytes each FilePacket should contain.
	 * @return
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
	
	@Override
	public boolean display() {
		return false;
	}

	/**
	 * @return The bytes encapsulated by this FilePacket.
	 */
	@Override
	public byte[] getBytes() {
		return bytes;
	}

	@Override
	public int compareTo(FilePacket fp) {
		if(fp.sequence == sequence) {
			return 0;
		}
		if(fp.sequence > sequence) {
			return -1;
		}
		return 1;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 23 * hash + Arrays.hashCode(this.bytes);
		hash = 23 * hash + this.start;
		hash = 23 * hash + this.sequence;
		hash = 23 * hash + (int)(this.totalBytes ^ (this.totalBytes >>> 32));
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		final FilePacket other = (FilePacket)obj;
		if(!Arrays.equals(this.bytes, other.bytes)) {
			return false;
		}
		if(this.start != other.start) {
			return false;
		}
		if(this.sequence != other.sequence) {
			return false;
		}
		return this.totalBytes == other.totalBytes;
	}
}
