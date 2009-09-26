/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.util;

import org.eclipse.swt.widgets.Display;
/**
 * A utility to repeatedly run a method on the UI thread.
 * Subclasses implement this method, called action().
 */
public abstract class Tickler implements Runnable {
	public static final int DEFAULT_PERIOD = 1000;
	private int period = DEFAULT_PERIOD;
	private boolean running;
	/**
	 * The first call to start schedules first execution.
	 */
	public synchronized void start() {
		if( running )
			return;
		
		running = true;
		Display.getCurrent().asyncExec(this);
	}
	/**
	 * Prevent further execution.
	 */
	public synchronized void stop() {
		running = false;
	}
	/**
	 * @return: the period between executions in ms.
	 */
	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}
	/**
	 * @return: true if execution is scheduled.
	 */
	public boolean isRunning() {
		return running;
	}
	/**
	 * This method will be periodically executed.
	 */
	protected abstract void action();
	/**
	 * Interface to the swt scheduler.
	 */
	public void run() {
		if(running)
			action();
		if( running )
			Display.getCurrent().timerExec(period, this);
	}
}