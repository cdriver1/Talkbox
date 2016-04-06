package talkbox.client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import talkbox.lib.*;

/**
 * This is the backend for the client. It is responsible for coordinating
 * between the GUI and networking. For ease of use, the main thread should
 * create a new backend with the static method Backend.Backend().
 */
public class Backend implements Runnable {
	public static final String hostname = "localhost";
	public static final int port = 5476;

	/**
	 * Create a new Backend, start it, then return it.
	 *
	 * @param controller The controller of the GUI.
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
	private final ConcurrentHashMap<String, FileGetter> downloadingFiles;
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
		downloadingFiles = new ConcurrentHashMap<>();
	}

	public Backend(ChatWindowController controller) {
		this.controller = controller;
		sendQueue = new ConcurrentLinkedQueue<>();
		started = new AtomicBoolean();
		thread = new Thread(this);
		clientMap = new ConcurrentHashMap<>();
		sharedFiles = new ConcurrentHashMap<>();
		downloadingFiles = new ConcurrentHashMap<>();
	}

	/**
	 * Set the controller this Backend should talk to. If the controller has
	 * already been set, this method will do nothing.
	 *
	 * @param controller The ChatWindowController to talk to.
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
	 * Start this Backend. If it has already been started, it will not be
	 * started again.
	 */
	public void start() {
		if(started.getAndSet(true)) {
			return;
		}
		thread.start();
	}

	/**
	 * Stop this Backend. Once stopped, it can not be restarted.
	 */
	public void stop() {
		running = false;
		resume();
	}

	/**
	 * Change the user's screen name. This method should usually be called with
	 * announce as true so that other Clients will be notified.
	 *
	 * @param name The new name to use.
	 * @param announce Should the name change send a message to everyone.
	 */
	public void changeName(String name, boolean announce) {
		if(self == null) {
			setNameAtStart = name;
			return;
		}
		String oldname = self.getName();
		self.setName(name);
		self.nameChangeProcessed();
		clientMap.put(self.id, self);
		controller.addOnline(self);
		if(announce) {
			sendMessage(oldname + " has changed their name to " + name);
		}
	}

	/**
	 * Queue a single message for sending.
	 *
	 * @param m The Message to send.
	 */
	public void sendMessage(Message m) {
		sendQueue.add(m);
		//resume();
	}

	/**
	 * Queue a single message for sending.
	 *
	 * @param s The text of the Message to send.
	 */
	public void sendMessage(String s) {
		sendQueue.add(new Message(self, s));
		//resume();
	}

	/**
	 * Queue multiple messages for sending.
	 *
	 * @param m An array of Messages to send.
	 */
	public void sendMessages(Message[] m) {
		sendQueue.addAll(Arrays.asList(m));
		//resume();
	}

	/**
	 * Queue a single ImageMessage.
	 *
	 * @param f The File of the image to send.
	 * @throws IOException
	 */
	public void sendImage(File f) throws IOException {
		sendQueue.add(new ImageMessage(self, f));
	}

	/**
	 * Queue a single ImageMessage with text.
	 *
	 * @param s The text of the Message to send.
	 * @param f The File of the image to send.
	 * @throws IOException
	 */
	public void sendImage(String s, File f) throws IOException {
		sendQueue.add(new ImageMessage(self, s, f));
	}

	/**
	 * Queue a single ImageMessage with text and specified recipients.
	 *
	 * @param s The text of the Message to send.
	 * @param f The File of the image to send.
	 * @param recipients The intended recipients.
	 * @throws IOException
	 */
	public void sendImage(String s, File f, Client... recipients) throws IOException {
		sendQueue.add(new ImageMessage(self, s, f, recipients));
	}

	/**
	 * Queue a single FileMessage.
	 *
	 * @param f The File to share.
	 */
	public void sendFile(File f) {
		FileMessage fm = new FileMessage(self, f);
		sharedFiles.put(fm.name, fm);
		sendQueue.add(fm);
	}

	/**
	 * Queue a single FileMessage to specified recipients.
	 *
	 * @param f The File to share.
	 * @param recipients The intended recipients.
	 */
	public void sendFile(File f, Client... recipients) {
		FileMessage fm = new FileMessage(self, f, recipients);
		sharedFiles.put(fm.name, fm);
		sendQueue.add(fm);
	}

