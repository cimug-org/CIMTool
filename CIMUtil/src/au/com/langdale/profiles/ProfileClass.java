/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import au.com.langdale.xmi.ShadowClassUtils;
import au.com.langdale.xmi.UML;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.util.OneToManyMap;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Represents a class in a profile, encapsulating its base (original) class and
 * restrictions.
 */
public class ProfileClass {
	private final OntResource clss;
	private final String namespace;
	private final OntModel model;

	protected OneToManyMap props;
	protected OntResource baseClass;
	protected Set classes;
	private boolean compoundBase;
	private boolean enumeratedBase;
	private boolean constrainedprimitive;
	private boolean primitive; // Included here for "next gen" impl
	private boolean cimdatatype; // Included here for "next gen" impl
	private String baseURI;
	private final OntResource defaultBase;

	public ProfileClass(OntResource clss, String namespace, OntResource base) {
		this.clss = clss;
		this.namespace = namespace;
		this.model = clss.getOntModel();
		this.defaultBase = base;
		this.baseURI = getNormativeNamespace();
		analyse();
	}

	public ProfileClass(OntResource clss, String namespace) {
		this(clss, namespace, clss.getOntModel().createClass(OWL.Thing.getURI()));
	}

	protected String getNormativeNamespace() {
		String canonicalCIMNamespace = null;
		ResIterator it = this.model.listSubjectsWithProperty(RDF.type, OWL2.Ontology);
		if (it.hasNext()) {
			OntResource ont = it.nextResource();
			ResIterator imports = ont.listProperties(OWL2.imports);
			if (imports.hasNext()) {
				OntResource resource = imports.nextResource();
				if (!imports.hasNext())
					canonicalCIMNamespace = resource.getURI();
			}
		}
		return canonicalCIMNamespace;
	}
	
	/**
	 * Construct a map of properties to restrictions in the context of a given
	 * class.
	 *
	 * <p>
	 * This method is {@code final} and delegates to {@link #internalAnalyse()},
	 * which subclasses may override to substitute an alternative analysis strategy
	 * (e.g. {@code SchemaProfileClass} derives property membership from
	 * {@code rdfs:domain} and {@code UML.schemaMin}/{@code UML.schemaMax}
	 * annotations rather than from OWL restriction triples).
	 * </p>
	 */
	public final void analyse() {
		internalAnalyse();
	}

	/**
	 * Performs the actual analysis work for {@link #analyse()}.
	 *
	 * <p>
	 * The default implementation scans OWL restriction superclasses of the profile
	 * class to build the {@code props} map and identify the external base class.
	 * Subclasses may override this method to substitute a different analysis
	 * strategy without altering the {@code final} public contract of
	 * {@link #analyse()}.
	 * </p>
	 *
	 * <p>
	 * <b>Important:</b> overriding implementations must set {@code props},
	 * {@code classes}, and {@code baseClass} to valid (non-null) values and must
	 * call {@link #analyseBaseClass()} at the end to ensure stereotype flags are
	 * propagated correctly.
	 * </p>
	 */
	protected void internalAnalyse() {
		props = new OneToManyMap();
		classes = new HashSet();
		baseClass = defaultBase;

		ResIterator it = clss.listSuperClasses(true);
		while (it.hasNext()) {
			OntResource node = it.nextResource();
			if (node.isClass() && !node.equals(MESSAGE.Reference)) {
				if (node.isRestriction()) {
					OntResource prop = node.getOnProperty();
					if (prop != null)
						props.put(prop, node);
				} else if (!node.isAnon()) {
					// its a named, general base class
					if (node.getNameSpace().equals(namespace)) {
						classes.add(node); // locally defined class
					} else {
						baseClass = node; // externally defined class (expect only one)
					}
				}
			}
		}
		analyseBaseClass();
	}

