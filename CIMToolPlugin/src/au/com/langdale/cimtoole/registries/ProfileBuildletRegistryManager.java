package au.com.langdale.cimtoole.registries;

import org.eclipse.core.runtime.ListenerList;

public class ProfileBuildletRegistryManager {

	/**
	 * ==============================================================================
	 * Builders changed listener related code:
	 * ==============================================================================
	 */
	private static ListenerList listeners = new ListenerList();

	/**
	 * Clients provide this interface to be notified of builder changes.
	 */
	public interface ManageBuildersListener {
		/**
		 * Indicates that the set of available builders has changed. Indicates a refresh
		 * is needed.
		 */
		public void buildersChanged();
	}

	/**
	 * Register to receive builders changed notifications.
	 * 
	 * @param listener: the receiver.
	 */
	public static void addManageBuildersListener(ManageBuildersListener listener) {
		listeners.add(listener);
	}

	/**
	 * Deregister to stop receiving builders changed notifications.
	 * 
	 * @param listener: the receiver.
	 */
	public static void removeManageBuildersListener(ManageBuildersListener listener) {
		listeners.remove(listener);
	}

	public static void fireBuildersChanged() {
		Object[] current = listeners.getListeners();
		for (int ix = 0; ix < current.length; ix++) {
			ManageBuildersListener listener = (ManageBuildersListener) current[ix];
			listener.buildersChanged();
		}
	}

}
