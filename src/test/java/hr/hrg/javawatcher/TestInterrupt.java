package hr.hrg.javawatcher;

import java.nio.file.Paths;
import java.util.Collection;

public class TestInterrupt {
	
	
	public static void main(String[] args) throws Exception {
		final FileMatchGlob matcher = new FileMatchGlob(Paths.get("./"), true);
		final FolderWatcher<FileMatcher> w = new FolderWatcher<FileMatcher>();
		w.add(matcher);

		w.init(true);

		Thread thread = new Thread(new Runnable() {
			public void run() {
				System.out.println("Before take");
				Collection<FileChangeEntry<FileMatcher>> files = w.takeOrNull();
				if(files == null) System.out.println("Interrupted and returned null");
				System.out.println("After take");
			}
		});
		thread.start();
		
		Thread.sleep(500);
		System.out.println("interrupt");
		thread.interrupt();
	}
}