	protected void analyseBaseClass() {
		compoundBase = baseClass.hasProperty(UML.hasStereotype, UML.compound);
		enumeratedBase = baseClass.hasProperty(UML.hasStereotype, UML.enumeration);
		constrainedprimitive = baseClass.hasProperty(UML.hasStereotype, UML.constrainedprimitive);
		primitive = baseClass.hasProperty(UML.hasStereotype, UML.primitive);
		cimdatatype = baseClass.hasProperty(UML.hasStereotype, UML.cimdatatype);
		if (enumeratedBase) {
			if (baseClass.hasProperty(UML.hasStereotype)) {
				ResIterator stereotypes = baseClass.listProperties(UML.hasStereotype);
				while (stereotypes.hasNext()) {
					OntResource stereo = stereotypes.nextResource();
					if (!clss.hasProperty(UML.hasStereotype, stereo))
						clss.addProperty(UML.hasStereotype, stereo);
				}
			}
		} else {
			ResIterator clazzes = baseClass.listSuperClasses(false);
			if (clazzes.hasNext()) {
				OntResource clazz = clazzes.nextResource();
				if (clazz.hasProperty(UML.hasStereotype)) {
					ResIterator stereotypes = clazz.listProperties(UML.hasStereotype);
					while (stereotypes.hasNext()) {
						OntResource stereo = stereotypes.nextResource();
						if (!clss.hasProperty(UML.hasStereotype, stereo))
							clss.addProperty(UML.hasStereotype, stereo);
					}
				}
			}
		}
	}

	/**
	 * remove a SomeValueFrom restriction.
	 */
	public void remove(Property prop, OntResource childClass) {
		Iterator it = props.getAll(prop);

		while (it.hasNext()) {
			OntResource res = (OntResource) it.next();
			if (res.isSomeValuesFromRestriction()) {
				Resource type = res.getSomeValuesFrom();
				if (type != null && type.equals(childClass))
					res.remove();
			}
		}
	}

	/**
	 * Change the type of the node in the underlying ontology.
	 */
	public void setBaseClass(OntResource type) {
		if (!OWL.Thing.equals(baseClass)) {
			if (baseClass.equals(type))
				return;
			clss.removeSuperClass(baseClass);
		}
		clss.addSuperClass(type);
		baseClass = type;
		analyseBaseClass();
	}

	/**
	 * Remove all restrictions on the given property.
	 */
	public void remove(OntResource prop) {
		Iterator jt = props.getAll(prop);
		while (jt.hasNext()) {
			OntResource res = (OntResource) jt.next();
			res.remove();
		}
		props.remove(prop);
	}

	public void setMaxCardinality(int card) {
		if (card < Integer.MAX_VALUE)
			clss.setProperty(UML.hasMaxCardinality, card);
		else
			clss.removeAll(UML.hasMaxCardinality);
	}

	public void setMinCardinality(int card) {
		if (card > 0)
			clss.setProperty(UML.hasMinCardinality, card);
		else
			clss.removeAll(UML.hasMinCardinality);
	}

	public int getMaxCardinality() {
		Integer card = clss.getInteger(UML.hasMaxCardinality);
		return card != null ? card.intValue() : Integer.MAX_VALUE;
	}

	public int getMinCardinality() {
		Integer card = clss.getInteger(UML.hasMinCardinality);
		return card != null ? card.intValue() : 0;
	}

	private boolean removeCardinality(OntResource prop) {
		boolean removed = false;
		Iterator it = props.getAll(prop);
		while (it.hasNext()) {
			OntResource res = (OntResource) it.next();
			if (res.isCardinalityRestriction()) {
				res.remove();
				it.remove();
				removed = true;
			}
		}
		return removed;
	}

	private void setMaxCardinality(OntResource prop, int card) {
		removeMaxCardinality(prop);
		if (card < Integer.MAX_VALUE) {
			OntResource res = model.createMaxCardinalityRestriction(null, prop, card);
			clss.addSuperClass(res);
			props.put(prop, res);
		}
	}

