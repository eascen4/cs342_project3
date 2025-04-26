package app.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {

    @FXML private GridPane boardGrid;
    @FXML private ListView<String> listChat;
    @FXML private TextField chatField;

    ComboBox<Integer> listUsers;
    ListView<String> listChat;

    private static final int COLS = 7;
    private static final int ROWS = 6;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        private Button getButton(int row, int col) {
            for (javafx.scene.Node node : boardGrid.getChildren()) {
                if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {
                    return (Button) node;
                }
            }
            return null;
        }

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
                            int col = data.col;
                            int row = data.row;
                            String playerSymbol = data.message; // like "X" or "O"
                            Button button = getButton(row, col);
                            button.setText(playerSymbol);
                            button.setDisable(true);
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
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Button cell = new Button();
                cell.setPrefSize(55, 55);
                cell.setStyle("-fx-background-color: white; -fx-border-color: black;");
                int lastColumn = col;
                cell.setOnAction(e -> handleColumnClick(lastColumn));
                boardGrid.add(cell, col, row);
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
    private void handleSendChat() {
        String message = chatField.getText();
        if (!message.isEmpty()) {
            ClientData.clientConnection.send(new Message(ClientData.username, message, Message.MessageType.TEXT));
            chatField.clear();
        }
    }
}

