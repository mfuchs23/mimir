package org.dbdoclet.mimir.dialog;

import java.io.IOException;
import java.util.PropertyResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.dbdoclet.mimir.MimirMain;

public class SearchDialog {

	private String pattern;
	private boolean canceled;

	public String getPattern() {
		return pattern;
	}

	public boolean isCanceled() {
		return canceled;
	}
	
	public void showAndWait() throws IOException {

		Stage dialog = new Stage();
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.initOwner(MimirMain.getMainWindow());

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SearchDialog.fxml"),
				PropertyResourceBundle
				.getBundle("org.dbdoclet.mimir.MimirResources"));
	
		Scene scene = new Scene(fxmlLoader.load());
		SearchDialogController controller = fxmlLoader.getController();

		dialog.setScene(scene);
		dialog.showAndWait();
		
		canceled = controller.isCanceled();
		pattern = controller.getPattern();
	}
}
