package com.cimphony.cimtoole.util;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;

import com.cimphony.cimtoole.ecore.EcoreGenerator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.XSD;

public class CIMToolEcoreUtil {
	
	private static Map<Resource, Class<?>> XSD_TO_CLASS_MAP = new HashMap<Resource, Class<?>>();
	private static Map<String, Class<?>> XSD_URI_TO_CLASS_MAP = new HashMap<String, Class<?>>();
	private static Map<Class<?>, Resource> CLASS_TO_XSD_MAP = new HashMap<Class<?>, Resource>();
	private static Map<String, EDataType> XSD_URI_TO_EDATATYPE_MAP = new HashMap<String, EDataType>();
	private static Map<Resource, String> XSD_TO_PATTERN = new HashMap<Resource, String>();
	private static boolean INITIALISED = false;
	
	private static void init(){
		XSD_TO_CLASS_MAP.put(XSD.xstring, java.lang.String.class);
		XSD_TO_CLASS_MAP.put(XSD.integer, java.math.BigInteger.class);
		XSD_TO_CLASS_MAP.put(XSD.xint, int.class);
		XSD_TO_CLASS_MAP.put(XSD.xlong, long.class);
		XSD_TO_CLASS_MAP.put(XSD.xshort, short.class);
		XSD_TO_CLASS_MAP.put(XSD.decimal, java.math.BigDecimal.class);
		XSD_TO_CLASS_MAP.put(XSD.xfloat, float.class);
		XSD_TO_CLASS_MAP.put(XSD.xdouble, double.class);
		XSD_TO_CLASS_MAP.put(XSD.xboolean, boolean.class);
		XSD_TO_CLASS_MAP.put(XSD.xbyte, byte.class);
		XSD_TO_CLASS_MAP.put(XSD.QName, javax.xml.namespace.QName.class);
		XSD_TO_CLASS_MAP.put(XSD.dateTime, java.util.Date.class);
		XSD_TO_CLASS_MAP.put(XSD.base64Binary, byte[].class);
		XSD_TO_CLASS_MAP.put(XSD.hexBinary, byte[].class);
		XSD_TO_CLASS_MAP.put(XSD.unsignedInt, long.class);
		XSD_TO_CLASS_MAP.put(XSD.unsignedShort, int.class);
		XSD_TO_CLASS_MAP.put(XSD.unsignedByte, short.class);
		XSD_TO_CLASS_MAP.put(XSD.time, java.util.Date.class);
		XSD_TO_CLASS_MAP.put(XSD.date, java.util.Date.class);
		XSD_TO_CLASS_MAP.put(XSD.gDay, java.util.Date.class);
		XSD_TO_CLASS_MAP.put(XSD.gMonth, java.util.Date.class);
		XSD_TO_CLASS_MAP.put(XSD.gMonthDay, java.util.Date.class);
		XSD_TO_CLASS_MAP.put(XSD.gYear, java.util.Date.class);
		XSD_TO_CLASS_MAP.put(XSD.gYearMonth, java.util.Date.class);
		XSD_TO_CLASS_MAP.put(XSD.anyURI, java.lang.Object.class);
		XSD_TO_CLASS_MAP.put(XSD.duration, javax.xml.datatype.Duration.class);
		XSD_TO_CLASS_MAP.put(XSD.NOTATION, javax.xml.namespace.QName.class);
		
		for (Resource k : XSD_TO_CLASS_MAP.keySet()){
			XSD_URI_TO_CLASS_MAP.put(k.getURI(), XSD_TO_CLASS_MAP.get(k));
			if (!CLASS_TO_XSD_MAP.containsKey(XSD_TO_CLASS_MAP.get(k))){
				CLASS_TO_XSD_MAP.put(XSD_TO_CLASS_MAP.get(k), k);
			}
		}
		
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.xstring.toString(), EcorePackage.eINSTANCE.getEString());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.integer.toString(), EcorePackage.eINSTANCE.getEInt());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.xint.toString(), EcorePackage.eINSTANCE.getEInt());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.xlong.toString(), EcorePackage.eINSTANCE.getELong());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.xshort.toString(), EcorePackage.eINSTANCE.getEShort());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.decimal.toString(), EcorePackage.eINSTANCE.getEBigDecimal());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.xfloat.toString(), EcorePackage.eINSTANCE.getEFloat());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.xdouble.toString(), EcorePackage.eINSTANCE.getEDouble());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.xboolean.toString(), EcorePackage.eINSTANCE.getEBoolean());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.xbyte.toString(), EcorePackage.eINSTANCE.getEByte());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.QName.toString(), EcorePackage.eINSTANCE.getEString());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.base64Binary.toString(), EcorePackage.eINSTANCE.getEByteArray());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.hexBinary.toString(), EcorePackage.eINSTANCE.getEByteArray());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.unsignedInt.toString(), EcorePackage.eINSTANCE.getEInt());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.unsignedShort.toString(), EcorePackage.eINSTANCE.getEShort());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.unsignedByte.toString(), EcorePackage.eINSTANCE.getEByte());
		XSD_URI_TO_EDATATYPE_MAP.put(XSD.NOTATION.toString(), EcorePackage.eINSTANCE.getEString());
		
		XSD_TO_PATTERN.put(XSD.dateTime, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		XSD_TO_PATTERN.put(XSD.time, "HH:mm:ss.SSSZ");
		XSD_TO_PATTERN.put(XSD.date, "yyyy-MM-dd");
		XSD_TO_PATTERN.put(XSD.gDay, "DD");
		XSD_TO_PATTERN.put(XSD.gMonth, "MM");
		XSD_TO_PATTERN.put(XSD.gMonthDay, "dd");
		XSD_TO_PATTERN.put(XSD.gYear, "yyyy");
		XSD_TO_PATTERN.put(XSD.gYearMonth, "MM");
		XSD_TO_PATTERN.put(XSD.duration, "HH:mm:ss.SSS");
	}
	
	private static Map<String, EDataType> getTimeTypes(){
		Map<String, EDataType> newMap = new HashMap<String, EDataType>();
		for (Resource r : XSD_TO_PATTERN.keySet()){
			EDataType newType  = EcoreFactory.eINSTANCE.createEDataType();
			String name = r.getLocalName();
			name = name.substring(0,1).toUpperCase() + name.substring(1);
			newType.setName("CIM"+name);
			newType.setInstanceClass(Date.class);
			EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
			annotation.setSource(EcoreGenerator.RDF_SERIALISATION_ANNOTATION);
			annotation.getDetails().put("CIMDatePattern", XSD_TO_PATTERN.get(r));
			newType.getEAnnotations().add(annotation);
			
			EAnnotation profileAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
			profileAnnotation.setSource("http:///org/eclipse/emf/ecore/util/ExtendedMetaData");
			profileAnnotation.getDetails().put("baseType", r.toString());
			newType.getEAnnotations().add(profileAnnotation);
			
			newMap.put(r.toString(), newType);
		}
		return newMap;
	}
	
	public static Map<String, EDataType> getEDataTypeMap(){
		if (!INITIALISED) init();
		Map<String, EDataType> newMap = new HashMap<String, EDataType>();
		newMap.putAll(XSD_URI_TO_EDATATYPE_MAP);
		newMap.putAll(CIMToolEcoreUtil.getTimeTypes());
		
		return Collections.unmodifiableMap(newMap);
	}
	
	public static Class<?> getTypeClass(Resource type){
		if (!INITIALISED) init();
		return XSD_TO_CLASS_MAP.get(type);
	}

	public static Class<?> getTypeClass(String type){
		if (!INITIALISED) init();
		return XSD_URI_TO_CLASS_MAP.get(type);
	}
	
	public static Resource getType(Class<?> cls){
		if (!INITIALISED) init();
		return CLASS_TO_XSD_MAP.get(cls);
	}

	
	public static String getDocumentation(EModelElement eModelElement){
		EAnnotation an = eModelElement.getEAnnotation("http://www.eclipse.org/emf/2002/GenModel"); //$NON-NLS-1$
		String text = null;
		if (an != null) {
			if (an.getDetails().containsKey("documentation")) {//$NON-NLS-1$
				text = an.getDetails().get("documentation"); //$NON-NLS-1$
			} else if (an.getDetails().containsKey("Documentation")) { //$NON-NLS-1$
				text = an.getDetails().get("Documentation"); //$NON-NLS-1$
			}
		}
		return text;

	}
}
