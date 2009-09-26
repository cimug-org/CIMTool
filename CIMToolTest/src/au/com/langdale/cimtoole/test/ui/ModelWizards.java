package au.com.langdale.cimtoole.test.ui;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.ui.IWorkbenchWizard;

import au.com.langdale.cimtoole.test.WorkbenchTest;
import au.com.langdale.cimtoole.wizards.ImportModel;
import au.com.langdale.ui.builder.Assembly;

public class ModelWizards extends WorkbenchTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupSchema();
		setupProfile();
		reader = null;
	}

	public final void testCase1() throws Exception {
		final String TEST_CASE = "base_case.xml";
		testImportModel(new ImportModel(), TEST_CASE);
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		assertTrue("model created", model.exists() );
		assertTrue("diagnostics created", getModelRelated("diagnostic").exists() );
		
		createReader();
		Deferred any = find(reader, pattern(ANY, ANY, ANY));
		Deferred bv1 = find(reader, pattern(MODEL_NS + "bv1", A, uri(SCHEMA_NS + "BaseVoltage")));
		reader.run();
		assertTrue("model acquired", any.getCount() > 1);
		assertTrue("test instance present", bv1.getCount() == 1);
	}
	
	protected void testImportModel(IWorkbenchWizard wizard, String sample) {
		TestWizardDialog dialog = runWizard(wizard);
		pause();
		
		Assembly content = dialog.getContent();
		String path = getSmallCasesFolder() + sample;
		content.getText("source").setText(path);
		pause();
		
		content.getText("namespace").setText(MODEL_NS);
		pause();
		
		content.getCheckboxTableViewer("projects").setChecked(project, true);
		content.fireWidgetEvent();
		pause();
		
		content.getCheckboxTableViewer("profiles").setChecked(profile, true);
		content.fireWidgetEvent();
		pause();
		
		assertTrue(dialog.getCurrentPage().canFlipToNextPage());
		dialog.nextPressed();
		
		content = dialog.getContent();
		content.getText("filename").setText(MODEL_NAME);
		pause();
		
		assertTrue(wizard.canFinish());
		dialog.finishPressed();
	}

}
