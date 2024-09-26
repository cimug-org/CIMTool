/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.profiles.ProfileModel.CatalogNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode.MessageNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode.SubTypeNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.EnumValueNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.SuperTypeNode;
import au.com.langdale.profiles.ProfileModel.TypeNode;
import au.com.langdale.sax.AbstractReader;
import au.com.langdale.xmi.UML;

/**
 * Convert a message model to XML. The form of the XML is designed to be easily
 * transformed to specific schemas in any schema language.
 * 
 * A MessageSerializer is a SAX XMLReader. Calling parse(InputSource) causes it
 * to emit SAX events representing a message definition.
 * 
 * However, the write() method is a more convenient way to generate XML to a
 * file.
 * 
 * If setStylesheet() is called before write, the output is transformed into a
 * schema according to the given templates.
 */
public class ProfileSerializer extends AbstractReader {
	public static final String XSDGEN = "http://langdale.com.au/2005/xsdgen";
	private JenaTreeModelBase model;
	private String baseURI = "";
	private String ontologyURI = "";
	private String version = "";
	private String copyrightMultiLine = "";
	private String copyrightSingleLine = "";
	private ArrayList templates = new ArrayList();
	TransformerFactory factory = TransformerFactory.newInstance();
	private final String xsd = XSD.anyURI.getNameSpace();
	private HashSet deferred = new HashSet();

	/**
	 * Construct a serializer for the given MessageModel.
	 */
	public ProfileSerializer(ProfileModel model) {
		this.model = model;
	}

	/**
	 * A URI that is passed as the baseURI attribute of the root element and is
	 * generally used to establish a namespace for the generated schema.
	 */
	public String getBaseURI() {
		return baseURI;
	}

	/**
	 * Set the base URI (see above).
	 */
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * A URI that is passed as the onotologyURI attribute of the root element of
	 * the ontology from which the profile model was derived from. This is
	 * generally used to establish an xml:base URI that may be necessary for
	 * RDF-based schema that may be generated.
	 */
	public String getOntologyURI() {
		return ontologyURI;
	}

	/**
	 * Set the ontologies base URI (see above).
	 */
	public void setOntologyURI(String ontologyURI) {
		if (ontologyURI != null)
			this.ontologyURI = ontologyURI;
	}

	/**
	 * A single-line copyright notification including a year. It will be passed
	 * as an attribute named "coyright-single-line" to the serializer therefore
	 * making it available to a profile builder for use as the copyright notice
	 * within the generated schema if the schema builder desires a single lined
	 * copyright as opposed to a multi-line copyright.
	 */
	public String getCopyrightSingleLine() {
		return copyrightSingleLine;
	}

	/**
	 * Set the single line copyright notice (see above).
	 */
	public void setCopyrightSingleLine(String copyrightSingleLine) {
		this.copyrightSingleLine = copyrightSingleLine;
	}

	/**
	 * A multi-line copyright notification including a year. It will be passed
	 * as an attribute named "coyright" to the serializer therefore making it
	 * available to a profile builder for use as the copyright notice within the
	 * generated schema.
	 */
	public String getCopyrightMultiLine() {
		return copyrightMultiLine;
	}

	/**
	 * Set the base URI (see above).
	 */
	public void setCopyrightMultiLine(String copyright) {
		this.copyrightMultiLine = copyright;
	}

	/**
	 * Install a stylesheet to transform the abstract message definition to a
	 * schema.
	 */
	public void setStyleSheet(InputStream s, String base)
			throws TransformerConfigurationException {
		templates.clear();
		addStyleSheet(s, base);
	}

	/**
	 * Install a stylesheet to apply after any previously installed stylesheets.
	 */
	public void addStyleSheet(InputStream s, String base)
			throws TransformerConfigurationException {
		templates.add(factory.newTemplates(new StreamSource(s, base)));
	}

