package au.com.langdale.cimtoole.test.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWizard;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.test.WorkbenchTest;
import au.com.langdale.cimtoole.wizards.ImportProfile;
import au.com.langdale.cimtoole.wizards.NewProfile;
import au.com.langdale.ui.builder.Assembly;

public class ProfileWizards extends WorkbenchTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupSchema();
	}
	
	public final void testImportProfile() throws CoreException {
		IWorkbenchWizard wizard = new ImportProfile();
		TestWizardDialog dialog = runWizard(wizard);
		pause();
		Assembly content = dialog.getContent();
		content.getText("source").setText(getSamplesFolder() + SAMPLE_PROFILE);
		assertEquals("suggested file", SAMPLE_PROFILE, content.getText("filename").getText());
		pause();
		content.getCheckboxTableViewer("projects").setChecked(project, true);
		content.fireWidgetEvent();
		pause();
		assertTrue(wizard.canFinish());
		dialog.finishPressed();
		
		long size = CIMToolPlugin.getCache().getOntologyWait(profile).size();
		assertTrue("expected profile size", size > 1000);
		
	}
	
	public final void testNewProfile() throws CoreException {
		IWorkbenchWizard wizard = new NewProfile();
		TestWizardDialog dialog = runWizard(wizard);
		pause();
		Assembly content = dialog.getContent();
		content.getText("filename").setText(SAMPLE_PROFILE);
		pause();
		content.getCheckboxTableViewer("projects").setChecked(project, true);
		content.fireWidgetEvent();
		pause();
		assertTrue(wizard.canFinish());
		dialog.finishPressed();
		
		long size = CIMToolPlugin.getCache().getOntologyWait(profile).size();
		assertTrue("expected profile size", size < 10);
		
	}
}
