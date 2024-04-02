# CIMTool 

**CIMTool** is an open source tool that supports the Common Information Model (CIM) standards used in the electric power industry.  It is potentially useful for other semantic applications too.

![image](https://github.com/cimug-org/CIMTool/assets/63370413/c348d882-8a7b-48a0-a953-18d62960a3a6)

## Project Supporters

[![image](docs/logos/project-supporter-osi-logo.png)](https://www.osii.com/)   [![image](docs/logos/project-supporter-statnett-logo.png)](https://www.statnett.no/en/)   [![image](docs/logos/project-supporter-ogs-logo.png)](https://www.opengrid.com/)


## CIMTool Discussion Forums

The CIMTool [Google Group](https://groups.google.com/g/cimtool) is no longer actively monitored and at this point only around for archival purposes.  Please post all questions, suggestions, and/or comments directly to the new [CIMTool Discussions](https://github.com/ucaiug/CIMTool/discussions) board for this repository.

## Using CIMTool with Enterprise Architect

Both past and current releases of the CIM Enterprise Architect (EA) project files used for the purposes of CIMTool are made publicly available for download from the UCAIug (CIM Users Group).  Current releases can be found at [Current CIM Model Drafts](https://cimug.ucaiug.org/CIM%20Model%20Releases/Forms/AllItems.aspx).  Older releases are available at [Past CIM Model Releases](https://cimug.ucaiug.org/CIM%20Releases/Forms/AllItems.aspx).  Generally, access to CIM EA project files does not require a UCAIug account. For content restricted to registered users you may create a UCAIug [registered account](https://cimug.ucaiug.org/pages/Join.aspx) for free.

Current versions of the CIM are maintained with the Sparx Systems UML design tool, [Enterprise Architect (EA)](https://sparxsystems.com/). Creating a profile requires that the CIM first be imported into **CIMTool**. The CIM can be imported in one of two possible formats as outlined next.

### Using XMI

The standard way is to export the CIM as an XMI file (i.e. a schema) and to do this from within EA. The exported XMI file can then be imported into **CIMTool**. The following export options within Enterprise Architect will export the version of XMI that can be imported into **CIMTool**:

* XMI Type = UML 1.4 (XMI1.2)
* Unisys/Rose Format = checked
* Export diagrams = unchecked
* stylesheet = blank
* Format XMI Output = checked
* Write log file = checked
* Generate Diagram Images = unchecked

![image](https://user-images.githubusercontent.com/63370413/200277774-aa0c18cb-2250-4798-802d-ce506231fdd8.png)

### Using the EAP File

Exporting XMI can be slow. An alternative is to directly import an Enterprise Architect project file (`.eap` or `.eapx`) into **CIMTool**. This eliminates the need to export anything from EA before importing to CIMTool.  

To import an `.eap` or `.eapx`file into a CIMTool project, use the **CIMTool** Schema Import wizard and select a file type of either `.eap` or `.eapx` respectively when browsing for the file. You must close the project in EA before importing it to CIMTool.

> Note that as of the EA 16.x release, `.eap` and `.eapx` files are no longer supported. **CIMTool** does not currently support the new EA 16.x project file format and instead the XMI schema option should be utilized if using the new release. For further information refer to EA's [EAP/EAPX File to QEA File Format](https://sparxsystems.com/enterprise_architect_user_guide/16.0/model_exchange/transfereap.html) page for a better understanding of changes in 16.x.

### File Format Considerations

The following table highlights the various tradeoffs of utilizing one format over another:

Format | Description | Pros | Cons
----- | -----| -----| -----
`.eap` / `.eapx`| Native EA project files. Standard in EA 15.x releases and earlier the internal format is based on MS Access. Specifically, `.eap` files are based on the Jet3.5 engine and `.eapx` on Jet4.0 (see [Access Database Engine History](https://en.wikipedia.org/wiki/Access_Database_Engine)) with both stored as binaries. <br/><br/>The Jet database engine is available only in 32 bit configurations. Which means that the `.eap` and `.eapx` file formats are still supported in the 32 bit version of EA 16.x, but not in the new 64 bit version of Enterprise Architect 16.0. To keep using the data in an `.eap` or `.eapx` file in a 64 bit release of EA 16.x you must transfer the contents of the file to another project that the 64 bit version can access. This project can be a file (such as a `.qea` or Firebird file) or a database repository (such as a SQL Server or MySQL database). Refer to the Sparx article [Migrate an EAP/EAPX File to QEA File Format](https://sparxsystems.com/enterprise_architect_user_guide/16.0/model_exchange/transfereap.html) for how to peform this. | Both file formats can be imported directly into **CIMTool** without the overhead of having to export as an `.xmi` file. <br/><br/>Multi-language support via unicode is available with an `.eapx` file. | If hosting a CIMTool project on Github it is not recommended that an EA project file be used (see: [About large files on GitHub](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github) and [Git LFS](https://docs.github.com/en/repositories/working-with-files/managing-large-files/configuring-git-large-file-storage)). Instead the `.xmi` file format is recommended. <br/><br/>The `.eap` file format does not support [unicode](https://unicode.org/standard/WhatIsUnicode.html) and therefore is not ideal for profiles derived from CIM classes or attributes with descriptions and/or notes containing non-ASCII characters. NOTE: To convert an `.eap` file to an `.eapx` format see [Use Languages Other Than English](https://sparxsystems.com/enterprise_architect_user_guide/15.0/team_support/check_in_languages_other_than_.html) which has the link to the [EABase JET4](https://sparxsystems.com/bin/EABase_JET4.zip) file that can be used for this purpose. Visit the [Project Data Transfers](https://sparxsystems.com/enterprise_architect_user_guide/15.0/model_publishing/performadatatransfer.html) page for further information on this process. If choosing to migrate to Jet4 then EA must also be configured to use Jet4. Refer to the [General Options](https://sparxsystems.com/enterprise_architect_user_guide/15.0/user_interface/generalsettings.html) page for details.
`.qea` / `.qeax` | Native EA project files. Introduced in EA 16.x the internal format of these files is based on the [SQLLite](https://www.sqlite.org/) open source database and is stored as binaries. Both file types support basic replication with the `.qeax` extension indicating that file sharing is enabled. A `.qeax` file can simply be renamed back to `.qea` to disable file sharing. | _These formats are not yet supported in **CIMTool**._ <br/><br/> These native project file formats will be able to be imported directly into **CIMTool** without the overhead of having to export an `.xmi` file.| | If hosting a CIMTool project on Github the use of one of these project files is not recommended (see: [About large files on GitHub](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github) and [Git LFS](https://docs.github.com/en/repositories/working-with-files/managing-large-files/configuring-git-large-file-storage)). Instead an `.xmi` file should be utilized.
`.xmi` | `.xmi` | Exporting the CIM as an `.xmi` schema file has added flexibility not available when using a native EA project file.  Specifically, a subset of the CIM can be exported by simply selecting a specific package for export. This approach can be used to reduce the size of the schema file in a **CIMTool** project. Given that profiles are commonly defined for a particular domain (e.g. Transmission, Distribution, Market-related profiles) just a subset of the CIM can be exported and used within **CIMTool**. This is more suitable when hosting a **CIMTool** project in Github. <br/><br/> **CIMTool** supports the ability to import multiple `.xmi` schema files and for a user to assign a distinct namespace to each. This is useful when defining and exporting custom extensions as a separate `.xmi` file that coexists alongside an `.xmi` for the CIM model. | Exporting `.xmi` files can be time consuming and therefore inconvenient if quick iterative changes are needed to the CIM with a reimport into a **CIMTool** project. In this scenario it is suggested to use one of the native EA project files. The direct use of a project file eliminates the roundtrip time needed for the "make changes to the UML, export to XMI, import XMI into CIMTool" cycle.

## CIMTool Profiling Tutorial

For further details on how to use CIMTool to create and edit profiles based on the CIM visit the How To section of [https://cimtool.ucaiug.io/](https://cimtool.ucaiug.io/).

## CIMTool-Builders-Library Repository

  Once you get acquainted with **CIMTool** visit the [CIMTool Builders Library](https://cimtool-builders.ucaiug.io/) companion repository that provides information on building your own **CIMTool** builders using XSLT transforms.

## CIMTool Development

  For developers interested in contributing to the **CIMTool** Project detailed instructions on installing and setting up an Eclipse Integrated Development Environment (IDE) can be found at [CIMTool Development Environment Setup](https://cimtool.ucaiug.io/developers/dev-env-setup/)

  Instructions on packaging and deployment of **CIMTool** as an **Eclipse Product** can be  found at [CIMTool Packaging & Deployment Guide](https://cimtool.ucaiug.io/developers/package-deploy/)

  Note that these instructions are purely for those interested in participating in the development of **CIMTool**. They do not describe standard use of the tool for creating profiles.

## Latest Release

  -   2.0.0

      - The latest release is available here on GitHub at [CIMTool-2.0.0](https://github.com/ucaiug/CIMTool/releases/tag/2.0.0) and is delivered as a ZIP file. Releases are also made available in the CIMug [tools download folder](https://cimug.ucaiug.org/Standards%20Artifacts/Forms/AllItems.aspx?RootFolder=%2FStandards%20Artifacts%2FUCA%20TF%20Tools&FolderCTID=0x0120001062F2F1DF27704DBB748ABBDC3B3AA2&View=%7BFEBD8EE1%2D6B40%2D42F6%2DB228%2DCCF131291FBE%7D) on the UCAIug website.
      - Information on features and fixes for the release can be found [here](https://cimtool.ucaiug.io/release-notes).

## Installation & Setup

For instructions on installation and setup of **CIMTool** see [CIMTool Installation & Setup](https://cimtool.ucaiug.io/getting-started/).

## License

  Distributed under the LGPL-2.1 license. See [LICENSE](LICENSE) for more information.
