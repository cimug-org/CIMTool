package au.com.langdale.cimtoole;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Convert {

	/**
	 * Convert an RDF file from one syntax to another.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if( args.length < 2 || args.length > 5) {
			System.err.println("arguments: input_file output_file [input_lang [output_lang [base_uri]]]");
			return;
		}
		
		String input = args[0];
		String output = args[1];
		String inputLang = args.length > 2? args[2] : "TTL";
		String outputLang = args.length > 3? args[3] : "RDF/XML";
		String base = args.length > 4? args[4] : "http://langdale.com.au/2008/network#";
		
		Model model = ModelFactory.createDefaultModel();
		model.read(new BufferedInputStream(new FileInputStream(input)), base, inputLang);
		model.write(new BufferedOutputStream(new FileOutputStream(output)), outputLang, base);
	}

}
