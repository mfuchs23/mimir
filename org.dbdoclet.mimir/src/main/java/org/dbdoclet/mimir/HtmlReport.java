package org.dbdoclet.mimir;

import java.io.IOException;

import javafx.scene.control.TreeItem;

import org.dbdoclet.mimir.tree.ZipTreeValue;

public class HtmlReport {

	private static final String SUFFIX_CLASS = ".class";

	private ArchiveModel archiveModel;

	public HtmlReport(ArchiveModel archiveModel) {
		this.archiveModel = archiveModel;
	}

	public String createReport() throws IOException {

		String report = "";
		return report;
	}

	private String createFqName(TreeItem<ZipTreeValue> item) {

		StringBuilder buffer = new StringBuilder();

		buffer.append(item.getValue());
		item = item.getParent();

		while (item != null) {
			buffer.insert(0, String.format("%s/", item.getValue().getName()));
			item = item.getParent();
		}

		return buffer.toString();
	}
}
