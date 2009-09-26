package au.com.langdale.cimtoole.test;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;

import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.builder.FurnishedWizardPage;

public class WorkbenchTest extends ProjectTest {
    private static final int SIZING_WIZARD_WIDTH = 470;
    private static final int SIZING_WIZARD_HEIGHT = 550;
    private static int delay;
    
    {
    	try {
    		delay = Integer.parseInt(System.getenv("CIMTOOL_TEST_DELAY"));
    	}
    	catch( NumberFormatException ex) {
    		delay = 0;
    	}
    }
    
    protected IWorkbench workbench;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		workbench = PlatformUI.getWorkbench();
		workbench.showPerspective("org.eclipse.ui.resourcePerspective", workbench.getActiveWorkbenchWindow());
	}
	
	public static class TestWizardDialog extends WizardDialog {

		public TestWizardDialog(Shell parentShell, IWizard newWizard) {
			super(parentShell, newWizard);
		}

		@Override
		public void cancelPressed() {
			super.cancelPressed();
		}

		@Override
		public void finishPressed() {
			super.finishPressed();
		}

		@Override
		public void nextPressed() {
			super.nextPressed();
		}
		
		public Assembly getContent() {
			FurnishedWizardPage page = (FurnishedWizardPage) getCurrentPage();
			return page.getContent();
		}
	}
	
	protected TestWizardDialog runWizard(IWorkbenchWizard wizard) {
		wizard.init(workbench, StructuredSelection.EMPTY);
		Shell parent = workbench.getActiveWorkbenchWindow().getShell();
		TestWizardDialog dialog = new TestWizardDialog(parent, wizard);
		dialog.create();
        dialog.getShell().setSize(SIZING_WIZARD_WIDTH, SIZING_WIZARD_HEIGHT);	
        dialog.setBlockOnOpen(false);
        dialog.open();
        react();
		return dialog;
	}

	protected void pause() {
		react();
		if( delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				// pass
			}
		}
	}

	protected void react() {
		while(workbench.getDisplay().readAndDispatch());
	}

}
