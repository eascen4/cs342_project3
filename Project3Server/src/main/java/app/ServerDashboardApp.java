package app;

import app.controllers.ServerDashboardController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerDashboardApp extends Application {

	Server server;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		log.info("Server GUI starting...");
		server = new Server();


		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/server-dashboard.fxml"));
			Parent root = loader.load();

			ServerDashboardController controller = loader.getController();
			controller.setServer(server);
			server.setServerDashboardController(controller);

			server.start();

			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.setTitle("Server Dashboard");

			primaryStage.show();
		} catch (Exception e) {
			Platform.exit();
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

}
