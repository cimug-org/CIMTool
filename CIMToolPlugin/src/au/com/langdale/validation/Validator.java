/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import au.com.langdale.inference.SimpleReasoner;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.kena.OntModel;

import au.com.langdale.util.Logger;
import au.com.langdale.validation.ValidatorUtil.ValidatorProtocol;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;

/**
 * A validator based on the Jena rule based inference engine.
 *
 */
public class Validator extends ValidatorUtil implements ValidatorProtocol {

	private Reasoner reasoner;
	
	public Validator(OntModel schema, String namespace, InputStream ruleText) throws ParserException, IOException {
		reasoner = new SimpleReasoner(expandRules(schema, namespace, ruleText, BuiltinRegistry.theRegistry));
	}
	
	public OntModel run(String source, String base, String namespace, Logger errors) throws IOException {
		String lang = "RDF/XML";
		if( source.endsWith(".ttl"))
			lang = "TURTLE";
		InputStream input = new FileInputStream(source);
		Model data = ModelFactory.createDefaultModel();
		RDFReader reader = data.getReader(lang);
		reader.setErrorHandler(errors.getRDFErrorHandler());
		reader.read(data, input, namespace);
		InfModel infmodel = ModelFactory.createInfModel(reasoner, data);
		infmodel.prepare();
		Model result = infmodel.getDeductionsModel();
		Graph graph = result.getGraph();
		logProblems(errors, graph);
		return au.com.langdale.kena.ModelFactory.createMem(graph);
	}
	
	static {
		ValidationBuiltins.registerAll();
	}
}
