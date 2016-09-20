#Introduction
Utility for watching file changes using Java 7 WatchService. Written to allow myself working with
file matching and file watching that feels more natural to me.

You could find it useful as a library, or just read source for examples of using Java 7 WatchService. 

#Usage

Simple example showing how to compile sass file on change [SimpleCompileExample](src/test/java/SimpleCompileExample.java)

```java

FolderWatcher<FileMatchGlob> folderWatcher = new FolderWatcher<>();
// create matcher on current folder without checking sub-folders
FileMatchGlob matcher = new FileMatchGlob(Paths.get("./"), false);

// if we do not define rules, then any file found will be accepted
// match any .scss file in root folder, and any .scss in subfolders
matcher.includes("*.scss");

folderWatcher.add(matcher);

//start watching, no configuration should happen after this as it wil give unexpected results
folderWatcher.init(true);

while(!Thread.interrupted()){
	Collection<FileChangeEntry<FileMatchGlob>> changedFiles = folderWatcher.take();
	
	for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {
		compileSass(changed.getPath());
	}
}

```

Example showing how to find files using {@link FileMatchGlob} by adding few include/exclude rules

```java

// create matcher on current folder also checking sub-folders
FileMatchGlob matcher = new FileMatchGlob(Paths.get("./"), true);

// if we do not define rules, then any file found will be accepted
// match any .scss file in root folder
matcher.includes("*.scss","**/*.scss").excludes(".sass-cache");

// by default matcher does not store matched or excluded files 
matcher.setCollectMatched(true);

FolderWatcher.fillMatcher(matcher);

for(Path path :matcher.getMatched()){
	System.out.println("found: "+path);
}

```

More complex example showing :

 - how to compile sass file on change
 - how to compile all main files when an include file changes.
 - how to avoid redundant compilation when burst change occurs 
 _(some editors might triger more than one change event in few miliseconds time)_

```java

// tweak this depending how responsive you want to be, but to still catch some duplicate changes
long burstChangeWait = 20;
long threadInterruptCheckInterval = 1000;

// for collecting files to compile, and to skip duplicates
HashSet<Path> todo = new HashSet<>();

FolderWatcher<FileMatchGlob> folderWatcher = new FolderWatcher<>();

// create matcher on current folder without checking sub-folders
FileMatchGlob sourceFiles = new FileMatchGlob(Paths.get("./"), false);

// if we do not define rules, then any file found will be accepted
// match any .scss file in root folder only
sourceFiles.includes("*.scss");

// sourceFiles.getMatched() does not work without this. And it must be set before folderWatcher.init(...)
sourceFiles.setCollectMatched(true);

// create matcher for our folder that holds the include-files (recursive check sub-folders as well)
FileMatchGlob includeFiles = new FileMatchGlob(Paths.get("./scss"), true);
// "**.scss" matches in all folders and subfolders
includeFiles.includes("**.scss");


folderWatcher.add(sourceFiles);
folderWatcher.add(includeFiles);

//start watching, no configuration should happen after this as it wil give unexpected results
folderWatcher.init(true);

Collection<FileChangeEntry<FileMatchGlob>> changedFiles = null;
long pollWait;

while(!Thread.interrupted()){
	
	if(changedFiles == null && !todo.isEmpty()){
		// once poll(threadInterruptCheckInterval) returns some results, we do not compile right away
		// we wait one of subsequent poll(burstChangeWait) to return null (no new burst changes happened)
		for(Path path: todo){
			compileSass(path);
		}
		todo.clear();
	}else{
		for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {

			if(changed.getMatcher() == includeFiles){
				// if any file in include folder changes, we want to recompile all source scss files
				todo.addAll(sourceFiles.getMatched());						
			}else{
				// a source file changed, add only it for recompilation
				todo.add(changed.getPath());						
			}
		}
	}
	
	pollWait = changedFiles == null ?
			// if no files changed wait: threadInterruptCheckInterval to allow the thread to be interrupted
			threadInterruptCheckInterval : 
			// if some files just changed, wait: burstChangeWait to allow for burst changes to be handled in batch 
			burstChangeWait;
			
	changedFiles = folderWatcher.poll(pollWait,	TimeUnit.MILLISECONDS);
}


```



## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).
