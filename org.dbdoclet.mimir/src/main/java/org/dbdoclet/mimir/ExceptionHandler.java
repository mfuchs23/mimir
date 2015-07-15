package org.dbdoclet.mimir;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.dbdoclet.mimir.dialog.FatalDialog;


public class ExceptionHandler implements ChangeListener<Throwable>{

	public void showDialog(Throwable oops) {

		FatalDialog dialog = new FatalDialog(oops);
		dialog.showAndWait();
	}

	@Override
	public void changed(ObservableValue<? extends Throwable> observable,
			Throwable oldValue, Throwable newValue) {
		showDialog(newValue);
	}
}