	/**
	 * Request a file from the sender of the FileMessage.
	 *
	 * @param fm The FileMessage representing the shared file.
	 * @param f the File to save the downloaded file to.
	 */
	public void getFile(FileMessage fm, File f) {
		try {
			if(isDownloading(fm)) {
				return;
			}
			downloadingFiles.put(fm.sender.id + fm.name, new FileGetter(fm, f));
			sendQueue.add(new FileMessage.FileRequest(self, fm));
		} catch(IOException ex) {
			Logger.getLogger(Backend.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * @param fm The FileMessage to check.
	 * @return True if it is being downloaded, false otherwise.
	 */
	public boolean isDownloading(FileMessage fm) {
		FileGetter get = downloadingFiles.get(fm.sender.id + fm.name);
		if(get != null && get.isClosed()) {
			downloadingFiles.remove(fm.sender.id + fm.name);
			return false;
		}
		return get != null;
	}

	/**
	 * A singe received message should be passed to this method. It will be
	 * processed and displayed.
	 *
	 * @param m The message that was received.
	 */
	public void receiveMessage(Message m) {
		if(m == null || m.sender == null) {
			return;
		}
		Client c = clientMap.get(m.sender.id);
		if(c == null) {
			addClient(m.sender);
		}
		if(m instanceof DataPacket) {
			if(m instanceof FilePacket) {
				FilePacket fp = (FilePacket)m;
				FileGetter get = downloadingFiles.get(fp.sender.id + fp.name);
				if(get != null) {
					get.receivePacket(fp);
					if(get.isClosed()) {
						downloadingFiles.remove(fp.sender.id + fp.name);
					}
				}
			}
		} else if(m instanceof FileMessage.FileRequest) {
			FileMessage.FileRequest fr = (FileMessage.FileRequest)m;
			FileMessage fm = sharedFiles.get(fr.name);
			if(fm != null) {
				try {
					FileSender.FileSender(this, fm, m.sender);
				} catch(IOException ex) {
					Logger.getLogger(Backend.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
		if(m.display()) {
			controller.receiveMessage(m);
		}
		//resume();
	}

	/**
	 * An array of received messages should be passed to this method. They will
	 * be processed and displayed.
	 *
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
	 *
	 * @param clients The ConcurrentHashMap to add.
	 */
	public void addClients(ConcurrentHashMap<String, Client> clients) {
		clients.values().stream().forEach((c) -> {
			Client old = clientMap.put(c.id, c);
			if(old != null && c.getName() == null) {
				c.setName(old.getName());
			}
		});
		updateClientList();
	}

	/**
	 * Like {@link #addClients(ConcurrentHashMap)}, but for a HashMap.
	 *
	 * @param clients The HashMap to add.
	 */
	public void addClients(HashMap<String, Client> clients) {
		clients.values().stream().forEach((c) -> {
			Client old = clientMap.put(c.id, c);
			if(old != null && c.getName() == null) {
				c.setName(old.getName());
			}
		});
		updateClientList();
	}

	/**
	 * Add a single Client.
	 *
	 * @param client The Client to add.
	 */
	public void addClient(Client client) {
		clientMap.put(client.id, client);
		controller.addOnline(client);
	}

	/**
	 * Remove a Client.
	 *
	 * @param client The Client to remove.
	 */
	public void removeClient(Client client) {
		clientMap.remove(client.id);
		controller.removeOnline(client);
	}

	/**
	 * Find out if a Client is connected.
	 *
	 * @param client The client to check.
	 * @return True if the Client is connected, false otherwise.
	 */
	public boolean hasClient(Client client) {
		return clientMap.get(client.id) != null;
	}

	/**
	 * Update the GUI with the latest names.
	 */
	public void updateClientList() {
		Client[] clients = clientMap.values().toArray(new Client[0]);
		controller.setOnlineNames(clients);
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
		//TODO: Change back to NetworkMethods
		try(Socket s = new Socket(hostname, port)) {
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(s.getInputStream());
			self = (Client)in.readObject();
			s.setSoTimeout(100);
			if(setNameAtStart != null) {
				self.setName(setNameAtStart);
				sendMessage(new Message(self, null));
			}
			while(running) {
				try {
					String ins = (String)in.readObject();
					s.setSoTimeout(0);
					switch(ins) {
						case "message":
							Message m = (Message)in.readObject();
							receiveMessage(m);
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
						receiveMessage(m);
					}
				}
			}
			out.reset();
			out.writeUTF("disconnect");
			out.flush();
			s.shutdownOutput();
		} catch(IOException | ClassNotFoundException ex) {
			Logger.getLogger(Backend.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
