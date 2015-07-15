package org.dbdoclet.mimir.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public abstract class MesssageDialog {

	private Alert dialog;

	public MesssageDialog(AlertType type) {

		dialog = new Alert(type);			
	}

	public void setTitle(String title) {
		dialog.setTitle(title);
	}
	
	public void setHeaderText(String text) {
		dialog.setHeaderText(text);
	}
	
	public void setContentText(String text) {
		dialog.setContentText(text);
	}
	
	public void showAndWait() {
		dialog.showAndWait();
	}

}
