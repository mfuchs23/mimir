package org.dbdoclet.mimir;

import javafx.scene.image.Image;

public class IconManager {

    private final Image rootIcon = new Image(getClass().getResourceAsStream("resources/icons/briefcase_16.png"));
    private final Image folderIcon = new Image(getClass().getResourceAsStream("resources/icons/folder_16.png"));

	public Image getRootIcon() {
		return rootIcon;
	}

	public Image getFolderIcon() {
		return folderIcon;
	}
  }
