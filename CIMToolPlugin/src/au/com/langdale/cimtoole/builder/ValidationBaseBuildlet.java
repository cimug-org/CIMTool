/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.ResourceOutputStream;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.SimpleOWLBuildlet;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.util.Logger;
import au.com.langdale.util.Profiler;
import au.com.langdale.util.Profiler.TimeSpan;
import au.com.langdale.validation.ValidatorUtil.ValidatorProtocol;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ModelFactory;

import com.hp.hpl.jena.shared.JenaException;
/**
 * Buildlet to generate diagnostics for a CIM/XML instance. 
 * Separate subclasses handle direct validation of (small) CIM/XML
 * instances and large split models.
 */
public abstract class ValidationBaseBuildlet extends Buildlet {

	public static final String EXT = "log";
	private ValidatorProtocol previousValidator = null;
	private IFile previousProfile = null;

	protected abstract boolean isInstanceResource(IResource resource);
	protected abstract ValidatorProtocol getValidator(OntModel schema, String namespace, InputStream ruleText) throws ParserException, IOException;
	
	@Override
	protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
		TimeSpan span = new TimeSpan("Total Validation - " + result.getName());
		IResource instance = Info.getInstanceFor(result);
		if( instance == null) {
			clean(result, monitor);
			IFile xerrors = Info.getRelated(result, "xml-log");
			if( xerrors.exists())
				xerrors.delete(false, monitor);
			return;
		}

		IFile profile = Info.getProfileFor(instance);
		if( profile == null) {
			clean(result, monitor);
			return;
		}

		IFile schema = Info.getRelated(profile, "simple-owl");
		IFile rules = Info.getRulesFor(instance);
		IResource base = Info.getBaseModelFor(instance);

		if( ! schema.exists()) {
			SimpleOWLBuildlet subordinate = new SimpleOWLBuildlet("RDF/XML", "simple-owl", false);
			subordinate.build(schema, monitor);
			if( ! schema.exists())
				return;
		}

		validate(result, instance, base, schema, rules, monitor);
		span.stop();
		Profiler.print();
	}

	@Override
	protected void clean(IFile result, IProgressMonitor monitor) throws CoreException {
		if( result.exists())
			result.delete(false, monitor);
		
		IFile diagnostic = Info.getRelated(result, "diagnostic");
		if( diagnostic.exists())
			diagnostic.delete(false, monitor);
	}
	
	
	@Override
	protected Collection getOutputs(IResource file) throws CoreException {
		if(isInstanceResource(file))
			return Collections.singletonList(Info.getRelated(file, EXT));
		
		if( Info.isProfile(file) || Info.isRuleSet(file))
			return getResultsFor(file);
		
		return Collections.EMPTY_LIST;
	}

	private Collection getResultsFor(final IResource file) throws CoreException {
		final ArrayList affected = new ArrayList();

		IResourceVisitor resourceVisitor = new IResourceVisitor() {
			
			public boolean visit(IResource resource) throws CoreException {
				if( isInstanceResource(resource)) {
					IFile profile = Info.getProfileFor(resource);
					if( profile != null) {
						if(	profile.equals(file))
							affected.add(Info.getRelated(resource, EXT));
						else {
							IFile rules = Info.getRulesFor(resource);
							if( rules != null && rules.equals(file) )
								affected.add(Info.getRelated(resource, EXT));
						}
					}
				
					return false;
				}

				return true;
			}
		};
		
		file.getProject().accept(resourceVisitor, IResource.DEPTH_INFINITE, false);

		return affected;
	}

	private ValidatorProtocol selectValidator(IFile profile, IFile rules, IProgressMonitor monitor)	throws CoreException, ParserException, IOException {
		if( previousProfile != null && profile.equals(previousProfile))
			return previousValidator;

		OntModel schema = CIMToolPlugin.getCache().getOntologyWait(profile);
		monitor.worked(1);

		InputStream contents;
		if( rules != null)
			contents = rules.getContents();
		else
			contents = null;
			
		String namespace = Info.getProperty(Info.PROFILE_NAMESPACE, profile);
		previousValidator = getValidator(schema, namespace, contents);
		previousProfile = profile;
		monitor.worked(1);

		return previousValidator;
	}

	private void validate(IFile result, IResource instance, IResource base, IFile schema,
			IFile rules, IProgressMonitor monitor) throws CoreException {

		IFile diagnostic = Info.getRelated(result, "diagnostic");

		if( rules != null)
			CIMBuilder.removeMarkers(rules);

		Logger logger = new Logger(new ResourceOutputStream(result, monitor, true, true));
		try {
			String namespace = Info.getProperty(Info.INSTANCE_NAMESPACE, instance);
			String instpath = instance.getLocation().toOSString();
			String basepath = base != null? base.getLocation().toOSString(): null;

			ValidatorProtocol validator = selectValidator(schema, rules, monitor);
			OntModel model = validator.run(instpath, basepath, namespace, logger);
			
			Task.write(model, null, false, diagnostic, "TURTLE", monitor);
			diagnostic.setDerived(true);
			logger.close();
		}
		catch (ParserException e) {
			if(rules != null ) {
				CIMBuilder.addMarker(rules, e.getMessage(), e.getLine(), IMarker.SEVERITY_ERROR);
				clearDiagnostics(result, diagnostic, monitor);
				return;
			}
			else {
				clearDiagnostics(result, diagnostic, monitor);
				error(e);
			}
		} catch (JenaException e) {
			clearDiagnostics(result, diagnostic, monitor);
			error(e);
		} catch (IOException e) {
			clearDiagnostics(result, diagnostic, monitor);
			error(e);
		}

		monitor.worked(1);
		
		int count = logger.getErrorCount();
		if( count > 0) {
			CIMBuilder.addMarker(diagnostic, instance.getName() + " has " + count + " validation error" + (count > 1? "s": ""));
		}
		else {
			CIMBuilder.removeMarkers(diagnostic);
		}
	}
	private void clearDiagnostics(IFile result, IFile diagnostic, IProgressMonitor monitor)
			throws CoreException {
		if( result.exists())
			result.delete(false, monitor);
		Task.write(ModelFactory.createMem(), null, false, diagnostic, "TURTLE",	monitor);
		diagnostic.setDerived(true);
		CIMBuilder.removeMarkers(diagnostic);
	}
	
	private void error(Exception e) throws CoreException {
		throw Info.error("could not validate", e);
	}
}
