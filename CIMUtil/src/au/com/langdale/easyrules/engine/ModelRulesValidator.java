/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.engine;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.kena.OntModel;

import java.util.List;

public interface ModelRulesValidator {

	public List<RuleViolation> validate(OntModel model);

}