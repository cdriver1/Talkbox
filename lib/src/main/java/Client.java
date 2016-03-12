package talkbox.lib;

import java.io.Serializable;
import java.net.Socket;
import java.util.Date;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class represents a user.
 * It contains a unique id and the screen name of the user.
 * If the program is connected directly, it also contains the Socket representing that connection.
 * If possible, the id is an MD5 hash of the Socket and the Date the Client connected at.
 * Otherwise the id is a concatenation of the Client address and the Date the Client connected at.
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

	/**
	 * Create a new Client from a Socket.
	 * @param  socket The Socket that connects to this Client.
	 */
	public Client(Socket socket) {
		this.socket = socket;
		id = createID(socket, new Date());
	}

	/**
	 * Create a new Client from a Socket and a Date.
	 * @param   socket The Socket that connects to this Client.
	 * @param   date The Date when this Client connected.
	 */
	public Client(Socket socket, Date date) {
		this.socket = socket;
		id = createID(socket, date);
	}
	
	/**
	 * Create a new Client from an id.
	 * @param id The unique id of this Client.
	 */
	public Client(String id) {
		this.socket = null;
		this.id = id;
	}
	 
	/**
	 * Create a new Client from an id and a Socket.
	 * @param id The unique id of this Client.
	 * @param socket The Socket that connects to this Client.
	 */
	public Client(String id, Socket socket) {
		this.id = id;
		this.socket = socket;
	}

	/**
	 * @return The current screenname of this Client.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Change the screenname of this Client.
	 * @param name The new screenname.
	 */
	public void setName(String name) {
		if(this.name.equals(name))
			return;
		this.name = name;
		nameChanged = true;
	}

	/**
	 * @return If the name has changed since nameChangeProcessed was last called, true, otherwise, false.
	 */
	public boolean hasNameChanged() {
		return nameChanged;
	}

	/**
	 * Call this method when a name change has been processed to mark it as processed.
	 */
	public void nameChangeProcessed() {
		nameChanged = false;
	}

	/**
	 * Get the Socket that is the connection to this Client.
	 * @return The Socket that connects to this Client, or null if the connection is not direct.
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * Create a MD5 hash from a Socket and a Date. Hopefully this provides a unique id.
	 * @param   socket The Socket that connects to this Client.
	 * @param   date The Date this Client connected at.
	 * @return  A MD5 hash made from the Socket and Date.
	 */
	public static String createMD5ID(Socket socket, Date date) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(socket.getInetAddress().getAddress());
		byte[] digest = md5.digest(date.toString().getBytes());
		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString(digest);
	}

	/**
	 * Try to create a MD5ID, fall back on String concatenation if not available.
	 * @param   socket The Socket that connectes to this Client.
	 * @param   date The Date this Client connected at.
	 * @return  A hopefully unique id.
	 */
	public static String createID(Socket socket, Date date) {
		try {
			return createMD5ID(socket, date);
		} catch(NoSuchAlgorithmException ex) {
		}
		return socket.getInetAddress().toString() + date.toString();
	}

	@Override
	public int hashCode() {
		return 197 * id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Client)) {
			return false;
		}
		Client c = (Client)o;
		return id.equals(c.id);
	}
}
