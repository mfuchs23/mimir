package org.dbdoclet.mimir.task;

import org.dbdoclet.mimir.ArchiveModel;
import org.dbdoclet.mimir.IVisitor;

import javafx.concurrent.Task;

public class ScanArchiveTask extends Task<Void> implements IVisitor<String>{

	private ArchiveModel archiveModel;

	public ScanArchiveTask(ArchiveModel archiveModel) {
		this.archiveModel = archiveModel;
		
	}
	
	@Override
	protected Void call() throws Exception {
		
		archiveModel.scanArchive(this);
		return null;
	}

	@Override
	public void accept(String name) {
		updateMessage(name);
	}

}
