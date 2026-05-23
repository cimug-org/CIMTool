/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.reporting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IFile;

/**
 * A class representing all potential report generation settings that can be
 * used during the generation of a CIM Modelling Guide violations report. A
 * builder pattern is utilized that enforces default values and performs null checks
 * where needed.
 */
public class ReportGenerationSettings {

	private final IFile schemaFile;
	private final Map<String, String> stereotypedNamespaces;
	private final boolean isMergeShadowExtensionsEnabled;
	private final boolean isSelfHealingOnSchemaImportEnabled;
	private final boolean shouldGenerateReport;
	private final boolean includeExtensions;
	private final boolean includeNormative;
	private final boolean includeGrid;
	private final boolean includeEnterprise;
	private final boolean includeMarket;

	private ReportGenerationSettings(Builder builder) {
		this.schemaFile = builder.schemaFile;
		this.stereotypedNamespaces = builder.stereotypedNamespaces;
		this.isMergeShadowExtensionsEnabled = builder.isMergeShadowExtensionsEnabled;
		this.isSelfHealingOnSchemaImportEnabled = builder.isSelfHealingOnSchemaImportEnabled;
		this.shouldGenerateReport = builder.shouldGenerateReport;
		this.includeExtensions = builder.includeExtensions;
		this.includeNormative = builder.includeNormative;
		this.includeGrid = builder.includeGrid;
		this.includeEnterprise = builder.includeEnterprise;
		this.includeMarket = builder.includeMarket;
	}

	public static class Builder {
		private IFile schemaFile;
		private Map<String, String> stereotypedNamespaces = new HashMap<>();
		private Boolean isMergeShadowExtensionsEnabled;
		private Boolean isSelfHealingOnSchemaImportEnabled;
		private Boolean shouldGenerateReport;
		private Boolean includeExtensions;
		private Boolean includeNormative;
		private Boolean includeGrid;
		private Boolean includeEnterprise;
		private Boolean includeMarket;

		public Builder schemaFile(IFile schemaFile) {
			this.schemaFile = schemaFile;
			return this;
		}

		public Builder isMergeShadowExtensionsEnabled(Boolean value) {
			this.isMergeShadowExtensionsEnabled = value;
			return this;
		}

		public Builder shouldGenerateReport(Boolean value) {
			this.shouldGenerateReport = value;
			return this;
		}

		public Builder includeExtensions(Boolean value) {
			this.includeExtensions = value;
			return this;
		}

		public Builder includeNormative(Boolean value) {
			this.includeNormative = value;
			return this;
		}

		public Builder includeGrid(Boolean value) {
			this.includeGrid = value;
			return this;
		}

		public Builder includeEnterprise(Boolean value) {
			this.includeEnterprise = value;
			return this;
		}

		public Builder includeMarket(Boolean value) {
			this.includeMarket = value;
			return this;
		}

		public Builder isSelfHealingOnSchemaImportEnabled(Boolean value) {
			this.isSelfHealingOnSchemaImportEnabled = value;
			return this;
		}

		public Builder stereotypedNamespaces(Map<String, String> stereotypedNamespaces) {
			this.stereotypedNamespaces = stereotypedNamespaces;
			return this;
		}

		public Builder stereotypedNamespaces(IFile stereotypedNamespacesPropertiesFile) {
			if (stereotypedNamespacesPropertiesFile == null) {
				this.stereotypedNamespaces = null;
				return this;
			}
			File file = stereotypedNamespacesPropertiesFile.getLocation().toFile();
			Properties props = new Properties();
			try (FileInputStream fis = new FileInputStream(file)) {
				props.load(fis);
				for (String key : props.stringPropertyNames()) {
					this.stereotypedNamespaces.put(key, props.getProperty(key));
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return this;
		}

		public ReportGenerationSettings build() {
			if (schemaFile == null) {
				throw new IllegalStateException("schemaFile must not be null");
			}
			if (stereotypedNamespaces == null) {
				stereotypedNamespaces = Map.of(); // default
			}
			if (isMergeShadowExtensionsEnabled == null) {
				isMergeShadowExtensionsEnabled = Boolean.FALSE;
			}
			if (isSelfHealingOnSchemaImportEnabled == null) {
				isSelfHealingOnSchemaImportEnabled = Boolean.FALSE;
			}
			if (shouldGenerateReport == null) {
				shouldGenerateReport = Boolean.FALSE;
			}
			if (includeExtensions == null) {
				includeExtensions = Boolean.FALSE;
			}
			if (includeNormative == null) {
				includeNormative = Boolean.FALSE;
			}
			if (includeGrid == null) {
				includeGrid = Boolean.FALSE;
			}
			if (includeEnterprise == null) {
				includeEnterprise = Boolean.FALSE;
			}
			if (includeMarket == null) {
				includeMarket = Boolean.FALSE;
			}
			return new ReportGenerationSettings(this);
		}
	}

	// Getters (if needed)
	public IFile getSchemaFile() {
		return schemaFile;
	}

	public Map<String, String> getStereotypedNamespaces() {
		return stereotypedNamespaces;
	}

	public boolean isMergeShadowExtensionsEnabled() {
		return isMergeShadowExtensionsEnabled;
	}

	public boolean isSelfHealingOnSchemaImportEnabled() {
		return isSelfHealingOnSchemaImportEnabled;
	}

	public boolean shouldGenerateReport() {
		return shouldGenerateReport;
	}

	public boolean includeExtensions() {
		return includeExtensions;
	}

	public boolean includeNormative() {
		return includeNormative;
	}

	public boolean includeGrid() {
		return includeGrid;
	}

	public boolean includeEnterprise() {
		return includeEnterprise;
	}

	public boolean includeMarket() {
		return includeMarket;
	}

	@Override
	public String toString() {
		return "ReportGenerationSettings{" + "schemaFile=" + schemaFile + ", stereotypedNamespaces="
				+ stereotypedNamespaces + ", isMergeShadowExtensionsEnabled=" + isMergeShadowExtensionsEnabled
				+ ", isSelfHealingOnSchemaImportEnabled=" + isSelfHealingOnSchemaImportEnabled
				+ ", shouldGenerateReport=" + shouldGenerateReport + ", includeExtensions=" + includeExtensions
				+ ", includeNormative=" + includeNormative + ", includeGrid=" + includeGrid + ", includeEnterprise="
				+ includeEnterprise + ", includeMarket=" + includeMarket + '}';
	}
}
