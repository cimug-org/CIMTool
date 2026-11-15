package au.com.langdale.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * Tests for {@link ProfileClass#getUnresolvedBase()} (issue #387). A profile
 * class names its schema class as a direct superclass. When the schema does not
 * declare that class, {@link ProfileClass#getBaseClass()} keeps returning the
 * {@code owl:Thing} sentinel, unchanged, and {@code getUnresolvedBase()}
 * reports the class the profile names. ProfileClass reads the direct
 * superclasses that CIMTool's inference adds, so each test states them
 * explicitly.
 */
public class ProfileClassUnresolvedBaseTest {

	private static final String SCHEMA = "http://example.com/schema#";
	private static final String PROFILE = "http://example.com/profile#";

	private OntModel model;
	private OntResource thing;

	@BeforeEach
	public void createModel() {
		model = ModelFactory.createMem();
		thing = model.createResource(OWL.Thing.getURI());
	}

	/** Creates a profile class with the given direct superclasses. */
	private OntResource profileClass(String name, OntResource... superClasses) {
		OntResource clss = model.createClass(PROFILE + name);
		for (OntResource superClass : superClasses) {
			clss.addSuperClass(superClass);
			clss.addProperty(ReasonerVocabulary.directSubClassOf, superClass);
		}
		return clss;
	}

	@Test
	public void resolvedBaseHasNoUnresolvedBase() {
		OntResource equipment = model.createClass(SCHEMA + "Equipment");
		ProfileClass profile = new ProfileClass(profileClass("Equipment", equipment), PROFILE);
		assertEquals(equipment, profile.getBaseClass());
		assertNull(profile.getUnresolvedBase());
	}

	@Test
	public void undeclaredBaseIsReportedAndSentinelIsUnchanged() {
		OntResource missing = model.createResource(SCHEMA + "Missing");
		ProfileClass profile = new ProfileClass(profileClass("Missing", missing), PROFILE);
		assertEquals(thing, profile.getBaseClass(), "getBaseClass() still returns the owl:Thing sentinel");
		assertEquals(missing, profile.getUnresolvedBase());
	}

	@Test
	public void classWithOnlyLocalSuperclassesHasNoUnresolvedBase() {
		OntResource local = profileClass("Local");
		ProfileClass profile = new ProfileClass(profileClass("Derived", local), PROFILE);
		assertEquals(thing, profile.getBaseClass());
		assertNull(profile.getUnresolvedBase(), "a class with no schema base is not unresolved");
	}

	@Test
	public void classWithNoSuperclassesHasNoUnresolvedBase() {
		ProfileClass profile = new ProfileClass(profileClass("Envelope"), PROFILE);
		assertEquals(thing, profile.getBaseClass());
		assertNull(profile.getUnresolvedBase());
	}

	@Test
	public void resolvedBaseWinsOverUndeclaredBase() {
		OntResource equipment = model.createClass(SCHEMA + "Equipment");
		OntResource missing = model.createResource(SCHEMA + "Missing");
		ProfileClass profile = new ProfileClass(profileClass("Both", missing, equipment), PROFILE);
		assertEquals(equipment, profile.getBaseClass());
		assertNull(profile.getUnresolvedBase());
	}

	@Test
	public void explicitDefaultBaseIsNotReportedAsUnresolved() {
		// The range constructor supplies a real class as the default base.
		OntResource equipment = model.createClass(SCHEMA + "Equipment");
		OntResource missing = model.createResource(SCHEMA + "Missing");
		ProfileClass profile = new ProfileClass(profileClass("Range", missing), PROFILE, equipment);
		assertEquals(equipment, profile.getBaseClass());
		assertNull(profile.getUnresolvedBase());
	}

	@Test
	public void setBaseClassClearsUnresolvedBase() {
		OntResource equipment = model.createClass(SCHEMA + "Equipment");
		OntResource missing = model.createResource(SCHEMA + "Missing");
		ProfileClass profile = new ProfileClass(profileClass("Repaired", missing), PROFILE);
		assertEquals(missing, profile.getUnresolvedBase());
		profile.setBaseClass(equipment);
		assertEquals(equipment, profile.getBaseClass());
		assertNull(profile.getUnresolvedBase());
	}
}
