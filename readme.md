#Introduction
Utility for watching file changes using Java 7 WatchService. Written to allow myself working with
file matching and file watching that feels more natural to me.

You could find it useful as a library, or just read source for examples of using Java 7 WatchService. 

#Usage

Add maven dependency or download from [maven central](http://repo1.maven.org/maven2/hr/hrg/java-watcher/)

```
<dependency>
	<groupId>hr.hrg</groupId>
	<artifactId>java-watcher</artifactId>
	<version>0.1.0</version>
</dependency>
```

[SimpleCompileExample.java](src/test/java/hr/hrg/javawatcher/SimpleCompileExample.java)
is an example showing how to compile sass file on change 


```java

// FolderWatcher is the utility that uses the WatchService but needs at least one FileMatcher
FolderWatcher<FileMatchGlob> folderWatcher = new FolderWatcher<>();

// in our case FileMatchGlob instance is used, and uses glob syntax
// this one is configured for current folder without checking sub-folders (second param is false)
FileMatchGlob matcher = folderWatcher.add(new FileMatchGlob(Paths.get("./"), false));

// if we do not define include rules, any file found will be accepted by the matcher
// and we are interested in .scss files only
matcher.includes("*.scss"); // this rule will match any .scss file directly in root folder

// init with intention to watch the files after that
folderWatcher.init(true);
// no configuration should happen after the init or it will give unexpected results

while(!Thread.interrupted()){
	Collection<FileChangeEntry<FileMatchGlob>> changedFiles = folderWatcher.take();
	
	for (FileChangeEntry<FileMatchGlob> changed : changedFiles) {
		compileSass(changed.getPath());
	}
}

```

[SimpleFindFiles.java](src/test/java/hr/hrg/javawatcher/SimpleFindFiles.java)
is an e xample showing how to find files using {@link FileMatchGlob} by adding few include/exclude rules.

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

[ComplexCompileExample.java](src/test/java/hr/hrg/javawatcher/ComplexCompileExample.java)
is a more complex example showing :

 - how to compile sass file on change
 - how to compile all main files when an include file changes.
 - how to avoid redundant compilation when burst change occurs 
 _(some editors might triger more than one change event in few miliseconds time)_



## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).
