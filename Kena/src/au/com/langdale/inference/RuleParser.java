/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.PrintUtil;
/**
 * Parser for the rule language, liberally adapted from Jena with additions
 * and a full lexical analyser.
 * 
 */
public class RuleParser {
	/**
	 * Reports a parse error with line number.
	 */
	public static class ParserException extends Exception {
		private static final long serialVersionUID = 3922830906543653802L;
		private int line;

		public ParserException(String message, RuleParser parser) {
			super(message);
			line = parser.getLineNumber();
		}
		
		@Override
		public String getMessage() {
			return super.getMessage() + " line: " + line;
		}

		public int getLine() {
			return line;
		}
	}
    
    private static InputStream openURL(String pathname) throws IOException,
			MalformedURLException {
		InputStream stream;
		if( pathname.startsWith("/"))
			stream = RuleParser.class.getResourceAsStream(pathname);
		else 
			stream = new URL(pathname).openStream();
		
		if( stream == null )
			throw new IOException("resource could not be found:" + pathname);
		return stream;
	}
    
    /** Tokenizer */
    private RuleLexer lexer;
    
    /** Look ahead, null if none */
    private String lookahead;
    
    /** Local prefix map */
    private PrefixMapping prefixMapping;

	private BuiltinRegistry registry;
	
	private boolean debug;
   
	/**
	 * Prepare to parse.
	 * @param reader: the input character stream.
	 * @param prefixes: a set of URI prefixes that may be used in rules. 
	 * This map is expanded with any prefixes declared in the rule text. 
	 * @param registry: the registery of functor implementations 
	 * @throws IOException
	 */
	public RuleParser(Reader reader, PrefixMapping prefixes, BuiltinRegistry registry) throws IOException {
    	lexer = new RuleLexer( reader );
    	lookahead = null;
    	this.registry = registry;
    	prefixMapping = prefixes;
	}
	/**
	 * Parse from a byte stream assuming default encoding.
	 */
    public RuleParser(InputStream stream, PrefixMapping prefixes, BuiltinRegistry registry) throws IOException {
    	this( new BufferedReader(new InputStreamReader(stream, "UTF-8")), prefixes, registry);
    }
	/**
	 * Parse from a byte stream assuming default encoding and no predefined prefixes.
	 */
    public RuleParser(InputStream stream, BuiltinRegistry registry) throws IOException {
        this(stream,  PrefixMapping.Factory.create(), registry);
    }
    
    public RuleParser(InputStream stream) throws IOException {
    	this(stream, new ProxyRegistry());
    }
    
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public boolean getDebug() {
		return debug;
	}
	private int getLineNumber() {
		return lexer.getLineNumber();
	}

	/**
     * Register a new namespace prefix with the parser
     */
    public void registerPrefix(String prefix, String namespace ) {
        prefixMapping.setNsPrefix(prefix, namespace);
    }
    
    /**
     * Register a set of prefix to namespace mappings with the parser
     */
    public void registerPrefixMap(Map map) {
        prefixMapping.setNsPrefixes(map);
    }
    
    /**
     * Return a map of all the discovered prefixes
     */
    public Map getPrefixMap() {
        return prefixMapping.getNsPrefixMap();
    }
    
    /**
     * Advance past the current token.
     */
    private void next() throws IOException {
        if (lookahead != null) {
            lookahead = null;
        } else {
            lookahead = lexer.nextToken();
        }
    }
    
    /**
     * Return the current token.
     */
    private String get() throws IOException {
        if (lookahead == null) {
        	lookahead = lexer.nextToken();
        }
        return lookahead;
    }
    
    private List comments() throws IOException {
    	get();
    	return lexer.getComments();
    }
    
    /**
     * Find the variable index for the given variable name
     * and return a Node_RuleVariable with that index.
     */
    private Node_RuleVariable getNodeVar(Map varMap, String name) {
        Node_RuleVariable node = (Node_RuleVariable)varMap.get(name);
        if (node == null) {
            node = new Node_RuleVariable(name, varMap.size());
            varMap.put(name, node);
        }
        return node;
    }

