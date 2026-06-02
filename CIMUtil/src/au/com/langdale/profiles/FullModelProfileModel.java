package au.com.langdale.profiles;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.vocabulary.OWL;

/**
 * A {@link ProfileModel} variant that presents the entire canonical background
 * (CIM) model as if it were a fully-populated profile, without requiring a
 * user-defined profile {@code .owl} file.
 *
 * <h3>Purpose</h3>
 * <p>
 * End users sometimes need the full canonical UML model expressed in CIMTool's
 * internal XML format (the {@code a:Catalog} document that {@link ProfileSerializer}
 * produces) so that they can author large-scale XSLT transforms that operate
 * over the complete schema rather than a hand-curated subset. Manually dragging
 * every class from right to left in the profile editor is impractical for a
 * schema of CIM's scale.
 * </p>
 *
 * <h3>Design</h3>
 * <p>
 * {@code FullModelProfileModel} overrides only the two points in
 * {@code ProfileModel} where profile-specific behaviour would otherwise be
 * applied:
 * </p>
 * <ol>
 *   <li><b>Initialisation</b> — {@link #initialize(OntModel)} sets the tree's
 *       underlying ontology model directly to the background model (bypassing
 *       the profile+background merge that {@link #setOntModel}/{@link #setBackgroundModel}
 *       normally perform, which requires a valid profile OWL file).</li>
 *   <li><b>Class enumeration</b> — the inner {@link FullModelCatalogNode}
 *       overrides {@link ProfileModel.CatalogNode#populate()} to enumerate every
 *       named class in the background model rather than reading class URIs from a
 *       profile OWL file. Each class is wrapped in a {@link SchemaProfileClass},
 *       which discovers properties via {@code rdfs:domain} and reads cardinalities
 *       from the {@code UML.schemaMin} / {@code UML.schemaMax} annotation
 *       properties written onto every property resource during schema import.</li>
 * </ol>
 *
 * <h3>What is unchanged</h3>
 * <p>
 * {@link ProfileSerializer}, {@link ProfileModel}'s node hierarchy
 * ({@code TypeNode}, {@code ElementNode}, etc.), and the existing builder
 * pipeline are all untouched. The schema-mode {@code ProfileClass} instances
 * created here plug directly into the existing serialisation path.
 * </p>
 *
 * <h3>Initialisation sequence</h3>
 * <pre>{@code
 * OntModel backgroundModel = Task.getBackgroundModel(file);
 * FullModelProfileModel model = new FullModelProfileModel();
 * model.initialize(backgroundModel);
 * ProfileSerializer serializer = new ProfileSerializer(model);
 * // ... configure serializer parameters ...
 * serializer.write(outputStream);
 * }</pre>
 */
public class FullModelProfileModel extends ProfileModel {

	/**
	 * Initializes this model from the background (canonical CIM) model alone.
	 * No profile {@code .owl} file is required.
	 *
	 * <p>
	 * This method:
	 * </p>
	 * <ol>
	 *   <li>Stores the background model via {@link #setBackgroundModel(OntModel)}
	 *       (so that {@link #getBackgroundModel()} and
	 *       {@link #getOntologyNamespace()} work correctly).</li>
	 *   <li>Calls {@link #initAsFullModel(OntModel)} to set the tree's underlying
	 *       ontology model directly to the background model, bypassing the merge
	 *       step that requires a profile OWL file.</li>
	 *   <li>Calls {@link #setRootResource(com.hp.hpl.jena.graph.FrontsNode)} with
	 *       the background model's {@code owl:Ontology} header resource so that the
	 *       tree root is classified via {@link #classify(OntResource)}, which this
	 *       class overrides to return a {@link FullModelCatalogNode}.</li>
	 * </ol>
	 *
	 * @param backgroundModel the merged background (CIM) ontology model
	 */
	public void initialize(OntModel backgroundModel) {
		// Store in parent so getBackgroundModel() / getOntologyNamespace() work.
		setBackgroundModel(backgroundModel);

		// Directly set the tree's ontModel to the background model.
		// setBackgroundModel() above called initModels(), which did nothing because
		// there is no profileModel. We must set the tree model explicitly here.
		initAsFullModel(backgroundModel);

		// Set the tree root from the background model's Ontology header so that
		// classify() is invoked and produces a FullModelCatalogNode.
		OntResource ont = backgroundModel.getValidOntology();
		if (ont != null) {
			setRootResource(ont);
		}
	}

