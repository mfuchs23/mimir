package org.dbdoclet.mimir.search;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

	
public class SearchHit {

	private final SimpleIntegerProperty indexProperty;
	private final SimpleObjectProperty<Data> contentProperty;

	public SearchHit(int index, String content, String path) {
		
		super();
		
		Data data = new Data();
		data.content = content;
		data.path = path;
		
		indexProperty = new SimpleIntegerProperty(index);
		contentProperty = new SimpleObjectProperty<Data>(data);
	}

	public Data getContent() {
		return contentProperty.get();
	}
	
	public int getIndex() {
		return indexProperty.get();
	}
	
	public void setContent(Data content) {
		contentProperty.set(content);
	}

	public void setIndex(int index) {
		indexProperty.set(index);
	}

	public class Data {
		public String path;
		public String content;
	}
}
