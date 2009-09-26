/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.validation.LOG;
import au.com.langdale.xmi.UML;

import au.com.langdale.kena.Composition;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;

public class ProfileModel extends JenaTreeModelBase {
	
	public ProfileModel() {}
	
	private String namespace = "http://example.com/NoName#";
	private OntModel profileModel, backgroundModel;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	public void setBackgroundModel(OntModel backgroundModel) {
		this.backgroundModel = backgroundModel;
		initModels();
	}
	
	@Override
	public void setOntModel(OntModel profileModel) {
		this.profileModel = profileModel;
		initModels();
	}
	
	private void initModels() {
		if( profileModel != null && backgroundModel != null)
			super.setOntModel(Composition.merge(profileModel, backgroundModel));
	}
	
	/**
	 * Interface for manipulating the cardinality of a Node.
	 * 
	 * Both TypeNodes (concrete classes) and ElementNodes (properties)
	 * have cardinality.
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
	}
	
	public class CatalogNode extends SortedNode {
		private OntResource subject;
		
		public CatalogNode(OntResource message) {
			this.subject = message;
		}

		@Override
		protected void populate() {
			Iterator it = ProfileClass.getProfileClasses(profileModel, getOntModel(), namespace);
			while( it.hasNext()) {
				ProfileClass profile = (ProfileClass) it.next();
				if( profile.getBaseClass() != null) {
					if( profile.getBaseClass().equals(MESSAGE.Message) )
						add( new EnvelopeNode(profile));
					else 
						add( new TypeNode(profile));
				}
			}
		}

		/**
		 * Create a new envelope class with the given URI. 
		 */
		public OntResource create(String uri) {
			OntResource child = getOntModel().createClass(uri);
			child.addSuperClass(MESSAGE.Message);
			return child;
		}

