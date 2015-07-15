package org.dbdoclet.mimir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;

import org.dbdoclet.mimir.tree.ZipTreeValue;

import javafx.scene.control.TreeItem;

public class HtmlReport {

	private static final String SUFFIX_CLASS = ".class";

	private ArchiveModel archiveModel;

	public HtmlReport(ArchiveModel archiveModel) {
		this.archiveModel = archiveModel;
	}

	public String createReport() throws IOException {

		String report = "";

		try (InputStream is = getClass().getResourceAsStream(
				"resources/templates/Report.html");
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"))) {

			StringBuilder template = new StringBuilder();
			String duplicateClassesSection = createDuplicateClassesSection();

			reader.lines().forEach(
					line -> {
						line = line.replace("__archive", archiveModel
								.getArchiveFile().getName());
						line = line.replace("__duplicateClasses",
								duplicateClassesSection);
						template.append(line);
					});

			report = template.toString();
		}

		return report;
	}

	private String createDuplicateClassesSection() {

		StringBuilder section = new StringBuilder();
		section.append("<!-- comment -->\n");

		TreeMap<String, ArrayList<TreeItem<ZipTreeValue>>> map = archiveModel
				.getEntryMap();

		map.forEach((k, v) -> {

			if (k.endsWith(SUFFIX_CLASS)) {
				if (v.size() > 1) {
					section.append(String.format("<h3>%s</h3>\n", k));
					section.append("<ol>\n");
					v.forEach(i -> {
						section.append(String.format("<li>%s/%s</li>",
								createFqName(i.getParent()), i.getValue().getName()));
					});
					section.append("</ol>\n");
				}
			}
		});

		return section.toString();
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
