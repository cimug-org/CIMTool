package au.com.langdale.rules.rules;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;

@Rule(name = "Duplicated Label", description = "Detects duplicate rdfs:label usage across resources")
public class DuplicatedLabelRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("labelMap") Map<String, String> labelMap, @Fact("baseURI") String baseURI) {
        String label = res.getLabel();
        if (label == null) return false;
        String uri = res.getURI();
        return labelMap.containsKey(label) && !labelMap.get(label).equals(uri);
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Label '" + res.getLabel() + "' is duplicated by resource <" + res.getURI() + ">.");
    }
}