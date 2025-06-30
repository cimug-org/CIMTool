package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;

@Rule(name = "Unused Class", description = "Detects classes with no instances or subclasses")
public class UnusedClassRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        return res.isClass() && !res.listInstances().hasNext() && !res.listSubClasses(false).hasNext();
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Class <" + res.getURI() + "> is unused (no instances or subclasses).");
    }
}