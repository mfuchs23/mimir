package org.dbdoclet.mimir.task;

import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;

import org.dbdoclet.mimir.ArchiveModel;
import org.dbdoclet.mimir.IVisitor;
import org.dbdoclet.mimir.IconManager;
import org.dbdoclet.mimir.tree.ZipTreeValue;

public class FilterTreeTask extends Task<Void> implements
		IVisitor<TreeItem<ZipTreeValue>> {

	private ArchiveModel archiveModel;

	private TreeItem<ZipTreeValue> treeRoot;
	private TreeItem<ZipTreeValue> treeParent;
	private IconManager iconManager;
	private String pattern;

	public FilterTreeTask(String pattern, ArchiveModel archiveModel) {
		this.pattern = pattern;
		this.archiveModel = archiveModel;
		iconManager = new IconManager();
	}

	@Override
	public void accept(TreeItem<ZipTreeValue> item) {

		String name = item.getValue().getName();
		updateMessage(name);

		if (item.getValue().isDirectory() == false) {

			if (name.matches(pattern)) {
				TreeItem<ZipTreeValue> copyOf = new TreeItem<>(item.getValue());
				copyOf.setGraphic(item.getGraphic());
				treeParent.getChildren().add(copyOf);
			}

		} else {

			TreeItem<ZipTreeValue> copyOf = new TreeItem<>(item.getValue());
			copyOf.setGraphic(item.getGraphic());
			copyOf.setExpanded(true);
			treeParent.getChildren().add(copyOf);
			treeParent = copyOf;
		}
	}

	@Override
	public void after(TreeItem<ZipTreeValue> item) {
		
		if (treeParent.getParent() != null && item.getValue().isDirectory() == true) {
		
			TreeItem<ZipTreeValue> treeFolder = treeParent;
			treeParent = treeParent.getParent();
			
			if (treeFolder.getChildren().size() == 0) {
				treeParent.getChildren().remove(treeFolder);
			}
		}
	}

	@Override
	protected Void call() throws Exception {

		treeRoot = new TreeItem<>(new ZipTreeValue(archiveModel
				.getArchiveFile().getName()));
		treeRoot.setGraphic(new ImageView(iconManager.getRootIcon()));
		treeParent = treeRoot;
		archiveModel.scanTree(archiveModel.getTreeRoot(), this);
		return null;
	}

	public TreeItem<ZipTreeValue> getTreeRoot() {
		return treeRoot;
	}

}
