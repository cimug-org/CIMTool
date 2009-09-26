/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.builder;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.xml.sax.SAXException;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.ResourceOutputStream;
import au.com.langdale.cimtoole.project.Cache;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.OWLGenerator;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.ProfileSerializer;
import au.com.langdale.profiles.RDFSBasedGenerator;
import au.com.langdale.profiles.RDFSGenerator;
import au.com.langdale.ui.binding.BooleanModel;
import au.com.langdale.ui.binding.OntModelProvider;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
/**
 * A series of <code>Buildlet</code>s for building profile artifacts.
 */
public class ProfileBuildlets extends Info {
	/**
	 * Buildlet for a profile artifact. 
	 * 
	 * Each type of profile buildlet is characterised by a specific file type
	 * and a flag in the profile that enables it.
	 */
	public abstract static class ProfileBuildlet extends Buildlet {
		private String ext;
		
		protected ProfileBuildlet(String fileType) {
			ext = fileType;
		}
	
		@Override
		protected Collection getOutputs(IResource file) throws CoreException {
			if(isProfile(file))
				return Collections.singletonList(getRelated(file, getFileType()));
			else
				return Collections.EMPTY_LIST;
		}
		
		public BooleanModel getFlag(final OntModelProvider context) {
			return new BooleanModel() {
				
				public boolean isTrue() {
					OntModel model = context.getModel();
					return model != null && isFlagged(model);
				}
				
				public void setTrue(boolean flag) {
					OntModel model = context.getModel();
					if( model != null)
						setFlagged(model, flag);
				}

				@Override
				public String toString() {
					return "Builder for " + getFileType();
				}
			};
		}
		
		public Resource getIdentifier() {
			return ResourceFactory.createResource(NS + getFileType()); 
		}
		
		public String getFileType() {
			return ext;
		}
		
		protected ProfileModel getMessageModel(IFile file) throws CoreException {
			ProfileModel model = new ProfileModel();
			model.setNamespace(getProperty(file, PROFILE_NAMESPACE));
			model.setOntModel(getProfileModel(file));
			model.setBackgroundModel(getBackgroundModel(file));
			model.setRootResource(MESSAGE.profile);
			return model;
		}

		protected OntModel getBackgroundModel(IFile file) throws CoreException {
			Cache cache = CIMToolPlugin.getCache();
			IFolder schema = getSchemaFolder(file.getProject());
			return cache.getMergedOntologyWait(schema);
		}

		protected OntModel getProfileModel(IFile file) throws CoreException {
			Cache cache = CIMToolPlugin.getCache();
			return cache.getOntologyWait(file);
		}

		public boolean isFlagged(IFile file) throws CoreException {
			Cache cache = CIMToolPlugin.getCache();
			OntModel model = cache.getOntologyWait(file);
			return model != null &&  isFlagged(model);
		}

		public boolean isFlagged(OntModel model) {
			return model.contains(getIdentifier(), RDF.type, MESSAGE.Flag );
		}

		public void setFlagged(OntModel model, boolean flag) {
			if(flag)
				model.add(getIdentifier(), RDF.type, MESSAGE.Flag);
			else
				model.remove(getIdentifier(), RDF.type, MESSAGE.Flag);
		}

		@Override
		public void run(IFile result, boolean cleanup, IProgressMonitor monitor) throws CoreException {
			IFile file = getRelated(result, "owl");
			if( cleanup || ! file.exists() || ! isFlagged(file))
				clean( result, monitor );
			else
				build( result, monitor );				
		}
	}
	/**
	 * Buildlet for a profile artifact that is the product of an XSLT transform.
	 */
	public static class TransformBuildlet extends ProfileBuildlet {
		private String style;

		public TransformBuildlet(String style, String ext) {
			super(ext);
			this.style = style;
		}
		
		@Override
		protected Collection getOutputs(IResource file) throws CoreException {
			if(isProfile(file) || isRuleSet(file, style + "-xslt") && isProfile(getRelated(file, "owl")))
				return Collections.singletonList(getRelated(file, getFileType()));
			else
				return Collections.EMPTY_LIST;
		}
		
		protected void setupPostProcessors( ProfileSerializer serializer) throws TransformerConfigurationException {}
		
		@Override
		protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
			IFile file = getRelated(result, "owl");
			ProfileModel tree = getMessageModel(file);
			ProfileSerializer serializer = new ProfileSerializer(tree);
			try {
				serializer.setBaseURI(getProperty(file, PROFILE_NAMESPACE));
				
				// TODO: make this better
				serializer.setVersion("Beta");
				
				IFile local = getRelated(result, style + "-xslt");
				if( local.exists()) {
					serializer.setErrorHandler(CIMBuilder.createErrorHandler(local));
					serializer.setStyleSheet(local.getContents(), ProfileSerializer.XSDGEN);
				}
				else {
					serializer.setStyleSheet(style);
				}
				
				setupPostProcessors(serializer);
				
			} catch (TransformerConfigurationException e) {
				error("error parsing XSLT script", e);
			}
			
