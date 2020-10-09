package hr.hrg.javawatcher;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Combination of single {@link FolderWatcherOld} and a {@link FileMatchGlob} to simplify simple watch situations*/
public class GlobWatcher<T> implements AutoCloseable{
	IFolderWatcher<T> watcher = Main.makeWatcher();
	FileMatchGlob<T> matcher;

	public GlobWatcher(Path root, boolean recursive) {
		if(root == null) throw new NullPointerException("root must be defined");
		matcher = new FileMatchGlob<T>(root, recursive);
		watcher.add(matcher);
	}

	/** Recursive from a root. */
	public GlobWatcher(Path root) {
		this(root,true);
	}

	
	public Path relativize(Path path) {
		return matcher.relativize(path);
	}

	public FileMatchGlob<T> add(FileMatchGlob<T> matcher) {
		return watcher.add(matcher);
	}
		
	public static final <T> Collection<Path> toPaths(Collection<FileChangeEntry<T>> changes) {
		if(changes == null) return null;
		Collection<Path> paths = new ArrayList<Path>(changes.size());
		
		for(FileChangeEntry<T> p:changes) paths.add(p.getPath());
		
		return paths;
	}
	
	public static final <T> Collection<Path> toPathsUnique(Collection<FileChangeEntry<T>> changes) {
		if(changes == null) return null;
		Collection<Path> paths = new HashSet<Path>(changes.size());
		
		for(FileChangeEntry<T> p:changes) paths.add(p.getPath());
		
		return paths;
	}

	public Collection<FileChangeEntry<T>> takeBatch(long burstDelay) {
		return watcher.takeBatch(burstDelay);
	}
	
	public Collection<Path> takeBatchFiles(long burstDelay) {
		return toPaths(watcher.takeBatch(burstDelay));
	}

	public Collection<Path> takeBatchFilesUnique(long burstDelay) {
		return toPathsUnique(watcher.takeBatch(burstDelay));
	}

	public Collection<FileChangeEntry<T>> getMatched() {
		return watcher.getMatched();
	}

	public Collection<Path> getMatchedFiles() {
		return watcher.getMatchedFiles();
	}

	public Set<Path> getMatchedFilesUnique() {
		return watcher.getMatchedFilesUnique();
	}

	public void init(boolean registerForWatch) {
		watcher.init(registerForWatch);
	}

	public FileMatchGlob<T> includes(Collection<String> globs) {
		return matcher.includes(globs);
	}

	public FileMatchGlob<T> includes(String... globs) {
		return matcher.includes(globs);
	}

	public FileMatchGlob<T> excludes(Collection<String> globs) {
		return matcher.excludes(globs);
	}

	public FileMatchGlob<T> excludes(String... globs) {
		return matcher.excludes(globs);
	}

	public List<PathMatcher> getExcludes() {
		return matcher.getExcludes();
	}

	public List<PathMatcher> getIncludes() {
		return matcher.getIncludes();
	}

	public Collection<Path> getExcluded() {
		return matcher.getExcluded();
	}

	public int getExcludedCount() {
		return matcher.getExcludedCount();
	}

	public boolean isCollectExcluded() {
		return matcher.isCollectExcluded();
	}

	public boolean isCollectMatched() {
		return matcher.isCollectMatched();
	}

	public void setCollectExcluded(boolean collectExcluded) {
		matcher.setCollectExcluded(collectExcluded);
	}

	public void setCollectMatched(boolean collectMatched) {
		matcher.setCollectMatched(collectMatched);
	}

	public boolean isMatch(Path path) {
		return matcher.isMatch(path);
	}

	public boolean isExcluded(Path path) {
		return matcher.isExcluded(path);
	}

	public Path getRootPath() {
		return matcher.getRootPath();
	}
	
	public Path getRootPathAbs() {
		return matcher.getRootPathAbs();
	}

	public boolean isRecursive() {
		return matcher.isRecursive();
	}

	public void close() {
		watcher.close();
	}
	
}
