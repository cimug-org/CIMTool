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

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.profiles.ProfileModel.CatalogNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode;
import au.com.langdale.profiles.ProfileModel.TypeNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode.MessageNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.EnumValueNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.SuperTypeNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode.SubTypeNode;
import au.com.langdale.sax.AbstractReader;
import au.com.langdale.xmi.UML;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Convert a message model to XML.  The form of the XML is designed
 * to be easily transformed to specific schemas in any schema language.
 *
 * A MessageSerializer is a SAX XMLReader. Calling parse(InputSource) 
 * causes it to emit SAX events representing a message definition.
 * 
 *  However, the write() method is a more convenient way to generate
 *  XML to a file.
 *  
 *  If setStylesheet() is called before write, the output is transformed
 *  into a schema according to the given templates. 
 */
public class ProfileSerializer extends AbstractReader {
	public static final String XSDGEN = "http://langdale.com.au/2005/xsdgen";
	private JenaTreeModelBase model;
	private String baseURI = "";
	private String version = "";
	private ArrayList templates = new ArrayList(); 
	TransformerFactory factory = TransformerFactory.newInstance();
	private final String xsd = XSD.anyURI.getNameSpace();
	private HashSet deferred = new HashSet();

	/**
	 * Construct a serializer for the given MessageModel.
	 */
	public ProfileSerializer(JenaTreeModelBase model) {
		this.model = model;
	}
	
	/**
	 * A URI that is passed as the baseURI attribute of the root element
	 * and is generally used to establish a namespace for the generated
	 * schema.
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
	 * Install a stylesheet to transform the abstract message definition to a schema. 
	 */
	public void setStyleSheet(InputStream s, String base) throws TransformerConfigurationException {
		templates.clear();
		addStyleSheet(s, base);
		
	}

	/**
	 * Install a stylesheet to apply after any previously installed stylesheets. 
	 */
	public void addStyleSheet(InputStream s, String base) throws TransformerConfigurationException {
		templates.add( factory.newTemplates(new StreamSource(s, base)));
	}
	
	/**
	 * Install a stylesheet from the standard set. Use null for no stylesheet.
	 */
	public void setStyleSheet(String name) throws TransformerConfigurationException {
		if( name == null)
			templates.clear();
		else
			setStyleSheet(getClass().getResourceAsStream(name + ".xsl"), XSDGEN);
	}

	/**
	 * Install a stylesheet to apply after any previously installed stylesheets. 
	 */
	public void addStyleSheet(String name) throws TransformerConfigurationException {
		addStyleSheet(getClass().getResourceAsStream(name + ".xsl"), "");
	}
	
	/**
	 * Install a standard SAX ErrorHandler for errors in the stylesheet.
	 * This will be wrapped in the ErrorListener required by the transform
	 * framework.
	 */
	@Override
	public void setErrorHandler(final ErrorHandler errors) {
		ErrorListener listener = new ErrorListener() {

			public void error(TransformerException ex) throws TransformerException {
				try {
					errors.error(convert(ex));
				}
				catch (SAXException e) {
					throw convert(e);
				}
			}

			public void warning(TransformerException ex) throws TransformerException {
				try {
					errors.warning(convert(ex));
				}
				catch (SAXException e) {
					throw convert(e);
				}
			}

			public void fatalError(TransformerException ex) throws TransformerException {
				try {
					errors.fatalError(convert(ex));
				}
				catch (SAXException e) {
					throw convert(e);
				}
			}
			
			private SAXParseException convert(TransformerException ex) {
				Throwable cause = ex.getCause();
				if( cause instanceof SAXParseException)
					return (SAXParseException)cause;
				
				SourceLocator loc = ex.getLocator();
				if(loc != null)
					return new SAXParseException(ex.getMessage(), loc.getPublicId(), loc.getSystemId(), loc.getLineNumber(), loc.getColumnNumber());

				return new SAXParseException(ex.getMessage(), "", "", 0, 0);
			}
			
			private TransformerException convert(SAXException ex) {
				return new TransformerException(ex.getMessage(), ex);
			}
		};
		
		factory.setErrorListener(listener);
	}

