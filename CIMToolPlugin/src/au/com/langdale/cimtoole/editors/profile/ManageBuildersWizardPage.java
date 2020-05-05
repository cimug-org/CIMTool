package au.com.langdale.cimtoole.editors.profile;

import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.DisplayField;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.RadioButton;
import static au.com.langdale.ui.builder.Templates.ReadOnlyComboViewer;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.JSONBuildlet;
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

public class ManageBuildersWizardPage extends FurnishedWizardPage {

	public enum Action {
		UNSELECTED, UPDATE, DELETE
	}

	private BuildersBinding builders;
	private TextBinding filename = new TextBinding(Validators.NONE);
	private ComboBinding type;
	private TextBinding ext = new TextBinding(Validators.NONE);

	private TransformBuildlet currentSelection;
	private Action selectedAction = Action.UNSELECTED;

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

	private class ActionSelectionListener extends SelectionAdapter {
		private Action selectionType;

		public ActionSelectionListener(Action type) {
			this.selectionType = type;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			Button source = (Button) e.getSource();
			if (source.getSelection()) {
				selectedAction = selectionType;
			}
		}
	};

	public ManageBuildersWizardPage() {
		super("main");
		setTitle("Manage XSLT Transform Builders");
		// setDescription("Manage XSLT transform builders.");
		initializeBindings();
	}

	protected void initializeBindings() {

		builders = new BuildersBinding() {
			@Override
			public String validate() {
				if (!allow(getValue()))
					return "A builder must be selected.";
				else
					return null;
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

	private String getBuilderKey() {
		return (currentSelection != null ? currentSelection.getStyle() : null);
	}

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				return Grid( //
						Group(Label("Existing XSLT Transform Builders:")), Group(CheckboxTableViewer("builders")),
						Group(Label("Builder XSLT name:"), DisplayField("filename")),
						Group(Label("Transform builder type:"), ReadOnlyComboViewer("type")),
						Group(Label("File extension of generated files:"), Field("ext")),
						Group(RadioButton(Action.UNSELECTED.name(), "No action selected")),
						Group(RadioButton(Action.UPDATE.name(), "Update existing builder *")),
						Group(RadioButton(Action.DELETE.name(), "Delete existing builder")), Group(Label("")), //
						Group(HRule()), //
						Group(Label(
								"* To completely replace the XSLT transform for a builder you must reimport the builder \n"
										+ "using the \"Import XSLT Transform Builder\" import wizard.")));
			}

			@Override
			protected void addBindings() {
				builders.bind("builders", this);
				filename.bind("filename", this);
				type.bind("type", this);
				ext.bind("ext", this);
				addVerifyListener("ext", verifyListener);
				addListener(Action.UNSELECTED.name(), new ActionSelectionListener(Action.UNSELECTED));
				addListener(Action.UPDATE.name(), new ActionSelectionListener(Action.UPDATE));
				addListener(Action.DELETE.name(), new ActionSelectionListener(Action.DELETE));
			}

			@Override
			public String validate() {
				if (type.getValue() == null)
					return "An builder type must be selected from the drop down.";

				if ((ext.getText().length() == 0) || ext.getText().startsWith(".")) {
					return "An extension without a leading period must be specified (e.g. xsd or draft-07.json)";
				} else {
					if (currentSelection != null && isFileExtAlreadyInUse(getBuilderKey(), ext.getText())) {
						return String.format(
								"Extension '%s' is already in use by a different builder.  Change the extension or add a prefix (e.g. draft-07.json)",
								ext.getText());
					}
				}

				if (getButton(Action.UNSELECTED.name()).getSelection())
					return "An update or delete action must be selected.";

				return null;
			}

			@Override
			public void refresh() {
				TransformBuildlet buildlet = (TransformBuildlet) builders.getValue();
				if (buildlet != currentSelection) {
					if (buildlet != null) {
						filename.setValue(buildlet.getStyle() + ".xsl");
						type.setValue(TransformType.toTransformType(buildlet));
						ext.setValue(buildlet.getFileExt());
						//
						filename.refresh();
						type.refresh();
						ext.refresh();
						//
						getButton(Action.UNSELECTED.name()).setSelection(true);
						getButton(Action.UPDATE.name()).setSelection(false);
						getButton(Action.DELETE.name()).setSelection(false);
						//
						currentSelection = buildlet;
					} else {
						filename.setValue(null);
						type.setValue(null);
						ext.setValue(null);
						//
						filename.refresh();
						type.refresh();
						ext.refresh();
						//
						getButton(Action.UNSELECTED.name()).setSelection(true);
						getButton(Action.UPDATE.name()).setSelection(false);
						getButton(Action.DELETE.name()).setSelection(false);
						//
						currentSelection = buildlet;
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
	protected boolean isFileExtAlreadyInUse(String builderKey, String fileExt) {
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

	public Action getSelectedAction() {
		return selectedAction;
	}

	public TransformBuildlet getTranformBuildlet() {
		/**
		 * For maintenance of a builder we don't want to change the date time created
		 * timestamp. That is only relevant to imports and does not change during updates.
		 */
		TransformBuildlet buildlet = null;
		try {
			TransformType transformType = (TransformType) type.getValue();
			switch (transformType) {
			case JSON:
				buildlet = new JSONBuildlet(getBuilderKey(), ext.getText(),
						currentSelection.getDateTimeCreated());
				break;
			case TEXT:
				buildlet = new TextBuildlet(getBuilderKey(), ext.getText(), currentSelection.getDateTimeCreated());
				break;
			case XSD:
				buildlet = new XSDBuildlet(getBuilderKey(), ext.getText(), currentSelection.getDateTimeCreated());
				break;
			case TRANSFORM:
				buildlet = new TransformBuildlet(getBuilderKey(), ext.getText(), currentSelection.getDateTimeCreated());
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buildlet;
	}
}