    /**
     * Parse a single variable, qname, uri, literal string, number or functor.
     */
    private Node parseNode(Map varMap) throws ParserException, DatatypeFormatException, IOException {
    	Node node = parseLiteral();
    	if( node != null)
    		return node;

    	node = parseBasicNode(varMap);
    	if( node != null)
    		return node;

    	String token = get();
    	
        if(token.equals("*")) {
        	next();
        	return AsyncModel.WILDCARD;
        } 

        if ( Character.isDigit(token.charAt(0)) || 
                     (token.charAt(0) == '-' && token.length() > 1 && Character.isDigit(token.charAt(1))) ) {
        	next();
           return parseNumber(token);
        } 

        if(token.length() > 0 && Character.isLetter(token.charAt(0))) {
        	next();
        	if (get().equals("(")) {
            	Functor f = new Functor(token, parseNodeList(varMap), registry);
            	return Functor.makeFunctorNode( f );
        	}
        	else
        		throw new ParserException("Expected '(' to define functor", this);
        }
        
        throw new ParserException("Expected a variable, resource, literal or functor", this);
    }
    
    private Node parseLiteral() throws IOException, ParserException {
    	String token = get();

        if (token.startsWith("'") && token.endsWith("'")
        			|| token.startsWith("\"") && token.endsWith("\"")) {
            // A plain literal
            next();
            String lit = token.substring(1, token.length()-1);

            // Check for an explicit datatype
            if (get().equals("^^")) {
            	next();
             
            	String dtURI = get();
                if (dtURI.indexOf(':') != -1) {
                	next();
                	
                    // Thanks to Steve Cranefield for pointing out the need for prefix expansion here
                    String exp = prefixMapping.expandPrefix(dtURI); // Local map first
                    exp = PrintUtil.expandQname(exp);  // Retain global map for backward compatibility
                    if (exp == dtURI) {
                        // No expansion was possible
                        String prefix = dtURI.substring(0, dtURI.indexOf(':'));
                        if (prefix.equals("http") || prefix.equals("urn") 
                         || prefix.equals("ftp") || prefix.equals("mailto")) {
                            // assume it is all OK and fall through
                        } else {
                            // Likely to be a typo in a qname or failure to register
                            throw new ParserException("Unrecognized qname prefix (" + prefix + ") in rule", this);
                        }
                    } else {
                        dtURI = exp;
                    }
                }
                else {
                	throw new ParserException("Expected a datatype name following '^^'", this); 
                }
                RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(dtURI);
                return Node.createLiteral(lit, "", dt);
            } else {
                return Node.createLiteral(lit, "", false);
            }                
        } 
		return null;
	}

	private Node parseBasicNode(Map varMap) throws IOException, ParserException {
    	String token = get();
        if (token.startsWith("?")) {
        	next();
            return getNodeVar(varMap, token);
        } 

        if (token.startsWith("<") && token.endsWith(">")) {
        	next();
            String uri = token.substring(1, token.length()-1).trim();
            return Node.createURI(uri);
        } 

        if (token.indexOf(':') != -1) {
        	next();
            String exp = prefixMapping.expandPrefix(token); // Local map first
            exp = PrintUtil.expandQname(exp);  // Retain global map for backward compatibility
            if (exp == token) {
                // No expansion was possible
                String prefix = token.substring(0, token.indexOf(':'));
                if (prefix.equals("http") || prefix.equals("urn") || prefix.equals("file")
                 || prefix.equals("ftp") || prefix.equals("mailto")) {
                    // assume it is all OK and fall through
                } else {
                    // Likely to be a typo in a qname or failure to register
                    throw new ParserException("Unrecognized qname prefix (" + prefix + ") in rule", this);
                }
            }
            return Node.createURI(exp);
        }
		
        return null;
	}

	/**
     * Turn a possible numeric token into typed literal else a plain literal
     */
    private Node parseNumber(String lit) {
        if ( Character.isDigit(lit.charAt(0)) || 
            (lit.charAt(0) == '-' && lit.length() > 1 && Character.isDigit(lit.charAt(1))) ) {
            if (lit.indexOf(".") != -1) {
                // Float?
                if (XSDDatatype.XSDfloat.isValid(lit)) {
                    return Node.createLiteral(lit, "", XSDDatatype.XSDfloat);
                }
            } else {
                // Int?
                if (XSDDatatype.XSDint.isValid(lit)) {
                    return Node.createLiteral(lit, "", XSDDatatype.XSDint);
                }
            }
        }
        // Default is a plain literal
        return Node.createLiteral(lit, "", false);
    }
    
