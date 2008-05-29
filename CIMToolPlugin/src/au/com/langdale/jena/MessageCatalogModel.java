package au.com.langdale.jena;

import java.util.Iterator;


import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.validation.LOG;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;

/**
 * Represents just the set of Message subclasses found in an ontology
 * as a tree structure.
 * 
 *
 */
public class MessageCatalogModel extends JenaTreeModelBase {

	protected class MessageNode extends ModelNode {
		private OntClass subject;
		
		public MessageNode(OntClass subject) {
			this.subject = subject;
		}

		@Override
		public boolean getErrorIndicator() {
			return subject.hasProperty(LOG.hasProblems);
		}

		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		protected void populate() {
			Iterator it = subject.listSubClasses(true); // FIXME: this may throw
			while( it.hasNext()) {
				add( new MessageNode( (OntClass) it.next()));
			}
		}

		/**
		 * Create a new Message definition derived from this node's definition. 
		 */
		public OntResource create(String uri) {
			OntClass child = getOntModel().createClass(uri);
			child.addSuperClass(subject);
			structureChanged();
			return child;
		}
	}
	
	/**
	 * Create a new top level Message definition.
	 */
	public OntResource create(String uri) {
		MessageNode parent = (MessageNode) getRoot();
		return parent.create(uri);
	}

	/**
	 * The root should be the generic Message class or a subclass.
	 */
	@Override
	protected Node classify(OntResource root) throws ConversionException {
		OntClass clss = root.asClass();
		if( clss.equals(MESSAGE.Message) || clss.hasSuperClass(MESSAGE.Message))
			return new MessageNode(clss);
		return null;
	}
}
