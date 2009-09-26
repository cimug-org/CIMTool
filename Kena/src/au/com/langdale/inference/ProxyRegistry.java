/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.Builtin;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;

/**
 * A registry of proxy functor implementations, needed for creating Rules
 * that contain functor clauses. 
 * 
 * Each proxy functor implementation is a dummy that serves only to verify 
 * that the functor has some implementation.  
 * 
 * Use this when the real implementation is provided as a FunctorActions instance.
 *  
 */
public class ProxyRegistry extends BuiltinRegistry {
	private static class Proxy implements Builtin {
		
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
			return false;
		}

		public int getArgLength() {
			return 0;
		}

		public String getName() {
			return null;
		}

		public String getURI() {
			return null;
		}

		public void headAction(Node[] args, int length, RuleContext context) {
		}

		public boolean isMonotonic() {
			return false;
		}

		public boolean isSafe() {
			return false;
		}
	}
	
	public static final Builtin IMPLIMENTED = new Proxy();
	public static final Builtin UNIMPLIMENTED = new Proxy();
	
	private boolean matchAll;
	
	public ProxyRegistry(Collection names) {
		for (Iterator it = names.iterator(); it.hasNext();) {
			String name = (String) it.next();
			builtins.put(name, IMPLIMENTED);
		}
	}
	
	public ProxyRegistry() {
		matchAll = true;
	}

	@Override
	public Builtin getImplementation(String name) {
		Builtin result = (Builtin)builtins.get(name);
		if( result == null && matchAll) {
			result = UNIMPLIMENTED;
			builtins.put(name, result);
		}
		return result;
	}
	
	
}
