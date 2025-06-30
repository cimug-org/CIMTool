package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;

@Rule(name = "Rule042", description = "All class names shall be singular")
public class Rule042_ClassNameSingularRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        return res.isClass() && res.getLabel() != null && res.getLabel().endsWith("s");
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Rule042: Class <" + res.getURI() + "> has a name that appears plural (should be singular): '" + res.getLabel() + "'.");
    }
}