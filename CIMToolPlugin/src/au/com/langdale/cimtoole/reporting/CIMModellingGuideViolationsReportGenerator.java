/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.reporting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleType;

public class CIMModellingGuideViolationsReportGenerator {

	private static final String CRLF = System.lineSeparator();

	private CIMModellingGuideViolationsReportGenerator() {
	}

	/**
	 * Method that generate the primary header of the document itself.
	 * 
	 * @param reportGenerationSettings The settings to be used during report
	 *                                 generation.
	 * @return The CIM modelling guide violations report as an asciidoc document.
	 */
	private static String generateDocumentHeader(ReportGenerationSettings reportGenerationSettings) {
		StringBuilder documentHeader = new StringBuilder();
		//
		documentHeader.append("//==================================================================").append(CRLF);
		documentHeader.append("// Following is the header attributes defined for this asciidoc     ").append(CRLF);
		documentHeader.append("//==================================================================").append(CRLF);
		documentHeader.append("//").append(CRLF);
		documentHeader.append(CRLF);
		documentHeader.append(":project-name: ").append(reportGenerationSettings.getSchemaFile().getProject().getName())
				.append(CRLF);
		documentHeader.append(":schema-name: ").append(reportGenerationSettings.getSchemaFile().getName()).append(CRLF);
		documentHeader.append(":isMergeShadowExtensionsEnabled: ")
				.append((reportGenerationSettings.isMergeShadowExtensionsEnabled() ? "Enabled" : "Not Enabled"))
				.append(CRLF);
		documentHeader.append(":isSelfHealingOnSchemaImportEnabled: ")
				.append((reportGenerationSettings.isSelfHealingOnSchemaImportEnabled() ? "Enabled" : "Not Enabled"))
				.append(CRLF);
		documentHeader.append(CRLF);
		documentHeader.append("//----------- Theme & Style definitions -----------").append(CRLF);
		documentHeader.append(":pdf-theme: cimtool-adoc-theme.yml").append(CRLF);
		documentHeader.append(":stylesheet: cimtool-adoc.css").append(CRLF);
		documentHeader.append(CRLF);
		documentHeader.append("//----------- General document settings -----------").append(CRLF);
		documentHeader.append(":doctype: book").append(CRLF);
		documentHeader.append(":reproducible:").append(CRLF);
		documentHeader.append(":icons: font").append(CRLF);
		documentHeader.append(":media: screen").append(CRLF);
		documentHeader.append(":compress:").append(CRLF);
		documentHeader.append(":sectnums:").append(CRLF);
		documentHeader.append(":sectnumlevels: 4").append(CRLF);
		documentHeader.append(":xrefstyle: short").append(CRLF);
		documentHeader.append(":table-stripes: even").append(CRLF);
		documentHeader.append("//==================================================================").append(CRLF);
		documentHeader.append("//").append(CRLF);
		documentHeader.append(CRLF);
		documentHeader.append("= CIMTool Schema Import Report").append(CRLF);
		documentHeader.append("Modelling Violations Summary").append(CRLF);
		documentHeader.append(CRLF);
		documentHeader.append(":toc-placement: preamble").append(CRLF);
		documentHeader.append(":toc-title: Table of Contents").append(CRLF);
		documentHeader.append(":toclevels: 6").append(CRLF);
		documentHeader.append(CRLF);
		//
		return documentHeader.toString();
	}

