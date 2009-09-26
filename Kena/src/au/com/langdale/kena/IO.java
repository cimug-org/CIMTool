package au.com.langdale.kena;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;


import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;

public class IO {

	public static final String RDF_XML_WITH_NODEIDS = "RDF/XML-WITH-NODEIDS";

	public static void read(OntModel model, InputStream contents, String namespace,	String syntax) {
		if( syntax.equals(RDF_XML_WITH_NODEIDS)){
			RDFParser parser = new RDFParser(contents, null, namespace, new GraphInjector(model.getGraph()), null, false);
			parser.run();
		}
		else
			ModelFactory.createModelForGraph(model.getGraph()).read(contents, namespace, syntax);
	}

	public static void write(OntModel model, OutputStream contents, String namespace, String syntax, Map style) {
		Model stage = ModelFactory.createModelForGraph(model.getGraph());
		stage.setNsPrefixes(model.getNsPrefixMap());
		
		RDFWriter writer;
		if( syntax.equals(RDF_XML_WITH_NODEIDS)) {
			writer = stage.getWriter("RDF/XML");
			writer.setProperty("longid", Boolean.TRUE);
		}
		else 
			writer = stage.getWriter(syntax);
		
		if( style != null ) {
			for (Iterator it = style.keySet().iterator(); it.hasNext();) {
				String key = (String) it.next();
				writer.setProperty(key, style.get(key));
			}
		}
		writer.write(stage, contents, namespace);
	}
	
	public static void print(OntModel model) {
		write(model, System.out, null, "TURTLE", null);
	}

	public static class GraphInjector implements Injector {
		Graph graph;

		public GraphInjector(Graph graph) {
			this.graph = graph;
		}

		public void addObjectProperty(Object subj, String pred, Object obj) {
			graph.add(Triple.create((Node)subj, Node.createURI(pred), (Node)obj));
		}

		public void addDatatypeProperty(Object subj, String pred, Object obj) {
			graph.add(Triple.create((Node)subj, Node.createURI(pred), (Node)obj));
		}

		public Object createAnon(String id) {
			if(id != null)
				return Node.createAnon(AnonId.create(id));
			else
				return Node.createAnon();
		}

		public Object createNamed(String uri) {
			return Node.createURI(uri);
		}

		public Injector createQuote(Object node) {
			// TODO Auto-generated method stub
			return null;
		}

		public Object createLiteral(String value, String lang, String type, boolean isXML) {
	        String dtURI = type;
	        if (dtURI == null)
	            return Node.createLiteral(value, lang, false);

	        if (isXML) 
	            return Node.createLiteral(value, null, true);

	        RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(dtURI);
	        return Node.createLiteral(value, null, dt);	
		}

		public void setPrefix(String prefix, String namespace) {
			// TODO Auto-generated method stub
			
		}
		
		public void close() throws IOException {
			// the graph is ready
		}
	}
}
