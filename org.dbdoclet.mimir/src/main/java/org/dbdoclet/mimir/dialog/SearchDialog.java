package org.dbdoclet.mimir.dialog;

import java.io.IOException;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.dbdoclet.mimir.MimirMain;

public class SearchDialog implements Initializable {

	private Stage dialog;

	@FXML
	private TextField seachPattern;
	@FXML 
	private Button startButton;
	@FXML 
	private Button cancelButton;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {		
		startButton.setOnAction(e -> onAction(e));
	}
	
	private void onAction(ActionEvent e) {
		System.out.println("Starte Suche");
	}

	public void showAndWait() throws IOException {

		dialog = new Stage();
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.initOwner(MimirMain.getMainWindow());

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SearchDialog.fxml"),
				PropertyResourceBundle
				.getBundle("org.dbdoclet.mimir.MimirResources"));
	
		dialog.setScene(new Scene(fxmlLoader.load()));
		dialog.showAndWait();
	}
}