	/**
	 * Generate a schema from the message definition 
	 * and write it to the given stream.
	 */
	@Override
	public void write(OutputStream ostream) throws TransformerException {
		Transformer[] tx; 
		if( ! templates.isEmpty()) {
			tx = new Transformer[templates.size()];
			for( int ix = 0; ix < templates.size(); ix++) {
			  Transformer ti = ((Templates)templates.get(ix)).newTransformer();
			  ti.setParameter("baseURI", baseURI);
			  ti.setParameter("version", version);
			  ti.setParameter("envelope", model.getRoot().getName());
			  tx[ix] = ti;
			}
		}
		else {
			tx = new Transformer[] { factory.newTransformer() };
		}
		
		Result result = new StreamResult(ostream);
		Source source = new SAXSource(this, new InputSource());
		for( int ix = 0; ix < tx.length-1; ix++ ) {
			DOMResult inter =  new DOMResult();
			tx[0].transform(source, inter);
			source = new DOMSource(inter.getNode());
		}
		tx[tx.length-1].transform(source, result);
	}

        protected void emit() throws SAXException {
            emit(model.getRoot());
        }
	
	private void emit(Node node) throws SAXException {
		if( node instanceof CatalogNode)
			emit((CatalogNode)node);
		else if( node instanceof EnvelopeNode)
			emit((EnvelopeNode)node);
		else if( node instanceof MessageNode)
			emit((MessageNode)node);
		else if( node instanceof TypeNode)
			emit((TypeNode)node);
		else if( node instanceof SuperTypeNode)
			emit((SuperTypeNode)node);
		else if( node instanceof ElementNode)
			emit((ElementNode)node);
		else if( node instanceof EnumValueNode)
			emit((EnumValueNode)node);
		else if( node instanceof SubTypeNode)
			emit((SubTypeNode)node);
		else if( node != null)
			emitChildren(node);
	}
	
	private void emitChildren(Node node) throws SAXException {
		Iterator it = node.iterator();
		while( it.hasNext())
			emit((Node)it.next());
	}