    /**
     * Parse a list of nodes delimited by parentheses
      */
    private List parseNodeList(Map varMap) throws ParserException, IOException {
        if (get().equals("(")) 
        	next();
        else 
            throw new ParserException("Expected '('", this);
        
        List nodeList = new ArrayList();
        String token = get();
        while (! token.equals(")")) {
            nodeList.add(parseNode(varMap));
            token = get();
            if( token.equals(",")) {
            	next();
            	token = get();
            }
        }
       	next();
        return nodeList;
    }
    
    /**
     * Parse a triple pattern, a rule or a functor.
     * Return null if none of these is found.
     */
    private ClauseEntry parseClause(Map varMap) throws ParserException, IOException {

        if (get().equals("[")) {
            return parseRule(new HashMap(varMap), true);
        } 

        return parseBasicClause(varMap);
    }
    
    private ClauseEntry parseBasicClause(Map varMap) throws ParserException, IOException {
        String token = get();
        if (token.equals("(")) {
        	return parseTriple(varMap);
        } 
        
        Node quote = parseBasicNode(varMap);
        if( quote != null ) {
        	if( get().equals("^")) {
        		next();
        		ClauseEntry clause = parseBasicClause(varMap);
        		if( clause != null) {
        			return new QuoteClause(quote, clause);
        		}
        		else
        			throw new ParserException("Expected quote, pattern or functor following '^'", this);
        	}
        	else
            	throw new ParserException("Expected '^' to define quoted clause", this);
        }

        if( token.length() > 0 && Character.isLetter(token.charAt(0))) {
            next();
            List args = parseNodeList(varMap);
            return new Functor(token, args, registry);
        }
        return null;
    }
    
    private TriplePattern parseTriple(Map varMap) throws IOException, DatatypeFormatException, ParserException {
		if( get().equals("(")) {
			next();
			Node s = parseNode(varMap);
			if( get().equals(","))
				next();
			Node p = parseNode(varMap);
			if( get().equals(","))
				next();
			if(Functor.isFunctor(s) || Functor.isFunctor(p))
				throw new ParserException("Functors not allowed as subject or predicate", this);
			Node o = parseNode(varMap);
			if( get().equals(")")) {
				next();
				return new TriplePattern(s, p, o);
			}
			throw new ParserException("Expected ')' to complete triple pattern", this);
		}
		throw new ParserException("Expected '(' to define new triple pattern", this);
	}

    /**
     * Execute the parse.
     * @return: A list of Rule objects.
     * @throws IOException
     * @throws ParserException
     */
	public List parse() throws IOException, ParserException {
    	List rules = new ArrayList();
    	
    	String token = get();
    	while( token.length() != 0) {
	    	if( token.equals("@prefix")) {
	    		parsePrefix();
	    	}
	    	else if( token.equals("@include")) {
	    		InputStream stream = openURL(parseInclude());
				RuleParser parser = new RuleParser(stream, prefixMapping, registry);
				rules.addAll(parser.parse());
	    	}
	    	else {
	    		rules.add(parseRule(new HashMap(), false));
	    	}
	    	token = get();
    	}
    	return rules;
    }
    
	private String parseInclude() throws IOException, ParserException {
		next();
		
		String uri = get();
		if( uri.startsWith("<") && uri.endsWith(">")) 
			next();
		else
			throw new ParserException("Invalid uri", this);
		
		if( get().equals("."))  
			next();
		else
			throw new ParserException("Expected '.' to complete include declaration", this);
		
		return uri.substring(1, uri.length()-1).trim();
	}

	private void parsePrefix() throws IOException, ParserException {
		next();
		String prefix = get();
		if( prefix.endsWith(":")) 
			next();
		else
			throw new ParserException("Invalid prefix", this);
		
		String uri = get();
		if( uri.startsWith("<") && uri.endsWith(">")) 
			next();
		else
			throw new ParserException("Invalid uri", this);
		
		if( get().equals("."))  
			next();
		else
			throw new ParserException("Expected '.' to complete prefix declaration", this);
			
		registerPrefix(prefix.substring(0, prefix.length()-1), uri.substring(1, uri.length()-1).trim());
	}

