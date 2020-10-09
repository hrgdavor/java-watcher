package hr.hrg.javawatcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.DirectoryWatcher.Builder;
import io.methvin.watcher.hashing.FileHasher;
import io.methvin.watchservice.MacOSXListeningWatchService;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

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
public class FolderWatcher<T> implements AutoCloseable, IFolderWatcher<T> {

	static Builder builder = DirectoryWatcher.builder();
	public Object takeLock = new Object();
	
    /** {@link FileMatcher}s we are tracking */
    protected List<FileMatcher<T>> matchers = new ArrayList<>();	
	
    /**
     * Add a {@link FileMatcher} that will be used to watch files/folders
     * @return 
     * */
    @Override
	public <F extends FileMatcher<T>> F add(F matcher) {
    	matchers.add(matcher);
    	return matcher;
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
	@Override
	public Collection<FileChangeEntry<T>> takeBatch(long burstDelay) {
		try {
			Collection<FileChangeEntry<T>> batch = take();

			Collection<FileChangeEntry<T>> changed = null;
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
	 * Takes changed files, but waits until available. If you want to get null when take is interrupted instead of catching InterruptedException use {@link #takeBatch(long)}. 
	 * 
     * @return  changed files
     * @throws InterruptedException when the Thread is interrupted
	 * */
	public Collection<FileChangeEntry<T>> take() throws InterruptedException {
		synchronized (takeLock) {
			takeLock.wait();
			return getChanges();
		}
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
	public Collection<FileChangeEntry<T>> poll(long timeout, TimeUnit unit) throws InterruptedException {
		Thread.sleep(unit.toMillis(timeout));
		List<FileChangeEntry<T>> files = getChanges();		
		return files.size() == 0 ? null:files;
	}

	private List<FileChangeEntry<T>> getChanges() {
		List<FileChangeEntry<T>> files = new ArrayList<>();
		for(FileMatcher<T> m:matchers) {
			FileChangeEntry<T> entry = m.getQ().poll();
			while(entry != null) {
				files.add(entry);
				entry = m.getQ().poll();
			}
		}
		return files;
	}
	
	
	/** 
	 * Takes changed files, but waits until available. Returns null if interrupted. It is less verbose than catching InterruptedException.
	 * 
     * @return  changed files or null if interrupted
	 * */
	public Collection<FileChangeEntry<T>> takeOrNull(){
		try {
			return take();
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	/**
	 * List of tracked {@link FileMatcher}s
	 * */
	@Override
	public List<FileMatcher<T>> getMatchers(){
		return matchers;
	}
	
	/** Get all files matched until now, including information on the matcher that matched the file. */
	@Override
	public Collection<FileChangeEntry<T>> getMatched(){
		Collection<FileChangeEntry<T>> matched = new ArrayList<>();
		for(FileMatcher<T> m:matchers){
			for(Path p: m.getMatched()){
				matched.add(new FileChangeEntry<T>(p, FileChangeType.MODIFY, m));
			}
		}
		return matched;
	}

	/** Get all files matched until now, including duplicates if mutliple matchers matched a file. Use {@link #getMatchedFilesUnique()} if you need only unique files.*/
	@Override
	public Collection<Path> getMatchedFiles(){
		Collection<Path> matched = new ArrayList<>();
		for(FileMatcher<T> m:matchers){
			matched.addAll(m.getMatched());
		}
		return matched;
	}

	
	/** Get all unique files matched until now. */
	@Override
	public Set<Path> getMatchedFilesUnique(){
		Set<Path> matched = new HashSet<>();
		for(FileMatcher<T> m:matchers){
			matched.addAll(m.getMatched());
		}
		return matched;
	}
	
    /** 
     * Initialise {@link FileMatcher}s and optionally start watching the files.
     * The WatchServise will be initialised if watching is requested, but to get changes
     * you must start your own thread and use one of: {@link #take()}
     * to check for changed files.
     * 
     * @param registerForWatch also register with watch service
     * 
     * */
	@Override
	public void init(final boolean registerForWatch){

		List<Path> paths = new ArrayList<Path>();
		for(FileMatcher<T> matcher:matchers){
			paths.add(matcher.getRootPath());
			fillMatcher(matcher, registerForWatch);
		}

		
	}
	
	protected void fillMatcher(final FileMatcher<T> matcher, boolean registerForWatch){
		fillMatcher(matcher);
		if(registerForWatch) {
			try {
				FileHasher fileHasher = FileHasher.LAST_MODIFIED_TIME;
				Path rootPath = matcher.getRootPath().toAbsolutePath().normalize();
				DirectoryWatcher watcher = builder 
						.path(rootPath) // or use paths(directoriesToWatch)
						.listener(new DirectoryChangeListener() {
							@Override
							public void onEvent(DirectoryChangeEvent event) throws IOException {
								try {				
									Path path = event.path();
									if(path == null) return;
									path = path.toAbsolutePath();
									
									Path filename = rootPath.relativize(path);
									
									if(!matcher.offer(filename)) return;
									
									synchronized (takeLock) {								
										switch (event.eventType()) {
										case CREATE: /* file created */;
										matcher.getQ().add(new FileChangeEntry<T>(filename, FileChangeType.CREATE, matcher));
										System.out.println("Create: "+path);
										break;
										case MODIFY: /* file modified */; 
										matcher.getQ().add(new FileChangeEntry<T>(filename, FileChangeType.MODIFY, matcher));
										System.out.println("Modify: "+path);
										break;
										case DELETE: /* file deleted */; 
										matcher.getQ().add(new FileChangeEntry<T>(filename, FileChangeType.DELETE, matcher));
										System.out.println("Delete: "+path);
										break;
										case OVERFLOW: 
											Main.logWarn("Overflow while listening "+rootPath.toAbsolutePath());
											break;
										}
										
										takeLock.notifyAll();
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						})
						.watchService(osDefaultWatchService(fileHasher))
						.fileHasher(fileHasher)
						// .logger(logger) // defaults to LoggerFactory.getLogger(DirectoryWatcher.class)
						// .watchService(watchService) // defaults based on OS to either JVM WatchService or the JNA macOS WatchService
						.build();

				matcher.setWatcher(watcher);
				//watcher.watchAsync();
				new Thread(new Runnable() {
					public void run() {
						System.err.println("Watch async "+rootPath);
						watcher.watch();
						System.err.println("Watch async "+rootPath+" DONE");
					}
				},"watch "+rootPath).start();
			} catch (IOException e) {
				Main.logError(e.getMessage(), e);
			}
			
		}
	}
	
    private WatchService osDefaultWatchService(FileHasher fileHasher) throws IOException {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        if (isMac) {
              return new MacOSXListeningWatchService(
                  new MacOSXListeningWatchService.Config() {
                    @Override
                    public FileHasher fileHasher() {
                      return fileHasher;
                    }
                  });
        } else {
          return FileSystems.getDefault().newWatchService();
        }
      }
	
	/** 
	 * Collect all files accepted by the matcher, starting with rootPath of the matcher
	 * an checking recursively if the matcher is defined as such. Used when you just
	 *  want to check which files the matcher targets.
	 * @param matcher 
	 * */
	public static<T> void fillMatcher(final FileMatcher<T> matcher){

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
	
	
	@Override
	public void close() {
		try {
			for(FileMatcher<T> m:matchers) {				
				if(m.getWatcher() != null) m.getWatcher().close();
			}
		} catch (IOException e) {
			Main.logError(e.getMessage(), e);
		}
	}
}
