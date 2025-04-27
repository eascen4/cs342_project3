package app;

import app.controllers.GameController;
import app.controllers.LobbyController;
import app.controllers.LoginController;
import app.controllers.ResultController;
import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import app.dto.messages.server.GameEndNotification;
import app.dto.messages.server.GameStartNotification;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GuiClient extends Application {

	@Getter private GameController gameController;
	@Getter private ResultController resultController;
	@Getter private LoginController loginController;
	@Getter private LobbyController lobbyController;

	@Getter private static GuiClient instance;
	@Getter private Client client;

	private Stage primaryStage;
	private SceneType currSceneType = SceneType.LOGIN;
	private enum SceneType { LOGIN, LOBBY, GAME, RESULT };

    @Override
	public void start(Stage primaryStage) throws Exception {
		instance = this;
		this.primaryStage = primaryStage;

		this.client = new Client();
		client.setGuiClient(this);

		primaryStage.setTitle("Connect Four Online");
		primaryStage.setOnCloseRequest(e -> {
			client.disconnect();
			System.exit(0);
		});

		showLoginScreen("Enter your username and password");
		primaryStage.show();
	}

	public void showLoginScreen(String message) {
		log.info("Going to show login screen");
		currSceneType = SceneType.LOGIN;
		loadScene("/MainMenu.fxml", message);
	}

	public void showLobby() {
		currSceneType = SceneType.LOBBY;
		loadScene("/Lobby.fxml", null);
	}

	public void showGameScene(GameStartNotification gameStartData) {
		currSceneType = SceneType.GAME;
		loadScene("/Game.fxml", gameStartData);
	}

	public void showResultScene(GameEndNotification gameOverData) {

		currSceneType = SceneType.RESULT;
		loadScene("/Result.fxml",  Map.of("gameOverData", gameOverData, "myPlayerId", client.getLastGamePlayerId()));
	}

	public void showResultScene(GameEndNotification gameOverData, int playerId) {
		currSceneType = SceneType.RESULT;
		loadScene("/Result.fxml",  Map.of("gameOverData", gameOverData, "myPlayerId", playerId));
	}

	private void loadScene(String path, Object userData) {
		try {
            log.info("Loading scene {}", path);
			FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
			Parent root = loader.load();

			if (currSceneType != SceneType.LOGIN) loginController = null;
			if (currSceneType != SceneType.LOBBY) lobbyController = null;
			if (currSceneType != SceneType.GAME) gameController = null;
			if (currSceneType != SceneType.RESULT) resultController = null;

			Object controller = loader.getController();
			if (controller instanceof LoginController) {
				log.info("Login screen loaded");
				loginController = (LoginController) controller;
				loginController.setGuiClient(this);
				loginController.setClient(client);
				if (userData instanceof String) loginController.setMessage((String) userData, false);
				loginController.resetUI();
			}
			else if (controller instanceof LobbyController) {
				lobbyController = (LobbyController) controller;
				lobbyController.setGuiClient(this);
				lobbyController.setClient(client);
			}
			else if (controller instanceof GameController) {
				gameController = (GameController) controller;
				gameController.setGuiClient(this);
				gameController.setClient(client);
				if (userData instanceof GameStartNotification) {
					gameController.initializeGame((GameStartNotification) userData);
				}
			}
			else if (controller instanceof ResultController) {
				resultController = (ResultController) controller;
				resultController.setGuiClient(this);
				resultController.setClient(client);
				log.info("ResultController configured.");
				if (userData instanceof Map) {
					Map<String, Object> dataMap = (Map<String, Object>) userData;
					GameEndNotification gameOverData = (GameEndNotification) dataMap.get("gameOverData");
					Integer myPlayerId = (Integer) dataMap.get("myPlayerId");
					if (gameOverData != null && myPlayerId != null) {
						log.debug("Calling initializeData with GameEndNotification and Player ID {}.", myPlayerId);
						resultController.initializeData(gameOverData, myPlayerId);
					} else {
						log.error("Result scene loaded but missing game over data or player ID in userData map.");
						showLobby();
						return;
					}
				}
			}

			Scene scene = primaryStage.getScene();
			if (scene == null) {
				scene = new Scene(root);
				primaryStage.setScene(scene);
			} else {
				scene.setRoot(root);
			}

			primaryStage.sizeToScene();

		} catch (IOException | IllegalStateException | NullPointerException e) {
			log.error("!!! Failed to load or initialize scene: {} !!!", path, e); // Log the full exception
			showError("Critical Error: Could not load the requested screen (" + path.substring(path.lastIndexOf('/')+1) + "). Please restart the application.\n\nDetails: " + e.getMessage());

		} catch (Exception e) {
			// Catch any other unexpected errors during loading
			log.error("!!! Unexpected error loading scene: {} !!!", path, e);
			showError("Unexpected Application Error: Could not load screen (" + path.substring(path.lastIndexOf('/')+1) + ").\n\nDetails: " + e.getMessage());
		}
	}

	// -------------------------------------------------------------------
	public void showError(String message) {
		if (Platform.isFxApplicationThread()) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Error");
			alert.setContentText(message);
			alert.showAndWait();
		} else {
			Platform.runLater(() -> showError(message));
		}
	}

	public boolean isGameSceneActive() { return currSceneType == SceneType.GAME; }
	public boolean isLobbySceneActive() { return currSceneType == SceneType.LOBBY; }
	public boolean isResultSceneActive() { return currSceneType == SceneType.RESULT; }

	@Override
	public void stop() throws Exception {
		log.info("ClientApp stopping...");
		if (client != null) {
			client.disconnect(); // Ensure disconnect is called
		}
		log.info("ClientApp stopped.");
	}

	public static void main(String[] args) {
		launch(args);
	}
}

