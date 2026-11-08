package au.com.langdale.profiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link ProfileSerializer} always uses Saxon, whatever the
 * context class loader of the thread that creates it.
 *
 * <p>
 * The serializer registers Saxon extension functions, so it cannot work with
 * any other XSLT processor. A thread whose context class loader cannot see
 * Saxon, such as one carrying the platform class loader, previously received
 * the JDK's built-in processor and failed with a ClassCastException.
 * </p>
 */
class ProfileSerializerFactoryTest {

	@Test
	void usesSaxonWhenTheContextClassLoaderCannotSeeIt() throws Exception {
		assertTrue(createOn(ClassLoader.getPlatformClassLoader()) instanceof net.sf.saxon.TransformerFactoryImpl,
				"Saxon with the platform class loader as context class loader");
	}

	@Test
	void usesSaxonWhenTheContextClassLoaderCanSeeIt() throws Exception {
		assertTrue(createOn(ProfileSerializer.class.getClassLoader()) instanceof net.sf.saxon.TransformerFactoryImpl,
				"Saxon with CIMUtil's class loader as context class loader");
	}

	/**
	 * Creates a serializer on a new thread with the given context class loader and
	 * returns its factory, rethrowing anything the constructor threw.
	 */
	private static Object createOn(ClassLoader contextClassLoader) throws Exception {
		AtomicReference<Object> factory = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			try {
				factory.set(new ProfileSerializer(new ProfileModel()).factory);
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		thread.setContextClassLoader(contextClassLoader);
		thread.start();
		thread.join();
		if (failure.get() instanceof Exception)
			throw (Exception) failure.get();
		if (failure.get() != null)
			throw new AssertionError(failure.get());
		return factory.get();
	}
}
