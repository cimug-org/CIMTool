/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import au.com.langdale.ui.binding.FilteredContentProvider.Filter;
import au.com.langdale.validation.Validation;
import au.com.langdale.cimtoole.project.Info;
/**
 * A set of table view bindings for workspace resources.
 */
public class ResourceUI {
	
	public static final Filter VALIDATION_FILTER = new Filter() {

		public boolean allow(Object object) {
			if(  object instanceof IProject ) {
				IProject project = (IProject) object;
				return project.isOpen();
			}
			if( object instanceof IFile ) {
				IFile resource = (IFile) object;
				IPath path = resource.getProjectRelativePath();
				return path.segmentCount() == 2 
					&& (path.segment(0).equals("Instances") ||
							path.segment(0).equals("Incremental"));
			}
			return false;
		}

		public boolean flatten(Object object) {
			return object instanceof IFolder;
		}
	
		public boolean prune(Object value) {
			return false;
		}
	};
	
	public static void displayWorkspace(StructuredViewer viewer, Filter filter) {
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.setComparator(new ViewerComparator());
		viewer.setContentProvider(new FilteredContentProvider(filter, new BaseWorkbenchContentProvider()));	
	}
	
	public static class ProjectBinding extends TableBinding implements Filter {
		
		@Override
		protected void configureViewer(StructuredViewer viewer) {
			displayWorkspace(viewer, this);
		}

		@Override
		protected Object getInput() {
			return ResourcesPlugin.getWorkspace().getRoot();
		}
		
		public IProject getProject() {
			return (IProject) getValue();
		}
		
		public void setSelected(IStructuredSelection selection ) {
			Object item = selection.getFirstElement();
			if( item instanceof IResource) {
				IResource resource = (IResource)item;
				setValue(resource.getProject());
			}
			else
				setValue(null);			
		}

		public boolean allow(Object value) {
			return value instanceof IProject && ((IProject)value).isOpen();
		}

		public boolean flatten(Object value) {
			return false;
		}
		
		public boolean prune(Object value) {
			return false;
		}

		@Override
		public String validate() {
			if(! allow(getValue()))
				return "A project must be selected.";
			else
				return null;
		}
	}
	
	public static abstract class ProjectMemberBinding extends TableBinding implements Filter {
		
		protected abstract boolean allow(IResource resource);
		
		@Override
		protected void configureViewer(StructuredViewer viewer) {
			displayWorkspace(viewer, this);
		}
		
		public boolean allow(Object object) {
			return object instanceof IResource && allow((IResource)object);
		}

		public boolean flatten(Object object) {
			return object instanceof IFolder;
		}
		
		public boolean prune(Object value) {
			return false;
		}
		
		@Override
		protected Object getInput() {
			return getParent().getValue();
		}

		public IProject getProject() {
			return (IProject) getParent().getValue();
		}
		
		public IResource getResource() {
			return (IResource) getValue();
		}
		
		public void setSelected(IStructuredSelection selection ) {
			Object value = selection.getFirstElement();
			if(value != null && allow(value)) 
				setValue(value);
			else
				setValue(null);			
		}

		@Override
		public String validate() {
			if(! allow(getValue()))
				return "A member of the project must be selected.";
			else
				return null;
		}
	}

	public static class DiagnosticsBinding extends ProjectMemberBinding {
		@Override
		public boolean allow(IResource resource) {
			return Info.isDiagnostic(resource);
		}
	}

	public static class InstanceBinding extends ProjectMemberBinding {
		@Override
		public boolean allow(IResource resource) {
			return Info.isSplitInstance(resource);
		}
	}
	
	public static class ProfileBinding extends ProjectMemberBinding {
		@Override
		protected boolean allow(IResource resource) {
			return Info.isProfile(resource);
		}

		public IFile getFile() {
			return (IFile)getValue();
		}
	}
	
	public static class LocalFileBinding extends TextBinding {
		private String ext;
		
		public LocalFileBinding(String ext, boolean required) {
			super(Validation.SimpleFile(ext, required));
			this.ext = ext;
		}
		
		public LocalFileBinding(String[] exts, boolean required) {
			super(Validation.SimpleFile(exts, required));
		}

		@Override
		protected String createSuggestion() {
			IPath path = Path.fromOSString(parentText());
			if(ext != null)
				path = path.removeFileExtension().addFileExtension(ext);
			return path.lastSegment();
		}
		
		public IFile getFile(IFolder folder) {
			String name = getText();
			if( name.length() == 0)
				return null;
			
			if( ! name.contains(".") && ext != null)
				name = name + "." + ext;
			
			return folder.getFile(name);
		}
	}
}
