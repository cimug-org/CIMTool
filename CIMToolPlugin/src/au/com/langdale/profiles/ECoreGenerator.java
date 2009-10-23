package au.com.langdale.profiles;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.ecore.*;

import au.com.langdale.kena.OntModel;

public class ECoreGenerator extends SchemaGenerator {

	private String namespace;

	Map<String, String>	xsdTypes = new HashMap<String, String>(); // xsdtype to java

	public ECoreGenerator(OntModel profileModel, OntModel backgroundModel,
			String namespace, boolean preserveNamespaces, boolean inverses) {
		super(profileModel, backgroundModel, namespace, preserveNamespaces, inverses);

		if(namespace != null)
			this.namespace = namespace;
			result.setNsPrefix("cim");
			result.setNsURI(namespace);

		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#string", "java.lang.String");
		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#float", "double");
		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#integer", "int");
		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#boolean", "boolean");
	}

	EcoreFactory coreFactory = EcoreFactory.eINSTANCE;

	EPackage result = coreFactory.createEPackage();

	public EPackage getResult() { return result; }

	Map<String, EModelElement> eModelElements = new HashMap<String, EModelElement>();
	Map<String, EPackage> ePackages = new HashMap<String, EPackage>(); 	// uri to EPackage
	Map<String, EClass> eClasses = new HashMap<String, EClass>(); 	// uri to EClass
	Map<String, EEnum> eEnums = new HashMap<String, EEnum>();
	Map<String, EDataType> eDataTypes = new HashMap<String, EDataType>();

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
		eClasses.put(uri, coreFactory.createEClass());
	}

	@Override
	protected void emitDefinedBy(String uri, String container) {
		if (ePackages.containsKey(container)) {
			EPackage parent = ePackages.get(container);

			if (ePackages.containsKey(uri)) {
				EPackage child = ePackages.get(uri);
				parent.getESubpackages().add(child);
			} else if (eClasses.containsKey(uri)) {
				EClass child = eClasses.get(uri);
				parent.getEClassifiers().add(child);
			} else if (eDataTypes.containsKey(uri)) {
				EDataType child = eDataTypes.get(uri);
				parent.getEClassifiers().add(child);
			} else if (eEnums.containsKey(uri)) {
				EEnum child = eEnums.get(uri);
				parent.getEClassifiers().add(child);
			} else {
				log("Problem location container [" + container + "] element [" + uri + "].");
			}
		} else {
			log("Container [" + container + "] for " + uri + " not found.");
		}
	}

	@Override
	protected void emitDatatype(String uri, String xsdtype) {
		EDataType dt = coreFactory.createEDataType();

		EAnnotation profileAnnotation = coreFactory.createEAnnotation();
		profileAnnotation.setSource("http:///org/eclipse/emf/ecore/util/ExtendedMetaData");
		profileAnnotation.getDetails().put("baseType", xsdtype);
		dt.getEAnnotations().add(profileAnnotation);

		if (xsdTypes.containsKey(xsdtype))
			dt.setInstanceTypeName(xsdTypes.get(xsdtype));

		eDataTypes.put(uri, dt);
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain,
			String type, String xsdtype, boolean required) {
		EAttribute attr = coreFactory.createEAttribute();

		if (eDataTypes.containsKey(type)) {
			EDataType dt = eDataTypes.get(type);
			attr.setEType(dt);
		}

		if (required == true) {
			attr.setUpperBound(1);
			attr.setLowerBound(1);
		}

		if (eClasses.containsKey(uri)) {
			EClass klass = eClasses.get(uri);
			klass.getEAttributes().add(attr);
		} else {
			log("Problem locating class [" + uri + "] for attribute [" + type + "].");
		}
	}

	/*
	 * http://iec.ch/TC57/2009/CIM-schema-cim14#VoltageLevel, http://langdale.com.au/2005/UML#ofAggregate
	 * http://iec.ch/TC57/2009/CIM-schema-cim14#VoltageLevel, http://langdale.com.au/2005/UML#byreference
     * http://iec.ch/TC57/2009/CIM-schema-cim14#VoltageLevel, http://langdale.com.au/2005/UML#concrete
	 */
	@Override
	protected void emitStereotype(String uri, String stereo) {
		if (eClasses.containsKey(uri)) {
			// EClass klass = eClasses.get(uri);
		} else {
			log("Problem locating stereotype [" + stereo + "] class [" + uri + "].");
		}
	}

	/*
	 * Enumerations are emitted as classes and must be converted to EEnums when
	 * the base stereotype is emitted.  Instances for the enumeration get stored
	 * as attributes of the class before being converted to EEnumLiterals.
	 */
	@Override
	protected void emitInstance(String uri, String base, String type) {
		if (eClasses.containsKey(type)) {
			EClass klass = eClasses.get(type);
			EAttribute attr = coreFactory.createEAttribute();
			klass.getEAttributes().add(attr);
		} else {
			log("Problem locating class [" + type + "] for instance [" + uri + "]");
		}
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

			eEnums.put(uri, eEnum);
			eClasses.remove(klass); // Remove the temporary class.
		}
	}

	@Override
	protected void emitObjectProperty(String uri, String base, String domain,
			String range, boolean required, boolean functional) {

		EReference ref = coreFactory.createEReference();

		if (eClasses.containsKey(uri) && eClasses.containsKey(domain)) {
			EClass klass = eClasses.get(uri);
			klass.getEReferences().add(ref);
			EClass referenced = eClasses.get(domain);
			ref.setEType(referenced);
		} else {
			log("Problem locating classes [" + domain +", " + range + "] for reference [" + uri + "].");
		}

		if (required == true) {
			ref.setUpperBound(1);
			ref.setLowerBound(1);
		}
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
//		System.out.println("Inverse: " + uri + ", " + iuri);
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
		} else if (eDataTypes.containsKey(uri)) {
			annotated = eDataTypes.get(uri);
		} else if (eEnums.containsKey(uri)) {
			annotated = eEnums.get(uri);
		} else {
			log("Problem locating annotated element [" + uri + "].");
		}

		if (annotated != null) {
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
