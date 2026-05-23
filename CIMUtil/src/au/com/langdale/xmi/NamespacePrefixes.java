/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple support for mapping namespace prefixes to namespaces.
 */
public class NamespacePrefixes {

	private static final Map<String, Map<String, String>> projectPrefix2NSMappings = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, String>> projectNS2PrefixMappings = new ConcurrentHashMap<>();

	public NamespacePrefixes() {
	}

	/**
	 * <pre>
	 * Per the W3C specification, both a namespace prefixes and namespaces are case-sensitive. 
	 * The below initialization method therefore treats the keys in a case sensitive manner.
	 * </pre>
	 * 
	 * @param projectName The unique name of the project.
	 * @param file        The namespaces prefixes file to use for initialization.
	 */
	public static void init(String projectName, File file) {

		Map<String, String> prefix2NSMap = new HashMap<>();
		Map<String, String> ns2PrefixMap = new HashMap<>();

		try {
			Map<String, String> nsPrefixes = Files.lines(file.toPath()).map(String::trim)
					.filter(line -> !line.isEmpty() && !line.startsWith("#")).filter(line -> line.contains("="))
					.map(line -> {
						int idx = line.indexOf('=');
						return new String[] { line.substring(0, idx), line.substring(idx + 1) };
					}).collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (old, new_) -> {
						throw new IllegalArgumentException(String.format(
								"Duplicate namespace prefixes defined for namespaces: '%s' and '%s' in file [%s]",
								old, new_, file.getName()));
					}));

			for (String prefix : nsPrefixes.keySet()) {
				prefix2NSMap.put(prefix, nsPrefixes.get(prefix));
				ns2PrefixMap.put(nsPrefixes.get(prefix), prefix);
			}
			projectPrefix2NSMappings.put(projectName, prefix2NSMap);
			projectNS2PrefixMappings.put(projectName, ns2PrefixMap);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static Map<String, String> getPrefixToNamespaceMap(String projectName) {
		if (projectName != null && projectPrefix2NSMappings.containsKey(projectName))
			return projectPrefix2NSMappings.get(projectName);

		return Collections.emptyMap();
	}

	public static Map<String, String> getNamespaceToPrefixMap(String projectName) {
		if (projectName != null && projectNS2PrefixMappings.containsKey(projectName))
			return projectNS2PrefixMappings.get(projectName);

		return Collections.emptyMap();
	}

	public static String getNamespace(String projectName, String prefix) {
		if (projectName != null && prefix != null && getPrefixToNamespaceMap(projectName).containsKey(prefix))
			return getPrefixToNamespaceMap(projectName).get(prefix);
		return null;
	}

	public static String getPrefix(String projectName, String namespace) {
		if (projectName != null && namespace != null && getNamespaceToPrefixMap(projectName).containsKey(namespace))
			return getNamespaceToPrefixMap(projectName).get(namespace);
		return null;
	}

	public static boolean hasPrefix(String projectName, String namespace) {
		return (projectName != null && namespace != null
				&& getNamespaceToPrefixMap(projectName).containsKey(namespace));
	}

	public static boolean hasPrefixes(String projectName) {
		return (projectName != null && projectPrefix2NSMappings.containsKey(projectName)
				? projectPrefix2NSMappings.get(projectName).size() > 0
				: false);
	}

}
