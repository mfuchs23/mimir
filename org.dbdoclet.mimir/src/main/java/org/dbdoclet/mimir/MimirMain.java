package org.dbdoclet.mimir;

import java.util.PropertyResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * <tt>Mimir</tt> ist ein Werkzeug zur Analyse von Ear, War und Jar-Archiven. Es
 * dient Entwicklern oder dem Betrieb dazu, Ungereimtheiten beim Laden von
 * Klassen, Methoden, Property-Dateien, Grafiken oder anderen Artefakten auf die
 * Spur zum kommen. Dazu durchsucht <tt>Mimir</tt> das Archiv und legt einen
 * Volltextindex des Inhalts aller gefundenen Dateien an. Binäre Dateien werden
 * so weit wie möglich und bekannt in eine textuelle Darstellung überführt.
 * 
 * 
 * @author Michael Fuchs
 */
public class MimirMain extends Application {

	private static Stage primaryStage;

	/**
	 * Die Startmethode der Application-Instanz bestimmt die Erscheinung der
	 * Oberfäche. Sie lädt die FXML-Datei für das Hauptfenster und öffnet es
	 * anschliessend.
	 */
	@Override
	public void start(Stage primaryStage) {

		// setUserAgentStylesheet(STYLESHEET_CASPIAN);
		setUserAgentStylesheet(STYLESHEET_MODENA);

		MimirMain.primaryStage = primaryStage;

		try {

			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
					"Mimir.fxml"),
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

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Der Einsprungspunkt des Java-Programms.
	 * 
	 * @param args
	 *            - Die Kommandozeilenparameter
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Die Methode {@code getMainWindow()} liefert die Instanz des Hauptfensters
	 * zurück.
	 * 
	 * @return
	 */
	public static Window getMainWindow() {
		return primaryStage;
	}
}