	private void removeMaxCardinality(OntResource prop) {
		Iterator it = props.getAll(prop);
		while (it.hasNext()) {
			OntResource res = (OntResource) it.next();
			if (res.isMaxCardinalityRestriction()) {
				res.remove();
				it.remove();
			}
		}
	}

	private void setMinCardinality(OntResource prop, int card) {
		removeMinCardinality(prop);
		if (card > 0) {
			OntResource res = model.createMinCardinalityRestriction(null, prop, card);
			clss.addSuperClass(res);
			props.put(prop, res);
		}
	}

	private void removeMinCardinality(OntResource prop) {
		Iterator it = props.getAll(prop);
		while (it.hasNext()) {
			OntResource res = (OntResource) it.next();
			if (res.isMinCardinalityRestriction()) {
				res.remove();
				it.remove();
			}
		}
	}

	public void setReference(boolean state) {
		setStereotype(UML.byreference, state);
		clss.removeSuperClass(MESSAGE.Reference); // deprecated
	}

	public void setStereotype(Resource stereo, boolean state) {
		if (state)
			clss.addProperty(UML.hasStereotype, stereo);
		else
			clss.removeProperty(UML.hasStereotype, stereo);
	}

	public boolean hasStereotype(Resource stereo) {
		return clss.hasProperty(UML.hasStereotype, stereo) || baseClass.hasProperty(UML.hasStereotype, stereo);
	}

	public OntResource createSomeValuesFrom(OntResource prop, OntResource type) {
		OntResource child = model.createClass();
		child.addSuperClass(type);
		String label = type.getLabel(null);
		if (label == null)
			label = type.getLocalName();
		child.addLabel(label, null);
		OntResource res = model.createSomeValuesFromRestriction(null, prop, child);
		clss.addSuperClass(res);
		props.put(prop, res);
		return child;
	}

	public OntResource createAllValuesFrom(OntResource prop, int minCard, int maxCard) {
		OntResource child;
		OntResource range = prop.getRange();

		if (prop.isDatatypeProperty()) {
			child = model.createIndividual(RDFS.Datatype);
			if (range != null)
				child.addProperty(OWL.equivalentClass, range);
		} else {
			child = model.createClass();
			if (range != null)
				child.addSuperClass(range);
		}
		String label = prop.getLabel(null);
		if (label == null)
			label = prop.getLocalName();
		child.addLabel(label, null);

		if (prop.hasProperty(UML.hasStereotype)) {
			ResIterator stereotypes = prop.listProperties(UML.hasStereotype);
			while (stereotypes.hasNext()) {
				OntResource stereo = stereotypes.nextResource();
				if (!child.hasProperty(UML.hasStereotype, stereo))
					child.addProperty(UML.hasStereotype, stereo);
			}
		}

		OntResource res = model.createAllValuesFromRestriction(null, prop, child);
		clss.addSuperClass(res);
		props.put(prop, res);

		if (minCard > 0 && canBeRequired(prop)) {
			OntResource req = model.createMinCardinalityRestriction(null, prop, minCard);
			clss.addSuperClass(req);
			props.put(prop, req);
		}
		if (maxCard != Integer.MAX_VALUE) {
			OntResource req = model.createMaxCardinalityRestriction(null, prop, maxCard);
			clss.addSuperClass(req);
			props.put(prop, req);
		}

		return child;
	}

