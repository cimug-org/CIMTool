# CIMToolHelp

An Eclipse help plugin that provides the integrated HTML documentation for the
CIMTool application, accessible via **Help > Help Contents** within the running
Eclipse workbench.

This project contains no Java source. Its sole purpose is to package and register
the CIMTool user documentation as an Eclipse help contribution.



## Overview

CIMToolHelp (`au.com.langdale.cimtoole.help`) is a lightweight Eclipse plugin
that contributes to the `org.eclipse.help.toc` extension point, registering
`toc.xml` as the table of contents for the CIMTool help system. All content is
static HTML stored in the `html/` directory.

The documentation covers the full range of CIMTool user topics — from getting
started and importing EA models through to profiling, validation, namespace
management, and OWL/RDF concepts — and is displayed directly within the Eclipse
Help viewer.



## Project Structure

```
CIMToolHelp/
├── META-INF/
│   └── MANIFEST.MF         ← OSGi bundle manifest (au.com.langdale.cimtoole.help)
├── build.properties        ← Declares html/ and toc.xml for inclusion in the bundle
├── plugin.xml              ← Registers toc.xml via org.eclipse.help.toc extension point
├── toc.xml                 ← Table of contents — defines the help topic tree and entry order
└── html/                   ← Static HTML documentation pages and supporting assets
    ├── CIMTool_Topics.html                        ← Help landing page
    ├── GettingStarted.html
    ├── ChangeLog.html
    ├── Browsing_the_CIM.html
    ├── Using_CIMTool_with_Enterprise_Architect.html
    ├── Using_CIMTool_with_Protege_and_Top_Braid.html
    ├── Searching_Schemas_and_Profiles.html
    ├── Profile_Maintenance.html
    ├── Comparing_Profiles_and_Schemas.html
    ├── Exchanging_CIMTool_Projects.html
    ├── Associations_and_Anonymous_Classes.html
    ├── MultipleProfiles.html
    ├── Enumerations_Support.html
    ├── CIMToolNamespaceSupport.html
    ├── Namespaces.html
    ├── OWL_as_a_Profile_Language.html
    ├── UMLOWL.html
    ├── HowToValidateCPSM.html
    ├── ValidationCases.html
    ├── ValidationRules.html
    ├── Generating_a_Database_Schema.html
    ├── ExportFromEnterpriseArchitect.html
    ├── Profile_Structure_Diagram.html
    ├── CIMCheck.html
    ├── CIMCheck_API.html
    ├── New_and_Notable_for_1_9.html
    ├── LGPL.html
    ├── graphic.css                                ← Stylesheet for all help pages
    └── ...                                        ← Supporting PNG images per topic
```



## Dependencies on Other Projects

CIMToolHelp has no compile-time or runtime OSGi dependencies on other in-repository
plugins. It declares no `Require-Bundle` entries in its MANIFEST and contains no
Java source. It relies solely on the Eclipse help framework
(`org.eclipse.help.toc` extension point) which is provided by the Eclipse
platform itself.

The only integration point with the rest of the CIMTool project is that
`CIMToolProduct` lists this plugin (`au.com.langdale.cimtoole.help`) in
`CIMTool.product` so it is included in the PDE product export and shipped as
part of the CIMTool distribution.

Additionally, `CIMToolProduct/helpData.xml` references this plugin's `toc.xml`
to control the ordering of help books in the Eclipse help table of contents:

```xml
<toc id="/au.com.langdale.cimtoole.help/toc.xml"/>
```



## Help Content

The `toc.xml` table of contents defines 21 top-level help topics covering:

| Topic | Description |
| --- | --- |
| Getting Started | How to create a CIMTool project |
| Browsing the CIM | Explore the CIM in CIMTool's browsing perspective |
| Using CIMTool with Enterprise Architect | How to transfer a model from EA to CIMTool |
| Using CIMTool with Protege and Top Braid | How to examine a profile in popular ontology editors |
| Searching Schemas and Profiles | CIMTool's search wizard |
| Profile Maintenance | Repairing profiles when the CIM changes |
| Comparing Profiles and Schemas | Finding out what changed in the CIM or in a profile |
| Exchanging CIMTool Projects | How to export and import whole projects with all settings intact |
| Associations and Anonymous Classes | The full story on profiling an association |
| Multiple Profiles | How to create several different restricted classes for a given CIM class |
| Enumerations Support | How to include enumerated classes in a profile |
| CIMTool Namespace Support | How to use namespaces to manage multiple CIM extensions and profiles |
| Namespaces | An explanation and discussion of namespaces |
| OWL as a Profile Language | CIMTool's representation of a profile in OWL |
| UMLOWL | How CIMTool interprets UML models as OWL ontologies |
| How To Validate CPSM | How to validate CIM/XML documents against a profile |
| Validation Cases | A full suite of CIM/XML validation test cases |
| Validation Rules | The rule language used for CIM/XML validation and how to customise it |
| Generating a Database Schema | Using profiles as a template for a database |
| CIMCheck / CIMCheck API | CIMCheck validation tool and its API |
| Change Log | A summary of changes and new features |



## Updating the Documentation

All documentation is hand-authored HTML. To add or update a topic:

1. Add or edit the HTML file in `html/`
2. If adding a new topic, add a `<topic>` entry to `toc.xml` referencing it via
   the `PLUGINS_ROOT/au.com.langdale.cimtoole.help/html/<filename>.html` path
3. Rebuild and re-export the product — no Java compilation is required

> **Note:** The `PLUGINS_ROOT/` prefix in `toc.xml` href values is an Eclipse
> help framework convention that resolves to the installed plugins directory at
> runtime. Do not use relative paths or absolute file paths in `toc.xml` entries.
