package org.hrg.javawatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.hrg.javawatcher.FileChangeEntry;
import org.hrg.javawatcher.FileMatchGlob;
import org.hrg.javawatcher.FolderWatcher;

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
		long threadInterruptCheckInterval = 1000;

		// for collecting files to compile, and to skip duplicates
		HashSet<Path> todo = new HashSet<>();
		
		FolderWatcher<FileMatchGlob> folderWatcher = new FolderWatcher<>();

		// create matcher on current folder without checking sub-folders
		FileMatchGlob sourceFiles = new FileMatchGlob(Paths.get("./"), false);
		
		// if we do not define rules, then any file found will be accepted
		// match any .scss file in root folder
		sourceFiles.includes("*.scss");
		
		// we want to know which files are fouond later on when an include changes
		sourceFiles.setCollectMatched(true);

		// create matcher on scss folder also checking sub-folders
		FileMatchGlob includeFiles = new FileMatchGlob(Paths.get("./scss"), true);
		// "*.scss" only matches root folder, so extra rule is needed for subfolders
		includeFiles.includes("*.scss","**/*.scss");
		// I have not yet found an easy way to say this in single rule (will change example if I find a better way)
		
		folderWatcher.add(sourceFiles);
		folderWatcher.add(includeFiles);
		
		//start watching, no configuration should happen after this as it wil give unexpected results
		folderWatcher.init(true);
		
		Collection<FileChangeEntry<FileMatchGlob>> changedFiles = null;

		while(!Thread.interrupted()){
			
			if(changedFiles == null && !todo.isEmpty()){
				// when poll(threadInterruptCheckInterval) returns some results
				// and one of subsequent poll(burstChangeWait) returns null (no new burst changes) 
				for(Path path: todo){
					compileSass(path);
				}
				todo.clear();
			}else{
				for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {

					if(changed.getMatcher() == includeFiles){
						// if any file in include folder changes, we want to recompile all source sass files
						todo.addAll(sourceFiles.getMatched());						
					}else{
						// a source file changed, add it for recompilation
						todo.add(changed.getPath());						
					}
				}
			}
			
			changedFiles = folderWatcher.poll( changedFiles == null ?  
					threadInterruptCheckInterval : burstChangeWait, TimeUnit.MILLISECONDS);
		}
	}

	static void compileSass(Path sassFile){
		System.out.println("compile: "+sassFile);
		// implement compilation here
	}
}
