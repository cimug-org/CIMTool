package au.com.langdale.rules.rules;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.xmi.UML;

@Rule(name = "Rule040", description = "All class names shall be unique")
public class Rule040_ClassNameUniquenessRule {
	@Condition
	public boolean when(@Fact("resource") OntResource res, @Fact("labelMap") Map<String, List<String>> classNamesMap,
			@Fact("baseURI") String baseURI) {
		String className = res.getLabel();
		if (res.isClass() && className != null && classNamesMap.containsKey(className) && classNamesMap.get(className).size() > 1) {
			if (res.hasProperty(UML.hasStereotype, UML.shadowextension)) {
				ResIterator subClasses = res.listSubClasses(false);
				while (subClasses.hasNext()) {
					OntResource subClass = subClasses.nextResource();
					if (subClass.getLabel().equals(res.getLabel()) && subClass.getURI().equals(baseURI)) {
						return false; // This is allowed for shadow classes so we return false
					}
				}
			} else if (!res.getURI().startsWith(baseURI)) {
				// We know at this point that res is an extension class
				// so we check to see if there exists a subclass that is
				// a normative CIM class with the same name.
				ResIterator subClasses = res.listSubClasses(false);
				while (subClasses.hasNext()) {
					OntResource subClass = subClasses.nextResource();
					if (subClass.getLabel().equals(res.getLabel()) && subClass.getURI().startsWith(baseURI)) {
						return false; // This is allowed for shadow classes so we return false
					}
				}
			} else if (res.getURI().startsWith(baseURI)) {
				// We know at this point that res is a normative CIM class
				// so we now check to see if there exists a superclass that 
				// is a shadow class with the same name.
				List<String> urisList = classNamesMap.get(className);
				int count = 0;
				for (String uri: urisList) {
					if (uri.equals(res.getURI()))
						count++;
				}
				if (count == 1)
					return false;
			}
			return true; // duplicate class name
		}
		return false;
	}

	@Action
	public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues,
			@Fact("baseURI") String baseURI) {
		issues.add("Rule040: Class label '" + res.getLabel() + "' is not globally unique (duplicate detected).");
	}
}