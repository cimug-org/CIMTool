/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.engine;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.kena.OntResource;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public interface DBModelRulesValidator {

	public void validatePackage(ResultSet rs, OntResource resource, Map<String, List<String>> packageNamesMap,
			List<RuleViolation> violations);

	public void validateClass(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap,
			List<RuleViolation> violations);

	public void validateAttribute(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap,
			List<RuleViolation> violations);

	public void validateAssociation(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap,
			List<RuleViolation> violations);

}