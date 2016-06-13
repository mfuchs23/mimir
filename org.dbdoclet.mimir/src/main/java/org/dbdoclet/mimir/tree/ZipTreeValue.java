package org.dbdoclet.mimir.tree;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.zip.ZipEntry;

public class ZipTreeValue  {

	private ZipEntry zipEntry;
	private String name;
	private String content;
	private boolean isDirectory = false;
	private String fullyQualifiedName;

	public ZipTreeValue(String name) {
		this.name = name;
	}

	public ZipTreeValue(ZipEntry entry) {

		if (entry == null) {
			throw new IllegalArgumentException("Argument entry must not be null!");
		}
		
		this.zipEntry = entry;

		String[] tokens = entry.getName().split("/");
		name = tokens[tokens.length - 1];
	}

	private String createTreeName(ZipEntry entry) {

		DateTimeFormatter df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
		
		String[] tokens = entry.getName().split("/");
		String name = tokens[tokens.length -1];
		Date time = new Date(entry.getTime());
		LocalDateTime ldt = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
		return String.format("%s %s %d", name, df.format(ldt), entry.getSize());
	}

	public String getContent() {
		
		if (content == null) {
			return "";
		}
		
		return content;
	}

	public String getName() {
		return name;
	}

	public String getFullyQualifiedName() {
		return fullyQualifiedName;
	}
	
	public void setFullyQualifiedName(String fullyQualifiedName) {
		this.fullyQualifiedName = fullyQualifiedName;
	}

	public ZipEntry getZipEntry() {
		return zipEntry;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
	
		if (zipEntry != null) {
			return createTreeName(zipEntry);
		}
		
		if (name != null) {
			return name;
			
		}
		return super.toString();
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void isDirectory(boolean isDiretory) {
		this.isDirectory= isDiretory ;
	}
}
