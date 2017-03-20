package org.dbdoclet.mimir.tab;

import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;

import org.dbdoclet.mimir.tree.ZipTreeValue;

public class TreeItemTab extends Tab {

	private TreeItem<ZipTreeValue> treeItem;

	public TreeItemTab(TreeItem<ZipTreeValue> treeItem) {
		super();
		this.treeItem = treeItem;
	}

	public TreeItem<ZipTreeValue> getTreeItem() {
		return treeItem;
	}
}
