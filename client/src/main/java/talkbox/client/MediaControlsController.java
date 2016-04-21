package talkbox.client;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class MediaControlsController implements Initializable {
	@FXML
	private Label label;
	@FXML
	private Slider seekbar;
	@FXML
	private Button playButton;
	private MediaPlayer player;

	@FXML
	private void onPlay(ActionEvent event) {
		if(player.getStatus() == MediaPlayer.Status.PLAYING) {
			player.pause();
		} else {
			player.play();
		}
		update();
	}

	private void update() {
		Platform.runLater(() -> {
			if(player.getStatus() == MediaPlayer.Status.PLAYING) {
				playButton.setText("Pause");
			} else {
				playButton.setText("Play");
			}
			if(!seekbar.isValueChanging()) {
				double s = player.getCurrentTime().toSeconds();
				double t = player.getTotalDuration().toSeconds();
				double d = s / t;
				d *= 100;
				seekbar.setValue(d);
			}
		});
	}

	@FXML
	private void onStop(ActionEvent event) {
		player.stop();
		seekbar.setValue(0);
		player.seek(player.getStartTime());
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		seekbar.valueProperty().addListener((Observable observable) -> {
			if(seekbar.isValueChanging()) {
				double d = seekbar.getValue();
				d /= 100;
				d *= player.getTotalDuration().toSeconds();
				player.seek(Duration.seconds(d));
			}
		});
	}

	public void init(MediaPlayer player, String text) {
		label.setText(text);
		this.player = player;
		seekbar.setValue(0);
		player.setOnEndOfMedia(() -> {
			player.stop();
			player.seek(player.getStartTime());
		});
		player.currentTimeProperty().addListener((Observable observable) -> {
			update();
		});
	}

	public static Parent MediaControls(MediaPlayer player, String text) {
		FXMLLoader loader = new FXMLLoader(MediaControlsController.class.getResource("MediaControls.fxml"));
		try {
			loader.load();
			((MediaControlsController)loader.getController()).init(player, text);
			return loader.getRoot();
		} catch(IOException ex) {
			Logger.getLogger(MediaControlsController.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
}
