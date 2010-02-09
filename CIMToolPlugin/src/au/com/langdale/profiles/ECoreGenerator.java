/* Copyright (c) 2009 Richard Lincoln */

package au.com.langdale.profiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.ecore.*;

import au.com.langdale.kena.OntModel;

public class ECoreGenerator extends SchemaGenerator {

	private String namespace;
	private boolean addRootClass;

	public static final String ELEMENT_CLASS_NAME = "Element";
	public static final String ELEMENT_CLASS_IDENTIFIER = "UUID";
	public static final String RDF_SERIALISATION_ANNOTATION = "http://cimphony.com/rdf/2010/serialisation";
	public static final String PROFILE_ANNOTATION = "http://cimphony.com/profiles/2010/profile";

	EcoreFactory coreFactory = EcoreFactory.eINSTANCE;
	EcorePackage corePackage = EcorePackage.eINSTANCE;

	EPackage result = coreFactory.createEPackage();

	Map<String, String>	xsdTypes = new HashMap<String, String>(); // xsdtype to java
	Map<String, EDataType> eTypes = new HashMap<String, EDataType>(); // xsdtype to ecore

	Map<String, EPackage> ePackages = new HashMap<String, EPackage>(); 	// uri to EPackage
	Map<String, EClass> eClasses = new HashMap<String, EClass>(); 	// uri to EClass
	Map<String, EAttribute> eAttributes = new HashMap<String, EAttribute>();
	Map<String, EReference> eReferences = new HashMap<String, EReference>();
	Map<String, EEnum> eEnums = new HashMap<String, EEnum>();
	Map<String, EDataType> eDataTypes = new HashMap<String, EDataType>();

	ArrayList<EReference> notInverted = new ArrayList<EReference>();

	public ECoreGenerator(OntModel profileModel, OntModel backgroundModel,
			String namespace, String profileNamespace, boolean preserveNamespaces, boolean inverses,
			boolean addRootClass) {
		super(profileModel, backgroundModel, preserveNamespaces, inverses);

		if (!namespace.equals(profileNamespace)){
			EAnnotation pAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
			pAnnotation.setSource(PROFILE_ANNOTATION);
			if (profileNamespace.endsWith("#"))
				profileNamespace = profileNamespace.substring(0, profileNamespace.length()-1);
			pAnnotation.getDetails().put("nsURI", profileNamespace);
			result.getEAnnotations().add(pAnnotation);
		}


		this.addRootClass = addRootClass;

		if(namespace != null){
			if (namespace.endsWith("#")){
				namespace = namespace.substring(0, namespace.length()-1);
				EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
				annotation.setSource(RDF_SERIALISATION_ANNOTATION);
				annotation.getDetails().put("suffix", "#");
				result.getEAnnotations().add(annotation);

			}
			this.namespace = namespace;
		}
		if (namespace!=null && profileNamespace!=null && !namespace.equals(preserveNamespaces))
			this.namespace = namespace+"?profile="+profileNamespace;
		
		result.setNsPrefix("cim");
		// TODO: Need a nice option pane to set whether we do this or not
		result.setNsURI(this.namespace);

		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#string", "java.lang.String");
		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#float", "double");
		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#integer", "int");
		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#int", "int");
		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#boolean", "boolean");
		this.xsdTypes.put("http://www.w3.org/2001/XMLSchema#dateTime", "java.util.Date");

		this.eTypes.put("http://www.w3.org/2001/XMLSchema#string", corePackage.getEString());
		this.eTypes.put("http://www.w3.org/2001/XMLSchema#float", corePackage.getEFloat());
		this.eTypes.put("http://www.w3.org/2001/XMLSchema#integer", corePackage.getEInt());
		this.eTypes.put("http://www.w3.org/2001/XMLSchema#int", corePackage.getEInt());
		this.eTypes.put("http://www.w3.org/2001/XMLSchema#boolean", corePackage.getEBoolean());
		this.eTypes.put("http://www.w3.org/2001/XMLSchema#dateTime", corePackage.getEDate());
	}

	public EPackage getResult() {
		return result;
	}

