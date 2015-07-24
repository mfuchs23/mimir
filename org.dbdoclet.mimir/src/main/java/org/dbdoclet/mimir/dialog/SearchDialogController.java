package org.dbdoclet.mimir.dialog;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SearchDialogController implements Initializable {

	@FXML
	private TextField searchPattern;
	@FXML
	private Button startButton;
	@FXML
	private Button cancelButton;

	private String pattern;
	private boolean canceled = false;

	public String getPattern() {
		return pattern;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		startButton.setOnAction(e -> onSearch(e));
		searchPattern.setOnAction(e -> onSearch(e));
		cancelButton.setOnAction(e -> onCancel(e));
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	private void closeDialog(ActionEvent e) {
		Node button = (Node) e.getSource();
		Scene scene = button.getScene();
		Stage dlg = (Stage) scene.getWindow();
		dlg.close();
	}

	private void onCancel(ActionEvent e) {
		setCanceled(true);
		closeDialog(e);
	}

	private void onSearch(ActionEvent e) {
		setPattern(searchPattern.getText());
		closeDialog(e);
	}

	private void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}
}
