package talkbox.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import talkbox.lib.*;

/**
 * This class keeps track of all connected clients and forwards sent messages.
 */
public class Server implements Runnable {
	public final int port;
	private final ServerSocket server;
	private final Thread thread;
	private final AtomicBoolean started;
	private final ExecutorService threadPool;
	private final HashMap<String, Client> clientMap;
	private boolean running = true, stopped = false;

	/**
	 * Create a new Server that listens on a specified port. The new server will
	 * have its own Thread.
	 *
	 * @param port The port to listen on.
	 * @throws IOException
	 */
	public Server(int port) throws IOException {
		server = new ServerSocket(port);
		server.setSoTimeout(250);
		this.port = server.getLocalPort();
		threadPool = Executors.newCachedThreadPool();
		clientMap = new HashMap<>();
		started = new AtomicBoolean();
		thread = new Thread(this);
	}

	/**
	 * Create a new server that listens on a specified port.
	 *
	 * @param port The port to listen on.
	 * @param thread Should a Thread be constructed for this Server.
	 * @throws IOException
	 */
	public Server(int port, boolean thread) throws IOException {
		server = new ServerSocket(port);
		server.setSoTimeout(250);
		this.port = server.getLocalPort();
		threadPool = Executors.newCachedThreadPool();
		clientMap = new HashMap<>();
		started = new AtomicBoolean();
		if(thread) {
			this.thread = new Thread(this);
		} else {
			this.thread = null;
		}
	}

	/**
	 * Start this server. If it has already been started it will not be started
	 * again. This method is for use when the Server has its own Thread.
	 */
	public void start() {
		if(started.getAndSet(true)) {
			return;
		}
		if(thread != null) {
			thread.start();
		}
	}

	/**
	 * Stop this server.
	 */
	public void stop() {
		running = false;
	}

	private void addClient(Client client) {
		synchronized(clientMap) {
			for(Client c : clientMap.values()) {
				try {
					c.writeObject("clientConnect");
					c.writeObject(client);
				} catch(IOException ex) {
					Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
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
				} catch(IOException ex) {
					Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * @return An array of all the connected Clients.
	 */
	public Client[] getClients() {
		synchronized(clientMap) {
			return clientMap.values().toArray(new Client[0]);
		}
	}

	/**
	 * Send messages to their intended recipients.
	 *
	 * @param messages An array of messages to send.
	 */
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
				} catch(IOException ex) {
					Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * Send a message to all connected Clients.
	 *
	 * @param message The message to send.
	 */
	public void sendToAll(Message message) {
		Client[] clients = getClients();
		for(Client c : clients) {
			if(c.id.equals(message.sender.id)) {
				continue;
			}
			try {
				c.writeObject("message");
				c.writeObject(message);
			} catch(IOException ex) {
				Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Wait for this Server to finish execution.
	 */
	public synchronized void join() {
		if(stopped) {
			return;
		}
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
			} catch(SocketTimeoutException ex) {
			} catch(IOException ex) {
				Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		try {
			server.close();
		} catch(IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
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
								System.out.println(client.id + ": " + client.getName() + " sent " + messages.length + " messages.");
								sendMessages(messages);
							} catch(ClassNotFoundException ex) {
								Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
							}
							break;
						case "disconnect":
							connected = false;
							client.getSocket().shutdownOutput();
							break;
					}
				}
			} catch(IOException ex) {
				Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
			}
			try {
				client.getSocket().close();
			} catch(IOException ex) {
				Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
			}
			connected = false;
			removeClient(client);
			System.out.println(client.id + ": disconnected");
		}
	}
}
