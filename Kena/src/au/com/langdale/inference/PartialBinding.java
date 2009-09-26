/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.reasoner.rulesys.impl.BindingVector;
/**
 * A <code>BindingEnvironment</code> for a <code>Rule</code> that 
 * supports nested rules.   
 */
public class PartialBinding extends BindingVector {

	public PartialBinding(int size) {
		super(size);
	}

	public PartialBinding(Node[] env) {
		super(env);
	}

	public PartialBinding(BindingVector clone) {
		super(clone);
	}

	public PartialBinding(BindingVector clone, int numVars) {
		super(numVars);
		Node[] orig = clone.getEnvironment();
        System.arraycopy(orig, 0, environment, 0, Math.min(numVars, orig.length)); 
		
	}

	@Override
	public Node getBinding(Node node) {
		if (node instanceof Node_RuleVariable) {
			Node_RuleVariable var = (Node_RuleVariable) node;
			if( var.getIndex() >= environment.length)
				return null;
			
		}
		return super.getBinding(node);
	}

}
