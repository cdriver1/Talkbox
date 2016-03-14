package talkbox.client;

import talkbox.lib.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;

public class ChatWindowController implements Initializable {
	@FXML
	private TextField nameField;
	@FXML
	private TextArea messageArea;
	@FXML
	private TextField messageField;
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
		String msg = m.sender.getName() + " <" + m.time.toString() + ">: " + m.text;
		String txt = messageArea.getText() + "\n" + msg;
		messageArea.setText(txt);
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		name = "guest-" + ThreadLocalRandom.current().nextInt(1000, 10000);
		nameField.setText(name);
		NetworkMethods.backend.changeName(name, false);
	}
}
