package au.com.langdale.kena;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
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
import com.hp.hpl.jena.util.FileUtils;

public class IO {

	public static void read(OntModel model, InputStream contents, String namespace,	String syntax) {
		if( syntax.equals(Format.RDF_XML_WITH_NODEIDS.toFormat())){
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
		
		if( syntax.equals(Format.RDF_XML_WITH_NODEIDS.toFormat())) {
			writer = stage.getWriter(Format.RDF_XML.toFormat());
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
	
	public static void write(OntModel model, OutputStream contents, String namespace, String format, Map style, String copyright) {
		
		/**
		 *  Given that a copyright has been provided this method handles the showXmlDeclaration style
		 *  setting in a slightly different manner. If this setting is both specified in the style map
		 *  and has a value of "true" we handle the printing of both the XML declaration header and the 
		 *  copyright outside of the normal writer.write() call. Note that we always set the parameter 
		 *  to "false" as we intentionally want to ensure that Jena's RDFWriter does not also add a 
		 *  second instance of the XML declaration in the output that is written.
		 */
		Boolean showXmlDeclaration = Boolean.valueOf((style != null && style.containsKey("showXmlDeclaration") ? (String) style.get("showXmlDeclaration") : "false"));
		style.put("showXmlDeclaration", "false"); // We set to false
		
		Model stage = ModelFactory.createModelForGraph(model.getGraph());
		stage.setNsPrefixes(model.getNsPrefixMap());
		
		RDFWriter writer;
		if( format.equals(Format.RDF_XML_WITH_NODEIDS.toFormat())) {
			writer = stage.getWriter(Format.RDF_XML.toFormat());
			writer.setProperty("longid", Boolean.TRUE);
		}
		else 
			writer = stage.getWriter(format);
		
		if( style != null ) {
			for (Iterator it = style.keySet().iterator(); it.hasNext();) {
				String key = (String) it.next();
				writer.setProperty(key, style.get(key));
			}
		}
		
		Writer out = FileUtils.asUTF8(contents);
		PrintWriter pw = (out instanceof PrintWriter ? (PrintWriter) out : new PrintWriter( out ));
		
		// For an RDFWRiter we only included the copyright for XML-based output...
		if (showXmlDeclaration && Format.isXML(format)) {
			StringBuffer declaration = new StringBuffer();
			declaration.append("<?xml version=" + "\"1.0\"" + "?>").append("\n");
			
			/**
			 * Since this is specifically a copyright for an RDF XML profile we utilize
			 * a standard XML compliant comment to "host" the copyright notice.
			 */
			if (copyright != null && !"".equals(copyright)) {
				declaration.append("<!-- ").append("\n");
				declaration.append(copyright).append("\n");
				declaration.append("-->");
			}
			pw.println(declaration);
		}
		
		writer.write(stage, pw, namespace);
	}

	public static void print(OntModel model) {
		write(model, System.out, null, Format.TURTLE.toFormat(), null);
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
