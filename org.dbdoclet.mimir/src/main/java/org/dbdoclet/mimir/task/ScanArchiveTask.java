package org.dbdoclet.mimir.task;

import javafx.concurrent.Task;

import org.dbdoclet.mimir.ArchiveModel;
import org.dbdoclet.mimir.IVisitor;

public class ScanArchiveTask extends Task<Void> implements IVisitor<String>{

	private ArchiveModel archiveModel;
	private long lastUpdate;
	
	public ScanArchiveTask(ArchiveModel archiveModel) {
		this.archiveModel = archiveModel;
		lastUpdate = System.currentTimeMillis();
	}
	
	@Override
	protected Void call() throws Exception {
		
		archiveModel.scanArchive(this);
		return null;
	}

	@Override
	public void accept(String name) {

		long msecs = System.currentTimeMillis();
	
		if (msecs - lastUpdate < 200) {
			return;
		}
		
		updateMessage(name);
		lastUpdate = System.currentTimeMillis();
	}

}
