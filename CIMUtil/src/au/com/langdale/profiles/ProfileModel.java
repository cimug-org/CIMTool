/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.inference.LOG;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.UMLTreeModel.PackageNode;
import au.com.langdale.jena.UMLTreeModel.SubClassNode;
import au.com.langdale.jena.UMLTreeModel.SuperClassNode;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.xmi.UML;

import au.com.langdale.kena.Composition;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;

public class ProfileModel extends JenaTreeModelBase {

	public ProfileModel() {
	}

	private String namespace = "http://example.com/NoName#";
	private OntModel profileModel, backgroundModel;
	private Refactory refactory;

	public Refactory getRefactory() {
		return refactory;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setBackgroundModel(OntModel backgroundModel) {
		super.setOntModel(null);
		this.backgroundModel = backgroundModel;
		initModels();
	}

	public String getOntologyNamespace() {
		return (this.backgroundModel != null && this.backgroundModel.getValidOntology() != null
				? this.backgroundModel.getValidOntology().getURI()
				: "");
	}

	@Override
	public void setOntModel(OntModel profileModel) {
		super.setOntModel(null);
		this.profileModel = profileModel;
		if (profileModel != null) {
			OntResource ont = profileModel.getValidOntology();
			if (ont != null) {
				namespace = ont.getURI() + "#";
				setRootResource(ont);
			}
		}
		initModels();
	}

	private void initModels() {
		if (profileModel != null && backgroundModel != null) {
			OntModel fullModel = Composition.merge(profileModel, backgroundModel);
			super.setOntModel(fullModel);
			refactory = new Refactory(profileModel, fullModel);
		}
	}

	@Override
	protected List findResourcePathTo(FrontsNode symbol) {
		OntResource target = profileModel.createResource(symbol.asNode());
		if (getRoot() == null || target == null)
			return null;

		OntResource start = getRoot().getSubject();
		if (start == null)
			return null;

		ArrayList path = new ArrayList(6);
		path.add(target);

		while (!target.equals(start)) {
			OntResource parent;
			if (target.isClass() && target.isURIResource())
				parent = start;
			else {
				parent = findParent(target);
				while (parent != null && !parent.hasRDFType(OWL.Class))
					parent = findParent(parent);
			}
			if (parent == null)
				return null;
			path.add(parent);
			target = parent;
		}

		Collections.reverse(path);
		return path;
	}

	private FrontsNode[] steps = new FrontsNode[] { RDFS.subClassOf, OWL.allValuesFrom, RDF.first, RDF.rest,
			OWL.unionOf, OWL.oneOf };

	private OntResource findParent(OntResource target) {
		for (int ix = 0; ix < steps.length; ix++) {
			OntResource parent = target.getSubject(steps[ix]);
			if (parent != null)
				return parent;
		}

//		OntResource type = backgroundModel.createResource(target.asNode()).getResource(RDF.type);
//		if( type != null && type.hasProperty(UML.hasStereotype, UML.enumeration)) 
//			return profileModel.createResource(type.asNode());

		return null;
	}

	/**
	 * Interface for manipulating the cardinality of a Node.
	 * 
	 * Both TypeNodes (concrete classes) and ElementNodes (properties) have
	 * cardinality.
	 *
	 */
	public interface Cardinality {
		public int getMaxCardinality();

		public int getMinCardinality();

		public boolean setMaxCardinality(int max);

		public boolean setMinCardinality(int min);

		public boolean isMinVariable();

		public boolean isMaxVariable();
	}

	public abstract class SortedNode extends ModelNode {

		public ProfileModel getProfileModel() {
			return ProfileModel.this;
		}

		@Override
		protected String collation() {
			return "0" + toString();
		}

		public void setName(String newName) {
			getSubject().setLabel(newName, null);
			changed();
		}

		public void setComment(String text) {
			getSubject().setComment(text, null);
		}
		
		public void setAsciiDoc(String text) {
			getSubject().setProperty(UML.asciidoc, text, null);
		}

		protected abstract void create(Node node, SelectionOptions selectedOptions);

		protected void createAnon(Node node, SelectionOptions selectedOptions) {
			// Anon classes are never concrete...
			SelectionOptions anonSelectionOptions = selectedOptions.cloneAndRemove(SelectionOption.Concrete);
			create(node, anonSelectionOptions);
		}

		protected void createDeep(Node node, SelectionOptions selectionOptions) {
			create(node, selectionOptions);
		}

		protected abstract void destroy();

		public void profileRemove(Node node) {
			if (node instanceof SortedNode) {
				SortedNode target = (SortedNode) node;
				target.destroy();
				InfGraph ig = (InfGraph) getSubject().getOntModel().getGraph();
				ig.rebind();
				structureChanged();
			}
		}

		public void profileAddAll(Collection args, SelectionOptions selectionOptions) {
			for (Iterator it = args.iterator(); it.hasNext();)
				create((Node) it.next(), selectionOptions);
			structureChanged();
		}

		public void profileAddAllDeep(Collection args, SelectionOptions selectionOptions) {
			for (Iterator it = args.iterator(); it.hasNext();)
				createDeep((Node) it.next(), selectionOptions);
			getRoot().structureChanged();
		}

		public void profileAddAnon(Node node, SelectionOptions selectionOptions) {
			// Anon classes are never concrete...
			SelectionOptions anonSelectionOptions = selectionOptions.cloneAndRemove(SelectionOption.Concrete);
			createAnon(node, anonSelectionOptions);
			structureChanged();
		}

		public Collection profileExpandArgs(Node node) {
			Collection args = new ArrayList();
			ProfileModel.buildArguments(args, node);
			return args;
		}
	}

	public class CatalogNode extends SortedNode {
		private OntResource subject;

		public CatalogNode(OntResource message) {
			this.subject = message;
		}

		@Override
		protected void populate() {
			Iterator it = ProfileClass.getProfileClasses(profileModel, getOntModel());
			while (it.hasNext()) {
				ProfileClass profile = (ProfileClass) it.next();
				if (profile.getBaseClass() != null) {
					if (profile.getBaseClass().equals(MESSAGE.Message))
						add(new EnvelopeNode(profile));
					else
						add(new TypeNode(profile));
				}
			}
		}

		/**
		 * Create a new envelope class with the given URI.
		 */
		public OntResource profileAddEnvelope(String uri) {
			OntResource child = getOntModel().createClass(uri);
			child.addSuperClass(MESSAGE.Message);
			return child;
		}

		@Override
		protected void destroy() {
			// can't destroy this node

		}

		/**
		 * Create a new named class derived from the given class.
		 */
		@Override
		protected void create(Node node, SelectionOptions selectionOptions) {
			OntResource base = node.getSubject();
			if (base.isClass())
				getRefactory().createProfileClass(base, selectionOptions.isConcrete());
		}

		@Override
		protected void createDeep(Node node, SelectionOptions selectionOptions) {
			OntResource base = node.getSubject();
			if (base.isClass()) {
				getRefactory().createCompleteProfile(base, selectionOptions);
			}
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return subject;
		}

		public String abbrevNamespace() {
			try {
				String path = new URI(namespace).getPath();
				while (path.endsWith("/"))
					path = path.substring(0, path.length() - 1);
				return path.substring(path.lastIndexOf('/') + 1);
			} catch (URISyntaxException e) {
				return namespace;
			}
		}
	}

	/**
	 * Base for all nodes. Handles the creation of child nodes.
	 * 
	 *
	 */
	abstract public class ProfileNode extends SortedNode {
		protected ProfileNode(ProfileClass profile) {
			this.profile = profile;
		}

		protected ProfileClass profile;

		/**
		 * Remove all definitions associated with a profile.
		 *
		 */
		@Override
		protected void destroy() {

			// destroy dependent nodes
			Iterator it = iterator();
			while (it.hasNext()) {
				SortedNode node = (SortedNode) it.next();
				node.destroy();
			}

			profile.getSubject().remove();
		}

		/**
		 * The (usually anonymous) message element class.
		 */
		@Override
		public OntResource getSubject() {
			return profile.getSubject();
		}

		@Override
		public boolean getErrorIndicator() {
			return profile.getSubject().hasProperty(LOG.hasProblems);
		}

		public boolean setStereotype(Resource stereo, boolean state) {
			if (hasStereotype(stereo) == state)
				return false;
			profile.setStereotype(stereo, state);
			changed();
			return true;
		}

		public boolean hasStereotype(Resource stereo) {
			return profile.hasStereotype(stereo);
		}

		/**
		 * The CIM class on which this message element is based.
		 */
		public OntResource getBaseClass() {
			return profile.getBaseClass();
		}

		/**
		 * The CIM property or class of which this element is profile.
		 */
		@Override
		public OntResource getBase() {
			return profile.getBaseClass();
		}

		@Override
		public void structureChanged() {
			profile.analyse();
			super.structureChanged();
		}

		public boolean isEnumerated() {
			return profile.isEnumerated();
		}

		public boolean isCompound() {
			return profile.isCompound();
		}

		public ProfileClass getProfile() {
			return profile;
		}
	}

	public abstract class NaturalNode extends ProfileNode {

		/**
		 * A subordinate element in a message.
		 */
		public class ElementNode extends ProfileNode implements Cardinality {
			/**
			 * A union member
			 */
			public class SubTypeNode extends GeneralTypeNode {

				public SubTypeNode(ProfileClass profile) {
					super(profile);
				}

				@Override
				public boolean isPruned() {
					return true;
				}

				@Override
				protected void destroy() {
					ElementNode.this.getProfile().removeUnionMember(getSubject());
					if (profile.getSubject().isAnon())
						super.destroy();
				}
			}

			private OntResource prop;
			private PropertyInfo info;

			/**
			 * The element is defined by a property and the collection of restrictions
			 * applied to that property.
			 */
			public ElementNode(PropertyInfo info) {
				super(info.createProfileClass());
				this.info = info;
				prop = info.getProperty();
			}

			/**
			 * Override in concrete nodes to control sort order.
			 * 
			 * @return a String that will serve as the sort key.
			 */
			@Override
			protected String collation() {
				String base = toString();
				String key = base.toLowerCase() + base;
				if (isDatatype() || isEnumerated()) {
					if (getName().equals("mRID"))
						return "1" + key;
					else
						return "2" + key;
				} else
					return "3" + key;

			}

			@Override
			public Class getIconClass() {
				if (prop.isDatatypeProperty())
					return AttributeNode.class;
				else if (profile.isReference())
					return ReferenceNode.class;
				else
					return ElementNode.class;
			}

			@Override
			public String toString() {
				return getName() + " " + getCardString();
			}

			public String getCardString() {
				int max = getMaxCardinality();
				int min = getMinCardinality();
				return cardString(min) + ".." + cardString(max);
			}

			public OntResource getBaseProperty() {
				return prop;
			}

			/**
			 * The CIM property or class of which this element is profile.
			 */
			@Override
			public OntResource getBase() {
				return prop;
			}

			public boolean isReference() {
				return profile.isReference();
			}

			public boolean isDatatype() {
				return prop.isDatatypeProperty();
			}

			public boolean isMaxVariable() {
				return !info.isAlwaysFunctional();
			}

			public boolean isMinVariable() {
				return info.canBeRequired();
			}

			public int getMaxCardinality() {
				return info.getMaxCardinality();
			}

			public int getMinCardinality() {
				return info.getMinCardinality();
			}

			@Override
			public boolean getAllowsChildren() {
				return !prop.isDatatypeProperty();

			}

			@Override
			public boolean getErrorIndicator() {
				return prop.hasProperty(LOG.hasProblems) || super.getErrorIndicator();
			}

			public void setReference(boolean state) {
				profile.setReference(state);
				changed();
			}

			public boolean setMaxCardinality(int card) {
				if (getMaxCardinality() == card || card < getMinCardinality() || !isMaxVariable())
					return false;

				info.setMaxCardinality(card);
				changed();
				return true;
			}

			public boolean setMinCardinality(int card) {
				if (getMinCardinality() == card || card > getMaxCardinality() || !isMinVariable())
					return false;

				info.setMinCardinality(card);
				changed();
				return true;
			}

			@Override
			protected void populate() {
				populateUnion();
			}

			private void populateUnion() {
				for (Iterator jt = profile.getUnionMembers().iterator(); jt.hasNext();) {
					add(new SubTypeNode((ProfileClass) jt.next()));
				}
			}

			public Collection profileExpandArgs(Node node) {
				return Collections.singletonList(node);
			}

			@Override
			protected void create(Node node, SelectionOptions selectionOptions) {
				OntResource base = node.getSubject();
				if (base.isClass() && !isDatatype()) {
					OntResource member = getRefactory().findOrCreateNamedProfile(base);
					profile.addUnionMember(member);
				}
			}

			@Override
			protected void createAnon(Node node, SelectionOptions selectionOptions) {
				OntResource base = node.getSubject();
				if (base.isClass() && !isDatatype()) {
					profile.createUnionMember(base);
				}
			}

			@Override
			protected void destroy() {
				super.destroy();
				NaturalNode.this.getProfile().remove(getBase());
			}
		}

		/**
		 * A supertype of a root element or other supertype in a message
		 */
		public class SuperTypeNode extends GeneralTypeNode {

			public SuperTypeNode(ProfileClass profile) {
				super(profile);
			}

			@Override
			public Class getIconClass() {
				return SuperTypeNode.class;
			}

			@Override
			public boolean isPruned() {
				return true;
			}

			@Override
			protected void destroy() {
				NaturalNode.this.getProfile().removeSuperClass(getSubject());
			}
		}

		public class EnumValueNode extends SortedNode {
			private OntResource subject;

			public EnumValueNode(OntResource subject) {
				this.subject = subject;
			}

			@Override
			public boolean getErrorIndicator() {
				return subject.hasProperty(LOG.hasProblems);
			}

			@Override
			public OntResource getSubject() {
				return subject;
			}

			@Override
			protected void populate() {
				// there are no children
			}

			@Override
			public boolean getAllowsChildren() {
				return false;
			}

			@Override
			protected void destroy() {
				NaturalNode.this.getProfile().removeIndividual(getSubject());
			}

			@Override
			public void setName(String name) {
				// can't change the name
			}

			@Override
			public void setComment(String name) {
				// can't comment
			}

			@Override
			protected void create(Node node, SelectionOptions selectionOptions) {
				// can't add children
			}
		}

		protected NaturalNode(ProfileClass profile) {
			super(profile);
		}

		/**
		 * Create a child element in the underlying ontology.
		 */
		@Override
		protected void create(Node node, SelectionOptions selectionOptions) {
			OntResource base = node.getSubject();
			if (base.isProperty()) {
				profile.createAllValuesFrom(base, selectionOptions);
			} else if (base.hasRDFType(profile.getBaseClass())) {
				profile.addIndividual(base);
			}
		}

		@Override
		protected void createDeep(Node node, SelectionOptions selectionOptions) {
			OntResource prop = node.getSubject();
			if (prop.isProperty()) {
				profile.createAllValuesFrom(prop, selectionOptions);
				getRefactory().createDefaultRange(profile, prop);
			}
		}

		@Override
		protected void populate() {
			populateProps();
			populateClasses();
			populateIndividuals();
		}

		private void populateIndividuals() {
			Iterator it = profile.getIndividuals();
			while (it.hasNext()) {
				add(new EnumValueNode((OntResource) it.next()));
			}
		}

		/**
		 * Recognise property restrictions as defining child elements.
		 */
		private void populateProps() {
			Iterator it = profile.getProperties();

			while (it.hasNext()) {
				PropertyInfo info = profile.getPropertyInfo((OntResource) it.next());

				// only add the child if a restriction identified its range class.
				if (info.getRange() != null)
					add(new ElementNode(info));
			}
		}

		private void populateClasses() {
			Iterator it = profile.getSuperClasses();

			while (it.hasNext()) {
				OntResource clss = (OntResource) it.next();
				SuperTypeNode node = new SuperTypeNode(new ProfileClass(clss, namespace));
				add(node);
			}
		}
	}

	public abstract class GeneralTypeNode extends NaturalNode implements Cardinality {
		public GeneralTypeNode(ProfileClass profile) {
			super(profile);
		}

		@Override
		public Class getIconClass() {
			if (hasStereotype(UML.compound))
				return CompoundElementNode.class;
			else if (hasStereotype(UML.enumeration))
				return EnumElementNode.class;
			else if (hasStereotype(UML.concrete) && hasStereotype(UML.description))
				return DescriptorRootElementNode.class;
			else if (hasStereotype(UML.concrete))
				return RootElementNode.class;
			else if (profile.getSubject().isAnon())
				return AnonTypeNode.class;
			else
				return TypeNode.class;
		}

		public int getMaxCardinality() {
			return profile.getMaxCardinality();
		}

		public int getMinCardinality() {
			return profile.getMinCardinality();
		}

		public boolean isMaxVariable() {
			return hasStereotype(UML.concrete);
		}

		public boolean isMinVariable() {
			return hasStereotype(UML.concrete);
		}

		public boolean setMaxCardinality(int card) {
			if (getMaxCardinality() == card || card < getMinCardinality() || !isMaxVariable())
				return false;

			profile.setMaxCardinality(card);
			changed();
			return true;
		}

		public boolean setMinCardinality(int card) {
			if (getMinCardinality() == card || card > getMaxCardinality() || !isMinVariable())
				return false;

			profile.setMinCardinality(card);
			changed();
			return true;
		}
	}

	/**
	 * A root element in a message
	 */
	public class TypeNode extends GeneralTypeNode {
		public TypeNode(ProfileClass profile) {
			super(profile);
		}

		@Override
		protected void destroy() {
			getRefactory().remove(getSubject());
			super.destroy();
		}
	}

	/**
	 * The root node of a message.
	 */
	public class EnvelopeNode extends ProfileNode {

		/**
		 * A root element in an envelope
		 */
		public class MessageNode extends NaturalNode {
			public MessageNode(ProfileClass profile) {
				super(profile);
			}

			@Override
			public Class getIconClass() {
				return RootElementNode.class;
			}

			@Override
			protected void destroy() {
				super.destroy();
				EnvelopeNode.this.getProfile().remove(MESSAGE.about, this.getProfile().getSubject());
			}
		}

		public EnvelopeNode(ProfileClass profile) {
			super(profile);
		}

		@Override
		protected void populate() {
			Iterator it = profile.getRestrictions(MESSAGE.about);

			while (it.hasNext()) {
				OntResource res = (OntResource) it.next();
				if (res.isSomeValuesFromRestriction()) {
					OntResource type = res.getSomeValuesFrom();
					if (type != null && type.isClass()) {
						Node node = new MessageNode(new ProfileClass(type, namespace));
						add(node);
					}
				}
			}
		}

		@Override
		protected void create(Node node, SelectionOptions selectionOptions) {
			OntResource type = node.getSubject();
			if (!type.isClass())
				return;
			OntResource prop = profileModel.createOntProperty(MESSAGE.about.getURI());
			profile.createSomeValuesFrom(prop, type);
		}
	}

	/**
	 * A marker class returned by getIconClass() is the node is attribute-like;
	 *
	 */
	public interface AttributeNode {
	}

	/**
	 * A marker class returned by getIconClass() is the node is reference-like;
	 *
	 */
	public interface ReferenceNode {
	}

	/**
	 * A marker class returned for concrete type nodes that also are marked as
	 * 'descriptors';
	 *
	 */
	public interface DescriptorRootElementNode {
	}

	/**
	 * A marker class returned for concrete type nodes;
	 *
	 */
	public interface RootElementNode {
	}

	/**
	 * A marker class returned for compound type nodes;
	 *
	 */
	public interface CompoundElementNode {
	}

	/**
	 * A marker class returned for enumerated property nodes;
	 *
	 */
	public interface EnumElementNode {
	}

	/**
	 * A marker class returned for anonymous types.
	 */
	public interface AnonTypeNode {
	}

	/**
	 * The root should be a subclass of the generic Message class.
	 */
	@Override
	protected Node classify(OntResource root) {
		if (root.hasRDFType(OWL.Ontology))
			return new CatalogNode(root);

		if (root.hasSuperClass(MESSAGE.Message))
			return new EnvelopeNode(new ProfileClass(root, namespace));

		return new TypeNode(new ProfileClass(root, namespace));
	}

	private static void buildArguments(Collection args, Node node) {

		if ((node instanceof SubClassNode) || (node instanceof SuperClassNode) || (node instanceof PackageNode)) {

			Iterator it = node.iterator();
			while (it.hasNext())
				buildArguments(args, (Node) it.next());

		} else
			args.add(node);
	}

	public static String cardString(int card) {
		return cardString(card, "n");
	}

	public static String cardString(int card, String unbounded) {
		return card == Integer.MAX_VALUE ? unbounded : Integer.toString(card);
	}

	public static int cardInt(String symbol) {
		if (symbol.equals("n"))
			return Integer.MAX_VALUE;
		int card = Integer.parseInt(symbol);
		if (card < 0)
			throw new NumberFormatException();
		return card;
	}

}
