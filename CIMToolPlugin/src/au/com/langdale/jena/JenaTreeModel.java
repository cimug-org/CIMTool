package au.com.langdale.jena;

import java.util.Iterator;

import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 *  Original tree model represents an ontology as a tree 
 *  using a UML inspired layout and recognises xmlattribute
 *  and xmlelement stereotypes imported from the UML.
 * 
 */
public class JenaTreeModel extends UMLTreeModel {

	/**
	 * The root node and nodes under packages will be 
	 * packages or classes. 
	 */
	@Override
	protected Node classify(OntResource child) {
		if( child.hasProperty(RDF.type, UML.Package))
			return new PackageNode(child);
		else if( child.canAs(OntClass.class)) {
			OntClass cl = (OntClass) child.as(OntClass.class);
			if( cl.hasProperty(UML.hasStereotype, UML.xmlelement))
				return new XMLElementNode(cl);
			else if( cl.hasProperty(UML.hasStereotype, UML.enumeration))
				return new EnumClassNode(cl);
			else
				return new ClassNode( cl );
		}
		return null;
	}
	
	/**
	 * A class that corresponds to an XML element.
	 * 
	 */
	protected class XMLElementNode extends ClassNode {
		public XMLElementNode(OntClass clss) {
			super(clss);
		}

		@Override
		protected void populate() {
			Iterator it = listProperties(true);
			while( it.hasNext()) {
				OntProperty pt = (OntProperty) it.next();
				if( pt.hasProperty(UML.hasStereotype, UML.xmlattribute))
					add( new XMLAttributeNode(pt));
				else {
					OntResource range = pt.getRange();
					if( range != null && range.hasProperty(UML.hasStereotype, UML.xmlelement) && range.canAs(OntClass.class))
						add( new XMLElementNode(range.asClass()));
				}
			}
		}
	}
	
	/**
	 * A datatype property that corresponds to an XML attribute.
	 * 
	 */
	protected class XMLAttributeNode extends DatatypeNode {
		public XMLAttributeNode(OntProperty prop) {
			super(prop);
		}
		
	}
	
}
