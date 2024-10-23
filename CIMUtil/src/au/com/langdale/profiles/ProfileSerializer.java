/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
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
	
	TransformerFactory factory = TransformerFactory.newInstance();
	private ArrayList templates = new ArrayList();
	private final String xsd = XSD.anyURI.getNameSpace();
	private JenaTreeModelBase model;
	
	/** The following are parameters to be passed into XSLT builders */
	private String baseURI = "";
	private String ontologyURI = "";
	private String version = "";
	private String copyrightMultiLine = "";
	private String copyrightSingleLine = "";
	// the next set of parameters are PlantUML specific
	private String concreteClassesColor = "";
	private String concreteClassesFontColor = "";
	private String abstractClassesColor = "";
	private String abstractClassesFontColor = "";
	private String enumerationsColor = "";
	private String enumerationsFontColor = "";
	private String cimDatatypesColor = "";
	private String cimDatatypesFontColor = "";
	private String compoundsColor = "";
	private String compoundsFontColor = "";
	private String primitivesColor = "";
	private String primitivesFontColor = "";
	private String errorsColor = "";
	private String errorsFontColor = "";
	private boolean enableDarkMode = false;
	private boolean enableShadowing = true;
	private boolean hideEnumerations = false;
	private boolean hideCIMDatatypes = true;
	private boolean hideCompounds = true;
	private boolean hidePrimitives = true;
	private boolean hideCardinalityForRequiredAttributes = false;
	
	private TreeSet deferred = new TreeSet<OntResource>(new Comparator<OntResource>() {
		@Override
		public int compare(OntResource o1, OntResource o2) {
			return o1.getURI().compareTo(o2.getURI());
		}
	});
	private TreeSet primitives = new TreeSet<OntResource>(new Comparator<OntResource>() {
		@Override
		public int compare(OntResource o1, OntResource o2) {
			String str1 = o1.getString(UML.primitiveDataType);
			String str2 = o2.getString(UML.primitiveDataType);
			return str1.compareTo(str2);
		}
	});

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
	 * A URI that is passed as the onotologyURI attribute of the root element of the
	 * ontology from which the profile model was derived from. This is generally
	 * used to establish an xml:base URI that may be necessary for RDF-based schema
	 * that may be generated.
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
	 * A single-line copyright template that includes an embedded ${year} variable.
	 * The copyright template is passed to the serializer as an attribute named
	 * "coyright-single-line" therefore making it available to any XSLT builder for
	 * use as the copyright notice within the output generated by the builder. The
	 * template should contain a ${year} variable within it to be substituted with
	 * the current year during profile serialization. This is intended for use by
	 * any XSLT builder that required a single lined copyright as opposed to a
	 * multi-line copyright.
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
	 * A multi-line copyright template that includes an embedded ${year} variable.
	 * The copyright template is passed to the serializer as an attribute named
	 * "copyright" therefore making it available to any XSLT builder for use as the
	 * copyright notice within the output generated by the builder. The template
	 * should contain a ${year} variable within it to be substituted with the
	 * current year during profile serialization.
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
	public void setStyleSheet(InputStream s, String base) throws TransformerConfigurationException {
		templates.clear();
		addStyleSheet(s, base);
	}

	/**
	 * Install a stylesheet to apply after any previously installed stylesheets.
	 */
	public void addStyleSheet(InputStream s, String base) throws TransformerConfigurationException {
		templates.add(factory.newTemplates(new StreamSource(s, base)));
	}

	/**
	 * Install a stylesheet from the standard set. Use null for no stylesheet.
	 * 
	 * When the name is null it indicates that no stylesheets are to be applied. In
	 * this scenario the output of a call to the ProfileSerializer's write() method
	 * will simply be the output of CIMTool's internal message model as a formatted 
	 * XML document. Therefore, any existing templates are "cleared" and only the  
	 * "indent-xml.xsl" stylesheet will be applied in order to format the XML when 
	 * written out. This stylesheet is for internal purposes only.
	 */
	public void setStyleSheet(String name) throws TransformerConfigurationException {
		if (name == null) {
			templates.clear();
			setStyleSheet(getClass().getResourceAsStream("indent-xml.xsl"), XSDGEN);
		} else {
			setStyleSheet(getClass().getResourceAsStream(name + ".xsl"), XSDGEN);
		}
	}

	/**
	 * Install a stylesheet to apply after any previously installed stylesheets.
	 */
	public void addStyleSheet(String name) throws TransformerConfigurationException {
		if (name != null)
			addStyleSheet(getClass().getResourceAsStream(name + ".xsl"), "");
	}

	/**
	 * Install a standard SAX ErrorHandler for errors in the stylesheet. This will
	 * be wrapped in the ErrorListener required by the transform framework.
	 */
	@Override
	public void setErrorHandler(final ErrorHandler errors) {
		ErrorListener listener = new ErrorListener() {

			public void error(TransformerException ex) throws TransformerException {
				try {
					errors.error(convert(ex));
				} catch (SAXException e) {
					throw convert(e);
				}
			}

			public void warning(TransformerException ex) throws TransformerException {
				try {
					errors.warning(convert(ex));
				} catch (SAXException e) {
					throw convert(e);
				}
			}

			public void fatalError(TransformerException ex) throws TransformerException {
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
					return new SAXParseException(ex.getMessage(), loc.getPublicId(), loc.getSystemId(),
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
				Transformer ti = ((Templates) templates.get(ix)).newTransformer();
				ti.setParameter("baseURI", baseURI);
				ti.setParameter("ontologyURI", ontologyURI);
				ti.setParameter("version", version);
				ti.setParameter("envelope", model.getRoot().getName());
				ti.setParameter("copyright", copyrightMultiLine);
				ti.setParameter("copyright-single-line", copyrightSingleLine);
				
				// PlantUML diagram preferences for all PlantUML diagram builders...
				ti.setParameter("concreteClassesColor", concreteClassesColor);
				ti.setParameter("concreteClassesFontColor", concreteClassesFontColor);
				ti.setParameter("abstractClassesColor", abstractClassesColor);
				ti.setParameter("abstractClassesFontColor", abstractClassesFontColor);
				ti.setParameter("enumerationsColor", enumerationsColor);
				ti.setParameter("enumerationsFontColor", enumerationsFontColor);
				ti.setParameter("cimDatatypesColor", cimDatatypesColor);
				ti.setParameter("cimDatatypesFontColor", cimDatatypesFontColor);
				ti.setParameter("compoundsColor", compoundsColor);
				ti.setParameter("compoundsFontColor", compoundsFontColor);
				ti.setParameter("primitivesColor", primitivesColor);
				ti.setParameter("primitivesFontColor", primitivesFontColor);
				ti.setParameter("errorsColor", errorsColor);
				ti.setParameter("errorsFontColor", errorsFontColor);
				ti.setParameter("enableDarkMode", enableDarkMode);
				ti.setParameter("enableShadowing", enableShadowing);
				ti.setParameter("hideEnumerations", hideEnumerations);
				ti.setParameter("hideCIMDatatypes", hideCIMDatatypes);
				ti.setParameter("hideCompounds", hideCompounds);
				ti.setParameter("hidePrimitives", hidePrimitives);
				ti.setParameter("hideCardinalityForRequiredAttributes", hideCardinalityForRequiredAttributes);
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
		elem.set("hideInDiagrams", (isHidden(node.getSubject()) ? "true" : "false"));
		elem.set("ontologyURI", ontologyURI);
		elem.set("baseURI", baseURI);
		elem.set("xmlns:m", baseURI);
		elem.set("name", node.getName());
		emitNote(node);
		emitPackages(node);
		emitChildren(node);

		Iterator it = deferred.iterator();
		while (it.hasNext()) {
			emitCIMDatatype((OntResource) it.next());
		}
		
		it = primitives.iterator();
		while (it.hasNext()) {
			emitPrimitive((OntResource) it.next());
		}
		
		elem.close();
	}

	private void emit(EnvelopeNode node) throws SAXException {
		Element elem = new Element("Message", MESSAGE.NS);
		elem.set("name", node.getName());
		elem.set("hideInDiagrams", (isHidden(node.getSubject()) ? "true" : "false"));
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
		elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
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
				if (node.getParent() instanceof ElementNode && ((ElementNode) node.getParent()).isReference())
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
				range = model.getOntModel().createResource(XSD.xstring.asNode());
			if (range.hasProperty(UML.hasStereotype, UML.primitive) || range.getNameSpace().equals(xsd)) {
				elem = new Element("Simple");
				/**
				 * For backwards compatibility, rather than setting the "dataType" attribute
				 * to the <<Primitive>> type in the CIM (which is now specified as a property 
				 * on the range which has historically specified the W3C XSD type such as:
				 * 
				 *    http://www.w3.org/2001/XMLSchema#float 
				 *    
				 * Given that we have numerous existing builders that produce profiles (e.g. 
				 * XSD & JSON schemas) that are XSL transforms that would break if this were
				 * changed we have opted to go this route as an interim solution. When first 
				 * class support for <<Primitive>> and <<CIMDatatype>>s is introduced we will
				 * updated at that time to ensure this implementation is updated.
				 */
				elem.set("primitiveDataType", range.getString(UML.primitiveDataType));
				primitives.add(range);
			} else {
				elem = new Element("Domain");
				elem.set("type", range.getLocalName());
				deferred.add(range);
			}
			elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
			elem.set("dataType", range.getURI());
			elem.set("xstype", xstype(range));
			emit(node, elem);
		} else {
			int size = node.getChildren().size();

			if (size == 0) {
				OntResource range = node.getBaseProperty().getRange();
				elem = new Element("Reference");
				elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
				if (range != null && range.isURIResource()) {
					elem.set("baseClass", range.getURI());
					elem.set("type", range.getLocalName());
				}
				emit(node, elem, true);

				OntResource inverse = (node.getBaseProperty() != null ? node.getBaseProperty().getInverse() : null);
				if ((inverse != null) && (node.getProfile().getPropertyInfo(inverse) != null)) {
					elem.close(); // First, close "Reference" ...
					elem = new Element("InverseReference");
					elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
					emitInverse(node, elem);
				}
			} else if (size == 1) {
				SubTypeNode child = (SubTypeNode) node.getChildren().get(0);
				elem = select(child);
				elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
				emit(node, elem, (!child.getSubject().isAnon() && !child.isEnumerated()));
				if (child.getSubject().isAnon()) {
					emitChildren(child);
				} else if (!child.isEnumerated()) {
					// If child is not an enumeration and not anonymous it indicates it is an
					// Instance
					// or Reference element and an InverseInstance or InverseReference must be
					// created...
					OntResource inverse = (node.getBaseProperty() != null ? node.getBaseProperty().getInverse() : null);
					if ((inverse != null) && (node.getProfile().getPropertyInfo(inverse) != null)) {
						elem.close(); // First, close out the "Reference" or "Instance" element...
						if (child.getParent() instanceof ElementNode
								&& ((ElementNode) child.getParent()).isReference()) {
							elem = new Element("InverseReference");
						} else {
							elem = new Element("InverseInstance");
						}
						elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
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
		OntResource inverse = (node.getBaseProperty() != null ? node.getBaseProperty().getInverse() : null);
		if ((inverse != null) && (node.getProfile().getPropertyInfo(inverse) != null)) {
			elem.set("baseClass", inverse.getRange().getURI());
			if (inverse.hasProperty(UML.id))
				elem.set("ea_guid", inverse.getString(UML.id));	
			elem.set("type", inverse.getRange().getLocalName());
			if (inverse.getLabel() != null)
				elem.set("name", inverse.getLabel());
			if (inverse.getURI() != null)
				elem.set("baseProperty", inverse.getURI());
			if (inverse.getDomain() != null)
				elem.set("basePropertyClass", inverse.getDomain().getURI());
			
			PropertyInfo info = node.getProfile().getPropertyInfo(inverse);
			if (!info.getProperty().isDatatypeProperty()) {
				int min = info.getMinCardinality();
				int max = info.getMaxCardinality();
				elem.set("minOccurs", ProfileModel.cardString(min));
				elem.set("maxOccurs", ProfileModel.cardString(max, "unbounded"));
			}

			elem.set("inverseBasePropertyClass", node.getBaseClass().getURI());
			elem.set("inverseBaseProperty", node.getBaseProperty().getURI());

			emitComment(info.getProperty().getComment(null));
			emitStereotypes(info.getProperty());
		}
	}

	private void emit(ElementNode node, Element elem) throws SAXException {
		emit(node, elem, false);
	}
	
	private void emitValueUnitMultiplier(OntResource cimDatatype) throws SAXException {
		if (cimDatatype == null)
			return;
		boolean hasMultiplier = cimDatatype.hasProperty(UML.hasMultiplier);
		boolean hasUnits = cimDatatype.hasProperty(UML.hasUnits);
		String valueDataType = cimDatatype.getString(UML.valueDataType, null);
		String valuePrimitiveDataType = cimDatatype.getString(UML.valuePrimitiveDataType, null);
		String unitDataType = cimDatatype.getString(UML.unitDataType, null);
		String multiplierDataType = cimDatatype.getString(UML.multiplierDataType, null);
		
		/** Generate the CIMDatatype "value" attribute. */
		Element valueElement = new Element("Value");
		valueElement.set("baseClass", valuePrimitiveDataType);
		valueElement.set("type", valuePrimitiveDataType.substring(valuePrimitiveDataType.lastIndexOf("#") + 1));
		valueElement.set("xstype", valueDataType.substring(valueDataType.lastIndexOf("#") + 1));
		valueElement.set("name", "value");
		valueElement.set("constant", "");
		valueElement.set("ea_guid", cimDatatype.getString(UML.valueEAGUID, null));
		valueElement.set("hideInDiagrams", "false");
		valueElement.set("baseProperty", cimDatatype.getURI() + ".value");
		valueElement.set("basePropertyClass", cimDatatype.getURI());
		valueElement.set("minOccurs", ProfileModel.cardString(0));
		valueElement.set("maxOccurs", ProfileModel.cardString(1, "1"));
		//
		Element valueAttrStereotype = new Element("Stereotype");
		valueAttrStereotype.set("label", UML.attribute.getLocalName());
		valueAttrStereotype.append(UML.attribute.getURI());
		valueAttrStereotype.close();
		//
		valueElement.close();
		
		/** Generate the CIMDatatype "unit" attribute. */
		Element unitElement = new Element("Unit");
		unitElement.set("baseClass", unitDataType);
		unitElement.set("type", unitDataType.substring(unitDataType.lastIndexOf("#") + 1));
		unitElement.set("name", "unit");
		unitElement.set("constant", (hasUnits ? cimDatatype.getString(UML.hasUnits, null) : "none"));
		unitElement.set("ea_guid", cimDatatype.getString(UML.unitEAGUID, null));
		unitElement.set("hideInDiagrams", "false");
		unitElement.set("baseProperty", cimDatatype.getURI() + ".unit");
		unitElement.set("basePropertyClass", cimDatatype.getURI());
		unitElement.set("minOccurs", ProfileModel.cardString(0));
		unitElement.set("maxOccurs", ProfileModel.cardString(1, "1"));
		//
		Element unitAttrStereotype = new Element("Stereotype");
		unitAttrStereotype.set("label", UML.attribute.getLocalName());
		unitAttrStereotype.append(UML.attribute.getURI());
		unitAttrStereotype.close();
		//
		unitElement.close();	
		
		/** Generate the CIMDatatype "multiplier" attribute. */
		Element multiplierElement = new Element("Multiplier");
		multiplierElement.set("baseClass", multiplierDataType);
		multiplierElement.set("type", multiplierDataType.substring(multiplierDataType.lastIndexOf("#") + 1));
		multiplierElement.set("name", "multiplier");
		multiplierElement.set("constant", (hasMultiplier ? cimDatatype.getString(UML.hasMultiplier, null) : "none"));
		multiplierElement.set("ea_guid", cimDatatype.getString(UML.multiplierEAGUID, null));
		multiplierElement.set("hideInDiagrams", "false");
		multiplierElement.set("baseProperty", cimDatatype.getURI() + ".multiplier");
		multiplierElement.set("basePropertyClass", cimDatatype.getURI());
		multiplierElement.set("minOccurs", ProfileModel.cardString(0));
		multiplierElement.set("maxOccurs", ProfileModel.cardString(1, "1"));
		//
		Element multiplierAttrStereotype = new Element("Stereotype");
		multiplierAttrStereotype.set("label", UML.attribute.getLocalName());
		multiplierAttrStereotype.append(UML.attribute.getURI());
		multiplierAttrStereotype.close();
		//
		multiplierElement.close();	
	}

	private void emit(ElementNode node, Element elem, boolean includeInverseBaseClass) throws SAXException {
		elem.set("name", node.getName());
		if (node.getBase().hasProperty(UML.id))
			elem.set("ea_guid", node.getBase().getString(UML.id));	
		elem.set("baseProperty", node.getBaseProperty().getURI());
		if (node.getBaseProperty().getDomain() != null) {
			elem.set("basePropertyClass", node.getBaseProperty().getDomain().getURI());
		}
		elem.set("minOccurs", ProfileModel.cardString(node.getMinCardinality()));
		elem.set("maxOccurs", ProfileModel.cardString(node.getMaxCardinality(), "unbounded"));

		if (includeInverseBaseClass) {
			OntResource inverseBaseClass = (node.getParent() != null ? node.getParent().getBase() : null);
			if ((inverseBaseClass != null) && (inverseBaseClass.getURI() != null)) {
				elem.set("inverseBasePropertyClass", inverseBaseClass.getURI());
			}
		}

		OntResource inverse = (node.getBaseProperty() != null ? node.getBaseProperty().getInverse() : null);
		if ((inverse != null) && (inverse.getURI() != null)) {
			elem.set("inverseBaseProperty", inverse.getURI());
		}

		emitComment(node.getBaseProperty().getComment(null));
		emitNote(node);
	}

	private void emitChoice(ElementNode node, Element elem) throws SAXException {
		elem.set("name", node.getName());
		elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
		elem.set("baseProperty", node.getBaseProperty().getURI());
		elem.set("minOccurs", ProfileModel.cardString(node.getMinCardinality()));
		elem.set("maxOccurs", ProfileModel.cardString(node.getMaxCardinality(), "unbounded"));
		/**
		 * We obtain the topmost class in the hierarchy via the call to
		 * node.getBaseClass(). We then loop through all of the child nodes of the
		 * choice and locate the one that matches the base class associated with the
		 * Choice property. This is the one we must use when generating the
		 * 'inheritanceBaseClass' and 'inheritanceBaseType' attributes for the Choice.
		 * These two attributes indicate the URI of the topmost base class in the
		 * hierarchy and the name of that class as specified in the context of the
		 * profile defined.
		 */
		OntResource baseClass = node.getBaseClass();
		Iterator it = node.iterator();
		while (it.hasNext()) {
			Node childNode = (Node) it.next();
			if (childNode.getBase().equals(baseClass)) {
				elem.set("inheritanceBaseClass", childNode.getSubject().getURI());
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

	private boolean isHidden(OntResource subject) throws SAXException {
		ResIterator it = subject.listProperties(UML.hasStereotype);
		boolean isHidden = false;
		while (it.hasNext()) {
			OntResource stereo = it.nextResource();
			if (!stereo.isURIResource())
				continue;
			if ((UML.hideOnDiagrams.getURI().equals(stereo.getURI())) || (stereo.getLocalName() != null && stereo.getLocalName().equals(UML.hideOnDiagrams.getLocalName())))
				return true; 
		}
		return isHidden;
	}
	
	private void emitStereotypes(OntResource subject) throws SAXException {
		ResIterator it = subject.listProperties(UML.hasStereotype);
		while (it.hasNext()) {
			OntResource stereotype = it.nextResource();
			emitStereotype(stereotype);
		}
	}

	private void emitStereotype(OntResource stereo) throws SAXException {
		if (!stereo.isURIResource())
			return;
		if ((UML.hideOnDiagrams.getURI().equals(stereo.getURI())) || (stereo.getLocalName() != null && stereo.getLocalName().equals(UML.hideOnDiagrams.getLocalName())))
			return;
		Element elem = new Element("Stereotype");
		if (stereo.getLabel() != null) {
			elem.set("label", stereo.getLabel());
		} else {
			if (stereo.getLocalName() != null)
				elem.set("label", stereo.getLocalName());
			else
				elem.set("label", "");
		}
		elem.append(stereo.getURI());
		elem.close();
	}

	private String xstype(OntResource type) {
		if (type.getNameSpace().equals(xsd))
			return type.getLocalName();

		if (type.hasProperty(UML.hasStereotype, UML.primitive)) {
			OntResource xsdType = type.getEquivalentClass();
			if (xsdType != null && xsdType.getNameSpace().equals(xsd))
				return xsdType.getLocalName();
		} else if (type.hasProperty(UML.hasStereotype, UML.cimdatatype)) {
			OntResource cimPrimitive = type.getEquivalentClass();
			if (cimPrimitive != null) {
				OntResource xsdType = cimPrimitive.getEquivalentClass();
				if (xsdType != null && xsdType.getNameSpace().equals(xsd)) {
					return xsdType.getLocalName();
				}
			}
		}

		System.out.println("Warning: undefined datatype: " + type);
		return "string";
	}

	private void emitPackage(OntResource aPackage) throws SAXException {
		if (aPackage != null) {
			Element elem = new Element("Package");
			elem.set("name", aPackage.getLabel());
			if (aPackage.hasProperty(UML.id))
				elem.set("ea_guid", aPackage.getString(UML.id));		
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
		elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
		if (node.getBaseClass().hasProperty(UML.id))
			elem.set("ea_guid", node.getBaseClass().getString(UML.id));		
		elem.set("baseClass", node.getBaseClass().getURI());
		elem.set("package", node.getPackageName());
		if (node.getPackage() != null) {
			elem.set("packageURI", node.getPackage().getURI());
		}
		elem.set("minOccurs", ProfileModel.cardString(node.getMinCardinality()));
		elem.set("maxOccurs", ProfileModel.cardString(node.getMaxCardinality(), "unbounded"));
		emitComment(node.getBaseClass().getComment(null));
		emitNote(node);

		emitChildren(node);
		elem.close();
	}

	private void emit(MessageNode node) throws SAXException {
		Element elem = new Element("Root");
		elem.set("name", node.getName());
		elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
		if (node.getBaseClass().hasProperty(UML.id))
			elem.set("ea_guid", node.getBaseClass().getString(UML.id));		
		elem.set("baseClass", node.getBaseClass().getURI());

		emitComment(node.getBaseClass().getComment(null));
		emitNote(node);

		emitChildren(node);
		elem.close();
	}

	private void emitCIMDatatype(OntResource type) throws SAXException {
		Element elem = new Element("SimpleType");

		elem.set("dataType", type.getURI());
		elem.set("name", type.getLocalName());

		if (type.hasProperty(UML.id))
			elem.set("ea_guid", type.getString(UML.id));	
		
		OntResource defin = type.getResource(RDFS.isDefinedBy);
		if (defin != null) {
			elem.set("package", defin.getLabel());
			elem.set("packageURI", defin.getURI());
		}
		elem.set("xstype", xstype(type));
		emitComment(type.getComment(null));
		emitStereotypes(type);
		emitValueUnitMultiplier(type);
		elem.close();
	}
	
	/**
	 * Some history on the below change introduced in 2.3.0. The context is that for the 
	 * CIMTool implementation of CIM <<Primitive>> UML classes (e.g. Decimal, Boolean, Date, 
	 * Float, Date, others...) the perspective was that the intent of those classes is that
	 * they were representative of XSD schema type primitives and therefor the original
	 * CIMTool design was to essentially remove them from the CIMTool *.OWL profile 
	 * representation generated by CIMTool. For a more detailed understanding of how this
	 * is done refer to the CIMInterpreter.applyPrimitiveStereotype() method. The result of
	 * the model processing done there is ultimately used here in the ProfileSerializer class.  
	 * 
	 * The below method was introduced in 2.3.0 as a result of the evolution in thinking in
	 * how we are now representing the profiles. Today, in formats such as RDFS2020 and others
	 * we include CIM <<Primitive>> as actual RDF classes (such as in the CGMES RDFS profiles). 
	 * However, to preserve backwards compatibility we have chosen (for the moment) not to update 
	 * the core representation in CIMTool but rather to limit changes to only the generated
	 * XML internal profile representation produced by the this ProfileSerializer class. Thus,  
	 * in 2.3.0 we have added the additional method below to generated entries in the XML
	 * internal format to include "Primitive" entries that can be used by XSLT transform 
	 * builders to generate output such as RDFS2020 and equivalent.  To change the core internal
	 * format at this point would break CIMTool's current instance data validation features. 
	 */
	private void emitPrimitive(OntResource type) throws SAXException {
		Element elem = new Element("PrimitiveType");

		String dataType = type.getString(UML.primitiveDataType);
		elem.set("dataType", dataType);	
		elem.set("name", dataType.substring(dataType.lastIndexOf("#") + 1));
		elem.set("hideInDiagrams", (isHidden(type) ? "true" : "false"));
		
		if (type.hasProperty(UML.id))
			elem.set("ea_guid", type.getString(UML.id));	
		
		OntResource definedBy = type.getResource(RDFS.isDefinedBy);
		if (definedBy != null) {
			elem.set("package", definedBy.getLabel());
			elem.set("packageURI", definedBy.getURI());
		}
		elem.set("xstype", xstype(type));

		emitComment(type.getComment(null));
		emitStereotypes(type);
		elem.close();
	}

	private void emit(EnumValueNode node) throws SAXException {
		OntResource value = node.getSubject();
		Element elem = new Element("EnumeratedValue");
		elem.set("hideInDiagrams", (node.getSubject() != null && isHidden(node.getSubject()) ? "true" : "false"));
		elem.set("name", node.getName());
		if (node.getBase().hasProperty(UML.id))
			elem.set("ea_guid", node.getBase().getString(UML.id));	
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
	
	public String getConcreteClassesColor() {
		return concreteClassesColor;
	}

	public void setConcreteClassesColor(String concreteClassesColor) {
		this.concreteClassesColor = concreteClassesColor;
	}

	public String getAbstractClassesColor() {
		return abstractClassesColor;
	}

	public void setAbstractClassesColor(String abstractClassesColor) {
		this.abstractClassesColor = abstractClassesColor;
	}

	public String getEnumerationsColor() {
		return enumerationsColor;
	}

	public void setEnumerationsColor(String enumerationsColor) {
		this.enumerationsColor = enumerationsColor;
	}
	
	public boolean isHideCardinalityForRequiredAttributes() {
		return hideCardinalityForRequiredAttributes;
	}

	public void setHideCardinalityForRequiredAttributes(boolean hideCardinalityForRequiredAttributes) {
		this.hideCardinalityForRequiredAttributes = hideCardinalityForRequiredAttributes;
	}
	
	public boolean isEnableDarkMode() {
		return enableDarkMode;
	}

	public void setEnableDarkMode(boolean enableDarkMode) {
		this.enableDarkMode = enableDarkMode;
	}
	
	public boolean isEnableShadowing() {
		return enableShadowing;
	}

	public void setEnableShadowing(boolean enableShadowing) {
		this.enableShadowing = enableShadowing;
	}
	
	public String getAbstractClassesFontColor() {
		return abstractClassesFontColor;
	}

	public void setAbstractClassesFontColor(String abstractClassesFontColor) {
		this.abstractClassesFontColor = abstractClassesFontColor;
	}

	public String getEnumerationsFontColor() {
		return enumerationsFontColor;
	}

	public void setEnumerationsFontColor(String enumerationsFontColor) {
		this.enumerationsFontColor = enumerationsFontColor;
	}

	public String getConcreteClassesFontColor() {
		return concreteClassesFontColor;
	}
	
	public void setConcreteClassesFontColor(String concreteClassesFontColor) {
		this.concreteClassesFontColor = concreteClassesFontColor;
	}

	public String getCIMDatatypesColor() {
		return cimDatatypesColor;
	}
	
	public void setCIMDatatypesColor(String cimDatatypesColor) {
		this.cimDatatypesColor = cimDatatypesColor;
	}
	
	public String getCIMDatatypesFontColor() {
		return cimDatatypesFontColor;
	}
	
	public void setCIMDatatypesFontColor(String cimDatatypesFontColor) {
		this.cimDatatypesFontColor = cimDatatypesFontColor;
	}
	
	public void setCompoundsColor(String compoundsColor) {
		this.compoundsColor = compoundsColor;
	}

	public void setCompoundsFontColor(String compoundsFontColor) {
		this.compoundsFontColor = compoundsFontColor;
	}
	
	public void setPrimitivesColor(String primitivesColor) {
		this.primitivesColor = primitivesColor;
	}
	
	public String getPrimitivesColor() {
		return this.primitivesColor;
	}

	public void setPrimitivesFontColor(String primitivesFontColor) {
		this.primitivesFontColor = primitivesFontColor;
	}
	
	public String getPrimitivesFontColor() {
		return this.primitivesFontColor;
	}
	
	public void setErrorsColor(String errorsColor) {
		this.errorsColor = errorsColor;
	}
	
	public String getErrorsColor() {
		return this.errorsColor;
	}

	public void setErrorsFontColor(String errorsFontColor) {
		this.errorsFontColor = errorsFontColor;
	}
	
	public String getErrorsFontColor() {
		return this.errorsFontColor;
	}
	

	public boolean isHideEnumerations() {
		return hideEnumerations;
	}

	public void setHideEnumerations(boolean hideEnumerations) {
		this.hideEnumerations = hideEnumerations;
	}

	public boolean isHideCIMDatatypes() {
		return hideCIMDatatypes;
	}

	public void setHideCIMDatatypes(boolean hideCIMDatatypes) {
		this.hideCIMDatatypes = hideCIMDatatypes;
	}
	
	public boolean isHidePrimitives() {
		return hidePrimitives;
	}

	public void setHidePrimitives(boolean hidePrimitives) {
		this.hidePrimitives = hidePrimitives;
	}
	
	public boolean isHideCompounds() {
		return hideCompounds;
	}

	public void setHideCompounds(boolean hideCompounds) {
		this.hideCompounds = hideCompounds;
	}
	
}
