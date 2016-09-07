package org.hrg.javawatcher;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for grouping different folder watching tasks and handling changes from a single
 * point. 
 * 
 * When needed, you can extend {@link FileMatcher} class to add extra information that can then easily 
 * be retrieved when files change. 
 * 
 * This class is generic to make it easier to work with your own customised {@link FileMatcher} Implementation.
 * 
 * */
public class FolderWatcher<F extends FileMatcher> {

	Logger log = LoggerFactory.getLogger(FolderWatcher.class);
	
    protected final Map<WatchKey,WatchEntry<F>> keys = new HashMap<WatchKey,WatchEntry<F>>();

    /** {@link FileMatcher}s we are tracking */
    protected List<F> matchers = new ArrayList<>();

    private WatchService watcher;

    /**
     * Add a {@link FileMatcher} that will be used to watch files/folders
     * */
    public void add(F matcher){
    	matchers.add(matcher);
    }
    
    /** 
     * Takes changed files without waiting, and returns {@code null} if none are changed yet.
     * 
     * @return  changed files, or {@code null}
     * */
	public Collection<FileChangeEntry<F>> poll() {
		
		WatchKey key = watcher.poll();
		
		return key == null ? null:getFiles(key);
	}
	
    /**
     *  Takes changed files waiting the desired time first, and returns {@code null} if none are changed yet.
     *  
     * @param   timeout
     *          how to wait before giving up, in units of unit
     * @param   unit
     *          a {@code TimeUnit} determining how to interpret the timeout
     *          parameter
     *
     * @return  changed files, or {@code null}
     *  */
	public Collection<FileChangeEntry<F>> poll(long timeout, TimeUnit unit) {
		try {
			
			WatchKey key = watcher.poll(timeout, unit);
		
			return key == null ? null:getFiles(key);

		} catch (InterruptedException e) {
			throw new RuntimeException("File watch interrupted.", e);
		}
	}

	/** 
	 * Takes changed files, but waits until available. 
	 * 
     * @return  changed files
	 * */
	public Collection<FileChangeEntry<F>> take() {
		try {
			return getFiles(watcher.take());
		} catch (InterruptedException e) {
			throw new RuntimeException("File watch interrupted.", e);
		}
	}

	/**
	 * List of tracked {@link FileMatcher}s
	 * */
	public List<F> getMatchers(){
		return matchers;
	}
	
    /**
     * Register the given directory with the WatchService.
     * @param dir
     * @param input - is this folder for input files
     */
    protected void register(Path dir, F matcher) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_MODIFY);
        WatchEntry<F> prev = keys.get(key);
        log.debug("watch {} for {}", dir, matcher);
        if (prev == null) {
        	prev = new WatchEntry<F>(dir);
            keys.put(key, prev);
        }
        if(!prev.getMatchers().contains(matcher))
        	prev.getMatchers().add(matcher);
        
    } 	

    /** 
     * Initialise {@link FileMatcher}s and optionally start watching the files.
     * The WatchServise will be initialised if watching is requested, but to get changes
     * you must start your own thread and use one of: {@link #take()} or {@link #poll()}  or {@link #poll(long, TimeUnit)}
     * to check for changed files.
     * 
     * @param registerForWatch also register with watch service
     * 
     * @return files found and accepted by all {@link FileMatcher}s (every FileChangeEntry in this phase will be of type:{@link FileChangeType#CREATE})
     * */
	public Collection<FileChangeEntry<F>> init(final boolean registerForWatch){

		if(registerForWatch) try {
			this.watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e){
			// this is not a recoverable error, so it is intentionally not a declared exception
			throw new RuntimeException(e.getMessage(), e);
		}

		Collection<FileChangeEntry<F>> found = new ArrayList<>();
		for(F matcher:matchers){
			initMatcher(matcher, registerForWatch, found);
		}

		return found;
	}
	
	/**
	 * Initialise the matcher by filling it with found files (matcher can reject some based on internal rules).
	 * Also register with watch service if needed (registerForWatch=true).
	 * 
	 *  @param matcher the {@link FileMatcher} to initialise
	 *  @param registerForWatch register with WatchService during walkFileTree
	 *  @param found collection to fill with files found and accepted by the {@link FileMatcher}
	 * */
	protected void initMatcher(final F matcher, final boolean registerForWatch, final Collection<FileChangeEntry<F>> found) {
	    try {
	    	register(matcher.getRootPath(), matcher);

	    	Files.walkFileTree(matcher.getRootPath(), new SimpleFileVisitor<Path>() {
			    @Override
			    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

			    	// we see the file for the first time, so it seems legitimate to use CREATE here
			    	if(matcher.offer(file))
			    		found.add(new FileChangeEntry<F>(file, FileChangeType.CREATE, matcher));
			    	
			        return FileVisitResult.CONTINUE;
			    }

			    @Override
			    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			    	return (matcher.isRecursive() && !matcher.excluded(dir)) || dir.equals(matcher.getRootPath()) ? 
			    			super.preVisitDirectory(dir, attrs) : FileVisitResult.SKIP_SUBTREE;
			    }
			    
			    @Override
			    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			    	
			    	
			    	if(registerForWatch && !matcher.excluded(dir)) register(dir, matcher);
			    	
			    	return super.postVisitDirectory(dir, exc);
			    }
			    
			    @Override
			    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			        return FileVisitResult.CONTINUE;
			    }
			});
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		
	}
	
	/**
	 * Collect all files from the WatchKey.
	 * 
	 * @return collection of {@link FileChangeEntry} 
	 * */
	protected Collection<FileChangeEntry<F>> getFiles(WatchKey key) {
		List<FileChangeEntry<F>> files = new ArrayList<>();
		WatchEntry<F> dir = keys.get(key);
		
		for (WatchEvent<?> event : key.pollEvents()) {
			FileChangeType type = FileChangeType.fromKind(event.kind()); 
			
			if (type == null) {// not supported by us
				continue;
			}
			
			@SuppressWarnings("unchecked")
			WatchEvent<Path> ev = (WatchEvent<Path>) event;
			Path filename = dir.getFolder().resolve(ev.context());
			
			for(F matcher:dir.getMatchers()){
				// if multiple {@link FileMatcher}s are registered for this folder
				// still that file can be excluded, so we need to check
				// before adding the FileChangeEntry for this FileMatcher to the collection
				if(matcher.offer(filename)){
					files.add(new FileChangeEntry<F>(filename, type, matcher));
					if(type == FileChangeType.DELETE)
						matcher.fileDeleted(filename);
				}
			}
		}
		
		boolean valid = key.reset();
		if (!valid) {
			key.cancel(); // unregister from watch service
			for(F matcher:dir.getMatchers()){
				// remove all files that were in the folder
				matcher.dirInvalid(dir.getFolder());
			}
		}
		
		return files;
	}
	
}
