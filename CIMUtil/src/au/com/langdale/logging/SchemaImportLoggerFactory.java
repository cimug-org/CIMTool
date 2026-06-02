package au.com.langdale.logging;

import org.slf4j.LoggerFactory;

/**
 * Factory for obtaining {@link SchemaImportLogger} instances.
 *
 * <p>
 * Returns an SLF4J-backed {@link SchemaImportSLF4JLoggerImpl} whose logger name
 * is derived from the supplied class, giving per-class control over log levels
 * via {@code logback.xml}. For example, to suppress informational model quality
 * notices from {@code AbstractEAProjectParsor} while retaining errors:
 *
 * <pre>
 * &lt;logger name="au.com.langdale.xmi.AbstractEAProjectParsor" level="WARN"/&gt;
 * </pre>
 *
 * <p>
 * This factory is an interim step on the path to removing the
 * {@link SchemaImportLogger} facade entirely. When that migration occurs
 * (Option 2), each calling class will hold its own
 * {@code private static final org.slf4j.Logger logger =
 * org.slf4j.LoggerFactory.getLogger(ClassName.class)} directly, and this
 * factory will be removed.
 */
public final class SchemaImportLoggerFactory {

	private SchemaImportLoggerFactory() {
		// utility class — not instantiable
	}

	/**
	 * Returns a {@link SchemaImportLogger} backed by an SLF4J logger named after
	 * the supplied class.
	 *
	 * @param clazz the class whose name will be used as the SLF4J logger name
	 * @return a {@link SchemaImportLogger} that routes events through the unified
	 *         SLF4J → Logback pipeline
	 */
	public static SchemaImportLogger getLogger(Class<?> clazz) {
		return new SchemaImportSLF4JLoggerImpl(LoggerFactory.getLogger(clazz));
	}

}
