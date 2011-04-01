/* Copyright (c) 2009 Richard Lincoln */

package com.cimphony.cimtoole.ecore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.profiles.ProfileClass;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.profiles.SchemaGenerator;
import au.com.langdale.xmi.UML;

import com.cimphony.cimtoole.util.CIMToolEcoreUtil;

public class EcoreGenerator extends SchemaGenerator {

	public class Index{
		public Map<String, EPackage> ePackages = new HashMap<String, EPackage>(); 	// uri to EPackage
		public Map<String, EClass> eClasses = new HashMap<String, EClass>(); 	// uri to EClass
		public Map<String, EAttribute> eAttributes = new HashMap<String, EAttribute>();
		public Map<String, EReference> eReferences = new HashMap<String, EReference>();
		public Map<String, EEnum> eEnums = new HashMap<String, EEnum>();
		public Map<String, EDataType> eDataTypes = new HashMap<String, EDataType>();
		public Map<String, EDataType> eTypes = new HashMap<String, EDataType>(); // xsdtype to ecore
		public ArrayList<EReference> notInverted = new ArrayList<EReference>();
		public EPackage root;
	}

	protected String namespace, profileNamespace;
	protected boolean addRootClass;

	public static final String ELEMENT_CLASS_NAME = "Element";
	public static final String ELEMENT_CLASS_IDENTIFIER = "UUID";
	public static final String RDF_SERIALISATION_ANNOTATION = "http://cimphony.com/rdf/2010/serialisation";
	public static final String PROFILE_ANNOTATION = "http://cimphony.com/profiles/2010/profile";
	public static final String UML_NS = UML.NS.substring(0, UML.NS.length()-1);
	
	EcoreFactory coreFactory = EcoreFactory.eINSTANCE;
	EcorePackage corePackage = EcorePackage.eINSTANCE;

	// EPackage result = coreFactory.createEPackage();


	protected Index index;
	protected boolean merged, preserveNamespaces;
	protected String originalNamespace, originalProfileNamespace;
	protected IProject project;

	protected boolean isEcoreSchema(){
		IFolder folder = Task.getSchemaFolder(project);
		try{
			for (IResource res : folder.members()){
				if (res.getName().endsWith(".ecore") || res.getName().endsWith(".ecore-registry"))
					return true;
			}
		}catch (CoreException ex){
			ex.printStackTrace();	
		}
		return false;
	}

	public EcoreGenerator(OntModel profileModel, OntModel backgroundModel,
			String namespace, String profileNamespace, boolean preserveNamespaces, boolean inverses,
			boolean addRootClass, IProject project) {
		this(profileModel, backgroundModel, namespace, profileNamespace, preserveNamespaces, inverses, addRootClass, project, false);
	}

	public EcoreGenerator(OntModel profileModel, OntModel backgroundModel,
			String namespace, String profileNamespace, boolean preserveNamespaces, boolean inverses,
			boolean addRootClass, IProject project, boolean merged) {
		super(profileModel, backgroundModel, preserveNamespaces, inverses);
		this.project = project;
		this.merged = merged;
		this.index = new Index();
		// index.root = result;
		this.originalNamespace = namespace;
		this.originalProfileNamespace = profileNamespace;
		this.addRootClass = addRootClass;
		this.profileNamespace = profileNamespace;
		this.preserveNamespaces = preserveNamespaces;
		this.index.eTypes.putAll(CIMToolEcoreUtil.getEDataTypeMap());

		if (namespace.endsWith("#"))
			namespace = namespace.substring(0, namespace.length()-1);

		if (profileNamespace.endsWith("#"))
			profileNamespace = profileNamespace.substring(0, profileNamespace.length()-1);

		if (namespace!=null && profileNamespace!=null && preserveNamespaces && !merged)
			this.namespace = namespace+"?profile="+profileNamespace;
		else
			this.namespace = namespace;



	}

