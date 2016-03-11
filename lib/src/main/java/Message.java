package talkbox.lib;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class represents a chat message.
 * It includes the text of the message, the time the message was sent, and the intended recipients.
 */
public class Message implements Serializable {
	public static final long serialVersionUID = 5467569l;
	public final String text;
	public final Date time;
	public final Client sender;
	private List<Client> recipients;

	/**
	 * Create a new Message with specified text.
	 * @param sender The Client that sent the message.
	 * @param  text The message text.
	 */
	public Message(Client sender, String text) {
		this.sender = sender;
		this.text = text;
		time = new Date();
		recipients = new ArrayList<>();
	}

	/**
	 * Create a new Message with specified text and recipients.
	 * @param sender The Client that sent the message.
	 * @param   text The message text.
	 * @param   recipients An array of Clients that are the intended recipients.
	 */
	public Message(Client sender, String text, Client[] recipients) {
		this.sender = sender;
		this.text = text;
		time = new Date();
		this.recipients = Arrays.asList(recipients);
	}

	/**
	 * Add a recipient to this Message.
	 * @param  recipient The recipient to add.
	 * @return           True if added, false otherwise.
	 */
	public boolean addRecipient(Client recipient) {
		if(recipients.contains(recipient))
			return false;
		return recipients.add(recipient);
	}

	/**
	 * Get an array of Clients that are the intended recipients.
	 * @return The Clients that this Message is intended for.
	 */
	public Client[] getRecipients() {
		return (Client[])recipients.toArray();
	}
}
