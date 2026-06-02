/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getLabelDefaultUnknown;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Association rules would typically have a @Condition (when) method that
 * operate on resources that are verified to be associations i.e.
 * isAssociation(resource) is true. However, for this particular rule it is more
 * straightforward to work with class resources since we must take into
 * consideration ShadowExtension classes and any associations that may exist on
 * them. Note that there may be more than one parent ShadowExtension class for a
 * normative CIM class and rule violations could occur between associations
 * spanning these classes as well. So
 */
@Rule(name = "Extension:Rule198", description = "[Rule198] In instances where there are multiple associations between extension classes, all role names shall be different.")
@RuleMetadata(compositeRule = "Extension", compositeSubRule = "Rule198", type = RuleType.Extension, category = RuleCategory.Association, errorTemplate = "In instances where there are multiple associations between two extensions classes, all role names shall be different. {elementType} {name} has the following model violations:\n {associationViolations}")
public class Extension_Rule198_AssociationMultipleAssociationsBetweenTwoClassesWithDuplicateName extends OntResourceBaseRule {

	public Extension_Rule198_AssociationMultipleAssociationsBetweenTwoClassesWithDuplicateName(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (!isRestrictedClass(resource) && !isNormative(resource)) {

			/**
			 * Each of the keys in this map will correspond to a class on the opposite end
			 * of an association that this resource participates in. In turn, each class key
			 * then maps to a corresponding list of one or more associations that are
			 * defined between this resource and the class that is the key. In this way if
			 * the size of any of the association lists is > 1 we know to validate the list
			 * for duplicates.
			 */
			Map<OntResource, List<OntResource>> multAssocationsList = new HashMap<>();

			/**
			 * First, we gather associations directly on the class itself and then
			 * subsequently all associations that may be defined for any ShadowExtension
			 * classes of this class.
			 */
			ResIterator resourcesWithThisAsDomain = resource.getOntModel().listSubjectsWithProperty(RDFS.domain,
					resource);
			while (resourcesWithThisAsDomain.hasNext()) {
				OntResource property = resourcesWithThisAsDomain.nextResource();
				if (isAssociation(property)) {
					OntResource range = property.getRange();
					if (multAssocationsList.containsKey(range)) {
						List<OntResource> list = multAssocationsList.get(range);
						list.add(property);
					} else {
						List<OntResource> list = new ArrayList<>();
						list.add(property);
						multAssocationsList.put(range, list);
					}
				}
			}
			//
			ResIterator superClasses = resource.listSuperClasses(false);
			if (superClasses.hasNext()) {
				while (superClasses.hasNext()) {
					OntResource superClass = superClasses.nextResource();
					if (isShadowClass(getBaseURI(), superClass)) {
						resourcesWithThisAsDomain = resource.getOntModel().listSubjectsWithProperty(RDFS.domain,
								superClass);
						while (resourcesWithThisAsDomain.hasNext()) {
							OntResource property = resourcesWithThisAsDomain.nextResource();
							if (isAssociation(property)) {
								OntResource range = property.getRange();
								if (multAssocationsList.containsKey(range)) {
									List<OntResource> list = multAssocationsList.get(range);
									list.add(property);
								} else {
									List<OntResource> list = new ArrayList<>();
									list.add(property);
									multAssocationsList.put(range, list);
								}
							}
						}
					}
				}
			}
			//
			for (OntResource aClass : multAssocationsList.keySet()) {
				List<OntResource> associations = multAssocationsList.get(aClass);
				if (associations.size() > 1) {
					Set<String> dedupedRoleEndNames = new HashSet<>();
					for (OntResource association : associations) {
						String roleEnd = getLabelDefaultUnknown(association);
						dedupedRoleEndNames.add(roleEnd);
						// Get the role end of the other side of the association via the inverse.
						OntResource inverse = association.getInverse();
						String inverseRoleEnd = getLabelDefaultUnknown(inverse);
						dedupedRoleEndNames.add(inverseRoleEnd);
					}
					if (dedupedRoleEndNames.size() < (associations.size() * 2))
						return true;
				}
			}
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		//
		Map<OntResource, List<OntResource>> assocationsList = new HashMap<>();
		//
		ResIterator resourcesWithThisAsDomain = resource.getOntModel().listSubjectsWithProperty(RDFS.domain, resource);
		while (resourcesWithThisAsDomain.hasNext()) {
			OntResource property = resourcesWithThisAsDomain.nextResource();
			if (isAssociation(property)) {
				OntResource range = property.getRange();
				if (assocationsList.containsKey(range)) {
					List<OntResource> list = assocationsList.get(range);
					list.add(property);
				} else {
					List<OntResource> list = new ArrayList<>();
					list.add(property);
					assocationsList.put(range, list);
				}
			}
		}
		//
		ResIterator superClasses = resource.listSuperClasses(false);
		if (superClasses.hasNext()) {
			while (superClasses.hasNext()) {
				OntResource superClass = superClasses.nextResource();
				if (isShadowClass(getBaseURI(), superClass)) {
					resourcesWithThisAsDomain = resource.getOntModel().listSubjectsWithProperty(RDFS.domain,
							superClass);
					while (resourcesWithThisAsDomain.hasNext()) {
						OntResource property = resourcesWithThisAsDomain.nextResource();
						if (isAssociation(property)) {
							OntResource range = property.getRange();
							if (assocationsList.containsKey(range)) {
								List<OntResource> list = assocationsList.get(range);
								list.add(property);
							} else {
								List<OntResource> list = new ArrayList<>();
								list.add(property);
								assocationsList.put(range, list);
							}
						}
					}
				}
			}
		}
		//
		StringBuffer associationViolations = new StringBuffer();
		for (OntResource aClass : assocationsList.keySet()) {
			List<OntResource> associations = assocationsList.get(aClass);
			if (associations.size() > 1) {
				List<String> allRoleNames = new ArrayList<>();
				Set<String> dedupedRoleEndNames = new HashSet<>();
				for (OntResource association : associations) {
					String roleEnd = getLabelDefaultUnknown(association);
					dedupedRoleEndNames.add(roleEnd);
					allRoleNames.add(roleEnd);
					// Get the role end of the other side of the association via the inverse.
					OntResource inverse = association.getInverse();
					String inverseRoleEnd = getLabelDefaultUnknown(inverse);
					dedupedRoleEndNames.add(inverseRoleEnd);
					allRoleNames.add(inverseRoleEnd);
				}
				if (dedupedRoleEndNames.size() < (associations.size() * 2)) {
					LinkedHashSet<String> namesAlreadyObserved = new LinkedHashSet<>();
					LinkedHashSet<String> duplicatedRoleNames = new LinkedHashSet<>();
					//
					for (String roleName : allRoleNames) {
						if (roleName == null)
							continue;
						if (!namesAlreadyObserved.add(roleName)) {
							duplicatedRoleNames.add(roleName);
						}
					}
					//
					StringBuffer theFullyQualifiedClassName = new StringBuffer();
					String thePackageHierarchy = getPackageHierarchy(aClass);
					theFullyQualifiedClassName
							.append((!thePackageHierarchy.isBlank() ? thePackageHierarchy + "::" : ""))
							.append(getLabelDefaultUnknown(aClass));
					//
					associationViolations.append("\n").append("- associations to class ")
							.append(applyAsciidocStyling(theFullyQualifiedClassName.toString()))
							.append(" have duplicate role name(s): ").append(duplicatedRoleNames.toString());
				}
			}
		}
		//
		values.put("associationViolations", associationViolations.toString());
		//
		return values;
	}

}