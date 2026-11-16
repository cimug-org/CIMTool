package au.com.langdale.inference.test;

import static au.com.langdale.kena.test.TestResources.CIM;
import static au.com.langdale.kena.test.TestResources.NET;
import static au.com.langdale.kena.test.TestResources.pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.shared.PrefixMapping;

import au.com.langdale.inference.AsyncModel;
import au.com.langdale.inference.Extractor;
import au.com.langdale.inference.ProxyRegistry;
import au.com.langdale.inference.RuleParser;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.inference.StandardFunctorActions;
import au.com.langdale.kena.test.TestResources;
import au.com.langdale.splitmodel.SplitReader;

/**
 * Tests for {@link Extractor}: rules parsed by {@link RuleParser} and run
 * against the sample split model, with and without the standard functors.
 */
public class ExtractorTest {

	public static final String TEST = "http://langdale.com.au/2007/test#";
	protected AsyncModel reader;

	@BeforeEach
	public void setUp() throws Exception {
		reader = new SplitReader(TestResources.splitModelLocation());
	}

	protected List parse(String rules) throws IOException, ParserException {
		RuleParser parser = new RuleParser(new StringReader(rules), PrefixMapping.Factory.create(), new ProxyRegistry());
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

	@Test
	public void testEmpty() throws Exception {
		Graph result = runExtractor(Collections.EMPTY_LIST, Collections.EMPTY_MAP);
		assertEquals(0, result.size());
	}

	@Test
	public void testQuery01() throws Exception {
		List rules = parse(
			"(net:_1744201 cim:IdentifiedObject.aliasName ?v) -> (tt:Result tt:value ?v)."
		);
		Graph result = runExtractor(rules, Collections.EMPTY_MAP);
		assertEquals(0, result.size());
	}

	@Test
	public void testQuery02() throws Exception {
		List rules = parse(
			"(net:_1744201 cim:IdentifiedObject.localName ?v) -> (tt:Result tt:value ?v)."
		);
		Graph result = runExtractor(rules, Collections.EMPTY_MAP);
		assertEquals(1, result.size());
		assertTrue(result.contains(pattern(TEST + "Result", TEST + "value", "VOLTAGE").asTriple()));
	}

	@Test
	public void testQuery03() throws Exception {
		List rules = parse(
			"(net:_2217201 ?p ?v) -> (tt:Result tt:value ?v)."
		);
		Graph result = runExtractor(rules, Collections.EMPTY_MAP);
		assertEquals(9, result.size());
	}

	@Test
	public void testQuery04() throws Exception {
		List rules = parse(
			"(net:_2217201 ?p ?v) (?v ?q ?w) -> (tt:Result tt:value ?v)."
		);
		Graph result = runExtractor(rules, Collections.EMPTY_MAP);
		assertEquals(4, result.size());
	}

	@Test
	public void testQuery05() throws Exception {
		List rules = parse(
			"(net:_2217201 ?p ?v) (?v ?q ?w) -> (tt:Result tt:value ?w)."
		);
		Graph result = runExtractor(rules, Collections.EMPTY_MAP);
		assertEquals(14, result.size());
	}

	@Test
	public void testAxiom01() throws Exception {
		List rules = parse(
			"axiom(tt:Result tt:value 'example') -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(0, result.size());
	}

	@Test
	public void testAxiom02() throws Exception {
		List rules = parse(
			"-> (tt:Result tt:value 'example').\n" +
			"axiom(tt:Result tt:value 'example') -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(1, result.size());
		assertTrue(result.contains(pattern(TEST + "Result", TEST + "value", "example").asTriple()));
	}

	@Test
	public void testSame01() throws Exception {
		List rules = parse(
			"same(tt:a tt:y) -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(0, result.size());
	}

	@Test
	public void testSame02() throws Exception {
		List rules = parse(
			"same(tt:a tt:a) -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(1, result.size());
	}

	@Test
	public void testAny01() throws Exception {
		List rules = parse(
			"notAny(net:_2217201 ?p ?v) -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(0, result.size());
	}

	@Test
	public void testAny02() throws Exception {
		List rules = parse(
			"any(net:_2217201 ?p ?v) -> (tt:Result tt:value 'example')."
		);
		Graph result = runExtractor(rules, StandardFunctorActions.create());
		assertEquals(1, result.size());
	}
}
