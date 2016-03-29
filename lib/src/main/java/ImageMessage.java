package talkbox.lib;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageMessage extends Message {
	public static final long serialVersionUID = 5498235l;
	public final String base64image;

	public ImageMessage(Client sender, File file) throws IOException {
		super(sender, "");
		base64image = loadImage(file);
	}

	public ImageMessage(Client sender, String text, File file) throws IOException {
		super(sender, text);
		base64image = loadImage(file);
	}

	public ImageMessage(Client sender, String text, Client[] recipients, File file) throws IOException {
		super(sender, text, recipients);
		base64image = loadImage(file);
	}

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
}
