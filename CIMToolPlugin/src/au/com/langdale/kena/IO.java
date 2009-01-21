package au.com.langdale.kena;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;

public class IO {

	public static void read(OntModel model, InputStream contents, String namespace,	String syntax) {
		ModelFactory.createModelForGraph(model.getGraph()).read(contents, namespace, syntax);
	}

	public static void write(OntModel model, OutputStream contents, String namespace, String syntax, Map style) {
		Model stage = ModelFactory.createModelForGraph(model.getGraph());
		stage.setNsPrefixes(model.getNsPrefixMap());
		RDFWriter writer = stage.getWriter(syntax);
		for (Iterator it = style.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			writer.setProperty(key, style.get(key));
		};
		writer.write(stage, contents, namespace);
	}

}