		/**
		 * Create a new named class derived from the given class.
		 */
		@Override
		public OntResource create(OntResource base) {
			if( ! base.isClass())
				return null;
			
			String uri = getNamespace() + base.getLocalName();
			OntResource probe = getOntModel().createResource(uri);
			
			int ix = 1;
			while( probe.isClass()) {
				probe = getOntModel().createResource(uri + ix);
				ix++;
			}
			
			OntResource child = getOntModel().createClass(probe.getURI());
			child.addSuperClass(base);
			return child;
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
				while( path.endsWith("/"))
					path = path.substring(0, path.length()-1);
				return path.substring(path.lastIndexOf('/') + 1);
			} catch (URISyntaxException e) {
				return namespace;
			}
		}
	}
	
	/**
	 * Base for all nodes.  Handles the creation
	 * of child nodes.
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
		public void destroy() {
			
			// destroy dependent nodes
			Iterator it = iterator();
			while(it.hasNext()) {
				Node node = (Node) it.next();
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
			if( hasStereotype(stereo) == state)
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

		public ProfileClass getProfile() {
			return profile;
		}
	}
	
	public abstract class NaturalNode extends ProfileNode {

		/**
		 * A subordinate element in a message.
		 * 
		 *
		 */
		public class ElementNode extends ProfileNode implements Cardinality {
			/**
			 * A union member
			 */
			public class SubTypeNode extends NaturalNode {
			
				public SubTypeNode(ProfileClass profile) {
					super(profile);
				}
				
				@Override
				public boolean isPruned() {
					return true;
				}
			
				@Override
				public void destroy() {
					ElementNode.this.destroy(this);
					if( profile.getSubject().isAnon())
						super.destroy();
				}
			}

			private OntResource prop;
			private PropertyInfo info;
		
			/**
			 * The element is defined by a property and the 
			 * collection of restrictions applied to that property.
			 */
			public ElementNode(PropertyInfo info) {
				super( info.createProfileClass());
				this.info = info;
				prop = info.getProperty();
			}
			
			/**
			 * Override in concrete nodes to control sort order.
			 * @return a String that will serve as the sort key.
			 */
			@Override
			protected String collation() {
				String base = toString();
				String key = base.toLowerCase() + base;
				if(isDatatype() || isEnumerated()) {
					if( getName().equals("mRID"))
						return "1" + key;
					else
						return "2" + key;
				}
				else
					return "3" + key;
				
			}
			
			@Override
			public Class getIconClass() {
				if( profile.isReference())
					return ReferenceNode.class;
				else if(prop.isDatatypeProperty())
					return AttributeNode.class;
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
				return ! info.isAlwaysFunctional();
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
				return 	! prop.isDatatypeProperty();
		
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
				if(getMaxCardinality() == card || card < getMinCardinality() || ! isMaxVariable())
					return false;
				
				info.setMaxCardinality(card);
				changed();
				return true;
			}
			
			public boolean setMinCardinality(int card) {
				if(getMinCardinality() == card || card > getMaxCardinality() || ! isMinVariable())
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
					add( new SubTypeNode((ProfileClass) jt.next()));
				}
			}
			
			@Override
			public OntResource create(OntResource base) {
				if( base.isClass() && ! isDatatype()) {
					OntResource child = profile.createUnionMember(base, true);
					return child;
				} else 
					return null;
			}
			
			protected void destroy(SubTypeNode child) {
				profile.removeUnionMember(child.getSubject());
			}

			@Override
			public void destroy() {
				super.destroy();
				NaturalNode.this.destroy(this);
			}
		}

		/**
		 * A supertype of a root element or other supertype in a message
		 */
		public class SuperTypeNode extends NaturalNode {
		
			public SuperTypeNode(ProfileClass profile) {
				super(profile);
			}
			
			@Override
			public boolean isPruned() {
				return true;
			}
		
			@Override
			public void destroy() {
				NaturalNode.this.destroy(this);
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
			public void destroy() {
				NaturalNode.this.destroy(this);
			}
			
			@Override
			public void setName(String name) {
				// TODO: to be implemented
			}
			
			@Override
			public void setComment(String name) {
				// TODO: to be implemented
			}
		}

		protected NaturalNode(ProfileClass profile) {
			super(profile);
		}

		/**
		 * Create a child element in the underlying ontology.
		 */
		@Override
		public OntResource create(OntResource base) {
			if( base.isProperty()) {
				OntResource child = profile.createAllValuesFrom(base, true);
				return child;
			}
			else if( base.hasRDFType(profile.getBaseClass())) {
				profile.addIndividual(base);
				return base;
			}
			else {
				return null;
			}
		}

		protected void destroy(ElementNode child) {
			profile.remove(child.getBase());
		}
		
		@Override
		protected void populate() {
			populateProps();
			populateClasses();
			populateIndividuals();
		}

		private void populateIndividuals() {
			Iterator it = profile.getIndividuals();
			while(it.hasNext()) {
				add( new EnumValueNode((OntResource)it.next()));
			}
		}

		/**
		 * Recognise property restrictions as defining child elements.
		 */
		private void populateProps() {
			Iterator it = profile.getProperties();
			
			while( it.hasNext()) {
				PropertyInfo info = profile.getPropertyInfo((OntResource)it.next());
				
				// only add the child if a restriction identified its range class.
				if(info.getRange() != null)
					add(new ElementNode(info));
			}
		}
		
		private void populateClasses() {
			Iterator it = profile.getSuperClasses();
			
			while( it.hasNext()) {
				OntResource clss = (OntResource) it.next();
				SuperTypeNode node = new SuperTypeNode(new ProfileClass(clss, namespace));
				add(node);
			}
		}

		protected void destroy(EnumValueNode child) {
			profile.removeIndividual(child.getSubject());
		}
		
		protected void destroy(SuperTypeNode child) {
			profile.removeSuperClass(child.getSubject());
		}
	}
	
	/**
	 * A root element in a message
	 */
	public class TypeNode extends NaturalNode implements Cardinality {
		public TypeNode(ProfileClass profile) {
			super(profile);
		}

		@Override
		public Class getIconClass() {
			if(hasStereotype(UML.concrete))
				return RootElementNode.class;
			else if(hasStereotype(UML.compound))
				return CompoundElementNode.class;
			else if(hasStereotype(UML.enumeration))
				return EnumElementNode.class;
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
			if(getMaxCardinality() == card || card < getMinCardinality() || ! isMaxVariable())
				return false;
			
			profile.setMaxCardinality(card);
			changed();
			return true;
		}

		public boolean setMinCardinality(int card) {
			if(getMinCardinality() == card || card > getMaxCardinality() || ! isMinVariable())
				return false;
			
			profile.setMinCardinality(card);
			changed();
			return true;
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
			public void destroy() {
				super.destroy();
				EnvelopeNode.this.destroy(this);
			}
		}
		
		public EnvelopeNode(ProfileClass profile) {
			super(profile);
		}


		@Override
		protected void populate() {
			Iterator it = profile.getRestrictions(MESSAGE.about);
			
			while(it.hasNext()) {
			    OntResource res = (OntResource) it.next();
			    if( res.isSomeValuesFromRestriction()) {
			    	OntResource type =  res.getSomeValuesFrom();
			    	if(type != null && type.isClass()) {
			    		Node node = new MessageNode(new ProfileClass(type, namespace));
			    		add(node);
			    	}
			    }
			}
		}
		
		@Override
		public OntResource create(OntResource type) {
			if( ! type.isClass())
				return null;
			OntResource prop = profileModel.createOntProperty(MESSAGE.about.getURI());
			OntResource child = profile.createSomeValuesFrom(prop, type);
			return child;
		}
		/**
		 * Remove child message element associated with the given class.
		 */
		protected void destroy(MessageNode child) {
			profile.remove(MESSAGE.about, child.getProfile().getSubject());
		}

		@Override
		public void destroy() {
			super.destroy();
		}
	}
	
	/**
	 * A marker class returned by getIconClass() is the node is attribute-like; 
	 *
	 */
	public interface AttributeNode {}
	
	/**
	 * A marker class returned by getIconClass() is the node is reference-like; 
	 *
	 */
	public interface ReferenceNode {}
	
	/**
	 * A marker class returned for concrete type nodes; 
	 *
	 */
	public interface RootElementNode {}
	
	/**
	 * A marker class returned for compound type nodes; 
	 *
	 */
	public interface CompoundElementNode {}
	
	/**
	 * A marker class returned for compound type nodes; 
	 *
	 */
	public interface EnumElementNode {}
	
	/**
	 * The root should be a subclass of the generic Message class.
	 */
	@Override
	protected Node classify(OntResource root) {
		if( root.equals(MESSAGE.profile)) 
			return new CatalogNode(root);
		
		if( root.hasSuperClass(MESSAGE.Message))
			return new EnvelopeNode(new ProfileClass(root, namespace));
		
		return new TypeNode(new ProfileClass(root, namespace));
	}
	
	public static String cardString(int card) {
		return cardString(card, "n");
	}
	
	public static String cardString(int card, String unbounded) {
		return card == Integer.MAX_VALUE? unbounded: Integer.toString(card);
	}
	
	public static int cardInt(String symbol) {
		if(symbol.equals("n")) 
			return Integer.MAX_VALUE;
		int card = Integer.parseInt(symbol);
		if( card < 0)
			throw new NumberFormatException();
		return card;
	}


}
