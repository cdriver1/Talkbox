package talkbox.server;

import talkbox.lib.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Server implements Runnable {
	public final int port;
	private final ServerSocket server;
	private final Thread thread;
	private final AtomicBoolean started;
	private final ExecutorService threadPool;
	private final HashMap<String, ClientManager> clientMap;
	private boolean running = true;

	public Server(int port) throws IOException {
		server = new ServerSocket(port);
		server.setSoTimeout(250);
		this.port = server.getLocalPort();
		threadPool = Executors.newCachedThreadPool();
		clientMap = new HashMap<>();
		started = new AtomicBoolean();
		thread = new Thread(this);
	}
	public Server(int port, Thread thread) throws IOException {
		server = new ServerSocket(port);
		server.setSoTimeout(250);
		this.port = server.getLocalPort();
		threadPool = Executors.newCachedThreadPool();
		clientMap = new HashMap<>();
		started = new AtomicBoolean();
		this.thread = thread;
	}

	public void start() {
		if(started.getAndSet(true))
			return;
		thread.start();
	}

	public void stop() {
		running = false;
	}

	public void removeClient(Client client) {
		synchronized(clientMap) {
			clientMap.remove(client);
		}
	}

	public void sendMessages(Message[] messages) {
		for(Message m : messages) {
			Client[] recipients = m.getRecipients();
			if(recipients.length == 0) {
				sendToAll(m);
				continue;
			}
			for(Client recipient : recipients) {
				if(recipient.id.equals(m.sender.id))
					continue;
				ClientManager cm;
				synchronized(clientMap) {
					cm = clientMap.get(recipient.id);
				}
				if(cm == null)
					continue;
				cm.sendMessage(m);
			}
		}
	}

	public void sendToAll(Message message) {
		ClientManager[] managers;
		synchronized(clientMap) {
			managers = clientMap.values().toArray(new ClientManager[0]);
		}
		for(ClientManager manager : managers) {
			if(message.sender.id == manager.client.id)
				continue;
			manager.sendMessage(message);
		}
	}

	@Override
	public void run() {
		while(running) {
			try {
				Socket s = server.accept();
				Client c = new Client(s);
				ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(s.getInputStream());
				ClientManager cm = new ClientManager(this, c, out, in);
				synchronized(clientMap) {
					clientMap.put(c.id, cm);
				}
				threadPool.submit(cm);
			} catch(IOException e) {
			}
		}
		try {
			server.close();
		} catch(IOException e) {
		}
	}
}
