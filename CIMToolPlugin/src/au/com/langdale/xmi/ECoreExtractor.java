package au.com.langdale.xmi;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory.Descriptor;
import org.eclipse.emf.ecore.util.EcoreUtil;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.XSD;

public class ECoreExtractor extends XMIModel {

	public class InvalidEDataTypeException extends Exception{
		private static final long serialVersionUID = -2616865485691314837L;
		EDataType type;

		public InvalidEDataTypeException(EDataType type){
			this.type = type;
		}

		public EDataType getType(){
			return type;
		}

	}

	protected IFile file;
	/** the ontology under construction */
	protected Map<EPackage, OntResource> packageMap;
	protected Map<EClassifier, OntResource> classMap;
	protected Set<EReference> processedAssociations;

	private ECoreExtractor(){
		model = ModelFactory.createMem();
		packageMap = new HashMap<EPackage, OntResource>();
		classMap = new HashMap<EClassifier, OntResource>();
		processedAssociations = new HashSet<EReference>();
	}

	public ECoreExtractor(IFile file){
		this();
		this.file = file;
	}

	/**
	 * Return the underlying Jena OWL model. 
	 */
	public OntModel getModel() {
		return model;
	}

	private static String getXUID(EObject o){
		return "_"+Integer.toHexString(EcoreUtil.getURI(o).toString().hashCode());
	}

