package au.com.langdale.cimtoole.test.ui;

import java.lang.reflect.Method;

import org.eclipse.core.resources.IFolder;
import org.eclipse.ui.IWorkbenchWizard;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Cache;
import au.com.langdale.cimtoole.project.Info;
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
	
	public final void testImportProfile() throws Exception {
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
		clearSchemaCache();
		dialog.finishPressed();
		
		long size = CIMToolPlugin.getCache().getOntologyWait(profile).size();
		assertTrue("expected profile size", size > 1000);
		
	}
	
	public final void testNewProfile() throws Exception {
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
		clearSchemaCache();
		dialog.finishPressed();
		
		long size = CIMToolPlugin.getCache().getOntologyWait(profile).size();
		assertTrue("expected profile size", size < 10);
		
	}

	/**
	 * Empties the cached merged schema model, so that the wizard has to load the
	 * schema while it runs, as it does when a user runs it before anything else
	 * has loaded the schema. The old model is disposed first so that
	 * it stops listening for cache changes.
	 */
	private void clearSchemaCache() throws Exception {
		IFolder folder = Info.getSchemaFolder(project);
		Object merged = folder.getSessionProperty(Cache.ONTMODEL);
		if (merged != null) {
			Method dispose = merged.getClass().getMethod("dispose");
			dispose.setAccessible(true);
			dispose.invoke(merged);
		}
		folder.setSessionProperty(Cache.ONTMODEL, null);
		assertNull("schema cache emptied", folder.getSessionProperty(Cache.ONTMODEL));
	}
}
