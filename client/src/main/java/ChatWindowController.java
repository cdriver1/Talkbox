package talkbox.client;

import talkbox.lib.*;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListView;
import javafx.collections.*;
import java.text.SimpleDateFormat;

public class ChatWindowController implements Initializable {
	@FXML
	private TextField nameField;
	@FXML
	private TextArea messageArea;
	@FXML
	private TextField messageField;
	@FXML
	private ListView<Client> onlineList;
	private String name;

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
		SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
		String msg = m.sender.getName() + " <" + time.format(m.time) + ">: " + m.text;
		String txt = messageArea.getText() + "\n" + msg;
		messageArea.setText(txt);
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		name = "guest-" + ThreadLocalRandom.current().nextInt(1000, 10000);
		nameField.setText(name);
		NetworkMethods.backend.changeName(name, false);
	}

	public void setOnlineNames(Client[] Names){
		ObservableList<Client> names = FXCollections.observableArrayList(Arrays.asList(Names));
		onlineList.setItems(names);
	}
}