	public OntResource createAllValuesFrom(OntResource prop, SelectionOptions selectionOptions) {
		boolean required = selectionOptions.isPropertyRequired();
		boolean byReference = selectionOptions.isByReference();
		boolean useSchemaCardinality = selectionOptions.useSchemaCardinality();

		OntResource child;
		OntResource range = prop.getRange();

		if (prop.isDatatypeProperty()) {
			child = model.createIndividual(RDFS.Datatype);
			if (range != null)
				child.addProperty(OWL.equivalentClass, range);
		} else {
			child = model.createClass();
			if (range != null) {
				child.addSuperClass(range);
				if (byReference) {
					child.addProperty(UML.hasStereotype, UML.byreference);
				}
			}
		}
		String label = prop.getLabel(null);
		if (label == null)
			label = prop.getLocalName();
		child.addLabel(label, null);

		if (prop.hasProperty(UML.hasStereotype)) {
			ResIterator stereotypes = prop.listProperties(UML.hasStereotype);
			while (stereotypes.hasNext()) {
				OntResource stereo = stereotypes.nextResource();
				if (!child.hasProperty(UML.hasStereotype, stereo))
					child.addProperty(UML.hasStereotype, stereo);
			}
		}

		OntResource res = model.createAllValuesFromRestriction(null, prop, child);
		clss.addSuperClass(res);
		props.put(prop, res);

		int minCard = 0; // Default min
		int maxCard = Integer.MAX_VALUE; // Default max

		/**
		 * If directed via useSchemaCardinality to use the cardinality as defined in the
		 * base CIM schema imported for the project, then any relevant restrictions will
		 * be based of those min and max cardinalities. Note that the values for these
		 * will be found via OWL2.AnnotationProperty(s) named schemaMin and schemaMax
		 * and defined on the property during initial import of the schema i.e. XMI,
		 * EAP, QEA file...
		 */
		if (useSchemaCardinality) {
			minCard = prop.getInteger(UML.schemaMin);
			maxCard = prop.getInteger(UML.schemaMax);
		}

		if (required && canBeRequired(prop)) {
			OntResource minRestriction = model.createMinCardinalityRestriction(null, prop, 1);
			clss.addSuperClass(minRestriction);
			props.put(prop, minRestriction);

			if (useSchemaCardinality) {
				if (maxCard != Integer.MAX_VALUE) {
					OntResource maxRestriction = model.createMaxCardinalityRestriction(null, prop, maxCard);
					clss.addSuperClass(maxRestriction);
					props.put(prop, maxRestriction);
				}
			}
		} else if (useSchemaCardinality) {
			if (minCard > 0 && canBeRequired(prop)) {
				OntResource req = model.createMinCardinalityRestriction(null, prop, minCard);
				clss.addSuperClass(req);
				props.put(prop, req);
			}
			if (maxCard != Integer.MAX_VALUE) {
				OntResource req = model.createMaxCardinalityRestriction(null, prop, maxCard);
				clss.addSuperClass(req);
				props.put(prop, req);
			}
		}

		return child;
	}

	public OntResource createSuperClass(OntResource base) {
		OntResource child = model.createClass(namespace + base.getLocalName());
		child.addSuperClass(base);
		return addSuperClass(child);
	}

	public OntResource addSuperClass(OntResource child) {
		clss.addSuperClass(child);
		classes.add(child);
		return child;
	}

	public Iterator getIndividuals() {
		if (clss.isEnumeratedClass()) {
			return clss.getOneOf().listResourceElements();
		}
		if (enumeratedBase && classes.size() == 0) {
			return baseClass.listInstances();
		}
		return Collections.EMPTY_LIST.iterator();
	}

	public void setRestrictedEnum(boolean state) {
		if (state && !clss.isEnumeratedClass()) {
			clss.addProperty(OWL.oneOf, model.createList(baseClass.listInstances()));
		} else if (!state && clss.isEnumeratedClass()) {
			OntResource extent = clss.getOneOf();
			clss.removeAll(OWL.oneOf);
			if (!extent.equals(RDF.nil))
				extent.removeList();
		}
	}

	public boolean isRestrictedEnum() {
		return clss.isEnumeratedClass();
	}

	public void addIndividual(OntResource indiv) {
		if (!indiv.hasRDFType(baseClass))
			return;

		if (!clss.isEnumeratedClass()) {
			clss.addProperty(OWL.oneOf, model.createList().cons(indiv));
		} else {
			OntResource extent = clss.getOneOf();
			if (!extent.contains(indiv))
				clss.setOneOf(extent.cons(indiv));
		}
	}

	public void removeIndividual(OntResource indiv) {
		setRestrictedEnum(true);
		OntResource extent = clss.getOneOf();
		clss.setOneOf(extent.remove(indiv));
	}

