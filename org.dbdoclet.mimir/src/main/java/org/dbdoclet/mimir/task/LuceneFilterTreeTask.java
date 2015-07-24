package org.dbdoclet.mimir.task;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.dbdoclet.mimir.ArchiveModel;
import org.dbdoclet.mimir.tree.ZipTreeValue;

public class LuceneFilterTreeTask extends FilterTreeTask {

	private List<Document> hits;

	public LuceneFilterTreeTask(Query query, ArchiveModel archiveModel) throws IOException, ParseException {
		
		super(archiveModel);
		hits = archiveModel.search(query);			
	}

	public boolean matches(ZipTreeValue item) {

		List<Document> list = hits.stream().filter(doc -> { 
			
			String docName = doc.get("name");
			return docName.equals(item.getFullyQualifiedName());
				
			}).collect(Collectors.toList()); 
		
		return list.size() > 0;
	}

}
