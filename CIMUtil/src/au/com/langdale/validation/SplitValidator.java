/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import au.com.langdale.inference.Extractor;
import au.com.langdale.inference.ProxyRegistry;
import au.com.langdale.inference.StandardFunctorActions;
import au.com.langdale.inference.ValidationBuiltins;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.splitmodel.SplitBase;
import au.com.langdale.splitmodel.SplitReader;
import au.com.langdale.util.Logger;
import au.com.langdale.validation.ValidatorUtil.ValidatorProtocol;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;

/**
 * A validation processor for split models.
 */
public class SplitValidator extends ValidatorUtil implements ValidatorProtocol {

	private List rules;
	private Map functors;
	GraphMem params = new GraphMem();
	
	/**
	 * Initialise with validation schema and rules.
	 * 
	 * @param schema: schema axioms to be used for validation
	 * @param namespace: not used
	 * @param ruleText: the validation rules in source form
	 * @throws ParserException
	 * @throws IOException
	 */
	public SplitValidator(OntModel schema, InputStream ruleText) throws ParserException, IOException {
		functors = StandardFunctorActions.create();
		BuiltinRegistry registry = new ProxyRegistry(functors.keySet());
		ValidationBuiltins.registerAll(registry);
		rules = expandRules(schema, ruleText, registry);
	}
	/**
	 * Set an option that will be available to the validation rules.
	 *  
	 * @param option: a resource representing the option 
	 * @param state: the value of the option
	 */
	public void setOption(Node option, boolean state) {
		StandardFunctorActions.setOption(params, option, state);
	}
	/**
	 * Read an option
	 * @param option: a resource representing the option
	 * @return the option value
	 */
	public boolean getOption(Node option) {
		return StandardFunctorActions.getOption(params, option);
	}

	public OntModel run(String source, String base, String namespace, Logger errors) throws IOException {
		GraphMem axioms = new GraphMem();
		axioms.getBulkUpdateHandler().add(params);
		
		SplitReader reader = new SplitReader(source);
		if(base != null)
			reader.assignQuote(Node.createURI(SplitBase.SPLITMODEL+"base_model"), base);
		
		Extractor extractor = new Extractor( reader, axioms, rules, functors);
		extractor.run();
		
		Graph result = extractor.getResult();
		logProblems(errors, result);
		return ModelFactory.createMem(result);
	}
}
