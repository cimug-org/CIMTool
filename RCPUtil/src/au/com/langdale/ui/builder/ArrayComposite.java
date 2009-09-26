package au.com.langdale.ui.builder;


import java.util.ArrayList;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


public class ArrayComposite extends ScrolledComposite {

	private Assembly assembly;
	private Template template;
	private ArrayList elements = new ArrayList();
	private ArrayList models = new ArrayList();
	
	public ArrayComposite(Composite parent, int style, Assembly assembly, Template template) {
		super(parent, style);
		this.assembly = assembly;
		this.template = template;
		setExpandHorizontal(true);
		setExpandVertical(true);

		Composite content = assembly.getToolkit().createComposite(this);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(content);
		setContent(content);
	}
	
	public void setSize(int size) {
		if( size != elements.size()) {
			
			Composite content = (Composite) getContent();
		
			elements.ensureCapacity(size);
			models.ensureCapacity(size);
			
			while( size < elements.size()) {
				Assembly sub = (Assembly) elements.remove(size);
				sub.dispose();
				models.remove(size);
			}
			
			while( size > elements.size()) {
				Assembly sub = assembly.createSubAssembly(content, template);
				elements.add(sub);
				Control root = sub.getRoot();
				LayoutGenerator.defaultsFor(root).span(1, 1).applyTo(root);
				models.add(null);
			}
		}
	}
	
	public Assembly getAssembly(int ix) {
		return (Assembly) elements.get(ix);
	}
	
	public Object get(int ix) {
		return models.get(ix);
	}
	
	public Object set(int ix, Object model) {
		return models.set(ix, model);
	}
	
	public int size() {
		return elements.size();
	}
}
