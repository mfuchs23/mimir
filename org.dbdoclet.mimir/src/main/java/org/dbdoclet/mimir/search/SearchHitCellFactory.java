package org.dbdoclet.mimir.search;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.dbdoclet.mimir.view.WebViewRegion;

public class SearchHitCellFactory implements
		Callback<TableColumn<SearchHit, String>, TableCell<SearchHit, String>> {

	@Override
	public TableCell<SearchHit, String> call(TableColumn<SearchHit, String> param) {
		
		TableCell<SearchHit, String> cell = new TableCell<SearchHit, String>()  {
			
			@Override
			public void updateItem(String item, boolean empty) {
				
				if (item != null) {
					VBox vb = new VBox();
					vb.setAlignment(Pos.CENTER);
					WebViewRegion webView = new WebViewRegion(item);
					vb.getChildren().add(webView);
					setGraphic(vb);
				}
			}
		};
		
		return cell;
	}

}
