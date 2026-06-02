package au.com.langdale.cimtoole.test.headless;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.ProjectTest;
import au.com.langdale.kena.OntModel;

public class TransformTasks extends ProjectTest {

	public static final String PART9_CASES = "Part9";
	public static final String PART9_SAMPLE_PROFILE = "MeterReadings.owl";
	public static final String PART9_SCHEMA = "iec61970cim17v38_iec61968cim13v13b_iec62325cim03v17a_CIM100.2.qea";
	public static final String PART9_SCHEMA_NS = "http://iec.ch/TC57/2007/CIM-schema-cim12#";

	public static final XSDBuildlet xsdBuildlet = new XSDBuildlet("xsd", "xsd");

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupSchema();
		setupProfile();
	}

	protected String getProfileForTesting() {
		return PART9_SAMPLE_PROFILE;
	}

	protected String getSchemaForTesting() {
		return PART9_SCHEMA;
	}

	protected String getSchemaNSForTesting() {
		return PART9_SCHEMA_NS;
	}
	
	protected void setupProfile() throws CoreException {
		setupProfile(getPart9Folder() + PART9_SAMPLE_PROFILE);
	}

	protected String getPart9Folder() {
		IPath rootPath = workspace.getRoot().getLocation();
		IPath part9Folder = rootPath.removeLastSegments(1).append(SAMPLES_FOLDER).append(PART9_CASES);
		String osString = part9Folder.addTrailingSeparator().toOSString();
		return osString;
	}

	public final void testXSDGeneration() throws CoreException {
		OntModel model = Task.getProfileModel(profile);
		xsdBuildlet.setFlagged(model, true);
		workspace.run(Task.saveProfile(profile, model), monitor);
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		assertTrue("generated XSD exists", getRelated("xsd").exists());
		assertTrue("generated XSD valid",
				getRelated("xsd").findMaxProblemSeverity(null, true, IResource.DEPTH_ZERO) < IMarker.SEVERITY_ERROR);
	}

	public final void testCustomXSDGeneration() throws CoreException {
		workspace.run(Task.importRules(getRelated("xsd-xslt"), getSamplesFolder() + ALT_XSD_RULES), monitor);
		OntModel model = Task.getProfileModel(profile);
		xsdBuildlet.setFlagged(model, true);
		workspace.run(Task.saveProfile(profile, model), monitor);
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		assertTrue("generated XSD exists", getRelated("xsd").exists());
		assertTrue("generated XSD valid",
				getRelated("xsd").findMaxProblemSeverity(null, true, IResource.DEPTH_ZERO) < IMarker.SEVERITY_ERROR);
	}
}
