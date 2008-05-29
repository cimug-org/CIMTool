package au.com.langdale.cim;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

import au.com.langdale.sax.XMLElement;
import au.com.langdale.sax.XMLMode;

import com.hp.hpl.jena.ontology.InverseFunctionalProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Read the parse events from Xpetal and generate OWL statements.
 */
public class CIM2OWL implements XMLMode {
	private static String CIM = "http://iec.ch/TC57/2001/CIM-schema-cim10#";
	private static String ROSE= "http://iec.ch/TC57/2004/quid#";
	private static String CIMS="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#";
	
	private OntModel model = ModelFactory.createOntologyModel();
	private OntClass CLASS_CATEGORY = model.createClass(CIMS + "ClassCategory");
	private InverseFunctionalProperty HAS_QUID = model.createInverseFunctionalProperty(CIMS + "hasQuid");

	public void loadSchema(String filename) throws IOException, SAXException {
//		XMLReader xpetal = new PetalReader();
//		xpetal.setContentHandler( new XMLInterpreter(this));
//		xpetal.parse(filename);
	}

	public void visit(XMLElement element, String text) {

	}

	public XMLMode visit(XMLElement element) {
		if( element.getName().equals("Class_Category")) {
			return new CategoryMode( element );
		}
		else if( element.matches("Class")) {
			
		} 
		else if( element.matches("Association")) {
			
		} 
		else if( element.matches("class_attributes")) {
			
		} 
		else if( element.matches("ClassAttribute")) {
			
		} 
		else if( element.matches("superclasses")) {
			
		} 
		else if( element.matches("Inheritance_Relationship")) {
			
		} 
		else if( element.matches("roles")) {
			
		} 
		else if( element.matches("Role")) {
			
		} 
		return this;
	}
	
	public void leave() {
		
	}
	
	class Collector implements XMLMode {
		public XMLElement root;
		public String name, quid, quidu, documentation;
		
		public Collector( XMLElement element) {
			root = element;
		}
		
		public void visit(XMLElement element, String text) {
			if( element.getParent() == root) {
				if( element.matches("arg1")) 
					name = text;
				else if( element.matches( "documentation")) 
					documentation = text;
				else if( element.matches( "quid")) 
					quid = text;
				else if( element.matches( "quidu")) 
					quidu = text;
			}
		}
		
		public void annotate( OntResource subject) {
			if( documentation != null)
				subject.addComment( documentation, "en");
			if( quid != null)
				subject.addProperty( HAS_QUID, intern(quid));
			if( name != null )
				subject.addLabel( name, "en");
		}
		
		public XMLMode visit(XMLElement element) {
			return this;
		}
		
		public void leave() {
		}
	}
	class CategoryMode extends Collector {
		public CategoryMode(XMLElement element) {
			super(element);
		}
		
		public OntResource getSubject() {
			return model.createIndividual( CIM + "Category_" + normalise(name), CLASS_CATEGORY);
		}

		@Override
		public XMLMode visit(XMLElement element) {
			if( element.matches("Class")) {
				return new ClassMode(element);
			} 
			else if( element.matches("Association")) {
				return new AssocMode( element );
			}
			return this;
		}
		
		@Override
		public void leave(){
			annotate(getSubject());
		}
	
		class ClassMode extends Collector {
			public ClassMode( XMLElement element ) {
				super( element );
			}
			@Override
			public void leave(){
				annotate(model.createClass( CIM + normalise(name)));
			}
		}
		
		class AssocMode extends Collector {
			RoleMode roleA, roleB;
			
			public AssocMode( XMLElement element ) {
				super(element);
			}

			@Override
			public XMLMode visit(XMLElement element) {
				if( element.matches("Role")) {
					RoleMode role = new RoleMode(element);
					if( roleA == null)
						roleA = role;
					else
						roleB = role;
					return role;
				} 
				return this;
			}
			
			@Override
			public void leave() {
				if( roleA != null && roleB != null) {
					roleA.mate( roleB );
					roleB.mate( roleA );
				}
			}
			
			class RoleMode extends Collector{
				public OntClass range;

				public RoleMode( XMLElement element) {
					super(element);
				}
				
				@Override
				public void leave() {
					range = findClass(quidu);
				}
				
				public void mate( RoleMode other) {
					if( other.range != null) {
						String lname = other.range.getLocalName() + "." + normalise(name);
						ObjectProperty prop = model.createObjectProperty(CIM+lname);
						prop.addDomain(other.range);
						if( range != null ) {
							prop.addRange( range );
						}
						annotate(prop);
					}
				}
			}
		}
	}
	
	public OntClass findClass(String quid) {
		Resource result;
		if( quid != null) {
			ResIterator it = model.listSubjectsWithProperty(HAS_QUID,intern(quid));
			while( it.hasNext()) {
				result = it.nextResource();
				return (OntClass) result.as(OntClass.class);
			}
		}
		return null;
	}
	
	public Resource intern(String quid) {
		return model.createResource(ROSE+quid);
	}
	
	private static int idSeq = 0;
	
	public String genid() {
		idSeq++;
		return "ID"+Integer.toHexString(idSeq);
	}
	
	public String normalise( String text ) {
		if( text == null)
			return genid();
		else
			return text.replace(' ', '_');
	}

	public OntModel getModel() {
		return model;
	}
	
	public static void main(String[] args) {
		CIM2OWL reader = new CIM2OWL();
		try {
			reader.loadSchema( args[0] );
			reader.getModel().write(new BufferedOutputStream( new FileOutputStream( args[1])), args[2]);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	public static Model readModel(String[] args) {
		try {
			String arg = args[0];
			Model model;
			if( arg.endsWith(".mdl"))
				model = readPetal(arg);
			else
				model = readRDF(arg);
			return model;
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + args[0] + e);
		} catch (IOException e) {
			System.err.println("File could not be read: " + args[0] + e );
		} catch (SAXException e) {
			System.err.println("File syntax error: " + args[0] + e);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("No file name specified");
		}
		System.exit(2);
		return null;
	}

	/**
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Model readRDF(String arg) throws FileNotFoundException {
		Model model = ModelFactory.createDefaultModel();
		model.read( new BufferedInputStream( new FileInputStream(arg)), null, null);
		return model;
	}

	public static Model readPetal(String arg) throws IOException, SAXException {
		// Construct Model and Graph
		CIM2OWL reader = new CIM2OWL();
		reader.loadSchema( arg );
		return reader.getModel();
	}
}
