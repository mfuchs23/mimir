package org.dbdoclet.mimir;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.controlsfx.dialog.ProgressDialog;
import org.dbdoclet.mimir.dialog.ErrorDialog;
import org.dbdoclet.mimir.search.SearchEngine;
import org.dbdoclet.mimir.search.SearchHit;
import org.dbdoclet.mimir.search.SearchHitCellFactory;
import org.dbdoclet.mimir.task.DuplicateReportTask;
import org.dbdoclet.mimir.task.FilterTreeTask;
import org.dbdoclet.mimir.task.RegexFilterTreeTask;
import org.dbdoclet.mimir.task.ScanArchiveTask;
import org.dbdoclet.mimir.tree.ZipTreeCell;
import org.dbdoclet.mimir.tree.ZipTreeValue;

/**
 * Der Controller für die JavaFX-Applikation Mimir verbindet die
 * FXML-Beschreibung der GUI mit dem Datenmodell und reagiert auf
 * Benutzereingaben.
 * 
 * @author fuchs
 */
public class MainController implements Initializable {

	@FXML
	private MenuBar menuBar;
	@FXML
	private ToolBar toolBar;
	@FXML
	private TabPane tabPane;
	@FXML
	private TreeView<ZipTreeValue> treeView;
	@FXML
	private TextField searchPattern;
	@FXML
	private TextField filterPattern;
	@FXML
	private WebView reportView;
	@FXML
	private Label archivePath;
	@FXML
	private TableView<SearchHit> searchHits;
	@FXML
	private Tab searchTab;
	@FXML
	private TableColumn<SearchHit, String> searchHitCol;

	private Stage stage;
	private ArchiveModel archiveModel;
	private SearchEngine searchEngine;
	private ExceptionHandler exceptionHandler;
	private FileChooser fileChooser = new FileChooser();
	private RecentlyUsed recentlyUsed = new RecentlyUsed();
	private ResourceBundle resources;
	private MimirWatcher watcher;
	private StringProperty fileNameProperty = new SimpleStringProperty();

	// private URL location;

	/**
	 * Die Methode erstellt das Datenmodell des Typs {@linkplain ArchivModel}
	 * und die Bindings zwischen den GUI-Elementen und den Modell-Properties.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		exceptionHandler = new ExceptionHandler();

		// this.location = location;
		this.resources = resources;

		try {

			recentlyUsed.load(menuBar);
			updateRecentlyUsedMenu();
			watcher = new MimirWatcher(this);
			watcher.start();

		} catch (Throwable oops) {
			exceptionHandler.showDialog(oops);
		}

		archiveModel = new ArchiveModel(resources);
		searchEngine = new SearchEngine();
		archiveModel.setSearchEngine(searchEngine);

		filterPattern.setOnAction(e -> onFilterChanged());
		treeView.setCellFactory((TreeView<ZipTreeValue> p) -> new ZipTreeCell(
				resources));
		treeView.setOnMouseClicked(e -> onTreeDoubleClick(e));

		if (recentlyUsed.getMostRecentlyUsed() != null) {
			try {
				openArchive(recentlyUsed.getMostRecentlyUsed().toFile());
			} catch (IOException oops) {
				exceptionHandler.showDialog(oops);
			}
		}

		initSearchTableView();
	}

	@FXML
	public void onClearFilter(ActionEvent event) {
		filterPattern.setText("");
		onFilterChanged();
	}

	@FXML
	public void onCloseArchive(ActionEvent event) {

		try {
			treeView.setRoot(null);
			archiveModel.setArchive(null);
			closeArchive();
		} catch (IOException e) {
			exceptionHandler.showDialog(e);
		}
	}

	@FXML
	public void onDuplicateClasses(ActionEvent event) {

		if (archiveModel.getTreeRoot() == null) {
			return;
		}

		DuplicateReportTask duplicateReportTask = new DuplicateReportTask(
				archiveModel, stage, reportView.getEngine());
		duplicateReportTask.exceptionProperty().addListener(exceptionHandler);

		stage.getScene().setCursor(Cursor.WAIT);
		stage.getScene().getRoot().setDisable(true);
		Executors.newSingleThreadExecutor().submit(duplicateReportTask);
	}

	/**
	 * Benutzerereignis zum öffnen eines Archivs. Die Methode öffnet den Dialog
	 * zu Dateiauswahl
	 * 
	 * @param event
	 */
	@FXML
	public void onOpenArchive(final ActionEvent event) {

		try {

			File file = null;
			if (event.getSource() instanceof MenuItem) {
				MenuItem item = (MenuItem) event.getSource();
				Object userData = item.getUserData();
				if (userData instanceof Path) {
					file = ((Path) userData).toFile();
				}
			}

			if (recentlyUsed.getMostRecentlyUsed() != null) {
				fileChooser.setInitialDirectory(recentlyUsed
						.getMostRecentlyUsed().toFile().getParentFile());
			}

			if (file == null) {
				file = fileChooser.showOpenDialog(stage);
			}

			if (file != null) {

				if (archiveModel.isZipFile(file) == false) {

					ErrorDialog dlg = new ErrorDialog();
					dlg.setTitle(resources.getString("key.error"));
					dlg.setHeaderText(resources
							.getString("key.open_archive_failed"));
					dlg.setContentText(MessageFormat.format(
							resources.getString("key.invalid_zip"),
							file.getName()));
					dlg.showAndWait();
					return;
				}

				fileNameProperty.setValue(file.getName());
				openArchive(file);
			}

		} catch (Throwable oops) {
			exceptionHandler.showDialog(oops);
		}
	}

