/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
/**
 * Intrusive profiler.   
 */
public class Profiler {
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
			return (t2 - t1)* 1.0e-6;
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
	/**
	 * Print all completed measurements and delete all measurements.
	 */
	public static void print() {
		if( current != null) 
			current.stop();
		
		System.out.println("===================");
		System.out.println("Task: Duration (ms)");
		for (Iterator it = spans.iterator(); it.hasNext();) {
			TimeSpan span = (TimeSpan) it.next();
			if(span.t2 != span.t1)
				System.out.println(span);
		}
		spans = new LinkedList(); // reset
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
