# Development Environment Setup

Instructions for setting up a development environment for the CIMTool 2.x release
line: installing the recommended Eclipse IDE, configuring a Java runtime, and
cloning and importing the CIMTool projects into an Eclipse workspace. Active
development on the CIMTool 1.x release line has ceased; these instructions apply
to the 2.x line only.

> **Note:** A specific Eclipse edition and release is recommended to eliminate
> unanticipated configuration and setup issues. Development and deployment of
> CIMTool against this release has been fully tested and its plugin dependencies
> verified out of the box. Questions may be posted to the
> [CIMTool 2.x Release Line — Development Community Discussion Board](https://github.com/cimug-org/CIMTool/discussions/92).


## Eclipse Installation

CIMTool is developed against a specific Eclipse IDE edition and release. The steps
below cover locating, installing, and configuring it on Windows. Editions for
macOS and Linux are available from the same downloads site, but the instructions
here are Windows-specific.

### Step 1: Download the Eclipse IDE

The recommended IDE is the *Eclipse IDE for Enterprise Java and Web Developers*
from the [Eclipse IDE 2023-06 R packages](https://www.eclipse.org/downloads/packages/release/2023-06/r).
The Windows 64-bit archive is available [here](https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/2023-06/R/eclipse-jee-2023-06-R-win32-x86_64.zip).

![Eclipse 2023-06 (4.28.0) About dialog](readme-images/eclipse-4.28.0-about.png)

### Step 2: Install a JDK

CIMTool requires Java 20 or newer. If a suitable JDK is not already installed, one
must be installed before continuing. The [Azul Zulu OpenJDK](https://www.azul.com/downloads/?package=jdk#zulu)
distribution provides a freely available Windows 64-bit option; the `.msi`
installer makes installation straightforward.

![Azul Zulu OpenJDK downloads](readme-images/azul-jdk-downloads.png)

### Step 3: Extract the Eclipse IDE

Installation consists of extracting the downloaded
`eclipse-jee-2023-06-R-win32-x86_64.zip` archive. Depending on the chosen
extraction location, the process may fail with "path too long" errors on Windows.
If this occurs, create a directory off the root of a local drive (for example,
`C:\temp-eclipse`) and extract into it; the shorter base path avoids the error.
Extraction creates an `\eclipse` folder under the chosen directory containing the
IDE, which may then be relocated to a preferred location on the file system.

### Step 4: Register the Installed JRE in Eclipse

When a newer JDK has been installed in Step 2, it must be registered in the Eclipse
IDE configuration before the CIMTool projects are imported.

![Configure the installed JRE — step 1](readme-images/eclipse-configure-jre-step1.png)

![Configure the installed JRE — step 2](readme-images/eclipse-configure-jre-step2.png)

![Configure the installed JRE — step 3](readme-images/eclipse-configure-jre-step3.png)

![Configure the installed JRE — step 4](readme-images/eclipse-configure-jre-step4.png)

![Configure the installed JRE — step 5](readme-images/eclipse-configure-jre-step5.png)


## Cloning and Importing the CIMTool Projects

The final step is to clone a development branch of the CIMTool codebase from the
[CIMTool GitHub repository](https://github.com/cimug-org/CIMTool) and import its
projects into the workspace. Two approaches are available:

- An external Git client — such as [GitHub Desktop](https://desktop.github.com/),
  [TortoiseGit](https://tortoisegit.org/), or [Git for Windows](https://gitforwindows.org/)
  (see the [full list](https://git-scm.com/downloads/guis)) — used to clone the
  repository to a local directory, from which the projects are then imported into
  Eclipse.

- The Git tooling bundled with Eclipse (EGit), used to clone and import the CIMTool
  projects directly from the repository. It is configured from the Eclipse
  preferences dialog.
