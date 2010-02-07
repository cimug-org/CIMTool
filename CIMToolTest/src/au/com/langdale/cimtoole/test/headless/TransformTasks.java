package au.com.langdale.cimtoole.test.headless;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.ProjectTest;
import au.com.langdale.kena.OntModel;

public class TransformTasks extends ProjectTest {

	public static final XSDBuildlet xsdBuildlet = new XSDBuildlet();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupSchema();
		setupProfile();
	}

	public final void testXSDGeneration() throws CoreException {
		OntModel model = Task.getProfileModel(profile);
		xsdBuildlet.setFlagged(model, true);
		workspace.run(Task.saveProfile(profile, model), monitor);
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		assertTrue("generated XSD exists", getRelated("xsd").exists());
		assertTrue("generated XSD valid", getRelated("xsd").findMaxProblemSeverity(null, true, IResource.DEPTH_ZERO) < IMarker.SEVERITY_ERROR);
	}

	public final void testCustomXSDGeneration() throws CoreException {
		workspace.run(Task.importRules(getRelated("xsd-xslt"), getSamplesFolder() + ALT_XSD_RULES), monitor);
		OntModel model = Task.getProfileModel(profile);
		xsdBuildlet.setFlagged(model, true);
		workspace.run(Task.saveProfile(profile, model), monitor);
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		assertTrue("generated XSD exists", getRelated("xsd").exists());
		assertTrue("generated XSD valid", getRelated("xsd").findMaxProblemSeverity(null, true, IResource.DEPTH_ZERO) < IMarker.SEVERITY_ERROR);
	}
}
