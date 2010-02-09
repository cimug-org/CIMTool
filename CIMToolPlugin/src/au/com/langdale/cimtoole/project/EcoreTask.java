package au.com.langdale.cimtoole.project;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.profiles.ECoreGenerator;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class EcoreTask {

	Map<OntResource, EClassifier> classMap = new HashMap<OntResource, EClassifier>();
	Map<OntResource, EPackage> packageMap = new HashMap<OntResource, EPackage>();
	Map<OntResource, EReference> referenceMap = new HashMap<OntResource, EReference>();
	Map<com.hp.hpl.jena.rdf.model.Resource, EDataType> basicTypeMap = new HashMap<com.hp.hpl.jena.rdf.model.Resource, EDataType>();
	EPackage rootPackage;
	OntModel model;
	String nsURI;
	String nsPrefix;

	public EcoreTask(OntModel model){
		this.model = model;

		basicTypeMap.put(XSD.date, EcorePackage.eINSTANCE.getEDate());
		basicTypeMap.put(XSD.dateTime, EcorePackage.eINSTANCE.getEDate());
		basicTypeMap.put(XSD.integer, EcorePackage.eINSTANCE.getEInt());
		basicTypeMap.put(XSD.xint, EcorePackage.eINSTANCE.getEInt());
		basicTypeMap.put(XSD.xfloat, EcorePackage.eINSTANCE.getEFloat());
		basicTypeMap.put(XSD.xdouble, EcorePackage.eINSTANCE.getEDouble());
		basicTypeMap.put(XSD.xbyte, EcorePackage.eINSTANCE.getEByte());
		basicTypeMap.put(XSD.xlong, EcorePackage.eINSTANCE.getELong());
		basicTypeMap.put(XSD.xshort, EcorePackage.eINSTANCE.getEShort());
		basicTypeMap.put(XSD.xstring, EcorePackage.eINSTANCE.getEString());
		basicTypeMap.put(XSD.xboolean, EcorePackage.eINSTANCE.getEBoolean());
	}

	private EPackage getEPackage(OntResource p){
		if (!packageMap.containsKey(p)){
			if (p.equals(UML.global_package)){
				String namespace = p.getNameSpace();
				if (namespace.endsWith("#")) namespace = namespace.substring(0, namespace.length()-1);
				rootPackage.setNsURI(namespace);
				packageMap.put(p, rootPackage);
			}else{
				EPackage npkg = EcoreFactory.eINSTANCE.createEPackage();
				npkg.setName(p.getLabel());
				npkg.setNsURI(nsURI+"#"+p.getLabel());
				npkg.setNsPrefix(nsPrefix+"."+p.getLabel().toLowerCase());
				packageMap.put(p, npkg);
				if (p.getIsDefinedBy()!=null){
					EPackage parent = getEPackage(p.getIsDefinedBy());
					parent.getESubpackages().add(npkg);
				}else{
					rootPackage.getESubpackages().add(npkg);
				}
				if (p.getComment()!=null){
					addComment(npkg, p.getComment());
				}

			}
		}
		return packageMap.get(p);

	}

	private boolean isEnum(OntResource c){
		ResIterator resIt = c.listProperties(UML.hasStereotype);
		while (resIt.hasNext()){
			if (resIt.next().equals(UML.enumeration)) return true;
		}
		return false;
	}

	private boolean isDataType(OntResource c){
		ResIterator resIt = c.listProperties(UML.hasStereotype);
		while (resIt.hasNext()){
			if (resIt.next().equals(UML.datatype)) return true;
		}
		return false;
	}

	private OntResource getSameAs(OntResource d){
		ResIterator resIt = d.listProperties(OWL.sameAs);
		if (resIt.hasNext())
			return resIt.nextResource();
		return null;

	}

	private Collection<EClass> getIndirectSuperEClasses(OntResource c){
		ResIterator sc = c.listProperties(RDFS.subClassOf);
		Collection<EClass> supers = new HashSet<EClass>(); 
		while (sc.hasNext()){
			supers.add((EClass)getEClass((OntResource)sc.next()));					
		}
		return supers;
	}

	private Collection<OntResource> getIndirectSuperOntResources(OntResource c){
		ResIterator sc = c.listProperties(RDFS.subClassOf);
		Collection<OntResource> supers = new HashSet<OntResource>(); 
		while (sc.hasNext()){
			supers.add((OntResource)sc.next());					
		}
		return supers;
	}

	private Collection<EClass> getDirectSuperEClasses(OntResource c){
		Collection<EClass> eSuper = getIndirectSuperEClasses(c);
		Collection<OntResource> oSuper = getIndirectSuperOntResources(c);

		Collection<EClass> remove = new Vector<EClass>();
		for (OntResource o: oSuper){
			for (EClass oc: getIndirectSuperEClasses(o)){
				if (eSuper.contains(oc)) remove.add(oc);
			}
		}
		eSuper.removeAll(remove);
		return eSuper;
	}

	private EClassifier getEClass(OntResource c){
		if (!classMap.containsKey(c) && !basicTypeMap.containsKey(c)){
			EClassifier cls;
			if (isEnum(c)){
				cls = EcoreFactory.eINSTANCE.createEEnum();
				ResIterator resIt = c.listInstances();
				while (resIt.hasNext()){
					OntResource r = resIt.nextResource();
					r.getLocalName();
					EEnumLiteral lit = EcoreFactory.eINSTANCE.createEEnumLiteral();
					String litName = r.getLabel().replaceAll("[^a-zA-Z0-9\\_]", "_");
					lit.setLiteral(r.getLabel());
					lit.setName(litName);
					lit.setValue(((EEnum)cls).getELiterals().size()+1);
					((EEnum)cls).getELiterals().add(lit);
				}
				/* Ecore doesn't like EEnum literals of the same name and different cases */

				for (EEnumLiteral lit : ((EEnum)cls).getELiterals()){
					boolean duplicateFound = false;

					for (EEnumLiteral l: ((EEnum)cls).getELiterals()){
						if (lit!=l && lit.getName().toLowerCase().equals(l.getName().toLowerCase())){
							duplicateFound = true;
							break;
						}
					}

					if (duplicateFound)
						lit.setName(lit.getName()+"0");
				}

			}else if (isDataType(c)){
				cls = EcoreFactory.eINSTANCE.createEDataType();
				if (getSameAs(c)!=null && basicTypeMap.containsKey(getSameAs(c))){
					((EDataType)cls).setInstanceClass(((EDataType)getEClass(getSameAs(c))).getInstanceClass());
					EAnnotation profileAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
					profileAnnotation.setSource("http:///org/eclipse/emf/ecore/util/ExtendedMetaData");
					profileAnnotation.getDetails().put("baseType", getSameAs(c).getURI());
					cls.getEAnnotations().add(profileAnnotation);
				}

			}else{
				cls = EcoreFactory.eINSTANCE.createEClass();
				/* for some reason the direct subclasses doesn't work so need to do this ourselves! */

				for (EClass s: getDirectSuperEClasses(c)){
					((EClass)cls).getESuperTypes().add(s);
				}

			}
			classMap.put(c, cls);
			cls.setName(c.getLocalName());
			OntResource p = c.getIsDefinedBy();
			EPackage pkg;
			if (p!=null)
				pkg = getEPackage(p);
			else
				pkg = rootPackage;
			pkg.getEClassifiers().add(cls);

			if (c.getComment()!=null){
				addComment(cls, c.getComment());
			}

		}
		if (basicTypeMap.containsKey(c)) return basicTypeMap.get(c);
		return classMap.get(c);
	}

	private void addComment(ENamedElement c, String comment){
		if (comment != null) {
			EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
			annotation.setSource("http://www.eclipse.org/emf/2002/GenModel");
			annotation.getDetails().put("Documentation", comment);
			c.getEAnnotations().add(annotation);
		}
	}

	private EStructuralFeature getFeature(OntResource prop){
		EClassifier domain = getEClass(prop.getDomain());
		if (domain instanceof EClass){
			EStructuralFeature feature;
			EClassifier range = getEClass(prop.getRange());
			if (prop.isDatatype() || range instanceof EEnum || range instanceof EDataType){
				feature = EcoreFactory.eINSTANCE.createEAttribute();
				feature.setUnsettable(true);
				if (prop.getComment()!=null){
					addComment(feature, prop.getComment());
				}
			}else{
				if (!referenceMap.containsKey(prop)){
					EReference nRef = EcoreFactory.eINSTANCE.createEReference();
					try{
						nRef.setLowerBound(prop.getInteger(OWL.minCardinality));
					}catch (NullPointerException npe){
						nRef.setLowerBound(0);
					}
					try{
						nRef.setUpperBound(prop.getInteger(OWL.maxCardinality));
					}catch (NullPointerException npe){
						if (prop.isFunctionalProperty())
							nRef.setUpperBound(1);
						else
							nRef.setUpperBound(-1);

					}

					referenceMap.put(prop, nRef);
					if (prop.getInverse()!=null){
						EReference op = (EReference)getFeature(prop.getInverse());
						nRef.setEOpposite(op);
					}

					if (prop.getComment()!=null){
						addComment(nRef, prop.getComment());
					}

				}
				feature = referenceMap.get(prop);
			}
			feature.setEType(range);
			feature.setName(prop.getLabel());
			((EClass)domain).getEStructuralFeatures().add(feature);


			return feature;
		}else if (domain instanceof EEnum){
			System.out.println("Adding literal");

			EEnumLiteral lit = EcoreFactory.eINSTANCE.createEEnumLiteral();
			lit.setLiteral(prop.getLocalName());
			((EEnum)domain).getELiterals().add(lit);
		}
		return null;	
	}

	@SuppressWarnings("unused")
	public Resource createEcore(boolean createRootElement, String nsPrefix, String nsURI){
		Resource xmiResource = new XMIResourceImpl();
		ResIterator cit = model.listNamedClasses();

		this.nsPrefix = nsPrefix;
		if (nsURI.endsWith("#")) this.nsURI = nsURI.substring(0, nsURI.length()-1);

		rootPackage = EcoreFactory.eINSTANCE.createEPackage();
		rootPackage.setNsPrefix(this.nsPrefix);
		rootPackage.setNsURI(this.nsURI);
		rootPackage.setName("Global");
		xmiResource.getContents().add(rootPackage);

		while (cit.hasNext()){
			OntResource res = (OntResource) cit.next();
			if (!res.equals(UML.Package) && !res.equals(UML.Stereotype)){
				EClassifier cls = getEClass(res);
			}
		}

		ResIterator opIt = model.listObjectProperties();
		while (opIt.hasNext()){
			OntResource prop = opIt.nextResource();
			EStructuralFeature feat = getFeature(prop);
		}

		opIt = model.listDatatypeProperties();
		while (opIt.hasNext()){
			OntResource prop = opIt.nextResource();
			EStructuralFeature feat = getFeature(prop);
		}

		/* Need to check that there are not duplicate structural feature names in a hierarchy
		 * as this is invalid for Ecore
		 */

		for (EClassifier c: classMap.values()){
			if (c instanceof EClass){
				EClass cls = (EClass)c;
				Collection<EStructuralFeature> removals = new Vector<EStructuralFeature>();
				for (EStructuralFeature f: cls.getEStructuralFeatures()){
					boolean remove = false;
					for (EStructuralFeature sf : cls.getEAllStructuralFeatures()){
						if (sf.getEContainingClass()!=cls){
							if (sf.getName().equals(f.getName())){
								remove = true;
								System.out.println("Marking "+f.getName()+" in "+cls.getName()+" for removal - duplicate in "+sf.getEContainingClass().getName());
								break;
							}

						}
					}

					if (remove)
						removals.add(f);
				}
				cls.getEStructuralFeatures().removeAll(removals);

			}
		}


		if (rootPackage.getEClassifiers().size()==0 && rootPackage.getESubpackages().size()==1){
			EPackage newRoot = rootPackage.getESubpackages().get(0);
			newRoot.setNsPrefix(this.nsPrefix);
			newRoot.setNsURI(this.nsURI);
			rootPackage.getESubpackages().remove(newRoot);
			xmiResource.getContents().remove(rootPackage);
			xmiResource.getContents().add(newRoot);
			rootPackage = newRoot;
		}

		if (createRootElement){
			EClass root = EcoreFactory.eINSTANCE.createEClass();
			root.setAbstract(true);
			root.setName(ECoreGenerator.ELEMENT_CLASS_NAME);
			EAttribute identifier = EcoreFactory.eINSTANCE.createEAttribute();
			identifier.setName(ECoreGenerator.ELEMENT_CLASS_IDENTIFIER);
			identifier.setEType(EcorePackage.eINSTANCE.getEString());
			identifier.setID(true);
			root.getEStructuralFeatures().add(identifier);
			boolean add = true;
			for (EClassifier c: classMap.values()){
				if (c.getName().equals(root.getName()) && 
						c instanceof EClass &&
						c.getEPackage() == rootPackage &&
						((EClass)c).getEStructuralFeatures().size()==1 &&
						((EClass)c).getEStructuralFeatures().get(0) instanceof EAttribute &&
						((EClass)c).getEStructuralFeatures().get(0).getName().equals(identifier.getName())){
					add = false;
					break;
				}
			}
			if (add){
				rootPackage.getEClassifiers().add(root);
				for (EClassifier c: classMap.values()){
					if (c instanceof EClass){
						EClass cls = (EClass)c;
						if (cls.getESuperTypes().size()==0)
							cls.getESuperTypes().add(root);
					}
				}
			}
		}

		return xmiResource;
	}

}
