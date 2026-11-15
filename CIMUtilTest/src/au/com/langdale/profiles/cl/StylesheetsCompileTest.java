package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import au.com.langdale.cimutil.test.TestProjects;

/**
 * Compiles every stylesheet in {@code CIMUtil/builders}, whether or not it is
 * registered in {@code builders.json}.
 *
 * <p>
 * {@link AllBuildersSmokeTest} runs only registered builders, so a stylesheet
 * kept beside them but not registered can stop compiling unnoticed. This
 * happened to {@code legacy-rdfs-augmented.xsl}, which called a named template
 * it did not define (issue #388). Stylesheets still in development are listed
 * in {@link #IN_DEVELOPMENT} and skipped until they are complete.
 * </p>
 */
class StylesheetsCompileTest {

	/** Stylesheets still in development, which are not yet expected to compile. */
	static final Set<String> IN_DEVELOPMENT = Set.of("iec-adoc-inline-rdfs.xsl");

	static Path buildersFolder() {
		return TestProjects.repositoryRoot().resolve("CIMUtil/builders");
	}

	static Stream<String> stylesheets() throws IOException {
		try (Stream<Path> files = Files.list(buildersFolder())) {
			return files.map(file -> file.getFileName().toString()).filter(name -> name.endsWith(".xsl")).sorted()
					.collect(Collectors.toList()).stream();
		}
	}

	@ParameterizedTest
	@MethodSource("stylesheets")
	void compiles(String stylesheet) {
		if (IN_DEVELOPMENT.contains(stylesheet))
			return;
		List<String> errors = new ArrayList<>();
		net.sf.saxon.TransformerFactoryImpl factory = new net.sf.saxon.TransformerFactoryImpl();
		factory.setErrorListener(new ErrorListener() {
			@Override
			public void warning(TransformerException exception) {
			}

			@Override
			public void error(TransformerException exception) {
				errors.add(exception.getMessageAndLocation());
			}

			@Override
			public void fatalError(TransformerException exception) {
				errors.add(exception.getMessageAndLocation());
			}
		});
		try {
			factory.newTemplates(new StreamSource(buildersFolder().resolve(stylesheet).toFile()));
		} catch (TransformerException e) {
			fail(stylesheet + " does not compile: " + (errors.isEmpty() ? e.getMessageAndLocation() : errors));
		}
	}

	/** Every stylesheet listed as in development still exists, so the list does not go stale. */
	@Test
	void inDevelopmentStylesheetsExist() {
		for (String stylesheet : IN_DEVELOPMENT)
			assertTrue(Files.isRegularFile(buildersFolder().resolve(stylesheet)),
					stylesheet + " is listed as in development but no longer exists");
	}
}
