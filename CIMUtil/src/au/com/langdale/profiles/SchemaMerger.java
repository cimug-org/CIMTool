package au.com.langdale.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.graph.impl.WrappedGraph;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.EAProjectParserFactory;
import au.com.langdale.xmi.Translator;

/**
 * Merges a project's schema files into one schema whose ontology is the
 * project's base CIM schema (issue #391).
 * <p>
 * One schema file is chosen as the base CIM schema. The merged schema contains
 * every file's content and an additional graph in which the base's ontology
 * header imports the header of every other schema file, so that
 * {@link OntModel#getValidOntology()} returns the base's header. The schema
 * files' own {@code owl:imports} statements are hidden from the merged schema:
 * in OWL a dependent ontology imports the one it depends on, so an extension
 * file importing the base would otherwise point the opposite way to the imports
 * added here. Nothing is written to the files' models, which are shared.
 * <p>
 * The base CIM schema is, in order:
 * <ol>
 * <li>the project's Enterprise Architect project file, of which a project may
 * have only one;</li>
 * <li>the one schema file flagged as the base ({@code tool:canonical_schema});</li>
 * <li>the only schema file, when there is just one;</li>
 * <li>the one schema file whose namespace equals the Schema Namespace
 * preference.</li>
 * </ol>
 * Otherwise there is no base, and the problem is reported.
 */
public class SchemaMerger {

	/** The default of the Schema Namespace preference. */
	public static final String DEFAULT_SCHEMA_NAMESPACE = "http://iec.ch/TC57/CIM100#";

	/** A schema file to merge. */
	public static final class SchemaFile {
		private final String name;
		private final OntModel model;
		private final String namespace;
		private final boolean base;

		/**
		 * @param name      the file's name, used in problem descriptions and to
		 *                  recognise an Enterprise Architect project file
		 * @param model     the model parsed from the file, or null when only the
		 *                  base is being selected ({@link SchemaMerger#selectBase})
		 * @param namespace the file's {@code tool:schema_namespace}, or null
		 * @param base      true if the file is flagged as the base CIM schema
		 */
		public SchemaFile(String name, OntModel model, String namespace, boolean base) {
			this.name = name;
			this.model = model;
			this.namespace = namespace;
			this.base = base;
		}

		public String getName() {
			return name;
		}

		public OntModel getModel() {
			return model;
		}

		public String getNamespace() {
			return namespace;
		}

