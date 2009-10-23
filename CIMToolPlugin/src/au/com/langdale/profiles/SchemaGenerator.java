/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.langdale.kena.Composition;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * This class is the driver for a number of profile model transformation and
 * conversion utilities.  It traverses a profile model and fires events (defined
 * by abstract methods) for each feature of the profile.
 *
 * TODO: this class has not been updated to handle OWL unions in the profile.
 */
public abstract class SchemaGenerator extends ProfileUtility implements Runnable {
	private OntModel model;
	private OntModel profileModel;
	private String namespace;
	private Catalog catalog;
	private Set datatypes = new HashSet();
	private Set packages = new HashSet();
	private PropertyAccumulator props = new PropertyAccumulator();
	private EnumAccumulator enums = new EnumAccumulator();

	private List work = new LinkedList(); // unprocessed profiles
	private boolean withInverses;
	private boolean preserveNamespaces;

	public class Catalog extends BaseMap {
		protected Map classes = new HashMap(); 	// base class to profile uri
		private Map profiles = new HashMap(); 	// profile uri to base class

		public Catalog() {
		}

		@Override
		public void add(OntResource base, OntResource clss) {

			if(clss.isAnon()) {
				if( ! classes.containsKey(base)) {
					add(base, constructURI(base));
				}
			}
			else {
				if( classes.containsKey(base) )
					rename(base);
				else
					add( base, constructURI(base, clss));
			}
			super.add(base, clss); // purely to support findProfiles()
		}

		public void add(OntResource base, String uri) {
			Object alias = profiles.get(uri);
			if( alias != null ){
				if(alias.equals(base))
					return;
				rename((OntResource)alias);
			}
			classes.put(base, uri);
			profiles.put(uri, base);
		}

		public boolean add(OntResource base) {
			if( classes.containsKey(base))
				return false;

			add(base, constructURI(base));
			return true;
		}

		private void rename(OntResource base) {
			String uri = constructURI(base);
			String old = (String) classes.get(base);
			if( old == null || uri.equals(old))
				return;
			classes.remove(base);
			profiles.remove(old);
			OntResource alias = (OntResource) profiles.get(uri);
			if( alias != null )
				rename(alias);
			classes.put(base, uri);
			profiles.put(uri, base);
		}

		public String getURI(OntResource base) {
			return (String) classes.get(base);
		}

		public Collection getBases() {
			return classes.keySet();
		}

		public Collection getURIs() {
			return classes.values();
		}

		public OntModel buildLattice() {
			OntModel hierarchy = ModelFactory.createTransInf();
			Collection bases = getBases();
			Iterator it = bases.iterator();
			while(it.hasNext()) {
				OntResource clss = (OntResource)it.next();
				OntResource profile = hierarchy.createClass(getURI(clss));

				Iterator jt = clss.listSuperClasses(false);
				while(jt.hasNext()) {
					OntResource superClass = (OntResource) jt.next();
					if( bases.contains(superClass))
						profile.addSuperClass(hierarchy.createClass(getURI(superClass)));
				}
			}
			return hierarchy;
		}
	}

	public static class TypeInfo {
		public final String type, xsdtype;
		public TypeInfo(OntResource range, SchemaGenerator context) {
			if( range != null ) {
				if( range.getNameSpace().equals(XSD.getURI())) {
					type = null;
					xsdtype = range.getURI();
				}
				else {
					type = context.constructURI(range);
					Resource cand = range.getSameAs();
					if( cand != null && cand.getNameSpace().equals(XSD.getURI()))
						xsdtype = cand.getURI();
					else
						xsdtype = null;
				}
			}
			else {
				type = xsdtype = null;
			}
		}
	}

	public SchemaGenerator(OntModel profileModel, OntModel backgroundModel, String namespace, boolean preserveNamespaces, boolean inverses) {
		this.profileModel = profileModel;
		this.model = Composition.merge(profileModel, backgroundModel);
		this.namespace = namespace;
		this.preserveNamespaces = preserveNamespaces;
		this.catalog = new Catalog();
		this.withInverses = inverses;
	}

	public SchemaGenerator(OntModel profileModel, OntModel backgroundModel, String namespace) {
		this( profileModel, backgroundModel, namespace, false, false);
	}

