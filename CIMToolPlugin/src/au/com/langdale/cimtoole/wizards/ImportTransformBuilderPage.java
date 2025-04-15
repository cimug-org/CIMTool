package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.DisplayField;
import static au.com.langdale.ui.builder.Templates.DisplayFileField;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.ReadOnlyComboViewer;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.TextBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;
import au.com.langdale.cimtoole.registries.ProfileBuildletConfigUtils;
import au.com.langdale.cimtoole.registries.TransformType;
import au.com.langdale.ui.binding.ComboBinding;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.BuildersBinding;
import au.com.langdale.workspace.ResourceUI.LocalFileBinding;

public class ImportTransformBuilderPage extends FurnishedWizardPage {

	private static String[] extensions = new String[] { "*.xsl" };

	private LocalFileBinding filename = new LocalFileBinding(extensions, Validators.NONE);
	private LocalFileBinding includesFilename = new LocalFileBinding(extensions, Validators.NONE);
	private BuildersBinding builders;
	private TextBinding source = new TextBinding(Validators.OPTIONAL_EXTANT_FILE);
	private TextBinding includesFileSource = new TextBinding(Validators.OPTIONAL_EXTANT_FILE);
	private TextBinding ext = new TextBinding(Validators.NONE);
	private ComboBinding type;

	private File file;
	private File includesFile;

	private TransformBuildlet currentSelection;

	/**
	 * We do not want to accept spaces in a file extension.
	 */
	private VerifyListener verifyListener = new VerifyListener() {

		@Override
		public void verifyText(VerifyEvent e) {
			for (int i = 0; i < e.text.length(); i++) {
				if (Character.isWhitespace(e.text.charAt(i))) {
					e.doit = false;
					return;
				}
			}
		}
	};

	public ImportTransformBuilderPage() {
		super("main");
		initializeBindings();
	}

	protected void initializeBindings() {

		builders = new BuildersBinding() {

			@Override
			public void update() {
				super.update();
				String xslFileName = (String) getParent().getValue();
				File xslFile = new File(xslFileName);

				TransformBuildlet builder = null; // Initialize back to null...
				if (xslFile.exists()) {
					String builderKey = xslFile.getName().substring(0, xslFile.getName().indexOf("."));
					builder = ProfileBuildletConfigUtils.getTransformBuildlet(builderKey);
				}

				super.setValue(builder);
			}
		};

		type = new ComboBinding() {
			@Override
			protected Object getInput() {
				return Arrays.asList(TransformType.values());
			}

			@Override
			protected void configureViewer(StructuredViewer viewer) {
			}
		};
	}

	public String getBuilderKey() {
		String builderKey = (getXslFile() != null
				? getXslFile().getName().substring(0, getXslFile().getName().indexOf("."))
				: null);
		return builderKey;
	}

	public File getXslFile() {
		return file;
	}
	
