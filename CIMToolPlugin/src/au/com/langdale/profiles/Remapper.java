/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
/**
 * Transform a profile model to match a base model.  An <code>NSMapper</code> is
 * used to map references in profile to entities in the base model.
 */
public class Remapper implements Runnable {
	private OntModel profileModel, baseModel;
	private NSMapper mapper;

	public Remapper(OntModel profileModel, OntModel baseModel) {
		this.profileModel = profileModel;
		this.baseModel = baseModel;
		mapper = new NSMapper(baseModel);
	}

	public void run() {
		int classes = 0;
		
		try {
			ResIterator it = profileModel.listSubjectsWithProperty(RDFS.subClassOf);
			while( it.hasNext()) {
				OntResource subject = it.nextResource();
				subject.addProperty(RDF.type, OWL.Class);
				handleClass(subject);
				classes += 1;
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log("Remapped " + classes + " classes.");
	}

	private void handleClass(OntResource clss) {
		int local_parents = 0;
		int anon_parents = 0;
		int foreign_parents = 0;
		int restricts = 0;
		boolean envelope = false;
		
		Set props = new HashSet();
		Set defined = new HashSet();
		Set bases = new HashSet();
		
		ResIterator it = clss.listProperties(RDFS.subClassOf);
		while( it.hasNext()) {
			OntResource node = it.nextResource();
			if( node.isURIResource()) {
				if( clss.isURIResource() && node.getNameSpace().equals(clss.getNameSpace())) {
					local_parents += 1;
				}
				else if( node.getNameSpace().equals(MESSAGE.NS)) {
					// it is an envelope definition
					if(node.equals(MESSAGE.Message))
						envelope = true;
				}
				else  {
					// inferred that the parent is a base and clss is its profile
					bases.add(node);
					foreign_parents += 1;
				}
			}
			else if( node.isAnon() ) {
				anon_parents += 1;
				if(node.isClass()) {
					restricts += 1;
					rebaseRestriction(clss, props, defined, node);
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

	private void rebaseClass(OntResource clss, Set bases) {
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

	private void rebaseRestriction(OntResource clss, Set props, Set defined, OntResource restrict) {
		Resource base = null;

		ResIterator it = restrict.listProperties(OWL.onProperty);
		while( it.hasNext()) {
			OntResource node = it.nextResource();
			if( node.isURIResource()) {
				Resource mapped = mapper.map(node, OWL.ObjectProperty);
				if( mapped == null )
					mapped  = mapper.map(node, OWL.DatatypeProperty);
				if( node.equals(mapped))
					base = node;
				else if(base == null )
					base = mapped;
			}
			else {
				log("ignoring malformed restriction for property", node);
			}
		}

		if( base != null ) {
			OntResource prop = base.inModel(baseModel);
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

	private void defineRanges(OntResource clss, Set props) {
		Iterator it = props.iterator();
		while( it.hasNext()) {
			OntResource prop = (OntResource) it.next();
			OntResource child;
			if( prop.hasRDFType(OWL.ObjectProperty)) 
				child = profileModel.createClass();
			else 
				child = profileModel.createIndividual(RDFS.Datatype);

			String label = prop.getLabel(null);
			if( label == null)
				label = prop.getLocalName();
			
			child.setLabel(label, null);
			OntResource restrict = profileModel.createAllValuesFromRestriction(null, prop, child);
			clss.addSuperClass(restrict);
		}
	}
	
	private void log(String string, Node node) {
		log(string + ": " + node);
	}
	
	private void log(String string, FrontsNode symbol) {
		log(string, symbol.asNode());
	}
	
	private void log(String item) {
		System.out.println(item);
	}
}
