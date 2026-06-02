package au.com.langdale.profiles;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.xmi.UML;

import java.util.HashSet;

import com.hp.hpl.jena.util.OneToManyMap;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A {@link ProfileClass} subclass that derives its property membership and
 * cardinalities directly from the canonical background (CIM) model rather than
 * from OWL restriction triples in a user-defined profile {@code .owl} file.
 *
 * <h3>How it differs from {@code ProfileClass}</h3>
 * <p>
 * The standard {@link ProfileClass#internalAnalyse()} walks
 * {@code owl:Restriction} superclasses of a profile class to build the
 * {@code props} map and determine each property's range and cardinality. Those
 * restriction triples are created by the user's drag-and-drop actions in the
 * CIMTool profile editor and do not exist in the background CIM schema.
 * </p>
 * <p>
 * {@code SchemaProfileClass} overrides {@link #internalAnalyse()} to instead:
 * </p>
 * <ul>
 *   <li>Set {@code baseClass} to {@code clss} itself — the canonical CIM class
 *       is its own base in this view (there is no profile-level wrapper class).</li>
 *   <li>Populate {@code props} via {@code rdfs:domain} lookup, recording every
 *       property whose declared domain matches this class. Both datatype
 *       properties (attributes) and object properties (associations) are included
 *       since both carry {@code UML.schemaMin} / {@code UML.schemaMax}
 *       annotations written during schema import (XMI, EAP, QEA).</li>
 * </ul>
 * <p>
 * It also overrides {@link #getPropertyInfo(OntResource)} to read range and
 * cardinality from schema annotations rather than from OWL restriction nodes:
 * </p>
 * <ul>
 *   <li>{@code range} — from the property's declared {@code rdfs:range}.</li>
 *   <li>{@code min} — from {@code UML.schemaMin} if present; otherwise the
 *       {@link PropertyInfo} constructor default (0) is retained.</li>
 *   <li>{@code max} — from {@code UML.schemaMax} if present; otherwise the
 *       constructor default ({@code Integer.MAX_VALUE}, or 1 for functional
 *       properties) is retained.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Typical use inside FullModelProfileModel.FullModelCatalogNode.populate():
 * ProfileClass profile = new SchemaProfileClass(clss, namespace);
 * add(new TypeNode(profile));
 * }</pre>
 *
 * <h3>What is unchanged</h3>
 * <p>
 * All of {@link ProfileClass}'s public API contracts remain valid.
 * {@link #analyseBaseClass()} is still called at the end of
 * {@link #internalAnalyse()} so that stereotype flags ({@code compoundBase},
 * {@code enumeratedBase}, {@code constrainedprimitive}) are propagated
 * correctly from the CIM base class. Enumeration value enumeration via
 * {@link #getIndividuals()} continues to work because {@code enumeratedBase} is
 * set correctly and {@code baseClass.listInstances()} lists the CIM enum values.
 * </p>
 *
 * <h3>Visibility note</h3>
 * <p>
 * This class accesses package-private members of {@link ProfileClass} and
 * {@link PropertyInfo} (the {@code props}, {@code baseClass}, and {@code classes}
 * fields; the {@code PropertyInfo} constructor; and the {@code range}, {@code min},
 * {@code max} fields of {@code PropertyInfo}). It uses the public
 * {@link ProfileClass#getSubject()} accessor wherever the underlying class resource
 * is needed, avoiding any need to promote {@code clss} or {@code model} beyond
 * their existing {@code private} visibility. This class must therefore reside in
 * the {@code au.com.langdale.profiles} package.
 * </p>
 */
public class SchemaProfileClass extends ProfileClass {

	/**
	 * Constructs a {@code SchemaProfileClass} for the given background-model class.
	 *
	 * <p>
	 * The superclass constructor calls {@link #analyse()} which delegates to
	 * {@link #internalAnalyse()}. Because this class overrides
	 * {@code internalAnalyse()}, the schema-based analysis runs immediately during
	 * construction. This is safe because {@code internalAnalyse()} only touches
	 * fields declared on {@code ProfileClass} (which the superclass constructor
	 * has already initialised before the call), and not any
	 * {@code SchemaProfileClass}-specific state.
	 * </p>
	 *
	 * @param clss      the background-model class resource whose properties and
	 *                  cardinalities are to be derived from schema annotations
	 * @param namespace the profile namespace; does not affect schema-mode property
	 *                  discovery but is propagated to {@link #analyseBaseClass()}
	 *                  for stereotype handling
	 */
	public SchemaProfileClass(OntResource clss, String namespace) {
		super(clss, namespace);
	}

	/**
	 * Schema-mode override of {@link ProfileClass#internalAnalyse()}.
	 *
	 * <p>
	 * Instead of walking OWL restriction superclasses (which do not exist in the
	 * background model), this method:
	 * </p>
	 * <ol>
	 *   <li>Sets {@code baseClass} to {@code clss} — the CIM class is its own
	 *       base in schema mode.</li>
	 *   <li>Populates {@code props} by querying {@code rdfs:domain} on the class's
	 *       ontology model. Each matching property is stored as its own sentinel
	 *       value in the map; actual range and cardinality are resolved on demand
	 *       by {@link #getPropertyInfo(OntResource)}.</li>
	 *   <li>Calls {@link #analyseBaseClass()} to propagate stereotype flags
	 *       ({@code compoundBase}, {@code enumeratedBase}, etc.) exactly as the
	 *       standard analysis path does.</li>
	 * </ol>
	 */
	@Override
	protected void internalAnalyse() {
		props = new OneToManyMap();
		classes = new HashSet();
		// The canonical CIM class is its own base — there is no profile-wrapper class.
		baseClass = getSubject();

		// Enumerate every property whose rdfs:domain is this class. Both datatype
		// properties (attributes) and object properties (associations) are included;
		// both carry UML.schemaMin / UML.schemaMax written during schema import.
		ResIterator it = getSubject().getOntModel().listSubjectsWithProperty(RDFS.domain, getSubject());
		while (it.hasNext()) {
			OntResource prop = it.nextResource();
			if (prop.isProperty()) {
				// The property is stored as its own sentinel value. Only the key set
				// matters for getProperties(); getPropertyInfo() uses a different path
				// in schema mode and does not call scanRestrict() on these values.
				props.put(prop, prop);
			}
		}

		// Shared with the restriction-based path — propagates stereotype flags
		// (enumeration, compound, constrainedprimitive) from baseClass.
		analyseBaseClass();
	}

	/**
	 * Schema-mode override of {@link ProfileClass#getPropertyInfo(OntResource)}.
	 *
	 * <p>
	 * Constructs a {@link PropertyInfo} for the given property by reading metadata
	 * directly from the background model rather than from OWL restriction nodes
	 * (which do not exist in the background model). Delegates to
	 * {@link #getPropertyInfoFromSchema(OntResource)}.
	 * </p>
	 *
	 * @param prop the property resource from the background model
	 * @return a {@link PropertyInfo} populated from schema annotations
	 */
	@Override
	public PropertyInfo getPropertyInfo(OntResource prop) {
		return getPropertyInfoFromSchema(prop);
	}

	/**
	 * Constructs a {@link PropertyInfo} for the given property using schema
	 * annotations rather than OWL restriction triples.
	 *
	 * <ul>
	 *   <li>{@code range} — set from the property's declared {@code rdfs:range}.</li>
	 *   <li>{@code min} — set from {@code UML.schemaMin} if present; otherwise the
	 *       {@link PropertyInfo} constructor default (0) is retained.</li>
	 *   <li>{@code max} — set from {@code UML.schemaMax} if present; otherwise the
	 *       constructor default ({@code Integer.MAX_VALUE}, or 1 for functional
	 *       properties) is retained.</li>
	 * </ul>
	 *
	 * <p>
	 * Direct access to {@link PropertyInfo}'s package-private constructor and
	 * fields is permitted because this class resides in
	 * {@code au.com.langdale.profiles}.
	 * </p>
	 *
	 * @param prop the property resource from the background model
	 * @return a fully populated {@link PropertyInfo}
	 */
	private PropertyInfo getPropertyInfoFromSchema(OntResource prop) {
		PropertyInfo info = new PropertyInfo(getSubject(), prop);
		// Range comes directly from the property's rdfs:range declaration.
		info.range = prop.getRange();
		// Cardinalities come from the annotation properties written during schema import.
		// Both attributes (datatype properties) and association roles (object properties)
		// carry these annotations — see XMIModel, XMIParser, AbstractEAProjectParsor.
		if (prop.hasProperty(UML.schemaMin)) {
			Integer min = prop.getInteger(UML.schemaMin);
			if (min != null)
				info.min = min;
		}
		if (prop.hasProperty(UML.schemaMax)) {
			Integer max = prop.getInteger(UML.schemaMax);
			if (max != null)
				info.max = max;
		}
		return info;
	}
}
