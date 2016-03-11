package talkbox.client;

import java.util.concurrent.atomic.AtomicBoolean;
import talkbox.lib.Client;

public class Backend implements Runnable {
	public static Backend Backend() {
		Backend b = new Backend();
		b.start();
		Client.createID(new java.net.Socket(), new java.util.Date());
		return b;
	}

	private final Thread thread;
	private final AtomicBoolean started;

	public Backend() {
		started = new AtomicBoolean();
		thread = new Thread(this);
	}

	public void start() {
		if(started.getAndSet(true))
			return;
		thread.start();
	}

	@Override
	public void run() {
	}
}
