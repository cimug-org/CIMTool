# Editing a Profile
In the navigator view, find the `.owl` file for the Contextual Profile you want to edit in the Profiles folder and right click. In the context menu choose "Profile Editor". The editor pane will show a blank Add/Remove page and the outline view will show a single root element.

- Editing is driven from the outline view. Select the root element there and the Add/Remove page will be populated, showing two panes. Add one or more classes to as top level definitions by selecting them in the right pane can clicking the '<' button to move them to the left pane.
- Double click one of the newly added classes in the left pane. The outline view, project model view, and Add/Remove page will all update and focus on the selected class.
- Select attributes and associations of the class and click the '<' button to add these to the profile.
- To produce profile artifacts, such as an XML schema, select the Summary tab and switch on the builder for XSD. Click the save button. As well as saving the profile definition to example.owl, the example.xsd file (in the same folder) will be generated. Open this file (ideally in the eclipse XML Schema editor) to see the results. The schema will update automatically every time the profile is saved.

!!! tip

    Folders and files that have errors will have a red 'X' on their associated icon. If the add/remove page is blank, select a message element in the outline view on the right. Similarly, if the Project Model view is blank select a message element, or just select the project in the navigator view.  You can explore the information model in the project model view. Double click an association to navigate to its opposite end.  You can place more than one XMI file in the Schema folder are they will be merged. In the File menu choose Import|CIMTool Schema to add XMI files to a project.