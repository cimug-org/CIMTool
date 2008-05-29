/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import au.com.langdale.cimtoole.builder.ProfileBuildlets;
import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.ui.binding.BooleanBinding;
import au.com.langdale.ui.binding.BooleanModel;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.plumbing.Template;

public final class Summary extends FurnishedEditor {
	private ProfileEditor master;
	private BooleanBinding buildlets;
	
	public Summary(String name, ProfileEditor master) {
		super(name);
		this.master = master;
		buildlets = new OptionsBinding();
	}

	public class OptionsBinding extends BooleanBinding {
		private BooleanModel[] flags = ProfileBuildlets.getAvailable(master);

		@Override
		protected BooleanModel[] getFlags() {
			return flags;
		}
	}
	
	private SelectionListener more = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
			// no action
		}
		
		public void widgetSelected(SelectionEvent e) {
			RefactorWizard wizard = new RefactorWizard(master);
			wizard.run();
			wizard.dispose();
		}
	};

	@Override
	protected Content createContent() {
		return new Content(master.getToolkit()) {
			

			@Override
			protected Template define() {
				return Form(
						Grid(
							Group(Label("Location:"), DisplayField("location")),
							Group(Label("Namespace:"), DisplayField("namespace")),
							Group(Row(PushButton("more", "Reorganize and Repair", more))), 
								//Row(
									// PushButton("remap", "Remap Profile", remap), 
									//PushButton("reorg", "Reorganize Profile", reorg),
									//CheckBox("refs", "all associations by reference"))),
							Group(Label("Build the following from this profile:")),
							Group(CheckboxTableViewer("buildlets", true))
						)
				);
			}

			@Override
			public Control realise(Composite parent) {
				Control form = super.realise(parent);
				buildlets.bind("buildlets", this);
				return form;
			}
			
//			private SelectionListener remap = new SelectionListener() {
//				public void widgetDefaultSelected(SelectionEvent e) {
//					// no action
//				}
//				
//				public void widgetSelected(SelectionEvent e) {
//					master.remap();
//				}
//			};
//			
//			private SelectionListener reorg = new SelectionListener() {
//				public void widgetDefaultSelected(SelectionEvent e) {
//					// no action
//				};
//				
//				public void widgetSelected(SelectionEvent e) {
//					master.reorganize(getButton("refs").getSelection());
//				};
//			};

			@Override
			public void refresh() {
				setTextValue("location", master.getFile().getFullPath().toString());
				setTextValue("namespace", master.getNamespace());
				// getButton("remap").setEnabled(master.isLoaded());					
				//getButton("reorg").setEnabled(master.isLoaded());
				getButton("more").setEnabled(master.isLoaded());
				getViewer("buildlets").getControl().setEnabled(master.isLoaded());
			}
		};
	}

}