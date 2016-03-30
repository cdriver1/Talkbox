package talkbox.client;

import talkbox.lib.*;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import javafx.application.Platform;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.text.*;
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
	private StringConverter<Message> messageConverter;
	private SimpleDateFormat timestampFormatter;

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

		timestampFormatter = new SimpleDateFormat("HH:mm:ss");

		messageConverter = new StringConverter<Message>() {
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

		messageList.setCellFactory(new Callback<ListView<Message>, ListCell<Message>>() {
			@Override
			public MessageCell call(ListView<Message> list) {
				MessageCell mc = new MessageCell();
				mc.setConverter(messageConverter);
				mc.setWrapText(true);
				return mc;
			}
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
	@Override
	public void updateItem(Message item, boolean empty) {
		if(!empty && item != null) {
			if(item instanceof ImageMessage) {
				//TODO: Create image.
			} else if(item instanceof FileMessage) {
				//TODO: Create Hyperlink.
			} else {
				super.updateItem(item, empty);
				Label l = new Label(getText());
				l.setWrapText(true);
				l.maxWidthProperty().bind(getListView().widthProperty().subtract(20));
				setGraphic(l);
				setText(null);
			}
		} else {
			super.updateItem(null, true);
		}
	}
}
