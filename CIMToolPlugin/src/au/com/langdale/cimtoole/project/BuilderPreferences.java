package au.com.langdale.cimtoole.project;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;
import au.com.langdale.cimtoole.registries.ProfileBuildletRegistry;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.util.Jobs;

public class BuilderPreferences extends Task {
	private static final Logger log = LoggerFactory.getLogger(BuilderPreferences.class);

	// This flag tracks whether the *next* POST_CHANGE is coming from a clean.
	private volatile boolean cleanInProgress = false;

	/**
	 * The batches in progress on the current thread, keyed by project. While a
	 * project has a batch here, its preferences are read from and written to the
	 * batch's working copy of the store rather than the session store.
	 */
	private final ThreadLocal<Map<IProject, Batch>> batches = ThreadLocal.withInitial(HashMap::new);

	private static class Batch {
		private final OntModel store;
		private boolean changed;

		private Batch(OntModel store) {
			this.store = store;
		}
	}

	public BuilderPreferences() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new ResourceListener(),
				IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * The session property used to cache models.
	 */
	public static final QualifiedName BUILDER_PREFERENCES = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"builder-preferences");

	public static final QualifiedName[] MIGRATED_SYMBOLS = new QualifiedName[] { //
			PLANTUML_THEME, //
			DOCROOT_CLASSES_COLOR, //
			CONCRETE_CLASSES_COLOR, //
			ABSTRACT_CLASSES_COLOR, //
			ENUMERATIONS_COLOR, //
			CIMDATATYPES_COLOR, //
			COMPOUNDS_COLOR, //
			PRIMITIVES_COLOR, //
			CHOICES_COLOR, //
			REFS_COLOR, //
			SHADOW_CLASSES_COLOR, //
			ERRORS_COLOR, //
			ANONYMOUS_CLASSES_COLOR_WHITE, //
			ENABLE_DARK_MODE, //
			ENABLE_SHADOWING, //
			HIDE_ENUMERATIONS, //
			HIDE_CIMDATATYPES, //
			HIDE_COMPOUNDS, //
			HIDE_PRIMITIVES, //
			HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES, //
			HORIZONTAL_SPACING, //
			VERTICAL_SPACING, //
			CURRENT_PROFILE_PREVIEW_STYLE //
	};

	private OntModel migrateBuilderPreferences(IProject project) throws CoreException {
		Migrator visitor = new Migrator();
		project.accept(visitor);
		OntModel preferencesStore = visitor.getStore();
		project.setSessionProperty(BUILDER_PREFERENCES, preferencesStore);

		log.info("Saving migrated builder preferences for {}", project.getName());
		Jobs.runJob(saveBuilderPreferences(project, preferencesStore), project,
				"Migrating CIMTool builder preferences");
		return preferencesStore;
	}

	private class Migrator implements IResourceVisitor {
		private OntModel store = ModelFactory.createMem();

		public OntModel getStore() {
			return store;
		}

		public boolean visit(IResource resource) throws CoreException {
			Map props = resource.getPersistentProperties();
			if (!props.isEmpty()) {
				OntResource subject = createSubject(resource.getProjectRelativePath(), store);
				for (int ix = 0; ix < MIGRATED_SYMBOLS.length; ix++) {
					Property prop = createProperty(MIGRATED_SYMBOLS[ix]);
					String value = (String) props.get(MIGRATED_SYMBOLS[ix]);
					if (value != null)
						subject.addProperty(prop, value);
				}
			}
			return true;
		}
	}

	/**
	 * Runs the given updates as a single batch for the project. Every
	 * {@link #putPreference(IResource, QualifiedName, String)} for the project made
	 * on this thread while the updates run is applied to one working copy of the
	 * store, and every read sees that copy. When the updates finish, the copy
	 * replaces the session store and is saved once, and only if something changed.
	 * If the updates throw, nothing is saved. A nested batch for the same project
	 * joins the outer one.
	 * 
	 * @param project the project whose builder preferences are being changed
	 * @param updates the changes to make
	 */
	public void runBatch(IProject project, Runnable updates) {
		Map<IProject, Batch> active = batches.get();
		if (active.containsKey(project)) {
			updates.run();
			return;
		}
		try {
			Batch batch = new Batch(Composition.copy(getBuilderPreferencesStore(project)));
			active.put(project, batch);
			try {
				updates.run();
			} finally {
				active.remove(project);
			}
			if (batch.changed)
				replaceBuilderPreferencesStore(project, batch.store);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The store to read from: the batch's working copy while the project has a
	 * batch in progress on this thread, otherwise the session store.
	 */
	private OntModel getCurrentStore(IProject project) throws CoreException {
		Batch batch = batches.get().get(project);
		return batch != null ? batch.store : getBuilderPreferencesStore(project);
	}

	public String getPreference(IResource resource, QualifiedName symbol) {
		try {
			return createSubject(resource.getProjectRelativePath(), getCurrentStore(resource.getProject()))
					.getString(createProperty(symbol));
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}
	
	private OntResource createSubject(IPath path, OntModel store) {
		try {
			return store.createResource(
					CIMToolPlugin.PROJECT_NS + new URI(null, path.toPortableString(), null).toASCIIString());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private OntModel getBuilderPreferencesStore(IProject project) throws CoreException {
		OntModel preferencesStore = (OntModel) project.getSessionProperty(BUILDER_PREFERENCES);
		if (preferencesStore == null) {
			IFile builderPreferences = getBuilderPreferences(project);
			if (builderPreferences.exists()) {
				log.info("Loading builder preferences for {}", project.getName());
				preferencesStore = Task.parse(builderPreferences);
				project.setSessionProperty(BUILDER_PREFERENCES, preferencesStore);
			} else {
				preferencesStore = migrateBuilderPreferences(project);
			}
			preferencesStore = migrateProjectDiagramStyle(project, preferencesStore);
		}
		return preferencesStore;
	}

	/**
	 * Moves a project-level Diagram Style out of the project's
	 * {@code .cimtool-settings} file, where releases before 2.4.0 stored it, into
	 * {@code .builder-preferences} with every other PlantUML setting.
	 * Runs when the project's builder preferences are loaded. A value already
	 * present in {@code .builder-preferences} is kept, and an empty value is
	 * dropped, since empty means inherit. Each file is saved at most once, and once
	 * the setting is gone from {@code .cimtool-settings} there is nothing left to
	 * move.
	 * 
	 * @param project the project being loaded
	 * @param store   the project's builder preferences as loaded
	 * @return the builder preferences, including any value moved in
	 */
	private OntModel migrateProjectDiagramStyle(IProject project, OntModel store) throws CoreException {
		Settings settings = CIMToolPlugin.getSettings();
		String style = settings.getSetting(project, CURRENT_PROFILE_PREVIEW_STYLE);
		if (style == null)
			return store;

		Property prop = createProperty(CURRENT_PROFILE_PREVIEW_STYLE);
		IPath path = project.getProjectRelativePath();
		OntModel result = store;
		if (!style.isEmpty() && createSubject(path, store).getString(prop) == null) {
			log.info("Moving the project Diagram Style of {} to its builder preferences", project.getName());
			result = Composition.copy(store);
			createSubject(path, result).setProperty(prop, style, null);
			replaceBuilderPreferencesStore(project, result);
		}
		settings.removeSetting(project, CURRENT_PROFILE_PREVIEW_STYLE);
		return result;
	}

	private void replaceBuilderPreferencesStore(IProject project, OntModel preferencesStore) throws CoreException {
		project.setSessionProperty(BUILDER_PREFERENCES, preferencesStore);

		log.info("Saving modified builder preferences for {}", project.getName());
		Jobs.runJob(saveBuilderPreferences(project, preferencesStore), project,
				"Saving CIMTool builder preferences");
	}

	public void putPreference(IResource resource, QualifiedName symbol, String value) {

		try {
			IProject project = resource.getProject();
			Batch batch = batches.get().get(project);
			OntModel preferencesStore = batch != null ? batch.store : getBuilderPreferencesStore(project);
			Property prop = createProperty(symbol);
			IPath path = resource.getProjectRelativePath();
			String extant = createSubject(path, preferencesStore).getString(prop);

			if (extant != null && extant.equals(value))
				return;

			if (batch != null) {
				createSubject(path, batch.store).setProperty(prop, value, null);
				batch.changed = true;
			} else {
				OntModel revised = Composition.copy(preferencesStore);
				createSubject(path, revised).setProperty(prop, value, null);
				replaceBuilderPreferencesStore(project, revised);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Removes a resource's own value for a builder preference, so that the
	 * resource inherits the value again. Does nothing if the resource has no value
	 * of its own. Inside a batch the removal is applied to the batch's working
	 * copy; otherwise the store is saved at once, as for
	 * {@link #putPreference(IResource, QualifiedName, String)}.
	 * 
	 * @param resource the file or project the preference applies to
	 * @param symbol   the preference
	 */
	public void removePreference(IResource resource, QualifiedName symbol) {
		try {
			IProject project = resource.getProject();
			Batch batch = batches.get().get(project);
			OntModel preferencesStore = batch != null ? batch.store : getBuilderPreferencesStore(project);
			Property prop = createProperty(symbol);
			IPath path = resource.getProjectRelativePath();

			if (createSubject(path, preferencesStore).getString(prop) == null)
				return;

			if (batch != null) {
				createSubject(path, batch.store).removeAll(prop);
				batch.changed = true;
			} else {
				OntModel revised = Composition.copy(preferencesStore);
				createSubject(path, revised).removeAll(prop);
				replaceBuilderPreferencesStore(project, revised);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private class ResourceListener implements IResourceChangeListener {

		public void resourceChanged(IResourceChangeEvent event) {

			if ((event.getType() & IResourceChangeEvent.PRE_BUILD) != 0) {
				if (event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD) {
					cleanInProgress = true;
				}
				return;
			}

			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {

				DeltaVisitor visitor = new DeltaVisitor();
				try {
					IResourceDelta delta = event.getDelta();
					if (delta == null)
						return;

					delta.accept(visitor);

					Iterator it = visitor.getRevised().entrySet().iterator();
					while (it.hasNext()) {
						Entry entry = (Entry) it.next();
						replaceBuilderPreferencesStore((IProject) entry.getKey(), (OntModel) entry.getValue());
					}

					delta.accept(d -> {
						IResource resource = d.getResource();
						if (resource.getType() == IResource.FILE && "puml".equalsIgnoreCase(resource.getFileExtension())
								&& d.getKind() == IResourceDelta.CHANGED
								&& (d.getFlags() & IResourceDelta.CONTENT) != 0) {
							Display.getDefault().asyncExec(() -> refreshPlantUmlFor(resource));
						}
						return true;
					});

				} catch (CoreException e) {
					throw new RuntimeException(e);
				} finally {
					if (cleanInProgress) {
						cleanInProgress = false;
					}
				}
			}
		}

		private void refreshPlantUmlFor(IResource resource) {
			if (!(resource instanceof IFile))
				return;

			IFile pumlFile = (IFile) resource;

			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			IEditorPart editor = page.findEditor(new FileEditorInput(pumlFile));
			if (editor == null)
				return;

			IEditorPart currentlyActive = page.getActiveEditor();
			if (editor != currentlyActive)
				return;

			page.activate(editor);
		}
	}

	private class DeltaVisitor implements IResourceDeltaVisitor {

		private Map revised = new HashMap();

		public Map getRevised() {
			return revised;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {

			if (delta.getResource().getType() == IResource.PROJECT
					&& (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED)) {
				return false;
			}

			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				copyBuilderPreferences(delta.getResource().getFullPath(), delta.getMovedToPath());
			}

			if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
				copyBuilderPreferences(delta.getMovedFromPath(), delta.getResource().getFullPath());
			}

			if (delta.getKind() == IResourceDelta.REMOVED) {
				if (!cleanInProgress) {
					removePreferences(delta.getResource().getFullPath());
				}
			}

			IResource resource = delta.getResource();

			if (resource instanceof IFile && Info.isProfile(resource)
					&& (delta.getFlags() & (IResourceDelta.MOVED_TO | IResourceDelta.MOVED_FROM)) != 0) {
				buildProjectIfAutoBuildOff(resource.getProject());
			}

			return true;
		}

		private OntModel getRevised(IProject project) throws CoreException {

			OntModel result = (OntModel) revised.get(project);
			if (result == null) {
				result = Composition.copy(getBuilderPreferencesStore(project));
				revised.put(project, result);
			}
			return result;

		}

		private void removePreferences(IPath fullPath) throws CoreException {

			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(fullPath.segment(0));

			OntModel preferencesStore = getBuilderPreferencesStore(project);

			OntResource subject = createSubject(fullPath.removeFirstSegments(1), preferencesStore);

			if (preferencesStore.contains(subject)) {
				OntModel target = getRevised(project);
				target.removeSubject(subject);
			}

		}

		private void copyBuilderPreferences(IPath patha, IPath pathb) throws CoreException {

			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

			IProject project = root.getProject(patha.segment(0));

			OntModel preferences = getBuilderPreferencesStore(project);

			IPath newPathA = patha.removeFirstSegments(1);
			IPath rootFileA = Info.removeFileExtension(newPathA);

			for (ProfileBuildlet buildlet : ProfileBuildletRegistry.INSTANCE.getBuildlets()) {

				IPath subjectPathA = rootFileA.addFileExtension(buildlet.getFileExt());

				OntResource subject = createSubject(subjectPathA, preferences);

				Iterator it = preferences.getGraph().find(subject.asNode(), Node.ANY, Node.ANY);

				if (it.hasNext()) {
					IProject target_project = root.getProject(pathb.segment(0));

					OntModel target_store = getRevised(target_project);

					IPath newPathB = pathb.removeFirstSegments(1);

					IPath rootFileB = Info.removeFileExtension(newPathB);

					IPath subjectPathB = rootFileB.addFileExtension(buildlet.getFileExt());

					OntResource target_subject = createSubject(subjectPathB, target_store);

					do {
						Triple t = (Triple) it.next();
						Property prop = ResourceFactory.createProperty(t.getPredicate());
						target_subject.addProperty(prop, t.getObject());
					} while (it.hasNext());
				}
			}

		}

		private static void buildProjectIfAutoBuildOff(IProject project) {
			if (project == null || !project.isAccessible())
				return;

			if (ResourcesPlugin.getWorkspace().isAutoBuilding()) {
				return; // auto-build will run builders for rename anyway
			}

			ISchedulingRule buildRule = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();

			Jobs.runJob(monitor -> {
				int kind = IncrementalProjectBuilder.INCREMENTAL_BUILD;
				project.build(kind, monitor);
			}, buildRule, "Building project '" + project.getName() + "' after profile rename");
		}

	}
}