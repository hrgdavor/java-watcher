package hr.hrg.javawatcher;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Combination of single {@link FolderWatcher} and a {@link FileMatchGlob} to simplify simple watch situations*/
public class GlobWatcher {
	FolderWatcher<FileMatchGlob> watcher = new FolderWatcher<FileMatchGlob>();
	FileMatchGlob matcher;
	
	public Path relativize(Path path) {
		return matcher.relativize(path);
	}

	public FileMatchGlob add(FileMatchGlob matcher) {
		return watcher.add(matcher);
	}

	public GlobWatcher(Path root, boolean recursive) {
		matcher = new FileMatchGlob(root, recursive);
		watcher.add(matcher);
	}

	/** Recursive from a root. */
	public GlobWatcher(Path root) {
		this(root,true);
	}

	public static final Collection<Path> toPaths(Collection<FileChangeEntry<FileMatchGlob>> poll) {
		if(poll == null) return null;
		Collection<Path> paths = new ArrayList<Path>(poll.size());
		
		for(FileChangeEntry<FileMatchGlob> p:poll) paths.add(p.getPath());
		
		return paths;
	}
	
	public Collection<FileChangeEntry<FileMatchGlob>> poll(){
		return watcher.poll();
	}

	public Collection<Path> pollFiles(){
		return toPaths(watcher.poll());
	}

	public Collection<FileChangeEntry<FileMatchGlob>> takeBatch(long burstDelay) {
		return watcher.takeBatch(burstDelay);
	}
	
	public Collection<Path> takeBatchFiles(long burstDelay) {
		return toPaths(watcher.takeBatch(burstDelay));
	}

	public Collection<FileChangeEntry<FileMatchGlob>> poll(long timeout, TimeUnit unit) throws InterruptedException {
		return watcher.poll(timeout, unit);
	}
	
	public Collection<Path> pollFiles(long timeout, TimeUnit unit) throws InterruptedException {
		return toPaths(watcher.poll(timeout, unit));
	}

	public Collection<FileChangeEntry<FileMatchGlob>> take() throws InterruptedException {
		return watcher.take();
	}
	
	public Collection<Path> takeFiles() throws InterruptedException {
		return toPaths(watcher.take());
	}

	public Collection<FileChangeEntry<FileMatchGlob>> takeOrNull() {
		return watcher.takeOrNull();
	}
	
	public Collection<Path> takeOrNullFiles() {
		return toPaths(watcher.takeOrNull());
	}

	public Collection<FileChangeEntry<FileMatchGlob>> getMatched() {
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

	public FileMatchGlob includes(Collection<String> globs) {
		return matcher.includes(globs);
	}

	public FileMatchGlob includes(String... globs) {
		return matcher.includes(globs);
	}

	public FileMatchGlob excludes(Collection<String> globs) {
		return matcher.excludes(globs);
	}

	public FileMatchGlob excludes(String... globs) {
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

	public boolean isRecursive() {
		return matcher.isRecursive();
	}
	

}
