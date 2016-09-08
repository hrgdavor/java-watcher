package org.hrg.javawatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.hrg.javawatcher.FileChangeEntry;
import org.hrg.javawatcher.FileMatchGlob;
import org.hrg.javawatcher.FolderWatcher;

/**
 *  Incomplete example showing how to compile sass file on change.
 *  The compilation step is omitted to make it simple to understand
 *  what this library does.
 * */
public class SimpleCompileExample {

	public static void main(String[] args) {
		
		FolderWatcher<FileMatchGlob> folderWatcher = new FolderWatcher<>();
		// create matcher on current folder without checking sub-folders
		FileMatchGlob matcher = new FileMatchGlob(Paths.get("./"), false);

		// if we do not define rules, then any file found will be accepted
		// match any .scss file in root folder
		matcher.addIncludes("*.scss");

		folderWatcher.add(matcher);
		
		//start watching, no configuration should happen after this as it wil give unexpected results
		folderWatcher.init(true);
		
		while(!Thread.interrupted()){
			Collection<FileChangeEntry<FileMatchGlob>> changedFiles = folderWatcher.take();
			
			for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {
				compileSass(changed.getPath());
			}
		}
	}

	static void compileSass(Path sassFile){
		System.out.println("compile: "+sassFile);
		// implement compilation here
	}
}
