package au.com.langdale.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;

import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * Tests for {@link ProfileClass#getPropertyInfo(OntResource)} when the profile
 * class has no restriction on the property, as for the inverse of an
 * association whose inverse end is not profiled (issue #342).
 * {@code ProfileModel.ElementNode} asks for the inverse property's information
 * in exactly this case, so the range must be reported as absent rather than
 * failing. Run with assertions enabled ({@code -ea}), as Eclipse JUnit launches
 * are by default.
 */
public class ProfileClassPropertyInfoTest {

	private static final String SCHEMA = "http://example.com/schema#";
	private static final String PROFILE = "http://example.com/profile#";

	@Test
	public void propertyWithoutRestrictionHasNoRange() {
		OntModel model = ModelFactory.createMem();
		OntResource equipment = model.createClass(SCHEMA + "Equipment");
		OntResource aggregated = model.createObjectProperty(SCHEMA + "Equipment.AggregatedEquipment");
		OntResource detailed = model.createObjectProperty(SCHEMA + "Equipment.DetailedEquipment");
		aggregated.addDomain(equipment);
		aggregated.addRange(equipment);
		detailed.addDomain(equipment);
		detailed.addRange(equipment);
		aggregated.addInverseOf(detailed);

		// A profile class that restricts only AggregatedEquipment, as the CIMUtilTest
		// sample profile does.
		// ProfileClass reads the direct superclasses that CIMTool's inference adds,
		// so the test states them explicitly.
		OntResource profileClass = model.createClass(PROFILE + "Equipment");
		OntResource restriction = model.createAllValuesFromRestriction(null, aggregated, profileClass);
		profileClass.addSuperClass(equipment);
		profileClass.addSuperClass(restriction);
		profileClass.addProperty(ReasonerVocabulary.directSubClassOf, equipment);
		profileClass.addProperty(ReasonerVocabulary.directSubClassOf, restriction);

		ProfileClass profile = new ProfileClass(profileClass, PROFILE);

		PropertyInfo restricted = profile.getPropertyInfo(aggregated);
		assertEquals(profileClass, restricted.getRange(), "restricted property keeps its range");

		PropertyInfo inverse = profile.getPropertyInfo(detailed);
		assertNull(inverse.getRange(), "unrestricted inverse property has no range");
	}
}
