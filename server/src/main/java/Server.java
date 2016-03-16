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
	private final HashMap<String, Client> clientMap;
	private boolean running = true, stopped = false;

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

	private void addClient(Client client) {
		synchronized(clientMap) {
			for(Client c : clientMap.values()) {
				try {
					c.writeObject("clientConnect");
					c.writeObject(client);
				} catch(IOException e) {
				}
			}
			clientMap.put(client.id, client);
		}
	}

	private void removeClient(Client client) {
		synchronized(clientMap) {
			clientMap.remove(client.id);
			for(Client c : clientMap.values()) {
				try {
					c.writeObject("clientDisconnect");
					c.writeObject(client);
				} catch(IOException e) {
				}
			}
		}
	}

	public Client[] getClients() {
		synchronized(clientMap) {
			return clientMap.values().toArray(new Client[0]);
		}
	}

	public void sendMessages(Message[] messages) {
		for(Message m : messages) {
			Client[] recipients = m.getRecipients();
			if(recipients == null || recipients.length == 0) {
				sendToAll(m);
				continue;
			}
			for(Client r : recipients) {
				Client s;
				synchronized(clientMap) {
					s = clientMap.get(r.id);
				}
				if(s == null) {
					continue;
				}
				try {
					s.writeObject("message");
					s.writeObject(m);
				} catch(IOException e) {
				}
			}
		}
	}

	public void sendToAll(Message message) {
		Client[] clients = getClients();
		for(Client c : clients) {
			if(c.id.equals(message.sender.id)) {
				continue;
			}
			try {
				c.writeObject("message");
				c.writeObject(message);
			} catch(IOException e) {
			}
		}
	}

	public synchronized void join() {
		if(stopped)
			return;
		try {
			wait();
		} catch(InterruptedException e) {
		}
	}

	private synchronized void unjoin() {
		notifyAll();
	}

	@Override
	public void run() {
		while(running) {
			try {
				Socket s = server.accept();
				Client c = new Client(s);
				addClient(c);
				threadPool.submit(new ServerClient(c));
			} catch(IOException e) {
			}
		}
		try {
			server.close();
		} catch(IOException e) {
		}
		stopped = true;
		unjoin();
	}

	private class ServerClient implements Runnable {
		private final Client client;
		private boolean connected = true;
		private ServerClient(Client client) {
			this.client = client;
		}

		@Override
		public void run() {
			try {
				System.out.println(client.id + ": connected");
				client.writeObject(client);
				synchronized(clientMap) {
					client.writeObject("clients");
					client.writeObject(clientMap);
				}
				while(connected) {
					String ins = client.readUTF();
					switch(ins) {
						case "message":
							try {
								Message[] messages = (Message[])client.readObject();
								client.setName(messages[messages.length - 1].sender.getName());
								System.out.println(client.id +": " + client.getName() + " sent " + messages.length + " messages.");
								sendMessages(messages);
							} catch(ClassNotFoundException e) {
							}
							break;
						case "disconnect":
							connected = false;
							client.getSocket().shutdownOutput();
							break;
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
			try {
				client.getSocket().close();
			} catch(IOException e) {
			}
			connected = false;
			removeClient(client);
			System.out.println(client.id + ": disconnected");
		}
	}
}
