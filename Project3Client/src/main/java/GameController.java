import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {

    @FXML private Label turnLabel;
    @FXML private Button returnMainButton;
    @FXML private Button returnLobbyButton;

    ComboBox<Integer> listUsers;
    ListView<String> listItems;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ClientData.clientConnection == null) {
            ClientData.clientConnection = new Client(data -> {
                Platform.runLater(() -> {
                    switch (data.type) {
                        case NEWUSER:
                            listUsers.getItems().add(data.recipient);
                            listItems.getItems().add(data.recipient + " has joined!");
                            break;
                        case DISCONNECT:
                            listUsers.getItems().remove(data.recipient);
                            listItems.getItems().add(data.recipient + " has disconnected!");
                            break;
                        case TEXT:
                            listItems.getItems().add(data.recipient+": "+data.message);
                        case GAME_UPDATE:
                            // update board with player's move
                            break;
                        case TURN_UPDATE:
                            // change turnLabel to show whos turn it is
                            break;
                        case GAME_END:
                            Stage stage = (Stage) turnLabel.getScene().getWindow();
                            stage.setScene(ClientData.sceneMap.get("result"));
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

