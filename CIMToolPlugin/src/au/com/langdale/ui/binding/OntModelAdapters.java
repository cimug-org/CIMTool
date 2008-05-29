/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
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
		private Resource ident, clss;
		

		public ClassInstance(Resource ident, Resource clss, String comment, OntModelProvider context) {
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

		private Resource ident;
		private Property prop;

		public Annotation(Resource ident, Property prop, String comment, OntModelProvider context) {
			super(comment, context);
			this.ident = ident;
			this.prop = prop;
		}

		public boolean isTrue() {
			OntResource subject = context.getSubject();
			return subject != null && subject.hasProperty(prop, ident);
		}

		public void setTrue(boolean flag) {
			OntResource subject = context.getSubject();
			if( subject != null )
				if(flag)
					subject.addProperty(prop, ident);
				else
					subject.removeProperty(prop, ident);
		}
	}

	public static BooleanModel[] findClassInstances(OntClass clss, OntModelProvider context) {
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

	public static BooleanModel[] findAnnotations(OntClass clss, Property annot, OntModelProvider context) {
		ArrayList flags = new ArrayList();
		for( Iterator it = clss.listInstances(); it.hasNext();) {
			OntResource ident = (OntResource)it.next();
			String comment = label(ident);
			if(comment != null) {
				flags.add( new Annotation(symbol(ident), annot, comment, context));
			}
		}
		return toArray(flags);
	}
	
	public static BooleanModel[] findAnnotations(Resource clss, Property annot, OntModelProvider context) {
		OntModel model = context.getModel();
		if( model == null )
			return BooleanModel.EMPTY_FLAGS;
		
		OntClass oclss = model.getOntClass(clss.getURI());
		if( oclss == null)
			return BooleanModel.EMPTY_FLAGS;
		
		return findAnnotations(oclss, annot, context);
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

	private static Resource symbol(OntResource ident) {
		return ResourceFactory.createResource(ident.getURI());
	}

	private static String label(OntResource ident) {
		String comment = ident.getComment(null);
		if( comment == null)
			comment = ident.getLabel(null);
		if( comment == null && ident.isURIResource())
			comment = ident.getLocalName();
		return comment;
	}
}
