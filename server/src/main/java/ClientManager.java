package talkbox.server;

import talkbox.lib.*;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientManager implements Runnable {
	private final Server server;
	public final Client client;
	private final Socket socket;
	private final ObjectInputStream in;
	private final ObjectOutputStream out;
	private final ConcurrentLinkedQueue<Message> sentMessages; //messages to send to the Client.
	private boolean running = true;

	public ClientManager(Server server, Client client, ObjectOutputStream out, ObjectInputStream in) {
		this.server = server;
		this.client = client;
		socket = client.getSocket();
		this.out = out;
		this.in = in;
		sentMessages = new ConcurrentLinkedQueue<>();
	}

	public void stop() {
		running = false;
	}

	public boolean isRunning() {
		return running;
	}

	public void sendMessage(Message m) {
		sentMessages.add(m);
	}

	public void sendMessages(Message[] m) {
		sentMessages.addAll(Arrays.asList(m));
	}

	@Override
	public void run() {
		try {
			while(running) {
				if(sentMessages.isEmpty()) {
					out.writeUTF("none");
				} else {
					Message[] m = sentMessages.toArray(new Message[0]);
					out.writeUTF("message");
					out.writeObject(m);
					for(Message mm: m)
						sentMessages.remove(m);
				}
				switch(in.readUTF()) {
					case "message":
						try {
							Message[] m = (Message[])in.readObject();
							server.sendMessages(m);
						} catch(ClassNotFoundException e) {
						}
						break;
					case "disconnect":
						running = false;
						break;
					case "none":
					default:
						break;
				}
			}
		} catch(IOException e) {
		}
		running = false;
		server.removeClient(client);
		try {
			socket.shutdownOutput();
		} catch(IOException e) {
		}
		try {
			socket.close();
		} catch(IOException e) {
		}
	}
}
