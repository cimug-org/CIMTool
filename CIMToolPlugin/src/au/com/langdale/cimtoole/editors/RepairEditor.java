/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors;

import static au.com.langdale.ui.builder.Templates.Array;
import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.Column;
import static au.com.langdale.ui.builder.Templates.DisplayArea;
import static au.com.langdale.ui.builder.Templates.DisplayField;
import static au.com.langdale.ui.builder.Templates.Form;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Image;
import static au.com.langdale.ui.builder.Templates.Stack;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.kena.OntResource;
import au.com.langdale.ui.builder.ArrayComposite;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.validation.DiagnosisModel;
import au.com.langdale.validation.DiagnosisModel.DetailNode;
import au.com.langdale.validation.DiagnosisModel.RepairNode;
import au.com.langdale.validation.RepairMan;

public class RepairEditor extends ModelEditor {

	private DiagnosisModel tree;

	@Override
	public JenaTreeModelBase getTree() {
		if( tree == null ) {
			tree = new DiagnosisModel(new RepairMan());
			//tree.setRootResource(DiagnosisModel.DIAGNOSIS_ROOT);
			tree.setSource(getFile().getFullPath().toString());
			modelCached(null);
		}
		return tree;
	}

	public void modelCached(IResource key) {
		tree.setOntModel(models.getOntology(getFile()));
		tree.setRootResource(DiagnosisModel.DIAGNOSIS_ROOT);
		tree.setRepairs(new RepairMan());
		doRefresh();
	}

	public void modelDropped(IResource key) {
		close();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		RepairMan repairs = tree.getRepairs();
		tree.setRepairs(new RepairMan());
		try {
			ResourcesPlugin.getWorkspace().run(Task.repairProfile(getFile(), repairs), monitor);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		super.doSave(monitor);
	}
	
	@Override
	protected void createPages() {
		addPage(main);
	}
	
	FurnishedEditor main = new FurnishedEditor("Diagnostics") {

		@Override
		protected Content createContent() {
			return new Content(getToolkit()) {

				@Override
				protected Template define() {
					return Form(
						Stack(
							Array("problems", 
								Column(
									Grid(Group(Image("symbol", "problem"), DisplayArea("problem", 3))),
									Array("corrections", 
										Grid(Group(
											CheckBox("check", "Select this repair action.", "right"),
											DisplayArea("correction",2))))
								)
							),
							DisplayField("summary")
						)
					);
				}

				@Override
				public void refresh() {
					Node node = getNode();
					
					
					if( node  instanceof ModelNode) {
						OntResource subject = node.getBase();
						getForm().setImage(IconCache.getIcons().get("key", 32));
						getForm().setText(DiagnosisModel.label(subject));
						showStackLayer("problems");
//						setTextValue("uri", subject.isAnon()? "": subject.getURI());

						if( node instanceof RepairNode ) {
							show((DetailNode)node.getParent(), Collections.singletonList(node));
						}
						else if( node instanceof DetailNode ) {
							show((DetailNode)node, node.getChildren());
						}
						else {
							show(node.getChildren());
						}
						getForm().layout(true, true);
					}
					else {
						getForm().setImage(IconCache.getIcons().get("general", 32));
						getForm().setText(getFile().getName());
						showStackLayer("summary");
//						setTextValue("uri", "");
						setTextValue("summary", "There are " + node.getChildren().size() +
							" problems found.  Select an item in the outline for more information.");
					}
				}

				private void show(List problems) {
					ArrayComposite probArray = getArrayComposite("problems");
					probArray.setSize(problems.size());
					int ix = 0;
					for( Iterator it = problems.iterator(); it.hasNext(); ) {
						DetailNode problem = (DetailNode) it.next();
						show(problem, problem.getChildren(), probArray.getAssembly(ix));
						ix++;
					}
				}

				private void show(DetailNode problem,	List repairs) {
					ArrayComposite probArray = getArrayComposite("problems");
					probArray.setSize(1);
					show(problem, repairs, probArray.getAssembly(0));
				}

				private void show(DetailNode problem, List repairs, Assembly assembly) {
					assembly.setTextValue("problem", problem.getDescription());
					assembly.setIconForObject("symbol", problem);
					ArrayComposite repArray = assembly.getArrayComposite("corrections");
					repArray.setSize(repairs.size());
					int ix = 0;
					for( Iterator it = repairs.iterator(); it.hasNext(); ) {
						RepairNode repair = (RepairNode) it.next();
						show(repair, repArray.getAssembly(ix));
						repArray.set(ix, repair);
						ix++;
					}
				}

				private void show(RepairNode repair, Assembly assembly) {
					assembly.setTextValue("correction", repair.getDescription());
					assembly.getButton("check").setSelection(repair.isSelected());
					assembly.setIconForObject("check", repair);
				}
				
				public void update() {
					ArrayComposite probArray = getArrayComposite("problems");
					for(int ix = 0; ix < probArray.size(); ix++) {
						collect(probArray.getAssembly(ix).getArrayComposite("corrections"));
					}
				}

				private void collect(ArrayComposite repArray) {
					for(int ix = 0; ix < repArray.size(); ix++) {
						Assembly assembly = repArray.getAssembly(ix);
						boolean selected = assembly.getButton("check").getSelection();
						RepairNode repair = (RepairNode) repArray.get(ix);
						if(selected && ! repair.isSelected()) {
							repair.setSelected(true);
							return;
						}
						if( ! selected && repair.isSelected()) {
							repair.setSelected(false);
						}
					}
				}
			};
		}
	};
}