	public void run() throws IOException, CoreException{
		if (!file.getFileExtension().equals("ecore"))
			return;
		Resource res = ((Descriptor)Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().get("ecore")).createFactory()
		.createResource(URI.createFileURI(file.getFullPath().toString()));
		res.load(file.getContents(), Collections.EMPTY_MAP);
		if (res.getContents().size()==0) throw new CoreException(new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID, "ECore Resource is empty"));
		if (!(res.getContents().get(0) instanceof EPackage)) throw new CoreException(new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID, "File contains no EPackages"));

		EPackage root = (EPackage)res.getContents().get(0);
		try{
			model.setNsPrefix(root.getNsPrefix(), root.getNsURI());
			processEPackage(root);
			for (EClassifier c: classMap.keySet())
				postProcessEClassifiers(c);
		}catch (NullPointerException npe){
			npe.printStackTrace();
		}

	}

	protected void processEPackage(EPackage p) throws CoreException{
		System.out.println("Starting processing of "+p.getName()+" Package");
		OntResource op = createIndividual(ECoreExtractor.getXUID(p), p.getName(), UML.Package);
		annotate(p, op);
		packageMap.put(p, op);
		OntResource parent = packageMap.get(p.getESuperPackage());
		op.addIsDefinedBy(parent == null? UML.global_package: parent);
		for (EPackage sp: p.getESubpackages()){
			processEPackage(sp);
		}
		for (EClassifier c: p.getEClassifiers()){
			processEClassifier(c);
		}
	}

	protected void processEClassifier(EClassifier c) throws CoreException{
		OntResource oc = createClass(ECoreExtractor.getXUID(c), c.getName());
		annotate(c, oc);
		classMap.put(c, oc);
		OntResource op = packageMap.get(c.getEPackage());
		oc.addIsDefinedBy(op == null? UML.global_package: op);

		if (c instanceof EEnum){
			oc.addProperty(UML.hasStereotype, UML.enumeration);
			EEnum ee = (EEnum)c;
			for (EEnumLiteral lit: ee.getELiterals()){
				createIndividual(ECoreExtractor.getXUID(lit), lit.getLiteral(), oc);
			}
		}else if (c instanceof EClass){
			EClass ec = (EClass)c;
			if (!ec.isAbstract()){
				oc.addProperty(UML.hasStereotype, UML.concrete);
			}
		}else if (c instanceof EDataType){
			oc.addProperty(UML.hasStereotype, UML.datatype);
			try{
				oc.addProperty(OWL.sameAs, getXSDType((EDataType)c));
			}catch(InvalidEDataTypeException ex){
				throw new CoreException(new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID, "Invalid EDataType in Ecore - no XSD Mapping", ex));
			}
		}else{
			System.err.println("EClassifier of type "+c.getClass().getName());			
		}
	}

	protected com.hp.hpl.jena.rdf.model.Resource getXSDType(EDataType d) throws InvalidEDataTypeException{
		/*
		http://www.w3.org/2001/XMLSchema#string = java.lang.String
		http://www.w3.org/2001/XMLSchema#float = double
		http://www.w3.org/2001/XMLSchema#integer = int
		http://www.w3.org/2001/XMLSchema#int = int
		http://www.w3.org/2001/XMLSchema#boolean = boolean
		http://www.w3.org/2001/XMLSchema#dateTime = java.util.Date
		 * 
		 */
		Class<?> ic = d.getInstanceClass();
		if (ic == String.class){
			return XSD.xstring;
		}else if (ic == double.class){
			return XSD.xdouble;
		}else if (ic == float.class){
			return XSD.xfloat;
		}else if (ic == int.class){
			return XSD.xint;
		}else if (ic == short.class){
			return XSD.xshort;
		}else if (ic == long.class){
			return XSD.xlong;
		}else if (ic == boolean.class){
			return XSD.xboolean;
		}else if (ic == Date.class){
			return XSD.dateTime;
		}
		throw new InvalidEDataTypeException(d);

	}

	protected void postProcessEClassifiers(EClassifier c) throws CoreException{
		if (c instanceof EClass){
			OntResource oc = classMap.get(c);
			for (EClass cs: ((EClass) c).getESuperTypes()){
				OntResource os = classMap.get(cs);
				oc.addSuperClass(os);
			}
			for (EStructuralFeature f: ((EClass) c).getEStructuralFeatures()){
		
				if (f instanceof EAttribute){
					OntResource subject = createAttributeProperty(ECoreExtractor.getXUID(f), f.getName());
					subject.addDomain(oc);
					annotate(f, subject);
					FrontsNode type = classMap.get(f.getEType());
					if (type == null && f.getEType() instanceof EDataType){
						try {
							type = getXSDType((EDataType)f.getEType());
						} catch (InvalidEDataTypeException ex) {
							throw new CoreException(new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID, "Invalid EDataType in Ecore - no XSD Mapping", ex));
						}
					}
					if (type != null)
						subject.addRange(type);
					else
						subject.addRange(XSD.anyURI);
				}else if (f instanceof EReference){
					EReference r = (EReference)f;
					if (!processedAssociations.contains(r)){
						Role rolea = extractProperty(r, classMap.get(r.getEType()), r.isContainer(), true);
						rolea.property.addDomain(oc);
						processedAssociations.add(r);
						if (r.getEOpposite()!=null && classMap.containsKey(r.getEOpposite().getEType())){
							Role roleb = extractProperty(r.getEOpposite(), classMap.get(r.getEOpposite().getEType()), r.getEOpposite().isContainer(), false);
							roleb.property.addDomain(classMap.get(r.getEOpposite().getEContainingClass()));
							rolea.mate(roleb);
							roleb.mate(rolea);
							processedAssociations.add(r.getEOpposite());
						}
					}
				}
			}
		}
	}

	protected Role extractProperty(EReference ref, OntResource destin, boolean aggregate, boolean sideA) {
		Role role = new Role();
		role.property = createObjectProperty(ECoreExtractor.getXUID(ref), sideA, ref.getName());
		role.range = destin;
		role.aggregate = aggregate;
		role.upper = ref.getUpperBound();
		role.lower = ref.getLowerBound();
		return role;
	}


	protected void annotate(ENamedElement el, OntResource o){
		if (el.getEAnnotation("http://www.eclipse.org/emf/2002/GenModel")!=null){
			if (el.getEAnnotation("http://www.eclipse.org/emf/2002/GenModel").getDetails().get("documentation")!=null){
				o.addComment(el.getEAnnotation("http://www.eclipse.org/emf/2002/GenModel").getDetails().get("documentation"), LANG);
			}
		}
	}
}