		public boolean isFlaggedAsBase() {
			return base;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** The kinds of problem found while choosing the base CIM schema. */
	public enum ProblemType {
		/** More than one schema file is an Enterprise Architect project file. */
		MULTIPLE_EA_SCHEMAS(true),
		/** A schema file other than the Enterprise Architect project file is flagged as the base. */
		IGNORED_BASE_FLAG(false),
		/** Several schema files, none flagged, and none has the Schema Namespace. */
		NO_BASE_SCHEMA(true),
		/** No schema file is flagged and several have the Schema Namespace. */
		AMBIGUOUS_BASE_SCHEMA(true),
		/** More than one schema file is flagged as the base. */
		MULTIPLE_BASE_SCHEMAS(true),
		/** The base schema file declares more than one ontology header. */
		BASE_SCHEMA_HEADERS(true),
		/** The base schema file's ontology header is a blank node or its URI contains '#'. */
		BASE_SCHEMA_INVALID_URI(true),
		/** The base schema file declares no ontology header, so one is built from its namespace. */
		BASE_SCHEMA_NO_HEADER(false);

		private final boolean error;

		private ProblemType(boolean error) {
			this.error = error;
		}

		/** True if the merged schema has no valid ontology because of this problem. */
		public boolean isError() {
			return error;
		}
	}

	/** A problem found while choosing the base CIM schema. */
	public static final class Problem {
		private final ProblemType type;
		private final List<SchemaFile> files;
		private final String description;

		Problem(ProblemType type, List<SchemaFile> files, String description) {
			this.type = type;
			this.files = Collections.unmodifiableList(new ArrayList<SchemaFile>(files));
			this.description = description;
		}

		public ProblemType getType() {
			return type;
		}

		public boolean isError() {
			return type.isError();
		}

		/** The schema files the problem concerns. */
		public List<SchemaFile> getFiles() {
			return files;
		}

		/** What is wrong, without the action to take, which depends on where it is shown. */
		public String getDescription() {
			return description;
		}

		@Override
		public String toString() {
			return type + ": " + description;
		}
	}

	/** The result of merging the schema files. */
	public static final class Result {
		private final MultiUnion graph;
		private final SchemaFile base;
		private final List<Problem> problems;

		Result(MultiUnion graph, SchemaFile base, List<Problem> problems) {
			this.graph = graph;
			this.base = base;
			this.problems = Collections.unmodifiableList(problems);
		}

		/**
		 * The merged schema. Callers wrap it in a model with or without inference,
		 * as they did with their own unions.
		 */
		public MultiUnion getGraph() {
			return graph;
		}

		/** The base CIM schema file, or null if none was identified. */
		public SchemaFile getBase() {
			return base;
		}

		public List<Problem> getProblems() {
			return problems;
		}

		/** True if the merged schema has no valid ontology. */
		public boolean hasErrors() {
			for (Problem problem : problems)
				if (problem.isError())
					return true;
			return false;
		}
	}

	/** The base CIM schema chosen from a project's schema files. */
	public static final class Selection {
		private final SchemaFile base;
		private final List<Problem> problems;

		Selection(SchemaFile base, List<Problem> problems) {
			this.base = base;
			this.problems = Collections.unmodifiableList(problems);
		}

		/** The base CIM schema file, or null if none was identified. */
		public SchemaFile getBase() {
			return base;
		}

		public List<Problem> getProblems() {
			return problems;
		}
	}

	private SchemaMerger() {
	}

	/**
	 * Chooses the base CIM schema from the schema files' names, namespaces and
	 * flags alone, without their models. It applies the same rules as
	 * {@link #merge}, apart from the checks on the base's ontology header, which
	 * need the base's model.
	 * 
	 * @param files                     the schema files, in a stable order; their
	 *                                  models may be null
	 * @param schemaNamespacePreference the Schema Namespace preference, or null
	 *                                  for its default
	 * @return the base CIM schema, if any, and the problems found
	 */
	public static Selection selectBase(List<SchemaFile> files, String schemaNamespacePreference) {
		List<Problem> problems = new ArrayList<Problem>();
		SchemaFile base = chooseBase(files, preference(schemaNamespacePreference), problems);
		return new Selection(base, problems);
	}

	private static String preference(String schemaNamespacePreference) {
		return schemaNamespacePreference != null && !schemaNamespacePreference.isEmpty() ? schemaNamespacePreference
				: DEFAULT_SCHEMA_NAMESPACE;
	}

	/**
	 * Merges the schema files.
	 *
	 * @param files                     the schema files, in a stable order
	 * @param schemaNamespacePreference the Schema Namespace preference, or null
	 *                                  for its default
	 * @return the merged schema, the base CIM schema and any problems
	 */
	public static Result merge(List<SchemaFile> files, String schemaNamespacePreference) {
		Selection selection = selectBase(files, schemaNamespacePreference);
		List<Problem> problems = new ArrayList<Problem>(selection.getProblems());

		SchemaFile base = selection.getBase();
		OntModel imports = ModelFactory.createMem();
		if (base != null && !addImports(base, files, imports, problems))
			base = null;

		MultiUnion union = new MultiUnion();
		union.addGraph(imports.getGraph());
		union.setBaseGraph(imports.getGraph());
		for (SchemaFile file : files)
			union.addGraph(new ImportsHiddenGraph(file.getModel().getGraph()));

		return new Result(union, base, problems);
	}

	private static SchemaFile chooseBase(List<SchemaFile> files, String preference, List<Problem> problems) {
		List<SchemaFile> eaProjects = new ArrayList<SchemaFile>();
		List<SchemaFile> flagged = new ArrayList<SchemaFile>();
		for (SchemaFile file : files) {
			if (EAProjectParserFactory.isEAProject(file.getName()))
				eaProjects.add(file);
			if (file.isFlaggedAsBase())
				flagged.add(file);
		}

		if (eaProjects.size() > 1) {
			problems.add(new Problem(ProblemType.MULTIPLE_EA_SCHEMAS, eaProjects, names(eaProjects)
					+ " are all Enterprise Architect project files; a project can have only one, which is its base CIM schema"));
			return null;
		}
		if (eaProjects.size() == 1) {
			SchemaFile eaProject = eaProjects.get(0);
			flagged.remove(eaProject);
			if (!flagged.isEmpty())
				problems.add(new Problem(ProblemType.IGNORED_BASE_FLAG, flagged, names(flagged)
						+ (flagged.size() == 1 ? " is" : " are") + " marked as the base CIM schema, but "
						+ eaProject.getName()
						+ " is the project's Enterprise Architect project file and is always the base, so the flag is ignored"));
			return eaProject;
		}

		if (flagged.size() == 1)
			return flagged.get(0);
		if (flagged.size() > 1) {
			problems.add(new Problem(ProblemType.MULTIPLE_BASE_SCHEMAS, flagged,
					names(flagged) + " are all marked as the base CIM schema; only one can be"));
			return null;
		}

		if (files.size() == 1)
			return files.get(0);

		List<SchemaFile> matching = new ArrayList<SchemaFile>();
		for (SchemaFile file : files)
			if (preference.equals(file.getNamespace()))
				matching.add(file);

		if (matching.size() == 1)
			return matching.get(0);
		if (matching.size() > 1) {
			problems.add(new Problem(ProblemType.AMBIGUOUS_BASE_SCHEMA, matching, names(matching)
					+ " all have the namespace " + preference + ", so the base CIM schema cannot be identified"));
			return null;
		}

		if (!files.isEmpty())
			problems.add(new Problem(ProblemType.NO_BASE_SCHEMA, files,
					"No base CIM schema is identified: no schema file is marked as the base, and none has the namespace "
							+ preference));
		return null;
	}

	/**
	 * Adds the base's header and its imports of every other file's headers.
	 *
	 * @return false if the base's header cannot be used
	 */
	private static boolean addImports(SchemaFile base, List<SchemaFile> files, OntModel imports,
			List<Problem> problems) {
		Set baseHeaders = headers(base.getModel());
		OntResource header;

		if (baseHeaders.size() > 1) {
			problems.add(new Problem(ProblemType.BASE_SCHEMA_HEADERS, Collections.singletonList(base),
					base.getName() + " declares more than one ontology header (" + uris(baseHeaders)
							+ "), so the base CIM ontology cannot be identified"));
			return false;
		}

		if (baseHeaders.size() == 1) {
			OntResource declared = (OntResource) baseHeaders.iterator().next();
			if (!declared.isURIResource() || declared.getURI().contains("#")) {
				problems.add(new Problem(ProblemType.BASE_SCHEMA_INVALID_URI, Collections.singletonList(base),
						base.getName() + "'s ontology header " + (declared.isURIResource() ? declared.getURI() : "has no URI")
								+ (declared.isURIResource() ? " contains '#'" : "")
								+ ", so it cannot be used to build the namespace"));
				return false;
			}
			header = imports.createResource(declared.getURI());
		} else {
			String namespace = base.getNamespace() != null ? base.getNamespace() : DEFAULT_SCHEMA_NAMESPACE;
			String uri = Translator.schemaOntologyUri(namespace);
			header = imports.createIndividual(uri, OWL2.Ontology);
			problems.add(new Problem(ProblemType.BASE_SCHEMA_NO_HEADER, Collections.singletonList(base),
					base.getName() + " declares no ontology header, so " + uri + ", built from its namespace "
							+ namespace + ", is used"));
		}

		for (SchemaFile file : files) {
			if (file == base)
				continue;
			for (Iterator it = headers(file.getModel()).iterator(); it.hasNext();) {
				OntResource other = (OntResource) it.next();
				if (!other.equals(header))
					header.addProperty(OWL2.imports, other);
			}
		}
		return true;
	}

	private static Set headers(OntModel model) {
		return model.listSubjectsWithProperty(RDF.type, OWL2.Ontology).toSet();
	}

	private static String names(List<SchemaFile> files) {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < files.size(); i++) {
			if (i > 0)
				text.append(i == files.size() - 1 ? " and " : ", ");
			text.append(files.get(i).getName());
		}
		return text.toString();
	}

