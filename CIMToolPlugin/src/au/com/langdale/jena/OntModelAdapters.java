/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.jena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.ui.binding.BooleanModel;
import au.com.langdale.xmi.UML;
/**
 * A set of BooleanModel's that reflect the contents of an ontology model.
 */
public class OntModelAdapters  {
	
	private static abstract class AdapterBase  implements BooleanModel {
		protected String comment;
		protected OntModelProvider context;
		
		public AdapterBase(String comment, OntModelProvider context) {
			this.comment = comment;
			this.context = context;
		}

		@Override
		public String toString() {
			return comment;
		}
	}

	private static class ClassInstance extends AdapterBase {
		private FrontsNode ident, clss;
		

		public ClassInstance(FrontsNode ident, FrontsNode clss, String comment, OntModelProvider context) {
			super(comment, context);
			this.ident = ident;
			this.clss = clss;
		}

		public boolean isTrue() {
			OntModel model = context.getModel();
			return model != null && model.contains(ident, RDF.type, clss);
		}

		public void setTrue(boolean flag) {
			OntModel model = context.getModel();
			if( model != null )
				if(flag)
					model.add(ident, RDF.type, clss);
				else
					model.remove(ident, RDF.type, clss);
		}
	}
	
	private static class Annotation extends AdapterBase {

		private FrontsNode ident;
		private FrontsNode prop;

		private static final List<String> NON_EDITABLE_IDENT = new ArrayList<String>();
		private static final List<String> EDITABLE_CLASS_IDENT = new ArrayList<String>();
		private static final List<String> EDITABLE_ASSOCIATION_IDENT = new ArrayList<String>();

		static {
			/** 
			 * Class-level stereotypes - imported via XMI/EAP. None
			 * of these should be editable by the end user as they 
			 * are core stereotypes native to CIM and used within 
			 * The UML. They would automatically be set on a Class,
			 * attribute, enumeration, etc. during import processing
			 * of an XMI or EAP file.
			 */
			NON_EDITABLE_IDENT.add(UML.primitive.getURI());
			NON_EDITABLE_IDENT.add(UML.datatype.getURI());
			NON_EDITABLE_IDENT.add(UML.cimdatatype.getURI());
			NON_EDITABLE_IDENT.add(UML.enumeration.getURI());
			NON_EDITABLE_IDENT.add(UML.compound.getURI());
			/** attribute-level stereotypes - imported via XMI/EAP */
			NON_EDITABLE_IDENT.add(UML.attribute.getURI());
			NON_EDITABLE_IDENT.add(UML.enumliteral.getURI());
			/** association related stereotypes - imported via XMI/EAP */
			NON_EDITABLE_IDENT.add(UML.compositeOf.getURI());
			NON_EDITABLE_IDENT.add(UML.ofComposite.getURI());
			NON_EDITABLE_IDENT.add(UML.aggregateOf.getURI());
			NON_EDITABLE_IDENT.add(UML.ofAggregate.getURI());
			
			/** Stereotypes editable only for top-level Classes in the tree. This doesn't include enumerations. */
			EDITABLE_CLASS_IDENT.add(UML.concrete.getURI());
			EDITABLE_CLASS_IDENT.add(UML.description.getURI());
			
			/** Stereotypes editable only for associations in the tree. */
			EDITABLE_ASSOCIATION_IDENT.add(UML.byreference.getURI());
		}
		
		public Annotation(FrontsNode ident, FrontsNode prop, String comment, OntModelProvider context) {
			super(comment, context);
			this.ident = ident;
			this.prop = prop;
		}

		public boolean isTrue() {
			OntResource subject = context.getSubject();
			return subject != null && subject.hasProperty(prop, ident);
		}

		public void setTrue(boolean flag) {
			if(isEditable()){
				OntResource subject = context.getSubject();
				if(flag) {
					subject.addProperty(prop, ident);
				} else {
					subject.removeProperty(prop, ident);
				}
			}
		}
		
