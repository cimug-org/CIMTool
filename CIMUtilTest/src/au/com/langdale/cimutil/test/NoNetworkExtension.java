package au.com.langdale.cimutil.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * The extension behind {@link NoNetwork}. It points the JDK's HTTP and HTTPS
 * proxy properties at a local port where nothing listens, and clears the hosts
 * exempted from the proxy, for the lifetime of the test class.
 */
public class NoNetworkExtension implements BeforeAllCallback, AfterAllCallback {

	private static final String[] PROPERTIES = { "http.proxyHost", "http.proxyPort", "https.proxyHost",
			"https.proxyPort", "http.nonProxyHosts" };

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
			.create(NoNetworkExtension.class);

	@Override
	public void beforeAll(ExtensionContext context) {
		Map<String, String> saved = new HashMap<>();
		for (String name : PROPERTIES)
			saved.put(name, System.getProperty(name));
		context.getStore(NAMESPACE).put("saved", saved);

		System.setProperty("http.proxyHost", "127.0.0.1");
		System.setProperty("http.proxyPort", "9");
		System.setProperty("https.proxyHost", "127.0.0.1");
		System.setProperty("https.proxyPort", "9");
		System.setProperty("http.nonProxyHosts", "");
	}

	@Override
	public void afterAll(ExtensionContext context) {
		@SuppressWarnings("unchecked")
		Map<String, String> saved = context.getStore(NAMESPACE).remove("saved", Map.class);
		if (saved == null)
			return;
		saved.forEach((name, value) -> {
			if (value == null)
				System.clearProperty(name);
			else
				System.setProperty(name, value);
		});
	}
}
