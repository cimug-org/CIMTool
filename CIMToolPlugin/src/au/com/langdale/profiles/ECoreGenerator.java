package au.com.langdale.profiles;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.ecore.*;

import au.com.langdale.kena.OntModel;

public class ECoreGenerator extends SchemaGenerator {

	public ECoreGenerator(OntModel profileModel, OntModel backgroundModel,
			String namespace, boolean preserveNamespaces, boolean inverses) {
		super(profileModel, backgroundModel, namespace, preserveNamespaces, inverses);
		if(namespace != null)
			result.setNsPrefix("cim");
			result.setNsURI(namespace);
	}

	EcoreFactory coreFactory = EcoreFactory.eINSTANCE;

	EPackage result = coreFactory.createEPackage();

	Map<String, EPackage> ePackages = new HashMap<String, EPackage>(); 	// uri to EPackage
	Map<String, EClass> eClasses = new HashMap<String, EClass>(); 	// uri to EClass

	public EPackage getResult() {
		return result;
	}

	public String getUriFragment(String uri) {
		String[] fragments = uri.split("#");
		return fragments[1];
	}

	public String[] getFragment(String frag) {
		String[] fragments = frag.split("\\.");
		return fragments;
	}

	/*
	 * Adds packages and classifiers without parent packages to the resulting package.
	 *
	 * @see au.com.langdale.profiles.SchemaGenerator#run()
	 */
	@Override
	public void run() {
		super.run();

		for (Iterator<EPackage> i = ePackages.values().iterator(); i.hasNext();) {
			EPackage pkg = i.next();
			if (pkg.getESuperPackage() == null) {
				result.getESubpackages().add(pkg);
			}
		}

		for (Iterator<EClass> i = eClasses.values().iterator(); i.hasNext();) {
			EClass klass = i.next();
			if (klass.getEPackage() == null) {
				result.getEClassifiers().add(klass);
			}
		}
	}

	@Override
	protected void emitPackage(String uri) {
//		System.out.println("Package: " + uri);

		EPackage pkg = coreFactory.createEPackage();
		pkg.setNsPrefix("cim");
		pkg.setNsURI(uri);
		ePackages.put(uri, pkg);
//		result.getESubpackages().add(pkg);
	}

	@Override
	protected void emitClass(String uri, String base) {
//		System.out.println("Class: " + uri + ", " + base);

		EClass klass = coreFactory.createEClass();
		eClasses.put(uri, klass);
//		klass.setName(uri);
//		result.getEClassifiers().add(klass);
	}

	@Override
	protected void emitDefinedBy(String uri, String container) {
//		System.out.println("DefinedBy: " + uri + ", " + container);

		if (ePackages.containsKey(container) && ePackages.containsKey(uri)) {
			EPackage parent = ePackages.get(container);
			EPackage child = ePackages.get(uri);
			parent.getESubpackages().add(child);
		} else if (ePackages.containsKey(container) && eClasses.containsKey(uri)) {
			EPackage parent = ePackages.get(container);
			EClass child = eClasses.get(uri);
			parent.getEClassifiers().add(child);
		} else {
			System.out.println("Container [" + container + "] for " + uri + " not found.");
		}
	}

	/*
	 * Label: http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor.length, length
	 */
	@Override
	protected void emitLabel(String uri, String label) {
//		System.out.println("Label: " + uri + ", " + label);

		if (ePackages.containsKey(uri)) {
			EPackage named = ePackages.get(uri);
			named.setName(label);
		} else if (eClasses.containsKey(uri)) {
			EClass named = eClasses.get(uri);
			named.setName(label);
		}
	}

	@Override
	protected void emitSuperClass(String subClass, String superClass) {
//		System.out.println("SuperClass: " + subClass + ", " + superClass);
	}

	@Override
	protected void emitDatatype(String uri, String xsdtype) {
//		System.out.println("DataType: " + uri + ", " + xsdtype);
	}

	/*
	 * DataType Property: http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor.length, http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor.length, http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor, http://iec.ch/TC57/2009/CIM-schema-cim14#Length, http://www.w3.org/2001/XMLSchema#float, false
	 */
	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain,
			String type, String xsdtype, boolean required) {
//		System.out.println("DataType Property: " + uri + ", " + base + ", " +
//				domain + ", " + type + ", " + xsdtype + ", " + required);
	}

	@Override
	protected void emitStereotype(String uri, String stereo) {
//		System.out.println("StereoType: " + uri + ", " + stereo);
	}

	/*
	 * BaseStereoType: http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor.length, http://langdale.com.au/2005/UML#attribute
	 */
	@Override
	protected void emitBaseStereotype(String uri, String stereo) {
//		System.out.println("BaseStereoType: " + uri + ", " + stereo);
	}

	/*
	 * Comment: http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor.length, Segment length for calculating line section capabilities, null
	 */
	@Override
	protected void emitComment(String uri, String baseComment,
			String profileComment) {
//		System.out.println("Comment: " + uri + ", " + baseComment + ", " + profileComment);
	}

	@Override
	protected void emitInstance(String uri, String base, String type) {
//		System.out.println("Instance: " + uri + ", " + base + ", " + type);
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
//		System.out.println("Inverse: " + uri + ", " + iuri);
	}

	@Override
	protected void emitObjectProperty(String uri, String base, String domain,
			String range, boolean required, boolean functional) {
//		System.out.println("Object Property: " + uri + ", " + base + ", " +
//				domain + ", " + range + ", " + required + ", " + functional);
	}

	@Override
	protected void emitOntProperty(String uri) {
//		System.out.println("Ont Property: " + uri);
	}

	@Override
	protected void emitOntProperty(String uri, String value) {
//		System.out.println("Ont Property2: " + uri + ", " + value);
	}


	/*
	 * Restriction: http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor.length, http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor, http://www.w3.org/2001/XMLSchema#float
	 * Restriction: http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor.length, http://iec.ch/TC57/2009/CIM-schema-cim14#Conductor, false, true
	 */
	@Override
	protected void emitRestriction(String uri, String domain, String range) {
//		System.out.println("Restriction: " + uri + ", " + domain + ", " + range);
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,
			boolean functional) {
//		System.out.println("Restriction2: " + uri + ", " + domain + ", " +
//				required + ", " + functional);
	}

}
