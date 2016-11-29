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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((matcher == null) ? 0 : matcher.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileChangeEntry other = (FileChangeEntry) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (matcher == null) {
			if (other.matcher != null)
				return false;
		} else if (!matcher.equals(other.matcher))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}
