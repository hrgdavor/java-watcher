package org.hrg.javawatcher;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
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

	private static final String NOT_COLLECTING_EXCLUDED = "This matcher is not collecting excluded files. Override this method if you do not want exception thrown. ";

	private static final String NOT_COLLECTING_MATCHES = "This matcher is not collecting matches. Override this method if you do not want exception thrown. ";

	static final Logger log = LoggerFactory.getLogger(FileMatchGlob.class);

    protected String rootString;
    protected boolean recursive;
	protected List<PathMatcher> includes = new ArrayList<>();
	protected List<PathMatcher> excludes = new ArrayList<>();

	protected volatile boolean started = false;
	protected Path rootPath;

	protected boolean collectMatched = false;
	protected Set<Path> matched = new TreeSet<>(); 
	protected boolean collectExcluded = false;
	protected Set<Path> excluded = new TreeSet<>(); 

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
	 * You must {@link #setCollectMatched(boolean)} during initialisation, or the list will be empty.
	 * */
	public Collection<Path> getMatched(){
		if(!collectMatched) throw new RuntimeException(NOT_COLLECTING_MATCHES+toString());
		return matched;
	}

	public int getMatchedCount(){
		if(!collectMatched) throw new RuntimeException(NOT_COLLECTING_MATCHES+toString());
		return matched.size();
	}

	/**
	 * Get the current collection of files offered but excluded based on the rules.
	 * You must {@link #setCollectMatched(boolean)} during initialisation, or the list will be empty.
	 * */
	public Collection<Path> getExcluded(){
		if(!collectExcluded) throw new RuntimeException(NOT_COLLECTING_EXCLUDED+toString());
		return excluded;
	}
	
	public int getExcludedCount(){
		if(!collectExcluded) throw new RuntimeException(NOT_COLLECTING_EXCLUDED+toString());
		return excluded.size();
	}

	public boolean isCollectExcluded() {
		return collectExcluded;
	}
	
	public boolean isCollectMatched() {
		return collectMatched;
	}

	/**
	 * Are excluded files for later listing. Use when you want to know what files were excluded.
	 * */
	public void setCollectExcluded(boolean collectExcluded) {
		this.collectExcluded = collectExcluded;
	}
	
	/**
	 * Are matched files for later listing. Use when you want to know what were collected.
	 * */
	public void setCollectMatched(boolean collectMatched) {
		this.collectMatched = collectMatched;
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
		excluded.remove(path);
	}
	
	/** {@inheritDoc} */
	@Override
	public void dirInvalid(Path path){
		removeAllFromDir(path, matched);
		removeAllFromDir(path, excluded);
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
            if(collectMatched) matched.add(file);
            return true;
        } else{
        	if(collectExcluded) excluded.add(file);
        	return false;
        }
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "FileMatchGlob:"+rootString;
	}	
}
