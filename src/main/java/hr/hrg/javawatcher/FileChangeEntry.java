package hr.hrg.javawatcher;

import java.nio.file.Path;


/**
 * Container for information regarding the changed file.
 * 
 * */
public class FileChangeEntry<T extends FileMatcher> {
	
	/** @see {@link #getPath()} */
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
	 
	/** Path to file that changed */
	public Path getPath() {
		return file;
	}
	
	/** FolderGlob that was listening for changes on this file when change occurred */
	public T getMatcher() {
		return matcher;
	}
	
	/** type of change */
	public FileChangeType getType() {
		return type;
	}
}
