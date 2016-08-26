package org.dbdoclet.mimir.view;

import java.util.Set;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker.State;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.dbdoclet.mimir.search.SearchHit;

public final class WebViewRegion extends Region {
 
    final WebView webview = new WebView();
    final WebEngine webEngine = webview.getEngine();
     
    public WebViewRegion(SearchHit.Data item) {    	
    	
    	webview.setPrefHeight(5);
    	
        widthProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) { 
                Double width = (Double)newValue;
                webview.setPrefWidth(width);
                adjustHeight();
            }    
        });
 
        webview.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue<? extends State> arg0, State oldState, State newState)         {
                if (newState == State.SUCCEEDED) {
                    adjustHeight();
                }				
            }
        });        
        
        webview.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> change) {
                Set<Node> scrolls = webview.lookupAll(".scroll-bar");
                scrolls.forEach(scroll -> scroll.setVisible(false));
            }
    	});
        
        webview.setOnMouseClicked(event -> {
        	if (event.getClickCount() == 2) {
        		System.out.println(item.path);
        	}
        });
        
        setContent(item.content);
        
        getChildren().add(webview);
    }
    
    public void setContent(final String content) {
        Platform.runLater(new Runnable(){
            @Override                                
            public void run() {
                webEngine.loadContent(getHtml(content));
                Platform.runLater(new Runnable(){
                    @Override                                
                    public void run() {
                        adjustHeight();
                    }               
                });
            }               
    	});
    }
    
 
    @Override 
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(webview,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
    }
    
    private void adjustHeight() {
        Platform.runLater(new Runnable(){
            @Override                                
            public void run() {
            	try {
                    Object result = webEngine.executeScript("document.getElementById('webview').offsetHeight");
                    if (result instanceof Integer) {
                        Integer i = (Integer) result;
                        double height = new Double(i);
                        height = height + 20;
                        webview.setPrefHeight(height);
                        webview.getPrefHeight();
                    }
            	} catch (Exception oops) { 
            		//  Kann passieren :-)
            	}
            }               
        });
    }
    
    private String getHtml(String content) {
        return "<html><body><div id=\"webview\">" 
        		+ content 
        		+ "</div></body></html>";
    }
 
}