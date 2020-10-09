package hr.hrg.javawatcher;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

import io.methvin.watcher.DirectoryWatcher;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/** 
 * {@link FileMatcher} implementation that uses glob syntax by default. 
 * The rule can be a regex if prefixed with {@code regex:} (see: {@link #makeRule(String)}).<br>
 * The {@link #collectMatched}
 * 
 *  @see <a href="https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob">What is a glob</a>
 *  */
public class FileMatchGlob<T> implements FileMatcher<T>{

	private static final String NOT_COLLECTING_EXCLUDED = "This matcher is not collecting excluded files. Call setCollectExcluded(true) or override this method if you do not want exception thrown. ";

	private static final String NOT_COLLECTING_MATCHES = "This matcher is not collecting matches. Call setCollectMatched(true) or override this method if you do not want exception thrown. ";

	//static final Logger log = LoggerFactory.getLogger(FileMatchGlob.class);

    protected String rootString;
    protected boolean recursive;
    protected DirectoryWatcher watcher;
	ArrayBlockingQueue<FileChangeEntry<T>> q = new ArrayBlockingQueue<>(4098);
    
	protected List<PathMatcher> includes = new ArrayList<>();
	protected List<PathMatcher> excludes = new ArrayList<>();

	protected volatile boolean started = false;
	protected Path rootPath;
	protected Path rootPathA;

	/** Default: TRUE. If this matcher will collect matched Files.  */
	protected boolean collectMatched = true;
	protected Set<Path> matched = new TreeSet<>();

	/** Default: FALSE. If this matcher will collect unmatched Files.  */
	protected boolean collectExcluded = false;
	protected Set<Path> excluded = new TreeSet<>();

	protected T context; 

	public FileMatchGlob(Path root, boolean recursive){
		this(root, null, recursive);
	}
	
	public FileMatchGlob(Path root, T context, boolean recursive){
		this.context = context;
		this.rootPath = root.normalize();
		rootPathA = rootPath.toAbsolutePath().normalize();
		//rootPath = rootPath.toAbsolutePath().normalize();
		this.recursive = recursive;
		this.rootString = rootPath.toString().replace('\\', '/');
	}

	/**
	 * Generate PathMatcher based on the rule. If the rule starts with {@code regex:} then it is used unchanged.
	 * The default is the glob syntax, and in that case prefix {@code glob:}+{@code root}+{@code /} is added so the glob
	 * works as expected. The default glob syntax is relative to root of the matcher, but {@code regex} must assume the 
	 * full path to the file. You can however add root to the {@code regex} where desired yourself. 
	 * 
	 * */
	public PathMatcher makeRule(String rule){
		if(rule.startsWith("regex:")) return FileSystems.getDefault().getPathMatcher(rule);
		return FileSystems.getDefault().getPathMatcher("glob:"+rule);
	}

	public FileMatchGlob<T> includes(Collection<String> globs){
		for (String glob : globs) {
			includes.add(makeRule(glob));
		}
		return this;
	}

	public FileMatchGlob<T> includes(String ... globs){
		for (String glob : globs) {
			includes.add(makeRule(glob));
		}
		return this;
	}

	public FileMatchGlob<T> excludes(Collection<String> globs){
		for (String glob : globs) {
			excludes.add(makeRule(glob));
		}
		return this;
	}
	
	public FileMatchGlob<T> excludes(String ... globs){
		for (String glob : globs) {
			excludes.add(makeRule(glob));
		}
		return this;
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
	
	@Override
	public T getContext() {
		return context;
	}
	
	/** 
	 * {@inheritDoc}
	 */
	@Override
	public Path relativize(Path path){
		if(!path.isAbsolute()) path = path.toAbsolutePath();
		return rootPathA.relativize(path);
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
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
	 * You must {@link #setCollectExcluded(boolean)} during initialisation, or the list will be empty.
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
	 * {@inheritDoc}
	 */
	@Override
	public void setCollectMatched(boolean collectMatched) {
		this.collectMatched = collectMatched;
	}
	
	// -------------------------- implements FolderGlob ------- interface --------------------------
	
	/** {@inheritDoc} */
	@Override
	public boolean isMatch(Path path){
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
	public boolean isExcluded(Path path){
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
        if (isMatch(file)) {
            if(collectMatched) matched.add(file);
            return true;
        } else{
        	if(collectExcluded) excluded.add(file);
        	return false;
        }
	}
	
	/** {@inheritDoc} */
	@Override
	public DirectoryWatcher getWatcher() {
		return watcher;
	}
	
	/** {@inheritDoc} */
	@Override
	public void setWatcher(DirectoryWatcher watcher) {
		this.watcher = watcher;
	}
	
	/** {@inheritDoc} */
	@Override
	public ArrayBlockingQueue<FileChangeEntry<T>> getQ() {
		return q;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "FileMatchGlob:"+rootString;
	}

	public Path getRootPathAbs() {
		return rootPathA;
	}	
}
