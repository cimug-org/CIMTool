/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;

import au.com.langdale.ui.binding.ResourceUI;

public class ValidationView extends ViewPart {

	private TreeViewer viewer;
	private IEditorRegistry registry;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		ResourceUI.displayWorkspace(viewer, ResourceUI.VALIDATION_FILTER);
		viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		//viewer.addOpenListener(listenerAlt);
		viewer.addSelectionChangedListener(listener);
		getSite().setSelectionProvider(viewer);
		registry = PlatformUI.getWorkbench().getEditorRegistry();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);

	}

//	private IOpenListener listenerAlt = new IOpenListener() {
//
//		public void open(OpenEvent event) {
//			if (event.getSelection() instanceof IStructuredSelection) {
//				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
//				if (selection.getFirstElement() instanceof IFile) {
//					IFile file = (IFile) selection.getFirstElement();
//					IEditorDescriptor editor = registry.getDefaultEditor(file.getName());
//					if( editor != null)
//						try {
//							getViewSite().getPage().openEditor(new FileEditorInput(file), editor.getId());
//						} catch (PartInitException e) {
//							throw new RuntimeException(e);
//						}
//					
//				}
//			}
//		}
//	};
	
	private ISelectionChangedListener listener = new ISelectionChangedListener() {

		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.getFirstElement() instanceof IFile) {
					IFile file = (IFile) selection.getFirstElement();
					IEditorDescriptor editor = registry.getDefaultEditor(file.getName());
					if( editor != null)
						try {
							getViewSite().getPage().openEditor(new FileEditorInput(file), editor.getId());
						} catch (PartInitException e) {
							throw new RuntimeException(e);
						}
					
				}
			}
		}
		
	};
	
	private IResourceChangeListener resourceListener = new IResourceChangeListener(){

		public void resourceChanged(IResourceChangeEvent event) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					TreePath[] elements = viewer.getExpandedTreePaths();
					viewer.refresh();
					viewer.setExpandedTreePaths(elements);
				}
			});
		}
	};
	
	@Override
	public void setFocus() {

	}

}
