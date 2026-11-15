package au.com.langdale.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import au.com.langdale.cimutil.test.TestProjects;
import au.com.langdale.inference.LOG;
import au.com.langdale.inference.RepairFunctors;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Tests that {@code ProfileValidator} reports a cyclic profile hierarchy
 * (issue #387). In {@code CyclicProfile.owl}, A and B name each other as
 * superclasses, and C names A.
 */
class ProfileValidatorCycleTest {

	private static final String NS = "http://example.com/CyclicProfile#";
	private static final String PHRASE = "has a cyclic superclass chain through";

	private static OntModel read(String resource, String base) throws Exception {
		OntModel model = ModelFactory.createMem();
		try (InputStream in = ProfileValidatorCycleTest.class.getResourceAsStream(resource)) {
			IO.read(model, in, base, Format.RDF_XML.toFormat());
		}
		return model;
	}

	private static OntModel validate(String profile) throws Exception {
		return validate(read(profile, NS), read("/minimal-project/Schema/MinimalSchema.owl", TestProjects.CIM100));
	}

	private static OntModel validate(OntModel profile, OntModel schema) throws Exception {
		ProfileValidator validator = new ProfileValidator(profile, schema);
		validator.run();
		return validator.getLog();
	}

	/** Every cycle problem the log records, by its detail. */
	private static List<String> allCycleProblems(OntModel log) {
		List<String> details = new ArrayList<String>();
		ResIterator it = log.listSubjectsWithProperty(RDF.type, LOG.Problem);
		while (it.hasNext()) {
			OntResource problem = it.nextResource();
			String comment = problem.getComment(null);
			if (comment != null && comment.contains(PHRASE))
				details.add(problem.getString(LOG.problemDetail));
		}
		return details;
	}

	/** The cycle problems the log records for the named profile class. */
	private static List<OntResource> cycleProblems(OntModel log, String name) {
		List<OntResource> problems = new ArrayList<OntResource>();
		ResIterator it = log.createResource(NS + name).listProperties(LOG.hasProblems);
		while (it.hasNext()) {
			OntResource problem = it.nextResource();
			String comment = problem.getComment(null);
			if (comment != null && comment.contains(PHRASE))
				problems.add(problem);
		}
		return problems;
	}

	@Test
	void eachStatementOnTheCycleIsReported() throws Exception {
		OntModel log = validate("/unresolved-project/Profiles/CyclicProfile.owl");
		assertEquals(1, cycleProblems(log, "A").size(), "A's superclass B closes the cycle");
		assertEquals(1, cycleProblems(log, "B").size(), "B's superclass A closes the cycle");
	}

	@Test
	void classLeadingIntoTheCycleIsNotReported() throws Exception {
		OntModel log = validate("/unresolved-project/Profiles/CyclicProfile.owl");
		assertTrue(cycleProblems(log, "C").isEmpty());
	}

	@Test
	void repairRemovesTheSuperClassStatement() throws Exception {
		OntModel log = validate("/unresolved-project/Profiles/CyclicProfile.owl");
		OntResource problem = cycleProblems(log, "A").get(0);
		OntResource repair = problem.getResource(LOG.hasRepairs);
		assertTrue(repair.hasRDFType(log.createResource(RepairFunctors.NS + "remove")));
		OntResource first = log.createResource(RDF.first.getURI());
		OntResource rest = log.createResource(RDF.rest.getURI());
		List<Node> args = new ArrayList<Node>();
		for (OntResource list = repair.getResource(LOG.repairArgs); list != null
				&& !list.asNode().equals(RDF.nil.asNode()); list = list.getResource(rest))
			args.add(list.getResource(first).asNode());
		assertEquals(List.of(Node.createURI(NS + "A"), RDFS.subClassOf.asNode(), Node.createURI(NS + "B")), args);
	}

	@Test
	void profileWithoutACycleHasNoCycleProblems() throws Exception {
		OntModel log = validate("/unresolved-project/Profiles/UnresolvedProfile.owl");
		assertEquals(List.of(), allCycleProblems(log));
	}

	/**
	 * Associations are not superclass relationships. In
	 * {@code AssociationsProfile.owl}, Equipment references itself through
	 * {@code Equipment.Parent}, and Terminal and ConnectivityNode reference each
	 * other. None of this is a cycle.
	 */
	@Test
	void selfAndMutualAssociationsAreNotCycles() throws Exception {
		OntModel profile = read("/unresolved-project/Profiles/AssociationsProfile.owl",
				"http://example.com/AssociationsProfile#");
		OntModel schema = read("/unresolved-project/Schema/AssociationsSchema.owl", TestProjects.CIM100);
		assertEquals(3, ((java.util.Set<?>) profile.listSubjectsWithProperty(RDF.type, OWL.Class).toSet()).stream()
				.filter(c -> !((OntResource) c).isAnon()).count(), "the profile defines three classes");
		assertEquals(List.of(), allCycleProblems(validate(profile, schema)));
	}

	/**
	 * The CGMES sample profile has superclasses between profile classes and a
	 * self-reference ({@code Equipment.AggregatedEquipment}), and no cycle.
	 */
	@Test
	void sampleProfileHasNoCycleProblems() throws Exception {
		OntModel profile = read("/sample-project/Profiles/" + TestProjects.SAMPLE_PROFILE + ".owl",
				TestProjects.SAMPLE_PROFILE_NAMESPACE);
		assertEquals(List.of(), allCycleProblems(validate(profile, TestProjects.sampleSchema())));
	}
}
