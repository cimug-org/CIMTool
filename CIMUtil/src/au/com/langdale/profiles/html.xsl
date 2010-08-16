<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:a="http://langdale.com.au/2005/Message#">

	<xsl:output indent="yes" doctype-public="-//W3C//DTD XHTML 1.1//EN" doctype-system="http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd" />
	<xsl:param name="version"></xsl:param>
	<xsl:param name="baseURI"></xsl:param>

	<xsl:template match="a:Catalog">
		<!--  the top level template generates the html and body elementa -->
		<html>
			<head>
				<title>Profile Documentation</title>

<style type="text/css">
/* typography: fonts */
body { font-family: arial, sans-serif; }

.name, .type, .cardinality, .superclass, .xsdtype, .namespace { font-family: courier, monospace; }


/* typography: emphasis */
h1 { color: white; background: gray;}

h2, h3 { color: gray; text-decoration: none;}

h2.abstract, h2.enumerated, h2.domain {font-style: italic } 

p.note { background-color: #ddd }

p.declaration { color: gray }

:link { color: purple; }

.xsdtype { color: black; font-weight: bold; }

.namespace { color: black }

.package { font-size: x-large; color: gray }


/* borders */
div.group {
	border-style: solid;
	border-color: gray;
	border-width: 1px;
	
}

th, td {
	border-top-style : solid;
	border-top-width : 1px;
	border-color: gray;
 }


/* spacing and alignment */
body {padding: 5px; }

h1 { padding: 5px;}

div.group {
	margin-top: 10px;
	padding: 5px;
	position: relative;
}
th, td {
	text-align : left;
	vertical-align : top;
}
th, td.type { width: 15em; overflow: visible; }

p.cardinality { width: 4em; }

p.package { position: absolute; right: 10px; top: 0px}				
</style>
				
			</head>
			<body>
				<h1>Profile Documentation</h1>
				<p><xsl:value-of select="a:Note" /></p>
				<p class="declaration">
					Profile namespace:
					<span class="namespace"><xsl:value-of select="$baseURI" /></span>
				</p>
				<h1>Concrete Classes</h1>
				<xsl:apply-templates select="a:Root|a:Message">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
				<h1>Abstract Classes</h1>
				<xsl:apply-templates select="a:ComplexType"  >
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
				<h1>Enumerations</h1>
				<xsl:apply-templates select="a:EnumeratedType" >
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
				<h1>Compound Types</h1>
				<xsl:apply-templates select="a:CompoundType"  >
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
				<h1>Datatypes</h1>
				<xsl:apply-templates select="a:SimpleType" >
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="a:Message">
		<!--  generates an envelope element for the message -->
		<div id="{@name}" class="group">
		<h2><xsl:value-of select="@name"/></h2>
		<xsl:apply-templates mode="annotate" />
		<xsl:apply-templates />
		</div>
	</xsl:template>

	<xsl:template match="a:Root">
		<!--  generates the root payload element definitions -->
		<div id="{@name}" class="group">
        <a href="#{@name}">
        <h2 class="concrete"><xsl:value-of select="@name"/></h2>
        </a>
		<xsl:call-template name="complex_type" />
		</div>
	</xsl:template>
    
    <xsl:template name="complex_type">
	    <!-- generates the body of a class -->
        <p class="package"><xsl:value-of select="@package"/></p>
        <xsl:apply-templates mode="annotate" />
        <xsl:if test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated">
	        <h3>Native Members</h3>
	        <table>
				<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated" />
			</table>
		</xsl:if>
		<xsl:if test="a:SuperType">
			<h3>Inherited Members</h3>
        	<xsl:apply-templates select="a:SuperType" mode="inherited"/>
        </xsl:if>	
	</xsl:template>

	<xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Domain">
		<!--  generates a property -->
		<tr>
	        <th><p class="name" id="{../@name}.{@name}"><xsl:value-of select="@name"/></p></th>
	        <td><p class="cardinality"><xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></p></td>
	        <td class="type"><p class="type"><a href="#{@type}"><xsl:value-of select="@type"/></a></p></td>
	        <td> <xsl:apply-templates mode="annotate" /> </td>
        </tr>
	</xsl:template>

	<xsl:template match="a:Simple">
        <!--  generates an attribute with an xsd part 2 simple type -->
        <tr>
	        <th><p class="name" id="{../@name}.{@name}"><xsl:value-of select="@name"/></p></th>
	        <td><p class="cardinality"><xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></p></td>
	        <td class="type"><p class="type"><xsl:value-of select="@xstype"/></p></td>
	        <td> <xsl:apply-templates mode="annotate" /> </td>
        </tr>
	</xsl:template>

	<xsl:template match="a:SuperType" mode="inherited">
		<xsl:apply-templates select="//a:ComplexType[@name=current()/@name]" mode="inherited"/>
		<xsl:apply-templates select="//a:Root[@name=current()/@name]" mode="inherited"/>
	</xsl:template>
	
	<xsl:template match="a:ComplexType|a:Root" mode="inherited">
		<table>
		  <xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated" mode="inherited"/>
		</table>
        <xsl:apply-templates select="a:SuperType" mode="inherited"/>
	</xsl:template>

	<xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Domain" mode="inherited">
		<!--  generates an inherited property -->
		<tr>
	        <th><p class="name"><xsl:value-of select="@name"/></p></th>
	        <td><p class="cardinality"><xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></p></td>
	        <td class="type"><p class="type"><a href="#{@type}"><xsl:value-of select="@type"/></a></p></td>
	        <td><p>see <a class="superclass" href="#{../@name}.{@name}"><xsl:value-of select="../@name"/></a></p></td>
        </tr>
	</xsl:template>

	<xsl:template match="a:Simple"  mode="inherited">
        <!--  generates an inherited attribute with an xsd part 2 simple type -->
        <tr>
	        <th><p class="name" ><xsl:value-of select="@name"/></p></th>
	        <td><p class="cardinality"><xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></p></td>
	        <td class="type"><p class="type"><xsl:value-of select="@xstype"/></p></td>
	        <td><p>see <a  class="superclass" href="#{../@name}.{@name}"><xsl:value-of select="../@name"/></a></p></td>
        </tr>
	</xsl:template>

    <xsl:template match="a:ComplexType">
      <div id="{@name}" class="group">
        <a href="#{@name}">
          <h2 class="abstract"><xsl:value-of select="@name"/></h2>
        </a>
        <xsl:call-template name="complex_type" />
      </div>
    </xsl:template>

    <xsl:template match="a:CompoundType">
      <div id="{@name}" class="group">
        <a href="#{@name}">
          <h2 class="abstract"><xsl:value-of select="@name"/></h2>
        </a>
        <p class="package"><xsl:value-of select="@package"/></p>
        <xsl:apply-templates mode="annotate" />
        <h3>Members</h3>
	    <table>
		  <xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated" />
		</table>
      </div>
    </xsl:template>

	<xsl:template match="a:SimpleType">
		<!--  declares a a CIM domain type in terms of an xsd part 2 simple type -->
		<div id="{@name}" class="group">
        <a href="#{@name}">
        <h2 class="domain"><xsl:value-of select="@name"/></h2>
        </a>
        <p class="package"><xsl:value-of select="@package"/></p>
        <xsl:apply-templates mode="annotate" />
        <p class="declaration">XSD type: <span class="xsdtype"><xsl:value-of select="@xstype"/></span></p>
        </div>
	</xsl:template>

	<xsl:template match="a:EnumeratedType">
		<!-- declares an enumerated type -->
		<div id="{@name}" class="group">
        <a href="#{@name}">
        <h2 class="enumerated"><xsl:value-of select="@name"/></h2>
        </a>
        <p class="package"><xsl:value-of select="@package"/></p>
        <xsl:apply-templates mode="annotate" />
        <table>
			<xsl:apply-templates />
		</table>
		</div>
	</xsl:template>

	<xsl:template match="a:EnumeratedValue">
		<!-- declares one value within an enumerated type -->
		<tr>
	        <th><p class="name"><xsl:value-of select="@name"/></p></th>
	        <td>  <xsl:apply-templates mode="annotate" /> </td>
        </tr>
	</xsl:template>

	<xsl:template match="a:Comment" mode="annotate">
		<!--  generate and annotation -->
		<p class="comment"><xsl:value-of select="." /></p>
	</xsl:template>

	<xsl:template match="a:Note" mode="annotate">
		<!--  generate and annotation -->
		<p class="note"><xsl:value-of select="." /></p>
	</xsl:template>

	<xsl:template match="node()">
		<!-- dont pass any defaults -->
	</xsl:template>

	<xsl:template match="node()" mode="annotate">
		<!-- dont pass any defaults in annotate mode -->
	</xsl:template>

</xsl:stylesheet>