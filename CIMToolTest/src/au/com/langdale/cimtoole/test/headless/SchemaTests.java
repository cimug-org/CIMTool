package au.com.langdale.cimtoole.test.headless;

import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.test.ValidationTest;

public class SchemaTests extends ValidationTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupProfile("cpsm2007_no_topol.owl");
	}

	public final void testCase1() throws CoreException {
		readTestdata("base_case.xml");
		assertNoProblems();
	}

	public final void testCase2() throws CoreException {
		readTestdata("association_case.xml");
		assertNoProblems();
	}

	public final void testCase3() throws CoreException {
		readTestdata("undefined_property.xml");
		assertProblem(SCHEMA_NS + "Bogus.property", "Undefined property");
		
	}

	public final void testCase4() throws CoreException {
		readTestdata("untyped_subject.xml");
		assertProblem(SCHEMA_NS + "IdentifiedObject.name", "Untyped subject of");
		
	}

	public final void testCase5() throws CoreException {
		readTestdata("untyped_object.xml");
		assertProblem(SCHEMA_NS + "ConductingEquipment.BaseVoltage", "Untyped object of");
		
	}

	public final void testCase6() throws CoreException {
		readTestdata("undefined_class.xml");
		assertProblem(SCHEMA_NS + "BogusClass", "Undefined class");
		
	}

	public final void testCase7() throws CoreException {
		readTestdata("abstract_class.xml");
		assertProblem(SCHEMA_NS + "IdentifiedObject", "Abstract class");
		
	}

	public final void testCase8() throws CoreException {
		readTestdata("literal_value_for_object.xml");
		assertProblem(SCHEMA_NS + "ConductingEquipment.BaseVoltage", "Literal value for Object property");
		
	}

	public final void testCase9() throws CoreException {
		readTestdata("range_of_object.xml");
		assertProblem(SCHEMA_NS + "ConductingEquipment.BaseVoltage", "Range of object property");
		
	}

	public final void testCase10() throws CoreException {
		readTestdata("domain_of_property.xml");
		assertProblem(SCHEMA_NS + "SynchronousMachine.operatingMode", "Domain of property");
		
	}

	public final void testCase11() throws CoreException {
		readTestdata("range_of_datatype_1.xml");
		assertProblem(SCHEMA_NS + "BaseVoltage.nominalVoltage", "Range of datatype property");
		
	}

	public final void testCase12() throws CoreException {
		readTestdata("range_of_datatype_2.xml");
		assertProblem(SCHEMA_NS + "BaseVoltage.nominalVoltage", "Range of datatype property");
		
	}

	public final void testCase13() throws CoreException {
		readTestdata("minimum_cardinality_1.xml");
		assertProblem(SCHEMA_NS + "IdentifiedObject.name","Minimum cardinality of Property");
		
	}

	public final void testCase14() throws CoreException {
		readTestdata("minimum_cardinality_2.xml");
		assertProblem(SCHEMA_NS + "ConductingEquipment.BaseVoltage","Minimum cardinality of Property");
		
	}

	public final void testCase15() throws CoreException {
		readTestdata("minimum_cardinality_3.xml");
		assertProblem(SCHEMA_NS + "SynchronousMachine.operatingMode","Minimum cardinality of Property");
		
	}

	public final void testCase16() throws CoreException {
		readTestdata("maximum_cardinality_1.xml");
		assertProblem(SCHEMA_NS + "IdentifiedObject.name","Maximum cardinality of Property");
		
	}

	public final void testCase17() throws CoreException {
		readTestdata("maximum_cardinality_2.xml");
		assertProblem(SCHEMA_NS + "ConductingEquipment.BaseVoltage","Maximum cardinality of Property");
		
	}

	public final void testCase18() throws CoreException {
		readTestdata("maximum_cardinality_3.xml");
		assertProblem(SCHEMA_NS + "SynchronousMachine.operatingMode","Maximum cardinality of Property");
		
	}

}
