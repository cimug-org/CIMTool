package au.com.langdale.cimtoole.test.headless;

import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.test.ValidationTest;

public class TopologyTests extends ValidationTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupProfile("cpsm2007.owl");
	}

	public final void testCase1() throws CoreException {
		readTestdata("topology_case.xml");
		assertNoProblems();
	}

	public final void testCase2() throws CoreException {
		readTestdata("extra_terminals_1.xml");
		assertProblem(SCHEMA_NS + "Load", "Extra terminals for");
	}

	public final void testCase3() throws CoreException {
		readTestdata("extra_terminals_2.xml");
		assertProblem(SCHEMA_NS + "ACLineSegment", "Extra terminals for");
	}

	public final void testCase4() throws CoreException {
		readTestdata("missing_terminal_1.xml");
		assertProblem(SCHEMA_NS + "Load", "Missing terminal for");
	}

	public final void testCase5() throws CoreException {
		readTestdata("missing_terminal_2.xml");
		assertProblem(SCHEMA_NS + "ACLineSegment", "Missing terminal for");
	}

	public final void testCase6() throws CoreException {
		readTestdata("missing_terminal_3.xml");
		assertProblem(SCHEMA_NS + "ACLineSegment", "Missing terminal for");
	}

	public final void testCase7() throws CoreException {
		readTestdata("isolated_node_1.xml");
		assertProblem(SCHEMA_NS + "ConnectivityNode", "Isolated node");
	}

	public final void testCase8() throws CoreException {
		readTestdata("isolated_node_2.xml");
		assertProblem(SCHEMA_NS + "ConnectivityNode", "Isolated node");
	}

	public final void testCase9() throws CoreException {
		readTestdata("base_voltages.xml");
		assertProblem(SCHEMA_NS + "ConnectivityNode", "Base voltages at");
	}

	public final void testCase10() throws CoreException {
		readTestdata("loop_created.xml");
		assertProblem(SCHEMA_NS + "ACLineSegment", "Loop created by");
	}
}
