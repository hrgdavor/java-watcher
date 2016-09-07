package org.hrg.javawatcher;

import java.nio.file.Path;


/**
 * Container for information regarding the changed file.
 * 
 * */
public class FileChangeEntry<T extends FileMatcher> {
	
	/** @see {@link #getFile()} */
	private final Path file;

	/** @see {@link #getMatcher()} */
	private final T matcher;

	/** @see {@link #getType()} */
	private final FileChangeType type;

	public FileChangeEntry(Path file, FileChangeType type, T matcher) {
		this.file = file;
		this.type = type;
		this.matcher = matcher;
	}
	 
	/** File that changed */
	public Path getFile() {
		return file;
	}
	
	/** FolderGlob that was listening for changes on this file when change occured */
	public T getMatcher() {
		return matcher;
	}
	
	/** type of change */
	public FileChangeType getType() {
		return type;
	}
}