	public void run() {

		scanProfiles();
		if(withInverses)
			addInverseProperties();
		scanDomainsAndRanges();

		// emit classes first
		Iterator it = catalog.getBases().iterator();
		while( it.hasNext()) {
			OntResource base = (OntResource)it.next();
			generateClass(base);
		}

		// emit datatypes
		Iterator nt = datatypes.iterator();
		while( nt.hasNext()) {
			OntResource type = (OntResource)nt.next();
			TypeInfo info = new TypeInfo(type, this);
			if( info.type != null) {
				emitDatatype(info.type, info.xsdtype);
				annotate(info.type, type);
			}
		}

		// emit properties
		Iterator lt = props.getGroups().iterator();
		while( lt.hasNext()) {
			generateProperty((PropertyGroup)lt.next());
		}

		// emit superclass relationships
		generateLattice(catalog.buildLattice());

		// emit any ontology (header) properties
		generateOntologyFlags();
	}

	// construct a URI for a base model class, datatype, property or individual
	private String constructURI(OntResource base) {
		if( ! preserveNamespaces)
			return namespace + base.getLocalName();
		else
			return base.getURI();
	}

	// construct a URI for named profile
	private String constructURI(OntResource base, OntResource profile) {
		if( ! preserveNamespaces)
			return profile.getURI(); // use the profile URI directly, usually in namespace but not always
		else
			return base.getURI();
	}

	private void scanProfiles() {
		Iterator it = ProfileClass.getProfileClasses(profileModel, model);
		while( it.hasNext())
			work.add(it.next());

		while( ! work.isEmpty()) {
			ProfileClass profile = (ProfileClass) work.remove(0);
			scanProperties(profile);
			OntResource base = profile.getBaseClass();
			if( base == null) {
				log("No base for profile class", profile.getSubject());
			}
			else {
				catalog.add(base, profile.getSubject());
				if((profile.isEnumerated() || profile.isRestrictedEnum()) && ! profile.isUnion())
					enums.add(base, profile.getIndividuals());
			}
		}
	}

	private boolean scanProperties(ProfileClass profile) {
		Iterator it = profile.getProperties();
		boolean some = it.hasNext();

		while( it.hasNext()) {
			PropertyInfo info = profile.getPropertyInfo((OntResource) it.next());
			ProfileClass range_profile = props.add( info );
			if( range_profile != null)
				work.add(range_profile);
		}

		return some;
	}

	private void addInverseProperties() {
		Iterator it = new ArrayList(props.getGroups()).iterator();
		while( it.hasNext()) {
			PropertyGroup group = (PropertyGroup) it.next();
			PropertySpec info = group.getSummary();
			OntResource inverse = info.prop.getInverse();
			if( inverse != null && ! props.containsKey(inverse)) {
				props.add(inverse, info.base_domain, info.base_range);
			}
		}
	}

	private void scanDomainsAndRanges() {
		Iterator it = props.getGroups().iterator();
		while( it.hasNext()) {
			PropertyGroup group = (PropertyGroup) it.next();
			scanSpec(group.getSummary());
			for (Iterator jt = group.getRestrictions().iterator(); jt.hasNext();) {
				scanSpec((PropertySpec) jt.next());
			}
		}
	}

	private void scanSpec(PropertySpec spec) {
		catalog.add(spec.base_domain);
		if( spec.base_range != null) {
			catalog.add(spec.base_range);
		}
		else  {
			OntResource range = spec.prop.getRange();
			if( range != null)
				datatypes.add(range);
		}
	}

	private void generateLattice(OntModel hierarchy) {
		Collection uris = catalog.getURIs();
		Iterator it = uris.iterator();
		while(it.hasNext()) {
			OntResource profile = hierarchy.createResource((String)it.next());

			Iterator jt = profile.listSuperClasses(true);
			while(jt.hasNext()) {
				OntResource superClass = (OntResource) jt.next();
				emitSuperClass(profile.getURI(), superClass.getURI());
			}
		}
	}

	private void generateClass(OntResource base) {
		String uri = catalog.getURI(base);

		emitClass(uri, base.getURI());
		emitLabel(uri, ResourceFactory.createResource(uri).getLocalName());
		emitComment(uri, extractComment(base), extractProfileComment(base));
		generateIndividuals(uri, base);
		generateBaseStereotypes(uri, base);
		for (Iterator it = catalog.find(base).iterator(); it.hasNext();) {
			OntResource clss = (OntResource) it.next();
			generateStereotypes(uri, clss);
		}
		generatePackage(uri, base);
	}

	private void generateIndividuals(String type_uri, OntResource base) {
		for (Iterator ix = enums.get(base).iterator(); ix.hasNext();) {
			OntResource instance = (OntResource) ix.next();
			String uri = constructURI(instance);
			emitInstance(uri, instance.getURI(), type_uri);
			annotate(uri, instance);
		}
	}

