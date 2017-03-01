package hr.hrg.javawatcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class Main {

	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public static void main(String[] args) throws Exception{
		
		if(args.length < 2) {
			printHelp();
		}
		
		
		String pathToWatch = args[0];
		String commandToRun = args[1];
		
		GlobWatcher watcher = new GlobWatcher(Paths.get(pathToWatch));
		long burstDelay = 50;
		
		
		for(int i=2; i<args.length; i++) {
			
			if(args[i].startsWith("--include=")) {
				watcher.includes(args[i].substring(10));
			
			}else if(args[i].startsWith("--burstDelay=")) {
				burstDelay = Long.valueOf(args[i].substring(13));
				
			}else if(args[i].startsWith("--exclude=")) {
				watcher.excludes(args[i].substring(10));
			}
		}
		

		watcher.init(true);
		

		while(!Thread.interrupted()){

			Collection<Path> changed = watcher.takeBatchFilesUnique(burstDelay);
			if(changed == null) break; // interrupted

			System.out.println(sdf.format(new Date())+" - "+changed.size()+" files changed");
			runScript(commandToRun, changed);
		}
		
	}

	static void runScript(String command, Collection<Path> changed) throws Exception{
		if(command.startsWith("http://")){
			System.out.println("sending changes to: "+command);

			
			StringBuilder b = new StringBuilder();
			
			for(Path p:changed) b.append(p.toAbsolutePath().toString()).append("\n");
			byte[] bytes = b.toString().getBytes();
			
			HttpURLConnection conn = (HttpURLConnection) new URL(command).openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod( "POST" );
			
			conn.setRequestProperty( "charset", "utf-8");			
			conn.setRequestProperty( "Content-Length", Integer.toString( bytes.length ));			
			conn.setUseCaches( false );
			conn.getOutputStream().write(bytes);
			int responseCode = conn.getResponseCode();
			System.out.println("response code:"+responseCode);
			InputStream inputStream = conn.getInputStream();
			byte[] buf = new byte[4096];
			int len;
			while((len = inputStream.read(buf)) != -1) {
				System.out.write(buf, 0, len);
			}
			System.out.println();
			conn.disconnect();
			
		}else {			
			System.out.println("running script: "+command);
			try {
				Runtime.getRuntime().exec(command);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void printHelp() {
		System.out.println("Usage: folder script [arguments]");
		System.out.println(" --burstDelay=x    - number of miliseconds to wait before sending changes ");
		System.out.println("                     (some programs may generate more than one chenge event in very short time when writing a file) ");
		System.out.println(" --include=pattern - can be used multiple times, defines an include pattern");
		System.out.println(" --include=pattern - can be used multiple times, defines an include pattern");
		System.out.println(" --exclude=pattern - can be used multiple times, defines an include pattern");
		System.out.println(" Example patterns");
		System.out.println(" *.txt - all files ending with .txt in root folder");
		System.out.println(" **.txt - all files ending with .txt in all folders");
		System.out.println(" nice/*.txt - all files ending with .txt in fodler \"nice\"");
		System.out.println(" nice/**.txt - all files ending with .txt in all subfolders of \"nice\"");
		System.out.println(" nice/first.txt - exactly that file");
		System.exit(0);
	}
	
	
}
