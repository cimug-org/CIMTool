# Generating a Database Schema
**CIMTool** can generate a database schema based on a CIM Contextual Profile. Potential uses of this feature might include:

  * Prototyping a CIM-based application, or
  * Creating a staging store for CIM-based data

!!! warning

    You could use a generated schema in a production application or product. However, remember that a new CIM UML version is released each year. The use of a profile provides some protection against CIM changes. In some cases, the profile can remain the same even though the underlying CIM changes. Even so, it may be difficult to prevent the churn in CIM versions from impacting the schema and the impact of that on the application as a whole may be unsustainable.

## Getting Started
To generate SQL statements for a profile, select the "SQL" checkbox on the summary page in the profile editor. When the profile is saved, a file with the same name as the profile and extension `.sql` will be generated in the Profile folder of the project.

## What is in the Schema
The generated schema closely follows the structure of the profile as seen the Outline view.

  * Each **ordinary** class results in a table. 
    * A primary key column called "mRID" is declared.
  * Each **enumerated class** results in a table
    * A primary key column called "name" is declared.
    * A record is inserted for each enumerated value.
  * Each **datatype property** (UML attribute) results in a column.
    * The column type is selected on the basis of the XML Schema Part II datatype of the property.
    * If the property is required (minimum cardinality of 1) the column is marked NOT NULL.
  * Each **enumeration property** (UML enumeration attribute) results in a column
    * A foreign key constraint is added such that the referenced table corresponds to the property type and the referenced column is "name".
    * If the property is required (minimum cardinality of 1) the column is marked NOT NULL.
  * Each **object property** (UML association) results in a column
    * The maximum cardinality of the property must be 1 (many valued properties are ignored).
    * A foreign key constraint is added such that the referenced table corresponds to the property type and the referenced column is "mRID".
    * If the property is required (minimum cardinality of 1) the column is marked NOT NULL.
  * Each **subclass relationship** results in a foreign key constraint.
    * The foreign key is defined on the "mRID" column of the subclass table and references the "mRID" column of the superclass table.

## Guidelines for Profiles
There are a few guidelines that should be observed when constructing profiles for database schemas. They concern:

  * Choice of Identifiers
  * Profile Structure

### Choice of Identifiers
Database systems generally place a length limit on table and column names and these names are normally case-insensitive. CIM class, attribute and association role names are sometimes longer than the limit and are case sensitive.

The identifiers in the profile are by default copied from the CIM, but can be changed on the detail page of the profile editor. In most cases, only one or two names need to be changed in the profile to comply with the database restrictions.

### Profile Structure
It is possible to create a profile with a property referencing a class outside the profile. A profile may also contain local (or nested) class definitions. In either case, the generated schema will contain an invalid foreign key constraint because the referenced table will not exist in the schema.

The rule is that the type of each object property and each enumeration property must be a global class. Global classes are the named classes and appear at the first level of the profile outline display.

The Reorganize and Repair tool can be used to enforce this rule. This tool is activated from the summary page of the profile editor. The "Reorganize profile per RDFS rules" option will introduce any global classes needed.

!!! note

    This option makes other changes as well. It merges classes based on the same schema class into a single profile class. It also merges all restrictions of the same schema property into a single restriction. The merged restriction is moved to the common base class of the individual restrictions. If necessary that class is added to the profile.