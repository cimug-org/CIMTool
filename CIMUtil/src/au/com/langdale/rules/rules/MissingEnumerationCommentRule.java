package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

@Rule(name = "Missing Enumeration Documentation", description = "Detects enumerations with missing documentation")
public class MissingEnumerationCommentRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        return res.hasProperty(UML.hasStereotype, UML.enumeration)
            && (res.getComment() == null || res.getComment().trim().isEmpty());
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Enumeration <" + res.getURI() + "> is missing proper documentation.");
    }
}