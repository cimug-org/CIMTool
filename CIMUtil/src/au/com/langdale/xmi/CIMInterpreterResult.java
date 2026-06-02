/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.kena.OntModel;

import java.util.List;

public class CIMInterpreterResult {

	private OntModel model;
	private List<RuleViolation> violations;

	public CIMInterpreterResult(OntModel model, List<RuleViolation> violations) {
		super();
		this.model = model;
		this.violations = violations;
	}

	public OntModel getModel() {
		return model;
	}

	public List<RuleViolation> getRuleViolations() {
		return violations;
	}

}
