package au.com.langdale.cimtoole.project;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import au.com.langdale.profiles.SchemaMerger;

/**
 * Shows the problems found while choosing a project's base CIM schema as
 * problem markers on the schema files involved (issue #391).
 * <p>
 * The markers are replaced each time the project's schema files are merged, so
 * they disappear as soon as the problem is resolved. Each marker states what is
 * wrong and the action that resolves it.
 */
public final class SchemaMergeMarkers {

	/** The marker type, declared in plugin.xml as a kind of CIMTool problem. */
	public static final String MARKER_TYPE = "au.com.langdale.cimtoole.schemaproblem";

	/** The marker attribute holding the {@link SchemaMerger.ProblemType} name. */
	public static final String PROBLEM_TYPE = "problemType";

	/**
	 * The family of the jobs that write the markers, so that a caller can wait for
	 * them with {@code Job.getJobManager().join(JOB_FAMILY, monitor)}.
	 */
	public static final Object JOB_FAMILY = SchemaMergeMarkers.class;

	private SchemaMergeMarkers() {
	}

	/**
	 * Replaces the markers on the schema folder's files with those for the given
	 * merge. The markers are written in a background job, since the merge may run
	 * while the workspace cannot be changed.
	 *
	 * @param folder the project's schema folder
	 * @param result the result of merging its schema files
	 */
	public static void update(IFolder folder, SchemaMerger.Result result) {
		update(folder, result.getProblems());
	}

	/**
	 * Replaces the markers on the schema folder's files with markers for the given
	 * problems, in a background job.
	 * 
	 * @param folder   the project's schema folder
	 * @param problems the problems found choosing or using the base CIM schema
	 */
	public static void update(final IFolder folder, final List<SchemaMerger.Problem> problems) {
		if (!folder.exists())
			return;
		WorkspaceJob job = new WorkspaceJob("Checking the base CIM schema of " + folder.getProject().getName()) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				replace(folder, problems);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == JOB_FAMILY;
			}
		};
		job.setRule(folder);
		job.setSystem(true);
		job.schedule();
	}

	/**
	 * Replaces the markers immediately. The caller must be able to change the
	 * folder's resources.
	 */
	public static void replace(IFolder folder, List<SchemaMerger.Problem> problems) throws CoreException {
		if (!folder.exists())
			return;
		folder.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ONE);
		for (SchemaMerger.Problem problem : problems) {
			String message = message(problem);
			int severity = problem.isError() ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING;
			for (SchemaMerger.SchemaFile file : problem.getFiles()) {
				IFile resource = folder.getFile(file.getName());
				if (resource.exists()) {
					IMarker marker = resource.createMarker(MARKER_TYPE);
					marker.setAttribute(IMarker.MESSAGE, message);
					marker.setAttribute(IMarker.SEVERITY, severity);
					marker.setAttribute(PROBLEM_TYPE, problem.getType().name());
				}
			}
		}
	}

	/** The marker's message: what is wrong, the action that resolves it, and its effect. */
	static String message(SchemaMerger.Problem problem) {
		StringBuilder text = new StringBuilder(problem.getDescription()).append(". ").append(action(problem));
		if (problem.isError())
			text.append(" Until then the project's profiles are not built, and their existing outputs are not updated.");
		return text.toString();
	}

	private static String action(SchemaMerger.Problem problem) {
		switch (problem.getType()) {
		case MULTIPLE_EA_SCHEMAS:
			return "Remove all but one of them from the Schema folder.";
		case IGNORED_BASE_FLAG:
			return "Clear \"Base CIM schema\" on the file's Properties page.";
		case MULTIPLE_BASE_SCHEMAS:
			return "Clear \"Base CIM schema\" on the Properties page of all but one of them.";
		case NO_BASE_SCHEMA:
		case AMBIGUOUS_BASE_SCHEMA:
			return "Check \"Base CIM schema\" on the Properties page of the schema file that is the base.";
		case BASE_SCHEMA_HEADERS:
			return "Edit the file so that it declares one owl:Ontology header, or make another schema file the base.";
		case BASE_SCHEMA_INVALID_URI:
			return "Correct the file's owl:Ontology header so that its URI is the schema namespace without the trailing '#'.";
		case BASE_SCHEMA_NO_HEADER:
			return "Add an owl:Ontology header to the file to set the ontology URI yourself.";
		default:
			return "";
		}
	}
}
