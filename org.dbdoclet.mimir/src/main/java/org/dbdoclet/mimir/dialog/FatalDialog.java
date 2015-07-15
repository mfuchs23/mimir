package org.dbdoclet.mimir.dialog;

import org.controlsfx.dialog.ExceptionDialog;

public class FatalDialog {

	private ExceptionDialog exceptionDialog;

	public FatalDialog(Throwable oops) {
		exceptionDialog = new ExceptionDialog(oops);			
	}

	public void showAndWait() {
		exceptionDialog.showAndWait();
	}

}
