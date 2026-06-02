/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.extensions;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.PlantUMLUtils;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.xmi.UML;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.DefaultRulesEngine;

import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * An rules class for aggregating rules that specifically target a UML class
 * that is a "shadow class" extension of a specific normative CIM class.
 */
@Rule(name = "Shadow Extension", description = "Shadow extensions aggregate ruleset.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.ShadowClass, errorTemplate = "Shadow extension {name} has {count} modelling violations.")
public class ShadowExtensionRule extends OntResourceBaseRule {

	public ShadowExtensionRule(String baseURI) {
		super(baseURI);
	}

	protected RuleViolation createRuleViolation(OntResource resource, String plantUML,
			Map<String, List<String>> namesMap, List<RuleViolation> shadowExtensionViolations) {
		return new RuleViolation( //
				getRuleId(), //
				getCompositeRuleId(), //
				getCompositeSubRuleId(), //
				getRuleType(), //
				getRuleCategory(), //
				getRuleErrorMsg(resource, namesMap, shadowExtensionViolations), //
				resource.getURI(), //
				resource.getURI().substring(0, resource.getURI().lastIndexOf("#") + 1), //
				resource.getLabel(), //
				getViolationSeverity(), //
				isNormative(resource), //
				getPackageHierarchy(resource), //
				getPlaceholderValues(resource, namesMap), //
				plantUML, //
				shadowExtensionViolations);
	}

	protected String getRuleErrorMsg(OntResource resource, Map<String, List<String>> namesMap,
			List<RuleViolation> shadowExtensionViolations) {
		String template = getErrorTemplate();
		Map<String, String> placeholderValues = getPlaceholderValues(resource, namesMap);
		placeholderValues.put("count", Integer.toString(shadowExtensionViolations.size()));
		if (template != null) {
			for (Map.Entry<String, String> entry : placeholderValues.entrySet()) {
				template = template.replace("{" + entry.getKey() + "}", entry.getValue());
			}
		}
		return template;
	}

	/**
	 * This method is responsible for determining if the OntResource passed in
	 * represents a shadow class. If so true is returned so that the rule extending
	 * this class can be executed.
	 * 
	 * There are two types of checks required when determining if a shadow class:
	 * 
	 * 1. If there is a <<ShadowExtension>> stereotype explicitly declared on the
	 * class then it is inherently a shadow class and no other checks are needed.
	 * 
	 * 2. For a class that does not have an explicitly declared <<ShadowExtension>>
	 * stereotype but which exist in a non-CIM namespace; it can still be considered
	 * a shadow class if it has a child class with an identical name which exists in
	 * the baseURI namespace (i.e. the child class is a normative CIM class). By
	 * definition this is an implied shadow class.
	 * 
	 * @param resource The OntSources to be tested to determine if it is a shadow
	 *                 class.
	 * @param baseURI  The base URI of the CIM schema (e.g.
	 *                 http://iec.ch/TC57/CIM100#)
	 * @return True if the resource represents a shadow class; false otherwise.
	 */
	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isClass(resource) || isEnumeration(resource)) {
			if (resource.hasProperty(UML.hasStereotype, UML.shadowextension))
				return true;
			if (!isNormative(resource) && !resource.getURI().startsWith(getBaseURI())) {
				OntModel model = resource.getOntModel();
				ResIterator it = model.listSubjectsWithProperty(RDFS.subClassOf, resource);
				while (it.hasNext()) {
					OntResource subclass = it.nextResource();
					if (isNormative(subclass) && subclass.getURI().endsWith("#" + resource.getLabel())
							&& subclass.getURI().startsWith(getBaseURI())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Action
	public void then(@Fact("resource") OntResource resource, @Fact("namesMap") Map<String, List<String>> namesMap,
			@Fact("violations") List<RuleViolation> violations) {

		List<RuleViolation> shadowClassViolations = new ArrayList<RuleViolation>();

		Rules rules = ExtensionRulesRegistry.getShadowExtensionRules(getBaseURI());
		DefaultRulesEngine rulesEngine = new DefaultRulesEngine();

		List<OntResource> relatedResources = getRelatedResources(resource);

		for (OntResource relatedResource : relatedResources) {
			Facts facts = new Facts();
			facts.put("resource", relatedResource);
			facts.put("namesMap", namesMap);
			facts.put("violations", shadowClassViolations);
			rulesEngine.fire(rules, facts);
		}

		if (shadowClassViolations.size() > 0) {
			String plantUMLDiagram = PlantUMLUtils.generateShadowClassPlantUMLDiagram(resource, shadowClassViolations);
			RuleViolation violation = createRuleViolation(resource, plantUMLDiagram, namesMap, shadowClassViolations);
			violations.add(violation);
		}
	}

	private List<OntResource> getRelatedResources(OntResource shadowClass) {
		// Next we gather a list of all attributes, associations, subclasses related to
		// the specified shadow class...
		List<OntResource> allResources = new ArrayList<>();
		//
		ResIterator resources = shadowClass.getOntModel().listSubjectsBuffered(RDFS.domain, shadowClass);
		resources.forEachRemaining(item -> {
			OntResource resource = (OntResource) item;
			allResources.add(resource);
		});
		//
		resources = shadowClass.getOntModel().listSubjectsBuffered(RDFS.range, shadowClass);
		resources.forEachRemaining(item -> {
			OntResource resource = (OntResource) item;
			allResources.add(resource);
		});
		//
		return allResources;
	}

}