package au.com.langdale.cimtoole.properties;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.hp.hpl.jena.ontology.OntResource;

import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.profiles.ProfileModel.ProfileNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.ui.util.IconCache;

public class PropertySupport implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if( adapterType.equals(IPropertySource.class)) {
			if( adaptableObject instanceof Node) {
				return new PropertySource((Node)adaptableObject);
			}
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] {IPropertySource.class};
	}
	
	
	public static abstract class Descriptor implements IPropertyDescriptor {
		private String name, description;

		public Descriptor(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public Object getId() {
			return this;
		}

		public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
			return equals(anotherProperty);
		}

		public String getDisplayName() {
			return name;
		}

		public String getDescription() {
			return description;
		}
		
		public abstract Object getValueFrom(Node node);

		public CellEditor createPropertyEditor(Composite parent) {
			return null;
		}

		public String getCategory() {
			return "Info";
		}

		public String[] getFilterFlags() {
			return null;
		}

		public Object getHelpContextIds() {
			return null;
		}

		public ILabelProvider getLabelProvider() {
			return null;
		}
	}
	
	public static abstract class ModelDescriptor extends Descriptor {

		public ModelDescriptor(String name, String description) {
			super(name, description);
		}
		
		@Override
		public Object getValueFrom(Node node) {
			if( node instanceof ModelNode) 
				return getValueFrom((ModelNode)node);
			else
				return "";
		}
		
		public abstract Object getValueFrom(ModelNode node);
	}
	
	public static abstract class PropertyDescriptor extends Descriptor {

		public PropertyDescriptor(String name, String description) {
			super(name, description);
		}
		
		@Override
		public Object getValueFrom(Node node) {
			if( node instanceof ElementNode)
				return getValueFrom((ElementNode)node );
			else
				return "";
		}
		
		@Override
		public String getCategory() {
			return "Property";
		}
		
		public abstract Object getValueFrom(ElementNode node);
		
	}
	
	public static abstract class ProfileDescriptor extends Descriptor {

		public ProfileDescriptor(String name, String description) {
			super(name, description);
		}
		
		@Override
		public Object getValueFrom(Node node) {
			if( node instanceof ProfileNode)
				return getValueFrom((ProfileNode)node );
			else
				return "";
		}
		
		@Override
		public String getCategory() {
			return "Profile";
		}
		
		public abstract Object getValueFrom(ProfileNode node);
		
	}
	
	private static Descriptor[] basicDescriptors = {
		new Descriptor("Name", "Canonical name of definition") {
			@Override
			public Object getValueFrom(Node node) {
				return node.getName();
			}
		},
		new ModelDescriptor("Package", "Package or document containing definition") {
			@Override
			public Object getValueFrom(ModelNode node) {
				return node.getPackageName();
			}
		},
		new Descriptor("Type", "Type of definition") {
			@Override
			public Object getValueFrom(Node node) {
				return IconCache.getName( node.getIconClass());
			}
		},
		new Descriptor("URI", "Full URI of defintion") {
			@Override
			public Object getValueFrom(Node node) {
				OntResource subject = node.getSubject();
				if( subject.isURIResource())
					return subject.getURI();
				else
					return "anonymous";
			}
		},
		new ProfileDescriptor("Based on", "Name in information model") {
			@Override
			public Object getValueFrom(ProfileNode node) {
				return node.getBase().getLocalName();
			}
		},
		new ProfileDescriptor("Base namespace", "Namespace in information model") {
			@Override
			public Object getValueFrom(ProfileNode node) {
				return node.getBase().getNameSpace();
			}
		},
		new PropertyDescriptor("Cardinality", "Property cardinality") {
			@Override
			public Object getValueFrom(ElementNode node) {
				return node.getCardString();
			}
		},
	};
	
	public static void copy(Descriptor[] a, Descriptor[] b, int offset) {
		for(int ix = 0; ix < a.length; ix++)
			b[ix + offset] = a[ix];
	}
	
	public static Descriptor[] concat(Descriptor[] a, Descriptor[] b) {
		Descriptor[] c = new Descriptor[a.length + b.length];
		copy(a, c, 0);
		copy(b, c, a.length);
		return c;
	}
	
	public static class PropertySource implements IPropertySource {
		
		private Node node;

		public PropertySource(Node node) {
			this.node = node;
		}

		public IPropertyDescriptor[] getPropertyDescriptors() {
			return basicDescriptors;
		}

		public Object getPropertyValue(Object id) {
			if(id instanceof Descriptor) {
				Descriptor desc = (Descriptor) id;
				return desc.getValueFrom(node);
			}
			return null;
		}

		public Object getEditableValue() {
			// not editable
			return null;
		}

		public boolean isPropertySet(Object id) {
			// default value not applicable therefore this is false
			return false;
		}

		public void resetPropertyValue(Object id) {
			// no action	
		}

		public void setPropertyValue(Object id, Object value) {
			// no action
		}
	}

	public static String getDescription(Node node) {
		StringBuffer result = new StringBuffer();
		for(int ix = 0; ix < basicDescriptors.length; ix++) {
			Descriptor line = basicDescriptors[ix];
			result.append(line.getDisplayName());
			result.append(": ");
			result.append(line.getValueFrom(node));
			result.append('\n');
		}
		return result.toString();
	}
}
