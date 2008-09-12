/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

public class ProfileMergeViewerCreator  implements IViewerCreator {

	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		return new ProfileMergeViewer(parent, config);
	}

}
