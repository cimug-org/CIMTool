package au.com.langdale.profiles;

import org.eclipse.emf.ecore.*;

import au.com.langdale.kena.ModelFactory;
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

	@Override
	protected void emitBaseStereotype(String uri, String stereo) {
		// ignored
	}

	@Override
	protected void emitClass(String uri, String base) {
		EClass emitted = coreFactory.createEClass();
		emitted.setName(base);
		result.getEClassifiers().add(emitted);
	}

	@Override
	protected void emitComment(String uri, String baseComment,
			String profileComment) {
		// ignored
	}

	@Override
	protected void emitDatatype(String uri, String xsdtype) {
		// ignored
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain,
			String type, String xsdtype, boolean required) {
		// ignored
	}

	@Override
	protected void emitDefinedBy(String uri, String container) {
		// ignored
	}

	@Override
	protected void emitInstance(String uri, String base, String type) {
		// ignored
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		// ignored
	}

	@Override
	protected void emitLabel(String uri, String label) {
		// ignored
	}

	@Override
	protected void emitObjectProperty(String uri, String base, String domain,
			String range, boolean required, boolean functional) {
		// ignored
	}

	@Override
	protected void emitOntProperty(String uri) {
		// ignored
	}

	@Override
	protected void emitOntProperty(String uri, String value) {
		// ignored
	}

	@Override
	protected void emitPackage(String uri) {
		// ignored
	}

	@Override
	protected void emitRestriction(String uri, String domain, String range) {
		// ignored
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,
			boolean functional) {
		// ignored
	}

	@Override
	protected void emitStereotype(String uri, String stereo) {
		// ignored
	}

	@Override
	protected void emitSuperClass(String subClass, String superClass) {
		// ignored
	}

}