	/**
	 * Install a stylesheet from the standard set. Use null for no stylesheet.
	 * 
	 * When the name is null it indicates that no stylesheets are to be applied.
	 * In this scenario the output of a call to the ProfileSerializer's write()
	 * method will simply be the output of the CIMTool's internal message model
	 * as a formatted XML document. Therefore, any existing templates are
	 * "cleared" and will apply only the "indent-xml.xsl" stylesheet to format
	 * the XML when written. This stylesheet is for internal purposes only.
	 */
	public void setStyleSheet(String name)
			throws TransformerConfigurationException {
		if (name == null) {
			templates.clear();
			setStyleSheet(getClass().getResourceAsStream("indent-xml.xsl"),
					XSDGEN);
		} else {
			setStyleSheet(getClass().getResourceAsStream(name + ".xsl"), XSDGEN);
		}
	}

	/**
	 * Install a stylesheet to apply after any previously installed stylesheets.
	 */
	public void addStyleSheet(String name)
			throws TransformerConfigurationException {
		if (name != null)
			addStyleSheet(getClass().getResourceAsStream(name + ".xsl"), "");
	}

	/**
	 * Install a standard SAX ErrorHandler for errors in the stylesheet. This
	 * will be wrapped in the ErrorListener required by the transform framework.
	 */
	@Override
	public void setErrorHandler(final ErrorHandler errors) {
		ErrorListener listener = new ErrorListener() {

			public void error(TransformerException ex)
					throws TransformerException {
				try {
					errors.error(convert(ex));
				} catch (SAXException e) {
					throw convert(e);
				}
			}

			public void warning(TransformerException ex)
					throws TransformerException {
				try {
					errors.warning(convert(ex));
				} catch (SAXException e) {
					throw convert(e);
				}
			}

			public void fatalError(TransformerException ex)
					throws TransformerException {
				try {
					errors.fatalError(convert(ex));
				} catch (SAXException e) {
					throw convert(e);
				}
			}

			private SAXParseException convert(TransformerException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof SAXParseException)
					return (SAXParseException) cause;

				SourceLocator loc = ex.getLocator();
				if (loc != null)
					return new SAXParseException(ex.getMessage(),
							loc.getPublicId(), loc.getSystemId(),
							loc.getLineNumber(), loc.getColumnNumber());

				return new SAXParseException(ex.getMessage(), "", "", 0, 0);
			}

			private TransformerException convert(SAXException ex) {
				return new TransformerException(ex.getMessage(), ex);
			}
		};

