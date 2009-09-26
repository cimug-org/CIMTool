package au.com.langdale.inference;

import au.com.langdale.util.Formater;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class Reporting {

	private static TypeMapper types = TypeMapper.getInstance();
	private static Node XSDStringType = XSD.xstring.asNode();

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

}
