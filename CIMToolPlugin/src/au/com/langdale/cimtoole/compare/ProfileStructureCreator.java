/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.compare;

import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.properties.PropertySupport;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.SuperTypeNode;
import au.com.langdale.ui.util.IconCache;

import com.hp.hpl.jena.ontology.OntModel;

public class ProfileStructureCreator implements IStructureCreator {

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
		if( input instanceof IEncodedStreamContentAccessor && input instanceof ITypedElement && input instanceof IResourceProvider)
			return new InputProxy(input);
		else if(input instanceof Node)
			return new Proxy((Node)input);
		else if( input instanceof Proxy)
			return (Proxy)input;
		else 
			throw new RuntimeException("Unexpected input to profile comparison");
	}
	
	public static class InputProxy implements IStructureComparator, ITypedElement {
		private Object node; // should be both a ITypedElement and IEncodedStreamContentAccessor
		
		public InputProxy(Object node) {
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

		public Object[] getChildren() {
			ProfileModel tree = new ProfileModel();
			OntModel model, backgroundModel;
			try {
				IResourceProvider resourceProvider = ((IResourceProvider)node);
				IEncodedStreamContentAccessor streamAccessor = ((IEncodedStreamContentAccessor)node);
				IFile file = ((IFile)resourceProvider.getResource());
				model = Task.parse(file, streamAccessor.getContents());
				backgroundModel = CIMToolPlugin.getCache().getMergedOntologyWait(Info.getSchemaFolder(file.getProject()));
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
			
			tree.setOntModel(model);
			tree.setBackgroundModel(backgroundModel);
			tree.setRootResource(MESSAGE.Message);

			return wrapChildren(tree.getRoot());
		}

		public Image getImage() {
			return ((ITypedElement)node).getImage();
		}

		public String getName() {
			return ((ITypedElement)node).getName();
		}

		public String getType() {
			return FOLDER_TYPE;
		}
	}
	
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
		if( node instanceof ElementNode || node instanceof SuperTypeNode)
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
