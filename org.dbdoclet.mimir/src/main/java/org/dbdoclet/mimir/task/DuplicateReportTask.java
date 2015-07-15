package org.dbdoclet.mimir.task;

import java.io.IOException;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;

import org.dbdoclet.mimir.ArchiveModel;
import org.dbdoclet.mimir.ExceptionHandler;
import org.dbdoclet.mimir.HtmlReport;
import org.dbdoclet.mimir.IVisitor;

public class DuplicateReportTask extends Task<Void> implements IVisitor<String>{

	private ArchiveModel archiveModel;
	private HtmlReport report;

	public DuplicateReportTask(ArchiveModel archiveModel, Stage stage, WebEngine engine) {
		this.archiveModel = archiveModel;
		setOnSucceeded(new SucceededHandler(stage, engine));
		setOnFailed(new FailedHandler(stage));
	}
	
	@Override
	protected Void call() throws Exception {
		
		report = new HtmlReport(archiveModel);
		archiveModel.scanArchive(this);
		return null;
	}

	public String getReport() throws IOException {
	
		if (report == null) {
			return "";
		}
		
		return report.createReport();
	}
	

	@Override
	public void accept(String name) {
		updateMessage(name);
	}

	class FailedHandler implements EventHandler<WorkerStateEvent> {

		private Stage stage;

		public FailedHandler(Stage stage) {
			this.stage = stage;
		}

		@Override
		public void handle(WorkerStateEvent event) {
			stage.getScene().setCursor(Cursor.DEFAULT);
			stage.getScene().getRoot().setDisable(false);
		}
	}
	
	class SucceededHandler implements EventHandler<WorkerStateEvent> {

		private Stage stage;
		private WebEngine engine;

		public SucceededHandler(Stage stage, WebEngine engine) {
			this.stage = stage;
			this.engine = engine;
		}

		@Override
		public void handle(WorkerStateEvent event) {
			stage.getScene().setCursor(Cursor.DEFAULT);
			stage.getScene().getRoot().setDisable(false);
			try {
				engine.loadContent(getReport());
			} catch (IOException oops) {
				new ExceptionHandler().showDialog(oops);
			}
		}
	}
}
