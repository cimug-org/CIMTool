/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.extensions;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Shadow Extensions Packaging", description = "Shadow classes must be contained in extension packages and not within normative CIM packages.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Package, errorTemplate = "{elementType} {name} is declared as a «ShadowExtension» and therefore must be located within an extensions package and not within the normative CIM package {packageHierarchy}.")
public class ShadowClassMustBeInExtensionsPackage extends OntResourceBaseRule {

	public ShadowClassMustBeInExtensionsPackage(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (resource.isClass() && resource.hasProperty(UML.hasStereotype, UML.shadowextension)
				&& isNormative(resource)) {
			return true;
		}
		return false;
	}

}