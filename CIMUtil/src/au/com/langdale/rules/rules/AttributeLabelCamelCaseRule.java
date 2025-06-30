package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

@Rule(name = "Attribute Label Not Camel Case", description = "Detects attributes whose rdfs:label is not in camelCase")
public class AttributeLabelCamelCaseRule {
	@Condition
	public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
		if (!res.hasProperty(UML.hasStereotype, UML.attribute))
			return false;
		String label = res.getLabel();
		return label != null && !label.matches("^[a-z]+([A-Z][a-z0-9]*)*$");
	}

	@Action
	public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues,
			@Fact("baseURI") String baseURI) {
		issues.add("Attribute <" + res.getURI() + "> has label not in camelCase: '" + res.getLabel() + "'.");
	}
}