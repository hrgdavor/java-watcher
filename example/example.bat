
rem you can put java isntallation in folder above this one (if you do not have permission to install java)
set java_exe="%~dp0\\..\\java\\bin\\java.exe"

rem installed java will be used otherwise (you must in that case install java)
if not exist %java_exe% set java_exe=java

%java_exe% -jar java-watcher-0.2.0-SNAPSHOT-shaded.jar testFolder http://localhost/test/example.php --burstDelay=50 --include=**.txt --exclude=**.html --exclude=**.doc