	/**
     * Parse a rule with any alternative rules.
     */
    private CompoundRule parseRule(Map varMap, boolean nested) throws ParserException, IOException {
    	CompoundRule result;

    	if (get().equals("[")) {
    		next();
    		result = parseBareRule(varMap, nested);
    		if( get().equals("]"))
    			next();
    		else
    			throw new ParserException("Expected a rule clause or ']' to complete rule", this);
    		
    		if( get().equals("||")) {
    			next();
    			if( get().equals("[")) {
    				CompoundRule alt = parseRule(varMap, nested);
    				result = new CompoundRule(result, alt);
    			}
    			else
    				throw new ParserException("Expected '[' to start an alternative rule", this);
    		}
    	}
    	else {
    		result = parseBareRule(varMap, nested);
    		if( get().equals("."))
    			next();
    		else
    			throw new ParserException("Expected a rule clause or '.' to complete rule", this);
    	}
    	return result;
    }
	

    private CompoundRule parseBareRule(Map varMap, boolean nested) throws IOException, ParserException {
    	// Start rule parsing with empty variable table
 
    	String name = parseRuleLabel();
    	List body = parseClauseList(varMap);
    	boolean backwardRule = parseArrow();
    	List head = parseClauseList(varMap);

    	if (backwardRule) {
    		List b = body; body = head; head = b;
        	
        	if(containsRule(head))
        		throw new ParserException("May not nest a rule in the head of a backwards rule", this);
    	} 
    	
    	if(containsRule(body))
    		throw new ParserException("May not nest a rule in the body of another", this);
    	if(containsQuote(head))
    		throw new ParserException("The head of a rule may not contain quoted clauses.", this);
    	
    	CompoundRule rule = new CompoundRule(name, head, body, varMap.keySet().size());
    	rule.setBackward(backwardRule || nested);
    	return rule;
    }

	private boolean containsRule(List body) {
		for (Iterator it = body.iterator(); it.hasNext();) {
			if( it.next() instanceof CompoundRule)
				return true;
		}
		return false;
	}

	private boolean containsQuote(List head) {
		for (Iterator it = head.iterator(); it.hasNext();) {
			if( it.next() instanceof QuoteClause)
				return true;
		}
		return false;
	}

	private String parseRuleLabel() throws IOException {
		String name = null;
    	String token = get();
    	if (token.endsWith(":")) {
    		name = token.substring(0, token.length()-1);
    		next();
    	}
		return name;
	}

	private boolean parseArrow() throws IOException, ParserException {
		boolean backwardRule;
    	String arrow = get();
    	if( arrow.equals("->")) {
    		next();
    		backwardRule = false;
    	}
    	else if( arrow.equals("<-")) {
    		next();
    		backwardRule = true;
    	}
    	else {
    		throw new ParserException("Expected rule clause or '<-' or '->'", this);
    	}
		return backwardRule;
	}

	private List parseClauseList(Map varMap) throws ParserException, IOException {
		List body = new ArrayList();
    	for(;;) {
    		Object clause = parseClause(varMap);
    		if(clause == null)
    			break;
    		body.add(clause);
    		if( debug ) {
    			Functor comment = parseComment(varMap);
    			if( comment != null)
    				body.add(comment);
    		}
    	}
		return body;
	}
	
	private Functor parseComment(Map varMap) throws IOException {
		if( comments().isEmpty())
			return null;
		
		List nodes = new ArrayList();
	
		for( Iterator it = comments().iterator(); it.hasNext();) {
			String next = (String)it.next();
			if( next.startsWith("?") && varMap.containsKey(next)) 
				nodes.add(varMap.get(next));
			else
				nodes.add(Node.createLiteral(next));
		}
		return new Functor("debug", nodes);
	}
	
	public static void main(String[] args) {
		System.out.println("Rule Parser");
		ValidationBuiltins.registerAll();
		List rules;
		try {
			RuleParser parser = new RuleParser(openURL("/au/com/langdale/cim/cimtool-simple.rules"), BuiltinRegistry.theRegistry);
			rules = parser.parse();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (ParserException e) {
			e.printStackTrace();
			return;
		}
		for (Iterator it = rules.iterator(); it.hasNext();) {
			CompoundRule rule = (CompoundRule) it.next();
			System.out.println(rule);
			
		}
	}
}
