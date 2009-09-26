/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import au.com.langdale.inference.RuleParser;
import au.com.langdale.inference.SimpleInfGraph;
import au.com.langdale.inference.SimpleReasoner;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.util.Formater;
import au.com.langdale.util.Logger;
import au.com.langdale.util.Profiler.TimeSpan;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public abstract class ValidatorUtil  {

	private static TypeMapper types = TypeMapper.getInstance();
	private static Node XSDStringType = XSD.xstring.asNode();

	public interface ValidatorProtocol {
	    /**
	 	 * Execute validation for the given inputs and output.  This method can
	 	 * be called more than once to apply a given set of rules to more than
	 	 * one input.
	 	 * 
		 * @param source: the pathname of the model to validate 
		 * @param base: the pathname of a base model when the source represents an incremental model
		 * @param namespace: not used for split models
		 * @param errors: the destination for errors reports (not validation reports)
		 * @return a diagnostic model
		 * @throws IOException
		 */
		public abstract OntModel run(String source, String base, String namespace, Logger errors) throws IOException;
	}
	
	public static InputStream openStandardRules(String name) {
		return ValidatorUtil.class.getResourceAsStream(name + ".rules");
	}

	public static List expandRules(OntModel schema, String namespace, InputStream ruleText, BuiltinRegistry registry) throws ParserException, IOException  {
		TimeSpan span = new TimeSpan("Parse Rules");
		RuleParser parser = new RuleParser(ruleText, registry);
		parser.registerPrefix("topol", guessTopolNameSpace(schema));
		List rules = parser.parse();

		span = span.start("Expand Rules");
		SimpleReasoner stage1 = new SimpleReasoner(rules); 
		Graph graph = schema.getGraph();
		SimpleInfGraph deductions = (SimpleInfGraph) stage1.bind(graph);
		List brules = deductions.getBRules();

		debug(graph, rules, brules);
		span.stop();
		return brules;
	}
	
	private static String guessTopolNameSpace(OntModel schema) {
		String ns = "http://langdale.com.au/2008/default_electrical_topology#";
		ResIterator it = schema.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty);
		while( it.hasNext()) {
			OntResource prop = it.nextResource();
			if(prop.getLocalName().equals("Terminal.ConnectivityNode")) {
				ns = prop.getNameSpace();
				break;
			}
		}
		return ns;
	}

	private static void debug(Graph graph, List rules, List brules) {
//		System.out.println("---funny triples---");
//		ExtendedIterator it = graph.find(Node.ANY, RDFS.subClassOf.asNode(), Node.ANY);
//		while (it.hasNext()) {
//			Triple t = (Triple) it.next();
//			if( t.getObject().isBlank())
//				System.out.println(t);
//		}

//		System.out.println("---initial rules---");
//		PrintUtil.printOut(rules.iterator());
//
//		System.out.println("---expanded rules---");
//		PrintUtil.printOut(brules.iterator());
	}

	public static void logProblems(Logger log, Graph deductions) {
		TimeSpan span = new TimeSpan("Generate Report");

		Iterator it = deductions.find(Node.ANY, RDF.type.asNode(), LOG.Problem.asNode());
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			logProblem(log, deductions, t.getSubject());
		}
		span.stop();
	}

	public static void logProblem(Logger log, Graph deductions, Node problem) {
		Node subject = getSubject(deductions, LOG.hasProblems.asNode(), problem, null);
		String key = subject != null && subject.isURI()? subject.getURI(): "";
		String phrase = getString(deductions, problem, RDFS.comment.asNode(), "");
		String detail = getString(deductions, problem, LOG.problemDetail.asNode(), "");
		log.log(phrase + ":\t" + key + "\t" + detail);
	}

	public static boolean isLexicalForm(Node val, Node dt) {
		if (!dt.isURI()) 
			return false;
		if (val.isBlank()) 
			return true;
		if (! val.isLiteral())
			return false;
		if (dt.equals(RDFS.Nodes.Literal) || dt.equals(XSDStringType)) 
			return true;

		RDFDatatype dtype = types.getTypeByName(dt.getURI());
		if( dtype == null)
			return true;

		return dtype.isValid(val.getLiteralLexicalForm());   
	}

	public static Node getReportSubject(Node[] args, int offset1, int offset2) {
		for( int ix = offset1; ix < offset2; ix++) 
			if( args[ix].isURI())
				return args[ix];
		return null;
	}

	public static Node getReportPhrase(String alternate, Node[] args,
			int offset1, int offset2) {
		for( int ix = offset1; ix < offset2; ix++) 
			if( args[ix].isLiteral()) 
				return args[ix];
		return Node.createLiteral(alternate);
	}

	public static Node createReport(Graph graph, Node subject, Node phrase,
			Node[] nodes, int offset1, int offset2) {

		Node report = Node.createAnon();
		graph.add(new Triple(report, RDF.type.asNode(), LOG.Problem.asNode()));
		graph.add(new Triple(report, RDFS.comment.asNode(), phrase));

		Node detail = Node.createLiteral(new Formater().print(nodes, offset1, offset2));
		graph.add(new Triple(report, LOG.problemDetail.asNode(), detail));


		if( subject != null ) {
			graph.add(new Triple(subject, LOG.hasProblems.asNode(), report ));
			for(int ix = offset1; ix < offset2; ix++) {
				if( nodes[ix].isURI() && ! nodes[ix].equals(subject))
					graph.add(new Triple(report, LOG.problemReference.asNode(), nodes[ix]));
			}
		}
		
		return report;
	}

	public static String getString(Graph graph, Node subject, Node prop,
			String alt) {
		ExtendedIterator it = graph.find(subject, prop, Node.ANY);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			Node n = t.getObject();
			if( n.isLiteral()) {
				it.close();
				return n.getLiteralLexicalForm();
			}
		}
		return alt;
	}

	public static Node getSubject(Graph graph, Node prop, Node object, Node alt) {
		ExtendedIterator it = graph.find(Node.ANY, prop, object);
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			Node n = t.getSubject();
			it.close();
			return n;
		}
		return alt;
	}

	public static Node cons(Graph graph, Node first, Node rest) {
		Node head = Node.createAnon();
		graph.add(new Triple(head, RDF.first.asNode(), first));
		graph.add(new Triple(head, RDF.rest.asNode(), rest));
		return head;
	}

}