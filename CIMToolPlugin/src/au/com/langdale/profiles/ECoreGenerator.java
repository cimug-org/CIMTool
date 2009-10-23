package au.com.langdale.profiles;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;

import au.com.langdale.kena.OntModel;

public class ECoreGenerator extends SchemaGenerator {

	private String namespace;

	public ECoreGenerator(OntModel profileModel, OntModel backgroundModel,
			String namespace, boolean preserveNamespaces, boolean inverses) {
		super(profileModel, backgroundModel, namespace, preserveNamespaces, inverses);
		if(namespace != null)
			this.namespace = namespace;
			result.setNsPrefix("cim");
			result.setNsURI(namespace);
	}

	EcoreFactory coreFactory = EcoreFactory.eINSTANCE;

	EPackage result = coreFactory.createEPackage();

	public EPackage getResult() {
		return result;
	}

	Map<String, EModelElement> eModelElements = new HashMap<String, EModelElement>();
	Map<String, EPackage> ePackages = new HashMap<String, EPackage>(); 	// uri to EPackage
	Map<String, EClass> eClasses = new HashMap<String, EClass>(); 	// uri to EClass
	Map<String, EEnumLiteral> eEnumLiterals = new HashMap<String, EEnumLiteral>();

	/*
	 * Adds packages and classifiers without parent packages to the resulting package.
	 */
	@Override
	public void run() {
		super.run();

		for (Iterator<EPackage> i = ePackages.values().iterator(); i.hasNext();) {
			EPackage pkg = i.next();
			if (pkg.getESuperPackage() == null)
				result.getESubpackages().add(pkg);
		}

		for (Iterator<EClass> i = eClasses.values().iterator(); i.hasNext();) {
			EClass klass = i.next();
			if (klass.getEPackage() == null)
				result.getEClassifiers().add(klass);
		}
	}

	@Override
	protected void emitPackage(String uri) {
		EPackage pkg = coreFactory.createEPackage();
		pkg.setNsPrefix("cim");
		pkg.setNsURI(uri);
		ePackages.put(uri, pkg);
	}

	@Override
	protected void emitClass(String uri, String base) {
		EClass klass = coreFactory.createEClass();
		eClasses.put(uri, klass);
	}

	@Override
	protected void emitDefinedBy(String uri, String container) {
		if (ePackages.containsKey(container) && ePackages.containsKey(uri)) {
			EPackage parent = ePackages.get(container);
			EPackage child = ePackages.get(uri);
			parent.getESubpackages().add(child);
		} else if (ePackages.containsKey(container) && eClasses.containsKey(uri)) {
			EPackage parent = ePackages.get(container);
			EClass child = eClasses.get(uri);
			parent.getEClassifiers().add(child);
		} else {
			log("Container [" + container + "] for " + uri + " not found.");
		}
	}

	@Override
	protected void emitDatatype(String uri, String xsdtype) {
//		System.out.println("DataType: " + uri + ", " + xsdtype);
	}

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

	@Override
	protected void emitBaseStereotype(String uri, String stereo) {
		if ((stereo == "http://langdale.com.au/2005/UML#enumeration") && eClasses.containsKey(uri)) {
			EClass klass = eClasses.get(uri);

			EEnum eEnum = coreFactory.createEEnum();
			eEnum.setName(klass.getName());

			Integer j = new Integer(0);

			for (Iterator<EAttribute> ix = klass.getEAttributes().iterator(); ix.hasNext();) {
				EAttribute attr = ix.next();

				EEnumLiteral literal = coreFactory.createEEnumLiteral();
				literal.setName(attr.getName());
				literal.setValue(j);
				eEnum.getELiterals().add(literal);
				j++;
			}
		}
	}

	@Override
	protected void emitInstance(String uri, String base, String type) {
		EEnumLiteral literal = coreFactory.createEEnumLiteral();
		eEnumLiterals.put(uri, literal);
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
	protected void emitRestriction(String uri, String domain, String range) {
//		System.out.println("Restriction: " + uri + ", " + domain + ", " + range);
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,
			boolean functional) {
//		System.out.println("Restriction2: " + uri + ", " + domain + ", " +
//				required + ", " + functional);
	}

	@Override
	protected void emitSuperClass(String subClass, String superClass) {
		if (eClasses.containsKey(subClass) && eClasses.containsKey(superClass)) {
			EClass subEClass = eClasses.get(subClass);
			EClass superEClass = eClasses.get(superClass);
			subEClass.getESuperTypes().add(superEClass);
		} else {
			log("Error setting super type [" + superClass + "] for " + subClass + ".");
		}
	}

	@Override
	protected void emitLabel(String uri, String label) {
		ENamedElement named = null;

		if (ePackages.containsKey(uri)) {
			named = ePackages.get(uri);
		} else if (eClasses.containsKey(uri)) {
			named = eClasses.get(uri);
		} else {
			log("Problem applying [" + uri +"] label: " + label);
		}

		if (named != null)
			named.setName(label);
	}

	@Override
	protected void emitComment(String uri, String baseComment, String profileComment) {
		EModelElement annotated = null;

		if (ePackages.containsKey(uri)) {
			annotated = ePackages.get(uri);
		} else if (eClasses.containsKey(uri)) {
			annotated = eClasses.get(uri);
		} else {
			log("Problem locating annotated element [" + uri + "].");
			return;
		}

		if (baseComment != null) {
			EAnnotation baseAnnotation = coreFactory.createEAnnotation();
			baseAnnotation.setSource(namespace);
			baseAnnotation.getDetails().put("Comment", baseComment);
			annotated.getEAnnotations().add(baseAnnotation);
		}

		if (profileComment != null) {
			EAnnotation profileAnnotation = coreFactory.createEAnnotation();
			profileAnnotation.setSource("http://langdale.com.au/2005/UML");
			profileAnnotation.getDetails().put("Profile Comment", profileComment);
			annotated.getEAnnotations().add(profileAnnotation);
		}
	}

	@Override
	protected void emitOntProperty(String uri) {
		// ignore
	}

	@Override
	protected void emitOntProperty(String uri, String value) {
		// ignore
	}

}