	public File getXslIncludesFile() {
		return includesFile;
	}

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				return Grid(Group(DisplayFileField("source", "XSLT transform file:", extensions)),
						Group(DisplayFileField("includesfilesource", "XSLT include file:", extensions)),
						Group(Label("Existing XSLT Transform Builders")), Group(CheckboxTableViewer("builders")),
						Group(Label("Builder XSLT name:"), DisplayField("filename")),
						Group(Label("Includes file name:"), DisplayField("includesfilename")),
						Group(Label("Transform builder type:"), ReadOnlyComboViewer("type")),
						Group(Label("File extension of generated files:"), Field("ext")),
						Group(CheckBox("replace", "Replace existing builder")),
						Group(Label("")), //
						Group(HRule()), //
						Group(Label("note", 
								"No new XSL includes file has been specified where one had previously existed. Either\n" + 
							    "add an XSLT includes file or if one is no longer needed, delete the existing builder\n" + 
							    "using the 'Manage XSLT Transform Builders' dialog and then reimport again here.")));
			}

			@Override
			protected void addBindings() {
				source.bind("source", this);
				includesFileSource.bind("includesfilesource", this);
				builders.bind("builders", this, source);
				filename.bind("filename", this, source);
				includesFilename.bind("includesfilename", this, includesFileSource);
				type.bind("type", this);
				ext.bind("ext", this);
				addVerifyListener("ext", verifyListener);
			}

			@Override
			public String validate() {
				if (source.getText().length() == 0) {
					getLabel("note").setVisible(false);
					return "Please select an XSLT transform file to import (*.xsl)";
				}

				file = new File(source.getText());
				if (!file.exists()) {
					return "The XSL file entered does not exist at the specified location on the file system ["
							+ file.getAbsolutePath() + "]";
				} else {
					if (includesFileSource.getText() != null && !"".equals(includesFileSource.getText())) {
						includesFile = new File(includesFileSource.getText());
						if (!includesFile.exists()) {
							return "The XSL includes file entered does not exist at the specified location on the file system ["
									+ includesFile.getAbsolutePath() + "]";
						} else {
							getLabel("note").setVisible(false);
						}
					} else if ( //
							(includesFileSource.getText() == null || "".equals(includesFileSource.getText())) && //
							currentSelection != null && //
							currentSelection.getIncludesFile() != null) {
						getLabel("note").setVisible(true);
					}
				}
				
				if (type.getValue() == null)
					return "An builder type must be selected from the drop down.";

				if ((ext.getText().length() == 0) || ext.getText().startsWith(".")) {
					return "An extension without a leading period must be specified (e.g. xsd or draft-07.json)";
				} else {
					if (isFileExtAlreadyInUse(getBuilderKey(), ext.getText())) {
						return String.format(
								"Extension '%s' is already in use by a builder.  Either change the extension or add a prefix (e.g. 'custom.%s')",
								ext.getText(), ext.getText());
					}
				}

				boolean exists = ProfileBuildletConfigUtils.hasBuildlet(getBuilderKey());

				getButton("replace").setEnabled(exists);
				if (exists && !getButton("replace").getSelection())
					return "Check option to confirm replacing this existing builder.";

				return null;
			}

			@Override
			public void refresh() {
				TransformBuildlet buildlet = (TransformBuildlet) builders.getValue();

				if (buildlet == null || !buildlet.equals(currentSelection)) {
					if (buildlet != null) {
						filename.setValue(buildlet.getStyle() + ".xsl");
						includesFilename.setValue(buildlet.getIncludesFile());
						type.setValue(TransformType.toTransformType(buildlet));
						ext.setValue(buildlet.getFileExt());
						//
						filename.refresh();
						includesFilename.refresh();
						type.refresh();
						ext.refresh();
						//
						getButton("replace").setSelection(false);
						getButton("replace").setEnabled(true);
						//
						currentSelection = buildlet;
					} else if (currentSelection != null) {
						String xslFileName = (String) source.getValue();
						if (xslFileName != null) {
							int index = xslFileName.lastIndexOf(File.separator);
							xslFileName = xslFileName.substring((index < 0 ? 0 : index + File.separator.length()))
									+ ".xsl";
						}
						filename.setValue(xslFileName);
						includesFilename.setValue(currentSelection.getIncludesFile());
						type.setValue(null);
						ext.setValue(null);
						//
						filename.refresh();
						includesFilename.refresh();
						type.refresh();
						ext.refresh();
						//
						getButton("replace").setSelection(false);
						getButton("replace").setEnabled(false);
						//
						currentSelection = buildlet;
					} else {
						getButton("replace").setSelection(false);
						getButton("replace").setEnabled(false);
					}
				}
			}
		};
	}

	/**
	 * There can not be two buildlets with the same file extension. The following
	 * convenience method is used to determine if the extension passed in is already
	 * "taken".
	 * 
	 * @param builderKey The key of the builder to test.
	 * @param fileExt    The file extension to test for whether in use or not.
	 * 
	 * @return true of the extension is already in use; false otherwise
	 */
	private boolean isFileExtAlreadyInUse(String builderKey, String fileExt) {
		Map<String, TransformBuildlet> existingBuildets = ProfileBuildletConfigUtils.getTransformBuildlets();

		for (TransformBuildlet buildlet : existingBuildets.values()) {
			// Note that we don't perform the equals check on the extension IF this is an
			// existing builder as it is NOT a conflict
			if (!buildlet.getStyle().equals(builderKey) && buildlet.getFileExt().equals(fileExt)) {
				return true;
			}
		}
		return false;
	}

	public TransformBuildlet getTranformBuildlet() {
		TransformBuildlet buildlet = null;
		try {
			String includesFileName = (includesFile != null && !"".equals(includesFile.getName()) ? includesFile.getName() : null);
			TransformType transformType = (TransformType) type.getValue();
			switch (transformType) {
			case TEXT:
				buildlet = new TextBuildlet(getBuilderKey(), ext.getText(), includesFileName);
				break;
			case XSD:
				buildlet = new XSDBuildlet(getBuilderKey(), ext.getText(), includesFileName);
				break;
			case TRANSFORM:
				buildlet = new TransformBuildlet(getBuilderKey(), ext.getText(), includesFileName);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buildlet;
	}

}