/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TextBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.registries.ProfileBuildletConfigUtils;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.ProfileSerializer;
import au.com.langdale.workspace.ResourceOutputStream;

/**
 * An always-on buildlet that generates a hidden PlantUML preview file for every
 * CIMTool profile ({@code .owl}) whenever it is saved.
 *
 * <p>
 * The output is written to the {@code Profiles/} folder alongside all other
 * generated artefacts, but with a dot prefix on the filename so that Eclipse's
 * default resource filters hide it from the Navigator and Project Explorer. For
 * example, {@code Profiles/EndDeviceConfig.owl} produces
 * {@code Profiles/.EndDeviceConfig.puml}.
 *
 * <p>
 * Unlike the user-configurable {@link ProfileBuildlets.ProfileBuildlet}
 * subclasses, this buildlet is wired unconditionally into
 * {@link CIMBuilder#createBuildlets()} and is never gated behind a builder flag
 * in the profile's OWL model.
 *
 * <p>
 * The XSLT style used for generation is read at build time from the
 * {@link Info#CURRENT_PROFILE_PREVIEW_STYLE} global preference
 * ({@code "puml-rdfs-t2b"} or {@code "puml-rdfs-l2r"}), configurable on the
 * PlantUML Builder Preferences page. Because the output filename is always
 * {@code .ProfileName.puml} regardless of which style is selected, changing the
 * preference never leaves stale hidden files behind.
 */
public class PlantUMLRealTimePreviewBuildlet extends TextBuildlet {

	/**
	 * The fixed output extension for the hidden preview file.
	 */
	public static final String PREVIEW_EXT = "puml";

	public PlantUMLRealTimePreviewBuildlet() {
		// Style is null here — resolved dynamically from preferences at build
		// time so preference changes take effect immediately on the next save.
		super(null, PREVIEW_EXT);
	}

	/**
	 * Returns the hidden output {@code IFile} for the given profile resource. The
	 * file lives in the folder:
	 * 
	 * <pre>
	 * Profiles/
	 * </pre>
	 * 
	 * with a dot-prefixed name:
	 * 
	 * <pre>
	 * Profiles/.EndDeviceConfig.puml
	 * </pre>
	 */
	private IFile getOutputFile(IResource owlFile) {
		IFolder profileFolder = Info.getProfileFolder(owlFile.getProject());
		String baseName = owlFile.getName().replaceFirst("\\.[^.]+$", "");
		return profileFolder.getFile("." + baseName + "." + PREVIEW_EXT);
	}

	/**
	 * Returns the hidden preview {@code .puml} file when the changed resource is an
	 * {@code .owl} profile, or an empty collection otherwise.
	 */
	@Override
	protected Collection getOutputs(IResource resource) throws CoreException {
		if (Info.isProfile(resource))
			return Collections.singletonList(getOutputFile(resource));
		return Collections.EMPTY_LIST;
	}

	/**
	 * Generates the hidden preview file.
	 *
	 * <p>
	 * This method owns the full build pipeline rather than delegating to
	 * {@link TransformBuildlet#build(IFile, IProgressMonitor)} because the parent
	 * always re-derives the source {@code .owl} from the output filename via
	 * {@code getRelated(result, "owl")}, which fails for dot-prefixed output files
	 * since the derived path would be {@code .ProfileName.owl} rather than
	 * {@code ProfileName.owl}.
	 *
	 * <p>
	 * Instead we derive the {@code .owl} source directly from the output file's
	 * name and project, then run the same serializer pipeline the parent would,
	 * loading the XSLT style fresh from preferences on every build.
	 */
	@Override
	protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
		String hiddenName = result.getName(); // ".EndDeviceConfig.puml"
		String baseName = hiddenName.replaceFirst("^\\.", "") // "EndDeviceConfig.puml"
				.replaceFirst("\\.[^.]+$", ""); // "EndDeviceConfig"
		IFolder profileFolder = Info.getProfileFolder(result.getProject());
		IFile owlFile = profileFolder.getFile(baseName + ".owl");

		if (!owlFile.exists())
			return;

		String style = Info.getCurrentProfileRealTimePreviewDisplayStyle(owlFile);
		ProfileModel tree = Task.getMessageModel(owlFile);
		if (tree == null)
			return;

		ProfileSerializer serializer = new ProfileSerializer(tree);
		try {
			serializer.setFileName(baseName);
			serializer.setBaseURI(tree.getNamespace());
			serializer.setOntologyURI(tree.getOntologyNamespace());
			serializer.setCopyrightMultiLine(Info.getMultiLineCopyrightText(owlFile.getProject()));
			serializer.setCopyrightSingleLine(Info.getSingleLineCopyrightText(owlFile.getProject()));
			// Pass builder parameters keyed to the owlFile so PlantUML colour
			// preferences etc. are applied exactly as they are for regular builders.
			serializer.setBuilderParameters(Info.getBuilderParameters(owlFile));
			serializer.setNamespacePrefixesBuilderParameter(
					Info.getNamespacePrefixesBuilderParameter(tree.getNsPrefixMap()));
			serializer.setVersion("Preview");

			try {
				InputStream is = ProfileBuildletConfigUtils.getTransformBuildletInputStream(style);
				if (is != null) {
					serializer.setStyleSheet(is, ProfileSerializer.XSDGEN);
				} else {
					serializer.setStyleSheet(style);
				}
			} catch (Exception e) {
				serializer.setStyleSheet(style);
			}

			setupPostProcessors(serializer);
		} catch (TransformerConfigurationException e) {
			throw new CoreException(new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID,
					"Error configuring XSLT stylesheet for preview buildlet", e));
		}

		try {
			/**
			 * Refresh the workspace cache before writing to prevent "Resource is out of
			 * sync" errors when two jobs race to write the same hidden .puml file.
			 */
			if (result.exists())
				result.refreshLocal(IResource.DEPTH_ZERO, monitor);
			ResourceOutputStream ostream = new ResourceOutputStream(result, monitor, false, true);
			serializer.write(ostream);
			ostream.close();
			result.setDerived(true, monitor);
		} catch (TransformerException e) {
			throw new CoreException(new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID,
					"Error transforming profile for preview buildlet", e));
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID, "Error writing preview buildlet output", e));
		}
	}

	/**
	 * Overrides the flag-gated {@code run()} in
	 * {@link ProfileBuildlets.ProfileBuildlet} so this buildlet always executes
	 * regardless of whether any builder flag is set in the profile's OWL model.
	 */
	@Override
	public void run(IFile result, boolean cleanup, IProgressMonitor monitor) throws CoreException {
		if (cleanup) {
			clean(result, monitor);
		} else {
			build(result, monitor);
		}
	}
}
