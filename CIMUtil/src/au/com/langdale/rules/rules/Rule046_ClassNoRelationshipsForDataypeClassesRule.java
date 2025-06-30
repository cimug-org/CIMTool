package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

@Rule(name = "Rule046", description = "Datatype classes must not have associations or inheritance")
public class Rule046_ClassNoRelationshipsForDataypeClassesRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        if (res.hasProperty(UML.hasStereotype, UML.enumeration) ||
               res.hasProperty(UML.hasStereotype, UML.cimdatatype) ||
               res.hasProperty(UML.hasStereotype, UML.primitive) ||
               res.hasProperty(UML.hasStereotype, UML.compound)) {
        	return true;
        }
        return false;
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Rule046: Class <" + res.getURI() + "> has a datatype-like stereotype and must not participate in relationships.");
    }
}