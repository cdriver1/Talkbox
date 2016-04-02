package talkbox.lib;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class represents a chat message. It includes the text of the message,
 * the time the message was sent, and the intended recipients.
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 4609441689316482567L;
	public final String text;
	public final Date time;
	public final Client sender;
	private final List<Client> recipients;

	/**
	 * Create a new Message with specified text.
	 *
	 * @param sender The Client that sent the message.
	 * @param text The message text.
	 */
	public Message(Client sender, String text) {
		this.sender = sender;
		this.text = text;
		time = new Date();
		recipients = new ArrayList<>();
	}

	/**
	 * Create a new Message with specified text and recipients.
	 *
	 * @param sender The Client that sent the message.
	 * @param text The message text.
	 * @param recipients An array of Clients that are the intended recipients.
	 */
	public Message(Client sender, String text, Client... recipients) {
		this.sender = sender;
		this.text = text;
		time = new Date();
		if(recipients != null) {
			this.recipients = Arrays.asList(recipients);
		} else {
			this.recipients = new ArrayList<>();
		}
	}

	/**
	 * Add a recipient to this Message.
	 *
	 * @param recipient The recipient to add.
	 * @return True if added, false otherwise.
	 */
	public boolean addRecipient(Client recipient) {
		if(recipients.contains(recipient)) {
			return false;
		}
		return recipients.add(recipient);
	}

	/**
	 * Get an array of Clients that are the intended recipients.
	 *
	 * @return The Clients that this Message is intended for.
	 */
	public Client[] getRecipients() {
		if(recipients == null || recipients.size() <= 0) {
			return null;
		}
		return recipients.toArray(new Client[0]);
	}

	public void setRecipients(Client... recipients) {
		this.recipients.clear();
		this.recipients.addAll(Arrays.asList(recipients));
	}

	public void setRecipients(List<Client> recipients) {
		this.recipients.clear();
		this.recipients.addAll(recipients);
	}

	public boolean display() {
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 59 * hash + Objects.hashCode(this.text);
		hash = 59 * hash + Objects.hashCode(this.time);
		hash = 59 * hash + Objects.hashCode(this.sender);
		hash = 59 * hash + Objects.hashCode(this.recipients);
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
		final Message other = (Message)obj;
		if(!Objects.equals(this.text, other.text)) {
			return false;
		}
		if(!Objects.equals(this.time, other.time)) {
			return false;
		}
		if(!Objects.equals(this.sender, other.sender)) {
			return false;
		}
		return Objects.equals(this.recipients, other.recipients);
	}
}
