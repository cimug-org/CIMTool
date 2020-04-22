## CIMTool Release Log

### Release 1.9.8-RC1 [Available 24-Apr-2020]
new feature: Beta release of support for the new IEC 62361-104 standard for JSON schema profiles.  This release does not include the variant of namespace support that will be included in the published standard.  That is planned for RC2.

enhancement: CIMTool supports a variety of different types of builders that generate profile artifacts. Examples include XSD schemas, JPA Java source code, RDBMS DDL scripts, RDFS profiles, etc.  The ability to create custom profile builders has been available via CIMTool Eclipse plugin extension points, but such an approach requires an understanding of Eclipse plugin development.  Internally, CIMTool supports a category of builders based on XSLT transforms.  This release exposes this functionality by providing a configuration-based approach to adding custom XSLT transform-based builders that will automatically appear within CIMTool's "Profile Summary" tab. This enhancement allows tools such as Altova's XMLSpy to be used to create and test XSLT 1.0 transforms and utilize them in CIMTool.

CIMug's goal for this enhancement is to create a GitHub repository of custom XSLT transforms contributed by the community and, in turn, beneficial to the CIM community at large.

### Release 1.9.7 [09-Dec-2013]
correction: .eap files are closed after parsing and can now be deleted from a project.

### Release 1.9.7 [09-Dec-2013]
correction: .eap files are closed after parsing and can now be deleted from a project.

### Release 1.9.6 [10-Sep-2013]
enhancement: platform updated to eclipse Indigo.

enhancement: direct import of EA project files. This is now the default option on the import file selector. The capability is provided by a new release of the jackcess library with MS Jet3 compatitbility.

enhancement: convenience button for the CIM 16 namespace.

correction: help facility restored.

### Release 1.9.5
This is one of a series of beta testing releases. See the blurb, New and Notable for 1.9. This release adds:

