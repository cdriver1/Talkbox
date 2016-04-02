package talkbox.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import talkbox.lib.*;

/**
 * This class is responsible for writing received files.
 */
public class FileGetter {
	public final FileMessage fm;
	private final File f;
	private final BufferedOutputStream out;
	private final LinkedList<FilePacket> packets;
	private long written = 0;
	private int next = 0;
	private boolean closed = false;

	/**
	 * Create a new FileGetter for a shared file.
	 *
	 * @param fm The shared file to get.
	 * @param f The File to write the shared file to.
	 * @throws IOException
	 */
	public FileGetter(FileMessage fm, File f) throws IOException {
		this.fm = fm;
		this.f = f;
		f.createNewFile();
		out = new BufferedOutputStream(new FileOutputStream(f, false));
		packets = new LinkedList<>();
	}

	/**
	 * @return How many bytes have been written by this FileGetter.
	 */
	public long getWrittenBytes() {
		return written;
	}

	/**
	 * Once the last FilePacket has been written, the FileGetter is closed.
	 *
	 * @return True if closed, false otherwise.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * When a FilePacket has arrived for this FileGetter, write it if it is the
	 * next FilePacket or hold it until the next FilePacket shows up.
	 *
	 * @param p The FilePacket to write.
	 */
	public void receivePacket(FilePacket p) {
		if(closed) {
			return;
		}
		if(p.sequence == next) {
			try {
				writePacket(p);
				return;
			} catch(IOException e) {
			}
		}
		synchronized(packets) {
			int i = -Collections.binarySearch(packets, p);
			i -= 1;
			packets.add(i, p);
		}
	}

	/**
	 * Write a FilePacket. This method does not check if the passed in
	 * FilePacket is the next in the sequence. It will write stored FilePackets
	 * if they come after the passed in FilePacket.
	 *
	 * @param p The FilePacket to write.
	 * @throws IOException
	 */
	protected void writePacket(FilePacket p) throws IOException {
		if(closed) {
			return;
		}
		out.write(p.getBytes());
		next = p.sequence + 1;
		written += p.getBytes().length;
		if(p.isLastPacket()) {
			out.flush();
			out.close();
			closed = true;
			return;
		}
		synchronized(packets) {
			while(packets.size() > 0 && packets.peek().sequence == next) {
				p = packets.get(0);
				out.write(p.getBytes());
				written += p.getBytes().length;
				packets.remove(0);
				if(p.isLastPacket()) {
					out.flush();
					out.close();
					closed = true;
					return;
				}
			}
		}
	}
}
