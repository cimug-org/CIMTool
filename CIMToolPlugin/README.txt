README for CIMTool VERSION
===========================

This is a beta release of CIMTool. CIMTool now works with 
the latest CIM version and several XMI formats. It produces
valid OWL Lite and WG14-style XML schemas. The message editor is
feature-complete.

Further releases and source code will be posted on http://CIMTool.org

Expect to see step by step instructions there in the future also.

Installation and Prerequisites
------------------------------

This alpha has only been tested on Windows and Linux with 
Java 1.5 (aka Java 5) at this stage. It is a pure java application 
with no particular operating system dependencies.

CIMTool does not work on Java 1.4, but that can be corrected in future, 
if required.  

To install:

1. Install a Java Runtime Environment (JRE) v1.5 or check that you have this
installed.

2. Unzip CIMToolVERSION.zip which will create directory CIMToolVERSION. This
directory may be moved to any convenient location. 

Running
-------

Open (double-click) CIMTool.jar, which is in the  directory CIMToolVERSION.
 
Alternatively, run the command:  java -jar CIMTool.jar 

You should then be presented with a wizard style user interface. 

Check the CIMTool.org web site for further instructions or, if none
are posted there yet, follow the wizard instructions.

Below is a summary of the top-level options.

Note:

- In the following, the CIM File can be supplied as XMI 1.1/1.2+ UML 1.4 format 
(with file names *.xmi or *.xml) or as an OWL ontology previously exported
(with filename *.owl or *.rdf).  Variants of the XMI format from the Rose
Unisys plugin, Poseidon and Rational XDE have been tested.

Look on cimtool.org to find samples or look on http://cimuser.org for the
latest CIM XMI file (which is in Unisys format).

- The XML schemas produced are intended to be compatible with the WG14 message
syntax.  To use them, you will need the file mdiMessage.xsd which defines the
WG14 message header and object association syntax. A copy of this file is available
on cimtool.org in the Downloads section.

- Merge and Browse Models

	This option lets you specify a CIM model and one other, 
	local, model.  The models are merged and presented in
	a browser. 
	
	CIMTool checks for inconsistencies between the local model
	and the CIM.  It will highlight inconsistent elements in red.

- Create or Edit a Message definition.

	This option guides you through the process of creating a 
	message definition based on the CIM (as extended by 
	your local model, if any).  
	
	The inputs are CIM and local model files, and a message 
	definition file. (The latter will be created if required.)

	The output is an OWL file containing a set of mappings
	between the CIM and a message structure. CIMTool 
	can re-open this file for further editing without loss
	of information. 

	We call this artifact an 'abstract message definition'
	because it is not commited to any particular message syntax. 
	That comes next...
	
- Generate Message XML Schema

	This option guides you through the process of generating
	and XML schema based on a message definition.  
	The inputs are the same as for the previous step.
	
	The output will be an XML Schema document (normally). 
	The WG14 message syntax is the default, although there
	are other options.  

- Generate OWL Ontology

	This option generates an OWL ontology document from 
	the CIM and (optionally, a local model).  
	
	The resulting ontology is compatible with the CIM/XML
	standard.   That is, the OWL file is equivalent to
	the RDFS file used in CIM/XML.

	The output strives to conform to the OWL/Lite subset
	which should make it usable in a number of ontology
	browsers and editors.

Advanced Usage
--------------

The form of the XSD generated is controlled by XML Stylesheets.  Thus you can 
retarget CIMTool to any schema language or XML syntax style.  

In this version the stylesheets are fairly easy to write.  You can probably
fine tune the generated schemas without much XSLT knowledge.  Try extracting 
and editing the xsdspec.xsl file found in CIMTool.jar (use WinZIP or similar).

To test your stylesheet, use the 'Custom' option  on the XML Schema generation 
page of CIMTool.
  
You can also obtain the raw message definition as an XML document by selecting
the 'Abstract' option.  This is a simple XML format that is easy to transform into 
XSD, HTML or other forms. 

Contact
-------

Arnold deVos
adv@langdale.com.au





