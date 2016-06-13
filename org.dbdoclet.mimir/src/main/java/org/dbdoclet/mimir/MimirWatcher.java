package org.dbdoclet.mimir;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import javafx.application.Platform;

public class MimirWatcher extends Thread {

	private WatchService watcher = null;
	private MainController controller;
	private File file;
	private WatchKey watchKey;

	public MimirWatcher(MainController controller) throws IOException {

		this.controller = controller;
		watcher = FileSystems.getDefault().newWatchService();
	}

	public void register(File file) throws IOException {

		if (file == null) {
			return;
		}
		
		this.file = file;

		if (watchKey != null) {
			watchKey.cancel();
		}

		watchKey = file.toPath().getParent().register(watcher, ENTRY_MODIFY);
	}

	@Override
	public void run() {

		while (isInterrupted() == false) {

			try {

				WatchKey key = watcher.take();
				for (WatchEvent<?> event : key.pollEvents()) {

					Path changed = (Path) event.context();
					if (changed != null && changed.toFile().getName()
									.equals(file.getName())) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								try {
									sleep(2000);
								} catch (InterruptedException oops) {
									//
								}
								controller.refresh();
							}
						});
					}
				}

			} catch (InterruptedException oops) {
				return;
			}
		}

	}
}
