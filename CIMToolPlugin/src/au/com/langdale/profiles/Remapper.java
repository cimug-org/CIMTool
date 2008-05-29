/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
/**
 * Transform a profile model to match a base model.  An <code>NSMapper</code> is
 * used to map references in profile to entities in the base model.
 */
public class Remapper implements Runnable {
	private OntModel profileModel;
	private NSMapper mapper;

	public Remapper(OntModel profileModel, OntModel baseModel) {
		this.profileModel = profileModel;
		mapper = new NSMapper(baseModel);
	}

	public void run() {
		int classes = 0;
		
		ResIterator it = profileModel.listSubjectsWithProperty(RDFS.subClassOf);
		while( it.hasNext()) {
			Resource subject = it.nextResource();
			subject.addProperty(RDF.type, OWL.Class);
			handleClass((OntClass) subject.as( OntClass.class));
			classes += 1;
		}
		
		log("Remapped " + classes + " classes.");
	}

	private void handleClass(OntClass clss) {
		int local_parents = 0;
		int anon_parents = 0;
		int foreign_parents = 0;
		int restricts = 0;
		boolean envelope = false;
		
		Set props = new HashSet();
		Set defined = new HashSet();
		Set bases = new HashSet();
		
		NodeIterator it = clss.listPropertyValues(RDFS.subClassOf);
		while( it.hasNext()) {
			RDFNode node = it.nextNode();
			if( node.isURIResource()) {
				OntResource parent = (OntResource) node.as(OntResource.class);
				if( clss.isURIResource() && parent.getNameSpace().equals(clss.getNameSpace())) {
					local_parents += 1;
				}
				else if( parent.getNameSpace().equals(MESSAGE.NS)) {
					// it is an envelope definition
					if(parent.equals(MESSAGE.Message))
						envelope = true;
				}
				else  {
					// inferred that the parent is a base and clss is its profile
					bases.add(parent);
					foreign_parents += 1;
				}
			}
			else if( node.isAnon() ) {
				anon_parents += 1;
				if(node.canAs(Restriction.class)) {
					restricts += 1;
					rebaseRestriction(clss, props, defined, (Restriction)node.as(Restriction.class));
				}
			}
			else {
				log("removed invalid superclass node", node);
				it.remove(); // not a resource
			}
		}
		
		if( foreign_parents + anon_parents > 0 && !envelope) 
			rebaseClass(clss, bases);
		
		log( "properties: total=" + props.size() + " type restricted=" + defined.size(), clss);
		log( "superclasses: restrictions=" + restricts + 
				" foreign=" + foreign_parents + 
				" local=" + local_parents + 
				" anon=" + (anon_parents-restricts), clss);
		
		props.removeAll(defined);
		defineRanges(clss, props);
	}

	private void rebaseClass(OntClass clss, Set bases) {
		Resource base = null;
		for (Iterator it = bases.iterator(); it.hasNext();) {
			OntResource cand = (OntResource) it.next();
			Resource mapped = mapper.map(cand, OWL.Class);
			if( cand.equals(mapped))
				base = cand;
			else if(base == null )
				base = mapped;
		}
		
		if( base == null && clss.isURIResource()) 
			base = mapper.map(clss.getLocalName(), OWL.Class); 

		if( base == null) {
			log("could not find base class for", clss);
			return;
		}
			
		for (Iterator ir = bases.iterator(); ir.hasNext();) {
			OntResource cand = (OntResource) ir.next();
			if( ! cand.equals(base))
				clss.removeSuperClass(cand);
		}

		clss.addSuperClass(base);
		log("rebased profile class", clss);
	}

	private void rebaseRestriction(OntClass clss, Set props, Set defined, Restriction restrict) {
		Resource base = null;

		NodeIterator it = restrict.listPropertyValues(OWL.onProperty);
		while( it.hasNext()) {
			RDFNode node = it.nextNode();
			if( node.isURIResource()) {
				Resource cand = (Resource) node.as(Resource.class);
				Resource mapped = mapper.map(cand, OWL.ObjectProperty);
				if( mapped == null )
					mapped  = mapper.map(cand, OWL.DatatypeProperty);
				if( cand.equals(mapped))
					base = cand;
				else if(base == null )
					base = mapped;
			}
			else {
				log("ignoring malformed restriction for property", node);
			}
		}

		if( base != null ) {
			Property prop = (Property) base.as(Property.class);
			restrict.setOnProperty(prop);
			if( restrict.isAllValuesFromRestriction())
				defined.add(prop);
			else
				props.add(prop);
		}
		else {
			log("incomplete restriction for class", clss);
		}
	}

	private void defineRanges(OntClass clss, Set props) {
		Iterator it = props.iterator();
		while( it.hasNext()) {
			Property prop = (Property) it.next();
			OntResource child;
			if( prop.hasProperty(RDF.type, OWL.ObjectProperty)) 
				child = profileModel.createClass();
			else 
				child = profileModel.createIndividual(RDFS.Datatype);

			String label = ((OntResource) prop.as(OntResource.class)).getLabel(null);
			if( label == null)
				label = prop.getLocalName();
			
			child.setLabel(label, null);
			Restriction restrict = profileModel.createAllValuesFromRestriction(null, prop, child);
			clss.addSuperClass(restrict);
		}
	}
	
	private void log(String string, RDFNode node) {
		log(string + ": " + node);
	}
	
	private void log(String item) {
		System.out.println(item);
	}
}
