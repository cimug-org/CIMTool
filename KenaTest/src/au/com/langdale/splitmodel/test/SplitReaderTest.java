package au.com.langdale.splitmodel.test;

import static au.com.langdale.kena.test.TestResources.ANY;
import static au.com.langdale.kena.test.TestResources.CIM;
import static au.com.langdale.kena.test.TestResources.NET;
import static au.com.langdale.kena.test.TestResources.pattern;
import static au.com.langdale.kena.test.TestResources.triple;
import static au.com.langdale.kena.test.TestResources.uri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import au.com.langdale.inference.AsyncResult;
import au.com.langdale.kena.test.TestResources;
import au.com.langdale.splitmodel.SplitReader;

import com.hp.hpl.jena.graph.Triple;

/**
 * Tests for {@link SplitReader}: finding triples by pattern in the sample
 * split model, and stopping when the result handler asks it to.
 */
public class SplitReaderTest {

	protected SplitReader reader;
	protected boolean complete;
	protected int count;

	@BeforeEach
	public void setUp() throws Exception {
		reader = new SplitReader(TestResources.splitModelLocation());
		complete = false;
		count = 0;
	}

	@Test
	public void testModulus() {
		assertEquals(128, reader.getModulus());
	}

	@Test
	public void testFind01() throws Exception {
		reader.find(pattern(NET + "_1744201", CIM + "IdentifiedObject.aliasName", ANY), new AsyncResult() {

			public boolean add(Triple result) {
				count++;
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

	@Test
	public void testFind02() throws Exception {
		reader.find(pattern(NET + "_1744201", CIM + "IdentifiedObject.localName", ANY), new AsyncResult() {

			public boolean add(Triple result) {
				assertEquals(triple(NET + "_1744201", CIM + "IdentifiedObject.localName", "VOLTAGE"), result);
				count++;
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

	@Test
	public void testFind03() throws Exception {
		reader.find(pattern(ANY, CIM + "IdentifiedObject.name", "TROYTRAFO1  SL_CURRN"), new AsyncResult() {

			public boolean add(Triple result) {
				assertEquals(triple(NET + "_2217201", CIM + "IdentifiedObject.name", "TROYTRAFO1  SL_CURRN"), result);
				count++;
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

	@Test
	public void testFind04() throws Exception {
		reader.find(pattern(ANY, CIM + "IdentifiedObject.name", "TROYTRAFO1  SL_CURRN"), new AsyncResult() {

			public boolean add(Triple result) {
				assertEquals(triple(NET + "_2217201", CIM + "IdentifiedObject.name", "TROYTRAFO1  SL_CURRN"), result);
				count++;
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

	@Test
	public void testFind05() throws Exception {
		reader.find(pattern(NET + "_2217201", ANY, ANY), new AsyncResult() {

			public boolean add(Triple result) {
				assertEquals(uri(NET + "_2217201"), result.getSubject());
				count++;
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

	@Test
	public void testFind06() throws Exception {
		reader.find(pattern(ANY, ANY, uri(NET + "_2217201")), new AsyncResult() {

			public boolean add(Triple result) {
				assertEquals(uri(NET + "_2217201"), result.getObject());
				count++;
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
