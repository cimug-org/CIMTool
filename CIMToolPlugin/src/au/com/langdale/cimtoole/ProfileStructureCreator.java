/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.swt.graphics.Image;

import au.com.langdale.ui.util.IconCache;

public class ProfileStructureCreator implements IStructureCreator {

	public String getContents(Object node, boolean ignoreWhitespace) {
		return node.toString();
	}

	public String getName() {
		return "example";
	}

	public IStructureComparator getStructure(final Object input) {
		return new Proxy(input);
	}
	
	public static class Proxy implements IStructureComparator, ITypedElement {
		private Object node;
		
		public Proxy(Object node) {
			this.node = node;
		}
		
		@Override
		public boolean equals(Object raw) {
			if (raw instanceof Proxy) {
				Proxy proxy = (Proxy) raw;
				return node.equals(proxy.node);
			}
			return false;
		}

		public Object[] getChildren() {
			return null;
		}

		public Image getImage() {
			return IconCache.get(node);
		}

		public String getName() {
			return node.toString();
		}

		public String getType() {
			return UNKNOWN_TYPE;
		}
		
	}

	public IStructureComparator locate(Object path, Object input) {
		// TODO Auto-generated method stub
		return null;
	}

	public void save(IStructureComparator node, Object input) {
		// TODO Auto-generated method stub
		
	}

}
