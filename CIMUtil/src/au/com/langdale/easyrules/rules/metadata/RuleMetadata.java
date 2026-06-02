/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.metadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define metadata related to a Rule. Currently defined metadata
 * includes:
 * 
 * <pre>
 *  1. compositeRule (optional): When an extension rule (e.g. Rule187) references 
 *     a normative rule (e.g. Rule038) the naming convention of the @Rule would be 
 *     "Rule187:Rule038". Such a rule is referred to as a "composite rule". For 
 *     asciidoc anchoring/linking, we would specify the compositeRule attribute as 
 *     "Rule187". 
 *  2. compositeSubRule (optional): For a composite link would be the subRule 
 *     (e.g. "Rule038").
 *  3. type: The category of the model element that a rule applies to.
 *  4. category: The specific category of UML model element that a rule applies to.
 *  5. severity: The severity level for a violation of a specified rule.
 *  6. errorTemplate: A default message template for when a rule is violated.
 * </pre>
 * 
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuleMetadata {

	String compositeRule() default "";

	String compositeSubRule() default "";

	RuleType type();

	RuleCategory category();

	RuleSeverity severity() default RuleSeverity.ERROR;

	String errorTemplate() default "";

}