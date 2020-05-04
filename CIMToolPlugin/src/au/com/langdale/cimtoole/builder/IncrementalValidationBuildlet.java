/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.builder;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IResource;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.inference.StandardFunctorActions;
import au.com.langdale.kena.OntModel;
import au.com.langdale.validation.SplitValidator;
import au.com.langdale.validation.ValidatorUtil;
import au.com.langdale.validation.ValidatorUtil.ValidatorProtocol;

/**
 * The <code>Buildlet</code> for validating an incremental CIM/XML model.
 */
public class IncrementalValidationBuildlet extends ValidationBaseBuildlet {

	@Override
	protected boolean isInstanceResource(IResource resource) {
		return Info.isIncremental(resource);
	}


	@Override
	protected ValidatorProtocol getValidator(OntModel schema, InputStream ruleText) throws ParserException, IOException {
		if( ruleText == null)
			ruleText = ValidatorUtil.openStandardRules("cimtool-inc");
		SplitValidator validator = new SplitValidator(schema, ruleText);
		validator.setOption(StandardFunctorActions.PROBLEM_PER_SUBJECT, Info.getPreferenceOption(Info.PROBLEM_PER_SUBJECT));
		return validator;
	}
}
