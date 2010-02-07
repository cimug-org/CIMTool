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
import au.com.langdale.kena.OntModel;
import au.com.langdale.validation.ModelValidator;
import au.com.langdale.validation.ValidatorUtil;
import au.com.langdale.validation.ValidatorUtil.ValidatorProtocol;


/**
 * Buildlet generates diagnostics directly from a CIM/XML instance. 
 */
public class ValidationBuildlet extends ValidationBaseBuildlet {

	@Override
	protected boolean isInstanceResource(IResource file) {
		return Info.isInstance(file);
	}

	@Override
	protected ValidatorProtocol getValidator(OntModel schema, InputStream ruleText) throws ParserException, IOException {
		if( ruleText == null)
			ruleText = ValidatorUtil.openStandardRules("cimtool-simple");
		return new ModelValidator(schema, ruleText);
	}
}
