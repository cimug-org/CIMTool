/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.Collection;
import java.util.Iterator;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.xmi.UML;

public class HierarchyModel extends JenaTreeModelBase {

	private boolean subClasses;
	private Refactory refactory;
	
	public HierarchyModel(boolean subClasses) {
		this.subClasses = subClasses;
	}

	public void setRefactory(Refactory refactory) {
		this.refactory = refactory;
		if(refactory != null) {
			refactory.refresh();
			setOntModel(refactory.getModel());
		}
		else
			setOntModel((OntModel)null);
	}

	@Override
	protected Node classify(OntResource root) {
		return new SubjectNode( root );
	}
		
	public interface RootElementNode {}
	public interface TypeNode {}
	
	public class SubjectNode extends ModelNode {

		private OntResource subject;

		public SubjectNode(OntResource root) {
			subject = root;
		}

		@Override
		protected void populate() {
			Collection related = refactory.findRelatedProfiles(subject, subClasses, false);
			for (Iterator it = related.iterator(); it.hasNext();) {
				OntResource clss = (OntResource) it.next();
				add( new NestedNode( new ProfileClass(clss, refactory.getNamespace())));
			}
		}

		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}
	}
	
	public class NestedNode extends ModelNode {

		protected ProfileClass profile;

		public NestedNode(ProfileClass profile) {
			this.profile = profile;
		}

		@Override
		protected void populate() {
			Iterator it = subClasses? profile.getSubClasses(): profile.getSuperClasses();
			while (it.hasNext()) {
				OntResource clss = (OntResource) it.next();
				add( new NestedNode( new ProfileClass(clss, profile.getNamespace())));
			}
		}

		@Override
		public Class getIconClass() {
			return profile.hasStereotype(UML.concrete)? RootElementNode.class: TypeNode.class;
		}

		@Override
		public OntResource getSubject() {
			return profile.getSubject();
		}

		@Override
		public OntResource getBase() {
			return profile.getBaseClass();
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}
	}
}
