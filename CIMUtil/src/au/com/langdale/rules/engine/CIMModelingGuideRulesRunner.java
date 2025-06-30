package au.com.langdale.rules.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;

import au.com.langdale.kena.OntResource;
import au.com.langdale.rules.rules.ConflictingPropertyTypesRule;
import au.com.langdale.rules.rules.DuplicatedLabelRule;
import au.com.langdale.rules.rules.MissingClassCommentRule;
import au.com.langdale.rules.rules.MissingDomainRule;
import au.com.langdale.rules.rules.MissingEnumerationCommentRule;
import au.com.langdale.rules.rules.MissingRangeRule;
import au.com.langdale.rules.rules.RestrictionWithoutPropertyRule;
import au.com.langdale.rules.rules.Rule038_ClassNameUpperCamelCaseRule;
import au.com.langdale.rules.rules.Rule039_ClassNameBritishEnglishRule;
import au.com.langdale.rules.rules.Rule040_ClassNameUniquenessRule;
import au.com.langdale.rules.rules.Rule042_ClassNameSingularRule;
import au.com.langdale.rules.rules.Rule043_ClassCIMDatatypeStandardAttributesRule;
import au.com.langdale.rules.rules.Rule046_ClassNoRelationshipsForDataypeClassesRule;
import au.com.langdale.rules.rules.Rule047_ClassOnlyUsedAsAttributeDatatypeRule;
import au.com.langdale.rules.rules.UnusedClassRule;

public class CIMModelingGuideRulesRunner {

	private Rules generalRules;
	private RulesEngine generalRulesEngine;
	
	private Rules classRules;
	private RulesEngine classRulesEngine;

	private Rules attributeRules;
	private RulesEngine attributeRulesEngine;

	private Rules extensionsRules;
	private RulesEngine extensionsRulesEngine;

	public CIMModelingGuideRulesRunner() {
		generalRules = new Rules();
		generalRulesEngine = new DefaultRulesEngine();

		generalRules.register(new MissingDomainRule());
		generalRules.register(new MissingRangeRule());
		generalRules.register(new UnusedClassRule());
		generalRules.register(new RestrictionWithoutPropertyRule());
		generalRules.register(new ConflictingPropertyTypesRule());
		generalRules.register(new DuplicatedLabelRule());
		generalRules.register(new MissingClassCommentRule());
		generalRules.register(new MissingEnumerationCommentRule());

		/**
		 * https://cim-mg.ucaiug.io/latest/section5-cim-uml-modeling-rules-and-recommendations/#class-rules
		 */
		classRules = new Rules();
		classRulesEngine = new DefaultRulesEngine();
		
		classRules.register(new Rule038_ClassNameUpperCamelCaseRule());
		classRules.register(new Rule039_ClassNameBritishEnglishRule());
		classRules.register(new Rule040_ClassNameUniquenessRule());
		classRules.register(new Rule042_ClassNameSingularRule());
		classRules.register(new Rule043_ClassCIMDatatypeStandardAttributesRule());
		classRules.register(new Rule046_ClassNoRelationshipsForDataypeClassesRule());
		classRules.register(new Rule047_ClassOnlyUsedAsAttributeDatatypeRule());
	}

	public List<String> validate(Collection<OntResource> resources, String baseURL) {
		List<String> issues = new ArrayList<>();
		Map<String, String> labelMap = new HashMap<>();

		// Preload label map
		for (OntResource res : resources) {
			String label = res.getLabel();
			if (label != null && !label.trim().isEmpty()) {
				labelMap.putIfAbsent(label, res.getURI());
			}
		}

		for (OntResource res : resources) {
			Facts facts = new Facts();
			facts.put("resource", res);
			facts.put("issues", issues);
			facts.put("labelMap", labelMap);
			facts.put("baseURL", baseURL);

			classRulesEngine.fire(classRules, facts);
		}

		return issues;
	}
}
