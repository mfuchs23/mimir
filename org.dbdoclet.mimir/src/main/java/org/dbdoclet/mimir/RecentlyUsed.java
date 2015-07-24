package org.dbdoclet.mimir;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javafx.scene.control.MenuBar;

public class RecentlyUsed {

	private static final int MAX_LIST_SIZE = 20;

	private ArrayList<Path> recentlyUsedList = new ArrayList<>();

	public void setMostRecentlyUsed(Path mostRecentlyUsed) {

		if (recentlyUsedList.contains(mostRecentlyUsed)) {
			recentlyUsedList.remove(mostRecentlyUsed);
		}

		recentlyUsedList.add(0, mostRecentlyUsed);

		while (recentlyUsedList.size() > MAX_LIST_SIZE) {
			recentlyUsedList.remove(recentlyUsedList.size() - 1);
		}
	}

	public void save() throws FileNotFoundException, IOException {

		try (PrintWriter fw = new PrintWriter(new FileWriter(getFileName()))) {
			recentlyUsedList.stream().forEach(path -> fw.println(path.toAbsolutePath().toString()));
		}
	}

	private Path getPath() {
		Path path = Paths.get(System.getProperty("user.home"), ".mimir-rcu");
		return path;
	}
	
	private String getFileName() {
		return getPath().toAbsolutePath().toString();
	}

	public void load(MenuBar menuBar) throws FileNotFoundException, IOException, ClassNotFoundException {

		Path path = getPath();
		
		if (path.toFile().exists() == false) {
			return;
		}
		
		try (BufferedReader reader = new BufferedReader(new FileReader(getFileName()))) {
			reader.lines().forEach(line -> {
				Path p = Paths.get(line);
				if (p.toFile().exists()) {
					recentlyUsedList.add(p);
				}
			});
		}
	}

	public Path getMostRecentlyUsed() {

		if (recentlyUsedList.size() > 0) {
			return recentlyUsedList.get(0);
		}

		return null;
	}

	public ArrayList<Path> getList() {
		return recentlyUsedList;
	}
}
