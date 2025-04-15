package au.com.langdale.cimtoole.project;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.util.Jobs;

public class Settings extends Task {
	public Settings() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				new ResourceListener(), IResourceChangeEvent.POST_CHANGE);
	}
	/**
	 * The session property used to cache models. 
	 */
	public static final QualifiedName SETTINGS = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "settings");
	
	public static final QualifiedName[] MIGRATED_SYMBOLS = new QualifiedName[] {
		PROFILE_PATH,
		BASE_MODEL_PATH,
		PROFILE_ENVELOPE,
		SCHEMA_NAMESPACE,
		MERGE_SHADOW_EXTENSIONS,
		SELF_HEAL_ON_IMPORT,
		PROFILE_NAMESPACE,
		INSTANCE_NAMESPACE,
		MERGED_SCHEMA_PATH
	};
	
	private OntModel migrateSettings(IProject project) throws CoreException {
		Migrator visitor = new Migrator();
		project.accept(visitor);
		OntModel store = visitor.getStore();
		project.setSessionProperty(SETTINGS, store);
		
		System.out.println("Saving migrated settings for " + project.getName());
		Jobs.runJob(saveSettings(project, store), project, "Migrating CIMTool settings");
		return store;
	}
	
	private class Migrator implements IResourceVisitor {
		private OntModel store = ModelFactory.createMem();
		
		public OntModel getStore() {
			return store;
		}

		public boolean visit(IResource resource) throws CoreException {
			Map props = resource.getPersistentProperties();
			if( ! props.isEmpty()) {
				OntResource subject = createSubject(resource.getProjectRelativePath(), store);
				for(int ix = 0; ix < MIGRATED_SYMBOLS.length; ix++) {
					Property prop = createProperty(MIGRATED_SYMBOLS[ix]);
					String value = (String) props.get(MIGRATED_SYMBOLS[ix]);
					if( value != null )
						subject.addProperty(prop, value);
				}
			}
			return true;
		}
	}
	
	
	public String getSetting(IResource resource, QualifiedName symbol) {
		try {
			return createSubject(resource.getProjectRelativePath(), getSettingsStore(resource.getProject())).getString(createProperty(symbol));
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}


	private Property createProperty(QualifiedName symbol) {
		String qualifier = symbol.getQualifier();
		if( qualifier == null || CIMToolPlugin.PLUGIN_ID.equals(qualifier))
			qualifier = CIMToolPlugin.SETTING_NS;
		else
			qualifier += "#";

		return ResourceFactory.createProperty(qualifier + symbol.getLocalName());
	}

	private OntResource createSubject(IPath path, OntModel store) {
		try {
			return store.createResource(CIMToolPlugin.PROJECT_NS + new URI(null, path.toPortableString(), null).toASCIIString());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private OntModel getSettingsStore(IProject project) throws CoreException {
		OntModel store = (OntModel) project.getSessionProperty(SETTINGS);
		if( store == null ) {
			IFile settings = getSettings(project);
			if( settings.exists()) {
				System.out.println("Loading settings for " + project.getName());
				store = Task.parse(settings);
				project.setSessionProperty(SETTINGS, store);
			}
			else
				store = migrateSettings(project);
		}
		return store;
	}
	
	private void replaceSettingsStore(IProject project, OntModel store) throws CoreException {
		IFile settings = getSettings(project);
		project.setSessionProperty(SETTINGS, store);

		System.out.println("Saving modified settings for " + project.getName());
		Jobs.runJob(saveSettings(project, store), settings, "Saving CIMTool settings");
	}
	
	public void putSetting(IResource resource, QualifiedName symbol, String value) {

		try {
			IProject project = resource.getProject();
			OntModel store = getSettingsStore(project);
			Property prop = createProperty(symbol);
			IPath path = resource.getProjectRelativePath();
			String extant = createSubject(path, store).getString(prop);
			
			if( extant == null || ! extant.equals(value)) {
				OntModel revised = Composition.copy(store);
				createSubject(path, revised).setProperty(prop, value, null);
				replaceSettingsStore(project, revised);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

	}
	
	private class ResourceListener implements IResourceChangeListener {

		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() != IResourceChangeEvent.POST_CHANGE)
				return;

			DeltaVisitor visitor = new DeltaVisitor();
			try {
				event.getDelta().accept(visitor);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
			
			Iterator it = visitor.getRevised().entrySet().iterator();
			while( it.hasNext()) {
				Entry entry = (Entry) it.next();
				try {
					replaceSettingsStore((IProject)entry.getKey(), (OntModel)entry.getValue());
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private class DeltaVisitor implements IResourceDeltaVisitor {
		private Map revised = new HashMap();
		
		public Map getRevised() {
			return revised;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			if( delta.getResource().getType() == IResource.PROJECT 
					&& (delta.getKind() == IResourceDelta.ADDED 
							|| delta.getKind() == IResourceDelta.REMOVED))
				return false;
			
			if((delta.getFlags()&IResourceDelta.MOVED_TO) != 0)
				copySettings(delta.getResource().getFullPath(), delta.getMovedToPath());
			if((delta.getFlags()&IResourceDelta.MOVED_FROM) != 0)
				copySettings(delta.getMovedFromPath(), delta.getResource().getFullPath());
			if( delta.getKind() == IResourceDelta.REMOVED) {
				removeSettings(delta.getResource().getFullPath());

			}
			return true;
		}
		
		private OntModel getRevised(IProject project) throws CoreException {
			OntModel result = (OntModel) revised.get(project);
			if( result == null ) {
				result = Composition.copy(getSettingsStore(project));
				revised.put(project, result);
			}
			return result;
		}

		private void removeSettings(IPath fullPath) throws CoreException {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(fullPath.segment(0));
			OntModel store = getSettingsStore(project);
			OntResource subject = createSubject(fullPath.removeFirstSegments(1), store);
			
			if( store.contains(subject)) {
				OntModel target = getRevised(project);
				target.removeSubject(subject);
			}
		}

		private void copySettings(IPath patha, IPath pathb) throws CoreException {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(patha.segment(0));
			OntModel store = getSettingsStore(project);
			OntResource subject = createSubject(patha.removeFirstSegments(1), store);
			
			Iterator it = store.getGraph().find(subject.asNode(), Node.ANY, Node.ANY);
 			if( it.hasNext()) {
 				IProject target_project = root.getProject(pathb.segment(0));
 				OntModel target_store = getRevised(target_project);
 				OntResource target_subject = createSubject(pathb.removeFirstSegments(1),target_store);
 				do {
 					Triple t = (Triple) it.next();
 					Property prop = ResourceFactory.createProperty(t.getPredicate());
 					target_subject.addProperty(prop, t.getObject());
 				} while( it.hasNext());
 			}
		}
	}
}
