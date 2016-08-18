package org.dbdoclet.mimir.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;


public class SearchEngine {

	private Directory directory;
	private Analyzer analyzer;

	public SearchEngine() {
		analyzer = new StandardAnalyzer();
		directory = new RAMDirectory();
	}

	public Query createQuery(String field, String text) throws ParseException {

		QueryParser queryParser = new QueryParser(field, analyzer);
		return queryParser.parse(text);
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}


	public Directory getDirectory() {
		return directory;
	}

	public List<Document> search(Query query) throws IOException, ParseException {

		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);

		ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
		List<Document> docs = Arrays.stream(hits).map(scoreDoc -> {
			try {
				return isearcher.doc(scoreDoc.doc);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}).collect(Collectors.toList());

		ireader.close();

		return docs;
	}
	
	public ObservableList<SearchHit> search(String text) throws ParseException, IOException, InvalidTokenOffsetsException {

		ObservableList<SearchHit> resultList = FXCollections.observableArrayList();
		int index = 1;
		if (text.trim().length() > 0 && text.equals("*") == false) {

			Query query = createQuery("content", text);
			
			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
			Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
			
			DirectoryReader ireader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(ireader);
			
			TopDocs hits = searcher.search(query, 1000);
			
			for (int i = 0; i < hits.totalHits; i++) {
			
				int id = hits.scoreDocs[i].doc;
				
				Document doc = searcher.doc(id);
				String content = doc.get("content");
				
				@SuppressWarnings("deprecation")
				TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "content", analyzer);
				TextFragment[] fragments = highlighter.getBestTextFragments(tokenStream, content, false, 10);				
				
				StringBuilder buffer = new StringBuilder();
				buffer.append("<h1 style='font-family: Arial; font-size: 10pt;'>");
				buffer.append(doc.get("name"));                                                                        
				buffer.append("</h1>\n");
				
				for (TextFragment frag : fragments) {
					buffer.append("<p style='font-family: Arial; font-size: 10pt;'>");
					buffer.append(applyFragStyle(frag.toString()));
					buffer.append("</p>\n");
					break;
				}
				
				resultList.add(new SearchHit(index++, buffer.toString()));
			}
		}

		return resultList;
	}

	private Object applyFragStyle(String html) {
		
		html = html.replaceAll("<[Bb]>", "<span style='background-color: yellow'>");
		html = html.replaceAll("</[Bb]>", "</span>");
		return html;
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}


	public void setDirectory(Directory directory) {
		this.directory = directory;
	}

	public void newDirectory() throws IOException {

		if (directory != null) {
			directory.close();
			directory = new RAMDirectory();
		}
	}

}