	private void emit(CatalogNode node) throws SAXException {
		Element elem = new Element("Catalog", MESSAGE.NS);
		elem.set("baseURI", baseURI);
		elem.set("xmlns:m", baseURI);
		elem.set("name", node.getName());
		emitNote(node);
		emitChildren(node);
		
		Iterator it = deferred.iterator();
		while( it.hasNext()) {
			emit((OntResource)it.next());
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
		elem.close();
	}
	
	private void emit(SubTypeNode node) throws SAXException {
		Element elem = select(node);
		elem.set("name", node.getName());
		elem.set("minOccurs", "1");
		elem.set("maxOccurs", "1");
		emitNote(node);
		if(node.getSubject().isAnon())
			emitChildren(node);
		elem.close();
	}
	
	private Element select(SubTypeNode node) throws SAXException {
		Element elem;
		boolean anon = node.getSubject().isAnon(); 
		if( node.isEnumerated()) {
			if(anon) 
				elem = new Element("SimpleEnumerated");
			else 
				elem = new Element("Enumerated");
		}
		else {
			if( anon ) 
				elem = new Element("Complex");
			else {
				if( node.getParent() instanceof ElementNode && ((ElementNode)node.getParent()).isReference())
					elem = new Element("Reference"); 
				else
					elem = new Element("Instance");
			}
		}
		
		elem.set("baseClass", node.getBaseClass().getURI());
		if( ! anon )
			elem.set("type", node.getName());

		return elem;
	}
	
	private void emit(ElementNode node) throws SAXException {
		Element elem;
		
		if( node.isDatatype() ) {
			OntResource range = node.getBaseProperty().getRange();
			if( range == null )
				range = model.getOntModel().createResource(XSD.xstring.asNode());
			if( range.getNameSpace().equals(xsd))  {
				elem = new Element("Simple");
			}
			else {
				elem = new Element("Domain");
				elem.set("type", range.getLocalName());
				deferred.add(range);
			}
			elem.set("dataType", range.getURI());
			elem.set("xstype", xstype(range));
			emit(node, elem);
		}
		else {
			
			int size = node.getChildren().size();
			
			if( size == 0 ) {
				OntResource range = node.getBaseProperty().getRange();
				elem = new Element("Reference"); 
				if( range != null && range.isURIResource())
					elem.set("type", range.getLocalName());
				emit(node, elem);
			}
			else if( size == 1) {
				SubTypeNode child = (SubTypeNode) node.getChildren().get(0);
				elem = select(child);
				emit(node, elem);
				if(child.getSubject().isAnon())
					emitChildren(child);
			}
			else {
				elem = new Element("Choice");
				emit(node, elem);
				emitChildren(node);
			}
			
		}
		
		elem.close();
	}

	private void emit(ElementNode node, Element elem) throws SAXException {
		elem.set("name", node.getName());
		elem.set("baseProperty", node.getBaseProperty().getURI());
		elem.set("minOccurs", ProfileModel.cardString(node.getMinCardinality()));
		elem.set("maxOccurs", ProfileModel.cardString(node.getMaxCardinality(), "unbounded"));
		
		emit(node.getBaseProperty().getComment(null));
		emitNote(node);
	}

	private void emit(String comment) throws SAXException {
		emit("Comment", comment);
	}
	
	private static Pattern delimiter = Pattern.compile(" *([\r\n] *)+");

	private void emit(String ename, String comment) throws SAXException {
		if( comment == null)
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
		if( ! stereo.isURIResource())
			return;
		Element elem = new Element("Stereotype");
		elem.append(stereo.getURI());
		elem.close();
	}

	private String xstype(OntResource type) {
		if(type.getNameSpace().equals(xsd))
			return type.getLocalName();
		
		OntResource xtype = type.getEquivalentClass();
		if( xtype != null && xtype.getNameSpace().equals(xsd))
			return xtype.getLocalName();

		System.out.println("Warning: undefined datatype: " + type);
		return "string";
	}

	private void emit(TypeNode node) throws SAXException {
		Element elem; 
		if( node.hasStereotype(UML.concrete))
			elem = new Element("Root");
		else if(node.isEnumerated()) 
			elem = new Element("EnumeratedType");
		else if( node.hasStereotype(UML.compound))
			elem = new Element("CompoundType");
		else
			elem = new Element("ComplexType");
		elem.set("name", node.getName());
		elem.set("baseClass", node.getBaseClass().getURI());
		elem.set("package", node.getPackageName());
		elem.set("minOccurs", ProfileModel.cardString(node.getMinCardinality()));
		elem.set("maxOccurs", ProfileModel.cardString(node.getMaxCardinality(), "unbounded"));
		emit(node.getBaseClass().getComment(null));
		emitNote(node);

		emitChildren(node);
		elem.close();
	}

	private void emit(MessageNode node) throws SAXException {
		Element elem = new Element("Root");
		elem.set("name", node.getName());
		elem.set("baseClass", node.getBaseClass().getURI());

		emit(node.getBaseClass().getComment(null));
		emitNote(node);

		emitChildren(node);
		elem.close();
	}

	private void emit(OntResource type) throws SAXException {
		Element elem = new Element("SimpleType");
		elem.set("dataType", type.getURI());
		elem.set("name", type.getLocalName());
		elem.set("xstype", xstype(type));

		emit(type.getComment(null));

		elem.close();
	}

	private void emit(EnumValueNode node) throws SAXException {
		OntResource value = node.getSubject();
		
		Element elem = new Element("EnumeratedValue");
		elem.set("name", node.getName());
                elem.set("baseResource", node.getBase().getURI());
		emit(value.getComment(null));
		elem.close();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
