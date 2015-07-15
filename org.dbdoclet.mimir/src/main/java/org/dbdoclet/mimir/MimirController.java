package org.dbdoclet.mimir;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.controlsfx.dialog.ProgressDialog;
import org.dbdoclet.mimir.dialog.ErrorDialog;
import org.dbdoclet.mimir.task.DuplicateReportTask;
import org.dbdoclet.mimir.task.FilterTreeTask;
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
public class MimirController implements Initializable {

	@FXML
	private MenuBar menuBar;
	@FXML
	private TabPane tabPane;
	@FXML
	private TreeView<ZipTreeValue> treeView;
	@FXML
	private TextField archivePath;
	@FXML
	private TextField filterPattern;
	@FXML
	private WebView reportView;

	private Stage stage;
	private ArchiveModel archiveModel;
	private ExceptionHandler exceptionHandler;
	private FileChooser fileChooser = new FileChooser();
	private RecentlyUsed recentlyUsed = new RecentlyUsed();
	private ResourceBundle resources;

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
			
		} catch (Throwable oops) {
			oops.printStackTrace();
		}
		
		archiveModel = new ArchiveModel(resources);
		archivePath.textProperty().bind(archiveModel.pathProperty());

		filterPattern.setOnAction(e -> onFilterChanged());

		treeView.setCellFactory((TreeView<ZipTreeValue> p) -> new ZipTreeCell(
				resources));
		treeView.setOnMouseClicked(e -> onTreeDoubleClick(e));
	}

	@FXML
	public void onCloseArchive(ActionEvent event) {

		treeView.setRoot(null);
		archiveModel.setArchive(null);
		closeArchive();
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
			if (event.getSource()  instanceof MenuItem) {
				MenuItem item = (MenuItem) event.getSource();
				Object userData = item.getUserData();
				if (userData instanceof Path) {
					file = ((Path) userData).toFile();
				}
			}
			
			if (recentlyUsed.getMostRecentlyUsed() != null) {
				fileChooser.setInitialDirectory(recentlyUsed.getMostRecentlyUsed().toFile().getParentFile());
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

				closeArchive();
				archiveModel.setArchive(file);
				recentlyUsed.setMostRecentlyUsed(file.toPath());
				updateRecentlyUsedMenu();
				
				ScanArchiveTask scanArchiveTask = new ScanArchiveTask(
						archiveModel);
				scanArchiveTask.exceptionProperty().addListener(
						exceptionHandler);

				ProgressDialog dialog = new ProgressDialog(scanArchiveTask);
				dialog.setHeaderText(resources
						.getString("key.archive_scanning"));
				Executors.newSingleThreadExecutor().submit(scanArchiveTask);
				dialog.showAndWait();

				TreeItem<ZipTreeValue> treeRoot = archiveModel.getTreeRoot();
				treeRoot.setExpanded(true);
				treeView.setRoot(treeRoot);
			}

		} catch (Throwable oops) {
			exceptionHandler.showDialog(oops);
		}
	}

	private void closeArchive() {
		tabPane.getTabs().clear();
	}

	@FXML
	public void onQuit(ActionEvent event) {
		try {
			recentlyUsed.save();
		} catch (IOException oops) {
			exceptionHandler.showDialog(oops);
		}
		stage.close();
	}

	public void setStage(Stage stage) {
		this.stage = stage;
		stage.setOnCloseRequest(e -> onCloseRequest(e));
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

	private Tab createTextTab(String name, String content) {

		Tab tab = new Tab(name);
		TextArea textPane = new TextArea();
		textPane.setEditable(false);
		textPane.setFont(Font.font("Courier New"));
		textPane.setText(content);
		tab.setContent(textPane);
		return tab;
	}

	private void onCloseRequest(WindowEvent e) {
		try {
			recentlyUsed.save();
		} catch (IOException oops) {
			exceptionHandler.showDialog(oops);
		}
		return;
	}

	private void onFilterChanged() {

		String pattern = filterPattern.getText();

		TreeItem<ZipTreeValue> treeRoot = archiveModel.getTreeRoot();

		if (treeRoot == null) {
			return;
		}

		if (pattern.trim().length() > 0) {

			FilterTreeTask filterTreeTask = new FilterTreeTask(pattern,
					archiveModel);
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

			ZipTreeValue value = selectedItem.getValue();
			if (selectedItem == null || value == null
					|| value.getContent() == null) {
				return;
			}

			String content = value.getContent();

			tabPane.getTabs().add(0, createTextTab(value.getName(), content));
			tabPane.getSelectionModel().select(0);
			tabPane.setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
		}
	}

	@FXML
	void onChangeCursor(ActionEvent event) {
		stage.getScene().setCursor(Cursor.HAND);
	}

}
