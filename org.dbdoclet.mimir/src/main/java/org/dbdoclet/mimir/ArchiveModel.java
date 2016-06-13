package org.dbdoclet.mimir;

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
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.stream.Collectors;
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
import org.apache.bcel.classfile.Method;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.dbdoclet.mimir.task.FilterTreeTask;
import org.dbdoclet.mimir.tree.ZipTreeValue;

public class ArchiveModel {

	class ScanContext {
		public IVisitor<String> visitor;
		public IndexWriter indexWriter;
		public String container;
	}

	private File archiveFile;
	private IconManager iconManager;
	private ArrayList<Scanner> openScannerList;
	private ArrayList<InputStream> openStreamList;
	private ArrayList<String> classpathList;
	private StringProperty path = new SimpleStringProperty();
	private ResourceBundle resources;
	private TreeItem<ZipTreeValue> treeRoot;
	private Directory directory;
	private Analyzer analyzer = new StandardAnalyzer();
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

		if (directory != null) {
			directory.close();
			directory = new RAMDirectory();
		}
	}

	public Query createQuery(String field, String text) throws ParseException {

		QueryParser queryParser = new QueryParser(field, analyzer);
		return queryParser.parse(text);
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

	public TreeItem<ZipTreeValue> scanArchive(IVisitor<String> visitor) throws IOException {

		if (archiveFile == null) {
			throw new IllegalStateException(resources.getString("key.archive_undefined"));
		}

		openStreamList.clear();
		openScannerList.clear();

		if (directory != null) {
			directory.close();
		}

		directory = new RAMDirectory();
		ScanContext scanContext = new ScanContext();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		scanContext.indexWriter = new IndexWriter(directory, config);
		try (FileInputStream fis = new FileInputStream(archiveFile);) {

			treeRoot = new TreeItem<ZipTreeValue>(new ZipTreeValue(archiveFile.getName()));
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

	public void scanTree(TreeItem<ZipTreeValue> treeItem, FilterTreeTask filterTreeTask) {

		filterTreeTask.before(treeItem);
		treeItem.getChildren().forEach(i -> {
			filterTreeTask.accept(i);
			scanTree(i, filterTreeTask);
		});
		filterTreeTask.after(treeItem);
	}

	public List<Document> search(Query query) throws IOException, ParseException {

		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);

		ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
		List<Document> docs = Arrays.stream(hits).map(scoreDoc -> {
			try {
				return isearcher.doc(scoreDoc.doc);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}).collect(Collectors.toList());

		ireader.close();

		return docs;
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

	private void addClasspath(String classpath) {
		classpathList.add(classpath);
	}

	private TreeItem<ZipTreeValue> findDirectoryNode(TreeItem<ZipTreeValue> treeParent, String name) {

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
				foundItem.setGraphic(new ImageView(iconManager.getFolderIcon()));

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

	private void scanZip(InputStream in, TreeItem<ZipTreeValue> treeParent, ScanContext scanContext)
			throws IOException {

		ZipInputStream zin = new ZipInputStream(in);
		openStreamList.add(zin);
		ZipEntry entry = zin.getNextEntry();

		while (entry != null) {

			/*
			 * Eingebettete Zip-Dateien müssen auch als Verzeichnisse behandelt
			 * werden.
			 */
			treeParent.getValue().isDirectory(true);

			String name = entry.getName();

			scanContext.visitor.accept(name);

			if (entry.isDirectory() == false) {

				try {

					TreeItem<ZipTreeValue> treeDirectory = findDirectoryNode(treeParent, name);

					ZipTreeValue zipTreeValue = new ZipTreeValue(entry);
					zipTreeValue.isDirectory(false);

					TreeItem<ZipTreeValue> treeChild = new TreeItem<ZipTreeValue>(zipTreeValue);
					treeDirectory.getChildren().add(treeChild);

					zipTreeValue.setFullyQualifiedName(createFullyQualifiedName(treeChild));
					
					String suffix = getSuffix(name.toLowerCase());
					
					Document doc = new Document();
					doc.add(new Field("name", zipTreeValue.getFullyQualifiedName(), TextField.TYPE_STORED));

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
							
							StringWriter buffer= new StringWriter();
							IOUtils.copy(zin, buffer, "UTF-8");
							IOUtils.closeQuietly(buffer);
							zipTreeValue.setContent(buffer.toString());
						}
					}

					doc.add(new Field("content", zipTreeValue.getContent(), TextField.TYPE_STORED));

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

	private void scanClassFile(ZipInputStream zin, ZipTreeValue zipTreeValue,
			Document doc) throws IOException,
			FileNotFoundException {
		
		File tempFile = File.createTempFile("Mimir", ".class");
		FileOutputStream fos = new FileOutputStream(tempFile);
		IOUtils.copy(zin, fos);
		IOUtils.closeQuietly(fos);
		
		ClassParser classParser = new ClassParser(tempFile.getCanonicalPath());
		JavaClass javaClass = classParser.parse();

		for (Method method : javaClass.getMethods()) {
			doc.add(new Field("method", method.getName(), TextField.TYPE_STORED));
		}
		
		zipTreeValue.setContent(javaClass.toString());
		tempFile.delete();
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
}
