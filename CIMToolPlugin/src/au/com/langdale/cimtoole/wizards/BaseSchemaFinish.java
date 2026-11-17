package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.cimtoole.project.Task;

/**
 * Completes a schema import with the project's base CIM schema (issue #391):
 * asks the user to confirm a change of base, and adds to the import the steps
 * that set the base and, if the user chose, remove the previous base.
 */
final class BaseSchemaFinish {

	private BaseSchemaFinish() {
	}

	/**
	 * Returns the import followed by the base CIM schema steps, or null if the
	 * user cancelled the change of base.
	 *
	 * @param importJob the job that imports the schema file
	 * @param page      the wizard page holding the target file and the checkbox
	 * @param shell     the shell for the confirmation
	 */
	static IWorkspaceRunnable chain(IWorkspaceRunnable importJob, SchemaWizardPage page, Shell shell) {
		BaseSchemaChoice choice = page.getBaseSchemaChoice();
		IFile target = page.getStoredFile();
		if (choice == null || target == null)
			return importJob;

		boolean ticked = page.isBaseSchemaTicked();
		IFolder folder = (IFolder) target.getParent();
		IFile previous = null;

		if (choice.getReplacedFile() != null) {
			// A project can have only one Enterprise Architect project file: the new
			// one replaces the old, which is always removed
			String old = choice.getReplacedFile();
			String c = target.getName();
			if (!MessageDialog.openConfirm(shell, "Replace the Enterprise Architect project file", "Replace " + old
					+ " with " + c + " as the project's base CIM schema? " + old
					+ " and its companion files will be removed from the project, and all profiles are rebuilt."))
				return null;
			previous = folder.getFile(old);
		} else if (choice.needsConfirmation(ticked)) {
			String a = choice.getCurrentBase().getName();
			String c = target.getName();
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(shell,
					"Change the base CIM schema",
					"Make " + c + " the project's base CIM schema in place of " + a + "? " + c
							+ "'s ontology becomes the project's ontology and imports the other schemas, and all profiles are rebuilt.\n\n"
							+ "Remove " + a + " if " + c
							+ " replaces it, for example a later CIM release in a differently named file.",
					"Also remove " + a + " and its companion files from the project",
					false, null, null);
			if (dialog.getReturnCode() != IDialogConstants.OK_ID)
				return null;
			if (dialog.getToggleState())
				previous = folder.getFile(a);
		}

		IWorkspaceRunnable job = importJob;
		String baseAfter = choice.getBaseAfterImport(ticked);
		if (baseAfter != null)
			job = Task.chain(job, Task.setBaseSchema(folder.getFile(baseAfter)));
		if (previous != null)
			job = Task.chain(job, Task.removeSchema(previous));
		return job;
	}
}
