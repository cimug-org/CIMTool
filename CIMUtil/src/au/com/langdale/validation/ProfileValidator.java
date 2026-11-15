/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;


import au.com.langdale.inference.Extractor;
import au.com.langdale.inference.GraphAdapter;
import au.com.langdale.inference.LOG;
import au.com.langdale.inference.RepairLibrary;
import au.com.langdale.inference.Reporting;
import au.com.langdale.inference.RuleParser;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.inference.StandardFunctorActions;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.transitiveReasoner.TransitiveReasoner;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

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
		checkSuperClassCycles(extractor.getResult(), trans);
		log = ModelFactory.createMem(extractor.getResult());
	}

	private static final String CYCLE_PHRASE = "has a cyclic superclass chain through";

	/**
	 * Report each named profile class whose superclass chain, through other
	 * named profile classes, leads back to itself (issue #387). Each superclass
	 * statement on such a chain is reported, with a repair that removes it.
	 * A class that names itself as its superclass is ignored, as it is by the
	 * consistency rules.
	 */
	private void checkSuperClassCycles(Graph result, Graph axioms) {
		ResIterator classes = target.listSubjectsWithProperty(RDF.type, OWL.Class);
		while (classes.hasNext()) {
			OntResource clss = classes.nextResource();
			if (clss.isAnon())
				continue;
			for (OntResource superClass : profileSuperClasses(clss)) {
				if (leadsTo(superClass, clss)) {
					Node[] args = new Node[] { clss.asNode(), Node.createLiteral(CYCLE_PHRASE), superClass.asNode() };
					Node report = Reporting.createReport(result, clss.asNode(), args[1], args, 0, args.length);
					new RepairLibrary.Remove().apply(
							new Node[] { report, clss.asNode(), RDFS.subClassOf.asNode(), superClass.asNode() },
							result, axioms, null);
				}
			}
		}
	}

	/** The named profile classes {@code clss} directly declares as superclasses. */
	private List<OntResource> profileSuperClasses(OntResource clss) {
		List<OntResource> result = new ArrayList<OntResource>();
		ResIterator it = clss.listProperties(RDFS.subClassOf);
		while (it.hasNext()) {
			OntResource superClass = it.nextResource();
			if (!superClass.isAnon() && !superClass.equals(clss) && target.contains(superClass, RDF.type, OWL.Class))
				result.add(superClass);
		}
		return result;
	}

	/** Whether following profile superclasses from {@code start} reaches {@code goal}. */
	private boolean leadsTo(OntResource start, OntResource goal) {
		Set<OntResource> visited = new HashSet<OntResource>();
		List<OntResource> pending = new ArrayList<OntResource>();
		pending.add(start);
		while (!pending.isEmpty()) {
			OntResource current = pending.remove(pending.size() - 1);
			if (current.equals(goal))
				return true;
			if (visited.add(current))
				pending.addAll(profileSuperClasses(current));
		}
		return false;
	}

}