	private void generateProperty(PropertyGroup group) {
		PropertySpec info = group.getSummary();
		OntResource prop = group.getProperty();
		String uri = constructURI(prop);
		String domain = catalog.getURI(info.base_domain);
		if(prop.isDatatypeProperty()) {
			TypeInfo range = new TypeInfo( prop.getRange(), this);
			emitDatatypeProperty(uri, prop.getURI(), domain, range.type, range.xsdtype, info.required);
		}
		else {
			String range = catalog.getURI(info.base_range);
			emitObjectProperty(uri, prop.getURI(), domain, range, info.required, info.functional);

			OntResource inverse = prop.getInverse();
			if( inverse != null && props.containsKey(inverse)) {
				emitInverse(uri, constructURI(inverse));
			}
		}
		if( info.label != null)
			emitLabel(uri, info.label);

		generateRestrictions(group);

		emitComment(uri, extractComment(prop), info.comment);
		generateBaseStereotypes(uri, prop);
		if(info.reference)
			emitStereotype(uri, UML.byreference.getURI());
	}

	private void generateRestrictions(PropertyGroup group) {
		String uri = constructURI(group.getProperty());

		for (Iterator it = group.getRestrictions().iterator(); it.hasNext();) {
			PropertySpec rest = (PropertySpec) it.next();
			String domain = catalog.getURI(rest.base_domain);
			if(rest.prop.isDatatypeProperty()) {
				TypeInfo range = new TypeInfo( rest.prop.getRange(), this);
				emitRestriction(uri, domain, range.xsdtype);
			}
			else if( rest.base_range != null){
				emitRestriction(uri, domain, catalog.getURI(rest.base_range));
			}
			if( rest.required || rest.functional ) {
				emitRestriction(uri, domain, rest.required, rest.functional);
			}
		}
	}

	private void generateBaseStereotypes(String uri, OntResource base) {
		ResIterator it = base.listProperties(UML.hasStereotype);
		while (it.hasNext()) {
			emitBaseStereotype(uri, it.nextResource().getURI());
		}
	}

	private void generateOntologyFlags() {
		for (Iterator it = profileModel.listIndividuals(MESSAGE.Flag); it.hasNext();) {
			OntResource	flag = (OntResource) it.next();
			emitOntProperty(flag.getURI());
		}
	}

	private void generateStereotypes(String uri, OntResource base) {
		ResIterator it = base.listProperties(UML.hasStereotype);
		while (it.hasNext()) {
			emitStereotype(uri, it.nextResource().getURI());
		}
	}

	private void generatePackage(String uri, OntResource base) {
		Resource symbol = base.getIsDefinedBy();
		if( symbol == null)
			return;

		OntResource pack = (OntResource)symbol;
		String curi = constructURI(pack);
		if( ! packages.contains(pack)) {
			emitPackage(curi);
			annotate(curi, pack);
			packages.add(pack);
			generatePackage(curi, pack);
		}
		emitDefinedBy(uri, curi);
	}

	private String extractProfileComment(OntResource base) {
		String comment = null;

		Iterator it = catalog.find(base).iterator();
		while(it.hasNext())
			comment = appendComment(comment, (OntResource) it.next());

		return comment;
	}

	private void annotate(String uri, OntResource base) {
		String label = base.getLabel(null);
		if( label != null)
			emitLabel(uri, label);
		String comment = base.getComment(null);
		if( comment != null)
			emitComment(uri, comment, null);
	}

	protected abstract void emitLabel(String uri, String label);
	protected abstract void emitComment(String uri, String baseComment, String profileComment);
	protected abstract void emitSuperClass(String subClass, String superClass);
	protected abstract void emitClass(String uri, String base) ;
	protected abstract void emitInstance(String uri, String base, String type);
	protected abstract void emitDatatype(String uri, String xsdtype) ;
	protected abstract void emitDatatypeProperty(String uri, String base, String domain, String type, String xsdtype, boolean required) ;
	protected abstract void emitObjectProperty(String uri, String base, String domain, String range, boolean required, boolean functional) ;
	protected abstract void emitRestriction(String uri, String domain, String range);
	protected abstract void emitRestriction(String uri, String domain, boolean required, boolean functional) ;
	protected abstract void emitInverse(String uri, String iuri) ;
	protected abstract void emitStereotype(String uri, String stereo) ;
	protected abstract void emitBaseStereotype(String uri, String stereo) ;
	protected abstract void emitOntProperty(String uri);
	protected abstract void emitOntProperty(String uri, String value);
	protected abstract void emitDefinedBy(String uri, String container);
	protected abstract void emitPackage(String uri) ;

	protected void log(String string, FrontsNode node) {
		log(string + ": " + node);
	}

	protected void log(String item) {
		System.out.println(item);
	}
}
