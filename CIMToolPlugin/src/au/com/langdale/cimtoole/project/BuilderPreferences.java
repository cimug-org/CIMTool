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

	public String getPreference(IResource resource, QualifiedName symbol) {
		try {
			return createSubject(resource.getProjectRelativePath(), getBuilderPreferencesStore(resource.getProject()))
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
		}
		return preferencesStore;
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
			OntModel preferencesStore = getBuilderPreferencesStore(project);
			Property prop = createProperty(symbol);
			IPath path = resource.getProjectRelativePath();
			String extant = createSubject(path, preferencesStore).getString(prop);

			if (extant == null || !extant.equals(value)) {
				OntModel revised = Composition.copy(preferencesStore);
				createSubject(path, revised).setProperty(prop, value, null);
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