package org.dbdoclet.mimir.search;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.dbdoclet.mimir.view.WebViewRegion;

public class SearchHitCellFactory implements
		Callback<TableColumn<SearchHit, SearchHit.Data>, TableCell<SearchHit, SearchHit.Data>> {

	@Override
	public TableCell<SearchHit, SearchHit.Data> call(
			TableColumn<SearchHit, SearchHit.Data> param) {

		TableCell<SearchHit, SearchHit.Data> cell = new TableCell<SearchHit, SearchHit.Data>() {

			@Override
			public void updateItem(SearchHit.Data item, boolean empty) {

				VBox vb = new VBox();
				vb.setAlignment(Pos.CENTER);
				
				if (item != null && empty == false) {
					WebViewRegion webView = new WebViewRegion(item);
					vb.getChildren().add(webView);
				} 

				setGraphic(vb);
			}
		};

		return cell;
	}

}