	public boolean isUnion() {
		return clss.hasProperty(OWL.unionOf);
	}

	public Iterator getProperties() {
		return props.keySet().iterator();
	}

	public boolean hasProperty(Property prop) {
		return props.containsKey(prop);
	}

	public Iterator getRestrictions(Property prop) {
		return props.getAll(prop);
	}

	public Iterator getSuperClasses() {
		if (isPropertyRange())
			return Collections.EMPTY_LIST.iterator();
		else
			return classes.iterator();
	}

	public Iterator getSubClasses() {
		Set subClasses = new HashSet();
		ResIterator it = clss.listSubClasses(true);
		while (it.hasNext()) {
			OntResource node = it.nextResource();
			if (node.isClass() && !node.isAnon())
				subClasses.add(node);
		}
		return subClasses.iterator();
	}

	public PropertyInfo getPropertyInfo(OntResource prop) {

		PropertyInfo info = new PropertyInfo(clss, prop);
		Iterator jt = props.getAll(prop);
		while (jt.hasNext()) {
			info.scanRestrict((OntResource) jt.next());
		}
		assert info.range != null;
		return info;
	}

	public OntResource getSubject() {
		return clss;
	}

	public String getNamespace() {
		return namespace;
	}

	public OntResource getBaseClass() {
		return baseClass;
	}

	public boolean isEnumerated() {
		return enumeratedBase;
	}

	public boolean isCompound() {
		return compoundBase;
	}

	public boolean isConstrainedPrimitive() {
		return constrainedprimitive;
	}

	public boolean isPrimitive() {
		return primitive;
	}

	public boolean isCIMDatatype() {
		return cimdatatype;
	}

	/**
	 * Convenience method that checks whether the profile class is based on a
	 * "restricted type". For the purposes of CIM, a "restricted type" is defined as
	 * any class defined in the CIM that has one of the "restricted stereotypes"
	 * assigned to it. The set of such stereotypes includes:
	 * 
	 * <pre>
	 * «enumeration»
	 * «CIMDatatype»
	 * «Primitive»
	 * «ConstrainedPrimitive»
	 * «Compound»
	 * </pre>
	 * 
	 * Note that the term "restricted" simply means that such classes are not
	 * allowed to participate in association or inheritance relationships, but
	 * instead are restricted to only being used as a declared type of an attribute
	 * in the model.
	 * 
	 * @param resource The resource for which to test whether or not it is a
	 *                 restricted class.
	 * @return True if this profile class is, by definition, a CIM restricted class;
	 *         false otherwise.
	 */
	public boolean isRestrictedClass() {
		return baseClass.hasProperty(UML.hasStereotype, UML.cimdatatype)
				|| baseClass.hasProperty(UML.hasStereotype, UML.enumeration) || //
				baseClass.hasProperty(UML.hasStereotype, UML.primitive) || //
				baseClass.hasProperty(UML.hasStereotype, UML.compound) || //
				baseClass.hasProperty(UML.hasStereotype, UML.constrainedprimitive);
	}

	public boolean isReference() {
		return hasStereotype(UML.byreference) || clss.hasSuperClass(MESSAGE.Reference, false);
	}

	public class PropertyInfo {
		private OntResource prop;
		OntResource range;
		private OntResource domain;
		int min = 0;
		int max = Integer.MAX_VALUE;

		PropertyInfo(OntResource domain, OntResource prop) {
			this.prop = prop;
			this.domain = domain;
			if (prop.isFunctionalProperty())
				max = 1;
			else if (prop.isDatatypeProperty() && prop.hasProperty(OWL2.maxCardinality))
				max = prop.getMaxCardinality();
		}

		public OntResource getDomain() {
			return domain;
		}

		public OntResource getProperty() {
			return prop;
		}

		public OntResource getRange() {
			return range;
		}

