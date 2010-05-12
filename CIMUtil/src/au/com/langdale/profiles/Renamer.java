package au.com.langdale.profiles;

import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;

public abstract class Renamer {
	protected OntModel model;

	public static class NamespaceChanger extends Renamer {
		private String namespace;
		private String replace;
		private String replaceNS;

		public NamespaceChanger(OntModel model, String namespace) {
			this.model = model;
			this.namespace = namespace;
			this.replace = model.getValidOntology().getURI();
			this.replaceNS = replace + "#";
		}
		
		@Override
		protected Node rename(Node uri) {
			if( uri.getNameSpace().equals(replaceNS))
				return Node.createURI(namespace + uri.getLocalName());
			if( uri.getURI().equals(replace))
				return Node.createURI(namespace.substring(0, namespace.length()-1));
			return uri;
		}
	}
	
	public static class URIMapper extends Renamer {
		private Map map;

		public URIMapper(OntModel model, Map map) {
			this.model = model;
			this.map = map;
		}
		
		@Override
		public Node rename(Node uri) {
			return (Node) map.get(uri);
		}
	}

	protected abstract Node rename(Node uri);

	public OntModel applyRenamings() {
		OntModel result = ModelFactory.createMem();
		Graph graph = result.getGraph();
		Iterator it = model.getGraph().find(Triple.ANY);
		while( it.hasNext()) {
			Triple t = (Triple) it.next();
			Node s = t.getSubject().isURI()? rename(t.getSubject()): null;
			Node o = t.getObject().isURI() ? rename(t.getObject()) : null;
			if( s != null || o != null)
				t = Triple.create(s != null? s: t.getSubject(), t.getPredicate(), o != null? o: t.getObject());
			graph.add(t);
		}
		return result;
	}
}
