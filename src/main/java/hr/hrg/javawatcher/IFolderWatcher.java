package hr.hrg.javawatcher;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface IFolderWatcher<T> {

	/**
	 *  Takes changed files and returns when something is changed, but waits until no files change for some time (burstDelay parameter).<br> 
	 *  Returns null if interrupted, even if some files were changed when interrupt happened.
	 *  
	 * @param   burstDelay
	 *          (ms) how long to wait for more changes to pickup burst changes in a single batch
	 *
	 * @return  changed files, or {@code null} when interrupted
	 *  */
	Collection<FileChangeEntry<T>> takeBatch(long burstDelay);


	/** Get all files matched until now, including information on the matcher that matched the file. */
	Collection<FileChangeEntry<T>> getMatched();
	
	/** Get all files matched until now, including duplicates if mutliple matchers matched a file. Use {@link #getMatchedFilesUnique()} if you need only unique files.*/
	Collection<Path> getMatchedFiles();
	
	/** Get all unique files matched until now. */
	Set<Path> getMatchedFilesUnique();

	
	/**
	 * Add a {@link FileMatcher} that will be used to watch files/folders
	 * @return 
	 * */
	public <F extends FileMatcher<T>> F add(F matcher);
	
	/**
	 * List of tracked {@link FileMatcher}s
	 * */
	List<FileMatcher<T>> getMatchers();

	/** 
	 * Initialise {@link FileMatcher}s and optionally start watching the files.
	 * The WatchServise will be initialised if watching is requested, but to get changes
	 * you must start your own thread.
	 * to check for changed files.
	 * 
	 * @param registerForWatch also register with watch service
	 * 
	 * */
	void init(boolean registerForWatch);

	void close();
}