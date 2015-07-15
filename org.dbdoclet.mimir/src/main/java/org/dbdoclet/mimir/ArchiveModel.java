package org.dbdoclet.mimir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;

import org.dbdoclet.mimir.task.FilterTreeTask;
import org.dbdoclet.mimir.tree.ZipTreeValue;

public class ArchiveModel {

	private File archiveFile;
	private TreeMap<String, ArrayList<TreeItem<ZipTreeValue>>> entryMap;
	private IconManager iconManager;
	private ArrayList<Scanner> openScannerList;
	private ArrayList<InputStream> openStreamList;
	private StringProperty path = new SimpleStringProperty();
	private ResourceBundle resources;
	private TreeItem<ZipTreeValue> treeRoot;

	public ArchiveModel(ResourceBundle resources) {
		this.resources = resources;
		iconManager = new IconManager();
		entryMap = new TreeMap<>();
		openStreamList = new ArrayList<>();
		openScannerList = new ArrayList<>();
	}

	public void clear() {
		
		archiveFile = null;
		setPath(null);
		treeRoot = null;
		entryMap.clear();
	}

	public File getArchiveFile() {
		return archiveFile;
	}

	public TreeMap<String, ArrayList<TreeItem<ZipTreeValue>>> getEntryMap() {
		return entryMap;
	}

	public String getPath() {
		return path.get();
	}

	public TreeItem<ZipTreeValue> getTreeRoot() {
		return treeRoot;
	}

	public boolean isZipFile(File file) throws IOException {

		try {
			new ZipFile(file);
		} catch (ZipException oops) {
			return false;
		}

		return true;
	}

	public StringProperty pathProperty() {
		return path;
	}

	public TreeItem<ZipTreeValue> scanArchive(IVisitor<String> visitor)
			throws IOException {

		if (archiveFile == null) {
			throw new IllegalStateException(
					resources.getString("key.archive_undefined"));
		}

		entryMap.clear();
		openStreamList.clear();
		openScannerList.clear();

		try (FileInputStream fis = new FileInputStream(archiveFile);) {
			treeRoot = new TreeItem<ZipTreeValue>(new ZipTreeValue(
					archiveFile.getName()));
			treeRoot.setGraphic(new ImageView(iconManager.getRootIcon()));
			scanZip(visitor, fis, treeRoot);
		}

		openStreamList.forEach(is -> {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		openScannerList.forEach(sc -> {
			try {
				sc.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		openStreamList.clear();
		openScannerList.clear();

		return treeRoot;
	}

	public void scanTree(TreeItem<ZipTreeValue> treeItem,
			FilterTreeTask filterTreeTask) {

		filterTreeTask.before(treeItem);
		treeItem.getChildren().forEach(i -> {
			filterTreeTask.accept(i);
			scanTree(i, filterTreeTask);
		});
		filterTreeTask.after(treeItem);
	}

	public void setArchive(File archiveFile) {
		this.setArchiveFile(archiveFile);
	}

	public void setArchiveFile(File archiveFile) {
		
		this.archiveFile = archiveFile;
		
		if (archiveFile != null) {
			setPath(archiveFile.getAbsolutePath());
		} else {
			clear();
		}
	}

	public void setPath(String value) {
		path.set(value);
	}

	private TreeItem<ZipTreeValue> findDirectoryNode(
			TreeItem<ZipTreeValue> treeParent, String name) {

		String[] pathTokens = name.split("/");

		TreeItem<ZipTreeValue> dirItem = treeParent;

		for (int i = 0; i < pathTokens.length - 1; i++) {

			String token = pathTokens[i];

			TreeItem<ZipTreeValue> foundItem = null;

			for (TreeItem<ZipTreeValue> subDirItem : dirItem.getChildren()) {
				if (token.equals(subDirItem.getValue().getName())) {
					foundItem = subDirItem;
					break;
				}
			}

			if (foundItem == null) {

				ZipTreeValue zipTreeValue = new ZipTreeValue(token);
				zipTreeValue.isDirectory(true);

				foundItem = new TreeItem<ZipTreeValue>(zipTreeValue);
				foundItem
						.setGraphic(new ImageView(iconManager.getFolderIcon()));

				dirItem.getChildren().add(foundItem);
			}

			dirItem = foundItem;
		}

		return dirItem;
	}

	private String identifyCharset(String name) {

		String charset = "UTF-8";
		
		if (name != null && name.toLowerCase().endsWith(".properties")) {
			return "ISO-8859-1";
		}
		
		return charset;
	}
	
	private void scanZip(IVisitor<String> visitor, InputStream is,
			TreeItem<ZipTreeValue> treeParent) throws IOException {

		ZipInputStream zin = new ZipInputStream(is);
		openStreamList.add(zin);
		ZipEntry entry = zin.getNextEntry();

		while (entry != null) {

			/*
			 * Eingebettete Zip-Dateien müssen auch als Verzeichnisse behandelt
			 * werden.
			 */
			treeParent.getValue().isDirectory(true);

			String name = entry.getName();

			visitor.accept(name);

			if (entry.isDirectory() == false) {

				try {

					TreeItem<ZipTreeValue> treeDirectory = findDirectoryNode(
							treeParent, name);

					ZipTreeValue zipTreeValue = new ZipTreeValue(entry);
					zipTreeValue.isDirectory(false);

					TreeItem<ZipTreeValue> treeChild = new TreeItem<ZipTreeValue>(
							zipTreeValue);
					treeDirectory.getChildren().add(treeChild);

					if (name.endsWith(".xml") || name.endsWith(".properties")
							|| name.endsWith(".MF")) {
						
						Scanner sc = new Scanner(zin, identifyCharset(name));
						openScannerList.add(sc);
						StringBuilder buffer = new StringBuilder();
						while (sc.hasNextLine()) {
							buffer.append(sc.nextLine());
							buffer.append("\n");
						}
						
						zipTreeValue.setContent(buffer.toString());
					}

					ArrayList<TreeItem<ZipTreeValue>> entryList = entryMap
							.get(name);

					if (entryList == null) {
						entryList = new ArrayList<>();
						entryMap.put(name, entryList);
					}

					entryList.add(treeChild);

					scanZip(visitor, zin, treeChild);

				} catch (ZipException oops) {
					oops.printStackTrace();
				}

			}

			zin.closeEntry();
			entry = zin.getNextEntry();
		}
	}

}