		public ProfileClass createProfileClass() {
			if (range == null)
				return null;

			OntResource type = prop.getRange();
			if (type != null && type.isClass())
				return new ProfileClass(range, namespace, type);
			else
				return new ProfileClass(range, namespace);
		}

		public ProfileClass getDomainProfile() {
			return ProfileClass.this;
		}

		public boolean isRequired() {
			return min > 0;
		}

		public boolean canBeRequired() {
			return ProfileClass.this.canBeRequired(prop);
		}

		public boolean isFunctional() {
			return max == 1;
		}

		public boolean isAlwaysFunctional() {
			// TODO: drop datatype term here and in ctor?
			return prop.isFunctionalProperty() || (prop.isDatatypeProperty()
					&& (prop.hasProperty(UML.schemaMax) && (prop.getInteger(UML.schemaMax).intValue() == 1)));
		}

		public void setMaxCardinality(int card) {
			if (removeCardinality(prop))
				ProfileClass.this.setMinCardinality(prop, min);
			ProfileClass.this.setMaxCardinality(prop, card);
			max = card;
		}

		public int getMaxCardinality() {
			return max;
		}

		public void setMinCardinality(int card) {
			if (removeCardinality(prop))
				ProfileClass.this.setMaxCardinality(prop, max);
			ProfileClass.this.setMinCardinality(prop, card);
			min = card;
		}

		public int getMinCardinality() {
			return min;
		}

		/**
		 * Scan a restriction computing net cardinality for this property.
		 */
		private void scanRestrict(OntResource res) {
			if (res.isAllValuesFromRestriction())
				scanAllValuesFromRestriction(res);
			else if (res.isCardinalityRestriction())
				scanCardinalityRestriction(res);
			else if (res.isMinCardinalityRestriction())
				scanMinCardinalityRestriction(res);
			else if (res.isMaxCardinalityRestriction())
				scanMaxCardinalityRestriction(res);
		}

		private void scanAllValuesFromRestriction(OntResource res) {
			range = res.getAllValuesFrom();
		}

		private void scanCardinalityRestriction(OntResource res) {
			if (res.getCardinality() > min)
				min = res.getCardinality();
			if (res.getCardinality() < max)
				max = res.getCardinality();
		}

		private void scanMinCardinalityRestriction(OntResource res) {
			if (res.getMinCardinality() > min)
				min = res.getMinCardinality();
		}

		private void scanMaxCardinalityRestriction(OntResource res) {
			if (res.getMaxCardinality() < max)
				max = res.getMaxCardinality();
		}

	}

	/**
	 * Returns a ProfileClass for each named class.
	 */
	public static Iterator getProfileClasses(final OntModel profileModel, final OntModel fullModel) {
		return new Iterator() {
			List classes = getNamedProfiles(profileModel, fullModel);
			int ix;

			public boolean hasNext() {
				return ix < classes.size();
			}

			public Object next() {
				OntResource clss = (OntResource) classes.get(ix++);
				return new ProfileClass(clss, clss.getNameSpace());
			}

			public void remove() {
			}
		};
	}

	/**
	 * Return a list of named classes (excluding some named support classes).
	 */
	public static List getNamedProfiles(OntModel profileModel, OntModel fullModel) {
		List classes = new ArrayList();
		ResIterator jt = profileModel.listNamedClasses();
		while (jt.hasNext()) {
			Resource symbol = jt.nextResource();
			if (!symbol.getNameSpace().equals(MESSAGE.NS) && !symbol.getNameSpace().equals(OWL.NS)) {
				classes.add(fullModel.createResource(symbol.asNode()));
			}
		}
		return classes;
	}

	public boolean canBeRequired(OntResource prop) {
		OntResource domain = prop.getDomain();
		return domain == null || baseClass.hasSuperClass(domain);
	}

	/**
	 * Broaden this class by making it a union of its present definition and a new
	 * anonymous profile of the given base class.
	 * 
	 * @return the new profile class
	 */
	public OntResource createUnionMember(OntResource base) {
		OntResource member = model.createClass();
		member.addSuperClass(base);
		member.addLabel(base.getLocalName(), null);
		addUnionMember(member);
		return member;
	}

