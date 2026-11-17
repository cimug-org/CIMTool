package au.com.langdale.cimtoole.wizards;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.com.langdale.profiles.SchemaMerger.SchemaFile;
import junit.framework.TestCase;

/**
 * Tests for {@link BaseSchemaChoice}: the "base CIM schema" checkbox of the
 * schema import wizard (issue #391). Each test is one row of the design's
 * table, W1 to W9, or one of the Enterprise Architect project file rules.
 * <p>
 * <i>A</i> is the current base, <i>B</i> another existing file and <i>C</i> a
 * new file. The test is in the wizard's package because the class is private to
 * it. It needs no workspace.
 */
public class BaseSchemaChoiceTest extends TestCase {

	private static final String CIM = "http://iec.ch/TC57/CIM100#";
	private static final String EU = "http://iec.ch/TC57/CIM100-European#";
	private static final String EXT = "http://example.com/ext#";

	private static SchemaFile file(String name, String namespace, boolean flagged) {
		return new SchemaFile(name, null, namespace, flagged);
	}

	private static BaseSchemaChoice decide(List<SchemaFile> existing, String target) {
		return BaseSchemaChoice.decide(existing, target, CIM);
	}

	private static void assertCheckbox(BaseSchemaChoice choice, boolean checked, boolean enabled) {
		assertEquals("checked", checked, choice.isChecked());
		assertEquals("enabled", enabled, choice.isEnabled());
	}

	public void testW1W2FirstSchemaFileIsTheBase() {
		BaseSchemaChoice choice = decide(Collections.<SchemaFile>emptyList(), "C.xmi");
		assertCheckbox(choice, true, false);
		assertTrue(choice.becomesBase(false));
		assertFalse("nothing to replace", choice.needsConfirmation(true));
		assertEquals("C.xmi", choice.getBaseAfterImport(false));
	}

