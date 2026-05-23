/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.utils;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.xmi.UML;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class PlantUMLUtils {

	private static final String CRLF = System.lineSeparator();
	private static final String RED_FONT_START = "<font color=red>";
	private static final String RED_FONT_END = "</font>";

	/**
	 * The below PlantUML "template" specifically provides the appropriate styling
	 * for emulating a typical UML class diagram as it appears in Sparx EA 16.x.
	 * 
	 * @return
	 */
	public static String generateShadowClassPlantUMLDiagram(OntResource shadowClass, List<RuleViolation> violations) {
		StringBuffer plantUML = new StringBuffer();
		//
		plantUML.append("[plantuml,converter-diagram,svg,width=100%,align=\"center\"]").append(CRLF);
		plantUML.append("----").append(CRLF);
		plantUML.append("@startuml").append(CRLF);
		plantUML.append("top to bottom direction").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("' optional gradient background to simulate EA 16.x diagrams").append(CRLF);
		plantUML.append("skinparam BackgroundColor #FBFBFB-#D6D2CE").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("' Hide class icons").append(CRLF);
		plantUML.append("hide circle").append(CRLF);
		plantUML.append("hide empty methods").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("' This hides the attribute icons").append(CRLF);
		plantUML.append("skinparam classAttributeIconSize 0").append(CRLF);
		plantUML.append("skinparam ArrowColor #454645").append(CRLF);
		plantUML.append("skinparam shadowing true").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("' skinning for notes").append(CRLF);
		plantUML.append("skinparam note {").append(CRLF);
		plantUML.append("  'BackgroundColor #f0f0f0").append(CRLF);
		plantUML.append("  BackgroundColor #fcf8e3").append(CRLF);
		plantUML.append("  BorderColor #454645").append(CRLF);
		plantUML.append("  FontColor #333333").append(CRLF);
		plantUML.append("  FontName Arial").append(CRLF);
		plantUML.append("  FontSize 12").append(CRLF);
		plantUML.append("  BorderThickness 1").append(CRLF);
		plantUML.append("  Shadowing true").append(CRLF);
		plantUML.append("}").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("skinparam class {").append(CRLF);
		plantUML.append("  BackgroundColor #FEFCF9|#F9EDE1").append(CRLF);
		plantUML.append("  AttributeFontColor #5A404C").append(CRLF);
		plantUML.append("  FontColor #000000").append(CRLF);
		plantUML.append("  FontStyle bold").append(CRLF);
		plantUML.append("  StereotypeFontColor #000000").append(CRLF);
		plantUML.append("  HeaderFontColor #000000").append(CRLF);
		plantUML.append("  HeaderFontStyle bold").append(CRLF);
		plantUML.append("  BorderColor #454645").append(CRLF);
		plantUML.append("  BorderThickness 1").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("  ' ShadowExtension classes style definition").append(CRLF);
		plantUML.append("  BackgroundColor<<ShadowExtension>> #EFEFEF|#lightgray").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("  ' Enumerations style definition").append(CRLF);
		plantUML.append("  BackgroundColor<<enumeration>> #E9FFE5|#D0FAC6").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("  ' Errors classes style definition").append(CRLF);
		plantUML.append("  BackgroundColor<<error>> #FFF3F5|#FFC0CB").append(CRLF);
		plantUML.append("  FontColor<<Error>> #000000").append(CRLF);
		plantUML.append("  AttributeFontColor<<Error>> #000000").append(CRLF);
		plantUML.append("}").append(CRLF);
		plantUML.append(CRLF);
		plantUML.append("hide <<Error>> stereotype").append(CRLF);
		plantUML.append(CRLF);
		//
		// Generate the "body" of the PlantUML diagram centered on the shadow class.
		plantUML.append(generateShadowClassPlantUML(shadowClass, violations)).append(CRLF);
		//
		plantUML.append("@enduml").append(CRLF);
		plantUML.append("----").append(CRLF);
		return plantUML.toString();
	}

	private static String generateShadowClassPlantUML(OntResource shadowClass, List<RuleViolation> violations) {
		StringBuffer plantUML = new StringBuffer();

		Map<String, RuleViolation> map = new HashMap<>();
		violations.forEach(violation -> {
			map.put(violation.getResourceURI(), violation);
		});

		ResIterator superClasses = shadowClass.listSuperClasses(false);
		while (superClasses.hasNext()) {
			OntResource superClass = superClasses.nextResource();
			String aClass = createClassPlantUML(superClass, true);
			plantUML.append(aClass);
		}

		OntModel model = shadowClass.getOntModel();
		ResIterator subClasses = model.listSubjectsWithProperty(RDFS.subClassOf, shadowClass);
		while (subClasses.hasNext()) {
			OntResource subClass = subClasses.nextResource();
			String aClass = createClassPlantUML(subClass, true);
			plantUML.append(aClass);
		}

		String aClass = createClassPlantUML(shadowClass, true);
		plantUML.append(aClass);

		plantUML.append(createGeneralizations(shadowClass));
		plantUML.append(createAssociations(shadowClass));

		return plantUML.toString();
	}

	private static String createClassPlantUML(OntResource aClass, boolean includeAttributes) {
		boolean isEnumeration = aClass.hasProperty(UML.hasStereotype, UML.enumeration);
		StringBuffer plantUMLClass = new StringBuffer();
		//
		plantUMLClass.append(CRLF);
		plantUMLClass.append("class ").append(aClass.getLabel()).append(" ").append(orderClassStereotypes(aClass))
				.append(" {").append(CRLF);
		//
		if (includeAttributes) {
			ResIterator funcProps = null;
			if (isEnumeration) {
				funcProps = aClass.getOntModel().listSubjectsBuffered(RDF.type, aClass);
			} else {
				funcProps = aClass.getOntModel().listSubjectsWithProperty(RDFS.domain, aClass);
			}
			while (funcProps.hasNext()) {
				OntResource prop = funcProps.nextResource();
				if (isEnumeration) {
					plantUMLClass.append("	").append(prop.getLabel());
				} else {
					// Only include properties and not associations...
					if (!prop.isObjectProperty()) {
						String declaredType = (prop.getRange() != null ? prop.getRange().getLabel() : "Unknown");
						String property = String.format("	%s+ %s: %s [%s]", propertyStereotypes(prop),
								prop.getLabel(), declaredType, getCardinality(prop));
						plantUMLClass.append(property).append(CRLF);
					}
				}
			}
		}
		//
		plantUMLClass.append("}").append(CRLF);
		return plantUMLClass.toString();
	}

	private static String createGeneralizations(OntResource shadowClass) {
		StringBuffer plantUMLGeneralizations = new StringBuffer();
		plantUMLGeneralizations.append(CRLF);

		ResIterator superClasses = shadowClass.listSuperClasses(false);
		while (superClasses.hasNext()) {
			OntResource superClass = superClasses.nextResource();
			plantUMLGeneralizations.append(superClass.getLabel()).append(" <|-- ").append(shadowClass.getLabel())
					.append(CRLF);
			plantUMLGeneralizations.append(CRLF);
		}

		OntModel model = shadowClass.getOntModel();
		ResIterator subClasses = model.listSubjectsWithProperty(RDFS.subClassOf, shadowClass);
		while (subClasses.hasNext()) {
			OntResource subClass = subClasses.nextResource();
			plantUMLGeneralizations.append(shadowClass.getLabel()).append(" <|-- ").append(subClass.getLabel())
					.append(CRLF);
			plantUMLGeneralizations.append(CRLF);
		}

		return plantUMLGeneralizations.toString();
	}

	private static String createAssociations(OntResource shadowClass) {
		StringBuffer plantUMLAssociations = new StringBuffer();
		plantUMLAssociations.append(CRLF);

		ResIterator superClasses = shadowClass.listSuperClasses(false);
		while (superClasses.hasNext()) {
			OntResource superClass = superClasses.nextResource();
			plantUMLAssociations.append(superClass.getLabel()).append(" <|-- ").append(shadowClass.getLabel())
					.append(CRLF);
			plantUMLAssociations.append(CRLF);
		}

		OntModel model = shadowClass.getOntModel();
		ResIterator subClasses = model.listSubjectsWithProperty(RDFS.subClassOf, shadowClass);
		while (subClasses.hasNext()) {
			OntResource subClass = subClasses.nextResource();
			plantUMLAssociations.append(shadowClass.getLabel()).append(" <|-- ").append(subClass.getLabel())
					.append(CRLF);
			plantUMLAssociations.append(CRLF);
		}

		return plantUMLAssociations.toString();
	}

	private static String getCardinality(OntResource property) {
		if (property.getMinCardinality() == property.getMaxCardinality())
			return Integer.toString(property.getMinCardinality());
		return property.getMinCardinality() + ".." + property.getMaxCardinality();
	}

	private static String orderClassStereotypes(OntResource aClass) {
		StringBuffer plantUMLStereotypes = new StringBuffer("");

		List<String> stereotypesList = new ArrayList<String>();

		if (aClass.hasProperty(UML.hasStereotype)) {
			ResIterator stereotypes = aClass.listProperties(UML.hasStereotype);
			while (stereotypes.hasNext()) {
				OntResource stereotype = stereotypes.nextResource();
				stereotypesList.add(stereotype.getLabel());
			}
		}

		// Since PlantUML styling/skinning is always done based on the first
		// stereotype in a list we need to order them as shown below...
		List<String> orderedStereotypes = stereotypesList.stream().sorted(Comparator.comparingInt((String s) -> {
			if ("Error".equals(s))
				return 0;
			if ("ShadowExtension".equals(s))
				return 1;
			if ("enumeration".equals(s))
				return 2;
			return 3;
		}).thenComparing(String::compareToIgnoreCase)).collect(Collectors.toList());

		for (String stereotype : orderedStereotypes) {
			plantUMLStereotypes.append("<<").append(stereotype).append(">>").append(" ");
		}

		return plantUMLStereotypes.toString();
	}

	private static String propertyStereotypes(OntResource property) {
		StringBuffer plantUMLStereotypes = new StringBuffer("");
		if (property.hasProperty(UML.hasStereotype)) {
			ResIterator stereotypes = property.listProperties(UML.hasStereotype);
			while (stereotypes.hasNext()) {
				OntResource stereotype = stereotypes.nextResource();
				plantUMLStereotypes.append("<<").append(stereotype.getLabel()).append(">>").append(" ");
			}
		}
		return plantUMLStereotypes.toString();
	}

}
