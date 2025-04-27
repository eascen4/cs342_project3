package app.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import app.ClientData;
import app.Client;
import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;

import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {

    @FXML private GridPane boardGrid;
    @FXML private ListView<String> listChat;
    @FXML private TextField chatField;
    @FXML private Label turnLabel;
    @FXML private Button returnMainButton;

    private ComboBox<Integer> listUsers;
    private boolean myTurn = false;

    private static final int COLS = 7;
    private static final int ROWS = 6;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateTurnLabel();
        if (ClientData.clientConnection == null) {
            ClientData.clientConnection = new Client(data -> {
                Platform.runLater(() -> {
                    switch (data.getType()) {
                        case CHAT_NOTIFICATION:
                            // implement
                            break;
                        case GAME_UPDATE:
                            // implement
                            break;
                        case GAME_END:
                            // show turn label
                        case FORFEIT_NOTIFICATION:
                        case SERVER_SHUTDOWN:
                            // implement
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
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Button cell = new Button();
                cell.setPrefSize(55, 55);
                cell.setStyle("-fx-background-color: white; -fx-border-color: black;");
                int lastColumn = col;
                // fix
                boardGrid.add(cell, col, row);
            }
        }
    }

    private void updateTurnLabel() {
        if (myTurn) {
            turnLabel.setText("Your turn");
            turnLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            turnLabel.setText("Opponent's turn");
            turnLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
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
    private void handleSendChat() {
        String message = chatField.getText();
        if (!message.isEmpty()) {
            // send chat
            chatField.clear();
        }
    }
}