	/**
	 * Broaden this class by making it a union of its present definition and the
	 * given profile class.
	 * 
	 */
	public void addUnionMember(OntResource child) {
		OntResource union = clss.getResource(OWL.unionOf);

		if (union != null && union.isList()) {
		} else if (isPropertyRange())
			union = buildUnion();
		else
			union = model.createList();

		union = union.cons(child);
		clss.setProperty(OWL.unionOf, union);
	}

	/**
	 * 
	 * Create an explicit union definition for this class.
	 * 
	 * If the class has profile superclasses these become the union members. (This
	 * is intended for the case of a single superclass that is to be broadened.)
	 * 
	 * If the class has property restrictions, they are moved to a fresh anonymous
	 * class and that becomes the single union member.
	 * 
	 * @return an RDFList representing the members of the union.
	 */
	private OntResource buildUnion() {
		OntResource union = model.createList();
		if (!classes.isEmpty()) {
			for (Iterator it = classes.iterator(); it.hasNext();) {
				OntResource sup = (OntResource) it.next();
				clss.removeSuperClass(sup);
				union = union.cons(sup);
			}
			classes = new HashSet();
		}
		if (!props.isEmpty()) {
			OntResource member = model.createClass();
			member.addSuperClass(baseClass);
			member.addLabel(baseClass.getLocalName(), null);

			for (Iterator it = props.keySet().iterator(); it.hasNext();) {
				OntResource prop = (OntResource) it.next();
				for (Iterator iv = props.getAll(prop); iv.hasNext();) {
					OntResource res = (OntResource) iv.next();
					clss.removeSuperClass(res);
					member.addSuperClass(res);
				}
			}
			union = union.cons(member);
			props = new OneToManyMap();
		}
		return union;
	}

	private void removeAllProps() {
		for (Iterator it = props.keySet().iterator(); it.hasNext();) {
			OntResource prop = (OntResource) it.next();
			remove(prop);
		}
	}

	/**
	 * Narrow this class by removing one of the union members of which it is
	 * composed.
	 */
	public void removeUnionMember(OntResource child) {

		OntResource union = clss.getResource(OWL.unionOf);
		if (union != null && union.isList()) {
			union = union.remove(child);
			clss.setProperty(OWL.unionOf, union);
		} else if (isPropertyRange()) {
			if (child.equals(clss))
				removeAllProps();
			else
				removeSuperClass(child);
		}
	}

	/**
	 * 
	 * If this class is a union, return its members as ProfileClasses otherwise
	 * return an empty list.
	 * 
	 * The only classes that are may be unions are the anonymous ranges of
	 * restricted properties and these are always regarded as unions.
	 * 
	 * If the class has an explicit unionOf axiom, its declared members are
	 * returned.
	 * 
	 * If the class if a property range and has property restrictions in turn, then
	 * it is itself regarded as the single member of the union.
	 * 
	 * Otherwise an empty List is returned.
	 * 
	 * 
	 * @return A List of ProfileClass
	 */
	public List getUnionMembers() {
		List members = new ArrayList();

		OntResource union = clss.getResource(OWL.unionOf);
		if (union != null && union.isList()) {
			for (ResIterator it = union.listResourceElements(); it.hasNext();) {
				OntResource item = it.nextResource();
				if (item.isClass()) {
					members.add(new ProfileClass(item, namespace));
				}
			}
		} else if (isPropertyRange()) {
			for (Iterator it = classes.iterator(); it.hasNext();)
				members.add(new ProfileClass((OntResource) it.next(), namespace));
			if (!props.isEmpty())
				members.add(new ProfileClass(clss, namespace, baseClass));
		}
		return members;
	}

	public boolean isPropertyRange() {
		return !defaultBase.equals(OWL.Thing);
	}

	public void removeSuperClass(OntResource child) {
		if (classes.remove(child))
			clss.removeSuperClass(child);
	}
}
