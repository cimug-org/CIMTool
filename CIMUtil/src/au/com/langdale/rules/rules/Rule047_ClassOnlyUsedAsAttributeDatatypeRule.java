package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

@Rule(name = "Rule047", description = "Datatype classes must only be used as attribute types")
public class Rule047_ClassOnlyUsedAsAttributeDatatypeRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        return res.hasProperty(UML.hasStereotype, UML.enumeration) ||
               res.hasProperty(UML.hasStereotype, UML.cimdatatype) ||
               res.hasProperty(UML.hasStereotype, UML.primitive) ||
               res.hasProperty(UML.hasStereotype, UML.compound);
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Rule047: Class <" + res.getURI() + "> has a restricted stereotype and should only be used as a datatype in attributes.");
    }
}