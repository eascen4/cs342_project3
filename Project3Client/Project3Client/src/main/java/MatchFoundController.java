import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;


public class MatchFoundController implements Initializable {
    @FXML private Button readyButton;
    @FXML private Button returnLobbyButton;
    @FXML private Button returnMainButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ClientData.clientConnection == null) {
            ClientData.clientConnection = new Client(data -> {
                Platform.runLater(() -> {
                    switch (data.type) {
                        case GAME_START:
                            Stage stage = (Stage) readyButton.getScene().getWindow();
                            stage.setScene(ClientData.sceneMap.get("game"));
                            break;
                    }
                });
            });
            try {
                ClientData.clientConnection.connect("127.0.0.1", 5555);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleReady() {
        Message readyMessage = new Message(MessageType.GAME_READY);
        ClientData.clientConnection.send(readyMessage);
    }

    @FXML
    private void handleReturnMain() {
        if (ClientData.sceneMap.containsKey("main")) {
            Stage stage = (Stage) returnMainButton.getScene().getWindow();
            stage.setScene(ClientData.sceneMap.get("main"));
        }
    }

    @FXML
    private void handleReturnLobby() {
        if (ClientData.sceneMap.containsKey("lobby")) {
            Stage stage = (Stage) returnLobbyButton.getScene().getWindow();
            stage.setScene(ClientData.sceneMap.get("lobby"));
        }
    }


}