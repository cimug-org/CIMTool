package au.com.langdale.util;

public class TextUtil {
	static int wrap(StringBuilder text, int cursor, int width) {
		cursor = text.indexOf(" ", cursor + width);
		if( cursor != -1) {
			text.replace(cursor, cursor + 1, "\n");
			return cursor + 1;
		}
		else
			return text.length();
	}
	
	public static String wrap( String s, int width) {
		StringBuilder text = new StringBuilder(s);
		int cursor = 0;
		while( cursor < text.length()) {
			cursor = wrap(text, cursor, width);
		}
		return text.toString();
	}
}
