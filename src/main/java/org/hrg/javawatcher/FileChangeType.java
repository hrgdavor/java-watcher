package org.hrg.javawatcher;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;

public enum FileChangeType {
	CREATE,DELETE,MODIFY;

	public static FileChangeType fromKind(Kind<?> kind) {
		if(kind == StandardWatchEventKinds.ENTRY_MODIFY) return MODIFY;
		if(kind == StandardWatchEventKinds.ENTRY_CREATE) return CREATE;
		if(kind == StandardWatchEventKinds.ENTRY_DELETE) return DELETE;
		return null;
	}
}
