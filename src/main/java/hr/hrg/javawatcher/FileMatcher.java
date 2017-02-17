package hr.hrg.javawatcher;

import java.nio.file.Path;
import java.util.Collection;

public interface FileMatcher {

	/** The root path of the matcher. Files found and include/exclude rules are all relative to this path. */
	public Path getRootPath();

	/** if this matcher should look for files recursively */
	public boolean isRecursive();

	/** Check if the provided path is a match
	 * 
	 * @param path 
	 * 
	 * @return true if matched by defined rules
	 * */
	public boolean isMatch(Path path);

	/** Check if the provided path is excluded
	 * 
	 * @param path 
	 * 
	 * @return true if matched by defined exclude rules 
	 * */
	public boolean isExcluded(Path path);

	/** Check if the provided path is a match and store in internal include/exclude lists.
	 * 
	 * Implementations that do not want to store matched/excluded paths can just return result from {@link #isMatch(Path)}
	 * 
	 * @param path 
	 * 
	 * @return true if matched by defined include rules
	 * */
	public boolean offer(Path file);

	/**
	 * Method that will be called when a previously existing folder is deleted, allowing maintenance of live files list.
	 * 
	 * Implementations that do not want to store matched/excluded paths can just leave method empty
	 * */
	public void dirInvalid(Path path);

	/**
	 * Method that will be called when a previously existing file is deleted, allowing maintenance of live files list.
	 * 
	 * Implementations that do not want to store matched/excluded paths can just leave method empty
	 * */
	public void fileDeleted(Path path);

	/**
	 * Get the current collection of files offered and accepted based on the rules.
	 * You must {@link #setCollectMatched(boolean)} during initialisation, or the list will be empty.
	 * */
	public Collection<Path> getMatched();

	/**
	 * Are matched files for later listing. Use when you want to know what were collected.
	 * */
	public void setCollectMatched(boolean collectMatched);

	public Path relativize(Path path);
	
}
