#Introduction
Utility for watching file changes using Java 7 WatchService. Written to allow myself working with
file matching and file watching that feels more natural to me.

You could find it useful as a library, or just read source for examples of using Java 7 WatchService. 

# Command line Usage

[Main.java](src/main/java/hr/hrg/javawatcher/Main.java) is a general purpose tool for direct command line usage.
To use it build project with maven and use the shaded jar from target folder.

Without parameter it will display help information 

```
> java -jar java-watcher-0.2.0-SNAPSHOT-shaded.jar 

Usage: folder script [arguments]
 --burstDelay=x    - number of miliseconds to wait before sending changes 
                     (some programs may generate more than one chenge event in very short time when writing a file) 
 --include=pattern - can be used multiple times, defines an include pattern
 --include=pattern - can be used multiple times, defines an include pattern
 --exclude=pattern - can be used multiple times, defines an include pattern
 Example patterns
 *.txt - all files ending with .txt in root folder
 **.txt - all files ending with .txt in all folders
 nice/*.txt - all files ending with .txt in fodler "nice"
 nice/**.txt - all files ending with .txt in all subfolders of "nice"
 nice/first.txt - exactly that file
```
Example usage [example.bat](example/example.bat) and example script in php to catch the changes [example.php](example/example.php)

```
java -jar java-watcher-0.2.0-SNAPSHOT-shaded.jar testFolder http://localhost/test/example.php --burstDelay=50 --include=**.txt --exclude=**.html --exclude=**.doc
```

# Use in java code

Add maven dependency or download from [maven central](http://repo1.maven.org/maven2/hr/hrg/java-watcher/)

```
<dependency>
	<groupId>hr.hrg</groupId>
	<artifactId>java-watcher</artifactId>
	<version>0.1.0</version>
</dependency>
```

## Simple Compile Example

[SimpleCompileExample.java](src/test/java/hr/hrg/javawatcher/SimpleCompileExample.java)
is an example showing how to compile sass file on change 


```java

// this one is configured for current folder without checking sub-folders (second param is false)
GlobWatcher watcher = new GlobWatcher(Paths.get("./"), false);

// if we do not define include rules, any file found will be accepted by the internal matcher
// and we are interested in .scss files only
watcher.includes("*.scss"); // this rule will match any .scss file directly in root folder

// init with intention to watch the files after that 
watcher.init(true);
// no configuration should happen after the init or it will give unexpected results

while(!Thread.interrupted()){
	
	Collection<FileChangeEntry<FileMatchGlob>> changedFiles = watcher.takeOrNull();
	if(changedFiles == null) break; // interrupted
	
	for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {
		compileSass(changed.getPath());
	}
}

```

## Simple Find Files

[SimpleFindFiles.java](src/test/java/hr/hrg/javawatcher/SimpleFindFiles.java)
is an e xample showing how to find files using {@link FileMatchGlob} by adding few include/exclude rules.

```java

// create matcher on current folder also checking sub-folders
FileMatchGlob matcher = new FileMatchGlob(Paths.get("./"), true);

// if we do not define rules, then any file found will be accepted
// match any .scss file in root folder, and any .scss in subfolders
matcher.includes("*.scss","**/*.scss").excludes(".sass-cache");

FolderWatcher.fillMatcher(matcher);

for(Path path :matcher.getMatched()){
	System.out.println("found: "+path);
}

```

## Complex Compile example

[ComplexCompileExample.java](src/test/java/hr/hrg/javawatcher/ComplexCompileExample.java)
is a more complex example showing :

 - how to compile sass file on change
 - how to compile all main files when an include file changes.
 - how to avoid redundant compilation when burst change occurs 
 _(some editors might triger more than one change event in few miliseconds time)_


```java

// tweak this depending how responsive you want to be, but to still catch some duplicate changes
long burstDelay = 20;

// for collecting files to compile, and to skip duplicates
HashSet<Path> todo = new HashSet<>();

GlobWatcher watcher = new GlobWatcher(Paths.get("./scss"), true);
// "**.scss" matches in all folders and subfolders
watcher.includes("**.scss");

// create matcher on current folder without checking sub-folders for the source scss
FileMatchGlob sourceFiles = new FileMatchGlob(Paths.get("./"), false);
// if we do not define rules, then any file found will be accepted
// match any .scss file in root folder only
sourceFiles.includes("*.scss");

// add the additional matcher to listen for changes too
watcher.add(sourceFiles);

//start watching, no configuration should happen after this as it wil give unexpected results
watcher.init(true);

Collection<FileChangeEntry<FileMatchGlob>> changedFiles = null;

while(!Thread.interrupted()){

	changedFiles = watcher.takeBatch(burstDelay);
	if(changedFiles == null) break; // interrupted
	
	for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {	
		if(changed.getMatcher() == sourceFiles){
			// a source file changed, add only it for recompilation
			todo.add(changed.getPath());						
		}else{
			// if any file in include folder changes, we want to recompile all source scss files
			todo.addAll(sourceFiles.getMatched());						
		}
	}
	
	for(Path path: todo){
		compileSass(path);
	}
}

```

## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).
