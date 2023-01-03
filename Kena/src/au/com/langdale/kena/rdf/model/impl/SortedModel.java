package au.com.langdale.kena.rdf.model.impl;

import com.hp.hpl.jena.enhanced.BuiltinPersonalities;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;

/**
 * Common methods for model implementations.
 * 
 * <P>
 * This class implements common methods, mainly convenience methods, for model
 * implementations. It is intended use is as a base class from which model
 * implemenations can be derived.
 * </P>
 * 
 * @author bwm hacked by Jeremy, tweaked by Chris (May 2002 - October 2002)
 */

public class SortedModel extends ModelCom {

	public SortedModel(Model model) {
		super(model.getGraph(), BuiltinPersonalities.model);
	}

	public StmtIterator listStatements(Resource s, Property p, RDFNode o) {
		return new SortedStmtIteratorImpl(super.listStatements(s, p, o));
	}

}
