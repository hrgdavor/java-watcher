package hr.hrg.javawatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 *  Incomplete example showing how to compile sass file on change.
 *  The compilation step is omitted to make it simple to understand
 *  what this library does.
 * */
public class SimpleExternalScriptExample {

	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public static void main(String[] args) {
		
		if(args.length < 2){
			System.out.println("2 parameters required: pathToWatch commandToRun");
			System.exit(-1);
		}
		
		String pathToWatch = args[0];
		String commandToRun = args[1];
		
		// GlobWatcher is for simple usages
		GlobWatcher watcher = new GlobWatcher(Paths.get(pathToWatch), true);

		// init with intention to watch the files after that
		watcher.init(true);

		long burstChangeWait = 50;

		while(!Thread.interrupted()){

			if(watcher.takeBatch(burstChangeWait) == null) break; // interrupted

			System.out.println(sdf.format(new Date())+" - files changed");
			runScript(commandToRun);

		}
	}

	static void runScript(String command){
		System.out.println("running script: "+command);
		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
