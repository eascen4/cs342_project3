package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.HashMap;


public class GuiClient extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader mainmenuLoader = new FXMLLoader(getClass().getResource("/MainMenu.fxml"));
		Scene mainMenuScene = new Scene(mainmenuLoader.load());
		ClientData.sceneMap.put("main", mainMenuScene);

		FXMLLoader gameLoader = new FXMLLoader(getClass().getResource("/Game.fxml"));
		Scene gameScene = new Scene(gameLoader.load());
		ClientData.sceneMap.put("game", gameScene);

		FXMLLoader resultLoader = new FXMLLoader(getClass().getResource("/Result.fxml"));
		Scene resultScene = new Scene(resultLoader.load());
		ClientData.sceneMap.put("result", resultScene);

		primaryStage.setTitle("Connect Four Online");
		primaryStage.setScene(mainMenuScene);
		primaryStage.setOnCloseRequest(e -> {
			System.exit(0);
		});
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}

