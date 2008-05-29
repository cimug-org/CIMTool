/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.util;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.langdale.validation.Validation;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_ANY;
import com.hp.hpl.jena.graph.Node_Literal;
import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.DAML_OIL;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * A collection of small utilites for pretty printing nodes, triples
 * and associated things. The core functionality here is a static
 * prefix map which is preloaded with known prefixes.
 * 
 * <p>updated by Chris March 2004 to use a PrefixMapping rather than the
 * specialised tables.
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.21 $ on $Date: 2007/01/02 11:52:14 $
 */
public class Formater {
	
	public final String NCNAME_REGEX = Validation.NCNAME_REGEX;
    
    private PrefixMapping prefixMapping = PrefixMapping.Factory.create();
        
    public Formater() {
        registerPrefix("rdf", RDF.getURI());
		registerPrefix("rdfs", RDFS.getURI());
		registerPrefix("drdfs", "urn:x-hp-direct-predicate:http_//www.w3.org/2000/01/rdf-schema#");
		registerPrefix("owl", OWL.getURI());
		registerPrefix("daml", DAML_OIL.NAMESPACE_DAML.getURI());
		registerPrefix("jr", ReasonerVocabulary.getJenaReasonerNS());
		registerPrefix("rb", ReasonerVocabulary.getRBNamespace());
		registerPrefix("xsd", XSDDatatype.XSD + "#");
    }
    
    public PrefixMapping getPrefixMapping() {
		return prefixMapping;
	}

	/**
     * Register a new prefix/namespace mapping which will be used to shorten
     * the print strings for resources in known namespaces.
     * 
     * Ignore this prefix mapping if the prefix is already in use and return false
     * otherwise true.
     */
    public boolean registerPrefix(String prefix, String namespace) {
    	if(prefixMapping.getNsPrefixURI(prefix) == null) {
    		prefixMapping.setNsPrefix( prefix, namespace );
    		return true;
    	}
    	return false;
    }
    
    public String createPrefix(String namespace) {
    	String prefix = prefixMapping.getNsURIPrefix(namespace);
    	if( prefix != null )
    		return prefix;
    	Pattern pattern = Pattern.compile(NCNAME_REGEX);
    	Matcher matcher = pattern.matcher(namespace);
    	while(matcher.find())
    		prefix = matcher.group();
    	if(prefix != null && registerPrefix(prefix, namespace))
    		return prefix;
    	else
    		return null;
    }
    
    /**
     * Return a simplified print string for a Node. 
     */
    public String print(Node node) {
        if (node instanceof Node_URI) {
        	createPrefix(node.getNameSpace());
            return node.toString( prefixMapping );
        } else if (node instanceof Node_Literal) {
            return node.getLiteralLexicalForm();
        } else if (node instanceof Node_ANY) {
            return "*";
        }
        if (node == null) {
            return "null";
        }
        return node.toString();
    }
    
    public String print(Node[] nodes, int offset1, int offset2) {
        StringBuffer desc = new StringBuffer();
        for (int j = offset1; j < offset2; j++) {
        	if(desc.length()>0)
                desc.append( " ");
            desc.append( print(nodes[j]));
        }
        return desc.toString();
    }
    
    /**
     * Return a simplified print string for an RDFNode. 
     */
    public String print(RDFNode node) {
        if (node == null) return "null";
        return print(node.asNode());
    }
    
    /**
     * Return a simplified print string for a Triple
     */
    public String print(Triple triple) {
        if (triple == null) return "(null)";
        return "(" + print(triple.getSubject()) + " " +
                      print(triple.getPredicate()) + " " +
                      print(triple.getObject()) + ")";
    }
    
    /**
     * Return a simplified print string for a TriplePattern
     */
    public String print(TriplePattern triple) {
        if (triple == null) return "(null)";
        return "(" + print(triple.getSubject()) + " " +
                      print(triple.getPredicate()) + " " +
                      print(triple.getObject()) + ")";
    }
    
    /**
     * Return a simplified print string for a statment
     */
    public String print(Statement stmt) {
        if (stmt == null) return "(null)";
        return print(stmt.asTriple());
    }
    
    /**
     * Default print which just uses tostring
     */
    public String print(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Triple) {
            return print((Triple)obj);
        } else if (obj instanceof TriplePattern) {
            return print((TriplePattern)obj);
        } else if (obj instanceof Node) {
            return print((Node)obj);
        } else if (obj instanceof RDFNode) {
            return print((RDFNode)obj);
        } else if (obj instanceof Statement) {
            return print((Statement)obj);
        } else {
            return obj.toString();
        }
    }
    
    /**
     * Print all the Triple values from a find iterator.
     */
    public void printOut(Iterator it) {
        while (it.hasNext()) {
            System.out.println("   " + print(it.next()));
        }
    }
}

/*
    (c) Copyright 2003, 2004, 2005, 2006, 2007 Hewlett-Packard Development Company, LP
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

    3. The name of the author may not be used to endorse or promote products
       derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/