		factory.setErrorListener(listener);
	}

	/**
	 * Generate a schema from the message definition and write it to the given
	 * stream.
	 */
	@Override
	public void write(OutputStream ostream) throws TransformerException {
		Transformer[] tx;
		if (!templates.isEmpty()) {
			tx = new Transformer[templates.size()];
			for (int ix = 0; ix < templates.size(); ix++) {
				Transformer ti = ((Templates) templates.get(ix))
						.newTransformer();
				ti.setParameter("baseURI", baseURI);
				ti.setParameter("ontologyURI", ontologyURI);
				ti.setParameter("version", version);
				ti.setParameter("envelope", model.getRoot().getName());
				ti.setParameter("copyright", copyrightMultiLine);
				ti.setParameter("copyright-single-line", copyrightSingleLine);
				tx[ix] = ti;
			}
		} else {
			tx = new Transformer[] { factory.newTransformer() };
		}

		Result result = new StreamResult(ostream);
		Source source = new SAXSource(this, new InputSource());
		for (int ix = 0; ix < tx.length - 1; ix++) {
			DOMResult inter = new DOMResult();
			tx[0].transform(source, inter);
			source = new DOMSource(inter.getNode());
		}
		tx[tx.length - 1].transform(source, result);
	}

	protected void emit() throws SAXException {
		emit(model.getRoot());
	}

	private void emitPackages(CatalogNode node) throws SAXException {
		SortedMap<String, OntResource> packages = new TreeMap<String, OntResource>();
		Iterator it = node.iterator();
		while (it.hasNext()) {
			Node nextNode = (Node) it.next();
			if (nextNode instanceof TypeNode) {
				TypeNode typeNode = (TypeNode) nextNode;
				OntResource aPackage = typeNode.getPackage();
				while (aPackage != null) {
					if (!packages.containsKey(aPackage.getURI()))
						packages.put(aPackage.getURI(), aPackage);
					aPackage = aPackage.getIsDefinedBy();
				}
			}
		}
		for (OntResource aPackage : packages.values()) {
			emitPackage(aPackage);
		}
	}

	private void emit(Node node) throws SAXException {
		if (node instanceof CatalogNode)
			emit((CatalogNode) node);
		else if (node instanceof EnvelopeNode)
			emit((EnvelopeNode) node);
		else if (node instanceof MessageNode)
			emit((MessageNode) node);
		else if (node instanceof TypeNode)
			emit((TypeNode) node);
		else if (node instanceof SuperTypeNode)
			emit((SuperTypeNode) node);
		else if (node instanceof ElementNode)
			emit((ElementNode) node);
		else if (node instanceof EnumValueNode)
			emit((EnumValueNode) node);
		else if (node instanceof SubTypeNode)
			emit((SubTypeNode) node);
		else if (node != null)
			emitChildren(node);
	}

	private void emitChildren(Node node) throws SAXException {
		Iterator it = node.iterator();
		while (it.hasNext())
			emit((Node) it.next());
	}

	private void emit(CatalogNode node) throws SAXException {
		Element elem = new Element("Catalog", MESSAGE.NS);
		elem.set("ontologyURI", ontologyURI);
		elem.set("baseURI", baseURI);
		elem.set("xmlns:m", baseURI);
		elem.set("name", node.getName());
		emitNote(node);
		emitPackages(node);
		emitChildren(node);

		Iterator it = deferred.iterator();
		while (it.hasNext()) {
			emit((OntResource) it.next());
		}
		elem.close();
	}

	private void emit(EnvelopeNode node) throws SAXException {
		Element elem = new Element("Message", MESSAGE.NS);
		elem.set("name", node.getName());
		emitChildren(node);
		elem.close();
	}

	private void emit(SuperTypeNode node) throws SAXException {
		Element elem = new Element("SuperType");
		elem.set("name", node.getName());
		elem.set("baseClass", node.getBaseClass().getURI());
		elem.close();
	}

	private void emit(SubTypeNode node) throws SAXException {
		Element elem = select(node);
		elem.set("name", node.getName());
		elem.set("minOccurs", "1");
		elem.set("maxOccurs", "1");
		emitNote(node);
		if (node.getSubject().isAnon())
			emitChildren(node);
		elem.close();
	}

	private Element select(SubTypeNode node) throws SAXException {
		Element elem;
		boolean anon = node.getSubject().isAnon();
		if (node.isEnumerated()) {
			if (anon)
				elem = new Element("SimpleEnumerated");
			else
				elem = new Element("Enumerated");
		} else {
			if (anon)
				elem = new Element("Complex");
			else {
				if (node.getParent() instanceof ElementNode
						&& ((ElementNode) node.getParent()).isReference())
					elem = new Element("Reference");
				else
					elem = new Element("Instance");
			}
		}

		elem.set("baseClass", node.getBaseClass().getURI());
		if (!anon)
			elem.set("type", node.getName());

		return elem;
	}

	private void emit(ElementNode node) throws SAXException {
		Element elem;

		if (node.isDatatype()) {
			OntResource range = node.getBaseProperty().getRange();
			if (range == null)
				range = model.getOntModel()
						.createResource(XSD.xstring.asNode());
			if (range.getNameSpace().equals(xsd)) {
				elem = new Element("Simple");
			} else {
				elem = new Element("Domain");
				elem.set("type", range.getLocalName());
				deferred.add(range);
			}
			elem.set("dataType", range.getURI());
			elem.set("xstype", xstype(range));
			emit(node, elem);
		} else {

			int size = node.getChildren().size();

			if (size == 0) {
				OntResource range = node.getBaseProperty().getRange();
				elem = new Element("Reference");
				if (range != null && range.isURIResource()) {
					elem.set("baseClass",  range.getURI());
					elem.set("type", range.getLocalName());
				}
				emit(node, elem);
				
				OntResource inverse = (node.getBaseProperty() != null ? node.getBaseProperty().getInverse() : null);
				if ((inverse != null) && (node.getProfile().getPropertyInfo(inverse) != null)) {
					elem.close(); // First, close  "Reference" ...
					elem = new Element("InverseReference");
					emitInverse(node, elem);
				}
			} else if (size == 1) {
				SubTypeNode child = (SubTypeNode) node.getChildren().get(0);
				elem = select(child);
				emit(node, elem);
				if (child.getSubject().isAnon()) {
					emitChildren(child);
				} else if (!child.isEnumerated()) {
					// If child is not an enumeration and not anonymous it indicates it is an Instance
					// or Reference element and an InverseInstance or InverseReference must be created...
					OntResource inverse = (node.getBaseProperty() != null ? node.getBaseProperty().getInverse() : null);
					if ((inverse != null) && (node.getProfile().getPropertyInfo(inverse) != null)) {
						elem.close(); // First, close out the "Reference" or "Instance" element...
						if (child.getParent() instanceof ElementNode && ((ElementNode) child.getParent()).isReference()) {
							elem = new Element("InverseReference");
						} else {
							elem = new Element("InverseInstance");
						}
						emitInverse(node, elem);
					}
				}
			} else {
				elem = new Element("Choice");
				emitChoice(node, elem);
				emitChildren(node);
			}
		}

		elem.close();
	}

	private void emitInverse(ElementNode node, Element elem) throws SAXException {
		OntResource inverse = (node.getBaseProperty() != null ? node
				.getBaseProperty().getInverse() : null);
		
		if ((inverse != null) && (node.getProfile().getPropertyInfo(inverse) != null)) {
			elem.set("baseClass", inverse.getRange().getURI());
			elem.set("type", inverse.getRange().getLocalName());
			if (inverse.getLabel() != null)
				elem.set("name", inverse.getLabel());
			if (inverse.getURI() != null)
				elem.set("baseProperty", inverse.getURI());

			PropertyInfo info = node.getProfile().getPropertyInfo(inverse);
			if (!info.getProperty().isDatatypeProperty()) {
				int min = info.getMinCardinality();
				int max = info.getMaxCardinality();
				elem.set("minOccurs", ProfileModel.cardString(min));
				elem.set("maxOccurs",
						ProfileModel.cardString(max, "unbounded"));
			}

			elem.set("inverseBaseProperty", node.getBaseProperty().getURI());
			
			emitComment(info.getProperty().getComment(null));
			emitStereotypes(info.getProperty());
		}
	}
	
	private void emit(ElementNode node, Element elem) throws SAXException {
		elem.set("name", node.getName());
		elem.set("baseProperty", node.getBaseProperty().getURI());
		elem.set("minOccurs", ProfileModel.cardString(node.getMinCardinality()));
		elem.set("maxOccurs",
				ProfileModel.cardString(node.getMaxCardinality(), "unbounded"));
		
		OntResource inverse = (node.getBaseProperty() != null ? node
				.getBaseProperty().getInverse() : null);
		if ((inverse != null) && (inverse.getURI() != null)) {
			elem.set("inverseBaseProperty", inverse.getURI());
		}
		
		emitComment(node.getBaseProperty().getComment(null));
		emitNote(node);
	}

	private void emitChoice(ElementNode node, Element elem) throws SAXException {
		elem.set("name", node.getName());
		elem.set("baseProperty", node.getBaseProperty().getURI());
		elem.set("minOccurs", ProfileModel.cardString(node.getMinCardinality()));
		elem.set("maxOccurs",
				ProfileModel.cardString(node.getMaxCardinality(), "unbounded"));
		//
		// We obtain the topmost class in the hierarchy via the call to
		// node.getBaseClass().
		// We then loop through all of the child nodes of the choice and locate
		// the one that
		// matches the base class associated with the Choice property. This is
		// the one we must
		// use when generating the 'inheritanceBaseClass' and
		// 'inheritanceBaseType' attributes
		// for the Choice. These two attributes indicate the URI of the topmost
		// base class in
		// the hierarchy and the name of that class as specified in the context
		// of the profile
		// defined.
		//
		OntResource baseClass = node.getBaseClass();
		Iterator it = node.iterator();
		while (it.hasNext()) {
			Node childNode = (Node) it.next();
			if (childNode.getBase().equals(baseClass)) {
				elem.set("inheritanceBaseClass", childNode.getSubject()
						.getURI());
				elem.set("inheritanceBaseType", childNode.getName());
				break;
			}
		}

		emitComment(node.getBaseProperty().getComment(null));
		emitNote(node);
	}

	private void emitComment(String comment) throws SAXException {
		emit("Comment", comment);
	}

	private static Pattern delimiter = Pattern.compile(" *([\r\n] *)+");

	private void emit(String ename, String comment) throws SAXException {
		if (comment == null)
			return;
		String[] pars = delimiter.split(comment.trim());
		for (int ix = 0; ix < pars.length; ix++) {
			Element elem = new Element(ename);
			elem.append(pars[ix]);
			elem.close();
		}
	}

	private void emitNote(Node node) throws SAXException {
		emit("Note", node.getSubject().getComment(null));
		emitStereotypes(node.getSubject());
	}

	private void emitStereotypes(OntResource subject) throws SAXException {
		ResIterator it = subject.listProperties(UML.hasStereotype);
		while (it.hasNext()) {
			emitStereotype(it.nextResource());
		}
	}

	private void emitStereotype(OntResource stereo) throws SAXException {
		if (!stereo.isURIResource())
			return;
		Element elem = new Element("Stereotype");
		if (stereo.getLabel() != null) {
			elem.set("label", stereo.getLabel());
		} else {
			if (stereo.getLocalName() != null)
				elem.set("label",  stereo.getLocalName());
			else
				elem.set("label", "");
		}
		elem.append(stereo.getURI());
		elem.close();
	}

	private String xstype(OntResource type) {
		if (type.getNameSpace().equals(xsd))
			return type.getLocalName();

		OntResource xtype = type.getEquivalentClass();
		if (xtype != null && xtype.getNameSpace().equals(xsd))
			return xtype.getLocalName();

		System.out.println("Warning: undefined datatype: " + type);
		return "string";
	}

	private void emitPackage(OntResource aPackage) throws SAXException {
		if (aPackage != null) {
			Element elem = new Element("Package");
			elem.set("name", aPackage.getLabel());
			elem.set("basePackage", aPackage.getURI());
			emitComment(aPackage.getComment(null));
			emitStereotypes(aPackage);
			// Determine if there is an owner package...
			if (aPackage.getIsDefinedBy() != null) {
				OntResource theOwnerPackage = aPackage.getIsDefinedBy();
				Element parentPackageElem = new Element("ParentPackage");
				parentPackageElem.set("name", theOwnerPackage.getLabel());
				parentPackageElem.set("basePackage", theOwnerPackage.getURI());
				parentPackageElem.close();
			}
			elem.close();
		}
	}

	private void emit(TypeNode node) throws SAXException {
		Element elem;
		if (node.hasStereotype(UML.concrete))
			elem = new Element("Root");
		else if (node.isEnumerated())
			elem = new Element("EnumeratedType");
		else if (node.hasStereotype(UML.compound))
			elem = new Element("CompoundType");
		else
			elem = new Element("ComplexType");
		elem.set("name", node.getName());
		elem.set("baseClass", node.getBaseClass().getURI());
		elem.set("package", node.getPackageName());
		if (node.getPackage() != null) {
			elem.set("packageURI", node.getPackage().getURI());
		}
		elem.set("minOccurs", ProfileModel.cardString(node.getMinCardinality()));
		elem.set("maxOccurs",
				ProfileModel.cardString(node.getMaxCardinality(), "unbounded"));
		emitComment(node.getBaseClass().getComment(null));
		emitNote(node);

		emitChildren(node);
		elem.close();
	}

	private void emit(MessageNode node) throws SAXException {
		Element elem = new Element("Root");
		elem.set("name", node.getName());
		elem.set("baseClass", node.getBaseClass().getURI());

		emitComment(node.getBaseClass().getComment(null));
		emitNote(node);

		emitChildren(node);
		elem.close();
	}

	private void emit(OntResource type) throws SAXException {
		Element elem = new Element("SimpleType");

		elem.set("dataType", type.getURI());
		elem.set("name", type.getLocalName());
		OntResource defin = type.getResource(RDFS.isDefinedBy);
		if(defin != null){
			elem.set("package", defin.getLabel());
			elem.set("packageURI", defin.getURI());
		}
		elem.set("xstype", xstype(type));

		emitComment(type.getComment(null));

		elem.close();
	}

	private void emit(EnumValueNode node) throws SAXException {
		OntResource value = node.getSubject();
		Element elem = new Element("EnumeratedValue");
		elem.set("name", node.getName());
		elem.set("baseResource", node.getBase().getURI());
		emitComment(value.getComment(null));
		elem.close();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
