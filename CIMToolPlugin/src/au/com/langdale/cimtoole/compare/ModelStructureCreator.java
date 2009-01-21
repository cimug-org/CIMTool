/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.compare;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.properties.PropertySupport;
import au.com.langdale.jena.TreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.ui.util.IconCache;

public class ModelStructureCreator implements IStructureCreator {

	public String getContents(Object node, boolean ignoreWhitespace) {
		if( node instanceof Proxy)
			return ((Proxy)node).getDescription();
		else
			return node.toString();
	}

	public String getName() {
		return "Profile Comparison";
	}

	public IStructureComparator getStructure(final Object input) {
		if(input instanceof Node)
			return new Proxy((Node)input);
		else if( input instanceof Proxy)
			return (Proxy)input;
		else if(input instanceof IResourceProvider && input instanceof ITypedElement) 
			return new ResourceProxy(input);
//		else if(input instanceof IStreamContentAccessor && input instanceof ITypedElement) 
//			return new StreamProxy(input);
		else
			throw new RuntimeException("Unexpected input to profile comparison");
	}
	
	public static abstract class InputProxy implements IStructureComparator, ITypedElement {
		private ITypedElement node; 
		
		public InputProxy(ITypedElement node) {
			this.node = node;
		}
		
		@Override
		public boolean equals(Object raw) {
			if (raw instanceof InputProxy) {
				InputProxy proxy = (InputProxy) raw;
				return node.equals(proxy.node);
			}
			return false;
		}

		public Image getImage() {
			return node.getImage();
		}

		public String getName() {
			return node.getName();
		}
		
		protected String getFileExtension() {
			return node.getType().toLowerCase();
		}

		public String getType() {
			return FOLDER_TYPE;
		}
	}
	
	public static class ResourceProxy extends InputProxy {
		IResourceProvider node;
		
		public ResourceProxy( Object node ) {
			super((ITypedElement)node);
			this.node = (IResourceProvider) node;
		}
		
		public Object[] getChildren() {
			try {
				IFile file = ((IFile)node.getResource());
				TreeModelBase tree = Task.createTreeModel(file);
				return wrapChildren(tree.getRoot());
				
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		}
		
	}
//	
//	public static class StreamProxy extends InputProxy {
//		private IStreamContentAccessor node;
//		public StreamProxy( Object node) {
//			super((ITypedElement)node);
//			this.node = (IStreamContentAccessor) node;
//		}
//		
//		public Object[] getChildren() {
//			try {
//				InputStream contents = node.getContents();
//				
//				TreeModelBase tree = Task.createTreeModel(file, contents);
//				return wrapChildren(tree.getRoot());
//				
//			} catch (CoreException e) {
//				throw new RuntimeException(e);
//			}
//		}
//	}

	public static class Proxy implements IStructureComparator, ITypedElement {
		private Node node;
		
		public Proxy(Node node) {
			this.node = node;
		}
		
		public String getDescription() {
			return PropertySupport.getDescription(node);
		}

		private String getIdent() {
			return node.getName() + " " + node.getBase().toString();
		}
		
		@Override
		public boolean equals(Object raw) {
			if (raw instanceof Proxy) {
				Proxy proxy = (Proxy) raw;
				return getIdent().equals(proxy.getIdent());
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return getIdent().hashCode();
		}

		public Object[] getChildren() {
			return wrapChildren(node);
		}

		public Image getImage() {
			return IconCache.get(node);
		}

		public String getName() {
			return node.toString();
		}

		public String getType() {
			return "owl";
		}
		
		public String toString() {
			return node.toString();
		}
	}
	
	private static Object[] wrapChildren(Node node) {
		if( node.isPruned())
			return new Object[0];
		
		List children = node.getChildren();
		Object[] result = new Object[children.size()];
		Iterator it = children.iterator();
		
		int ix = 0;
		while (it.hasNext()) {
			Node child = (Node) it.next();
			result[ix++] = new Proxy(child);
		}
		return result;
	}

	public IStructureComparator locate(Object path, Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public void save(IStructureComparator node, Object input) {
		// TODO Auto-generated method stub
		
	}
}
