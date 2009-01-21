package au.com.langdale.cimtoole.test;

import java.io.PrintWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.SplitModelImporter;
import au.com.langdale.kena.OntModel;

import com.hp.hpl.jena.vocabulary.RDFS;

public class ValidationTest extends ProjectTest {
	private static final String SEP = "\t";
	public final String LOG_NS = "http://langdale.com.au/2007/log#";
	protected OntModel diagnostics;

	private static PrintWriter record;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupSchema();
		if(record == null)
			record = new PrintWriter(getSmallCasesFolder() + "TEST_RECORD.txt");
	}
	
	@Override
	protected void tearDown() throws Exception {
		record.println();
		record.flush();
	}
	
	@Override
	protected void setupProfile(String name) throws CoreException {
		record.print("Profile: " + name + SEP);
		super.setupProfile(getSmallCasesFolder() + name);
	}

	protected void assertProblem(String subject, String message) {
		record.print("Expected result: " + message + " " + subject + SEP);
		
		assertTrue("no diagnostics found, expected: " + message, diagnostics.size() > 0);
		Deferred d = find( diagnostics, pattern(subject, LOG_NS + "hasProblem", ANY), pattern(ANY, RDFS.comment.getURI(), message));
		assertFalse("incorrect diagnostics found, expected: " + message, d.getCount() == 0);
		assertFalse("duplicate diagnostic reports found", d.getCount() > 1);
		Deferred e = find( diagnostics, pattern(ANY, LOG_NS + "hasProblem", ANY));
		assertTrue("spurious diagnostic reports found", d.getCount() == e.getCount());
		
	}

	protected void assertNoProblems() {
		record.print("Expected result: no problems." + SEP);
		assertTrue("no validation messages expected", diagnostics.size() == 0);
	}

	protected void readTestdata(String sample) throws CoreException {
		record.print("Model: " + sample + SEP);
		
		readBaseModel(sample);
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		
		IFile diagfile = getModelRelated("diagnostic");
		assertTrue("diagnostics created", diagfile.exists() );
		diagnostics = CIMToolPlugin.getCache().getOntologyWait(diagfile);
	}

	protected void readTestdata(String base, String diff) throws CoreException {
		record.print("Base model: " + base + SEP + "Incremental model: " + diff + SEP);

		readBaseModel(base);
		
		String pathname = getSmallCasesFolder() + diff;
		IWorkspaceRunnable op = new SplitModelImporter(increment, pathname, MODEL_NS, profile, model);
		op.run(monitor);
		assertTrue("incremental model created", increment.exists() );

		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		
		IFile diagfile = getIncrementRelated("diagnostic");
		assertTrue("diagnostics created", diagfile.exists() );
		diagnostics = CIMToolPlugin.getCache().getOntologyWait(diagfile);
	}

	private void readBaseModel(String sample) throws CoreException {
		String pathname = getSmallCasesFolder() + sample;
		IWorkspaceRunnable op = new SplitModelImporter(model, pathname, MODEL_NS, profile, null);
		op.run(monitor);
		assertTrue("model created", model.exists() );
	}
}
