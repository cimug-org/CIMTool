package au.com.langdale.cimtoole.wizards;

import java.util.Collections;
import java.util.List;

import au.com.langdale.profiles.SchemaMerger;
import au.com.langdale.xmi.EAProjectParserFactory;

/**
 * Decides how the "base CIM schema" checkbox of the schema import wizard
 * behaves when a schema file is imported into a project (issue #391, rules W1
 * to W9 and the Enterprise Architect project file rules).
 * <p>
 * The decision depends only on the project's existing schema files, with their
 * namespaces and base CIM schema flags, and on the name the imported file will
 * have in the project's Schema folder. The wizard is its only user: the command
 * line never adds schema files to a project.
 */
final class BaseSchemaChoice {

	private final SchemaMerger.SchemaFile currentBase;
	private final boolean targetExists;
	private final boolean checked;
	private final boolean enabled;
	private final boolean forcedBase;
	private final String replaced;
	private final String targetName;
	private final String hint;

	private BaseSchemaChoice(String targetName, SchemaMerger.SchemaFile currentBase, boolean targetExists,
			boolean checked, boolean enabled, boolean forcedBase, String replaced, String hint) {
		this.targetName = targetName;
		this.hint = hint;
		this.currentBase = currentBase;
		this.targetExists = targetExists;
		this.checked = checked;
		this.enabled = enabled;
		this.forcedBase = forcedBase;
		this.replaced = replaced;
	}

	/**
	 * Decides the checkbox for importing a schema file.
	 *
	 * @param existing                  the project's schema files, with their
	 *                                  namespaces and flags; their models are not
	 *                                  needed and may be null
	 * @param targetName                the name the imported file will have in
	 *                                  the Schema folder
	 * @param schemaNamespacePreference the Schema Namespace preference, used when
	 *                                  no file is flagged as the base
	 */
	static BaseSchemaChoice decide(List<SchemaMerger.SchemaFile> existing, String targetName,
			String schemaNamespacePreference) {
		if (existing == null)
			existing = Collections.emptyList();
		SchemaMerger.SchemaFile base = existing.isEmpty() ? null
				: SchemaMerger.selectBase(existing, schemaNamespacePreference).getBase();
		boolean exists = false;
		SchemaMerger.SchemaFile eaProject = null;
		for (SchemaMerger.SchemaFile file : existing) {
			if (file.getName().equals(targetName))
				exists = true;
			if (EAProjectParserFactory.isEAProject(file.getName()))
				eaProject = file;
		}
		boolean targetIsEA = EAProjectParserFactory.isEAProject(targetName);

		// W1, W2: the first schema file is the base
		if (existing.isEmpty())
			return new BaseSchemaChoice(targetName, null, false, true, false, true, null,
					"The first schema file of a project is its base CIM schema.");

		// A project can have only one Enterprise Architect project file, so a
		// different one replaces it, for example a later CIM release
		if (eaProject != null && targetIsEA && !eaProject.getName().equals(targetName))
			return new BaseSchemaChoice(targetName, base, exists, true, false, true, eaProject.getName(),
					"This file replaces the project's Enterprise Architect project file.");

		// W4, W6: re-importing the base keeps it the base
		if (base != null && base.getName().equals(targetName))
			return new BaseSchemaChoice(targetName, base, exists, true, false, true, null,
					"This file is already the project's base CIM schema.");

		// The project's Enterprise Architect project file is always the base
		if (eaProject != null)
			return new BaseSchemaChoice(targetName, base, exists, false, false, false, null,
					"The project's Enterprise Architect project file is always its base CIM schema.");

		// An Enterprise Architect project file always becomes the base
		if (targetIsEA)
			return new BaseSchemaChoice(targetName, base, exists, true, false, true, null,
					"An Enterprise Architect project file is always the project's base CIM schema.");

		// W3, W5, W7, W8, W9: the user chooses
		return new BaseSchemaChoice(targetName, base, exists, false, true, false, null,
				base != null ? "The project already has a base CIM schema; ticking this replaces it."
						: "The project has no base CIM schema yet.");
	}

	/** The project's base CIM schema before the import, or null if it has none. */
	SchemaMerger.SchemaFile getCurrentBase() {
		return currentBase;
	}

	/** Whether a schema file of the target's name is already in the project. */
	boolean isTargetExisting() {
		return targetExists;
	}

	/** The checkbox's initial state. */
	boolean isChecked() {
		return checked;
	}

	/** Whether the user may change the checkbox. */
	boolean isEnabled() {
		return enabled;
	}

	/**
	 * The name of the project's Enterprise Architect project file that the
	 * imported file replaces, or null. When it is not null, finishing removes that
	 * file and its companion files, since a project can have only one.
	 */
	String getReplacedFile() {
		return replaced;
	}

	/**
	 * A one-line explanation shown under the checkbox: why it cannot be changed, or
	 * what ticking it does.
	 */
	String getHint() {
		return hint;
	}

	/** Whether the imported file is the base after the import. */
	boolean becomesBase(boolean ticked) {
		return forcedBase || (enabled && ticked);
	}

	/**
	 * Whether finishing replaces an existing base with the imported file, which
	 * the user is asked to confirm (W3, W5, W7, and an Enterprise Architect
	 * project file imported into a project with another base).
	 */
	boolean needsConfirmation(boolean ticked) {
		if (replaced != null)
			return true;
		return becomesBase(ticked) && currentBase != null && !currentBase.getName().equals(targetName);
	}

	/**
	 * The name of the file that is the base after the import, or null if the
	 * project has none.
	 */
	String getBaseAfterImport(boolean ticked) {
		if (becomesBase(ticked))
			return targetName;
		return currentBase == null ? null : currentBase.getName();
	}
}
