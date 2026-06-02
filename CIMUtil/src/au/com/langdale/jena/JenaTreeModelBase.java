/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.jena;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;

import com.hp.hpl.jena.graph.FrontsNode;
import au.com.langdale.kena.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Adapt a Jena Model as a TreeModel.  
 * 
 * Implementations must subclass this, provide Node subclasses 
 * and fill in the classify() method.
 * 
 */
abstract public class JenaTreeModelBase extends TreeModelBase {
	private OntModel ontModel;
	private FrontsNode rootResource;
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
		
		/**
		 * Provide a name for the package or document that contains this
		 * definition.
		 */
		public OntResource getPackage() {
			return extractPackage(getBase());
		}

	}
	
	private String extractPackageName(OntResource subject) {
		OntResource defin = subject.getResource(RDFS.isDefinedBy);
		if( defin != null ){
			return label(defin);
		}
		else if( source != null ){
			return com.hp.hpl.jena.graph.Node.createURI(source).getLocalName();
		}
		else {
			return "";
		}
	}
	
	private OntResource extractPackage(OntResource subject) {
		OntResource defin = subject.getResource(RDFS.isDefinedBy);
		return defin;
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
	 * The URI of the ontology resource at the root of the tree. 
	 * @return a URI as a String
	 */
	public FrontsNode getRootResource() {
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
	public void setRootResource(FrontsNode root) {
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

	protected OntResource asOntResource(FrontsNode subject) {
		if( subject != null && ontModel != null)
			return ontModel.createResource(subject.asNode());
		else
			return null;
	}
	
	/**
	 * Called to create the root node of the tree given the root resource.
	 * 
	 * If the resource is not suitable this may return null or throw ConversionException.
	 */
	abstract protected Node classify(OntResource root);

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
