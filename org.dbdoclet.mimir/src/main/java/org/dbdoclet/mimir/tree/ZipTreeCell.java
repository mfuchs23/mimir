package org.dbdoclet.mimir.tree;

import java.util.ResourceBundle;

import javafx.scene.control.TreeCell;

public class ZipTreeCell extends TreeCell<ZipTreeValue> {

	private final TreeContextMenu contextMenu;

	public ZipTreeCell(ResourceBundle resources) {
		contextMenu = new TreeContextMenu(resources);
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
			contextMenu.setItem(item);
			setContextMenu(contextMenu);
		}
	}
}