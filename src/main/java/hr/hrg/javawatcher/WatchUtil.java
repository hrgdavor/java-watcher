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

    public static boolean isLinux(){
        String os = System.getProperty("os.name");  
        return os.toLowerCase().indexOf("linux") >= 0;
    }

	public static String join(String string, String  ...params) {
		StringBuffer b = new StringBuffer();
		for(int i=0; i<params.length; i++){
			if(i > 0) b.append(' ');
			b.append(params[i]);
		}
		return b.toString();
	}

}
