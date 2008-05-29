package au.com.langdale.sax;

import java.util.Enumeration;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;


public abstract class AbstractReader extends XMLReaderBase {

	/** Optimisation provides a single copy of the empty atts */
	private final Attributes empty = new AttributesImpl();

	/** The top of the current element stack */
	private Element top = null;
	
	/** The current namespace bindings */
	NamespaceSupport spaces = new NamespaceSupport();

	/** 
	 * Convenience class to generate an element from a given word or the
	 * current token.
	 */
	protected class Element {
		private Element parent;
		private String type;
		private boolean committed = false;
		private AttributesImpl atts = null;
		private int depth = 0;
		private boolean printing = false;
		private String[] parts = new String[3];
		private String[] attr_parts;

		public Element(String type, String namespace) throws SAXException {
			this( type );
			spaces.declarePrefix( "", namespace);
			output.startPrefixMapping("", namespace );
		}

		public Element(String type) throws SAXException {
			parent = top;
			top = this;
			this.type = type;
			spaces.pushContext();
			if( parent != null) {
				parent.commit();
				depth = parent.depth + 1;
			}
		}

		public void commit() throws SAXException {
			if (!committed) {
				print( "+" + type );
				Attributes passed = atts != null ? atts : empty;
				if( spaces.processName(type, parts, false) == null)
					throw new SAXException( "undeclared namespace prefix: " + type );
				output.startElement(parts[0], parts[1], parts[2], passed);
				committed = true;
			}
		}

		public void close() throws SAXException {
			commit();
			output.endElement(parts[0], parts[1], parts[2]);
			Enumeration ix = spaces.getDeclaredPrefixes();
			while( ix.hasMoreElements()) {
				String prefix = (String) ix.nextElement();
				output.endPrefixMapping(prefix);
			}
			print( "-" + type );
			if( top != this ) {
				throw new SAXException( "internal error: elements out of order" );
			}
			if( depth == 0 ) {
				print( "the end" );
			}
			top = parent;
			spaces.popContext();
		}
		
		private void declare( String name, String value ) throws SAXException {
			int colon = name.indexOf(':');
			if( colon == -1 ) {
				spaces.declarePrefix("", value);
				output.startPrefixMapping("", value );
			}
			else {
				String prefix = name.substring(colon + 1);
				spaces.declarePrefix(prefix,value);
				output.startPrefixMapping(prefix, value );
			}
		}

		public void set(String name, String value) throws SAXException {
			if (committed)
				throw new SAXException("attribute out of order");
				
			if( name.startsWith("xmlns")) {
				declare( name, value );
				return;
			}

			if (atts == null) {
				atts = new AttributesImpl();
				attr_parts = new String[3];
			}

			if( spaces.processName(name, attr_parts,true) == null)
				throw new SAXException( "undeclared namespace prefix in: " + name );

			atts.addAttribute(attr_parts[0], attr_parts[1], attr_parts[2], "CDATA", value);
		}

		public void set(String name, StringBuffer value) throws SAXException {
			set(name, value.toString());	
		}

		public void append(String text) throws SAXException {
			commit();
			char[] ch = new char[text.length()];
			text.getChars(0,text.length(), ch, 0);
			output.characters(ch, 0, text.length()) ;
		}

		public void append(StringBuffer text) throws SAXException {
			commit();
			char[] ch = new char[text.length()];
			text.getChars(0,text.length(), ch, 0);
			output.characters(ch, 0, text.length()) ;
		}
		public void print( String message ) {
			if( printing ) {
				for( int i = 0; i < depth; i++)
					System.out.print("  ");
				System.out.println(message);
			}
		}
	}

}