	/**
	 * Method to generate an import report of all CIM modeling violations that are
	 * not compliant with the CIM Modeling Guide.
	 * 
	 * @param reportGenerationSettings The report generation settings the report is
	 *                                 to use.
	 * @param violations               The collection of all modeling compliance
	 *                                 violations.
	 * @param reportGenerationSettings Whether or not the 'merge shadow extensions
	 *                                 on import' setting is on for the project.
	 * @return The generated import report.
	 */
	public static String generateReport(ReportGenerationSettings reportGenerationSettings,
			List<RuleViolation> violations) {
		StringBuilder report = new StringBuilder();
		try {
			String docHeader = generateDocumentHeader(reportGenerationSettings);
			report.append(docHeader);
			RuleType[] ruleTypes = new RuleType[] { //
					RuleType.Normative, //
					RuleType.Extension //
			};

			for (RuleType ruleType : ruleTypes) {
				switch (ruleType) {
				case Normative:
					if (reportGenerationSettings.includeNormative()) {

						report.append(generateSectionHeader(2, ruleType.getSectionTitle()));

						if (reportGenerationSettings.includeGrid()) {
							List<RuleViolation> violationsSubset = violations.stream()
									.filter(violation -> violation.getType() == RuleType.Normative
											&& violation.isInTopLevelGridPackage())
									.collect(Collectors.toList());
							report.append(
									generateSection(3, violationsSubset, "Grid Package Model Violations", ruleType));
						}
						if (reportGenerationSettings.includeEnterprise()) {
							List<RuleViolation> violationsSubset = violations.stream()
									.filter(violation -> violation.getType() == RuleType.Normative
											&& violation.isInTopLevelEnterprisePackage())
									.collect(Collectors.toList());
							report.append(generateSection(3, violationsSubset, "Enterprise Package Model Violations",
									ruleType));
						}
						if (reportGenerationSettings.includeMarket()) {
							List<RuleViolation> violationsSubset = violations.stream()
									.filter(violation -> violation.getType() == RuleType.Normative
											&& violation.isInTopLevelMarketPackage())
									.collect(Collectors.toList());
							report.append(
									generateSection(3, violationsSubset, "Market Package Model Violations", ruleType));
						}
					}
					break;
				case Extension:
					if (reportGenerationSettings.includeExtensions()) {
						report.append(generateSection(2, violations, ruleType.getSectionTitle(), ruleType));
					}
					break;
				}
			}

			report.append("include::{includesdir}/cim_modelling_guide.adoc[]").append(CRLF);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return report.toString();
	}

	/**
	 * Method for generating a new section header with the specified title and at
	 * the specified level.
	 * 
	 * @param level
	 * @param sectionTitle
	 * @return
	 */
	private static String generateSectionHeader(int level, String sectionTitle) {
		StringBuffer sectionHeader = new StringBuffer();
		//
		if (level < 2 || level > 5)
			throw new IllegalArgumentException(
					String.format("The section level specified <%d> is out of range (2 <= %d <= 5).", level, level));

		String sectionLevel = "";
		for (int i = 1; i <= level; i++) {
			sectionLevel += "=";
		}

		sectionHeader.append(CRLF);
		sectionHeader.append(sectionLevel).append(" ").append(sectionTitle).append(CRLF);
		sectionHeader.append(CRLF);
		//
		return sectionHeader.toString();
	}

	/**
	 * Method to order the categories.
	 * 
	 * @return A list of RuleCategory(s) in sorted order by order priority.
	 */
	private static List<RuleCategory> getOrderedCategories() {
		List<RuleCategory> categories = Arrays.stream(RuleCategory.values())
				.sorted(Comparator.comparingInt(RuleCategory::getOrder)).collect(Collectors.toList());
		return categories;
	}

	private static String generateSection(int level, List<RuleViolation> violations, String sectionTitle,
			RuleType ruleType) {
		StringBuffer section = new StringBuffer();
		//
		section.append(generateSectionHeader(level, sectionTitle));
		//
		Set<String> namespaces = new TreeSet<>(); // TreeSet is inherently sorted...
		violations.forEach(violation -> namespaces
				.add(violation.getResourceURI().substring(0, violation.getResourceURI().lastIndexOf("#") + 1)));

		for (String namespace : namespaces) {
			String nsSectionTitle = String.format("%s Namespace [%s]", ruleType.name(), namespace);

			List<RuleViolation> violationsSubset = filterAndSortViolations(violations, ruleType, null, namespace);

			if (violationsSubset.size() > 0) {
				section.append(generateSectionHeader(level + 1, nsSectionTitle));

				for (RuleCategory category : getOrderedCategories()) {
					List<RuleViolation> categorySpecificViolations = filterAndSortViolations(violationsSubset, ruleType,
							category, namespace);
					if (categorySpecificViolations.size() > 0) {
						section.append(generateSectionHeader(level + 2, category.getSectionTitle()));
						section.append("[%header,width=100%,cols=\"10%a,20%a,70%a\"]").append(CRLF);
						section.append("|===").append(CRLF);
						section.append("| Level 2+| Violation Description").append(CRLF);
						for (RuleViolation violation : categorySpecificViolations) {
							section.append(toSimpleAsciiDocTableRow(violation));
						}
						section.append("|===").append(CRLF);
					}
				}

			}
		}
		//
		return section.toString();
	}

	/**
	 * Method for filtering and sorting. Intended to allow for flexible sorting
	 * based on violation category. Currently, all categories are falling back to
	 * the default sorting.
	 * 
	 * @param violations
	 * @param ruleType
	 * @param category
	 * @return
	 */
	protected static List<RuleViolation> filterAndSortViolations(List<RuleViolation> violations, RuleType ruleType,
			RuleCategory category, String namespace) {
		List<RuleViolation> violationsSubset;
		//
		violationsSubset = violations.stream()
				.filter(violation -> (ruleType == null || violation.getType() == ruleType)
						&& (category == null || violation.getCategory() == category)
						&& (namespace == null || violation.getResourceURI().startsWith(namespace)))
				.sorted(Comparator.comparing((RuleViolation rv) -> rv.getCategory().getOrder())
						.thenComparing(RuleViolation::getSeverity).thenComparing(RuleViolation::getRuleId)
						.thenComparing(RuleViolation::getErrorMsg))
				.collect(Collectors.toList());
		//
		return violationsSubset;
	}

	/**
	 * <pre>
	 * ╔══════════╦═══════════════════════════════════════════════════════════╗
	 * ║  LEVEL   ║                 Violation Description                     ║
	 * ╠══════════╬══════════╦════════════════════════════════════════════════╣
	 * ║  ERROR   ║ Rule104  ║ Attribute 'ACAFlag' is missing documentation.  ║
	 * ║          ║          ║════════════════════════════════════════════════╣
	 * ║          ║          ║ Element: TC57CIM::IEC62325::MarketCommon       ║
	 * ╠══════════╬══════════╬════════════════════════════════════════════════╣
	 * ║   ...    ║ ...      ║ ...                                            ║
	 * ╠══════════╬══════════╬════════════════════════════════════════════════╣
	 * ║   ...    ║ ...      ║ ...                                            ║
	 * ╚══════════╩══════════╩════════════════════════════════════════════════╝
	 * </pre>
	 * 
	 * @param violation The rule violation that a table row is to be created for.
	 * @return A table row (in asciidoc) representing the rule violation passed to
	 *         the method.
	 */
	private static String toAsciiDocTableRow(RuleViolation violation) {
		StringBuffer row = new StringBuffer();
		//
		String elementName = (violation.getPlaceholderValueDomainName() != null
				? violation.getPlaceholderValueDomainName() + "." + violation.getResourceLabel()
				: violation.getResourceLabel());
		//
		// The first column of the row will span two rows (-> .2+)
		row.append(".2+| *").append(violation.getSeverity().name()).append("*").append(CRLF);
		row.append(".2+| <<").append(violation.getRuleId()).append(",").append(violation.getRuleId()).append(">>");
		// third column / row 1
		row.append(" | ").append(violation.getErrorMsg()).append(CRLF);
		// third column / row 2
		row.append(" | ").append(violation.getPlaceholderValueElementType()).append(": [.package]#")
				.append(violation.getPackageHierarchy()).append("#[.element-name]#").append(elementName)
				.append(violation.getResourceLabel()).append("#").append(CRLF);
		//
		return row.toString();
	}

	/**
	 * <pre>
	 * ╔══════════╦═══════════════════════════════════════════════════════════╗
	 * ║  LEVEL   ║                 Violation Description                     ║
	 * ╠══════════╬══════════╦════════════════════════════════════════════════╣
	 * ║  ERROR   ║ Rule104  ║ Attribute 'ACAFlag' is missing documentation.  ║
	 * ╠══════════╬══════════╬════════════════════════════════════════════════╣
	 * ║   ...    ║ ...      ║ ...                                            ║
	 * ╠══════════╬══════════╬════════════════════════════════════════════════╣
	 * ║   ...    ║ ...      ║ ...                                            ║
	 * ╚══════════╩══════════╩════════════════════════════════════════════════╝
	 * </pre>
	 * 
	 * @param violation The rule violation that a table row is to be created for.
	 * @return A table row (in asciidoc) representing the rule violation passed to
	 *         the method.
	 */
	private static String toSimpleAsciiDocTableRow(RuleViolation violation) {
		StringBuffer row = new StringBuffer();
		//
		// The first column of the row will span two rows (-> .1+)
		row.append(" | *").append(violation.getSeverity().name()).append("*").append(CRLF);
		if (violation.isCompositeRule()) {
			if (violation.getCompositeRuleId().startsWith("Rule")) {
				row.append(" | <<").append(violation.getCompositeRuleId()).append(",")
						.append(violation.getCompositeRuleId()).append(">>");
			} else {
				row.append(" | ").append(violation.getCompositeRuleId());
			}
			row.append(" : ");
			if (violation.getCompositeSubRuleId().startsWith("Rule")) {
				row.append("<<").append(violation.getCompositeSubRuleId()).append(",")
						.append(violation.getCompositeSubRuleId()).append(">>");
			} else {
				row.append(violation.getCompositeSubRuleId());
			}
		} else {
			if (violation.getRuleId().startsWith("Rule")) {
				row.append(" | <<").append(violation.getRuleId()).append(",").append(violation.getRuleId())
						.append(">>");
			} else {
				row.append(" | ").append(violation.getRuleId());
			}
		}
		row.append(" | ").append(violation.getErrorMsg()).append(CRLF);
		//
		return row.toString();
	}

}
