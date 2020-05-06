# CIMTool Development Environment Setup

## Part 1:  Eclipse Installation

#### Installation of the Eclipse 3.7.2 Plugin Development Environment (PDE)

> First, a special edition of the Eclipse development environment is required to develop CIMTool.  This environment is known as the [Eclipse for RCP and RAP Developers](https://www.eclipse.org/downloads/packages/release/indigo/sr2/eclipse-rcp-and-rap-developers) package with newer 4.x version referred to as the Eclipse Committers package.  Since CIMTool is currently built on the older Eclipse 3.x SWT plugins platform this older version of the Eclipse Plugin Development Environment (PDE) is required.  The following steps describe how to locate, install and setup this Eclipse package:

###### Step 1:
The Windows 64-bit version of Eclipse PDE 3.7.2 (Indigo) can be downloaded at [eclipse-rcp-indigo-SR2-win32-x86_64.zip](https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/indigo/SR2/eclipse-rcp-indigo-SR2-win32-x86_64.zip). (Note that versions for Mac and Linux are also available at [Eclipse for RCP and RAP Developers](https://www.eclipse.org/downloads/packages/release/indigo/sr2/eclipse-rcp-and-rap-developers) but the instructions that follow are for Windows only.)

###### Step 2:
Though the CIMTool plugins themselves are configured and compiled for a later version of Java the Eclipse PDE 3.7.2 (Indigo) development environment itself requires Java 1.5.  If you do not have this older version of the JRE or JDK on your system you should locate and install it before moving on to the next step.  Currently, the [Oracle's Java Archive](https://www.oracle.com/java/technologies/oracle-java-archive-downloads.html) contains a [Java Development Kit 5.0 Update 22](https://www.oracle.com/java/technologies/java-archive-javase5-downloads.html#license-lightbox) Windows 64-bit release. Note that you will need to accept Oracles license agreement and have a (free) Oracle account to do so.  Feel free to locate and install Java from an alternative provider if desired.

###### Step 3:
The installation process for the Eclipse environment is straightforward in that it involves simply extracting the [eclipse-rcp-indigo-SR2-win32-x86_64.zip](https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/indigo/SR2/eclipse-rcp-indigo-SR2-win32-x86_64.zip) archive downloaded in step 1.  It should be noted that depending on where you choose to extract the archive that on Windows the process may fail due to path names being too long.  If this occurs it is suggested you create a temp directory on a local drive (e.g. ```C:\temp-indigo```) and extract the ZIP archive into that folder.  A folder called ```\eclipse``` will be created under the selected extract directory and will contain your eclipse environment.  Feel free to relocate this new folder to a location on the file system where you prefer to host your Eclipse development environment.

Note at this stage you may want to create a shortcut to the ```eclipse.exe``` located within the extracted ```eclipse``` directory at the location hosted on the file system.  In the example screen shot below this location is ```C:\eclipse-committers-3.7```.  Though not fully visible in the screen shot the **Target** field contains:

```
C:\eclipse-committers-3.7.2\eclipse.exe -vm "C:\Program Files\Java\jdk1.5.0_22\bin\javaw.exe"
```

In this example the default JRE/JDK on the Windows system is a newer version of Java.  Consequently, the JVM for launching the Eclipse PDE 3.7.2 environment is explicitly specified via the ```-vm "C:\Program Files\Java\jdk1.5.0_22\bin\javaw.exe"``` command line option.

<img src="/assets/img/install-eclipse-shortcut-1.png"  border="1px" style="max-width:100%"/></a>

The host location as referenced for this example would look like the following after the ZIP file is extracted (note the shortcut in this screenshot was created after extraction):

<img src="/assets/img/install-eclipse-shortcut-2.png"  border="1px" style="max-width:100%"/></a>

## Part 2:  Install Additional Plugin Dependencies

> There are two additional plugin dependencies needed by CIMTool that are not pre-packaged with this packaged instance of the Eclipse 3.7.2 Plugin Development Environment.  These dependencies must be installed prior to importing the CIMTool project in Part 3 of these instructions.

#### Installation of the Eclipse EMF 2.6.0 (org.eclipse.emf) Plugin

The installation of this plugin is performed manually using the steps as outlined. Be sure that your instance of Eclipse is **shut down** before performing the installation.

###### Step 1:
Download the [update-site.zip](http://download.scala-ide.org/sdk/helium/e37/scala29/stable/update-site.zip) file and extract it to a local directory on your file system.  The directory structure after extraction should look like:


    |
    site (root directory)
        |
        features (directory containing the scala-ide features .jar files)
        |
        plugins (directory containing the scala-ide plugins .jar files)
        |
        artifacts.jar
        content.jar (this is the archive file that will be selected in a later step)


###### Step 2:
Next launch Eclipse and select the "**Install New Software...**" menu option as shown in the screenshot.

<img src="/assets/img/install-scala-ide-plugin-0.png"  border="1px" style="max-width:100%"/></a>


#### Installation of the ScalaIDE 3.0.0 Plugin

An older version of the Scala-IDE plugin (located at the [ScalaIDE for Eclipse Archive site](http://scala-ide.org/download/prev-stable.html)). The installation steps are:

###### Step 1:
Download the [update-site.zip](http://download.scala-ide.org/sdk/helium/e37/scala29/stable/update-site.zip) file and extract it to a local directory on your file system.  The directory structure after extraction should look like:


    |
    site (root directory)
        |
        features (directory containing the scala-ide features .jar files)
        |
        plugins (directory containing the scala-ide plugins .jar files)
        |
        artifacts.jar
        content.jar (this is the archive file that will be selected in a later step)


###### Step 2:
Next launch Eclipse and select the "**Install New Software...**" menu option as shown in the screenshot.

<img src="/assets/img/install-scala-ide-plugin-0.png"  border="1px" style="max-width:100%"/></a>

###### Step 3:
In the **Install** wizard dialog that is launched click "**Add...**" to launch the **Add Repository** dialog.

<img src="/assets/img/install-scala-ide-plugin-1.png"  border="1px" style="max-width:100%"/></a>

###### Step 4:
In the **Add Repository** dialog click "**Local...**" button:

<img src="/assets/img/install-scala-ide-plugin-2a.png"  border="1px" style="max-width:100%"/></a>

Using the file chooser navigate to the location  where the [update-site.zip](http://download.scala-ide.org/sdk/helium/e37/scala29/stable/update-site.zip) archive was extracted to and select the ```\site``` directory and click "**OK**".

<img src="/assets/img/install-scala-ide-plugin-2b.png"  border="1px" style="max-width:100%"/></a>

Finally, enter a name for this new local plugin repository in the **Name** field.  Note that the **Location** field should now reference the ```\site``` directory you just selected (i.e. the "root" of the file structure described in **step 1**):

<img src="/assets/img/install-scala-ide-plugin-2c.png"  border="1px" style="max-width:100%"/></a>

###### Step 5:
Next, select the check boxes as shown in the **details** sections of the screen shot below.  Once selected click the "**Select All**" button and then "**Next**" to proceed to the next page of the installation wizard.

<img src="/assets/img/install-scala-ide-plugin-3.png"  border="1px" style="max-width:100%"/></a>

###### Step 6:
Continue by clicking "**Next**" to accept the items to be installed.

<img src="/assets/img/install-scala-ide-plugin-4.png"  border="1px" style="max-width:100%"/></a>

###### Step 7:
On the **Review Licenses** page select the "I accept the terms of the license agreements" radio button and click the "**Finish**" button.

<img src="/assets/img/install-scala-ide-plugin-5.png"  border="1px" style="max-width:100%"/></a>

###### Step 8:
A security warning will be displayed.  Just click "**OK**" to continue.

<img src="/assets/img/install-scala-ide-plugin-6.png"  border="1px" style="max-width:100%"/></a>

###### Step 9:
If all was successful a final dialog is displayed asking if you want to restart in order for the plugins to take affect.  Click the "**Restart Now**" button and allow your instance of Eclipse to restart.

<img src="/assets/img/install-scala-ide-plugin-7.png"  border="1px" style="max-width:100%"/></a>

## Part 3:  Clone and Import of the CIMTool Project
