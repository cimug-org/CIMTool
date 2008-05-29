package au.com.langdale.cimtoole.test.headless;

import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.test.ValidationTest;

public class IncrementalTests extends ValidationTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupProfile("cpsm2007.owl");
	}
	
	public final void testCase1() throws CoreException {
		readTestdata("topology_case.xml", "add_case.xml");
		assertNoProblems();
	}
	
	public final void testCase2() throws CoreException {
		readTestdata("topology_case.xml", "remove_case.xml");
		assertNoProblems();
	}
	
	public final void testCase3() throws CoreException {
		readTestdata("topology_case.xml", "duplicate_property.xml");
		assertProblem(SCHEMA_NS + "Equipment.MemberOf_EquipmentContainer", "Duplicate property");
	}
	
	public final void testCase4() throws CoreException {
		readTestdata("topology_case.xml", "missing_property.xml");
		assertProblem(SCHEMA_NS + "Equipment.MemberOf_EquipmentContainer", "Missing property");
	}
	
	public final void testCase5() throws CoreException {
		readTestdata("topology_case.xml", "failed_precondition.xml");
		assertProblem(SCHEMA_NS + "Equipment.MemberOf_EquipmentContainer", "Failed precondition");
	}
	
	public final void testCase6() throws CoreException {
		readTestdata("topology_case.xml", "add_undefined_property.xml");
		assertProblem(SCHEMA_NS + "Bogus.Property", "Undefined property");
	}
	
	public final void testCase7() throws CoreException {
		readTestdata("topology_case.xml", "add_undefined_class.xml");
		assertProblem(SCHEMA_NS + "BogusClass", "Undefined class");
	}
}
