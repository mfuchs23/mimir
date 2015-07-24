package org.dbdoclet.mimir.task;

import java.util.regex.Pattern;

import org.dbdoclet.mimir.ArchiveModel;
import org.dbdoclet.mimir.tree.ZipTreeValue;

public class RegexFilterTreeTask extends FilterTreeTask {

	private Pattern pattern;

	public RegexFilterTreeTask(Pattern pattern, ArchiveModel archiveModel) {
		
		super(archiveModel);
		this.pattern = pattern;
	}

	public boolean matches(ZipTreeValue item) {

		if (pattern == null || item.getName() == null) {
			return false;
		}
		
		return pattern.matcher(item.getName()).matches();
	}

}
