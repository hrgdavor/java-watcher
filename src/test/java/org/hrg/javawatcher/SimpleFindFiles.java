package org.hrg.javawatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.hrg.javawatcher.FileChangeEntry;
import org.hrg.javawatcher.FileMatchGlob;
import org.hrg.javawatcher.FolderWatcher;

/**
 *  Example showing how to find files using {@link FileMatchGlob}
 * */
public class SimpleFindFiles {

	public static void main(String[] args) {
		
		// create matcher on current folder also checking sub-folders
		FileMatchGlob matcher = new FileMatchGlob(Paths.get("./"), true);

		// if we do not define rules, then any file found will be accepted
		// match any .scss file in root folder, and any .scss in subfolders
		matcher.includes("*.scss","**/*.scss").excludes(".sass-cache");

		// by default matcher does not store matched or excluded files 
		matcher.setCollectMatched(true);

		FolderWatcher.fillMatcher(matcher);
		
		for(Path path :matcher.getMatched()){
			System.out.println("found: "+path);
		}
		
	}

}
