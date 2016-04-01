package talkbox.client;

import talkbox.lib.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class FileSender implements Runnable {
	private static final ExecutorService threadPool = Executors.newCachedThreadPool();
	private final Backend backend;
	private final FileMessage fm;
	private final BufferedInputStream in;
	public final long from, to;

	public FileSender(Backend backend, FileMessage fm) throws IOException {
		this.backend = backend;
		this.fm = fm;
		in = new BufferedInputStream(new FileInputStream(fm.file));
		from = 0;
		to = fm.file.length();
	}

	public void start() {
	}

	public static FileSender FileSender(Backend backend, FileMessage fm) throws IOException {
		FileSender fs = new FileSender(backend, fm);
		fs.start();
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
						while(r < FilePacket.preferredSize) {
							r += in.read(bytes, r, FilePacket.preferredSize - r);
						}
						if(last == null) {
							last = new FilePacket(fm, bytes, size);
						} else {
							last = last.nextPacket(bytes);
						}
						size -= FilePacket.preferredSize;
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
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
