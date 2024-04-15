# Using CIMTool with Protege and Top Braid
CIMTool is interoperable with popular ontology editors. It has been tested with Protege 4 and TopBraid Composer 3.2. (Note that Protege 3.2 is not directly compatible).

Both editors have similar internals to **CIMTool**. They use [Apache Jena](https://jena.apache.org/) (to some extent) and are built in Java using the OSGI platform that underlies Eclipse. In fact, Top Braid is based on the Eclipse workbench and it is possible to install **CIMTool** in TopBraid and use them simultaneously on the same project.

The main components of **CIMTool** that make this possible are the ontology header and proper representation of datatypes. All OWL ontologies produced by **CIMTool** are read correctly by the two editors tested. That includes the main profile files (`.owl`), the standalone profiles (`.simple-owl` and friends) and the merged schema (`.merged-owl`).

This page contains some instructions for getting started with using these editors.

## Rebuilding Your OWL Documents
When you open an existing project with **CIMTool** none of the OWL files are changed unless you cause them to be changed. This is a design decision to prevent unexpected side effects. Before using these files with an ontology editor you need to rebuild them. Either:

  * Create a new **CIMTool** project an import the XMI schema and profiles from an old project. (Be careful to get the namespace of the schema right!).
  * Rebuild a specific profile and its dependent artifacts. Open the profile in the profile editor, make a change, and save it.

Then rebuild (or create) an OWL version of the CIM UML, delete the `schema.merged-owl` file (if present) and use the export wizard to rebuild it. From the menu: File > Export > CIMTool > Export merged schema. You can accept the defaults in this wizard.

**CIMTool** often uses anonymous classes to represent the type of a property, as restricted in a profile. Such classes in an existing profile are not always recognised properly by the ontology editors (they may be interpreted as individuals). Newly created definitions include more context and are properly recognised.

You may not want to recreate these classes in a large profile just so they appear properly in the ontology editor. Instead, if your profile is intended for CIM/XML (RDF) usage, you can reorganize it. First make a copy. Then, in the profile editor summary tab, click Reorganise and Remap > Reorganise per RDFS rules.

## Protege
[Download](https://protege.stanford.edu) Protege. You need version 4 or higher for use with **CIMTool**.

Once installed and running you view an ontology by selecting "Open OWL Ontology" and navigating to the folder containing your **CIMTool** project. You can choose any OWL file (ie `.owl`,`.simple-owl` and friends, or `.merged-owl`).

When you open a `.owl` file you may want to see the definitions from the CIM UML together with the profile. (The `.owl` files are not self-contained, they add definitions to the CIM UML.)

  * Ensure that the schema.merged-owl file exists or create it with the export wizard.
  * From the menu open the libraries list: File > Ontology Libraries...
  * Add the CIMTool project folder to the list.

Protege will then locate the CIM OWL file based on the import definition in the profile.

## TopBraid Composer
[Download](http://www.topquadrant.com/products/TB_download.html) TopBraid. You may want to try the free edition.

TopBraid uses the familiar Eclipse workbench and its workspaces and projects to organise its files. Once installed and running you can create a project and import OWL files from your **CIMTool** project.

  * Use File > Import > Filesystem and navigate to your CIMTool project and select the files of interest.
  * Rename the imported files to have a .owl extension so that TopBraid will recognise their type.


As with mentioned above, when you view a `.owl` file you may want the corresponding CIM schema. In TopBraid, it is only necessary for the referenced schema to be present in the project to be found.
