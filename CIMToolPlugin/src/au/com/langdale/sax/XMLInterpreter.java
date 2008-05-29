package au.com.langdale.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX Content handler that delegates to an XMLMode
 * object or objects, and maintains a stack of XMLElements.
 * 
  */
public class XMLInterpreter extends DefaultHandler {
	private Element top;
	private XMLMode mode;
	public XMLInterpreter(XMLMode initialMode) {
		mode = initialMode;
	}
	
	protected class Element implements XMLElement {
		private String name;
		private String namespace;
		private Attributes atts;
		private Element parent;
		private XMLMode creation_mode;
		
		public Element(String namespace, String name, Attributes atts) {
			this.name = name;
			this.namespace = namespace;
			this.atts = atts;
			parent = top;
			creation_mode = mode;
		}
		
		public XMLElement getParent() {
			return parent;
		}
		
		/* (non-Javadoc)
		 * @see au.com.Langdale.sax.XMLElement#getAttributes()
		 */
		public Attributes getAttributes() {
			return atts;
		}

		/* (non-Javadoc)
		 * @see au.com.Langdale.sax.XMLElement#getName()
		 */
		public String getName() {
			return name;
		}

		/* (non-Javadoc)
		 * @see au.com.Langdale.sax.XMLElement#getNameSpace()
		 */
		public String getNameSpace() {
			return namespace;
		}

		/* (non-Javadoc)
		 * @see au.com.Langdale.sax.XMLElement#getValue(java.lang.String, java.lang.String)
		 */
		public String getValue(String namespace, String name) {
			return atts.getValue(namespace, name);
		}

		public void pop() {
			if( mode != creation_mode) {
				if ( mode != null )
					mode.leave();
				mode = creation_mode;
			}
			top = parent;
		}
		
		public boolean matches(String ns, String local) {
			return namespace.equals( ns ) && name.equals( local );
		}

		public boolean matches(String local) {
			return name.equals( local );
		}

		/* (non-Javadoc)
		 * @see au.com.Langdale.sax.XMLElement#matches(au.com.Langdale.sax.XMLElement, java.lang.String, java.lang.String)
		 */
		public boolean matches(XMLElement parent, String namespace, String name) {
			// TODO Auto-generated method stub
			return false;
		}
		/* (non-Javadoc)
		 * @see au.com.Langdale.sax.XMLElement#matches(au.com.Langdale.sax.XMLElement, java.lang.String)
		 */
		public boolean matches(XMLElement parent, String name) {
			// TODO Auto-generated method stub
			return false;
		}
	}

	@Override
	public void startElement(
		String uri,
		String localName,
		String qName,
		Attributes attributes)
		throws SAXException {
		top = new Element( uri, localName,  attributes );
		if( mode != null)
			mode = mode.visit( top );
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
		throws SAXException {
		if( mode != null ) {
			String text = String.copyValueOf(ch, start, length);
			mode.visit( top, text);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
		throws SAXException {
		top.pop();
	}
}