	/**
	 * Overrides {@link ProfileModel#classify(OntResource)} to return a
	 * {@link FullModelCatalogNode} when the root is an {@code owl:Ontology}
	 * resource, ensuring that class enumeration draws from the background model
	 * rather than from a profile OWL file.
	 */
	@Override
	protected Node classify(OntResource root) {
		if (root.hasRDFType(OWL.Ontology))
			return new FullModelCatalogNode(root);
		return super.classify(root);
	}

	// -------------------------------------------------------------------------
	// Inner class: FullModelCatalogNode
	// -------------------------------------------------------------------------

	/**
	 * A {@link ProfileModel.CatalogNode} whose {@link #populate()} method
	 * enumerates every named class in the background model instead of reading
	 * class URIs from a profile OWL file.
	 *
	 * <p>
	 * Each background class is wrapped in a {@link ProfileClass} created via
	 * {@link ProfileClass#forSchema(OntResource, String)}, which derives property
	 * membership and cardinalities directly from {@code rdfs:domain},
	 * {@code UML.schemaMin}, and {@code UML.schemaMax} annotations stored on
	 * every property resource during schema import — no OWL restriction triples
	 * are synthesised.
	 * </p>
	 *
	 * <p>
	 * System namespaces (OWL, MESSAGE) are excluded from enumeration so that
	 * infrastructure classes do not appear in the output.
	 * </p>
	 */
	public class FullModelCatalogNode extends CatalogNode {

		public FullModelCatalogNode(OntResource message) {
			super(message);
		}

		/**
		 * Enumerates all named classes from the background model, creating a
		 * schema-mode {@link ProfileClass} and a corresponding {@link TypeNode} or
		 * {@link EnvelopeNode} for each.
		 *
		 * <p>
		 * Classes in the OWL and MESSAGE system namespaces are skipped. All other
		 * named classes — regardless of their namespace — are included, so that the
		 * output covers the full extent of the canonical CIM schema including any
		 * extension namespaces that were merged into the background model.
		 * </p>
		 */
		@Override
		protected void populate() {
			OntModel model = getOntModel();
			if (model == null)
				return;

			// Drain the live Jena iterator into a snapshot list before constructing
			// any SchemaProfileClass instances. The constructor calls analyseBaseClass()
			// which writes stereotype annotations back onto class resources via
			// clss.addProperty(...). That mutation invalidates the live iterator and
			// causes a ConcurrentModificationException if we iterate and construct
			// simultaneously.
			List<OntResource> snapshot = new ArrayList<>();
			ResIterator it = model.listNamedClasses();
			while (it.hasNext()) {
				snapshot.add(it.nextResource());
			}

			for (OntResource clss : snapshot) {

				// Skip system infrastructure classes that must not appear in output.
				String ns = clss.getNameSpace();
				if (ns == null)
					continue;
				if (ns.equals(MESSAGE.NS))
					continue;
				if (ns.equals(OWL.NS))
					continue;

				// Create a SchemaProfileClass for this background-model class.
				// SchemaProfileClass derives property membership and cardinalities from
				// rdfs:domain / UML.schemaMin / UML.schemaMax annotations rather than
				// from OWL restriction triples that do not exist in the background model.
				ProfileClass profile = new SchemaProfileClass(clss, getNamespace());

				// Replicate the routing logic of the standard CatalogNode.populate():
				// envelope classes (those based on MESSAGE.Message) become EnvelopeNodes;
				// everything else becomes a TypeNode.
				if (profile.getBaseClass() != null) {
					if (profile.getBaseClass().equals(MESSAGE.Message))
						add(new EnvelopeNode(profile));
					else
						add(new TypeNode(profile));
				}
			}
		}
	}
}
