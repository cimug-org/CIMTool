package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;

@Rule(name = "Rule038", description = "Names for classes shall use the Upper Camel Case naming convention")
public class Rule038_ClassNameUpperCamelCaseRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        return res.isClass() && res.getLabel() != null && !res.getLabel().matches("^[A-Z][a-z0-9]*([A-Z][a-z0-9]*)*$");
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Rule038: Class <" + res.getURI() + "> has a name that is not in UpperCamelCase: '" + res.getLabel() + "'.");
    }
}