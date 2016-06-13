package org.dbdoclet.mimir;

import java.util.PropertyResourceBundle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Mimir ist ein Werkzeug zur Analyse von Ear, War und Jar-Archive. Im
 * Vordergrund steht das Suche von Resourcen, das Aufzeigen von Fehlern und die
 * Anzeige der Meta-Informationen.
 * 
 * @author Michael Fuchs
 */
public class MimirMain extends Application {
	
	private static Stage primaryStage;

	@Override
	public void start(Stage primaryStage) {

		MimirMain.primaryStage = primaryStage;
		
		try {

			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Mimir.fxml"),
					PropertyResourceBundle
							.getBundle("org.dbdoclet.mimir.MimirResources"));
	
			Parent root = fxmlLoader.load();
			MainController controller = fxmlLoader.getController();
			controller.setStage(primaryStage);
			
			Scene scene = new Scene(root, 1024, 768);
			
			scene.getStylesheets().add(
					getClass().getResource("application.css").toExternalForm());
			
			primaryStage.setScene(scene);
			primaryStage.show();
	
			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override    
				public void handle(WindowEvent event) {
					Platform.exit();
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	public static Window getMainWindow() {
		return primaryStage;
	}
}
