package talkbox.lib;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

/**
 * This class represents an image. The image is stored as a base64 string.
 */
public class ImageMessage extends Message {
	private static final long serialVersionUID = -2015517444054302221L;
	public final String base64image;

	/**
	 * Create a new ImageMessage from an image file.
	 *
	 * @param sender The sender of the message.
	 * @param file The File containing the image.
	 * @throws java.io.IOException
	 */
	public ImageMessage(Client sender, File file) throws IOException {
		super(sender, "");
		base64image = loadImage(file);
	}

	/**
	 * Create a new ImageMessage with specified text.
	 *
	 * @param sender The sender of the message.
	 * @param text The text of the message.
	 * @param file The File containing the image.
	 * @throws java.io.IOException
	 */
	public ImageMessage(Client sender, String text, File file) throws IOException {
		super(sender, text);
		base64image = loadImage(file);
	}

	/**
	 * Create a new ImageMessage with specified text and recipients.
	 *
	 * @param sender The sender of the message.
	 * @param text The text of the message.
	 * @param file The File containing the image.
	 * @param recipients Who to send the message to.
	 * @throws java.io.IOException
	 */
	public ImageMessage(Client sender, String text, File file, Client... recipients) throws IOException {
		super(sender, text, recipients);
		base64image = loadImage(file);
	}

	/**
	 * get the contents of a File as a base64 string.
	 *
	 * @param file The File to get contents from.
	 * @return A base64 string of the contents of that file.
	 * @throws java.io.IOException
	 */
	public static String loadImage(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream in = new BufferedInputStream(fis);
		byte[] bytes = new byte[(int)file.length()];
		int r = 0;
		while(r < bytes.length) {
			r = in.read(bytes, r, bytes.length - r);
		}
		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString(bytes);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 83 * hash + Objects.hashCode(this.base64image);
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
		final ImageMessage other = (ImageMessage)obj;
		return Objects.equals(this.base64image, other.base64image);
	}
}
