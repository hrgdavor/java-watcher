#Usage

Simple example showing how to compile sass file on change

```java
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

```

## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).
