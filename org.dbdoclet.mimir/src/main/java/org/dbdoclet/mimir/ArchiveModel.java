package org.dbdoclet.mimir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.dbdoclet.mimir.search.SearchEngine;
import org.dbdoclet.mimir.task.FilterTreeTask;
import org.dbdoclet.mimir.tree.ZipTreeValue;

import com.sun.tools.javap.JavapTask;
import com.sun.tools.javap.JavapTask.BadArgs;

public class ArchiveModel {

	private static final Logger log = LogManager.getLogger(ArchiveModel.class);
	
	class ScanContext {
		public IVisitor<String> visitor;
		public IndexWriter indexWriter;
		public String container;
	}

	private File archiveFile;
	private IconManager iconManager;
	private SearchEngine searchEngine;
	private ArrayList<Scanner> openScannerList;
	private ArrayList<InputStream> openStreamList;
	private ArrayList<String> classpathList;
	private StringProperty path = new SimpleStringProperty();
	private ResourceBundle resources;
	private TreeItem<ZipTreeValue> treeRoot;
	private HashSet<String> binarySuffixMap = new HashSet<>();
	private HashSet<String> archiveSuffixMap = new HashSet<>();
	private HashSet<String> textSuffixMap = new HashSet<>();

	public ArchiveModel(ResourceBundle resources) {

		this.resources = resources;
		iconManager = new IconManager();
		openStreamList = new ArrayList<>();
		openScannerList = new ArrayList<>();
		classpathList = new ArrayList<>();

		textSuffixMap.add(".css");
		textSuffixMap.add(".html");
		textSuffixMap.add(".js");
		textSuffixMap.add(".mf");
		textSuffixMap.add(".properties");
		textSuffixMap.add(".xsd");
		textSuffixMap.add(".xml");

		archiveSuffixMap.add(".ear");
		archiveSuffixMap.add(".jar");
		archiveSuffixMap.add(".war");
		archiveSuffixMap.add(".zip");

		binarySuffixMap.add(".class");
		binarySuffixMap.add(".png");
		binarySuffixMap.add(".jpg");
		binarySuffixMap.add(".gif");
		binarySuffixMap.add(".ttf");
	}

	public void clear() throws IOException {

		archiveFile = null;

		setPath(null);
		treeRoot = null;

		searchEngine.newDirectory();
	}

	public File getArchiveFile() {
		return archiveFile;
	}

	public ArrayList<String> getClasspathList() {
		return classpathList;
	}

	public String getPath() {
		return path.get();
	}

	public SearchEngine getSearchEngine() {
		return searchEngine;
	}

	public TreeItem<ZipTreeValue> getTreeRoot() {
		return treeRoot;
	}

