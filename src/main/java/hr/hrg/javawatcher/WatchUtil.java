package hr.hrg.javawatcher;

public class WatchUtil {

	public static final boolean classAvailable(String name){
		try {
			Class.forName(name);
			return true;
		} catch (ClassNotFoundException e) {
			// ignore
		}
		return false;
	}
}
