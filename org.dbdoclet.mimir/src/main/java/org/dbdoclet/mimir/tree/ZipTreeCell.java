package org.dbdoclet.mimir.tree;

import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;

import org.dbdoclet.mimir.ExceptionHandler;
import org.dbdoclet.mimir.dialog.SearchDialog;

public class ZipTreeCell extends TreeCell<ZipTreeValue> {

	private final ContextMenu contextMenu = new ContextMenu();

	public ZipTreeCell(ResourceBundle resources) {

		MenuItem addMenuItem = new MenuItem(resources.getString("key.search"));
		contextMenu.getItems().add(addMenuItem);
		addMenuItem.setOnAction((ActionEvent t) -> {
			System.out.println("onAction");
		});
	}

	@Override
	public void updateItem(ZipTreeValue item, boolean empty) {
		
		super.updateItem(item, empty);
		
		if (empty == true) {
			setText(null);
			setGraphic(null);
		} else {
			setText(item.toString());
			setGraphic(getTreeItem().getGraphic());
			setContextMenu(contextMenu);
		}
	}
}