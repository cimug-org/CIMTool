package au.com.langdale.cimtoole.views;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import au.com.langdale.jena.TreeModelBase;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;

public class DocView extends ProjectModelFollower {
	private Text text;
	private OntModel model;
	
	@Override
	public boolean ignoreNode(ModelNode node) {
		return false;
	}
	
	@Override
	public void previewTarget(Resource base) {
		text.setText("");
		if( model != null) {
			OntResource subject = model.createResource(base.asNode());
			String label = TreeModelBase.label(subject);
			text.append(label);
			text.append("\n\n");
			String descr = subject.getComment();
			if( descr != null)
				text.append(descr);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		text = new Text(parent, SWT.MULTI|SWT.WRAP);
		text.setBackground(JFaceResources.getResources(text.getDisplay()).createColor(new RGB(0xeb, 0xd4, 0x8a)));
		text.setEditable(false);
		listenToSelection(getSite().getPage());
	}

	@Override
	public void setFocus() {
		text.setFocus();
	}

	@Override
	public void selectModel(OntModel model) {
		this.model = model;
	}

}
