# Create a Project
To create a new project. Select File -> New -> CIMTool Project.

![CreateNewProject.png](../images/CreateNewProject.png "Create New Project")

In the dialog box that appears, give it a name (e.g. 'MyProject') then click 'Next'. 

![ProjectNameLocation.png](../images/ProjectNameLocation.png "Project Name and Location")

In the next dialog box that appears. select whether or not you want to include copyrights within your auto-generated artifacts (e.g. XSD, RDFS, etc.). Here you can load your own custom copyright headers or use the default UCAIug Apache 2.0 copyright headers provided by CIMTool. If you do not require copyrights you can simply select 'Do not include copyrights'.

![ProjectCopyrightTemplatesConfig.png](../images/ProjectCopyrightTemplatesConfig.png "Project Copyright Templates")

Here we have specified a project without copyrights for now. Click 'Next' or 'Finish' depending on whether you will be specifying a CIM schema for your project at this time. It is suggested, but not required, to specify your schema while creating your project by clicking 'Next'.

Note:  it is possible to add copyrights later to your project via 'File -> Import...'

![ProjectCopyrightTemplatesConfig2.png](../images/ProjectCopyrightTemplatesConfig2.png "Project Copyright Templates")

Next in the Create New Project wizard you will be asked to specify an initial base schema. Click 'Browse...'

![ImportInitialSchema.png](../images/ImportInitialSchema.png "Import Initial Schema")

Select the source file you want to use as your schema.  This could be an `.xmi` file or a Sparx EA 15.x `.eap` or 16.x `.qea` project file for example. 

![SelectSchemaFileDialog.png](../images/SelectSchemaFileDialog.png "Select Schema File Dialog")

Last you must declare the namespace URI to be associated with the CIM schema you chose in the previous step. At this point you can select one of the radio buttons to specify a predefined namespace or select the 'Preference' radio button to enter your own. Click the 'Finish' button. A new project folder is created in your CIMTool workspace.

![ImportInitialSchema2.png](../images/ImportInitialSchema2.png "Import Initial Schema")
