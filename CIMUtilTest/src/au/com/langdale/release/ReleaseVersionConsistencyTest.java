package au.com.langdale.release;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Verifies that the version declarations which must change together at each
 * release agree, as listed in the CIMToolProduct developer documentation's
 * product-aligned projects table and in the cimtool-cli developer
 * documentation.
 *
 * <p>
 * A missed bump matters beyond the release artifacts: the CIMUtil
 * Bundle-Version is what CIMTool compares with each project's stamp to decide
 * whether to regenerate its artifacts after an upgrade (issue #310). If it is
 * not bumped, upgraded workspaces are silently not regenerated.
 * </p>
 *
 * <p>
 * The test reads the files from the repository, which it finds by walking up
 * from the working folder. It is skipped when run outside a checkout. The
 * {@code <features>} list in {@code CIMTool.product} is not checked: the
 * product sets {@code useFeatures="false"}, so that list is ignored at export
 * (see the CIMToolFeature developer documentation).
 * </p>
 */
class ReleaseVersionConsistencyTest {

	private static final Pattern CIMTOOL_NAME = Pattern.compile("CIMTool (\\d+(?:\\.\\d+)*)");

	private static Path repository;
	private static String productVersion;

	@BeforeAll
	static void findRepository() throws Exception {
		Path folder = Paths.get("").toAbsolutePath();
		while (folder != null && !Files.isRegularFile(folder.resolve("CIMToolProduct/CIMTool.product")))
			folder = folder.getParent();
		assumeTrue(folder != null, "not run from within a CIMTool repository checkout");
		repository = folder;
		productVersion = xml("CIMToolProduct/CIMTool.product").getDocumentElement().getAttribute("version");
		assertFalse(productVersion.isEmpty(), "CIMTool.product declares a version");
	}

	@Test
	void productAlignedVersionsMatchTheProduct() throws Exception {
		List<Executable> checks = new ArrayList<>();
		checks.add(matches("CIMToolProduct/CIMTool.product name",
				cimToolName(xml("CIMToolProduct/CIMTool.product").getDocumentElement().getAttribute("name"))));
		for (String name : cimToolNames("CIMToolProduct/plugin.xml"))
			checks.add(matches("CIMToolProduct/plugin.xml \"CIMTool …\" name", name));
		for (String project : new String[] { "CIMToolProduct", "CIMToolPlugin", "CIMToolHelp", "CIMUtil",
				"RCPUtil" })
			checks.add(matches(project + "/META-INF/MANIFEST.MF Bundle-Version", bundleVersion(project)));
		checks.add(matches("CIMToolFeature/feature.xml version",
				xml("CIMToolFeature/feature.xml").getDocumentElement().getAttribute("version")));
		assertAll("versions aligned with CIMTool.product " + productVersion, checks);
	}

	@Test
	void cliVersionsMatchTheirBundles() throws Exception {
		Document pom = xml("cimtool-cli/pom.xml");
		String cimutil = bundleVersion("CIMUtil");
		String kena = bundleVersion("Kena");
		assertAll("cimtool-cli/pom.xml",
				() -> assertEquals(cimutil, childText(pom.getDocumentElement(), "version"),
						"project version, which tracks the CIMUtil Bundle-Version"),
				() -> assertEquals(cimutil, dependencyVersion(pom, "cimutil"),
						"cimutil dependency, which must match the CIMUtil Bundle-Version"),
				() -> assertEquals(kena, dependencyVersion(pom, "kena"),
						"kena dependency, which must match the Kena Bundle-Version"));
	}

	private static Executable matches(String where, String version) {
		return () -> assertEquals(productVersion, version, where);
	}

	private static Document xml(String path) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		try (InputStream in = Files.newInputStream(repository.resolve(path))) {
			return factory.newDocumentBuilder().parse(in);
		}
	}

	private static String bundleVersion(String project) throws IOException {
		try (InputStream in = Files.newInputStream(repository.resolve(project + "/META-INF/MANIFEST.MF"))) {
			String version = new Manifest(in).getMainAttributes().getValue("Bundle-Version");
			return version == null ? null : version.trim();
		}
	}

	private static String cimToolName(String name) {
		Matcher matcher = CIMTOOL_NAME.matcher(name);
		return matcher.matches() ? matcher.group(1) : name;
	}

	private static List<String> cimToolNames(String path) throws IOException {
		List<String> versions = new ArrayList<>();
		Matcher matcher = Pattern.compile("\"" + CIMTOOL_NAME.pattern() + "\"")
				.matcher(Files.readString(repository.resolve(path)));
		while (matcher.find())
			versions.add(matcher.group(1));
		assertFalse(versions.isEmpty(), path + " names \"CIMTool <version>\"");
		return versions;
	}

	private static String dependencyVersion(Document pom, String artifactId) {
		NodeList dependencies = pom.getElementsByTagName("dependency");
		for (int i = 0; i < dependencies.getLength(); i++) {
			Element dependency = (Element) dependencies.item(i);
			if (artifactId.equals(childText(dependency, "artifactId")))
				return childText(dependency, "version");
		}
		return null;
	}

	private static String childText(Element parent, String name) {
		for (org.w3c.dom.Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
			if (node instanceof Element && name.equals(node.getNodeName()))
				return node.getTextContent().trim();
		return null;
	}
}
