package com.hp.hpl.jena.rdf.arp.states;

import com.hp.hpl.jena.rdf.arp.impl.AbsXMLContext;

/**
 * Modified parser Frame to support RDF quoting. 
 */
public class WantQuotedDescription extends WantDescription {

	public WantQuotedDescription(FrameI s, AbsXMLContext x) {
		super(s, x);
	}
}