- Map primitive 'duration' to xs:duration. (Note: we already map decimal, date, time, dateTime to XSD types of the same name.)
- Mappings of AbsoluteDate and AbsoluteTime removed. (Can't remove other oddball mappings such as AbsoluteDateTime because that would affect legacy schema generation.)

### Release 1.9.4
This release was not generally made available pending resolution of CIM data and time type issues.

This is one of a series of beta testing releases. See the blurb, New and Notable for 1.9. This release adds:

- Hard coded translation of CIM datatypes AbsoluteDate and AbsoluteTime to xs:date and xs:time. This is to get around a (hopefully temporary) problem in the CIM where these identifiers were used instead of the XSD ones.

### Release 1.9.3
This is one of a series of beta testing releases. See the blurb, New and Notable for 1.9. This release adds:

enhancement: profile repair and remapping to the CIM will ignore case when searching for a CIM term if an exact match can't be found.

Java SE 1.6 is now the minimum requirement for the JVM

correction: a bug interpreting .eap files caused namespaces to be incorrectly assigned in some cases.

enhancement: recognise ontology headers in annotations files. The given namespace overrides the default namespace for their schema (or CIM). This also triggers an improved package URI generation scheme.

correction: change the heap memory maximum to 1GB (from 2GB) allowing the product version of CIMTool to run on more systems.

enhancement: cosmetic changes to startup.

enhancement: added schema mapping wizard to 'new' menu.

### Release 1.9.2 [09-Jan-2011]
This is one of a series of beta testing releases. See the blurb, New and Notable for 1.9. This release adds:

enhancement: tree views in the user interface retain their state accross refreshes including save profile and navigation away from a node and back again. This makes it easier to keep your place in the model during profile building.

Updated Ecore functionality to enable registered EPackages to be used for the base schema. Updates to the Ecore generation to ensure profiled Ecore matches the package hierarchy of the base if the base is also Ecore. Removed unused legacy merged schema generation

### Release 1.9.1
This is a beta testing release to trial a number of major changes. See the blurb, New and Notable for 1.9. In summary:

- Generalised profile editing.
- Schema mapping and extension editor.
- Ecore model support.
- Recognise new CIMDatatype stereotype.

### Release 1.8.3 [10-Aug-2010]
correction: improved backwards compatibility with version 1.7 for profile (.owl) files. A profile can be round-tripped from 1.7 to 1.8.2 and back repeatedly. In 1.8.2 this caused errors in the profile editor.

enhancement: basic validation for the new standard headers in CIM/XML documents. This reduces the number of spurious validation errors.

### Release 1.8.2 [05-Jun-2010]
The release corrects the following defect in 1.8.1:

correction: a defect in handling of attributes (datatype properties) caused spurious validation errors and incorrectly generated XML schema.

### Release 1.8.1 [06-Feb-2010]
This release incorporates changes to make CIMTool compatible with commonly used ontology editors. It has been tested with Protege 4 and Top Braid 3.2.

enhancement: namespaces for OWL files are not longer stored as properties in the CIMTool project they are embedded in the files as part of the ontology header. This change affects the user interface for profile properties as well as several wizards and the Detail tab of the profile editor for the root element of the profile.

enhancement: the ontology header is now compliant and includes the profile description, label, and (implicitly) namespace. the .owl header includes an import for the merged information schema.

enhancement: CIM datatypes are now modeled using equivalentClass axioms. Anonymous classes in their profile are linked by equivalentClass or subClass to their respective information model classes.

### Release 1.7.3
correction: profile validation rules for the case where a member of a child class is lifted into a parent caused spurious diagnostics. This affected the EndDeviceAssett profile in the IEC 61968 part 9 profiles.

### Release 1.7.2 [10-Nov-2009]
correction: Older Rose based CIM versions sometimes have invalid, top level datatype definitions. Attempts in release 1.4.1 to deal with these in some older EDF CIM versions broke compatibility with some older Siemens models. They are now interpreted even more flexibly in the light of other definitions to obtain a valid overall model.

### Release 1.7.1 [10-Nov-2009]
correction: the inverse properties that are added the profile for the "augmented" OWL and RDFS artifacts are incorrect when the range and domain are related by inheritance.

### Release 1.6.9 [19-Oct-2009]
enhancement: new .simple-flat-owl format which corresponds to the simple-owl format in version 1.6.3

### Release 1.6.8 [30-Sep-2009]
enhancement: new options on the profile add/remove page control whether super or sub class members are available. It is now possible to redefine or separately define a member in subclasses.

### Release 1.6.7
correction: ensure artifact generation runs to completion even when a profile contains severe errors.

### Release 1.6.6 [27-Sep-2009]
correction: a problem introduced in 1.6.5 that prevented importing Enterprise Architect Jet4 projects

### Release 1.6.5 [26-Sep-2009]
enhancement: additional help

correction: corrected problem introduced in 1.6.4 with the new simple-owl format that also affected validation

note: deployment now contains separate plugins for the main supporting libraries

### Release 1.6.4 [26-Sep-2009]
new feature: profile searching and enhanced CIM searches. The search wizard is now available on the profile outline as well as the add/remove page and the project model view. The wizard now shows all matching classes, properties or other items in a list with context and allows the user to navigate to a specific hit.

enhancement: the Project Model View and the Documentation view now track selections made in the profile editor add/remove page and profile outline. This makes it much easier to choose items to add to a profile. It also works well with the new profile search wizard to explore a profile.

enhancement: the format of the simple-owl and simple-owl-augmented files has been changed to eliminate all nodeID attributes. Restriction elements are nested directly in Class elements to which they apply. Some URI's have been shortened due better choice of the base namespace.

### Release 1.6.3 [26-Sep-2009]
enhancement: more robust import of Enterprise Architect project databases, ignoring errors such as disconnected attributes.

### Release 1.6.2 [26-Sep-2009]
new feature: the Documentation View continuously shows documentation from the CIM for the current selection. To see this view, enter the CIMTool perspective using Window > Open Perspective and reset it using Window > Reset Perspective.

new feature: the Browsing Perspective is designed for browsing the CIM. To use it, choose Window > Open Perspective > CIMTool > Browsing and select a project in the Project Explorer View. Use the Project Model View and its Jump and Search buttons (top right of view) to browse. This perspective is sometimes easier to use than a standard UML editor, especially if the UML diagrams are not complete.

enhancement: addition profile checking rules and actions in the repair assistant. These detect and repair several profile problems discovered in the wild. A rule that caused a false alarm in the 61968-9 profiles has been rectified.

enhancement: the Project Model View now has a Jump button (next to Search) that moves the selection to the opposite end of an association or subclass-superclass relationship.

correction: Resource names containing spaces caused processing of the project to stall.

### Release 1.6.1 [26-Sep-2009]
new feature: Profile Repair Assistant.

The Profile Repair Assistant helps correct errors in profiles, especially those which arise when the profile is paired with an updated CIM.

Each time a profile is saved or a the CIM schema is changed, the profile is checked against a set of rules. If any errors are detected, a repair file appears as a red icon adjacent to the profile in the project explorer.

Opening the repair file causes the repair assistant to run, which displays the errors and offers suggested repair actions. Any desired repairs can be checked and are applied when the save button is pressed.

new feature: directly import Enterprise Architect project files

The create project and import schema wizards now offer the option to directly import an Enterprise Architect project file. This eliminates the need to create an XMI file for import to CIMTool and speeds up the round trip when the CIM or an extension is edited concurrently with a profile.

The feature is limited to EA project files in the Jet4 format. This requires the project to be converted to Jet4 and the "use Jet4" option in EA to be enabled.

new feature: cardinality for concrete profile classes.

It is mow possible to set the minimum and maximum cardinality of a a top-level, concrete class. This is reflected in generated XML schemas by the cardinality of the corresponding first-level element defintions.

new feature: preliminary support for compound datatypes.

The <<Compound>> stereotype is now recognised and compound classes are displayed and documented differently to normal classes.

enhancement: namespaces and other settings are stored in the project, enabling projects to be exported and imported between projects without loss of information or functionality.

enhancement: commonly used namespaces are offered by the schema import and project creation wizards.

enhancement: profile namespace checked against schema namespaces in the import profile and new profile wizards. conflicts between the profile namespaces and a schema namespace are prevented.

enhancement: improved software update system (better support of the P2 provisioning system)

enhancement: now based on eclipse 3.5 (Galileo)

correction: the namespace field in the import schema wizard was ignored.

correction: cases where the build system failed to trigger a build or triggered the same build twice corrected.

correction: the bases of an anonymous profile class could not be changed on the hierarchy page of the profile editor.

### Release 1.5.5 [12-May-2009]
new feature: CIMTool can now generate java class definitions with JPA annotations. These classes can be used in a java program to implement the objects of a CIM profile.

The JPA (Java Persistance API) annotations map the classes to relational database tables. Thus the CIM profile objects can be loaded from and saved to a database with a minimum of effort. The JPA feature has been tested with a Hibernate and Oracle combination. See http://hibernate.org

The database can be created using the DDL feature released in version 1.4.4.

To use these features, check the SQL and java builder options on the summary page of your CIM profile. SQL and java artifacts will be created. The database structure is described in the CIMTool help.

correction: a regression in version 1.5.4 affecting mRID fields in SQL artifacts was fixed.

### Release 1.5.4 [12-May-2009]
enhancement: A description can be added to a profile and edited on the profile editor detail tab. It will appear in the HTML documentation and as an annotation to any XML schema.

enhancement: The profile envelope name is now preserved when profiles or projects containing profiles are exported and imported. It, and the description, are stored within the profile OWL file and it is edited on the detail tab instead of the property dialog box.

### Release 1.5.3 [09-Mar-2009]
enhancement: HTML profile documentation changed to more closely follow the part 301 format. Inherited members are shown for each class, grouped by superclass.

### Release 1.5.2 [01-Feb-2009]
enhancement: accept comments encoded in as 'description' tags from some XMI documents generated by EA

correction: the outline and add/remove display was not updating following the assignment of a type to an element (ie a profile property).

### Release 1.5.1 [21-Jan-2009]
enhancement: updated CIMTool help

correction: the remap profile to schema function failed for some profiles

package with eclipse 3.4 platform for windows (no JDT hence lighter download)

Development Releases 1.4.*
Caveat: Releases 1.4.* are development releases. They contain internal changes in preparation for the a new guided profile update feature. While they pass the CIMTool test suite and unstructured testing, there could nevertheless be regressions.

The 1.4.* releases can be only obtained from an eclipse update site at: http://files.cimtool.org/development

Stable releases are preferred and these are available in a number of forms detailed here: http://wiki.cimtool.org/Download.html

### Development Release 1.4.4 [21-Jan-2009]
new feature: CIMTool can now generate a database schema from a CIM profile. This is done by selecting the "SQL" option in the profile editor. An SQL92 DDL artifact is generated. This has been tested for compatibility with Oracle 10g.

The generated code includes tables and fields for classes and properties and comments are copied through for clarity. Constraint definitions are generated for mandatory/optional cardinality, enumeration membership, association type and class inheritance.

Class inheritance has been implemented using the PK-PK join approach.

correction: the comparison functions were not configured correctly in the previous release and would yield no results.

correction: a regression causing the cardinality of some associations extracted from xmi to be incorrect.

correction: a regression in the "make leaf classes concrete" function caused all classes to be marked as not concrete.

correction: an infinite loop when generating artifacts from some profiles containing a circular association.

correction: the eclipse platform sometimes reported errors when a profile was deleted.

### Development Release 1.4.3 [21-Jan-2009]
enhancement: comparisons of profiles to profiles or schema to schema are now much faster and require much less memory. These operations now use CIMTool's cache for the inputs.

enhancement: Repair and remap profile function now corrects the namespace of enumeration values

enhancement: enumerations can now be added directly to the profile (previsously they were added only as a side effect of property definition)

correction: a regression prevented restriction of the range of a property to a subclass.

correction: enumeration restrictions were not carried into the generated OWL and RDFS artifacts and were not validated in instance documents.

### Development Release 1.4.2 [21-Jan-2009]
new feature: profile consistency checking and problem display. CIMTool will check each profile against the project schema during the build phase. Inconsistencies are recorded in a diagnostic file which can be viewed with CIMTool's diagnostic editor (also used for instance validation).

When consistency errors exist error markers appear on the profile resource (in the Project Explorer view) and the affected profile elements in the profile outline view. A entry is also added to the eclipse problems view.

This is a first release of this feature and only the most common inconsistencies are detected. Some further improvements in the checking algorithm and the user interface may be expected.

Note: to check profiles a project build must be triggered. A trigger is generated if the schema (CIM and extensions) or a profile is updated.

new feature: support for the <<extendedBy> and <<extension>> stereotypes defined by EDF Research. These stereotypes are used to define CIM extensions. CIMTool displays extensions marked with these stereotypes using distinctive icons and. In profile editing, extension class members can be added directly to the profile of the extended CIM class.

enhancement: support for the Siemens PowerCC namespace tags. These tags associate a namespace prefix such as 'ext' with each individual class, association or attribute. To use this information you must declare each prefix in the annotation file with a statement like this:

<http://www.ercot.com/CIM11R0/2008/2.0/extension#> uml:uriHasPrefix "etx" .

enhancements to support some older versions of CIM and EDF models:

do not accept top level datatype definitions in an XMI file as written. Flexibly assign a type to these identifiers based on related definitions found elsewhere in the model.

accept 'units' as well as 'unit' for the units attribute of a datatype

correction: the baseuri tag was not recognised in some XMI files.

corrections: various regressions introduced in the 1.4.1 development release.

### Development Release 1.4.1 [21-Jan-2009]
First release with internal changes to the ontology API needed for future features.

new feature: Schema comparison using the eclipse compare functionality. Each project has at least one schema file (generally a .xmi file) in the Schema folder. To compare two schemas, select them in the Navigator, right click and choose Compare With > Each Other. To compare a schema with a recent version select it, right click and choose Compare With > Local History.

new feature: Integrated help. To access CIMTool help and documentation within eclipse, choose Help > Help Contents from the main menu. Look for the topic "Common CIMTool Tasks" in the table of contents. In the release, the included help consists of the most useful material from the http://cimtool.org web site.

### Release 1.3.3 [11-Sep-2008]
new feature: "Add package name to default schema namespaces" preference causes unique RDF namespaces to be generated from the UML package hierarchy.

new feature: CIM/XML profiles can now have more than one restriction, in different contexts, on the same property. This was previously only supported for XSD profiles.

The simple-owl format now preserves all property restrictions and the validation rules have been reworked to enforce them.

The standard CIM/XML RDFS format cannot support this and CIMTool rolls up the restrictions into a single, consistent property definition.

correction: when rolling up restrictions to base classes for RDFS, they are always relaxed. this prevents restrictions from incorrectly affecting sibling classes.

correction: use the concrete stereotype in validation instead of treating the leaf classes as concrete. The "Stereotype leaf classes as concrete" operation can be used to obtain the previous behaviour.

enhancement: stereotypes assigned in the profile editor are copied into the generated RDFS and simple-OWL.

enhancement: build target flags and stereotypes defined in a profile are preserved in the "Reorganize per RDFS Rules" operation.

enhancement: the property sheet view for diagnostic nodes has been improved.

enhancement: tested with eclipse 3.4 (Ganymede)

### Release 1.3.2 [11-Sep-2008]
correction: allow unnamed schema properties to be profiled

new feature: sawsdl semantic annotations in generated XML Schema see: http://www.w3.org/TR/sawsdl/

enhancement: generated HTML profile documentation reworked with better ordering and formating of information.

enhancement: package names added to properties sheet and generated profile documentation.

### Release 1.3.1 [11-Sep-2008]
new feature: Property sheet view shows details of the selected profile or schema definition. This view has been added to the CIMTool perspective. From the menu choose Window > Reset Perspective to update your perspective. The property sheet replaces some of the fields on the profile editor detail page.

new feature: Profile comparisons using the eclipse compare functionality. To compare two profiles, select them in the Navigator, right click and choose Compare With > Each Other. To compare a profile with a recent version select it, right click and choose Compare With > Local History.

enhancement: preserve association documentation from EA UML 1.4 (Rose compatible) XMI export files. It is now possible to use EA models with CIMTool using those XMI export settings.

new feature: simple-owl-augmented and legacy-rdfs-augmented targets can be built from a profile. The new targets provide both a forward and inverse property for each property defined in the profile. They replace the old nested-simple-owl and nested-legacy-rdfs build targets. Build new build targets can be selected on the profile editor summary page.

### Release 1.2.5 [18-Jul-2008]
new feature: add a class hierarchy page to the profile editor

new feature: allow more than one named profile per information model class

correction to search feature: failed to display matches in some cases

### Release 1.2.4 [10-Jul-2008]
change schema tree icons to reflect UML aggregation and composition

the 'make leaf classes concrete' option in the profile editor now does exactly that and ignores other

capture aggregation annotations from Unisys XMI input

support for non-standard primitive to XSD part II mappings

### Release 1.2.3 [03-Jul-2008]
Fix regression: ProjectModelView not responding to selection or double click

### Release 1.2.2
Fix a jena ConversionException when remapping profile to new CIM namespace.

Add missing wizard for new incremental validation rules.

Correct title in import schema wizard.

### Release 1.2.1
Released under LGPL.

Remove a Java 6 dependency.

### Release 1.1.7
Added AbsoluteDateTime to the built-in list of fundamental types. It is mapped to an xsd:datetime

Fixed a regression that prevented the Import Incremental Model wizard from being completed.

Fixed a regression in the Validation View in the Validation Perspective. The view action buttons were missing.

### Release 1.1.6
Fixed a bug in the backwards compatibility code for the 1.1.5 profile format change. The symptom was a failure to generate XML schemas from profile created in earlier versions.

A bug in the handling of the unit attribute of datatype definitions has been corrected.Unit and multiplier values should now be captured in the whole-of-schema OWL output as annotation properties. (Although there don't seem to be many multiplier values defined in the CIM so far.) To see this output choose File|Export|CIMTool|Export Merged Schema from the menus.

The schema search wizard has been added to the profile editor.Look for a search button on the top right of the Add/Remove page. Classes can found by typing in a few characters of their name or the name of one of their properties and selecting the desired match.

### Release 1.1.5
The form of the generated XML schema has been changed in the case where a property is defined with more than one range type. In this case no element is generated for the property and an xsd:choice of element representing the types appears.

A new stereotype "Preserve" has been defined. When assigned to a property it causes the property element to be generated wrapping the type elements. This can be used to avoid ambiguities that the above schema generation rule might cause.

A schema search wizard has been added for testing. It is accessed from the project model view toolbar.

### Release 1.1.4
Numerical cardinality values other than 0, 1 and unbounded are now supported.

The Add/Remove tab now has fields for entering the min and max cardinality of a property as well as check boxes for the common options. As before, cardinality values that would exceed those defined in the CIM for the given property are not allowed.

Cardinality values are carried through the generated XML schema. In the generated RDFS or simple-owl any cardinality greater than one is treated as unbounded because those formats do not allow for arbitrary values.

### Release 1.1.3
Completed performance enhancements, improving the display refresh behavior in the profile editor.

### Release 1.1.2
Numerous regressions that crept into 1.1.1 have been corrected.

Performance of the profile editor has been improved. There may be some minor display refresh problems introduced by this work, but the editor is much more pleasant to use.

A merged schema export wizard has been added. This allows you to export OWL for the schema (as opposed to a profile) to a file in the workspace, which will then be maintained up-to-date- by the build system, or to a file in the filesystem. Only the direct rdfs:subClassOf axioms are included, as requested.

Enumerated values in generated XSD have simple names (with no class prefix).

When a range is added to a profile property it is no longer shown with label "Unamed". It is named after the schema class. (Range profiles are new feature that allow the range defined in the schema to be narrowed to a union of its subclasses.)

A stereotype page has been added to the profile editor. Stereotypes can be added and removed from profile classes and properties. These are transmitted to the generated profile artifacts and can be used in custom profile generation rules. The list of available stereotypes is drawn from the UML and from a stereotype definition file and from a built-in list.

The By Reference and Concrete flags are now represented as stereotypes and can be assigned on the stereotype page. By Reference flags in old profiles are automatically converted to stereotypes upon editing.

The concrete flag was formerly computed and is now a manually assigned stereotype. However, the concrete stereotype can be compute for all classes in the profile by the new "Repair and Reorganise" wizard.

A "Repair and Reorganise" wizard is available from the Summary page of the profile editor. This provides a check list of global profile transformations and will apply the selected ones in the appropriate order.

The order is:

1. Stereotype all properties as By Reference
2. Stereotype leaf classes as Concrete
3. Repair and remap profile to schema
4. Reorganise profile per RDFS rules

The remap operation is usefull if namespaces in the schema have been changed. It updates the profile to reference a new schema finding the best match for each class and property. It performs a number of other repairs as well.

The reorganise operation we present before. Warning: RDFS rules are incompatible with the new property range profiles and these will be lost if this operation is performed.

Changes 2008-02-15
CIM/XML validator passes the full test suite, available at http://files.cimtool.org/Validation-Cases-2008-02-15.zip

Includes incremental CIM/XML validation.

User interface improvements.

Changes 2008-01-31
CIMTool has expanded namespace support described here: http://wiki.cimtool.org/CIMToolNamespaceSupport.html

There is support for multiple extension model namespaces.

Optionally, those namespaces can be carried through to profile artifacts (legacy-rdfs and simple-owl). This corresponds to the current practice on the ERCOT project.

The pros and cons of the latter practice are discussed here: http://wiki.cimtool.org/Namespaces.html

For a discussion of CIM extension model techniques using CIMTool see the UISOL Distributech presentation.

The CIM/XML validation function has been further developed.

Basic topology validation tests are now included and the profile conformance tests have been expanded.

Memory footprint is down and speed is up. It is now possible to validate giant models such ERCOT in about the same time again it takes to parse their XML. That is about 5 minutes parse plus 5 minutes validation on my box. The models used for IOP testing only take seconds.

A simplified user interface for validating models has been added. It is implemented as an eclipse 'perspective' with fixed layout and new views, actions and wizards for streamlining this task. From the menu select Window > Show Perspective > Validation.

A validation result browser has been implemented. This groups messages by class and property and is linked to the Project Model View.

Repetitive validation messages involving with the same description and the same class or property are now suppressed by default. This behaviour is controlled by a preference. Check Window > Preferences > CIMTool > Limit validation output.

The validation rule language has been improved and simplified. It is still rather technical to write these rules, but this is a step towards practical per-project validation rules.

Changes in the profile editor have been made.

A simplification has been made in the logic for detecting a concrete class. This affects XML schema generation.

Note that ''concrete'' classes are rendered in XML Schema as top level elements while other classes become complexTypes. A class is concrete if it is not ''nested'' in another class anywhere in the profile except with the ''by reference'' flag.

This change has the potential to change the XML Schema generated from existing profiles where a class was used both by reference and not in the same profile. That is an unlikely scenario.

The icon for a ''by reference'' association has been changed to an arrow. The icon for a ''nested'' association is a class box.

The ''Reorganize Profile'' function (found on the Summary tab) now has an option to convert all associations to ''by reference''. This is mainly useful for CIM/XML profiles where that as the correct way to model associations.

The ''Remap Profile'' has been temporarily withdrawn. A more reliable way to adjust a profile when the information model namespace(s) change is being developed.
