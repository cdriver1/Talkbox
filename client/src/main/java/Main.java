package talkbox.client;

import java.io.File;
import java.nio.file.Files;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.Dragboard;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
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

		scene.setOnDragOver((DragEvent dg) -> {
			Dragboard db = dg.getDragboard();
			if(db.hasFiles()) {
				dg.acceptTransferModes(TransferMode.ANY);
			} else {
				dg.consume();
			}
		});

		scene.setOnDragDropped((DragEvent dg) -> {
			Dragboard db = dg.getDragboard();
			if(db.hasFiles()) {
				for(File f : db.getFiles()) {
					try {
						switch(Files.probeContentType(f.toPath())) {
							case "image/png":
							case "image/jpeg":
							case "image/gif":
							case "image/bmp":
								NetworkMethods.backend.sendImage(f.getName(), f);
								break;
							default:
								NetworkMethods.backend.sendFile(f);
								break;
						}
					} catch(Exception e) {
					}
				}
			} else {
				dg.setDropCompleted(false);
			}
			dg.consume();
		});

		stage.setScene(scene);
		stage.show();
	}
}
