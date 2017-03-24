package hr.hrg.javawatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 *  Incomplete example showing how to compile sass file on change.
 *  The compilation step is omitted to make it simple to understand
 *  what this library does.
 * */
public class SimpleCompileExample {

	public static void main(String[] args) {
		
		// GlobWatcher implements AutoCloseable. Use it in try-with-resources or call .close() manually 
		try( GlobWatcher watcher = new GlobWatcher(Paths.get("./"), false) ){
			// this one is configured for current folder without checking sub-folders (second param is false)
	
			// if we do not define include rules, any file found will be accepted by the internal matcher
			// and we are interested in .scss files only
			watcher.includes("*.scss"); // this rule will match any .scss file directly in root folder
	
			// init with intention to watch the files after that 
			watcher.init(true);
			// no configuration should happen after the init or it will give unexpected results
	
			// after init you can request all files that were found and for example recompile
			// everything before going to watch mode
			// Collection<Path> matchedFiles = watcher.getMatchedFiles();
			
			while(!Thread.interrupted()){
				
				Collection<FileChangeEntry<FileMatchGlob>> changedFiles = watcher.takeOrNull();
				if(changedFiles == null) break; // interrupted
				
				for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {
					compileSass(changed.getPath());
				}
			}
		}
	}

	static void compileSass(Path sassFile){
		System.out.println("compile: "+sassFile);
		// implement compilation here
	}
}
