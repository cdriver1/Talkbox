package talkbox.lib;

import java.io.File;
import java.util.Objects;

/**
 * This class represents a shared file.
 */
public class FileMessage extends Message {
	private static final long serialVersionUID = 3492097681194084659L;
	public final transient File file;
	public final String name;

	/**
	 * Create a new FileMessage for a file.
	 *
	 * @param sender The sender of the message.
	 * @param file The file to share.
	 */
	public FileMessage(Client sender, File file) {
		super(sender, "");
		this.file = file;
		name = file.getName();
	}

	/**
	 * Create a new FileMessage for a file and send it only to the specified
	 * recipients.
	 *
	 * @param sender The sender of the message.
	 * @param file The file to share.
	 * @param recipients Who to send the message to.
	 */
	public FileMessage(Client sender, File file, Client... recipients) {
		super(sender, "", recipients);
		this.file = file;
		name = file.getName();
	}

	/**
	 * Create a new FileMessage from another FileMessage.
	 *
	 * @param fm The FileMessage to copy data from.
	 */
	public FileMessage(Client sender, FileMessage fm) {
		super(sender, fm.text, fm.getRecipients());
		this.file = fm.file;
		this.name = fm.name;
	}

	/**
	 * Create a new FileMessage from another FileMessage with specified
	 * recipients.
	 *
	 * @param fm The FileMessage to copy data from.
	 * @param recipients The recipients of the new FileMessage.
	 */
	public FileMessage(Client sender, FileMessage fm, Client... recipients) {
		super(sender, fm.text, recipients);
		this.file = fm.file;
		this.name = fm.name;
	}

	/**
	 * Convenience function to determine if this FileMessage is actually a
	 * FileRequest.
	 *
	 * @return True if this FileMessage is a FileRequest, false otherwise.
	 */
	public boolean isFileRequest() {
		return false;
	}
	
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 29 * hash + Objects.hashCode(this.name);
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
		final FileMessage other = (FileMessage)obj;
		return Objects.equals(this.name, other.name);
	}

	/**
	 * Class that represents a request for a shared file.
	 */
	public static class FileRequest extends FileMessage {
		private static final long serialVersionUID = -1031604353046017724L;
		public final long start;
		public final long end;

		/**
		 * Create a new request for the file specified by a FileMessage.
		 *
		 * @param sender The sender of the message.
		 * @param fm The FileMessage that represents the shared file to request.
		 */
		public FileRequest(Client sender, FileMessage fm) {
			super(sender, fm);
			start = 0;
			end = 0;
		}

		/**
		 * Create a new request for part of a shared file.
		 *
		 * @param sender The sender of the message.
		 * @param fm The FileMessage that represents the shared file to request.
		 * @param start Skip this many bytes from the start of the file.
		 * @param end The total number of bytes sent will be end - start.
		 */
		public FileRequest(Client sender, FileMessage fm, long start, long end) {
			super(sender, fm);
			if(start <= 0) {
				this.start = 0;
			} else {
				this.start = start;
			}
			if(end <= 0) {
				this.end = 0;
			} else {
				this.end = end;
			}
		}
		
		@Override
		public boolean isFileRequest() {
			return true;
		}
		
		@Override
		public boolean display() {
			return false;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 23 * hash + (int)(this.start ^ (this.start >>> 32));
			hash = 23 * hash + (int)(this.end ^ (this.end >>> 32));
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
			final FileRequest other = (FileRequest)obj;
			if(this.start != other.start) {
				return false;
			}
			return this.end == other.end;
		}
	}
}
