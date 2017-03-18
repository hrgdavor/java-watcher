package hr.hrg.javawatcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private WatchService watchService;

    /**
     * Add a {@link FileMatcher} that will be used to watch files/folders
     * @return 
     * */
    public F add(F matcher){
    	matchers.add(matcher);
    	return matcher;
    }
    
    /** 
     * Takes changed files without waiting, and returns {@code null} if none are changed yet.
     * 
     * @return  changed files, or {@code null}
     * */
	public Collection<FileChangeEntry<F>> poll() {
		
		WatchKey key = watchService.poll();
		
		return key == null ? null:getFilesOrNull(key);
	}
	
    /**
     *  Takes changed files and returns when something is changed, but waits until no files change for some time (burstDelay parameter).<br> 
     *  Returns null if interrupted, even if some files were changed when interrupt happened.
     *  
     * @param   burstDelay
     *          (ms) how long to wait for more changes to pickup burst changes in a single batch
     *
     * @return  changed files, or {@code null} when interrupted
     *  */
	public Collection<FileChangeEntry<F>> takeBatch(long burstDelay) {
		try {
			
			Collection<FileChangeEntry<F>> batch = take();

			Collection<FileChangeEntry<F>> changed = null;
			while(!Thread.interrupted()){
				changed = poll(burstDelay, TimeUnit.MILLISECONDS);

				if(changed == null && batch.size() >0) return batch;

				if(changed != null) batch.addAll(changed);
			}
		} catch (InterruptedException e) {
			// ignore the exception, and return null, thus notifying the caller that interrupt happened
		}

		return null;
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
     * @throws InterruptedException because null means no results yet, and can not be used to differentiate when the Thread was interrupted
     *  */
	public Collection<FileChangeEntry<F>> poll(long timeout, TimeUnit unit) throws InterruptedException {
		
		WatchKey key = watchService.poll(timeout, unit);
	
		return key == null ? null:getFilesOrNull(key);

	}
	
	
	/** 
	 * Takes changed files, but waits until available. If you want to get null when take is interrupted instead of catching InterruptedException use {@link #takeBatch(long)}. 
	 * 
     * @return  changed files
     * @throws InterruptedException when the Thread is interrupted
	 * */
	public Collection<FileChangeEntry<F>> take() throws InterruptedException {
		while(!Thread.interrupted()) {
			Collection<FileChangeEntry<F>> files = getFiles(watchService.take());
			if(files.size() > 0) return files;
			// something changed, but none for our matchers
		}
		throw new InterruptedException("take interrupted");
	}

	/** 
	 * Takes changed files, but waits until available. Returns null if interrupted. It is less verbose than catching InterruptedException.
	 * 
     * @return  changed files or null if interrupted
	 * */
	public Collection<FileChangeEntry<F>> takeOrNull(){
		try {
			return take();
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	/**
	 * List of tracked {@link FileMatcher}s
	 * */
	public List<F> getMatchers(){
		return matchers;
	}
	
	/** Get all files matched until now, including information on the matcher that matched the file. */
	public Collection<FileChangeEntry<F>> getMatched(){
		Collection<FileChangeEntry<F>> matched = new ArrayList<>();
		for(F m:matchers){
			for(Path p: m.getMatched()){
				matched.add(new FileChangeEntry<F>(p, FileChangeType.MODIFY, m));
			}
		}
		return matched;
	}

	/** Get all files matched until now, including duplicates if mutliple matchers matched a file. Use {@link #getMatchedFilesUnique()} if you need only unique files.*/
	public Collection<Path> getMatchedFiles(){
		Collection<Path> matched = new ArrayList<>();
		for(F m:matchers){
			matched.addAll(m.getMatched());
		}
		return matched;
	}

	
	/** Get all unique files matched until now. */
	public Set<Path> getMatchedFilesUnique(){
		Set<Path> matched = new HashSet<>();
		for(F m:matchers){
			matched.addAll(m.getMatched());
		}
		return matched;
	}
	
    /**
     * Register the given directory with the WatchService.
     * @param dir
     * @param matcher
     */
    protected void register(Path dir, F matcher) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_MODIFY);
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
     * */
	public void init(final boolean registerForWatch){

		if(registerForWatch && watchService == null) try {
			this.watchService = FileSystems.getDefault().newWatchService();
		} catch (IOException e){
			// this is not a recoverable error, so it is intentionally not a declared exception
			throw new RuntimeException(e.getMessage(), e);
		}

		for(F matcher:matchers){
			initMatcher(matcher, registerForWatch);
		}

	}
	
	/**
	 * Initialise the matcher by filling it with found files (matcher can reject some based on internal rules).
	 * Also register with watch service if needed (registerForWatch=true).
	 * 
	 *  @param matcher the {@link FileMatcher} to initialise
	 *  @param registerForWatch register with WatchService during walkFileTree
	 * */
	protected void initMatcher(final F matcher, final boolean registerForWatch) {
	    try {
	    	if(registerForWatch) register(matcher.getRootPath(), matcher);

	    	Files.walkFileTree(matcher.getRootPath(), new SimpleFileVisitor<Path>() {
			    
	    		@Override
			    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

			    	// we see the file for the first time, so it seems legitimate to use CREATE here
			    	matcher.offer(file);
			        return FileVisitResult.CONTINUE;
			    }

			    @Override
			    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			    	return (matcher.isRecursive() && !matcher.isExcluded(dir)) || dir.equals(matcher.getRootPath()) ? 
			    			super.preVisitDirectory(dir, attrs) : FileVisitResult.SKIP_SUBTREE;
			    }
			    
			    @Override
			    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			    	
			    	if(registerForWatch && !matcher.isExcluded(dir)) register(dir, matcher);
			    	
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
	 * Collect all files accepted by the matcher, starting with rootPath of the matcher
	 * an checking recursively if the matcher is defined as such. Used when you just
	 *  want to check which files the matcher targets.
	 * */
	public static void fillMatcher(final FileMatcher matcher){
		try {
			
			final boolean recursive = matcher.isRecursive();
			final Path rootPath = matcher.getRootPath();
			matcher.setCollectMatched(true);
	
			Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					matcher.offer(file);
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return (recursive && !matcher.isExcluded(dir)) || dir.equals(rootPath) ? 
							super.preVisitDirectory(dir, attrs): FileVisitResult.SKIP_SUBTREE;
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

	protected Collection<FileChangeEntry<F>> getFilesOrNull(WatchKey key) {
		Collection<FileChangeEntry<F>> files = getFiles(key);
		if(files.size() == 0) return null;
		return files;
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
