package au.com.langdale.cimtoole.test;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.inference.AsyncModel;
import au.com.langdale.splitmodel.SplitReader;

public class ProjectTest extends WorkspaceTest {

	public static final String SAMPLES_FOLDER = "CIMToolTestFiles";
	public static final String SAMPLE_SCHEMA = "cim11v09combined.xmi";
	public static final String SAMPLE_PROFILE = "cpsm2007.owl";
	public static final String SAMPLE_RULES = "profile.rules";
	public static final String SAMPLE_HTML_RULES = "profile.html-xslt";
	public static final String SAMPLE_XSD_RULES = "profile.xsd-xslt";
	public static final String ALT_XSD_RULES = "alternative.xsd-xslt";
	public static final String SCHEMA_NS = "http://iec.ch/TC57/2007/CIM-schema-cim12#";
	public static final String PROFILE_NS = "http://example.com/profile#";
	public static final String MODEL_NS = "http://example.com/network#";
	public static final String PROFILE_ENVELOPE = "Envelope";
	public static final String MODEL_NAME = "TestModel";
	public static final String INCREMENT_NAME = "TestIncrement";
	public static final String SMALL_CASES = "SmallCases";

	protected IFile schema;
	protected IFile profile;
	protected IFolder model;
	protected IFolder increment;
	protected AsyncModel reader;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		workspace.run(Task.createProject(project, null), monitor);
		schema = Info.getSchemaFolder(project).getFile(SAMPLE_SCHEMA);
		profile = Info.getProfileFolder(project).getFile(SAMPLE_PROFILE);
		model = Info.getInstanceFolder(project).getFolder(MODEL_NAME);
		increment = Info.getIncrementalFolder(project).getFolder(INCREMENT_NAME);
	}
	
	protected void createReader() throws IOException {
		reader = new SplitReader(model.getLocation().toOSString());
	}
	
	protected String getSamplesFolder() {
		return workspace.getRoot().getLocation()
				.removeLastSegments(1).append(SAMPLES_FOLDER)
				.addTrailingSeparator().toOSString();
	}
	
	protected String getSmallCasesFolder() {
		return workspace.getRoot().getLocation()
				.removeLastSegments(1)
				.append(SAMPLES_FOLDER)
				.append(SMALL_CASES)
				.addTrailingSeparator().toOSString();
	}

	protected void setupSchema() throws CoreException  {
		IWorkspaceRunnable task = Task.importSchema(
				schema, 
				getSamplesFolder() + SAMPLE_SCHEMA, 
				SCHEMA_NS);
		task.run(monitor);
	}
	
	protected void setupProfile() throws CoreException {
		setupProfile(getSamplesFolder() + SAMPLE_PROFILE);
		
	}

	protected void setupProfile(final String path) throws CoreException {
		IWorkspaceRunnable task = Task.importProfile(
				profile, 
				path, 
				PROFILE_NS);
		task.run(monitor);
	}

	protected IFile getRelated(String ext) {
		return project.getFile(
				profile.getProjectRelativePath()
				.removeFileExtension()
				.addFileExtension(ext));
	}

	protected IFile getModelRelated(String ext) {
		return project.getFile(
				model.getProjectRelativePath()
				.addFileExtension(ext));
	}

	protected IFile getIncrementRelated(String ext) {
		return project.getFile(
				increment.getProjectRelativePath()
				.addFileExtension(ext));
	}
}
