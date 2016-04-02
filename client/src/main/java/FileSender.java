package talkbox.client;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import talkbox.lib.*;

public class FileSender implements Runnable {
	private final Backend backend;
	private final FileMessage fm;
	private final BufferedInputStream in;
	private final List<Client> recipients;
	public final long from, to;

	protected FileSender(Backend backend, FileMessage fm, Client... recipients) throws IOException {
		this.backend = backend;
		this.fm = fm;
		in = new BufferedInputStream(new FileInputStream(fm.file));
		from = 0;
		to = fm.file.length();
		this.recipients = Arrays.asList(recipients);
	}

	public static FileSender FileSender(Backend backend, FileMessage fm, Client... recipients) throws IOException {
		FileSender fs = new FileSender(backend, fm, recipients);
		Main.submit(fs);
		return fs;
	}

	@Override
	public void run() {
		try {
			long size = fm.file.length();
			if(size <= FilePacket.preferredSize) {
				byte[] bytes = new byte[(int)size];
				int r = 0;
				while(r < size) {
					r += in.read(bytes, r, (int)(size - r));
				}
				FilePacket fp = new FilePacket(fm, bytes);
				backend.sendMessage(fp);
			} else {
				FilePacket last = null;
				while(size > 0) {
					if(size > FilePacket.preferredSize) {
						byte[] bytes = new byte[FilePacket.preferredSize];
						int r = 0;
						while(r < FilePacket.minimumSize) {
							r += in.read(bytes, r, FilePacket.preferredSize - r);
						}
						if(r != FilePacket.preferredSize) {
							byte[] bb = new byte[r];
							System.arraycopy(bytes, 0, bb, 0, r);
							bytes = bb;
						}
						if(last == null) {
							last = new FilePacket(fm, bytes, size);
						} else {
							last = last.nextPacket(bytes);
						}
						size -= r;
					} else {
						int s = (int)size;
						byte[] bytes = new byte[s];
						int r = 0;
						while(r < s) {
							r += in.read(bytes, r, s - r);
						}
						last = last.nextPacket(bytes);
						size = 0;
					}
					backend.sendMessage(last);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
