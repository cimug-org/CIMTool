# Namespaces in CIMTool

See also: [CIMTool Support for Extension Namespaces](cimtool-support-for-extension-namespaces.md)

## Background

In CIM/XML, each name, such as ACLineSegment, is qualified by a namespace, such as `http://cim.ucaiug.io/CIM-schema-cim16#`. The combination forms a globally unique term. By convention, only the authority designated by the namespace URI would define a term beginning with `http://cim.ucaiug.io/CIM-schema-cim16#...`

The same applies in XML schema's derived from the CIM. An XML schema has a target namespace, declared in its header, that qualifies names defined in the body of the schema.

In practical applications of the CIM it is combined with project and vendor-specific extensions. Each of these may have an independent author. Separate namespaces are used to prevent name conflicts between them.

### Version Control and Namespaces

Namespaces prevent name conflicts between authors of terms. They are also used to prevent conflicts between different versions of a term's definition. For example, `http://cim.ucaiug.io/CIM-schema-cim17##ACLineSegment` might be succeeded by `http://cim.ucaiug.io/CIM-schema-cim18##ACLineSegment` with a slightly different definition.

Both versions of the term might be in use at the same time. The potential conflict can be detected because the different versions have different namespaces. Resolving a version conflict is another matter, however.

### The Meaning of a Namespace

Namespaces don't carry much information in themselves. If the namespace is an HTTP URI (http://authority/...) then the identity of the issuing authority can be found via the whois service.

But there is no inheritance among namespaces, nor is http:/x/y/ a superset of http://x/y/z/. You can't necessarily obtain a document from a namespace via HTTP protocol either.

In the end, a namespace is just an identifier whose allocation is controlled by the IANA and its delegates.

## Profiles and Namespaces

A profile is a subset of an information model defined such that:

An instance that conforms to the profile also conforms to the general information model.[^1] 

The classes and properties in a profile are subsets, or restrictions, of those in the information model.

What namespace should be used for the terms in a profile? There are two schools of thought.

### Separate Profile Namespaces

The argument here is that profile classes and properties are distinct from the general classes and properties they derive from. Therefore they require distinct terms.

We need a separate namespace and separate terms so we can define restrictions for the profile without affecting the general information model definitions. This also allows us to specify a specific profile to which an instance should conform.

### Same Namespaces for Profile and Information Model

The argument here is that the profile definitions are consistent (see footnote) with those of the general information model. Therefore they can use the same terms. The restricted class ACLineSegment can have the same namespace as the general class ACLineSegment.

An advantage of this is that the origin of any term in the profile is immediately obvious. Its namespace indicates the author (IEC, Vendor etc). There is no need to correlate terms via the profile definition. The same term is used everywhere.

#### A Philosophical Objection

A profile will be consistent with the information model, but different profiles are not necessarily consistent with each other. The same term can denote a different definition in each profile. The terms are not globally distinct despite the use of namespaces. That breaks one of the architectural precepts we adopted at the outset.

#### Practical Objections

In practical terms, the 'same namespace' approach creates a number of problems:

Since we can't distinguish profiles by namespace, there is no commonly understood way to associate instances with them. This becomes a matter for per-project and per-vendor conventions.

Similarly, there is no obvious way to version profiles separately from the general information model. The current practice is to indicate versions via the namespace. It would be usual for profiles to undergo more than one formal release in the lifetime of the system. However, the CIM version would typically be frozen, or updated on a different timetable.

The 'same namespace' approach is unworkable with XML schema's. Normal practice is to give each XML schema its own target namespace. To do otherwise confuses standard tools and makes it impossible to write a WSDL definitions involving more than one profile schema.

[^1]: consistent:  Technically, consistent means that, when all the definitions are asserted at once, there is a non-empty set of instances that conform with them. That is true for profile and general information model definitions. 

