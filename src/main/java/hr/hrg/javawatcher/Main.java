package hr.hrg.javawatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public static void main(String[] args) throws Exception{

		if(args.length < 2) {
			printHelp();
		}
		
		String pathToWatch = args[0];
		String commandToRun = args[1];
		boolean postChanges = false;
		Logger log = LoggerFactory.getLogger(Main.class);

		GlobWatcher watcher = new GlobWatcher(Paths.get(pathToWatch));
		long burstDelay = 50;
		
		
		for(int i=2; i<args.length; i++) {
			
			if(args[i].startsWith("--include=")) {
				watcher.includes(args[i].substring(10));
			
			}else if(args[i].startsWith("--burstDelay=")) {
				burstDelay = Long.valueOf(args[i].substring(13));
				
			}else if(args[i].startsWith("--exclude=")) {
				watcher.excludes(args[i].substring(10));
			}else if(args[i].equals("--postChanges")) {
				postChanges = true;
			}
		}

		watcher.init(true);
		

		while(!Thread.interrupted()){

			Collection<Path> changed = watcher.takeBatchFilesUnique(burstDelay);
			if(changed == null) break; // interrupted

			System.out.println(sdf.format(new Date())+" - "+changed.size()+" files changed");
			runScript(log,commandToRun, null, changed, postChanges, System.out, System.err);
		}
		
	}

	public static void runHttp(Logger log, String command, Collection<Path> changed, boolean postChanges, PrintStream out) throws Exception{
		log.info("sending changes to url: "+command);
		
		byte[] bytes = bytesToWrite(changed);
		
		HttpURLConnection conn = (HttpURLConnection) new URL(command).openConnection();
		if(postChanges) {
			conn.setDoOutput(true);
			conn.setRequestMethod( "POST" );
			
			conn.setRequestProperty( "charset", "utf-8");			
			conn.setRequestProperty( "Content-Length", Integer.toString( bytes.length ));			
			conn.setUseCaches( false );
			conn.getOutputStream().write(bytes);			
		}
		int responseCode = conn.getResponseCode();
		out.println("response code:"+responseCode);
		InputStream inputStream = conn.getInputStream();
		pipeStream(inputStream,out);
		conn.disconnect();
	}

	private static byte[] bytesToWrite(Collection<Path> changed) {
		StringBuilder b = new StringBuilder();
		for(Path p:changed) b.append(p.toAbsolutePath().toString()).append("\n");
		byte[] bytes = b.toString().getBytes();
		return bytes;
	}

	private static void pipeStream(InputStream inputStream, PrintStream out) throws Exception{
		byte[] buf = new byte[4096];
		int len;
		while((len = inputStream.read(buf)) != -1) {
			out.write(buf, 0, len);
		}
		out.println();
		
	}

	public static void runScript(Logger log, String command, String[] params, Collection<Path> changed, boolean postChanges, PrintStream out, final PrintStream err) throws Exception{
		if(command.startsWith("http://")){
				runHttp(log, command, changed, postChanges, out);
		}else {
			log.info("running script: "+command);
			try {
				String[] cmdArray = null;
				if(params != null && params.length >0) {
					cmdArray = new String[params.length+1];
					cmdArray[0] = command;
					System.arraycopy(params, 0, cmdArray, 1, params.length);
				}else {
					cmdArray = new String[]{command};
				}
				final Process process = Runtime.getRuntime().exec(cmdArray);
				if(postChanges) {
					process.getOutputStream().write(bytesToWrite(changed));
					process.getOutputStream().close();
				}
				new Thread(new Runnable() {
					public void run() {						
						try {
							pipeStream(process.getErrorStream(), err);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();

				pipeStream(process.getInputStream(), out);
				log.info("done running script: "+command);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void printHelp() {
		System.out.println("Usage: folder script [arguments]");
		System.out.println(" --burstDelay=x    - number of miliseconds to wait before sending changes ");
		System.out.println("                     (some programs may generate more than one chenge event in very short time when writing a file) ");
		System.out.println(" --postChanges     - write changed files info to the script/url (script input stream or HTTP POST for url) ");
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
	

	public static String quotedJsonString(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char         c = 0;
        int          i;
        int          len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String       t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\');
                sb.append(c);
                break;
            case '/':
//                if (b == '<') {
                    sb.append('\\');
//                }
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
               sb.append("\\r");
               break;
            default:
                if (c < ' ') {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u" + t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }	

}
