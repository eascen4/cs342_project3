package app.controllers;

import app.Server;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import lombok.Setter;

public class ServerDashboardController {

    @Setter
    private Server server;

    @FXML
    public ListView playerListView;
    @FXML
    public ListView activeGamesListView;
    @FXML
    public TextArea logTextArea;

}