			try {
			    serializer.write(new ResourceOutputStream(result, monitor, false, true));
			} catch (TransformerException e) {
			    error("error transforming profile", e);
		    }
			catch(IOException e) {
				error("error writing output", e);
			}	
		}
	}
	/**
	 * Buildlet for XML Schema profiles.
	 * 
	 * The basic XSLT transform is followed by XML Schema validation.
	 */
	public static class XSDBuildlet extends TransformBuildlet {
		public XSDBuildlet() {
			this("xsd");
		}
		public XSDBuildlet(String style) {
			super(style, "xsd");
		}
		
		@Override
		protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
			super.build(result, monitor);
			SchemaFactory parser = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			parser.setErrorHandler(CIMBuilder.createErrorHandler(result));
			Source source = new StreamSource(result.getContents());
			try {
				parser.newSchema(source);
			} catch (SAXException e) {
				throw error("error validating generated schema", e);
			}
		}
	}
	
	/**
	 * Buildlet for java artifacts.
	 */
	public static class TextBuildlet extends TransformBuildlet {

		public TextBuildlet(String style, String ext) {
			super(style, ext);
		}
		
		@Override
		protected void setupPostProcessors( ProfileSerializer serializer) throws TransformerConfigurationException {
			serializer.addStyleSheet("indent");
		}
	}
	
	/**
	 * Buildlet for profile artifacts that are related to the simplified RDFS
	 * representation.  
	 */
	public static abstract class RDFSBasedBuildlet extends ProfileBuildlet {
		private String lang;
		protected boolean withInverses;
		
		protected RDFSBasedBuildlet(String lang, String fileType, boolean withInverses) {
			super(fileType);
			this.lang = lang;
			this.withInverses = withInverses;
		}

		@Override
		protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
			IFile file = getRelated(result, "owl");
			RDFSBasedGenerator generator = getGenerator(file);
			generator.run();
			System.out.println("Generated ontology size: " + generator.getResult().size());
			Task.write(generator.getResult(), getNamespace(file), true, result, lang, monitor);
			result.setDerived(true);
		}

		protected String getNamespace(IFile file) throws CoreException {
			return getProperty(file, PROFILE_NAMESPACE);
		}

		protected abstract RDFSBasedGenerator getGenerator(IFile file) throws CoreException ;
	}
	/**
	 * Buildlet for the simple OWL representation of the profile.
	 */
	public static class SimpleOWLBuildlet extends RDFSBasedBuildlet {
		public SimpleOWLBuildlet(String lang, String fileType, boolean withInverses) {
			super(lang, fileType, withInverses);
		}

		@Override
		protected RDFSBasedGenerator getGenerator(IFile file) throws CoreException {
			return new OWLGenerator(getProfileModel(file), getBackgroundModel(file), getNamespace(file), getPreferenceOption(PRESERVE_NAMESPACES), withInverses);
		}
	}
	/**
	 * Buildlet for a profile in the original IEC RDFS language. 
	 */
	public static class LegacyRDFSBuildlet extends RDFSBasedBuildlet {
		public LegacyRDFSBuildlet(String lang, String fileType, boolean withInverses) {
			super(lang, fileType, withInverses);
		}

		@Override
		protected RDFSBasedGenerator getGenerator(IFile file) throws CoreException {
			return new RDFSGenerator(getProfileModel(file), getBackgroundModel(file), getNamespace(file), getPreferenceOption(PRESERVE_NAMESPACES), withInverses);
		}
	}
	
	public static BooleanModel[] getAvailable(OntModelProvider context) {
		ProfileBuildlet[] buildlets = getAvailable();
		BooleanModel[] flags = new BooleanModel[buildlets.length];
		for(int ix = 0; ix < buildlets.length; ix++)
			flags[ix] = buildlets[ix].getFlag(context);
		return flags;
	}
	/**
	 * @return: a list of all profile buildlets.
	 */
	private static ProfileBuildlet[] getAvailable() {
		return new ProfileBuildlet[] {
				
				new XSDBuildlet(),
				new TransformBuildlet(null, "xml"),
				new TransformBuildlet("html", "html"),
				new TextBuildlet("sql", "sql"),
				new TextBuildlet("jpa", "java"),
				new SimpleOWLBuildlet("RDF/XML", "simple-owl", false),
				new LegacyRDFSBuildlet("RDF/XML", "legacy-rdfs", false),
				new SimpleOWLBuildlet("RDF/XML", "simple-owl-augmented", true),
				new LegacyRDFSBuildlet("RDF/XML", "legacy-rdfs-augmented", true),
				
			};
	}
}