	public void testW3NewFileBesideTheOnlyFile() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", CIM, true)), "C.xmi");
		assertCheckbox(choice, false, true);
		assertEquals("A.xmi", choice.getCurrentBase().getName());
		assertTrue("ticked: confirm replacing A", choice.needsConfirmation(true));
		assertEquals("C.xmi", choice.getBaseAfterImport(true));
		assertFalse(choice.needsConfirmation(false));
		assertEquals("unticked: A stays the base", "A.xmi", choice.getBaseAfterImport(false));
	}

	public void testW4ReimportingTheOnlyFile() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", CIM, true)), "A.xmi");
		assertCheckbox(choice, true, false);
		assertTrue(choice.isTargetExisting());
		assertFalse(choice.needsConfirmation(true));
		assertEquals("A.xmi", choice.getBaseAfterImport(false));
	}

	public void testW5NewFileBesideSeveral() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", CIM, true), file("B.xmi", EU, false)), "C.xmi");
		assertCheckbox(choice, false, true);
		assertTrue(choice.needsConfirmation(true));
		assertEquals("C.xmi", choice.getBaseAfterImport(true));
	}

	public void testW6ReimportingTheBaseAmongSeveral() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", CIM, true), file("B.xmi", EU, false)), "A.xmi");
		assertCheckbox(choice, true, false);
		assertFalse(choice.needsConfirmation(true));
	}

	public void testW7ReimportingAnotherFile() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", CIM, true), file("B.xmi", EU, false)), "B.xmi");
		assertCheckbox(choice, false, true);
		assertTrue(choice.isTargetExisting());
		assertTrue("ticked: confirm B replacing A", choice.needsConfirmation(true));
		assertEquals("B.xmi", choice.getBaseAfterImport(true));
		assertEquals("A.xmi", choice.getBaseAfterImport(false));
	}

	public void testW8NewFileWithNoBase() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", EU, false), file("B.xmi", EXT, false)), "C.xmi");
		assertCheckbox(choice, false, true);
		assertNull(choice.getCurrentBase());
		assertFalse("no base to replace, so no confirmation", choice.needsConfirmation(true));
		assertEquals("C.xmi", choice.getBaseAfterImport(true));
		assertNull("unticked: still no base", choice.getBaseAfterImport(false));
	}

	public void testW9ReimportingWithNoBase() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", EU, false), file("B.xmi", EXT, false)), "B.xmi");
		assertCheckbox(choice, false, true);
		assertFalse(choice.needsConfirmation(true));
		assertEquals("B.xmi", choice.getBaseAfterImport(true));
	}

	public void testTwoFlaggedFilesCountAsNoBase() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", CIM, true), file("B.xmi", EU, true)), "C.xmi");
		assertCheckbox(choice, false, true);
		assertNull(choice.getCurrentBase());
		assertEquals("ticked: C becomes the only base", "C.xmi", choice.getBaseAfterImport(true));
	}

	public void testFallbackBaseIsKeptWhenUnticked() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", CIM, false), file("B.xmi", EU, false)), "C.xmi");
		assertEquals("chosen by the Schema Namespace preference", "A.xmi", choice.getCurrentBase().getName());
		assertEquals("the wizard writes A's flag explicitly", "A.xmi", choice.getBaseAfterImport(false));
	}

	public void testADifferentEAProjectFileReplacesTheExistingOne() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("CIM17.qea", CIM, false), file("B.owl", EXT, false)),
				"CIM18.qea");
		assertCheckbox(choice, true, false);
		assertEquals("the old file is replaced", "CIM17.qea", choice.getReplacedFile());
		assertTrue("always the base", choice.becomesBase(false));
		assertTrue("the replacement is confirmed", choice.needsConfirmation(false));
		assertEquals("CIM18.qea", choice.getBaseAfterImport(false));
	}

	public void testNothingIsReplacedOtherwise() {
		List<SchemaFile> eaBase = Arrays.asList(file("CIM.qea", CIM, false), file("B.owl", EXT, false));
		assertNull("re-importing the EA file", decide(eaBase, "CIM.qea").getReplacedFile());
		assertNull("another kind of file", decide(eaBase, "Ext.owl").getReplacedFile());
		assertNull("an XMI change of base",
				decide(Arrays.asList(file("A.xmi", CIM, true)), "C.xmi").getReplacedFile());
	}

	public void testReimportingTheEAProjectFileKeepsItTheBase() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("CIM.qea", CIM, false), file("B.owl", EXT, false)),
				"CIM.qea");
		assertCheckbox(choice, true, false);
		assertFalse(choice.needsConfirmation(true));
	}

	public void testOtherFilesBesideAnEAProjectFileAreNeverTheBase() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("CIM.qea", CIM, false)), "Ext.owl");
		assertCheckbox(choice, false, false);
		assertFalse("the checkbox cannot be ticked", choice.becomesBase(true));
		assertEquals("CIM.qea", choice.getBaseAfterImport(true));
	}

	public void testAnEAProjectFileImportedOverAnotherBaseNeedsConfirmation() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", CIM, true), file("B.xmi", EU, false)),
				"CIM18.qea");
		assertCheckbox(choice, true, false);
		assertTrue("always the base", choice.becomesBase(false));
		assertTrue("replacing A is confirmed", choice.needsConfirmation(false));
		assertEquals("CIM18.qea", choice.getBaseAfterImport(false));
	}

	public void testHintExplainsEachCase() {
		List<SchemaFile> cimBase = Arrays.asList(file("A.xmi", CIM, true), file("B.xmi", EU, false));
		List<SchemaFile> noBase = Arrays.asList(file("A.xmi", EU, false), file("B.xmi", EXT, false));
		List<SchemaFile> eaBase = Arrays.asList(file("CIM.qea", CIM, false), file("B.owl", EXT, false));
		assertEquals("The first schema file of a project is its base CIM schema.",
				decide(Collections.<SchemaFile>emptyList(), "C.xmi").getHint());
		assertEquals("This file is already the project's base CIM schema.", decide(cimBase, "A.xmi").getHint());
		assertEquals("This file is already the project's base CIM schema.", decide(eaBase, "CIM.qea").getHint());
		assertEquals("An Enterprise Architect project file is always the project's base CIM schema.",
				decide(noBase, "CIM.eapx").getHint());
		assertEquals("The project's Enterprise Architect project file is always its base CIM schema.",
				decide(eaBase, "Ext.owl").getHint());
		assertEquals("The project already has a base CIM schema; ticking this replaces it.",
				decide(cimBase, "C.xmi").getHint());
		assertEquals("The project has no base CIM schema yet.", decide(noBase, "C.xmi").getHint());
		assertEquals("This file replaces the project's Enterprise Architect project file.",
				decide(eaBase, "Other.eap").getHint());
	}

	public void testAnEAProjectFileImportedWithNoBaseNeedsNoConfirmation() {
		BaseSchemaChoice choice = decide(Arrays.asList(file("A.xmi", EU, false), file("B.xmi", EXT, false)),
				"CIM.eapx");
		assertCheckbox(choice, true, false);
		assertFalse(choice.needsConfirmation(false));
	}
}
