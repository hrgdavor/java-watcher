#Usage

Simple example showing how to compile sass file on change

```java

FolderWatcher<FileMatchGlob> folderWatcher = new FolderWatcher<>();
// create matcher on current folder without checking sub-folders
FileMatchGlob matcher = new FileMatchGlob(Paths.get("./"), false);

// if we do not define rules, then any file found will be accepted
// match any .scss file in root folder
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


```



## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).
