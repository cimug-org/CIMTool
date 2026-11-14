package au.com.langdale.cimtoole.test.headless;

import au.com.langdale.ui.binding.Validators;
import au.com.langdale.util.NamespaceUtils;
import junit.framework.TestCase;

/**
 * Tests for {@link Validators#NAMESPACE}, which validates every namespace entered
 * in the CIMTool user interface (issue #377), and for its agreement with
 * {@link NamespaceUtils} in Kena, on which RCPUtil does not depend.
 */
public class NamespaceValidatorTest extends TestCase {

	private static final String MESSAGE = "The namespace must begin with a scheme such as 'http:' or 'https:' and end with a '#' or '/'";

	public void testRuleMatchesNamespaceUtils() {
		assertEquals("Validators.NAMESPACE_REGEX must stay identical to NamespaceUtils.NAMESPACE_PATTERN",
				NamespaceUtils.NAMESPACE_PATTERN.pattern(), Validators.NAMESPACE_REGEX.pattern());
		assertEquals(NamespaceUtils.NAMESPACE_ERROR, MESSAGE);
	}

	public void testAcceptsHashAndSlashNamespaces() {
		String[] valid = { "http://iec.ch/TC57/CIM100#", "http://purl.org/dc/terms/",
				"https://ucaiug.ai/ns/example#", "https://ucaiug.ai/ns/example/",
				"http://ucaiug.org/ns/CGMES/CoreEquipment-EU/3.0#" };
		for (String namespace : valid)
			assertNull(namespace, Validators.NAMESPACE.validate(namespace));
	}

	public void testRejectsOtherNamespaces() {
		String[] invalid = { "http://purl.org/dc/terms", "http://example.org/ns:", "urn:example:ns#",
				"htp://example.org/ns#", "http:example.org/ns#", "http://example.org/my ns#" };
		for (String namespace : invalid)
			assertEquals(namespace, MESSAGE, Validators.NAMESPACE.validate(namespace));
	}

	public void testRequiresANamespace() {
		assertEquals("A symbolic namespace URI is required", Validators.NAMESPACE.validate(""));
	}
}
