package talkbox.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import talkbox.lib.*;

public class ChatWindowController implements Initializable {
	@FXML
	private TextField nameField;
	@FXML
	private ListView<Message> messageList;
	@FXML
	private TextField messageField;
	@FXML
	private ListView<Client> onlineList;
	@FXML
	private Button microphoneButton;

	private CaptureAudio capt = new CaptureAudio();
	static final long RECORD_TIME = 30000;  // 30 seconds
	private String name;
	private final StringConverter<Message> messageConverter = new StringConverter<Message>() {
		@Override
		public Message fromString(String string) {
			return null;
		}

		@Override
		public String toString(Message m) {
			String msg = m.sender.getName();
			if(m.text == null) {
				msg += ": connected at <" + timestampFormatter.format(m.time) + ">";
			} else {
				msg += " <" + timestampFormatter.format(m.time) + ">: " + m.text;
			}
			return msg;
		}
	};
	private final SimpleDateFormat timestampFormatter = new SimpleDateFormat("HH:mm:ss");

	@FXML
	private void changeName(ActionEvent event) {
		String name = nameField.getText();
		if(name.length() < 1 || name.equals(this.name)) {
			return;
		}
		NetworkMethods.backend.changeName(name, true);
		this.name = name;
	}

	@FXML
	private void sendMessage(ActionEvent event) {
		String m = messageField.getText();
		messageField.setText("");
		if(m.length() == 0) {
			return;
		}
		NetworkMethods.backend.sendMessage(m);
	}

	@FXML
	private void microphoneAction(ActionEvent event) {
		System.out.println("Mic button pressed.");
		if(microphoneButton.getText().startsWith("Record")) {
			capt.start();
			microphoneButton.setText("Stop");
		} else {
			System.out.println("in microphoneAction before b = capt.getAudioBytes ");
			CompletableFuture<byte[]> audioBytes = capt.getAudioBytes();
			capt.stop();
			byte[] b;
			try {
				b = audioBytes.get();
				System.out.println("in microphoneAction after b = capt.getAudioBytes " + b.length);
				NetworkMethods.backend.sendAudio(b, capt.getFormat());
			} catch(InterruptedException ex) {
				Logger.getLogger(ChatWindowController.class.getName()).log(Level.SEVERE, null, ex);
			} catch(ExecutionException ex) {
				Logger.getLogger(ChatWindowController.class.getName()).log(Level.SEVERE, null, ex);
			}
			microphoneButton.setText("Record");
		}
	}

	public void receiveMessage(Message m) {
		Platform.runLater(() -> {
			messageList.getItems().add(m);
			messageList.scrollTo(messageList.getItems().size() - 1);
			addOnline(m.sender);
		});
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		name = "guest-" + ThreadLocalRandom.current().nextInt(1000, 10000);
		nameField.setText(name);
		NetworkMethods.backend.changeName(name, false);

		messageList.setCellFactory((ListView<Message> list) -> {
			MessageCell mc = new MessageCell();
			mc.setConverter(messageConverter);
			mc.setWrapText(true);
			return mc;
		});

		Platform.runLater(() -> {
			messageField.requestFocus();
		});

		Image microphone = new Image(getClass().getResourceAsStream("Microphone.png"));
		ImageView mp = new ImageView(microphone);
		microphoneButton.setText("Record");
	}

	public void setOnlineNames(Client[] Names) {
		Platform.runLater(() -> {
			ObservableList<Client> names = FXCollections.observableArrayList(Arrays.asList(Names));
			onlineList.setItems(names);
		});
	}

	public void addOnline(Client add) {
		Platform.runLater(() -> {
			int i = onlineList.getItems().indexOf(add);
			if(i >= 0) {
				onlineList.getItems().remove(add);
				onlineList.getItems().add(i, add);
			} else {
				onlineList.getItems().add(add);
			}
		});
	}

	public void removeOnline(Client remove) {
		Platform.runLater(() -> {
			onlineList.getItems().remove(remove);
		});
	}
}

class MessageCell extends TextFieldListCell<Message> {
	public static final Base64.Decoder b64decoder = Base64.getDecoder();

	@Override
	public void updateItem(Message item, boolean empty) {
		super.updateItem(null, true);
		if(!empty && item != null) {
			super.updateItem(item, empty);
			String msg = getText();
			setText(null);
			setGraphic(null);
			if(item instanceof ImageMessage) {
				ImageMessage im = (ImageMessage)item;
				byte[] imagebytes = b64decoder.decode(im.base64image);
				ByteArrayInputStream imagestream = new ByteArrayInputStream(imagebytes);
				Image image = new Image(imagestream);
				ImageView iv = new ImageView(image);
				if(im.text != null && im.text.length() > 0) {
					Tooltip t = new Tooltip(im.text);
					Tooltip.install(iv, t);
				}
				VBox vb = new VBox();
				vb.getChildren().addAll(new Label(msg), iv);
				setGraphic(vb);
			} else if(item instanceof FileMessage) {
				Hyperlink hl = new Hyperlink(((FileMessage)item).name);
				hl.setOnAction(new FileLinkListener((FileMessage)item));
				TextFlow tf = new TextFlow(new Label(msg), hl);
				setGraphic(tf);
			} else if(item instanceof AudioMessage) {
				//dostuff
				File f = new File("" + System.nanoTime() + ".wav");
				if(!f.exists()) {
					f.deleteOnExit();
					try {
						new FileOutputStream(f).write(((AudioMessage)item).getAudioBytes());
					} catch(Exception ex) {
						Logger.getLogger(MessageCell.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
				Media media = new Media(f.toURI().toString());
				MediaPlayer mp = new MediaPlayer(media);
				setGraphic(MediaControlsController.MediaControls(mp, msg));
//				MediaView mv = new MediaView(mp);
//				setGraphic(mv);
				/*Hyperlink hyperlink = new Hyperlink("AudioMessage");
				hyperlink.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						if(mp.getStatus() == MediaPlayer.Status.PLAYING) {
							mp.stop();
						} else {
							mp.play();
						}
					}
				});
				TextFlow tf = new TextFlow(new Label(msg), hyperlink);
				setGraphic(tf);*/
			} else {
				Label l = new Label(msg);
				l.setWrapText(true);
				l.maxWidthProperty().bind(getListView().widthProperty().subtract(20));
				setGraphic(l);
			}
		}
	}
}

class FileLinkListener implements EventHandler<ActionEvent> {
	public final FileMessage fm;

	public FileLinkListener(FileMessage fm) {
		this.fm = fm;
	}

	@Override
	public void handle(ActionEvent e) {
		if(!NetworkMethods.backend.hasClient(fm.sender)) {
			//TODO: add indication that this failed because the file is no longer available.
			return;
		}
		FileChooser fc = new FileChooser();
		fc.setTitle("Save file to...");
		fc.setInitialFileName(fm.name);
		java.io.File f = fc.showSaveDialog(((Hyperlink)e.getSource()).getScene().getWindow());
		NetworkMethods.backend.getFile(fm, f);
	}
}
