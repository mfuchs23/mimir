package org.dbdoclet.mimir.search;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SearchHit {

	private final SimpleIntegerProperty indexProperty;
	private final SimpleStringProperty fileNameProperty;
	
	public SearchHit(int index, String fileName) {
		super();
		indexProperty = new SimpleIntegerProperty(index);
		fileNameProperty = new SimpleStringProperty(fileName);
	}
	
	public String getFileName() {
		return fileNameProperty.get();
	}
	
	public int getIndex() {
		return indexProperty.get();
	}
	
	public void setFileName(String fileName) {
		fileNameProperty.set(fileName);
	}

	public void setIndex(int index) {
		indexProperty.set(index);
	}
}
