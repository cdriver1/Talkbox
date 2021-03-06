package talkbox.lib;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

/**
 * This class represents a user. It contains a unique id and the screen name of
 * the user. If the program is connected directly, it also contains the Socket
 * representing that connection. If possible, the id is an MD5 hash of the
 * Socket and the Date the Client connected at. Otherwise the id is a
 * concatenation of the Client address and the Date the Client connected at.
 */
public class Client implements Serializable {
	public static final long serialVersionUID = 5477434248976l;
	/**
	 * The unique id of this Client.
	 */
	public final String id;
	private String name;
	private transient boolean nameChanged = false;
	private transient final Socket socket;
	private transient final ObjectInputStream in;
	private transient final ObjectOutputStream out;

	/**
	 * Create a new Client from a Socket.
	 *
	 * @param socket The Socket that connects to this Client.
	 * @throws java.io.IOException
	 */
	public Client(Socket socket) throws IOException {
		this.socket = socket;
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
		id = createID(socket, new Date());
	}

	/**
	 * Create a new Client from a Socket and a Date.
	 *
	 * @param socket The Socket that connects to this Client.
	 * @param date The Date when this Client connected.
	 * @throws java.io.IOException
	 */
	public Client(Socket socket, Date date) throws IOException {
		this.socket = socket;
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
		id = createID(socket, date);
	}

	/**
	 * Create a new Client from an id.
	 *
	 * @param id The unique id of this Client.
	 */
	public Client(String id) {
		this.socket = null;
		this.in = null;
		this.out = null;
		this.id = id;
	}

	/**
	 * Create a new Client from an id and a Socket.
	 *
	 * @param id The unique id of this Client.
	 * @param socket The Socket that connects to this Client.
	 */
	public Client(String id, Socket socket) throws IOException {
		this.id = id;
		this.socket = socket;
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
	}

	/**
	 * @return The current screenname of this Client.
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		if(name == null) {
			return "";
		}
		return name;
	}

	/**
	 * Change the screenname of this Client.
	 *
	 * @param name The new screenname.
	 */
	public void setName(String name) {
		if(this.name != null && this.name.equals(name)) {
			return;
		}
		this.name = name;
		nameChanged = true;
	}

	/**
	 * @return If the name has changed since nameChangeProcessed was last
	 * called, true, otherwise, false.
	 */
	public boolean hasNameChanged() {
		return nameChanged;
	}

	/**
	 * Call this method when a name change has been processed to mark it as
	 * processed.
	 */
	public void nameChangeProcessed() {
		nameChanged = false;
	}

	/**
	 * Get the Socket that is the connection to this Client.
	 *
	 * @return The Socket that connects to this Client, or null if the
	 * connection is not direct.
	 */
	public Socket getSocket() {
		return socket;
	}

	public void writeUTF(String str) throws IOException {
		out.reset();
		out.writeUTF(str);
		out.flush();
	}

	public void writeObject(Object o) throws IOException {
		out.reset();
		out.writeObject(o);
		out.flush();
	}

	public String readUTF() throws IOException {
		return in.readUTF();
	}

	public Object readObject() throws IOException, ClassNotFoundException {
		return in.readObject();
	}

	/**
	 * Create a MD5 hash from a Socket and a Date. Hopefully this provides a
	 * unique id.
	 *
	 * @param socket The Socket that connects to this Client.
	 * @param date The Date this Client connected at.
	 * @return A MD5 hash made from the Socket and Date.
	 * @throws java.security.NoSuchAlgorithmException
	 */
	public static String createMD5ID(Socket socket, Date date) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		if(socket != null) {
			md5.update(socket.getInetAddress().getAddress());
		}
		long t = System.nanoTime();
		for(int i = 0; i < Long.BYTES; i++) {
			md5.update((byte)t);
			t >>= 8;
		}
		byte[] digest = md5.digest(date.toString().getBytes());
		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString(digest);
	}

	/**
	 * Try to create a MD5ID, fall back on String concatenation if not
	 * available.
	 *
	 * @param socket The Socket that connectes to this Client.
	 * @param date The Date this Client connected at.
	 * @return A hopefully unique id.
	 */
	public static String createID(Socket socket, Date date) {
		try {
			return createMD5ID(socket, date);
		} catch(NoSuchAlgorithmException ex) {
		}
		return socket.getInetAddress().toString() + date.toString() + System.nanoTime();
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + Objects.hashCode(this.id);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		final Client other = (Client)obj;
		return Objects.equals(this.id, other.id);
	}
}