	@FXML
	public void onQuit(ActionEvent event) {

		try {

			recentlyUsed.save();

		} catch (IOException oops) {
			exceptionHandler.showDialog(oops);
		}

		watcher.interrupt();
		stage.close();
		Platform.exit();
	}

	@FXML
	public void onReloadArchive(final ActionEvent event) {
		reload();
	}

	@FXML
	public void onSearch(ActionEvent event) {

		try {

			if (archiveModel == null) {
				return;
			}

			TreeItem<ZipTreeValue> treeRoot = archiveModel.getTreeRoot();

			if (treeRoot == null) {
				return;
			}

			ObservableList<SearchHit> hits = searchEngine.search(searchPattern
					.getText());
			searchHits.setItems(hits);

		} catch (Exception e) {
			new ExceptionHandler().showDialog(e);
		}
	}

	@FXML
	public void onSelectSearchTab(ActionEvent event) {

		try {
			selectSearchTab(tabPane);
		} catch (Exception e) {
			new ExceptionHandler().showDialog(e);
		}
	}

	public void refresh() {

		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(resources.getString("key.refresh_title"));
		alert.setHeaderText(resources.getString("key.refresh_header"));
		alert.setContentText(MessageFormat.format(resources
				.getString("key.refresh_content"), archiveModel
				.getArchiveFile().getName()));

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			try {
				openArchive(archiveModel.getArchiveFile());
			} catch (IOException e) {
				exceptionHandler.showDialog(e);
			}
		}
	}

	public void reload() {

		try {

			openArchive(archiveModel.getArchiveFile());
			watcher.register(archiveModel.getArchiveFile());

		} catch (IOException e) {
			exceptionHandler.showDialog(e);
		}
	}

	public void setStage(Stage stage) {

		this.stage = stage;

		stage.titleProperty().bind(fileNameProperty);

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				try {
					recentlyUsed.save();
				} catch (IOException oops) {
					exceptionHandler.showDialog(oops);
				}
				watcher.close();
				Platform.exit();
				System.exit(0);
			}
		});
	}

	public void updateRecentlyUsedMenu() {

		if (menuBar == null) {
			return;
		}

		ObservableList<Menu> menus = menuBar.getMenus();

		if (menus == null || menus.size() == 0) {
			return;
		}

		Menu menu = menus.get(0);

		ArrayList<MenuItem> removeOld = new ArrayList<>();

		boolean sepFound = false;

		for (MenuItem item : menu.getItems()) {
			if (item instanceof SeparatorMenuItem || sepFound) {
				removeOld.add(item);
				sepFound = true;
			}
		}
		
		removeOld.forEach(item -> menu.getItems().remove(item));

		menu.getItems().add(new SeparatorMenuItem());

		recentlyUsed.getList().stream().forEach(path -> {
			MenuItem item = new MenuItem(path.toString());
			item.setUserData(path);
			item.setOnAction(e -> onOpenArchive(e));
			menu.getItems().add(item);
		});
	}

	private void closeArchive() {

		if (tabPane != null) {
			List<Tab> removeTabList = tabPane
					.getTabs()
					.stream()
					.filter(tab -> tab.getId() != null
							&& tab.getId().startsWith("sys:") == false)
					.collect(Collectors.toList());
			tabPane.getTabs().removeAll(removeTabList);
		}
	}

	private Tab createTextTab(String name, String content) {

		Tab tab = new Tab(name);
		TextArea textPane = new TextArea();
		textPane.setEditable(false);
		textPane.setFont(Font.font("Lucida Sans Typewriter", 13.0));
		textPane.setText(content);
		tab.setContent(textPane);
		return tab;
	}

	private void initSearchTableView() {

		searchTab.setClosable(false);
		ObservableList<SearchHit> data = FXCollections.observableArrayList();
		searchHits.setItems(data);
		searchHitCol.setCellFactory(new SearchHitCellFactory());
	}

	private void onFilterChanged() {

		String pattern = filterPattern.getText();

		TreeItem<ZipTreeValue> treeRoot = archiveModel.getTreeRoot();

		if (treeRoot == null) {
			return;
		}

		if (pattern.trim().length() > 0) {

			FilterTreeTask filterTreeTask = new RegexFilterTreeTask(
					Pattern.compile("^.*" + pattern + ".*$"), archiveModel);
			filterTreeTask.exceptionProperty().addListener(exceptionHandler);

			ProgressDialog dialog = new ProgressDialog(filterTreeTask);
			dialog.setHeaderText(resources.getString("key.archive_scanning"));
			Executors.newSingleThreadExecutor().submit(filterTreeTask);
			dialog.showAndWait();

			treeRoot = filterTreeTask.getTreeRoot();
		}

		treeView.setRoot(treeRoot);
		treeRoot.setExpanded(true);
	}

	private void onTreeDoubleClick(MouseEvent event) {

		if (event.getButton().equals(MouseButton.PRIMARY)
				&& event.getClickCount() == 2) {

			TreeItem<ZipTreeValue> selectedItem = treeView.getSelectionModel()
					.getSelectedItem();

			if (selectedItem == null) {
				return;
			}

			ZipTreeValue value = selectedItem.getValue();
			if (selectedItem == null || value == null
					|| value.getContent() == null) {
				return;
			}

			String content = value.getContent();

			FilteredList<Tab> tabList = tabPane.getTabs().filtered(
					t -> t.getText().equals(value.getName()));

			if (tabList.size() == 0) {
				tabPane.getTabs().add(0,
						createTextTab(value.getName(), content));
				tabPane.getSelectionModel().select(0);
			} else {
				tabPane.getSelectionModel().select(tabList.get(0));
			}

			tabPane.setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
		}
	}

	private void openArchive(File file) throws IOException {

		if (file == null) {
			return;
		}

		closeArchive();

		archiveModel.setArchive(file);
		recentlyUsed.setMostRecentlyUsed(file.toPath());
		updateRecentlyUsedMenu();

		archivePath.setText(file.getCanonicalPath());

		ScanArchiveTask scanArchiveTask = new ScanArchiveTask(archiveModel);
		scanArchiveTask.exceptionProperty().addListener(exceptionHandler);

		ProgressDialog dialog = new ProgressDialog(scanArchiveTask);
		dialog.setTitle(resources.getString("key.archive_scanning"));
		dialog.headerTextProperty().bind(fileNameProperty);
		dialog.setOnCloseRequest(closeRequest -> {
			scanArchiveTask.cancel();
		});

		DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.setPrefSize(600, 200);
		dialogPane.getButtonTypes().setAll(ButtonType.CANCEL);

		Executors.newSingleThreadExecutor().submit(scanArchiveTask);
		dialog.showAndWait();

		if (scanArchiveTask.isCancelled()) {
			return;
		}

		TreeItem<ZipTreeValue> treeRoot = archiveModel.getTreeRoot();
		treeRoot.setExpanded(true);
		treeView.setRoot(treeRoot);

		watcher.register(archiveModel.getArchiveFile());
	}

	private Tab selectSearchTab(TabPane tabPane) throws IOException {

		Tab searchTab = null;

		String tabTitle = resources.getString("key.search");
		List<Tab> searchTabList = tabPane.getTabs().stream()
				.filter(tab -> tab.getText().equals(tabTitle))
				.collect(Collectors.toList());

		if (searchTabList.size() > 1) {
			searchTabList.stream()
					.forEach(tab -> tabPane.getTabs().remove(tab));
		}

		int selectedIndex = 0;

		if (searchTabList.size() == 1) {
			searchTab = searchTabList.get(0);
			ObservableList<Tab> tabs = tabPane.getTabs();
			OptionalInt hit = IntStream.range(0, tabs.size())
					.filter(i -> tabs.get(i).getText().equals(tabTitle))
					.findFirst();
			if (hit.isPresent()) {
				selectedIndex = hit.getAsInt();
			}

		}

		tabPane.getSelectionModel().select(selectedIndex);
		return searchTab;
	}

	@FXML
	void onChangeCursor(ActionEvent event) {
		stage.getScene().setCursor(Cursor.HAND);
	}

}
