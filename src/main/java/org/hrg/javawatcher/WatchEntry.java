package org.hrg.javawatcher;

import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

/** Data object to store info while registering listeners with {@link WatchService}.
 * */
public class WatchEntry<T extends FileMatcher> {
	private final Path folder;
	private List<T> matchers;
	
	public WatchEntry(Path folder) {
		this(folder, new ArrayList<T>());
	}

	public WatchEntry(Path folder, List<T> folderGlob) {
		this.folder = folder;
		this.matchers = folderGlob;
	}

	public Path getFolder() {
		return folder;
	}
	
	public List<T> getMatchers() {
		return matchers;
	}
}
