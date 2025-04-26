import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;


public class LobbyController implements Initializable {
    @FXML private Button joinButton;
    @FXML private Button returnButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ClientData.clientConnection == null) {
            ClientData.clientConnection = new Client(data -> {
                Platform.runLater(() -> {
                    switch (data.type) {
                        case MATCH_FOUND:
                            Stage stage = (Stage) joinButton.getScene().getWindow();
                            stage.setScene(ClientData.sceneMap.get("match"));
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
    private void handleJoin() {
        Message matchRequest = new Message(MessageType.MATCH_REQUEST);
        ClientData.clientConnection.send(matchRequest);
    }

    @FXML
    private void handleReturn() {
        if (ClientData.sceneMap.containsKey("main")) {
            Stage stage = (Stage) returnButton.getScene().getWindow();
            stage.setScene(ClientData.sceneMap.get("main"));
        }
    }
}