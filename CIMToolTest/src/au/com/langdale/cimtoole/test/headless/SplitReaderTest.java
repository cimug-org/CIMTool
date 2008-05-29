package au.com.langdale.cimtoole.test.headless;

import au.com.langdale.cimtoole.test.SplitModelTest;
import au.com.langdale.splitmodel.SplitReader;
import au.com.langdale.splitmodel.SplitReader.SplitResult;

import com.hp.hpl.jena.graph.Triple;

public class SplitReaderTest extends SplitModelTest {
	
	protected SplitReader reader;
	protected boolean complete;
	protected int count;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		reader = new SplitReader(SAMPLE_FILES + SPLIT_MODEL);
		complete = false;
		count = 0;
	}
	
	public final void testModulus() {
		assertEquals(128, reader.getModulus());
	}

	public final void testFind01() throws Exception {
		reader.find(pattern(NET + "_1744201", CIM + "IdentifiedObject.name", ANY), new SplitResult() {

			public boolean add(Triple result) {
				count ++;
				return true;
			}

			public void close() {
				complete = true;
			}
			
		});
		
		reader.run();
		
		assertEquals(0, count);
		assertTrue(complete);
	}

	public final void testFind02() throws Exception {
		reader.find(pattern(NET + "_1744201", CIM + "IdentifiedObject.localName", ANY), new SplitResult() {

			public boolean add(Triple result) {
				assertEquals(triple(NET + "_1744201", CIM + "IdentifiedObject.localName", "VOLTAGE"), result);
				count ++;
				return true;
			}

			public void close() {
				complete = true;
			}
			
		});
		
		reader.run();
		
		assertEquals(1, count);
		assertTrue(complete);
	}

	public final void testFind03() throws Exception {
		reader.find(pattern(ANY, CIM + "IdentifiedObject.name", "TROYTRAFO1  SL_CURRN"), new SplitResult() {

			public boolean add(Triple result) {
				assertEquals(triple(NET + "_2217201", CIM + "IdentifiedObject.name", "TROYTRAFO1  SL_CURRN"), result);
				count ++;
				return true;
			}

			public void close() {
				complete = true;
			}
			
		});
		
		reader.run();
		
		assertEquals(1, count);
		assertTrue(complete);
	}

	public final void testFind04() throws Exception {
		reader.find(pattern(ANY, CIM + "IdentifiedObject.name", "TROYTRAFO1  SL_CURRN"), new SplitResult() {

			public boolean add(Triple result) {
				assertEquals(triple(NET + "_2217201", CIM + "IdentifiedObject.name", "TROYTRAFO1  SL_CURRN"), result);
				count ++;
				return false;
			}

			public void close() {
				complete = true;
			}
			
		});
		
		reader.run();
		
		assertEquals(1, count);
		assertFalse(complete);
	}

	public final void testFind05() throws Exception {
		reader.find(pattern(NET + "_2217201", ANY, ANY), new SplitResult() {

			public boolean add(Triple result) {
				assertEquals(uri(NET + "_2217201"), result.getSubject());
				count ++;
				System.out.println("testFind05 " + count + " " + result);
				return true;
			}

			public void close() {
				complete = true;
			}
			
		});
		
		reader.run();
		
		assertEquals(9, count);
		assertTrue(complete);
	}

	public final void testFind06() throws Exception {
		reader.find(pattern(ANY, ANY, uri(NET + "_2217201")), new SplitResult() {

			public boolean add(Triple result) {
				assertEquals(uri(NET + "_2217201"), result.getObject());
				count ++;
				System.out.println("testFind06 " + count + " " + result);
				return true;
			}

			public void close() {
				complete = true;
			}
			
		});
		
		reader.run();
		
		assertTrue(count > 0);
		assertTrue(complete);
	}

}
