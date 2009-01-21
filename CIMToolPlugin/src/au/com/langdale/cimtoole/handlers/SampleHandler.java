/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public SampleHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IViewDescriptor[] views = PlatformUI.getWorkbench().getViewRegistry().getViews();
		for( int ix = 0; ix < views.length; ix++)
			System.out.println(views[ix].getLabel() 
					+ " - " + views[ix].getId()
					+ " - " + views[ix].getDescription());

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		try {
			window.getActivePage().showView("org.eclipse.ui.views.ContentOutline");
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		MessageDialog.openInformation(
				window.getShell(),
				"CIMTool Plug-in",
				"Hello, Eclipse world");
		return null;
	}
}