	private static String uris(Set headers) {
		Set<String> uris = new LinkedHashSet<String>();
		for (Iterator it = headers.iterator(); it.hasNext();) {
			OntResource header = (OntResource) it.next();
			uris.add(header.isURIResource() ? header.getURI() : "a blank node");
		}
		return String.join(", ", uris);
	}

	/**
	 * A view of a schema file's graph without its {@code owl:imports}
	 * statements. The file's graph itself is not changed.
	 */
	static final class ImportsHiddenGraph extends WrappedGraph {
		private static final Node IMPORTS = OWL2.imports.asNode();

		private static final Filter<Triple> NOT_IMPORTS = new Filter<Triple>() {
			@Override
			public boolean accept(Triple triple) {
				return !IMPORTS.equals(triple.getPredicate());
			}
		};

		ImportsHiddenGraph(Graph graph) {
			super(graph);
		}

		@Override
		public ExtendedIterator<Triple> find(TripleMatch match) {
			return super.find(match).filterKeep(NOT_IMPORTS);
		}

		@Override
		public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
			return super.find(s, p, o).filterKeep(NOT_IMPORTS);
		}

		@Override
		public boolean contains(Node s, Node p, Node o) {
			if (IMPORTS.equals(p))
				return false;
			ExtendedIterator<Triple> it = find(s, p, o);
			try {
				return it.hasNext();
			} finally {
				it.close();
			}
		}

		@Override
		public boolean contains(Triple triple) {
			return contains(triple.getSubject(), triple.getPredicate(), triple.getObject());
		}
	}
}
