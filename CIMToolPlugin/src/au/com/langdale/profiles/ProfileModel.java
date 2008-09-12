/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.Models;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.validation.LOG;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class ProfileModel extends JenaTreeModelBase {
	
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
			super.setOntModel(Models.merge(profileModel, backgroundModel));
	}

	public abstract class SortedNode extends ModelNode {

		@Override
		protected String collation() {
			return "0" + toString();
		}
	}

	public class CatalogNode extends SortedNode {
		private OntResource message;
		
		public CatalogNode(OntResource message) {
			this.message = message;
		}

		@Override
		protected void populate() {
			Iterator it = ProfileClass.getProfileClasses(profileModel, getOntModel());
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
			OntClass child = getOntModel().createClass(uri);
			child.addSuperClass(message);
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
			OntResource probe = getOntModel().createOntResource(uri);
			
			int ix = 1;
			while( probe.isClass()) {
				probe = getOntModel().createOntResource(uri + ix);
				ix++;
			}
			
			OntClass child = getOntModel().createClass(probe.getURI());
			child.addSuperClass(base);
			return child;
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return message;
		}
		
		@Override
		public String toString() {
			try {
				String path = new URI(namespace).getPath();
				while( path.endsWith("/"))
					path = path.substring(0, path.length()-1);
				return path.substring(path.lastIndexOf('/') + 1);
			} catch (URISyntaxException e) {
				return super.toString();
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
			
			// work around a Jena bug - exception thrown if by r.remove() if (r, p, rdf:nil)
			StmtIterator ii = profile.getSubject().listProperties();
			while (ii.hasNext()) {
				Statement s = ii.nextStatement();
				if( s.getObject().equals(RDF.nil)) {
					ii.close();
					s.remove();
					ii  = profile.getSubject().listProperties();
				}
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
		
		public void setName(String newName) {
			profile.getSubject().setLabel(newName, null);
			changed();
		}
		
		public boolean setStereotype(Resource stereo, boolean state) {
			if( profile.hasStereotype(stereo) == state)
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
		public OntClass getBaseClass() {
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
		public class ElementNode extends ProfileNode {
			/**
			 * A union member
			 */
			public class SubTypeNode extends NaturalNode {
			
				public SubTypeNode(ProfileClass profile) {
					super(profile);
				}
			
				@Override
				public void destroy() {
					ElementNode.this.destroy(this);
				}
			}

			private OntProperty prop;
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
		
			public OntProperty getBaseProperty() {
				return prop;
			}
			
			/**
			 * The CIM property or class of which this element is profile.
			 */
			@Override
			public OntResource getBase() {
				return prop;
			}
			
			public boolean isRequired() {
				return info.isRequired();
			}
		
			public boolean canBeRequired() {
				return info.canBeRequired();
			}
			
			public boolean isReference() {
				return profile.isReference();
			}
			
			public boolean isDatatype() {
				return prop.isDatatypeProperty();
			}
			
			public boolean isFunctional() {
				return info.isFunctional();
			}
			
			public boolean isUnbounded() {
				return info.getMaxCardinality() == Integer.MAX_VALUE;
			}
		
			public boolean isAlwaysFunctional() {
				return info.isAlwaysFunctional(); 
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
			
			public boolean setFunctional(boolean state) {
				if( state == info.isFunctional()) 
					return false;
				
				info.setMaxCardinality(state? 1:Integer.MAX_VALUE);
				changed();
				return true;
			}
			
			public boolean setUnbounded(boolean state) {
				boolean actual = info.getMaxCardinality() == Integer.MAX_VALUE;
				if(state == actual)
					return false;

				info.setMaxCardinality(state? Integer.MAX_VALUE:1);
				changed();
				return true;
			}
			
			public boolean setMaxCardinality(int card) {
				if(info.getMaxCardinality() == card || card != 1 && isAlwaysFunctional())
					return false;
				
				info.setMaxCardinality(card);
				changed();
				return true;
			}
			
			public boolean setMinCardinality(int card) {
				if(info.getMinCardinality() == card || card > 0 && ! info.canBeRequired())
					return false;
				
				info.setMinCardinality(card);
				changed();
				return true;
			}
			
			public boolean setRequired(boolean state) {
				if( state == info.isRequired() || state && ! info.canBeRequired())
					return false;
		
				info.setMinCardinality(state? 1:0);
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
			
			public String getName() {
				return label(subject);
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
				OntProperty prop = base.asProperty();
				OntResource child = profile.createAllValuesFrom(prop, true);
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
			profile.remove(child.getBase().asProperty());
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
				PropertyInfo info = profile.getPropertyInfo((OntProperty)it.next());
				
				// only add the child if a restriction identified its range class.
				if(info.getRange() != null)
					add(new ElementNode(info));
			}
		}
		
		private void populateClasses() {
			Iterator it = profile.getSuperClasses();
			
			while( it.hasNext()) {
				OntClass clss = (OntClass) it.next();
				SuperTypeNode node = new SuperTypeNode(new ProfileClass(clss));
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
	public class TypeNode extends NaturalNode {
		public TypeNode(ProfileClass profile) {
			super(profile);
		}

		@Override
		public Class getIconClass() {
			return profile.hasStereotype(UML.concrete)? RootElementNode.class: TypeNode.class;
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
			    Restriction res = (Restriction) it.next();
			    if( res.isSomeValuesFromRestriction()) {
			    	SomeValuesFromRestriction some = res.asSomeValuesFromRestriction();
			    	Resource type =  some.getSomeValuesFrom();
			    	if(type != null && type.canAs(OntClass.class)) {
			    		Node node = new MessageNode(new ProfileClass((OntClass)type.as(OntClass.class)));
			    		add(node);
			    	}
			    }
			}
		}
		
		@Override
		public OntResource create(OntResource type) {
			if( ! type.isClass())
				return null;
			OntClass child = profile.createSomeValuesFrom(MESSAGE.about, type);
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
	public interface AttributeNode {
		
	}
	
	/**
	 * A marker class returned by getIconClass() is the node is reference-like; 
	 *
	 */
	public interface ReferenceNode {
		
	}
	
	/**
	 * A marker class returned for concrete type nodes; 
	 *
	 */
	public interface RootElementNode {
		
	}
	
	/**
	 * The root should be a subclass of the generic Message class.
	 */
	@Override
	protected Node classify(OntResource root) throws ConversionException {
		if( root.equals(MESSAGE.Message)) 
			return new CatalogNode(root);
		
		OntClass clss = root.asClass();
		if( clss.hasSuperClass(MESSAGE.Message))
			return new EnvelopeNode(new ProfileClass(clss));
		
		return new TypeNode(new ProfileClass(clss));
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