	public EPackage getResult() {
		return index.root;//result;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void scanProfiles() {
		if (!merged){
			super.scanProfiles();
			return;
		}
		Iterator<?> it = getProfileClasses(profileModel, model, merged);
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

	@Override
	protected boolean scanProperties(ProfileClass profile) {
		if (!merged){
			return super.scanProperties(profile);
		}
		return scanForResource(profile.getBaseClass(), profile);
	}

	@SuppressWarnings("unchecked")
	protected boolean scanForResource(OntResource res, ProfileClass profileClass){
		ResIterator[] iterators = new ResIterator[]{
				profileModel.listObjectProperties(),
				profileModel.listDatatypeProperties()
		};
		boolean some = false;
		for (ResIterator it : iterators){
			some = some | it.hasNext();
			while( it.hasNext()) {
				OntResource next = (OntResource)it.next();
				if (next.getDomain().equals(res)){
					PropertyInfo info = profileClass.getPropertyInfo((OntResource)next);
					ProfileClass range_profile = props.add( info );
					if( range_profile != null)
						work.add(range_profile);
				}else{
					// System.out.println(next.toString() + next.getDomain());
				}

			}
		}
		return some;
	}

	public static Iterator<?> getProfileClasses(final OntModel profileModel, final OntModel fullModel, final boolean merged) {
		return new Iterator<Object>() {
			List<?> classes = ProfileClass.getNamedProfiles(profileModel, fullModel);
			int ix;

			public boolean hasNext() {
				return ix < classes.size();
			}

			public Object next() {
				OntResource clss = (OntResource)classes.get(ix++);
				if (merged)
					return new ProfileClass(clss, clss.getNameSpace(), clss);
				else
					return new ProfileClass(clss, clss.getNameSpace());
			}

			public void remove() {
			}
		};
	}


	/*
	 * Adds packages and classifiers without parent packages to the 'result' package.
	 * Create an Element class from which all other classes derive.
	 */
	@Override
	public void run() {
		super.run();
		EPackage result = null;

		Collection<EPackage> roots = new HashSet<EPackage>();
		for (EPackage p : index.ePackages.values()){
			if (p.getESuperPackage() == null &&
					p.eContents().size()>0){
				roots.add(p);
			}			
		}
		if (roots.size() == 1) result = roots.iterator().next();
		else{
			result = EcoreFactory.eINSTANCE.createEPackage();
			result.getESubpackages().addAll(roots);
		}
		index.root = result;

		if (originalNamespace.endsWith("#")){
			EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
			annotation.setSource(RDF_SERIALISATION_ANNOTATION);
			annotation.getDetails().put("suffix", "#");
			index.root.getEAnnotations().add(annotation);
		}

		if (!originalNamespace.equals(originalProfileNamespace)){
			EAnnotation pAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
			pAnnotation.setSource(PROFILE_ANNOTATION);
			pAnnotation.getDetails().put("nsURI", profileNamespace);
			index.root.getEAnnotations().add(pAnnotation);
		}

		index.root.setNsPrefix("cim");
		index.root.setNsURI(this.namespace);

		Iterator<?> nt = datatypes.iterator();
		while( nt.hasNext()) {
			OntResource type = (OntResource)nt.next();
			EDataType dt = index.eDataTypes.get(type.getURI());
			if (dt!= null && type.getIsDefinedBy() != null){
				EPackage p = index.ePackages.get(type.getIsDefinedBy().getURI());
				if (p!=null)
					p.getEClassifiers().add(dt);
			}
		}		

		/* Create root Element class from which all other classes derive. */
		EClass element = coreFactory.createEClass();
		if (addRootClass) {
			if (index.root.getEClassifier(ELEMENT_CLASS_NAME)!=null && index.root.getEClassifier(ELEMENT_CLASS_NAME) instanceof EClass)
				element = (EClass)index.root.getEClassifier(ELEMENT_CLASS_NAME);
			else
				element.setName(EcoreGenerator.ELEMENT_CLASS_NAME);

			element.setAbstract(true);
			EAttribute uri;
			if (element.getEStructuralFeature(ELEMENT_CLASS_IDENTIFIER) == null ||
					!(element.getEStructuralFeature(ELEMENT_CLASS_IDENTIFIER) instanceof EAttribute)){
				uri = coreFactory.createEAttribute();
				uri.setName(EcoreGenerator.ELEMENT_CLASS_IDENTIFIER);
				uri.setEType(corePackage.getEString());
				element.getEStructuralFeatures().add(uri);
			}else
				uri = (EAttribute)element.getEStructuralFeature(ELEMENT_CLASS_IDENTIFIER);
			uri.setID(true);
			index.root.getEClassifiers().add(element);

		}

		for (Iterator<EClass> ix = index.eClasses.values().iterator(); ix.hasNext();) {
			EClass klass = ix.next();
			if (klass.getEPackage() == null)
				index.root.getEClassifiers().add(klass);
			/* Make all classes derive from Element. */
			if (addRootClass && 
					(klass.getESuperTypes().size() == 0) &&
					klass!=element &&
					!isCompound(klass)) {
				klass.getESuperTypes().add(element);
			}
			for (EReference ref : klass.getEReferences()){
				if (ref.getName()==null){
					String name = ref.getEType().getName();
					if (ref.isMany()) name+="s";
					log("Reference between "+klass.getName()+" and "+ ref.getEType().getName()+" has no role name, setting to "+name);
					ref.setName(name);
				}
			}
		}

		for (Iterator<EEnum> ix = index.eEnums.values().iterator(); ix.hasNext();) {
			EEnum eEnum= ix.next();
			if (eEnum.getEPackage() == null)
				index.root.getEClassifiers().add(eEnum);
		}

		for (Iterator<EDataType> ix = index.eDataTypes.values().iterator(); ix.hasNext();) {
			EDataType dt = ix.next();
			if (dt.getEPackage() == null)
				index.root.getEClassifiers().add(dt);
		}

		for (Iterator<EDataType> ix = index.eTypes.values().iterator(); ix.hasNext();) {
			EDataType dt = ix.next();
			if (dt.getEPackage() == null)
				index.root.getEClassifiers().add(dt);
		}

		for (Iterator<EPackage> ix = index.ePackages.values().iterator(); ix.hasNext();) {
			EPackage pkg = ix.next();
			if (pkg.getESuperPackage() == null && pkg != index.root){
				index.root.getESubpackages().add(pkg);
			}
		}
		if (!isEcoreSchema()){
			for (Iterator<EPackage> ix = index.ePackages.values().iterator(); ix.hasNext();) {
				EPackage pkg = ix.next();			
				if (pkg.getESuperPackage() == index.root && index.root.getESubpackages().size()==1 && pkg.getESubpackages().size()==1){
					index.root.getESubpackages().addAll(pkg.getESubpackages());
					index.root.getEClassifiers().addAll(pkg.getEClassifiers());
					index.root.getEAnnotations().addAll(pkg.getEAnnotations());
					if (index.root.getName() == null)
						index.root.setName(pkg.getName());
					index.root.getESubpackages().remove(pkg);
				}
			}
		}


		for (Iterator<EReference> ix = index.notInverted.iterator(); ix.hasNext();) {
			EReference ref = ix.next();
			if (!isCompound((EClass)ref.getEType())) log("Non-inverted reference: " + ref.getName());
		}
	}

	@Override
	protected void emitPackage(String uri) {
		if (!uri.equals(UML.global_package.getURI())){
			EPackage pkg = coreFactory.createEPackage();
			index.ePackages.put(uri, pkg);
		}
	}

	@Override
	protected void emitClass(String uri, String base) {
		EClass klass = coreFactory.createEClass();
		// Assume abstract unless 'concrete' stereotype emitted.
		if (!merged)
			klass.setAbstract(true);
		index.eClasses.put(uri, klass);
	}

	@Override
	protected void emitDefinedBy(String uri, String container) {
		if (index.ePackages.containsKey(container)) {
			EPackage parent = index.ePackages.get(container);

			if (index.ePackages.containsKey(uri)) {
				EPackage child = index.ePackages.get(uri);
				parent.getESubpackages().add(child);
			} else if (index.eClasses.containsKey(uri)) {
				EClass child = index.eClasses.get(uri);
				parent.getEClassifiers().add(child);
			} else if (index.eDataTypes.containsKey(uri)) {
				EDataType child = index.eDataTypes.get(uri);
				parent.getEClassifiers().add(child);
			} else if (index.eEnums.containsKey(uri)) {
				EEnum child = index.eEnums.get(uri);
				parent.getEClassifiers().add(child);
			} else {
				log("Problem location contained [" + container + "] element [" + uri + "].");
			}
		} else if (!container.equals(UML.global_package.getURI())){
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

		if (CIMToolEcoreUtil.getTypeClass(xsdtype)!=null) {
			dt.setInstanceTypeName(CIMToolEcoreUtil.getTypeClass(xsdtype).toString());
			dt.setInstanceClass(CIMToolEcoreUtil.getTypeClass(xsdtype));
		} else {
			log("Data type [" + xsdtype + "] not found.");
			dt.setInstanceClass(Object.class);

		}

		index.eDataTypes.put(uri, dt);
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain,
			String type, String xsdtype, boolean required) {
		EAttribute attr = coreFactory.createEAttribute();
		if (index.eDataTypes.containsKey(type)) {
			EDataType dt = index.eDataTypes.get(type);
			attr.setEType(dt);
		} else if (index.eTypes.containsKey(xsdtype)) {
			attr.setEType(index.eTypes.get(xsdtype));
		} else {
			log("No EType [" + xsdtype + "] found for " + uri + ".");
		}

		if (!merged && required == true)
			attr.setLowerBound(1);
		
		attr.setUpperBound(1);
		attr.setUnsettable(true);

		if (index.eClasses.containsKey(domain)) {
			EClass klass = index.eClasses.get(domain);
			klass.getEStructuralFeatures().add(attr);
		} else {
			log("Problem locating class [" + uri + "] for attribute [" + type + "].");
		}

		index.eAttributes.put(uri, attr);
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
		if (index.eClasses.containsKey(uri)) {
			EClass klass = index.eClasses.get(uri);
			if (stereo.equals(UML.concrete.toString())) {
				klass.setAbstract(false);
			}
		} else {
			log("Problem locating stereotype [" + stereo + "] class [" + uri + "].");
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
		if (index.eClasses.containsKey(type)) {
			EClass klass = index.eClasses.get(type);
			EAttribute attr = coreFactory.createEAttribute();
			klass.getEStructuralFeatures().add(attr);
			index.eAttributes.put(uri, attr);
		} else {
			log("Problem locating class [" + type + "] for instance [" + uri + "]");
		}
	}

	@Override
	protected void emitBaseStereotype(String uri, String stereo) {
		// Convert classes with enumeration base sterotypes to EEnums.
		if ((stereo.equals(UML.enumeration.toString())) && index.eClasses.containsKey(uri)) {
			EClass klass = index.eClasses.get(uri);

			EEnum eEnum = coreFactory.createEEnum();
			eEnum.setName(klass.getName());

			Integer j = new Integer(0);

			for (Iterator<EAttribute> ix = klass.getEAttributes().iterator(); ix.hasNext();) {
				EAttribute attr = ix.next();

				EEnumLiteral literal = coreFactory.createEEnumLiteral();
				literal.setName(attr.getName());
				literal.setValue(j);
				eEnum.getELiterals().add(literal);
				index.eAttributes.remove(uri + "." + attr.getName());
				j++;
			}

			index.eEnums.put(uri, eEnum); // Substitute the class with the enumeration.
			index.eClasses.remove(uri);

		} else if (stereo.equals(UML.compound.toString())) {
			if (index.eClasses.containsKey(uri)){
				System.out.println(uri +" is compound");
				EClass cls = index.eClasses.get(uri);
				if (cls.getEAnnotation(UML_NS) == null){
					EAnnotation profileAnnotation = coreFactory.createEAnnotation();
					profileAnnotation.setSource(UML_NS);
					cls.getEAnnotations().add(profileAnnotation);
				}
				cls.getEAnnotation(UML_NS).getDetails().put("CIMDataType", "Compound");
			}
			//			ref.setContainment(true);
		}
	}
	
	protected boolean isCompound(EClass cls){
		if (cls.getEAnnotation(UML_NS)==null) return false;
		if (cls.getEAnnotation(UML_NS).getDetails().get("CIMDataType")==null) return false;
		if (cls.getEAnnotation(UML_NS).getDetails().get("CIMDataType").equals("Compound")) return true;
		return false;
		
	}

	@Override
	protected void emitObjectProperty(String uri, String base, String domain,
			String range, boolean required, boolean functional) {
		if (index.eClasses.containsKey(domain) && index.eClasses.containsKey(range)) {
			EReference ref = coreFactory.createEReference();
			EClass klass = index.eClasses.get(domain);
			klass.getEStructuralFeatures().add(ref);

			EClass referenced = index.eClasses.get(range);
			ref.setEType(referenced);

			if (!merged && required == true)
				ref.setLowerBound(1);

			if (functional == false)
				ref.setUpperBound(-1);
			if (isCompound(referenced))
				ref.setContainment(true);
			
			index.eReferences.put(uri, ref);
		} else if (index.eClasses.containsKey(domain) && index.eEnums.containsKey(range)) {
			EAttribute attr = coreFactory.createEAttribute();
			EClass klass = index.eClasses.get(domain);
			klass.getEStructuralFeatures().add(attr);

			EEnum eEnum = index.eEnums.get(range);
			attr.setEType(eEnum);
			attr.setUnsettable(true);

			if (!merged && required == true)
				attr.setLowerBound(1);

			index.eAttributes.put(uri, attr);
		} else {
			log("Problem locating classes [" + domain + ", " + range + "] for reference [" + uri + "].");
		}
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		if (index.eReferences.containsKey(uri) && index.eReferences.containsKey(iuri)) {
			index.eReferences.get(uri).setEOpposite(index.eReferences.get(iuri));
			index.eReferences.get(iuri).setEOpposite(index.eReferences.get(uri));
			index.notInverted.remove(index.eReferences.get(uri));
			index.notInverted.remove(index.eReferences.get(iuri));
		} else if (index.eReferences.containsKey(uri)) {
			index.notInverted.add(index.eReferences.get(uri));
		} else if (index.eReferences.containsKey(iuri)) {
			index.notInverted.add(index.eReferences.get(iuri));
		} else {
			log("Problem inverting " + uri + " and " + iuri + ".");
		}
	}

	@Override
	protected void emitRestriction(String uri, String domain, String range) {
		// Do nothing
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,
			boolean functional) {
		//Do nothing
	}

	@Override
	protected void emitSuperClass(String subClass, String superClass) {
		if (index.eClasses.containsKey(subClass) && index.eClasses.containsKey(superClass)) {
			index.eClasses.get(subClass).getESuperTypes().add(index.eClasses.get(superClass));
		} else {
			log("Error setting super type [" + superClass + "] for " + subClass + ".");
		}
	}

	@Override
	protected void emitLabel(String uri, String label) {
		ENamedElement named = null;

		if (index.ePackages.containsKey(uri)) {
			named = index.ePackages.get(uri);
			EPackage pkg = (EPackage)named;
			if (namespace.endsWith("#"))
				pkg.setNsURI(namespace+label);
			else
				pkg.setNsURI(namespace+"#"+label);
			pkg.setNsPrefix("cim"+label);
		} else if (index.eClasses.containsKey(uri)) {
			named = index.eClasses.get(uri);
		} else if (index.eAttributes.containsKey(uri)) {
			named = index.eAttributes.get(uri);
		} else if (index.eReferences.containsKey(uri)) {
			named = index.eReferences.get(uri);
		} else if (index.eEnums.containsKey(uri)) {
			named = index.eEnums.get(uri);
		} else if (index.eDataTypes.containsKey(uri)) {
			named = index.eDataTypes.get(uri);
		} else {
			log("Problem applying [" + uri +"] label: " + label);
		}

		if (named != null)
			named.setName(label);
	}

	@Override
	protected void emitComment(String uri, String baseComment, String profileComment) {
		EModelElement annotated = null;

		if (index.ePackages.containsKey(uri)) {
			annotated = index.ePackages.get(uri);
		} else if (index.eClasses.containsKey(uri)) {
			annotated = index.eClasses.get(uri);
		} else if (index.eAttributes.containsKey(uri)) {
			annotated = index.eAttributes.get(uri);
		} else if (index.eReferences.containsKey(uri)) {
			annotated = index.eReferences.get(uri);
		} else if (index.eDataTypes.containsKey(uri)) {
			annotated = index.eDataTypes.get(uri);
		} else if (index.eEnums.containsKey(uri)) {
			annotated = index.eEnums.get(uri);
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

	public Index getIndex(){
		return index;
	}
}
