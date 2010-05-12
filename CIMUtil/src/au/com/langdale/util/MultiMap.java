package au.com.langdale.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiMap {
	private Map trace = new HashMap(); 		
	
	protected void putRaw(Object key, Object value) {
		Set traces = (Set) trace.get(key);
		if( traces == null ) {
			traces = new HashSet();
			trace.put(key, traces);
		}
		traces.add(value);
	}

	public Set find(Object key) {
		Set traces = (Set) trace.get(key);
		if( traces == null ) 
			return Collections.EMPTY_SET;

		return traces;
	}
	
	public void remove(Object key, Object value) {
		find(key).remove(value);
	}
	
	public Set keySet() {
		return trace.keySet();
	}
}
