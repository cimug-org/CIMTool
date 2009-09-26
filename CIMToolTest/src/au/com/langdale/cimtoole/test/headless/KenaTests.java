package au.com.langdale.cimtoole.test.headless;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.cimtoole.test.ProjectTest;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.workspace.ResourceOutputStream;

public class KenaTests extends ProjectTest {
	public final String NS = "http://example.com/example#";
	
	public final Resource eg(String name) {
		return ResourceFactory.createProperty(NS + name);
	}
	
	public final void testCreateStatement() {
		OntModel model = ModelFactory.createMem();

		model.add(eg("s"), eg("p"), "v");
		assertTrue("statement remembered", model.contains(eg("s"), eg("p"), "v"));
	}
	
	public final void testReadRDFXML() throws CoreException, FileNotFoundException {
		OntModel model = ModelFactory.createMem();
		IO.read(model, new FileInputStream(getSamplesFolder() + SAMPLE_PROFILE), NS, "RDF/XML");
		assertTrue("read some statements", model.size() > 0);
	}
	
	public final void testWriteReadXML() throws IOException, CoreException {
		OntModel model = ModelFactory.createMem();

		model.add(eg("s"), eg("p"), "v");
		ResourceOutputStream contents = new ResourceOutputStream(profile, monitor, true, true);
		IO.write(model, contents, NS, "RDF/XML", Collections.EMPTY_MAP);
		contents.close();
		assertTrue("file was written", profile.exists());
		
		model = ModelFactory.createMem();
		IO.read(model, profile.getContents(), NS, "RDF/XML");
		assertTrue("read some statements", model.size() > 0);
		assertTrue("statement remembered", model.contains(eg("s"), eg("p"), "v"));
		
	}
	
	public final void testOntResource() throws CoreException, FileNotFoundException {
		OntModel model = ModelFactory.createMem();
		IO.read(model, new FileInputStream(getSamplesFolder() + SAMPLE_PROFILE), NS, "RDF/XML");
		assertTrue("read some statements", model.size() > 0);
		
		int count = 0;
		ResIterator all = model.listNamedClasses();
		while( all.hasNext()) {
			OntResource clss = all.nextResource();
			assertTrue("found a class", clss != null);
			assertTrue("class claims to have URI", clss.isURIResource());
			assertTrue("class has type", clss.hasRDFType(OWL.Class));
			//assertTrue("class has a label", clss.getLabel(null) != null);
			//assertTrue("class has a non trivial label", clss.getLabel(null).length() > 0);
			count++;
		}
		assertTrue("found a dozen classes", count >= 12);
	}
	
	public final void testInference1() {
		OntModel model = ModelFactory.createTransInf();
		model.add(eg("c"), RDFS.subClassOf, eg("b1"));
		model.add(eg("c"), RDFS.subClassOf, eg("b2"));
		model.add(eg("b1"), RDFS.subClassOf, eg("b2"));
		model.add(eg("b2"), RDFS.subClassOf, eg("b3"));
		
		OntResource c = model.createResource(eg("c").asNode());
		assertTrue("closure of inheritance", c.hasSuperClass(eg("b3")));
		assertTrue("spurious inference", ! c.hasSubClass(eg("b3")));
		
	}
	
	public final void testInference2() {
		OntModel model = ModelFactory.createTransInf();
		model.add(eg("c"), RDFS.subClassOf, eg("b1"));
		model.add(eg("c"), RDFS.subClassOf, eg("b2"));
		model.add(eg("b1"), RDFS.subClassOf, eg("b2"));
		model.add(eg("b2"), RDFS.subClassOf, eg("b3"));
		
		OntResource c = model.createResource(eg("c").asNode());
		assertTrue("closed inheritance", ! c.hasSuperClass(eg("b3"), true));
		assertTrue("indirect but asserted inheritance", ! c.hasSuperClass(eg("b2"), true));
		assertTrue("direct inheritance", c.hasSuperClass(eg("b1"), true));
		assertTrue("filter self from direct inheritance", ! c.hasSuperClass(c, true));
		assertTrue("include self in closed inheritance", c.hasSuperClass(c));
		
	}
	
	
	public final void testInference3() {
		OntModel model = ModelFactory.createTransInf();
		model.add(eg("c"), RDFS.subClassOf, eg("b1"));
		model.add(eg("c"), RDFS.subClassOf, eg("b2"));
		model.add(eg("b1"), RDFS.subClassOf, eg("b2"));
		model.add(eg("b2"), RDFS.subClassOf, eg("b3"));
		
		boolean b1detect = false;
		
		OntResource c = model.createResource(eg("c").asNode());
		int count = 0;
		ResIterator it = c.listSuperClasses(true);
		while(it.hasNext()) {
			count++;
			OntResource base = it.nextResource();
			if(base.equals(eg("b1")))
				b1detect = true;
		}
		assertTrue("direct inheritance", b1detect);
		assertTrue("unique result", count == 1);
	}

}
