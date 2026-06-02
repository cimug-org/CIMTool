/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.common;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.EADatabaseMetadata;
import au.com.langdale.xmi.NamespaceResolver;
import au.com.langdale.xmi.UML;
import au.com.langdale.xmi.XMI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;

/**
 * Base class for rule classes to enable access to rule metadata like name
 * description.
 */
public abstract class DBBaseRule extends BaseRule implements EADatabaseMetadata {

	private boolean selfHealingEnabled = false;
	private NamespaceResolver namespaceResolver;

	public DBBaseRule(String baseURI, boolean selfHealingEnabled, NamespaceResolver namespaceResolver) {
		super(baseURI);
		this.selfHealingEnabled = selfHealingEnabled;
		this.namespaceResolver = namespaceResolver;
	}

	/**
	 * This is intentionally made final to ensure the any child classes of
	 * DBBaseRule do not override this method but instead override DBBaseRule's
	 * getPlaceholderValues() method that include the ResultSet as a parameter.
	 */
	protected final Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		return super.getPlaceholderValues(resource, namesMap);
	}

	protected boolean isSelfHealingEnabled() {
		return selfHealingEnabled;
	}

	protected String getResourceURI(OntResource resource) {
		if (resource.getURI().startsWith(XMI.NS) || resource.getURI().startsWith(UML.NS)) {
			return this.namespaceResolver.findBaseURI(resource) + resource.getLabel();
		}
		return resource.getURI();
	}

	protected String getResourceNamespace(OntResource resource) {
		return this.namespaceResolver.findBaseURI(resource);
	}

	protected RuleViolation createRuleViolation(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap,
			String message) {
		return new RuleViolation( //
				getRuleId(), //
				getCompositeRuleId(), //
				getCompositeSubRuleId(), //
				getRuleType(), //
				getRuleCategory(), //
				message, //
				getResourceURI(resource), //
				getResourceNamespace(resource), //
				resource.getLabel(), //
				getViolationSeverity(), //
				isNormative(resource), //
				getPackageHierarchy(resource), //
				getPlaceholderValues(rs, resource, namesMap));
	}

	protected RuleViolation createRuleViolation(ResultSet rs, OntResource resource,
			Map<String, List<String>> namesMap) {
		return createRuleViolation(rs, resource, namesMap, getRuleErrorMsg(rs, resource, namesMap));
	}

	/**
	 * Replaces placeholders in the error message template with actual values.
	 * Supported format: {key} for replacement using Map values.
	 * 
	 * Additionally, delegates to a separate method call to retrieve and append the
	 * results of any attempt by CIMTool to perform self healing on the model during
	 * import. This is only applicable if the 'self heal on import' setting is
	 * enabled for the project.
	 * 
	 * @param resultSet The SQL result set from which to extract placeholder values.
	 * @param resource  The resource from which to extract placeholder values.
	 * @param namesMap  The class, package, or empty map of element names in the
	 *                  model (which type is context specific to the type of rule).
	 * @return Formatted rule violations message as a string.
	 */
	protected String getRuleErrorMsg(ResultSet resultSet, OntResource resource, Map<String, List<String>> namesMap) {
		String template = getErrorTemplate();

		if (isSelfHealingEnabled())
			template = template + getSelfHealingResultsMsg(resultSet, resource, namesMap);

		if (template != null && !template.isBlank()) {
			for (Map.Entry<String, String> entry : getPlaceholderValues(resultSet, resource, namesMap).entrySet()) {
				template = template.replace("{" + entry.getKey() + "}", entry.getValue());
			}
		}

		return template;
	}

	/**
	 * A default implementation for providing the a message of the result of any
	 * attempt by CIMTool to perform self healing on the model during import. This
	 * method is called by the getRuleErrorMsg method and only invoked when the
	 * 'self heal on import' setting is enabled.
	 * 
	 * This default implementation simply returns an empty string which is the
	 * default implementation for rule violations that have no "fix" that can be
	 * performed during import. For those that do this method should be overridden
	 * and implemented by the rule subclass where the fix was attempted and
	 * performed. If the "fix" was not able to be fully applied the method should
	 * indicate as much. If a fix was applied then the message should indict what
	 * was fixed.
	 * 
	 * Finally, it should be noted that any overridden implementations should
	 * prepend the message that is returned with however many carriage returns
	 * (typically two) are needed to visually separate the self healing results
	 * message from the rule violation description.
	 * 
	 * NOTE: The parameters passes to the method may or may not be needed by
	 * implementations. Most commonly not, but in the event they are necessary to
	 * extract context needed to create the self healing message they are provided.
	 * 
	 * @param resultSet The SQL result set from which to extract placeholder values
	 *                  to be replaced in the message.
	 * @param resource  The resource from which to extract placeholder values to be
	 *                  replaced in the message.
	 * @param namesMap  The class, package, or empty map of element names in the
	 *                  model (which type is context specific to the type of rule).
	 * @return Formatted self healing results message as a string.
	 */
	protected String getSelfHealingResultsMsg(ResultSet resultSet, OntResource resource,
			Map<String, List<String>> namesMap) {
		return "";
	}

	/**
	 * Replaces placeholders in the errorMsg template with actual values. This method
	 * is intended to be overridden within any subclass that might need to
	 * additionally, retrieve placeholder values from a JDBC ResultSet object.
	 * 
	 * Supported format: {key} for replacement using Map values.
	 * 
	 * Calls the parent class's method and can additionally pull from the result set
	 * if needed.
	 * 
	 * @param resultSet The SQL result set from which to extract placeholder values
	 *                  if relevant.
	 * @param resource  The resource from which to extract placeholder values.
	 * @return A map of placeholder keys to their replacement values.
	 */
	protected Map<String, String> getPlaceholderValues(ResultSet resultSet, OntResource resource,
			Map<String, List<String>> namesMap) {
		Map<String, String> placeholderValues = super.getPlaceholderValues(resource, namesMap);
		return placeholderValues;
	}

	/**
	 * The when method has been implemented in the base DB rule to more gracefully
	 * handle SQL exceptions. The actual application of the rule is delegated to a
	 * doWhen() method.
	 * 
	 * @param rs       The result set to apply the rule to.
	 * @param resource The resource to apply the rule to.
	 * @return true if the rule was violated; false otherwise.
	 */
	@Condition
	public final boolean when(@Fact("rs") ResultSet rs, @Fact("resource") OntResource resource,
			@Fact("namesMap") Map<String, List<String>> namesMap) {
		try {
			return doWhen(rs, resource, namesMap);
		} catch (SQLException e) {
			return false;
		}
	}

	protected abstract boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap)
			throws SQLException;

	@Action
	public void then(@Fact("rs") ResultSet rs, @Fact("resource") OntResource resource,
			@Fact("namesMap") Map<String, List<String>> namesMap, @Fact("violations") List<RuleViolation> violations) {
		violations.add(createRuleViolation(rs, resource, namesMap));
	}

}