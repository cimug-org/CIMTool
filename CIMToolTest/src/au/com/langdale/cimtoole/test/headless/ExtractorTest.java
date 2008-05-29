package au.com.langdale.cimtoole.test.headless;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.shared.PrefixMapping;

import au.com.langdale.cimtoole.test.SplitModelTest;
import au.com.langdale.inference.Extractor;
import au.com.langdale.inference.ProxyRegistry;
import au.com.langdale.inference.RuleParser;
import au.com.langdale.inference.StandardFunctorActions;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.splitmodel.SplitReader;

public class ExtractorTest extends SplitModelTest {

	public static final String TEST = "http://langdale.com.au/2007/test#";
	protected SplitReader reader;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		reader = new SplitReader(SAMPLE_FILES + SPLIT_MODEL);
	}
	
	protected List parse(String rules) throws IOException, ParserException {
		RuleParser parser = new RuleParser(new StringReader(rules),  PrefixMapping.Factory.create(), new ProxyRegistry());
		parser.registerPrefix("cim", CIM);
		parser.registerPrefix("net", NET);
		parser.registerPrefix("tt", TEST);
		return parser.parse();
	}

	protected Graph runExtractor(List rules, Map functors) throws IOException {
		Extractor extractor = new Extractor(reader, new GraphMem(), rules, functors);
		extractor.run();
		Graph result = extractor.getResult();
		return result;
	}
	
	public final void testEmpty() throws Exception {
		Graph result = runExtractor(Collections.EMPTY_LIST, Collections.EMPTY_MAP);
		assertEquals(0, result.size());
	}
	
	public final void testQuery01() throws Exception {
		List rules = parse(
			"(net:_1744201 cim:IdentifiedObject.name ?v) -> (tt:Result tt:value ?v)."
		);
		Graph result = runExtractor(rules , Collections.EMPTY_MAP);
		assertEquals(0, result.size());
	}
	
	public final void testQuery02() throws Exception {
		List rules = parse(
			"(net:_1744201 cim:IdentifiedObject.localName ?v) -> (tt:Result tt:value ?v)."
		);
		Graph result = runExtractor(rules , Collections.EMPTY_MAP);
		assertEquals(1, result.size());
		assertTrue(result.contains(pattern(TEST + "Result", TEST + "value", "VOLTAGE").asTriple()));
	}
	
	public final void testQuery03() throws Exception {
		List rules = parse(
			"(net:_2217201 ?p ?v) -> (tt:Result tt:value ?v)."
		);
		Graph result = runExtractor(rules , Collections.EMPTY_MAP);
		assertEquals(9, result.size());
	}
	
	public final void testQuery04() throws Exception {
		List rules = parse(
			"(net:_2217201 ?p ?v) (?v ?q ?w) -> (tt:Result tt:value ?v)."
		);
		Graph result = runExtractor(rules , Collections.EMPTY_MAP);
		assertEquals(4, result.size());
	}
	
	public final void testQuery05() throws Exception {
		List rules = parse(
			"(net:_2217201 ?p ?v) (?v ?q ?w) -> (tt:Result tt:value ?w)."
		);
		Graph result = runExtractor(rules , Collections.EMPTY_MAP);
		assertEquals(14, result.size());
	}

	public final void testAxiom01()  throws Exception {
		List rules = parse(
			"axiom(tt:Result tt:value 'example') -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(0, result.size());
	}

	public final void testAxiom02()  throws Exception {
		List rules = parse(
			"-> (tt:Result tt:value 'example').\n" + 
			"axiom(tt:Result tt:value 'example') -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules , StandardFunctorActions.create());
		assertEquals(1, result.size());
		assertTrue(result.contains(pattern(TEST + "Result", TEST + "value", "example").asTriple()));
	}

	public final void testSame01()  throws Exception {
		List rules = parse(
			"same(tt:a tt:y) -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(0, result.size());
	}

	public final void testSame02()  throws Exception {
		List rules = parse(
			"same(tt:a tt:a) -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(1, result.size());
	}

	public final void testAny01()  throws Exception {
		List rules = parse(
			"notAny(net:_2217201 ?p ?v) -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(0, result.size());
	}

	public final void testAny02()  throws Exception {
		List rules = parse(
				"any(net:_2217201 ?p ?v) -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(1, result.size());
	}
}
