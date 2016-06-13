package org.dbdoclet.mimir.tree;

import java.io.File;
import java.io.FileWriter;
import java.util.ResourceBundle;




import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class TreeContextMenu extends ContextMenu {

	private ZipTreeValue item;
	
	public TreeContextMenu(ResourceBundle resources) {
		
		MenuItem openMenuItem = new MenuItem(resources.getString("key.open"));
		getItems().add(openMenuItem);
		openMenuItem.setOnAction((ActionEvent t) -> {

			try {
				
				if (item == null || item.isDirectory()) {
					return;
				}
			
				String content = item.getContent();
				String name = item.getName();
				
				String extension = getFileExtension(name);
				
				File tmpFile = File.createTempFile("mimir", extension);
				tmpFile.deleteOnExit();
				
				FileWriter writer = new FileWriter(tmpFile);
				writer.write(content);
				writer.close();
				 
				System.out.println(String.format("Path: %s", tmpFile.getCanonicalPath()));
				new ProcessBuilder().command("C:/Program Files/emacs/bin/emacsclientw", "--no-wait", tmpFile.getCanonicalPath()).start();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private String getFileExtension(String name) {
		
		String extension = null;

		if (name == null) {
			return null;
		}
		
		int indexOfDot = name.lastIndexOf(".");
		if (indexOfDot > -1) {
			extension = name.substring(indexOfDot);
		}
		
		return extension;
	}

	public void setItem(ZipTreeValue item) {
		this.item = item;
	}
}