	/*
	 * Adds packages and classifiers without parent packages to the 'result' package.
	 * Create an Element class from which all other classes derive.
	 */
	@Override
	public void run() {
		super.run();

		/* Create root Element class from which all other classes derive. */
		EClass element = coreFactory.createEClass();
		if (addRootClass) {
			element.setName(ECoreGenerator.ELEMENT_CLASS_NAME);
			element.setAbstract(true);
			EAttribute uri = coreFactory.createEAttribute();
			uri.setName(ECoreGenerator.ELEMENT_CLASS_IDENTIFIER);
			uri.setEType(corePackage.getEString());
			uri.setID(true);
			element.getEStructuralFeatures().add(uri);
			result.getEClassifiers().add(element);

		}

		for (Iterator<EPackage> ix = ePackages.values().iterator(); ix.hasNext();) {
			EPackage pkg = ix.next();
			if (pkg.getESuperPackage() == null)
				result.getESubpackages().add(pkg);
		}

		for (Iterator<EClass> ix = eClasses.values().iterator(); ix.hasNext();) {
			EClass klass = ix.next();
			if (klass.getEPackage() == null)
				result.getEClassifiers().add(klass);
			/* Make all classes derive from Element. */
			if (addRootClass && (klass.getESuperTypes().size() == 0)) {
				klass.getESuperTypes().add(element);
			}
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

		for (Iterator<EReference> ix = notInverted.iterator(); ix.hasNext();) {
			EReference ref = ix.next();
			log("Non-inverted reference: " + ref.getName());
		}
	}

	@Override
	protected void emitPackage(String uri) {
		EPackage pkg = coreFactory.createEPackage();
		ePackages.put(uri, pkg);
	}

	@Override
	protected void emitClass(String uri, String base) {
		EClass klass = coreFactory.createEClass();
		// Assume abstract unless 'concrete' stereotype emitted.
		klass.setAbstract(true);
		eClasses.put(uri, klass);
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
			dt.setInstanceClass(Object.class);
			
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
			log("No EType [" + xsdtype + "] found for " + uri + ".");
		}

		if (required == true)
			attr.setUpperBound(1);
		attr.setLowerBound(1);

		if (eClasses.containsKey(domain)) {
			EClass klass = eClasses.get(domain);
			klass.getEStructuralFeatures().add(attr);
		} else {
			log("Problem locating class [" + uri + "] for attribute [" + type + "].");
		}

		eAttributes.put(uri, attr);
	}

	/*
	 * Aggregation (has a).
	 * http://iec.ch/TC57/2009/CIM-schema-cim14#VoltageLevel, http://langdale.com.au/2005/UML#ofAggregate
	 *
	 * Composition (owns a).  If the container is destroyed, normally every instance that it contains is destroyed as well.
	 * http://langdale.com.au/2005/UML#compositeOf
	 *
	 * Changes the element from a nested structure to a reference.
	 * http://iec.ch/TC57/2009/CIM-schema-cim14#VoltageLevel, http://langdale.com.au/2005/UML#byreference
	 *
	 * Normally, any structured class that has no subclasses would be marked concrete.
	 * http://iec.ch/TC57/2009/CIM-schema-cim14#VoltageLevel, http://langdale.com.au/2005/UML#concrete
	 */
	@Override
	protected void emitStereotype(String uri, String stereo) {
		if (eClasses.containsKey(uri)) {
			EClass klass = eClasses.get(uri);
			if (stereo == "http://langdale.com.au/2005/UML#concrete") {
				klass.setAbstract(false);
			}
		} else {
			//			log("Problem locating stereotype [" + stereo + "] class [" + uri + "].");
		}
	}

	/*
	 * Enumerations are emitted as classes and must be converted to EEnums when
	 * the base stereotype is emitted.  Instances for the enumeration get stored
	 * as attributes of the class before being converted to EEnumLiterals.  The
	 * EAttributes are also stored in the list of all attributes for labelling.
	 */
	@Override
	protected void emitInstance(String uri, String base, String type) {
		if (eClasses.containsKey(type)) {
			EClass klass = eClasses.get(type);
			EAttribute attr = coreFactory.createEAttribute();
			klass.getEStructuralFeatures().add(attr);
			eAttributes.put(uri, attr);
		} else {
			log("Problem locating class [" + type + "] for instance [" + uri + "]");
		}
	}

