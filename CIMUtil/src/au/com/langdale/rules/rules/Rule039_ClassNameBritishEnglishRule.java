package au.com.langdale.rules.rules;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import au.com.langdale.kena.OntResource;
import au.com.langdale.rules.utils.BritishSpellingUtil;

@Rule(name = "Rule039", description = "Names for classes shall be British English names")
public class Rule039_ClassNameBritishEnglishRule {
    @Condition
    public boolean when(@Fact("resource") OntResource res, @Fact("baseURI") String baseURI) {
        if (res.isClass() && res.getLabel() != null) {
        	List<Map.Entry<String, String>> result = BritishSpellingUtil.findAmericanWordsInUpperCamelCase(res.getLabel());
        	if (result.size() > 0)
        		return true;
        }
        return false;
    }

    @Action
    public void then(@Fact("resource") OntResource res, @Fact("issues") List<String> issues, @Fact("baseURI") String baseURI) {
        issues.add("Rule039: Class <" + res.getURI() + "> label may not be British English: '" + res.getLabel() + "'.");
    }
}