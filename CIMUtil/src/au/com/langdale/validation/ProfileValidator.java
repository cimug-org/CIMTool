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
import au.com.langdale.inference.GraphAdapter;
import au.com.langdale.inference.LOG;
import au.com.langdale.inference.RepairLibrary;
import au.com.langdale.inference.RuleParser;
import au.com.langdale.inference.StandardFunctorActions;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.transitiveReasoner.TransitiveReasoner;

/**
 * Check a profile for consistency with a base schema.
 * 
 */
public class ProfileValidator {
	
	private OntModel log, target, reference;
	private List rules;
	private Map functors;

	/**
	 * Construct validator from models.
	 * 
	 * @param target the profile to be checked
	 * @param ref the base schema
	 * @param namespace the namespace of the profile
	 */
	public ProfileValidator(OntModel target, OntModel reference) {
		this.log = ModelFactory.createMem();
		this.target = target;
		this.reference = reference;
	}
	
	private void buildRules() throws IOException, ParserException {
		if( rules != null)
			return;
		
		InputStream text = ProfileValidator.class.getResourceAsStream("profile.rules");
		functors = StandardFunctorActions.create();
		functors.putAll(RepairLibrary.getFunctorMap());
		
//		for(Iterator it = functors.keySet().iterator(); it.hasNext(); ) {
//			System.out.println( "Profile Functor " + it.next());
//		}
		
		RuleParser parser = new RuleParser(text);
		// parser.setDebug(true);
		//parser.registerPrefix("topol", guessTopolNameSpace(schema));
		rules = parser.parse();
//		for (Iterator it = rules.iterator(); it.hasNext();) {
//			CompoundRule rule = (CompoundRule) it.next();
//			System.out.println(rule);
//		}
	}
	
	/**
	 * Retrieve a model containing error annotations.  For each
	 * inconsistent statement in the target, the log contains one
	 * or more LOG.hasProblems statements.  The subjects of the
	 *  problem is a resource in the target associated with the 
	 * inconsistent statement.  
	 * 
	 * @return the log model
	 */
	public OntModel getLog() {
		return log;
	}

	public boolean hasErrors() {
		return log.listIndividuals(LOG.Problem).hasNext();
	}

	/**
	 * Check a target model for inconsistent use of a namespace.
	 * Any resource in the given namespace that is used in the target
	 * must be defined in the reference. Errors are recorded in the log
	 * model.
	 * 
	 * @return true if there are validation errors
	 * @throws IOException 
	 * @throws ParserException 
	 */
	public void run() throws IOException, ParserException {
		buildRules();
		Graph ref = reference.getGraph();
		assert ! ( ref instanceof InfGraph);
		InfGraph trans = new TransitiveReasoner().bind(ref);
		Extractor extractor = new Extractor(new GraphAdapter(target.getGraph()), trans, rules, functors);
		extractor.run();
		log = ModelFactory.createMem(extractor.getResult());
	}
	
	
}
