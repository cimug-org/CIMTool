package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;

@Rule(name = "Class Name Not Upper Camel Case", description = "Detects classes whose class name is not in UpperCamelCase")
public class ClassLabelUpperCamelCaseRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        if (!res.isClass()) return false;
        String label = res.getLabel();
        return label != null && !label.matches("^[A-Z][a-z0-9]*([A-Z][a-z0-9]*)*$");
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Class <" + res.getURI() + "> has label not in UpperCamelCase: '" + res.getLabel() + "'.");
    }
}