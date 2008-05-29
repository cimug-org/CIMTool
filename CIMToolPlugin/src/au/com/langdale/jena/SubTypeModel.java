package au.com.langdale.jena;

import au.com.langdale.jena.UMLTreeModel.SubClassNode;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class SubTypeModel extends FilteredTreeModel {
	private UMLTreeModel inner = new UMLTreeModel();
	
	@Override
	protected boolean filter(Node node) {
		return node instanceof SubClassNode;
	}

	public Resource getRootResource() {
		return inner.getRootResource();
	}

	public void setOntModel(Model model) {
		inner.setOntModel(model);
		setBaseNode((Node)inner.getRoot());
	}

	public void setRootResource(Resource root) {
		inner.setRootResource(root);
		setBaseNode((Node)inner.getRoot());
	}

	@Override
	public void setBaseNode(Node base) {
		if( base != null)
			super.setBaseNode(base);
		else
			setRoot(null);
	}
}
