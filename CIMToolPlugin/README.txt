== License ==

This is a preliminary release of CIMTool Eclipse Plugin for evaluation and beta
testing.  The final release will be made on an open source basis.  This release
is made under the terms in the file LICENSE.txt included in the distribution.

== Getting Started ==

1. Prerequisites: You will need Java 5 or 6 and Eclipse 3.3. Ideally, you
should also have the XML schema editor plugin.  

2. Unzip the CIMToolPlugin file in the eclipse installation directory
(ie in the same directory where you find eclipse.exe).  Restart eclipse.

3. Open the CIMTool perspective.  If this is the first time you have run eclipse
then dismiss the welcome screen.  Select the CIMTool perspective from the 
top right toolbar.  

4. Create a new CIMTool project. Select File|New|CIMTool Project.  
You will need a CIM XMI file (with extension .xmi) to complete the wizard. 

5. Create a new Profile (ie message definition).   Select File|New CIMTool Profile.
In the wizard, nominate the project you just created and give this profile a 
name.  I will assume it is called "example".

=== Editing a Profile ===

6. In the navigator view find example.owl in the Profiles folder
and right click.  In the context menu choose "Profile Editor". The
editor pane will show a blank Add/Remove page and the outline view will show
a single root element.

7. Editing is driven from the outline view.  Select the root element
there and the Add/Remove page will be populated, showing two panes.  Add one or
more classes to as top level definitions by selecting them in the right pane
can clicking the '<' button to move them to the left pane. 

8. Double click one of the newly added classes in the left pane. The outline view,
project model view, and Add/Remove page will all update and focus on the 
selected class. 

9. Select attibutes and associations of the class and click the '<' button
to add these to the profile. 

10. To produce profile artifacts, such as an XML schema, select the Summary
tab and switch on the builder for XSD.  Click the save button.  
As well as saving the profile definition to example.owl, the example.xsd file 
(in the same folder) will be generated.  Open this file (ideally in the eclipse 
XML Schema editor) to see the results.  The schema will update automatically every 
time the profile is saved.

=== Tips ===

 * If the add/remove page is blank, select a message element in the 
 outline view on the right.
 
 * Similarly, if the Project Model view is blank select a message
element, or just select the project in the navigator view.

* You can explore the information model in the project model view.
Double click an association to navigate to its opposite end. 

 * You can place more than one XMI file in the Schema folder are 
 they will be merged.  In the File menu choose Impport|CIMTool Schema 
 to add XMI files to a project. 
 