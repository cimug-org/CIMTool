/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.jena;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.impl.OntResourceImpl;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.UniqueExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OntSubject extends OntResourceImpl {

	public OntSubject(Node n, EnhGraph g) {
		super(n, g);
	}

	public OntSubject(OntResource r) {
		super(r.asNode(), ((OntResourceImpl)r).getGraph());
	}

    /**
     * Modified version of OntClass.listSuperClasses to iterate OntResource views
     * and therefore not throw conversion exceptions.
     */
    public ExtendedIterator listSuperClasses( boolean direct ) {
        return UniqueExtendedIterator.create(
                listDirectPropertyValues( RDFS.subClassOf, "", OntResource.class, RDFS.subClassOf, direct, false )
                .filterDrop( new SingleEqualityFilter( this ) ) );
    }

    /**
     * Modified version of OntClass.listSubClasses to iterate OntResource views
     * and therefore not throw conversion exceptions.
     */
    public ExtendedIterator listSubClasses( boolean direct ) {
        return UniqueExtendedIterator.create(
                listDirectPropertyValues( RDFS.subClassOf, "", OntResource.class, RDFS.subClassOf, direct, true )
                .filterDrop( new SingleEqualityFilter( this ) ) );
    }
}
