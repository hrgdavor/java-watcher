package org.hrg.javawatcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link FileMatcher} implementation that uses glob syntax.
 * 
 *  @see {@link <a href="https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob">What is a glob</a>}
 *  */
public class FileMatchGlob implements FileMatcher{

	static final Logger log = LoggerFactory.getLogger(FileMatchGlob.class);

    protected String rootString;
    protected boolean recursive;
	protected List<PathMatcher> includes = new ArrayList<>();
	protected List<PathMatcher> excludes = new ArrayList<>();

	protected volatile boolean started = false;
	protected Path rootPath;

	protected Set<Path> matched = new TreeSet<>(); 
	protected Set<Path> skipped = new TreeSet<>(); 

	public FileMatchGlob(Path root, boolean recursive){
		this.rootPath = root;
		this.recursive = recursive;
		this.rootString = rootPath.toString().replace('\\', '/');
	}

	public void addIncludes(Collection<String> globs){
		for (String glob : globs) {
			includes.add(FileSystems.getDefault().getPathMatcher("glob:"+rootString+"/"+glob));
		}
	}

	public void addIncludes(String ... globs){
		for (String glob : globs) {
			includes.add(FileSystems.getDefault().getPathMatcher("glob:"+rootString+"/"+glob));
		}
	}

	public void addExcludes(Collection<String> globs){
		for (String glob : globs) {
			excludes.add(FileSystems.getDefault().getPathMatcher("glob:"+rootString+"/"+glob));
		}
	}
	
	public void addExcludes(String ... globs){
		for (String glob : globs) {
			excludes.add(FileSystems.getDefault().getPathMatcher("glob:"+rootString+"/"+glob));
		}
	}

	/**
	 * Remove all Paths that are in the specified directory.
	 * */
	public static final void removeAllFromDir(Path path, Collection<Path> collection){
		Iterator<Path> iterator = collection.iterator();
		Path p = null;
		while(iterator.hasNext()){
			p = iterator.next();
			if(path.equals(p.getParent())) iterator.remove();
		}		
	}
	
	public List<PathMatcher> getExcludes() {
		return excludes;
	}
	
	public List<PathMatcher> getIncludes() {
		return includes;
	}

	/**
	 * Get the current collection of files offered and accepted based on the rules.
	 * */
	public Collection<Path> getMatched(){
		return matched;
	}

	public int getMatchedCount(){
		return matched.size();
	}

	/**
	 * Get the current collection of files offered but excluded based on the rules.
	 * */
	public Collection<Path> getSkipped(){
		return matched;
	}
	
	public int getSkippedCount(){
		return matched.size();
	}

	Object fillLock = new Object();
	public void fill(){
		synchronized (fillLock) {
			try {
				Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						offer(file);
						return FileVisitResult.CONTINUE;
					}
					
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return (recursive && !excluded(dir)) || dir.equals(rootPath) ? 
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
		
	}
	
	// -------------------------- implements FolderGlob ------- interface --------------------------
	
	/** {@inheritDoc} */
	@Override
	public boolean matches(Path path){
		if(includes.size() >0){
			boolean included = false;
			for (PathMatcher inc : includes) {
				if(inc.matches(path)){
					included = true;
					break;
				}
			}
			if(!included) return false;
		}
		for (PathMatcher ex : excludes) {
			if(ex.matches(path)) return false;
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean excluded(Path path){
		for (PathMatcher ex : excludes) {
			if(ex.matches(path)) return true;
		}
		return false;
	}
	
	/** {@inheritDoc} */
	@Override
	public void fileDeleted(Path path){
		matched.remove(path);
		skipped.remove(path);
	}
	
	/** {@inheritDoc} */
	@Override
	public void dirInvalid(Path path){
		removeAllFromDir(path, matched);
		removeAllFromDir(path, skipped);
	}
	
	/** {@inheritDoc} */
	@Override
	public Path getRootPath() {
		return rootPath;
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isRecursive() {
		return recursive;
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean offer(Path file) {
        if (matches(file)) {
            matched.add(file);
            return true;
        } else{
        	skipped.add(file);
        	return false;
        }
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "FileMatchGlob";
	}	
}
