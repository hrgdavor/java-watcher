package hr.hrg.javawatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;

/**<p>
 *  More complex example showing :
 *  <ul>
 *  	<li>how to compile sass file on change
 *  	<li>how to compile all main files when an include file changes.
 *  	<li>how to avoid redundant compilation when burst change occurs <br>
 *  		<i>(some editors might triger more than one change event in few miliseconds time)</i>
 *  </ul>
 * </p><p>
 *  The compilation step is omitted to make it simple to understand
 *  what this library does.
 *  </p>
 *  
 * */
public class ComplexCompileExample {

	public static void main(String[] args) {

		// tweak this depending how responsive you want to be, but to still catch some duplicate changes
		long burstChangeWait = 20;

		// for collecting files to compile, and to skip duplicates
		HashSet<Path> todo = new HashSet<>();
		
		FolderWatcher<FileMatchGlob> folderWatcher = new FolderWatcher<>();

		// create matcher on current folder without checking sub-folders
		FileMatchGlob sourceFiles = new FileMatchGlob(Paths.get("./"), false);
		
		// if we do not define rules, then any file found will be accepted
		// match any .scss file in root folder only
		sourceFiles.includes("*.scss");

		// create matcher for our folder that holds the include-files (recursive check sub-folders as well)
		FileMatchGlob includeFiles = new FileMatchGlob(Paths.get("./scss"), true);
		// "**.scss" matches in all folders and subfolders
		includeFiles.includes("**.scss");
		
		
		folderWatcher.add(sourceFiles);
		folderWatcher.add(includeFiles);
		
		//start watching, no configuration should happen after this as it wil give unexpected results
		folderWatcher.init(true);
		
		Collection<FileChangeEntry<FileMatchGlob>> changedFiles = null;
		
		while(!Thread.interrupted()){

			changedFiles = folderWatcher.takeBatch(burstChangeWait);
			if(changedFiles == null) break; // interrupted
			
			for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {	
				if(changed.getMatcher() == includeFiles){
					// if any file in include folder changes, we want to recompile all source scss files
					todo.addAll(sourceFiles.getMatched());						
				}else{
					// a source file changed, add only it for recompilation
					todo.add(changed.getPath());						
				}
			}
			
			for(Path path: todo){
				compileSass(path);
			}
		}
	}

	static void compileSass(Path sassFile){
		System.out.println("compile: "+sassFile);
		// implement compilation here
	}
}