		/**
		 * Method to determine if this Annotation is editable for the currently selected subject.
		 * The method uses the static List(s) of stereotypes to "test" if a stereotype should be 
		 * allowed to be edited.
		 */
		public boolean isEditable() {
			OntResource subject = context.getSubject();
			if((subject == null) || NON_EDITABLE_IDENT.contains(ident.asNode().getURI()) || //
					((subject != null) && //
							// Indicates it is the profile envelope that is selected...nothing is editable
							(!subject.isClass() && !subject.isDatatype()) ||
							// A subject that is a datatype indicates that a attribute of type primitive is currently selected...
							(subject.isDatatype() && (EDITABLE_CLASS_IDENT.contains(ident.asNode().getURI()) || EDITABLE_ASSOCIATION_IDENT.contains(ident.asNode().getURI()))) || //
							// This is relevant for a top-level class that is selected... 
							(subject.isClass() && !subject.hasProperty(UML.hasStereotype, UML.attribute) && !subject.hasProperty(OWL.unionOf) && !subject.hasProperty(UML.hasStereotype, UML.enumeration) && EDITABLE_ASSOCIATION_IDENT.contains(ident.asNode().getURI())) || //
							// This is relevant for a top-level enumeration that is selected... 
							(subject.isClass() && !subject.hasProperty(UML.hasStereotype, UML.attribute) && !subject.hasProperty(OWL.unionOf) && subject.hasProperty(UML.hasStereotype, UML.enumeration) && (EDITABLE_CLASS_IDENT.contains(ident.asNode().getURI()) || EDITABLE_ASSOCIATION_IDENT.contains(ident.asNode().getURI()))) || //
							// This is relevant for an association that is selected... 
							(subject.isClass() && !subject.hasProperty(UML.hasStereotype, UML.attribute) && subject.hasProperty(OWL.unionOf) && !subject.hasProperty(UML.hasStereotype, UML.enumeration) && (EDITABLE_CLASS_IDENT.contains(ident.asNode().getURI()))) || //
							// Remaining checks are for a non-primitive attribute...
							(subject.isClass() && subject.hasProperty(UML.hasStereotype, UML.attribute) && //
									(EDITABLE_CLASS_IDENT.contains(ident.asNode().getURI()) || 
									// The presence of an 'enumeration' stereotype on an attribute indicates that an attribute of that enumeration type is currently selected...
									(subject.hasProperty(UML.hasStereotype, UML.enumeration) && EDITABLE_ASSOCIATION_IDENT.contains(ident.asNode().getURI())))))) {
				return false;
			}
			return true;
		}
		
		@Override
		public String toString() {
			return comment + (!isEditable() ? "   [" + "Not Modifiable" + "]" : "");
		}
	}

	public static BooleanModel[] findClassInstances(OntResource clss, OntModelProvider context) {
		ArrayList flags = new ArrayList();
		for( Iterator it = clss.listInstances(); it.hasNext();) {
			OntResource ident = (OntResource)it.next();
			String comment = label(ident);
			if(comment != null) {
				flags.add( new ClassInstance(symbol(ident), symbol(clss), comment, context));
			}
		}
		return toArray(flags);
	}
	
	public static BooleanModel[] findAnnotations(FrontsNode seed, FrontsNode annot, OntModelProvider context) {
		OntModel model = context.getModel();
		if( model == null )
			return BooleanModel.EMPTY_FLAGS;
		
		OntResource clss = model.createResource(seed.asNode());
		if(! clss.isClass())
			return BooleanModel.EMPTY_FLAGS;

		ArrayList flags = new ArrayList();
		for( ResIterator it = clss.listInstances(); it.hasNext();) {
			OntResource ident = it.nextResource();
			String comment = label(ident);
			if(comment != null) {
				flags.add( new Annotation(symbol(ident), annot, comment, context));
			}
		}
		return toArray(flags);
	}
	
	private static BooleanModel[] toArray(Collection coll) {
		BooleanModel[] result = new BooleanModel[coll.size()];
		int ix = 0;
		for (Iterator it = coll.iterator(); it.hasNext();) {
			BooleanModel bm = (BooleanModel) it.next();
			result[ix++] = bm;
		}
		return result;
	}

	private static FrontsNode symbol(OntResource ident) {
		return ResourceFactory.createResource(ident.getURI());
	}

	private static String label(OntResource ident) {
		String comment = ident.getComment();
		if( comment == null)
			comment = ident.getLabel();
		if( comment == null && ident.isURIResource())
			comment = ident.getLocalName();
		return comment;
	}
}
