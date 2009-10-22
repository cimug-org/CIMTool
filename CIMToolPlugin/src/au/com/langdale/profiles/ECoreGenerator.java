package au.com.langdale.profiles;

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

	@Override
	protected void emitPackage(String uri) {
		System.out.println("Package: " + uri);

		EPackage pkg = coreFactory.createEPackage();
		pkg.setName(uri);
		pkg.setNsPrefix("cim");
		pkg.setNsURI(uri);
		result.getESubpackages().add(pkg);
	}

	@Override
	protected void emitClass(String uri, String base) {
		System.out.println("Class: " + uri + ", " + base);

		EClass klass = coreFactory.createEClass();
		klass.setName(uri);
		result.getEClassifiers().add(klass);
	}

	@Override
	protected void emitLabel(String uri, String label) {
		System.out.println("Label: " + uri + ", " + label);
	}

	@Override
	protected void emitSuperClass(String subClass, String superClass) {
		System.out.println("SuperClass: " + subClass + ", " + superClass);
	}

	@Override
	protected void emitDatatype(String uri, String xsdtype) {
		System.out.println("DataType: " + uri + ", " + xsdtype);
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain,
			String type, String xsdtype, boolean required) {
		System.out.println("DataType Property: " + uri + ", " + base + ", " +
				domain + ", " + type + ", " + xsdtype + ", " + required);
	}

	@Override
	protected void emitStereotype(String uri, String stereo) {
		System.out.println("StereoType: " + uri + ", " + stereo);
	}

	@Override
	protected void emitBaseStereotype(String uri, String stereo) {
		System.out.println("BaseStereoType: " + uri + ", " + stereo);
	}

	@Override
	protected void emitComment(String uri, String baseComment,
			String profileComment) {
		System.out.println("Comment: " + uri + ", " + baseComment + ", " + profileComment);
	}

	@Override
	protected void emitDefinedBy(String uri, String container) {
		System.out.println("DefinedBy: " + uri + ", " + container);
	}

	@Override
	protected void emitInstance(String uri, String base, String type) {
		System.out.println("Instance: " + uri + ", " + base + ", " + type);
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		System.out.println("Inverse: " + uri + ", " + iuri);
	}

	@Override
	protected void emitObjectProperty(String uri, String base, String domain,
			String range, boolean required, boolean functional) {
		System.out.println("Object Property: " + uri + ", " + base + ", " +
				domain + ", " + range + ", " + required + ", " + functional);
	}

	@Override
	protected void emitOntProperty(String uri) {
		System.out.println("Ont Property: " + uri);
	}

	@Override
	protected void emitOntProperty(String uri, String value) {
		System.out.println("Ont Property2: " + uri + ", " + value);
	}

	@Override
	protected void emitRestriction(String uri, String domain, String range) {
		System.out.println("Restriction: " + uri + ", " + domain + ", " + range);
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,
			boolean functional) {
		System.out.println("Restriction: " + uri + ", " + domain + ", " +
				required + ", " + functional);
	}

}
