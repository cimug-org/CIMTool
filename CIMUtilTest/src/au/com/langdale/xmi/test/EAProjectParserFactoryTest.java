package au.com.langdale.xmi.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import au.com.langdale.xmi.EAPParser;
import au.com.langdale.xmi.EAProjectParser;
import au.com.langdale.xmi.EAProjectParserFactory;
import au.com.langdale.xmi.QEAParser;

/**
 * Tests for {@link EAProjectParserFactory}: the one definition of an Enterprise
 * Architect project file, shared by the CIMTool plug-in and the command line
 * (issue #391), and its agreement with the parsers the factory creates.
 */
class EAProjectParserFactoryTest {

	@ParameterizedTest
	@ValueSource(strings = { "a.eap", "a.eapx", "a.qea", "a.qeax", "A.QEA", "CIM.Model.qeax" })
	void recognisesEAProjectFilesIgnoringCase(String name) {
		assertTrue(EAProjectParserFactory.isEAProject(name), name);
	}

	@ParameterizedTest
	@ValueSource(strings = { "a.xmi", "a.owl", "a.qea.bak", "qea", "a.", "" })
	void rejectsOtherFiles(String name) {
		assertFalse(EAProjectParserFactory.isEAProject(name), name);
	}

	@Test
	void rejectsNull() {
		assertFalse(EAProjectParserFactory.isEAProject(null));
	}

	@Test
	void everyListedExtensionHasAParser() {
		for (String ext : EAProjectParserFactory.EXTENSIONS) {
			EAProjectParser parser = EAProjectParserFactory.createParser("http://example.com/cim#",
					new File("model." + ext), false, false, false, null, null, null);
			assertNotNull(parser, ext);
			assertTrue(parser instanceof EAPParser || parser instanceof QEAParser, ext);
		}
	}

	@Test
	void otherExtensionsHaveNoParser() {
		assertThrows(IllegalArgumentException.class, () -> EAProjectParserFactory.createParser(
				"http://example.com/cim#", new File("model.xmi"), false, false, false, null, null, null));
	}
}
