/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;

/**
 * Exports Eclipse PreferenceStore values to a project-level file for CLI
 * consumption.
 * 
 * <p>
 * This class writes the user's customized global preferences (from the Eclipse
 * PreferenceStore, as set via PlantUML Builders preference page) to a file
 * called {@code .cimtool-global-preferences} in the project directory.
 * </p>
 * 
 * <p>
 * The CLI (in the CIMUtil project) can then read this file to use the same
 * customized defaults as the Eclipse UI, without requiring access to the
 * Eclipse PreferenceStore.
 * </p>
 * 
 * <p>
 * The exported file uses Turtle (TTL) format:
 * </p>
 * 
 * <pre>
 * &lt;http://cimtoole.langdale.com.au/2009/global-prefs#&gt;
 *     prefs:plantuml_compounds_color
 *            "#FFEBCD" ;
 * </pre>
 * 
 * @see Info
 */
public class GlobalPreferencesExporter extends Task {

	/**
	 * Export current global preferences to all project-specific global preferences
	 * files.
	 * 
	 * <p>
	 * This writes all PlantUML-related preferences from the Eclipse PreferenceStore
	 * to the {@code .cimtool-global-preferences} file in each open project's
	 * directory.
	 * </p>
	 * 
	 * @throws CoreException if the file cannot be written
	 */
	public static void exportGlobalPreferencesToAllOpenProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			try {
				if (project.isOpen() && Info.isCIMToolProject(project)) {
					exportGlobalPreferencesToProject(project);
				}
			} catch (CoreException e) {
				CIMToolPlugin.getDefault().getLog().error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Export current global preferences to a project specific global preferences
	 * file.
	 * 
	 * <p>
	 * This writes all PlantUML-related preferences from the Eclipse PreferenceStore
	 * to the {@code .cimtool-global-preferences} file in the project's directory.
	 * </p>
	 * 
	 * <p>
	 * A shallow refresh of the project's root directory is performed before writing
	 * to ensure Eclipse's workspace model is in sync with the actual filesystem
	 * state. This prevents a {@code CoreException} (wrapped as
	 * {@code java.io.IOException}) that would otherwise occur if the
	 * {@code .cimtool-global-preferences} file was deleted outside of Eclipse while
	 * the project was closed, leaving Eclipse's cached resource state stale.
	 * </p>
	 * 
	 * @param project the CIMTool project to export preferences to
	 * @throws CoreException if the file cannot be written
	 */
	public static void exportGlobalPreferencesToProject(final IProject project) throws CoreException {
		IPreferenceStore store = CIMToolPlugin.getDefault().getPreferenceStore();

		// Build the RDF model
		final OntModel model = ModelFactory.createMem();
		OntResource subject = model.createResource(GLOBAL_PREFS_NS);

		// Export each preference
		for (QualifiedName symbol : PREFERENCES_SYMBOLS) {
			String value = store.getString(symbol.getLocalName());
			if (value != null && !value.isEmpty()) {
				Property property = createProperty(symbol);
				subject.addProperty(property, value);
			}
		}

		model.setNsPrefix("prefs", BUILDER_PREFS_NS);
		model.setNsPrefix("project", PROJECT_NS);

		final IFile prefsFile = getGlobalPreferences(project);

		// Perform the filesystem operations (shallow refresh + write) atomically
		// under the project's scheduling rule. The shallow refresh on the project
		// root requires project scope, so we acquire it explicitly here rather
		// than relying on the caller to supply a compatible rule context. If a
		// caller invokes this method while holding a narrower rule (e.g. a single
		// file), Eclipse will surface the rule mismatch at this entry point with
		// this method visible at the top of the stack, rather than deep inside
		// refreshLocal where the cause is less obvious.
		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				if (prefsFile != null) {
					prefsFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
				}
				write(model, PROJECT_NS, true, prefsFile, Format.TURTLE.toFormat(), monitor);
			}
		}, project, IResource.NONE, new NullProgressMonitor());
	}
	
}