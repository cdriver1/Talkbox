package talkbox.client;

import talkbox.lib.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;

/**
 * This is the backend for the client. It is responsible for coordinating between the GUI and networking.
 * For ease of use, the main thread should create a new backend with the static method Backend.Backend().
 */
public class Backend implements Runnable {
	public static final String hostname = "java.cjdeakin.me";
	public static final int port = 5476;
	/**
	 * Create a new Backend, start it, then return it.
	 * @return A new Backend.
	 */
	public static Backend Backend(ChatWindowController controller) {
		Backend b = new Backend(controller);
		b.start();
		return b;
	}
	public static Backend Backend() {
		Backend b = new Backend();
		b.start();
		return b;
	}

	private ChatWindowController controller;
	private final Thread thread;
	private final AtomicBoolean started;
	private final ConcurrentLinkedQueue<Message> sendQueue;
	private final ConcurrentHashMap<String, Client> clientMap;
	private final ConcurrentHashMap<String, FileMessage> sharedFiles;
	private boolean running = true;
	private Client self;
	private String setNameAtStart;

	public Backend() {
		this.controller = null;
		sendQueue = new ConcurrentLinkedQueue<>();
		started = new AtomicBoolean();
		thread = new Thread(this);
		clientMap = new ConcurrentHashMap<>();
		sharedFiles = new ConcurrentHashMap<>();
	}

	public Backend(ChatWindowController controller) {
		this.controller = controller;
		sendQueue = new ConcurrentLinkedQueue<>();
		started = new AtomicBoolean();
		thread = new Thread(this);
		clientMap = new ConcurrentHashMap<>();
		sharedFiles = new ConcurrentHashMap<>();
	}

	/**
	 * Set the controller this Backend should talk to. If the controller has
	 * already been set, this method will do nothing.
	 * @param  controller The ChatWindowController to talk to.
	 * @return True if set, false otherwise.
	 */
	public boolean setController(ChatWindowController controller) {
		if(this.controller == null) {
			this.controller = controller;
			return true;
		}
		return false;
	}

	/**
	 * Start this Backend.
	 * If it has already been started, it will not be started again.
	 */
	public void start() {
		if(started.getAndSet(true))
			return;
		thread.start();
	}

	/**
	 * Stop this Backend.
	 * Once stopped, it can not be restarted.
	 */
	public void stop() {
		running = false;
		resume();
	}

	/**
	 * Change the user's screen name. This method should usually be called with
	 * annouce as true so that other Clients will be notified.
	 * @param  name The new name to use.
	 * @param  announce Should the name change send a message to everyone.
	 */
	public void changeName(String name, boolean announce) {
		if(self == null) {
			setNameAtStart = name;
			return;
		}
		String oldname = self.getName();
		self.setName(name);
		self.nameChangeProcessed();
		if(announce)
			sendMessage(oldname + " has changed their name to " + name);
	}

	/**
	 * Queue a single message for sending.
	 * @param m The Message to send.
	 */
	public void sendMessage(Message m) {
		sendQueue.add(m);
		//resume();
	}

	/**
	 * Queue a single message for sending.
	 * @param s The text of the Message to send.
	 */
	public void sendMessage(String s) {
		sendQueue.add(new Message(self, s));
		//resume();
	}

	/**
	 * Queue multiple messages for sending.
	 * @param m An array of Messages to send.
	 */
	public void sendMessages(Message[] m) {
		sendQueue.addAll(Arrays.asList(m));
		//resume();
	}

	/**
	 * Queue a single ImageMessage.
	 * @param f The File of the image to send.
	 * @throws IOException
	 */
	public void sendImage(java.io.File f) throws IOException {
		sendQueue.add(new ImageMessage(self, f));
	}

	/**
	 * Queue a single ImageMessage with text.
	 * @param s The text of the Message to send.
	 * @param f The File of the image to send.
	 * @throws IOException
	 */
	public void sendImage(String s, java.io.File f) throws IOException {
		sendQueue.add(new ImageMessage(self, s, f));
	}

	/**
	 * Queue a single ImageMessage with text and specified recipients.
	 * @param s The text of the Message to send.
	 * @param f The File of the image to send.
	 * @param recipients The intended recipients.
	 * @throws IOException
	 */
	public void sendImage(String s, java.io.File f, Client... recipients) throws IOException {
		sendQueue.add(new ImageMessage(self, s, recipients, f));
	}

	/**
	 * Queue a single FileMessage.
	 * @param f The File to share.
	 */
	public void sendFile(java.io.File f) {
		FileMessage fm = new FileMessage(self, f);
		sharedFiles.put(f.getAbsolutePath(), fm);
		sendQueue.add(fm);
	}

