# Comparing Profiles and Schemas
CIMTool can help you track changes in the CIM that affect your profiles and track changes between different versions of a profile.

## Comparing Schema (i.e. CIM) Versions
To compare two versions of the CIM they should be both present in the workspace. In other words, you must create two projects containing the respective CIM versions.

  * In the Project Explorer, select the .xmi file containing the first version of the CIM. This is located in the Schema folder of the first project.
  * Add the .xmi file for the second version of the CIM to the selection. That is, hold the Control Key and select the file in the Project Explorer. It is located in the schema folder of the second project.
  * With both .xmi files selected, right click and in the context menu choose Compare with each other...
  * The eclipse compare editor opens.

## The Compare Editor
The eclipse compare editor gives a structured view of the differences between two schemas or two profiles.

  * The tree view in the top left pane shows the added, removed and changed elements.
  * When a changed element is selected, the detail pane below shows the differences in that element.

## Comparing Profile Versions
To compare two profiles they should both be present in the workspace. They may be in the same project or different projects. As with schema comparisons, the two candidates must be selected:

  * In the Project Explorer, select the .owl file representing the first profile and add the second .owl file to the selection (with Control-Select).
  * Right click and in the context menu choose Compare with each other...
  * The eclipse compare editor opens. Navigate the differences as described above.

## Quirks and Caveats
The Eclipse Modelling Framework (EMF) defines its own comparison editor for .xmi files. If this editor is installed in your version of eclipse you will be unable to use the CIMTool schema compare editor. Unfortunately, eclipse is prone to hanging in that case if a comparison of .xmi files is attempted.