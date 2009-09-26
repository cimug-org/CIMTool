/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.BindingEnvironment;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.impl.BindingVector;
/**
 * A Rule whose clauses may include other rules.
 */
public class CompoundRule extends Rule {
	private CompoundRule alternative;

	public CompoundRule(String name, List head, List body, int numVars) {
		super(name, head, body);
		this.numVars = numVars;
	}

	public CompoundRule(String name, ClauseEntry[] head, ClauseEntry[] body, int numVars) {
		super(name, head, body);
		this.numVars = numVars;
	}
	
	public CompoundRule(CompoundRule main, CompoundRule alternative) {
		super(main.name, main.head, main.body);
		this.numVars = main.numVars;
		this.name = main.name;
		this.isBackward = main.isBackward;
		this.alternative = alternative;
	}
	
	public boolean isFunction() {
		if( body.length == 0 || ! (body[0] instanceof Functor))
			return false;
		
		Node[] formals = ((Functor)body[0]).getArgs();
		for(int ix = 0; ix < formals.length; ix++)
			if(! formals[ix].isVariable())
				return false;

		return true;
	}

	@Override
	public int getNumVars() {
		assert numVars > -1;
		return numVars;
	}
	
	public CompoundRule getAlternative() {
		return alternative;
	}

	/**
	 * Instantiate a rule given a variable binding environment.
	 * This will clone any non-bound variables though that is only needed
	 * for trail implementations.
	 */
	@Override
	public Rule instantiate(BindingEnvironment env) {
		env = new PartialBinding(((BindingVector)env).getEnvironment());
	    HashMap vmap = new HashMap();
	    return instantiate(vmap, env);
	}
    
    /**
     * Clone a rule, cloning any embedded variables.
     */
    @Override
	public Rule cloneRule() {
        if (getNumVars() > 0) {
            HashMap vmap = new HashMap();
            return new CompoundRule(name, cloneClauseArray(head, vmap, null), cloneClauseArray(body, vmap, null), numVars);
        } else {
            return this;
        }
    }
 
	private CompoundRule instantiate(HashMap vmap, BindingEnvironment env) {
		return new CompoundRule(name, cloneClauseArray(head, vmap, env), cloneClauseArray(body, vmap, env), numVars);
	}

	/**
	 * Clone a clause array.
	 */
	private ClauseEntry[] cloneClauseArray(ClauseEntry[] clauses, Map vmap, BindingEnvironment env) {
	    ClauseEntry[] cClauses = new ClauseEntry[clauses.length];
	    for (int i = 0; i < clauses.length; i++ ) {
	        cClauses[i] = cloneClause(clauses[i], vmap, env);
	    }
	    return cClauses;
	}

	/**
	 * Clone a clause, cloning any embedded variables.
	 */
	private ClauseEntry cloneClause(ClauseEntry clause, Map vmap, BindingEnvironment env) {
	    if (clause instanceof TriplePattern) {
	        return cloneTriplePattern((TriplePattern)clause, vmap, env);
	    } else if(clause instanceof Functor ){
	        return cloneFunctor((Functor)clause, vmap, env);
	    }
	    else if(clause instanceof CompoundRule)
	    	return ((CompoundRule)clause).instantiate(new HashMap(vmap), env);
	    else if(clause instanceof QuoteClause)
	    	return cloneQuoteClause((QuoteClause)clause, vmap, env);
	    else
	    	return null;
	}

	private ClauseEntry cloneQuoteClause(QuoteClause clause, Map vmap, BindingEnvironment env) {
		return new QuoteClause(cloneNode(clause.getQuote(), vmap, env), cloneClause(clause.getClause(), vmap, env));
	}

	private TriplePattern cloneTriplePattern(TriplePattern tp, Map vmap, BindingEnvironment env) {
		return new TriplePattern (
		                cloneNode(tp.getSubject(), vmap, env),
		                cloneNode(tp.getPredicate(), vmap, env),
		                cloneNode(tp.getObject(), vmap, env)
		            );
	}

	/**
	 * Clone a functor, cloning any embedded variables.
	 */
	private Functor cloneFunctor(Functor f, Map vmap, BindingEnvironment env) {
	    Node[] args = f.getArgs();
	    Node[] cargs = new Node[args.length];
	    for (int i = 0; i < args.length; i++) {
	        cargs[i] = cloneNode(args[i], vmap, env);
	    }
	    Functor fn = new Functor(f.getName(), cargs);
	    fn.setImplementor(f.getImplementor());
	    return fn;
	}

	/**
	 * Clone a single node.
	 */
	private Node cloneNode(Node nIn, Map vmap, BindingEnvironment env) {
	    Node n = (env == null) ? nIn : env.getGroundVersion(nIn);
	    if (n instanceof Node_RuleVariable) {
	        Node_RuleVariable nv = (Node_RuleVariable)n;
	        Node c = (Node)vmap.get(nv);
	        if (c == null) {
	            c = nv.cloneNode();
	            vmap.put(nv, c);
	        }
	        return c;
	    } else if (Functor.isFunctor(n)) {
	        Functor f = (Functor)n.getLiteralValue();
	        return Functor.makeFunctorNode(cloneFunctor(f, vmap, env));
	    } else {
	        return n;
	    }
	}
	
	@Override
	public String toString() {
		if( alternative == null )
			return super.toString();
		else
			return super.toString() + "||" + alternative.toString();
	}

}