	/**
	 * Queue a single FileMessage to specified recipients.
	 * @param f The File to share.
	 * @param recipients The intended recipients.
	 */
	public void sendFile(java.io.File f, Client... recipients) {
		FileMessage fm = new FileMessage(self, f, recipients);
		sharedFiles.put(f.getAbsolutePath(), fm);
		sendQueue.add(fm);
	}

	/**
	 * A singe received message should be passed to this method.
	 * It will be processed and displayed.
	 * @param m The message that was received.
	 */
	public void receiveMessage(Message m) {
		Client c = clientMap.get(m.sender.id);
		if(c == null || !c.getName().equals(m.sender.getName())) {
			addClient(c);
		}
		controller.receiveMessage(m);
		//resume();
	}

	/**
	 * An array of received messages should be passed to this method.
	 * They will be processed and displayed.
	 * @param m The array of messages that were received.
	 */
	public void receiveMessages(Message[] m) {
		for(Message message : m) {
			receiveMessage(message);
		}
		//resume();
	}

	/**
	 * Add all Clients in a ConcurrentHashMap to this Backend.
	 * @param  clients The ConcurrentHashMap to add.
	 */
	public void addClients(ConcurrentHashMap<String, Client> clients) {
		for(Client c : clients.values()) {
			Client old = clientMap.put(c.id, c);
			if(old != null && c.getName() == null) {
				c.setName(old.getName());
			}
		}
		updateClientList();
	}

	/**
	 * Like {@link #addClients(ConcurrentHashMap)}, but for a HashMap.
	 * @param  clients The HashMap to add.
	 */
	public void addClients(HashMap<String, Client> clients) {
		for(Client c : clients.values()) {
			Client old = clientMap.put(c.id, c);
			if(old != null && c.getName() == null) {
				c.setName(old.getName());
			}
		}
		updateClientList();
	}

	/**
	 * Add a single Client.
	 * @param client The Client to add.
	 */
	public void addClient(Client client) {
		clientMap.put(client.id, client);
		updateClientList();
	}

	/**
	 * Remove a Client.
	 * @param client The Client to remove.
	 */
	public void removeClient(Client client) {
		clientMap.remove(client);
		updateClientList();
	}

	/**
	 * Update the GUI with the latest names.
	 */
	public void updateClientList() {
		Client[] clients = clientMap.values().toArray(new Client[0]);
		String[] names = new String[clients.length];
		Platform.runLater(() -> {
			int i = 0;
			for(Client c : clients) {
				names[i++] = c.getName();
			}
			controller.setOnlineNames(names);
		});
	}

	private synchronized void pause() {
		try {
			wait();
		} catch(InterruptedException e) {
		}
	}

	private synchronized void resume() {
		notifyAll();
	}

	@Override
	public void run() {
		/*self = NetworkMethods.openConnection(hostname, port);
		if(setNameAtStart != null) {
			self.setName(setNameAtStart);
		}
		while(running) {
			NetworkMethods.receiveMessage();
			if(!sendQueue.isEmpty()) {
				Message[] messages = sendQueue.toArray(new Message[0]);
				NetworkMethods.sendMessage(messages);
				for(Message m : messages) {
					controller.receiveMessage(m);
					sendQueue.remove(m);
				}
			}
		}
		try {
			NetworkMethods.closeConnection();
		} catch(IOException e) {
		}*/
		//TODO: Change back to NetworkMethods
		try(Socket s = new Socket(hostname, port)) {
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(s.getInputStream());
			self = (Client)in.readObject();
			s.setSoTimeout(100);
			if(setNameAtStart != null) {
				self.setName(setNameAtStart);
			}
			while(running) {
				try {
					String ins = (String)in.readObject();
					System.out.println(ins);
					s.setSoTimeout(0);
					switch(ins) {
						case "message":
							Message m = (Message)in.readObject();
							addClient(m.sender);
							controller.receiveMessage(m);
							break;
						case "clientDisconnect":
							removeClient((Client)in.readObject());
							break;
						case "clientConnect":
							addClient((Client)in.readObject());
							break;
						case "clients":
							addClients((HashMap<String, Client>)in.readObject());
							break;
						default:
							break;
					}
					s.setSoTimeout(100);
				} catch(SocketTimeoutException e) {
				}
				if(!sendQueue.isEmpty()) {
					Message[] messages = sendQueue.toArray(new Message[0]);
					out.reset();
					out.writeUTF("message");
					out.writeObject(messages);
					for(Message m : messages) {
						sendQueue.remove(m);
						controller.receiveMessage(m);
					}
				}
			}
			out.reset();
			out.writeUTF("disconnect");
			out.flush();
			s.shutdownOutput();
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
