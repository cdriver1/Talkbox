package talkbox.client;

import talkbox.lib.*;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import javafx.application.Platform;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.collections.*;
import java.text.SimpleDateFormat;

public class ChatWindowController implements Initializable {
	@FXML
	private TextField nameField;
	@FXML
	private ListView<Message> messageList;
	@FXML
	private TextField messageField;
	@FXML
	private ListView<Client> onlineList;
	private String name;
	private final StringConverter<Message> messageConverter = new StringConverter<Message>() {
		public Message fromString(String string) {
			return null;
		}

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
		if(m.length() == 0)
			return;
		NetworkMethods.backend.sendMessage(m);
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

		messageList.setCellFactory(new Callback<ListView<Message>, ListCell<Message>>() {
			@Override
			public MessageCell call(ListView<Message> list) {
				MessageCell mc = new MessageCell();
				mc.setConverter(messageConverter);
				mc.setWrapText(true);
				return mc;
			}
		});

		Platform.runLater(() -> {
			messageField.requestFocus();
		});
	}

	public void setOnlineNames(Client[] Names){
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
		FileChooser fc = new FileChooser();
		fc.setTitle("Save file to...");
		fc.setInitialFileName(fm.name);
		java.io.File f = fc.showSaveDialog(((Hyperlink)e.getSource()).getScene().getWindow());
		//TODO: tell backend to request the file and save it.
	}
}
