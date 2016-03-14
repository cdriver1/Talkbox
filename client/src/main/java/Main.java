package talkbox.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {
	@Override
	public void start(Stage stage) throws Exception {
		NetworkMethods.backend = new Backend();
		FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatWindow.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root);
		NetworkMethods.backend.setController(loader.getController());
		NetworkMethods.backend.start();
		stage.setOnCloseRequest((WindowEvent event) -> {
			NetworkMethods.backend.stop();
		});
		stage.setScene(scene);
		stage.show();
	}
}