	@Override
	protected void emitBaseStereotype(String uri, String stereo) {
		// Convert classes with enumeration base sterotypes to EEnums.
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
				eAttributes.remove(uri + "." + attr.getName());
				j++;
			}

			eEnums.put(uri, eEnum); // Substitute the class with the enumeration.
			eClasses.remove(uri);

		} else if ((stereo == "http://langdale.com.au/2005/UML#compositeOf") && eReferences.containsKey(uri)) {
			//			EReference ref = eReferences.get(uri);
			//			ref.setContainment(true);
		}
	}

	@Override
	protected void emitObjectProperty(String uri, String base, String domain,
			String range, boolean required, boolean functional) {

		if (eClasses.containsKey(domain) && eClasses.containsKey(range)) {
			EReference ref = coreFactory.createEReference();
			EClass klass = eClasses.get(domain);
			klass.getEStructuralFeatures().add(ref);

			EClass referenced = eClasses.get(range);
			ref.setEType(referenced);

			if (required == true)
				ref.setLowerBound(1);

			if (functional == false)
				ref.setUpperBound(-1);

			eReferences.put(uri, ref);
		} else if (eClasses.containsKey(domain) && eEnums.containsKey(range)) {
			EAttribute attr = coreFactory.createEAttribute();
			EClass klass = eClasses.get(domain);
			klass.getEStructuralFeatures().add(attr);

			EEnum eEnum = eEnums.get(range);
			attr.setEType(eEnum);

			if (required == true)
				attr.setLowerBound(1);

			eAttributes.put(uri, attr);
		} else {
			log("Problem locating classes [" + domain + ", " + range + "] for reference [" + uri + "].");
		}
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		if (eReferences.containsKey(uri) && eReferences.containsKey(iuri)) {
			eReferences.get(uri).setEOpposite(eReferences.get(iuri));
			eReferences.get(iuri).setEOpposite(eReferences.get(uri));
			notInverted.remove(eReferences.get(uri));
			notInverted.remove(eReferences.get(iuri));
		} else if (eReferences.containsKey(uri)) {
			notInverted.add(eReferences.get(uri));
		} else if (eReferences.containsKey(iuri)) {
			notInverted.add(eReferences.get(iuri));
		} else {
			log("Problem inverting " + uri + " and " + iuri + ".");
		}
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
			EPackage pkg = (EPackage)named;
			pkg.setNsURI(namespace+"#"+label);
			pkg.setNsPrefix("cim"+label);
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
			if ((baseComment != null) || (profileComment != null)) {
				/* Annotations with GenModel source are added to EMF generated code. */
				EAnnotation genModelAnnotation = coreFactory.createEAnnotation();
				genModelAnnotation.setSource("http://www.eclipse.org/emf/2002/GenModel");

				if (baseComment != null) {
					EAnnotation baseAnnotation = coreFactory.createEAnnotation();
					baseAnnotation.setSource(namespace);
					baseAnnotation.getDetails().put("Documentation", baseComment);
					annotated.getEAnnotations().add(baseAnnotation);

					genModelAnnotation.getDetails().put("Documentation", baseComment);
				}

				if (profileComment != null) {
					EAnnotation profileAnnotation = coreFactory.createEAnnotation();
					profileAnnotation.setSource("http://langdale.com.au/2005/UML");
					profileAnnotation.getDetails().put("Profile documentation", profileComment);
					annotated.getEAnnotations().add(profileAnnotation);

					genModelAnnotation.getDetails().put("Profile documentation", profileComment);
				}

				annotated.getEAnnotations().add(genModelAnnotation);
			}
		}
	}

	@Override
	protected void emitHeader(String uri, String label, String comment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void emitFlag(String uri) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void emitImport(String uri) {
		// TODO Auto-generated method stub
		
	}
}
