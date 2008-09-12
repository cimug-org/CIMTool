/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.jena;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Adapt a Jena Model as a TreeModel.  
 * 
 * Implementations must subclass this, provide Node subclasses 
 * and fill in the classify() method.
 * 
 */
abstract public class JenaTreeModelBase extends TreeModelBase {
	private OntModel ontModel;
	private Resource rootResource;
	private String source;
	
	/**
	 * A Node where the subject represents a definition in an ontology.
	 * (Sometimes called a 'model' probably incorrectly.)
	 */
	public abstract class ModelNode extends Node {
		
		/**
		 * Access the tree model containing this node. 
		 * (From there, the ontology can be obtained.)
		 */
		public JenaTreeModelBase getModel() {
			return JenaTreeModelBase.this;
		}
		
		/**
		 * Provide a name for the package or document that contains this
		 * definition.
		 */
		public String getPackageName() {
			return extractPackageName(getBase());
		}

	}
	
	private String extractPackageName(OntResource subject) {
		Resource defin = subject.getIsDefinedBy();
		if( defin != null ){
			return label((OntResource)defin.as(OntResource.class));
		}
		else if( source != null ){
			return ResourceFactory.createResource(source).getLocalName();
		}
		else {
			return "";
		}
	}

	/**
	 * Construct an initially empty tree.
	 *
	 */
	public JenaTreeModelBase() {
		setRoot(new Empty("empty model"));
	}
	
	/**
	 * The currently displayed ontology.
	 * @return Jena ontology.
	 */
	public OntModel getOntModel() {
		return ontModel;
	}
	
	/**
	 * Set the ontology and update the tree.
	 */
	public void setOntModel(OntModel model) {
		ontModel = model;
		init();
	}
	
	/**
	 * Set the ontology and update the tree.
	 */
	public void setOntModel(Model model) {
		if( model == null ) {
			setOntModel((OntModel)null);
		}
		else
			setOntModel(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_TRANS_INF, model));
	}
	/**
	 * Display an ontology from a file.
	 * @param filename the file containing an OWL ontology in RDF/XML syntax.
	 * @throws FileNotFoundException
	 */
	public void setOntModel( String filename ) throws FileNotFoundException {
		setRoot(new Empty("loading " + filename + " ..."));
		Model input = ModelFactory.createDefaultModel();
		input.read(new BufferedInputStream( new FileInputStream(filename)), new File(filename).toURI().toString());
		setOntModel(input);
	}

	/**
	 * The URI of the ontology resource at the root of the tree. 
	 * @return a URI as a String
	 */
	public Resource getRootResource() {
		return rootResource;
	}
	
	/**
	 * Set the root resource and update the tree.
	 * @param root the root resource URI as a String.
	 */
	public void setRootResource(String root) {
		rootResource = ResourceFactory.createResource(root);
		init();
	}
	
	/**
	 * Set the root resource and update the tree.
	 * @param root the root resource URI as a Resource.
	 */
	public void setRootResource(Resource root) {
		rootResource = root;
		init();
	}

	/**
	 * Called after the model or rootResource has changed.
	 *
	 */
	private void init() {
		if( ontModel != null && rootResource != null ) 
			setRoot(classify(asOntResource(rootResource)));
		else 
			setRoot(new Empty("Pending ..."));
	}

	protected OntResource asOntResource(RDFNode subject) {
		if( subject != null && ontModel != null)
			return (OntResource)(subject.inModel(ontModel).as(OntResource.class));
		else
			return null;
	}
	
	/**
	 * Called to create the root node of the tree given the root resource.
	 * 
	 * If the resource is not suitable this may return null or throw ConversionException.
	 */
	abstract protected Node classify(OntResource root) throws ConversionException;

	/**
	 * The source file containing this message definition as a pathname.
	 */
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
