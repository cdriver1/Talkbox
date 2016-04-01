package talkbox.lib;

import java.io.File;
import java.io.IOException;

public class FileMessage extends Message {
	public static final long serialVersionUID = 8743985432l;
	public final transient File file;
	public final String name;

	public FileMessage(Client sender, File file) {
		super(sender, "");
		this.file = file;
		name = file.getName();
	}

	public FileMessage(Client sender, File file, Client[] recipients) {
		super(sender, "", recipients);
		this.file = file;
		name = file.getName();
	}

	public FileMessage(FileMessage fm) {
		super(fm.sender, fm.text, fm.getRecipients());
		this.file = fm.file;
		this.name = fm.name;
	}
}
