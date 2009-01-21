/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
/**
 * Intrusive profiler.   
 */
public class Profiler {
	private static final double MS = 1.0e-6;
	private static final int CALIBRATION_SAMPLE = 1000;
	
	/**
	 * Represents a single time measurement. 
	 */
	public static class TimeSpan {
		private String name;
		private long t1, t2;
		
		/**
		 * Begin timing.
		 * @param name: designates what is being timed.
		 */
		public TimeSpan(String name) {
			this.name = name;
			spans.add(this);
			t1 = t2 = System.nanoTime();
		}
		/**
		 * Stop timing and complete this mea.  
		 */
		public void stop() {
			t2 = System.nanoTime();
		}
		/**
		 * Start a new measurement, stopping the present measurement.
		 * @param name: designates what is being timed.
		 * @return: the new measurement, timing in progress
		 */
		public TimeSpan start(String name) {
			stop();
			return new TimeSpan(name);
		}
		/**
		 * @return: the measurment in ms.
		 */
		public double getDuration() {
			return (t2 - t1)* MS;
		}
		
		@Override
		public String toString() {
			return name + ": " + getDuration();
		}
	}
	
	private static List spans = new LinkedList();
	private static TimeSpan current = null;
	private static long res;
	/**
	 * Start or restart the global timer.
	 * @param name
	 */
	public static void start(String name) {
		if( current != null) 
			current.stop();
		current = new TimeSpan(name);
	}
	/**
	 * Stop the global timer.
	 */
	public static void stop() {
		if( current != null) 
			current.stop();
		current = null;
	}
	
	public static MultiMap summarize() {
		if( current != null) 
			current.stop();

		MultiMap map = new MultiMap();
		for (Iterator it = spans.iterator(); it.hasNext();) {
			TimeSpan span = (TimeSpan) it.next();
			if(span.t2 != span.t1)
				map.putRaw(span.name, span);
		}

		spans = new LinkedList(); // reset
		return map;
	}
	
	/**
	 * Print all completed measurements and delete all measurements.
	 */
	public static void print() {
		MultiMap summary = summarize();
		
		System.out.println("=====================================================");
		System.out.println("Task: Total (ms), Count, Ave (ms), Min (ms), Max (ms)");
		
		Object[] keys = summary.keySet().toArray();
		Arrays.sort(keys);
		
		for (int ix = 0; ix < keys.length; ix++) {
			
			long total = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
			int count = 0;
			
			Set parts = summary.find(keys[ix]);
			for (Iterator it = parts.iterator(); it.hasNext();) {
				TimeSpan span = (TimeSpan) it.next();
				long dur = span.t2 -span.t1;
				min = dur < min? dur: min;
				max = dur > max? dur: max;
				total += dur;
				count += 1;
			}
			
			System.out.println( 
					keys[ix] + ": " 
					+ total*MS + ", " 
					+ count + ", " 
					+ total*MS/count + ", "  
					+ min*MS + ", " 
					+ max*MS);
		}
	}

	
	public static void calibrate() {
		res = 0;
		for(int ix = 0; ix < CALIBRATION_SAMPLE; ix++) {
			long t1 = System.nanoTime();
			long t2;
			for(;;) {
				t2 = System.nanoTime();
				if(t1 !=t2)
					break;
			}
			res += t2 - t1;
		}
		System.out.println("Profile resolution (ns): " + (res + 0.5)/CALIBRATION_SAMPLE );
	}
}
