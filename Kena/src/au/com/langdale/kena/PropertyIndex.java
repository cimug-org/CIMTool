package au.com.langdale.kena;

import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class PropertyIndex extends SearchIndex {
	Property prop;
	
	public PropertyIndex(Property prop) {
		this.prop = prop;
	}
	
	/**
	 * Index resources 
	 * @param model: the model containing the resources 
	 */
	@Override
	public void scan(OntModel model) {
		Iterator it = model.getGraph().find(Node.ANY, prop.asNode(), Node.ANY);
		while( it.hasNext()) {
			Triple t = (Triple) it.next();
			Node n = t.getObject();
			if( n.isLiteral()) {
				addWord(n.getLiteralLexicalForm());
			}
		}
	}
	
	/**
	 * Find resources by local name.
	 * @param name: the local name
	 * @param model: the model containing the resources
	 * @return a set of <code>Resource</code>
	 */
	@Override
	public Set locate(String name, OntModel model) {
		return model.listSubjectsWithProperty(prop, name).toSet();
	}
}
