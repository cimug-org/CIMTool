package au.com.langdale.profiles;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.ecore.*;

import au.com.langdale.kena.OntModel;

public class ECoreGenerator extends SchemaGenerator {

	private String namespace;

	Map<String, String>	xsdTypes = new HashMap<String, String>(); // xsdtype to java
	Map<String, EDataType> eTypes = new HashMap<String, EDataType>(); // xsdtype to ecore

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

		this.eTypes.put("http://www.w3.org/2001/XMLSchema#string", corePackage.getEString());
		this.eTypes.put("http://www.w3.org/2001/XMLSchema#float", corePackage.getEFloat());
		this.eTypes.put("http://www.w3.org/2001/XMLSchema#integer", corePackage.getEInt());
		this.eTypes.put("http://www.w3.org/2001/XMLSchema#boolean", corePackage.getEBoolean());

	}

	EcoreFactory coreFactory = EcoreFactory.eINSTANCE;
	EcorePackage corePackage = EcorePackage.eINSTANCE;

	EPackage result = coreFactory.createEPackage();

	public EPackage getResult() { return result; }

	Map<String, EPackage> ePackages = new HashMap<String, EPackage>(); 	// uri to EPackage
	Map<String, EClass> eClasses = new HashMap<String, EClass>(); 	// uri to EClass
	Map<String, EAttribute> eAttributes = new HashMap<String, EAttribute>();
	Map<String, EReference> eReferences = new HashMap<String, EReference>();
	Map<String, EEnum> eEnums = new HashMap<String, EEnum>();
	Map<String, EDataType> eDataTypes = new HashMap<String, EDataType>();

	/*
	 * Adds packages and classifiers without parent packages to the resulting package.
	 */
	@Override
	public void run() {
		super.run();

		for (Iterator<EPackage> ix = ePackages.values().iterator(); ix.hasNext();) {
			EPackage pkg = ix.next();
			if (pkg.getESuperPackage() == null)
				result.getESubpackages().add(pkg);
		}

		for (Iterator<EClass> ix = eClasses.values().iterator(); ix.hasNext();) {
			EClass klass = ix.next();
			if (klass.getEPackage() == null)
				result.getEClassifiers().add(klass);
		}

		for (Iterator<EEnum> ix = eEnums.values().iterator(); ix.hasNext();) {
			EEnum eEnum= ix.next();
			if (eEnum.getEPackage() == null)
				result.getEClassifiers().add(eEnum);
		}

		for (Iterator<EDataType> ix = eDataTypes.values().iterator(); ix.hasNext();) {
			EDataType dt = ix.next();
			if (dt.getEPackage() == null)
				result.getEClassifiers().add(dt);
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
				log("Problem location contained [" + container + "] element [" + uri + "].");
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

		if (xsdTypes.containsKey(xsdtype)) {
			dt.setInstanceTypeName(xsdTypes.get(xsdtype));
		} else {
			log("Data type [" + xsdtype + "] not found.");
		}

		eDataTypes.put(uri, dt);
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain,
			String type, String xsdtype, boolean required) {
		EAttribute attr = coreFactory.createEAttribute();

		if (eDataTypes.containsKey(type)) {
			EDataType dt = eDataTypes.get(type);
			attr.setEType(dt);
		} else if (eTypes.containsKey(xsdtype)) {
			attr.setEType(eTypes.get(xsdtype));
		} else {
			log("Not EType [" + xsdtype + "] found for " + uri + ".");
		}

		if (required == true) {
			attr.setUpperBound(1);
			attr.setLowerBound(1);
		}

		if (eClasses.containsKey(domain)) {
			EClass klass = eClasses.get(domain);
			klass.getEStructuralFeatures().add(attr);
		} else {
			log("Problem locating class [" + uri + "] for attribute [" + type + "].");
		}

		eAttributes.put(uri, attr);
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
			klass.getEStructuralFeatures().add(attr);
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

			eEnums.put(uri, eEnum); // Substitute the class with the enumeration.
			eClasses.remove(klass);
		}
	}

	@Override
	protected void emitObjectProperty(String uri, String base, String domain,
			String range, boolean required, boolean functional) {

		EReference ref = coreFactory.createEReference();

		if (eClasses.containsKey(domain) && eClasses.containsKey(range)) {
			EClass klass = eClasses.get(domain);
			klass.getEStructuralFeatures().add(ref);

			EClass referenced = eClasses.get(range);
			ref.setEType(referenced);
		} else {
			log("Problem locating classes [" + domain + ", " + range + "] for reference [" + uri + "].");
		}

		if (required == true) {
			ref.setUpperBound(1);
			ref.setLowerBound(1);
		}

		eReferences.put(uri, ref);
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		if (eReferences.containsKey(uri) && eReferences.containsKey(iuri))
			eReferences.get(uri).setEOpposite(eReferences.get(iuri));
	}

	@Override
	protected void emitRestriction(String uri, String domain, String range) {
		// ignore
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,
			boolean functional) {
		// ignore
	}

	@Override
	protected void emitSuperClass(String subClass, String superClass) {
		if (eClasses.containsKey(subClass) && eClasses.containsKey(superClass)) {
			eClasses.get(subClass).getESuperTypes().add(eClasses.get(superClass));
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
		} else if (eAttributes.containsKey(uri)) {
			named = eAttributes.get(uri);
		} else if (eReferences.containsKey(uri)) {
			named = eReferences.get(uri);
		} else if (eEnums.containsKey(uri)) {
			named = eEnums.get(uri);
		} else if (eDataTypes.containsKey(uri)) {
			named = eDataTypes.get(uri);
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
		} else if (eAttributes.containsKey(uri)) {
			annotated = eAttributes.get(uri);
		} else if (eReferences.containsKey(uri)) {
			annotated = eReferences.get(uri);
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
