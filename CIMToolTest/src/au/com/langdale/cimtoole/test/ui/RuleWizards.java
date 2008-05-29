package au.com.langdale.cimtoole.test.ui;

import java.io.File;

import org.eclipse.ui.IWorkbenchWizard;

import au.com.langdale.cimtoole.test.WorkbenchTest;
import au.com.langdale.cimtoole.wizards.ImportHTMLRules;
import au.com.langdale.cimtoole.wizards.ImportIncRuleset;
import au.com.langdale.cimtoole.wizards.ImportRuleset;
import au.com.langdale.cimtoole.wizards.ImportXSDRules;
import au.com.langdale.cimtoole.wizards.NewHTMLRules;
import au.com.langdale.cimtoole.wizards.NewIncRuleset;
import au.com.langdale.cimtoole.wizards.NewRuleset;
import au.com.langdale.cimtoole.wizards.NewXSDRules;
import au.com.langdale.ui.builder.ContentBuilder;

public class RuleWizards extends WorkbenchTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupSchema();
		setupProfile();
	}
	
	public final void testImportRules() {
		testImportRules(new ImportRuleset(), SAMPLE_RULES);
		assertTrue("rules created", getRelated("split-rules").exists() );
	}
	
	public final void testNewEmptyRules() {
		testNewRules(new NewRuleset(), false);	
		assertTrue("rules created", getRelated("split-rules").exists() );
		assertEquals("rules empty", 0, new File(getRelated("split-rules").getLocation().toOSString()).length());
	}
	
	public final void testNewDefaultRules() {
		testNewRules(new NewRuleset(), true);	
		assertTrue("rules created", getRelated("split-rules").exists() );
		assertTrue("rules not empty", new File(getRelated("split-rules").getLocation().toOSString()).length() > 0);
	}
	
	public final void testImportIncRules() {
		testImportRules(new ImportIncRuleset(), SAMPLE_RULES);
		assertTrue("rules created", getRelated("inc-rules").exists() );
	}
	
	public final void testNewEmptyIncRules() {
		testNewRules(new NewIncRuleset(), false);	
		assertTrue("rules created", getRelated("inc-rules").exists() );
		assertEquals("rules empty", 0, new File(getRelated("inc-rules").getLocation().toOSString()).length());
	}
	
	public final void testNewDefaultIncRules() {
		testNewRules(new NewIncRuleset(), true);	
		assertTrue("rules created", getRelated("inc-rules").exists() );
		assertTrue("rules not empty", new File(getRelated("inc-rules").getLocation().toOSString()).length() > 0);
	}
	
	public final void testImportXSDRules() {
		testImportRules(new ImportXSDRules(), SAMPLE_XSD_RULES);
		assertTrue("rules created", getRelated("xsd-xslt").exists() );
	}
	
	public final void testNewEmptyXSDRules() {
		testNewRules(new NewXSDRules(), false);	
		assertTrue("rules created", getRelated("xsd-xslt").exists() );
		assertEquals("rules empty", 0, new File(getRelated("xsd-xslt").getLocation().toOSString()).length());
	}
	
	public final void testNewDefaultXSDRules() {
		testNewRules(new NewXSDRules(), true);	
		assertTrue("rules created", getRelated("xsd-xslt").exists() );
		assertTrue("rules not empty", new File(getRelated("xsd-xslt").getLocation().toOSString()).length() > 0);
	}
	
	public final void testImportHTMLules() {
		testImportRules(new ImportHTMLRules(), SAMPLE_HTML_RULES);
		assertTrue("rules created", getRelated("html-xslt").exists() );
	}
	
	public final void testNewEmptyHTMLRules() {
		testNewRules(new NewHTMLRules(), false);	
		assertTrue("rules created", getRelated("html-xslt").exists() );
		assertEquals("rules empty", 0, new File(getRelated("html-xslt").getLocation().toOSString()).length());
	}
	
	public final void testNewDefaultHTMLRules() {
		testNewRules(new NewHTMLRules(), true);	
		assertTrue("rules created", getRelated("html-xslt").exists() );
		assertTrue("rules not empty", new File(getRelated("html-xslt").getLocation().toOSString()).length() > 0);
	}
	
	protected void testImportRules(IWorkbenchWizard wizard, String sample) {
		TestWizardDialog dialog = runWizard(wizard);
		pause();
		
		ContentBuilder content = dialog.getContent();
		content.getText("source").setText(getSamplesFolder() + sample);
		pause();
		
		content.getCheckboxTableViewer("projects").setChecked(project, true);
		content.fireWidgetEvent();
		pause();
		
		content.getCheckboxTableViewer("profiles").setChecked(profile, true);
		content.fireWidgetEvent();
		pause();
		
		assertTrue(wizard.canFinish());
		dialog.finishPressed();
	}


	protected void testNewRules(IWorkbenchWizard wizard, boolean copy) {
		TestWizardDialog dialog = runWizard(wizard);
		pause();
		
		ContentBuilder content = dialog.getContent();
		content.getCheckboxTableViewer("projects").setChecked(project, true);
		content.fireWidgetEvent();
		pause();

		content.getCheckboxTableViewer("profiles").setChecked(profile, true);
		content.fireWidgetEvent();
		pause();
		
		content.getButton("copy").setSelection(copy);
		content.fireWidgetEvent();
		pause();
		
		assertTrue(wizard.canFinish());
		dialog.finishPressed();
	}
}
