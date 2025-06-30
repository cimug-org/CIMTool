package au.com.langdale.rules.rules;

import java.util.List;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.xmi.UML;

@Rule(name = "Rule043", description = "CIMDatatype classes must have attributes: value, unit, multiplier")
public class Rule043_ClassCIMDatatypeStandardAttributesRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        if (!res.hasProperty(UML.hasStereotype, UML.cimdatatype)) return false;

        OntModel model = res.getOntModel();
        ResIterator attributes = model.listSubjectsWithProperty(RDFS.domain, res);

        boolean hasValue = false;
        boolean hasUnit = false;
        boolean hasMultiplier = false;

        while (attributes.hasNext()) {
            OntResource attr = attributes.nextResource();
            String label = attr.getLabel();
            if ("value".equalsIgnoreCase(label)) hasValue = true;
            if ("unit".equalsIgnoreCase(label)) hasUnit = true;
            if ("multiplier".equalsIgnoreCase(label)) hasMultiplier = true;
        }

        return !(hasValue && hasUnit && hasMultiplier);
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        OntModel model = res.getOntModel();
        ResIterator attributes = model.listSubjectsWithProperty(RDFS.domain, res);

        boolean hasValue = false;
        boolean hasUnit = false;
        boolean hasMultiplier = false;

        while (attributes.hasNext()) {
            OntResource attr = attributes.nextResource();
            String label = attr.getLabel();
            if ("value".equalsIgnoreCase(label)) hasValue = true;
            if ("unit".equalsIgnoreCase(label)) hasUnit = true;
            if ("multiplier".equalsIgnoreCase(label)) hasMultiplier = true;
        }

        StringBuilder missing = new StringBuilder();
        if (!hasValue) missing.append("value, ");
        if (!hasUnit) missing.append("unit, ");
        if (!hasMultiplier) missing.append("multiplier, ");

        if (missing.length() > 0)
            missing.setLength(missing.length() - 2); // remove trailing comma

        issues.add("Rule043: CIMDatatype <" + res.getURI() + "> is missing required attributes: " + missing + ".");
    }
}