	public boolean isZipFile(File file) throws IOException {

		try {
			ZipFile zipFile = new ZipFile(file);
			zipFile.close();
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

		openStreamList.clear();
		openScannerList.clear();

		searchEngine.newDirectory();
		ScanContext scanContext = new ScanContext();
		IndexWriterConfig config = new IndexWriterConfig(
				searchEngine.getAnalyzer());
		scanContext.indexWriter = new IndexWriter(searchEngine.getDirectory(),
				config);

		try (FileInputStream fis = new FileInputStream(archiveFile);) {

			treeRoot = new TreeItem<ZipTreeValue>(new ZipTreeValue(
					archiveFile.getName()));
			treeRoot.setGraphic(new ImageView(iconManager.getRootIcon()));

			scanContext.visitor = visitor;
			scanContext.container = archiveFile.getName();
			scanZip(fis, treeRoot, scanContext);
		}

		scanContext.indexWriter.close();

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

	public void setArchive(File archiveFile) throws IOException {
		this.setArchiveFile(archiveFile);
	}

	public void setArchiveFile(File archiveFile) throws IOException {

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

	public void setSearchEngine(SearchEngine searchEngine) {
		this.searchEngine = searchEngine;
	}

	private void addClasspath(String classpath) {
		classpathList.add(classpath);
	}

	private String createFullyQualifiedName(TreeItem<ZipTreeValue> treeNode) {

		StringBuilder buffer = new StringBuilder();

		buffer.append(treeNode.getValue().getName());
		TreeItem<ZipTreeValue> parent = treeNode.getParent();

		while (parent != null) {
			buffer.insert(0, '/');
			buffer.insert(0, parent.getValue().getName());
			parent = parent.getParent();
		}

		buffer.insert(0, '/');

		return buffer.toString();
	}

	private TreeItem<ZipTreeValue> findDirectoryNode(
			TreeItem<ZipTreeValue> treeParent, String name) {

		String[] pathTokens = name.split("/");
		
		final ArrayList<TreeItem<ZipTreeValue>> dirItemList = new ArrayList<>();
		dirItemList.add(treeParent);
		
		Arrays.stream(pathTokens)
			.limit(pathTokens.length - 1)
			.forEach(token -> {

			TreeItem<ZipTreeValue> dirItem = dirItemList.get(dirItemList.size() - 1);
			
			Optional<TreeItem<ZipTreeValue>> optionalItem = dirItem.getChildren()
					.stream()
					.filter(child -> token.equals(child.getValue().getName()))
					.findFirst();
			
			if (optionalItem.isPresent() == false) {

				ZipTreeValue zipTreeValue = new ZipTreeValue(token);
				zipTreeValue.isDirectory(true);

				TreeItem<ZipTreeValue> tokenItem = new TreeItem<>(zipTreeValue);
				tokenItem.setGraphic(new ImageView(iconManager.getFolderIcon()));

				dirItem.getChildren().add(tokenItem);
				dirItemList.add(tokenItem);
			
			} else {
				dirItemList.add(optionalItem.get());				
			}
		});

		return dirItemList.get(dirItemList.size() - 1);
	}

	private String getSuffix(String name) {

		if (name == null) {
			return "";
		}

		int p = name.lastIndexOf(".");

		if (p > 0) {
			return name.substring(p);
		}

		return "";
	}

	private String identifyCharset(String name) {

		String charset = "UTF-8";

		if (name != null && name.toLowerCase().endsWith(".properties")) {
			return "ISO-8859-1";
		}

		return charset;
	}

	private void scanClassFile(ZipInputStream zin, ZipTreeValue zipTreeValue,
			Document doc) throws IOException, FileNotFoundException {

		File tempFile = File.createTempFile("Mimir", ".class");
		FileOutputStream fos = new FileOutputStream(tempFile);
		IOUtils.copy(zin, fos);
		IOUtils.closeQuietly(fos);

		ClassParser classParser = new ClassParser(tempFile.getCanonicalPath());
		JavaClass javaClass = classParser.parse();

		Arrays.stream(javaClass.getMethods()).forEach(
				method -> doc.add(new Field("method", method.getName(),
						TextField.TYPE_STORED)));

		/*
		String byteCodeString = "";
		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(65536)) {
			
			JavapTask javap = new JavapTask();
			javap.setLog(baos);
			javap.handleOptions(new String[] {"-c", tempFile.getAbsolutePath()});
			javap.call();
			byteCodeString = baos.toString();
			
		} catch (BadArgs e) {
			log.error("javap call failed", e);
		}
		*/
		
		zipTreeValue.setContent(javaClass.toString());
		tempFile.delete();
	}

	private void scanZip(InputStream in, TreeItem<ZipTreeValue> treeParent,
			ScanContext scanContext) throws IOException {

		ZipInputStream zin = new ZipInputStream(in);
		openStreamList.add(zin);
		ZipEntry entry = zin.getNextEntry();

		while (entry != null) {

			/*
			 * Eingebettete Zip-Dateien müssen auch als Verzeichnisse behandelt
			 * werden.
			 */

			if (scanContext.visitor.isCancelled()) {
				return;
			}

			treeParent.getValue().isDirectory(true);

			String name = entry.getName();

			scanContext.visitor.accept(name);

			if (entry.isDirectory() == false) {

				try {

					TreeItem<ZipTreeValue> treeDirectory = findDirectoryNode(
							treeParent, name);

					ZipTreeValue zipTreeValue = new ZipTreeValue(entry);
					zipTreeValue.isDirectory(false);

					TreeItem<ZipTreeValue> treeChild = new TreeItem<ZipTreeValue>(
							zipTreeValue);
					treeDirectory.getChildren().add(treeChild);

					zipTreeValue
							.setFullyQualifiedName(createFullyQualifiedName(treeChild));

					String suffix = getSuffix(name.toLowerCase());

					Document doc = new Document();
					doc.add(new Field("name", zipTreeValue
							.getFullyQualifiedName(), TextField.TYPE_STORED));

					if (suffix.equals(".class")) {

						scanClassFile(zin, zipTreeValue, doc);

					} else if (textSuffixMap.contains(suffix)) {

						Scanner sc = new Scanner(zin, identifyCharset(name));
						openScannerList.add(sc);
						StringBuilder buffer = new StringBuilder();
						while (sc.hasNextLine()) {
							buffer.append(sc.nextLine());
							buffer.append("\n");
						}

						zipTreeValue.setContent(buffer.toString());

					} else {

						if (archiveSuffixMap.contains(suffix) == false
								&& binarySuffixMap.contains(suffix) == false) {

							StringWriter buffer = new StringWriter();
							IOUtils.copy(zin, buffer, "UTF-8");
							IOUtils.closeQuietly(buffer);
							zipTreeValue.setContent(buffer.toString());
						}
					}

					FieldType ftype = TextField.TYPE_STORED;
					// ftype.setStoreTermVectors(true);

					doc.add(new Field("content", zipTreeValue.getContent(),
							ftype));

					scanContext.indexWriter.addDocument(doc);
					scanZip(zin, treeChild, scanContext);

				} catch (ZipException oops) {
					oops.printStackTrace();
				}

			} else {

				if (name.endsWith(".war") || name.endsWith(".jar")) {
					scanContext.container = name;
					addClasspath(name);
				}
			}

			zin.closeEntry();
			entry = zin.getNextEntry();
		}
	}
}
