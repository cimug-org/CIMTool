package au.com.langdale.xmi;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

/**
 * Simple support for namespace specifications via stereotypes on packages,
 * classes, associations and attributes. This is equivalent to what is supported
 * in CimConteXtor.
 */
public class StereotypedNamespaces {

	// Map for Stereotype -> Namespaces
	private Map<String, String> stereo2NSMap = new HashMap<>();

	// Map for Stereotype -> OntResource (i.e. stereotype)
	private Map<String, OntResource> stereo2ResourceMap = new HashMap<>();

	// Map for Namespace -> OntResource
	private Map<String, OntResource> ns2ResourceMap = new HashMap<>();

	private static boolean hasNamespaces = false;

	public StereotypedNamespaces() {
	}
	
	public StereotypedNamespaces(File file, Map<String, OntResource> namedStereotypes) {
		init(file, namedStereotypes);
	}

	/**
	 * <pre>
	 * Based on UML 2.5 specifications, UML stereotypes are considered
	 * case-insensitive.
	 * 
	 * While the convention is for Stereotype names to start with
	 * an upper-case letter, UML allows for variations and matches between
	 * stereotype definitions and applications to be case-insensitive. 
	 * 
	 * This means that a tool or system should recognize «stereotype» and 
	 * «STEREOTYPE» as the same stereotype, even if their casing differs. 
	 * 
	 * However, some tools may display stereotype names with a different 
	 * initial letter than how they are defined, which is considered valid 
	 * but stylistically obsolete in later versions of UML (2.4+). The best 
	 * practice according to UML 2.4 and later is to start the stereotype 
	 * name with an uppercase letter, similar to how Class names are 
	 * capitalized.
	 * 
	 * As a result the below mapping handles stereotypes in a case insensitive
	 * manner and converts the keys (i.e. stereotypes from the namespaces 
	 * file) to lowercase. Note that the namespaces themselves are case-sensitive 
	 * and therefore this implementation does not do any case-related operations 
	 * on them.
	 * 
	 * It also initializes this namespace mapping class with ALL of the stereotypes 
	 * that exist in the CIM schema imported for the file. That list of stereotypes
	 * is passed in via the namedStereotypes map.
	 * </pre>
	 * 
	 * @param file
	 * @param namedStereotypes
	 */
	private void init(File file, Map<String, OntResource> namedStereotypes) {
		stereo2ResourceMap = new HashMap<>();
		if (namedStereotypes != null) {
			for (String stereotypeKey : namedStereotypes.keySet()) {
				String lowercaseKey = stereotypeKey.toLowerCase();
				stereo2ResourceMap.put(lowercaseKey, namedStereotypes.get(stereotypeKey));
			}
		}
		
		Properties props = new Properties();

		try (FileInputStream fis = new FileInputStream(file)) {
			props.load(fis);

			Map<String, String> s2nMap = new HashMap<>();
			s2nMap = new HashMap<>();
			Map<String, OntResource> s2rMap = new HashMap<>();
			Map<String, OntResource> n2rMap = new HashMap<>();

			for (String stereotypeKey : props.stringPropertyNames()) {
				// The key in props will not be lowercase so we get
				// the properties using the key directly...
				String namespace = props.getProperty(stereotypeKey);
				String lowercaseStereotypeKey = stereotypeKey.toLowerCase();
				if (stereo2ResourceMap.containsKey(lowercaseStereotypeKey)) {
					s2nMap.put(lowercaseStereotypeKey, namespace);
					s2rMap.put(lowercaseStereotypeKey, stereo2ResourceMap.get(lowercaseStereotypeKey));
					n2rMap.put(namespace, stereo2ResourceMap.get(lowercaseStereotypeKey));
				}
			}

			stereo2NSMap = s2nMap;
			stereo2ResourceMap = s2rMap;
			ns2ResourceMap = n2rMap;

			// last..
			hasNamespaces = ns2ResourceMap.size() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Accepts an ontological resource object and processes any stereotypes that are
	 * associated with the resource looking for which one may have a mapping entry
	 * to a namespace and if so returns the namespace.
	 * 
	 * @param stereotypes
	 * @return
	 */
	public String getNamespace(OntResource resource) {
		ResIterator stereotypes = resource.listProperties(UML.hasStereotype);
		while (stereotypes.hasNext()) {
			OntResource stereotype = stereotypes.nextResource();
			if (stereo2NSMap.containsKey(stereotype.getLabel().toLowerCase()))
				return stereo2NSMap.get(stereotype.getLabel().toLowerCase());
		}
		return null;
	}

	/**
	 * Returns the ontological resource object that represents the stereotype
	 * corresponding to the namespace passed in. If one does not exist simply
	 * returns null.
	 * 
	 * @param namespace The fully qualified namespace to return the corresponding
	 *                  stereotype resource for.
	 * @return The stereotype corresponding to the namespace passed i (e.g.
	 *         'Entsoe2') .
	 */
	public OntResource getStereotypeResource(String namespace) {
		return (namespace != null ? ns2ResourceMap.get(namespace) : null);
	}

	/**
	 * Convenience method to determine if a specific namespace is a mapped
	 * namespaced by the end-user.
	 * 
	 * @param namespace The case-sensitive namespace to be tested.
	 * 
	 * @return Whether the specified namespace is defined as a "mapped namespace"
	 */
	public boolean hasNamespace(String namespace) {
		return (namespace != null ? ns2ResourceMap.containsKey(namespace) : false);
	}

	/**
	 * Convenience method to determine whether or not this class has been initialed
	 * with a namespace mappings. If not then CIMTool will revert to checking for the
	 * use of baseuri tagged value definitions in the UML model to determine extension
	 * namespaces. 
	 * 
	 * @return
	 */
	public boolean hasNamespaces() {
		return hasNamespaces;
	}

}
