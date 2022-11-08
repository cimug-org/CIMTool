# CIMTool

**CIMTool** is an open source tool that supports the Common Information Model (CIM) standards used in the electric power industry.  It is potentially useful for other semantic applications too.

![image](https://user-images.githubusercontent.com/63370413/186975970-e0afe4f1-1b09-4d61-b060-577b255db027.png)

## The CIMTool Wiki

  The original [CIMTool wiki](https://wiki.cimtool.org/), though not actively maintained with new content, is a great resource for information. The [download](https://wiki.cimtool.org/Download.html) page hosts releases <= 1.9.7 and the [CIMTool Topics](https://wiki.cimtool.org/CIMTool_Topics.html) page provides a wealth of articles on topics that may be of interest.

## CIMTool Discussion Forums

  The are two avenues for discussion for **CIMTool**.  Though not as active as it once was you can join the CIMTool Google Group by registering [here](https://groups.google.com/g/cimtool). Alternatively, you can post direct to the new [discussions](https://github.com/CIMug-org/CIMTool/discussions) section of this repository.
  
## Using CIMTool with Enterprise Architect

Both past and current releases of the CIM Enterprise Architect (EA) project files used for the purposes of CIMTool are made publicly available for download from the UCAIug (CIM Users Group).  Current releases can be found at [Current CIM Model Drafts](https://cimug.ucaiug.org/CIM%20Model%20Releases/Forms/AllItems.aspx).  Older releases are available at [Past CIM Model Releases](https://cimug.ucaiug.org/CIM%20Releases/Forms/AllItems.aspx).  Generally, access to CIM EA project files does not require a UCAIug account. For content restricted to registered users you may create a UCAIug [registered account](https://cimug.ucaiug.org/pages/Join.aspx) for free.

Current versions of the CIM are maintained with the Sparx Systems UML design tool, [Enterprise Architect (EA)](https://sparxsystems.com/). Creating a profile requires that the CIM be imported from EA into **CIMTool**. 

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

Exporting XMI can be slow. An alternative is to directly import an Enterprise Architect project file (```.eap``` or ```.eapx```) into **CIMTool**. This eliminates the need to export anything from EA before importing to CIMTool. 

An ```.eap``` file is actually a Microsoft Jet 3.5 database, and an ```.eapx``` file is a Microsoft Jet 4.0 database. The key difference is that Jet4 supports Unicode and  therefore is suited for multi-language support. Though not required it is recommended that you migrate your ```.eap``` project file to the Jet4 format.  Some preparation is required to make this work. The EA project must first be converted from Microsoft Access Jet3 format to Jet4 format. See [Use Languages Other Than English](https://sparxsystems.com/enterprise_architect_user_guide/15.0/team_support/check_in_languages_other_than_.html) which has the link to the [EABase JET4](https://sparxsystems.com/bin/EABase_JET4.zip) file that can be used to migrate from Jet3 to Jet4.  Visit the [Project Data Transfers](https://sparxsystems.com/enterprise_architect_user_guide/15.0/model_publishing/performadatatransfer.html) page for further information on this process. If choosing to migrate to Jet4 then EA must also be configured to use Jet4.  Refer to the [General Options](https://sparxsystems.com/enterprise_architect_user_guide/15.0/user_interface/generalsettings.html) page for details. 

To import an EAPX (Jet4) file into a CIMTool project, use the **CIMTool** Schema Import wizard and select a file type of either ```.eap``` or ```.eapx``` when browsing for the file. You must close the project in EA before importing it to CIMTool.

> Note that as of the EA 16.x release, ```.eap``` and ```.eapx``` files are no longer supported. **CIMTool** does not currently support the new EA 16.x project file format and instead the XMI schema option should be utilized if using the new release. For further information refer to EA's [EAP/EAPX File to QEA File Format](https://sparxsystems.com/enterprise_architect_user_guide/16.0/model_exchange/transfereap.html) page for a better understanding of changes in 16.x. 

## CIMTool Profiling Tutorial

The [CIMTool Tutorial](CIMTool_Tutorial.pdf) PDF provides a detailed overview on how to use CIMTool to create and edit profiles based on the Common Information Model. It also highlights many of CIMTool's core features. The PDF is an extract from a publicly available resource on the UCAIug website ([here](https://cimug.ucaiug.org/CIMGroups/CIMInterop/IEEE%20Tutorial/CIM%20Tutorial%20Master%20Slide%20Deck-2013.pdf?Mobile=1&Source=%2FCIMGroups%2FCIMInterop%2F%5Flayouts%2F15%2Fmobile%2Fviewa%2Easpx%3FList%3D19ff6048%2De79f%2D46d7%2D8825%2Ddd1416ad3397%26View%3D022235bd%2D8a45%2D4bc7%2D98fc%2D9145d38d1795%26wdFCCState%3D1)) 

Note that some content is dated:
* pp. 9 - 10: The information on this slide id dates.  See [CIMTool Installation & Setup](https://github.com/CIMug-org/CIMTool/blob/gh-pages/cimtool-installation-and-setup.md) for the latest information.
* pp. 22 - 23: The EA screenshots are from a much older release.
* p. 55: CIMTool's output options are greatly expanded.
* p. 56: CIMTool also generates MS Word compatible RTF documents.
* p. 86: This GitHub repository is now the official site for CIMTool development.

## CIMTool-Builders-Library Repository

  Once you get acquainted with **CIMTool** visit the [CIMTool-Builders-Library](https://github.com/CIMug-org/CIMTool-Builders-Library) companion repository that provides information on building your own **CIMTool** builders using XSLT transforms.

## CIMTool Development

  For developers interested in contributing to the **CIMTool** Project detailed instructions on installing and setting up an Eclipse 3.7 (Indigo) Plug-ins Development Environment can be found at [CIMTool Development Environment Setup](https://github.com/CIMug-org/CIMTool/blob/gh-pages/dev-env-setup.md)

  Instructions on packaging and deployment of **CIMTool** as an **Eclipse Product** can be  found at [CIMTool Packaging & Deployment Guide](https://github.com/CIMug-org/CIMTool/blob/gh-pages/cimtool-deploy-instructions.md)

  Note that these instructions are purely for those interested in participating in the development of **CIMTool**. They do not describe standard use of the tool for creating profiles.

## Latest Release

  -   1.11.0

      - The latest release is available here on GitHub at [CIMTool-1.11.0](https://github.com/CIMug-org/CIMTool/releases/tag/1.11.0) and is delivered as a ZIP file. Releases are also made available in the CIMug [tools download folder](https://cimug.ucaiug.org/Standards%20Artifacts/Forms/AllItems.aspx?RootFolder=%2FStandards%20Artifacts%2FUCA%20TF%20Tools&FolderCTID=0x0120001062F2F1DF27704DBB748ABBDC3B3AA2&View=%7BFEBD8EE1%2D6B40%2D42F6%2DB228%2DCCF131291FBE%7D) on the UCAIug website.
      - Information on features and fixes for the release can be found [here](https://cimug-org.github.io/CIMTool/).

## Installation & Setup

For instructions on installation and setup of **CIMTool** see [CIMTool Installation & Setup](https://github.com/CIMug-org/CIMTool/blob/gh-pages/cimtool-installation-and-setup.md).

## License

  Distributed under the LGPL-2.1 license. See [LICENSE](LICENSE) for more information.
