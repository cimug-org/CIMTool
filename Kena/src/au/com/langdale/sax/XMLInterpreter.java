/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX Content handler that delegates to an XMLMode object or objects, and
 * maintains a stack of XMLElements.
 * 
 */
public class XMLInterpreter extends DefaultHandler {
	private Element top;
	private XMLMode initialMode;
	private XMLMode mode;

	public XMLInterpreter(XMLMode mode) {
		this.initialMode = mode;
		this.mode = initialMode;
	}

	@Override
	public void endDocument() throws SAXException {
		this.initialMode.leave();
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

		public Attributes getAttributes() {
			return atts;
		}

		public String getName() {
			return name;
		}

		public String getNameSpace() {
			return namespace;
		}

		public String getValue(String namespace, String name) {
			return atts.getValue(namespace, name);
		}

		public void pop() {
			if (mode != creation_mode) {
				if (mode != null)
					mode.leave();
				mode = creation_mode;
			}
			top = parent;
		}

		public boolean matches(String ns, String local) {
			return namespace.equals(ns) && name.equals(local);
		}

		public boolean matches(String local) {
			return name.equals(local);
		}

		public boolean matches(XMLElement parent, String namespace, String name) {
			return false;
		}

		public boolean matches(XMLElement parent, String name) {
			return false;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		top = new Element(uri, localName, attributes);
		if (mode != null)
			mode = mode.visit(top);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (mode != null) {
			String text = String.copyValueOf(ch, start, length);
			mode.visit(top, text);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		top.pop();
	}
}
