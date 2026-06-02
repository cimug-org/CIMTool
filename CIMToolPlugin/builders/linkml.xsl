<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright 2024 UCAIug

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="3.0" 
	xmlns:xalan="http://xml.apache.org/xalan" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
	xmlns:a="http://langdale.com.au/2005/Message#" 
	xmlns:map="http://www.w3.org/2005/xpath-functions/map" 
	exclude-result-prefixes="a xalan" >

    <!-- Output the result as plain text -->
    <xsl:output indent="no" method="text" encoding="utf-8"/>
    <xsl:param name="copyright-single-line" />
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
    
	<!-- This is the template that serves as the entry point of the builder. In the context of generating
		 a LinkML .yaml document, it essentially generates the LinkML metadata such as the license, copyright, 
		 prefix definitions, imports, etc. and then delegates the generation of the class and enums to other
		 templates. -->
    <xsl:template match="a:Catalog">
    	<item>id: <xsl:value-of select="@baseURI" /></item>
    	<xsl:text>&#10;</xsl:text>
    	<item>name: <xsl:value-of select="@name" /></item>
    	<xsl:text>&#10;</xsl:text>
    	<item>title: <xsl:value-of select="@name" /> Vocabulary</item>
    	<xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<item><xsl:text>comments: </xsl:text>"<xsl:value-of select="$comments" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
    	<xsl:if test="$copyright-single-line != ''">
			<xsl:text>annotations:</xsl:text>
			<xsl:text>&#10;</xsl:text>
			<item><xsl:text>  copyright: </xsl:text>"<xsl:value-of select="$copyright-single-line" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
    	<!-- Prefixes top-level section -->	
    	<item>prefixes:</item>
    	<xsl:text>&#10;</xsl:text>
		<xsl:for-each select="map:keys($prefixes)">
            <item><xsl:text>  </xsl:text><xsl:value-of select="map:get($prefixes, .)"/>: <xsl:value-of select="."/></item>
			<xsl:text>&#10;</xsl:text>
        </xsl:for-each>
    	<item>imports:</item>
    	<xsl:text>&#10;</xsl:text>
    	<item>- linkml:types</item>
    	<xsl:text>&#10;</xsl:text>
    	<item>default_curi_maps:</item>
    	<xsl:text>&#10;</xsl:text>
    	<item>- semweb_context</item>
    	<xsl:text>&#10;</xsl:text>
    	<item>default_range: string</item>
    	<xsl:text>&#10;</xsl:text>
    	<item>default_prefix: this</item>
    	<xsl:text>&#10;</xsl:text>
    	
    	<!-- Classes LinkML section -->
    	<item>classes:</item>
    	<xsl:text>&#10;</xsl:text>
    	<xsl:apply-templates />

    	<!-- Types LinkML section -->
    	<item>types:</item>
    	<xsl:text>&#10;</xsl:text>
    	<xsl:apply-templates select="a:SimpleType" mode="types"/>
    	    	
    	<!-- Enumerations LinkML section -->
    	<item>enums:</item>
    	<xsl:text>&#10;</xsl:text>
    	<xsl:apply-templates select="a:EnumeratedType" mode="gen-enums"/>
	</xsl:template>

    <xsl:template match="a:Root|a:ComplexType|CompoundType ">
        <item><xsl:text>  </xsl:text><xsl:value-of select="@name" />:</item>
        <xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>    description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<item><xsl:text>    comments: </xsl:text>"<xsl:value-of select="$comments" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:text>    annotations:</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>      ea_guid: </xsl:text>"<xsl:value-of select="@ea_guid" />"</item>
		<xsl:text>&#10;</xsl:text>
		<xsl:if test="a:SuperType">
			<!-- The name of the superclass is determined using the @baseClass attribute (and not the a:SuperType/@name) -->
		    <item><xsl:text>    is_a: </xsl:text><xsl:value-of select="substring-after(a:SuperType/@baseClass, '#')" /></item>
		    <xsl:text>&#10;</xsl:text>
		</xsl:if>
        <xsl:if test="not(a:Stereotype[contains(., '#concrete')])">
			<xsl:text>    abstract: true</xsl:text>
			<xsl:text>&#10;</xsl:text>
        </xsl:if>
        <item><xsl:text>    class_uri: </xsl:text><xsl:call-template name="prefix"><xsl:with-param name="uri" select="@baseClass"/></xsl:call-template></item>
        <xsl:text>&#10;</xsl:text>
        <item><xsl:text>    attributes: </xsl:text></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:apply-templates/>
		<!-- This was commented out after discussions with Bart. Originally thought to be needed, but was
		     determined not to be the case . Leaving it in the XSLT due to the complex XPath query should 
		     it ever be needed in the future to determine the InverseReference (or InverseInstance) of the
		     association
        <xsl:variable name="class" select="substring-after(@baseClass, '#')" />
		<xsl:for-each select="//a:InverseReference[contains(@baseProperty, concat('#', $class, '.'))]|//a:InverseInstance[contains(@baseProperty, concat('#', $class, '.'))]">
			<xsl:call-template name="inverse"/>
        </xsl:for-each> -->
    </xsl:template>
    
    <xsl:template match="a:SimpleType" mode="types">
        <item><xsl:text>  </xsl:text><xsl:value-of select="@name" />:</item>
        <xsl:text>&#10;</xsl:text>
		<item><xsl:text>    uri: </xsl:text><xsl:value-of select="concat('xsd:', @xstype)" /></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:variable name="base_type">
			<xsl:call-template name="xsd_type_to_python_type">
				<xsl:with-param name="xstype" select="@xstype"/>
			</xsl:call-template>
		</xsl:variable>
		<!-- LinkML's base metadata attribute:  python base type that implements this type definition -->
       	<item><xsl:text>    base: </xsl:text><xsl:value-of select="$base_type"/></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>    description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<item><xsl:text>    comments: </xsl:text>"<xsl:value-of select="$comments" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:text>    annotations:</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>      ea_guid: </xsl:text>"<xsl:value-of select="@ea_guid" />"</item>
		<xsl:text>&#10;</xsl:text>
		<xsl:text>      cim_data_type: true</xsl:text>
		<xsl:text>&#10;</xsl:text>
        <item><xsl:text>      uri: </xsl:text><xsl:call-template name="prefix"><xsl:with-param name="uri" select="@dataType"/></xsl:call-template></item>
        <xsl:text>&#10;</xsl:text>
        <!-- <xsl:apply-templates/> -->
    </xsl:template>

    <xsl:template match="a:Simple">
        <item><xsl:text>      </xsl:text><xsl:value-of select="@name" />:</item>
        <xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>        description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<item><xsl:text>    comments: </xsl:text>"<xsl:value-of select="$comments" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:text>        annotations:</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>          ea_guid: </xsl:text>"<xsl:value-of select="@ea_guid" />"</item>
		<xsl:text>&#10;</xsl:text>
        <item><xsl:text>        slot_uri: </xsl:text><xsl:call-template name="prefix"><xsl:with-param name="uri" select="@baseProperty"/></xsl:call-template></item>
        <xsl:text>&#10;</xsl:text>
        <!-- Must map from an @xstype to a native linkml type which is what must be specified for the range of an attribute. -->
        <xsl:variable name="linkml_type">
			<xsl:call-template name="xsd_type_to_linkml_type">
				<xsl:with-param name="xstype" select="@xstype"/>
			</xsl:call-template>
		</xsl:variable>
       	<item><xsl:text>        range: </xsl:text><xsl:value-of select="$linkml_type"/></item>
        <xsl:text>&#10;</xsl:text>
        <item><xsl:text>        minimum_cardinality: </xsl:text><xsl:value-of select="@minOccurs" /></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:choose>
			<xsl:when test="not(@maxOccurs = 'unbounded')">
				<item><xsl:text>        maximum_cardinality: </xsl:text><xsl:value-of select="@maxOccurs" /></item>
				<xsl:text>&#10;</xsl:text>			
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>        multivalued: true</xsl:text>
				<xsl:text>&#10;</xsl:text>		
			</xsl:otherwise>					
		</xsl:choose>
    </xsl:template>
    
    <xsl:template match="a:Domain">
        <item><xsl:text>      </xsl:text><xsl:value-of select="@name" />:</item>
        <xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>        description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<xsl:text>    comments: </xsl:text>"<xsl:value-of select="$comments" />"
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:text>        annotations:</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>          ea_guid: </xsl:text>"<xsl:value-of select="@ea_guid" />"</item>
		<xsl:text>&#10;</xsl:text>
        <item><xsl:text>        slot_uri: </xsl:text><xsl:call-template name="prefix"><xsl:with-param name="uri" select="@baseProperty"/></xsl:call-template></item>
        <xsl:text>&#10;</xsl:text>
        <item><xsl:text>        range: </xsl:text><xsl:value-of select="substring-after(@dataType, '#')" /></item>
        <xsl:text>&#10;</xsl:text>
        <item><xsl:text>        minimum_cardinality: </xsl:text><xsl:value-of select="@minOccurs" /></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:choose>
			<xsl:when test="not(@maxOccurs = 'unbounded')">
				<item><xsl:text>        maximum_cardinality: </xsl:text><xsl:value-of select="@maxOccurs" /></item>
				<xsl:text>&#10;</xsl:text>			
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>        multivalued: true</xsl:text>
				<xsl:text>&#10;</xsl:text>		
			</xsl:otherwise>					
		</xsl:choose>
    </xsl:template>

    <xsl:template match="a:Reference|a:Instance">
        <item><xsl:text>      </xsl:text><xsl:value-of select="@name" />:</item>
        <xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>        description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<item><xsl:text>    comments: </xsl:text>"<xsl:value-of select="$comments" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:text>        annotations:</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>          ea_guid: </xsl:text>"<xsl:value-of select="@ea_guid" />"</item>
		<xsl:text>&#10;</xsl:text>
        <item><xsl:text>        slot_uri: </xsl:text><xsl:call-template name="prefix"><xsl:with-param name="uri" select="@baseProperty"/></xsl:call-template></item>
        <xsl:text>&#10;</xsl:text>
        <item><xsl:text>        range: </xsl:text><xsl:value-of select="@type" /></item>
        <xsl:text>&#10;</xsl:text>
        <item><xsl:text>        minimum_cardinality: </xsl:text><xsl:value-of select="@minOccurs" /></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:choose>
			<xsl:when test="not(@maxOccurs = 'unbounded')">
				<item><xsl:text>        maximum_cardinality: </xsl:text><xsl:value-of select="@maxOccurs" /></item>
				<xsl:text>&#10;</xsl:text>			
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>        multivalued: true</xsl:text>
				<xsl:text>&#10;</xsl:text>		
			</xsl:otherwise>					
		</xsl:choose>
    </xsl:template>

    <xsl:template match="a:EnumeratedType" mode="gen-enums">
        <item><xsl:text>  </xsl:text><xsl:value-of select="concat(@name, ':')" /></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>    description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<item><xsl:text>    comments: </xsl:text>"<xsl:value-of select="$comments" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:text>    annotations:</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>      ea_guid: </xsl:text>"<xsl:value-of select="@ea_guid" />"</item>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>    enum_uri:  </xsl:text><xsl:value-of select="@baseClass" /></item>
        <xsl:text>&#10;</xsl:text>
        <item><xsl:text>    permissible_values:</xsl:text></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:apply-templates select="a:EnumeratedValue" mode="gen-enums"/>
    </xsl:template>
	
	<xsl:template match="a:EnumeratedValue" mode="gen-enums">	
		<item><xsl:text>      </xsl:text><xsl:value-of select="concat(@name, ':')"/></item>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>        meaning:  </xsl:text><xsl:call-template name="prefix"><xsl:with-param name="uri" select="@baseResource"/></xsl:call-template></item>
		<xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>        description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<item><xsl:text>    comments: </xsl:text>"<xsl:value-of select="$comments" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:text>        annotations:</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>          ea_guid: </xsl:text>"<xsl:value-of select="@ea_guid" />"</item>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
	
	<!-- Template that converts an @xstype into a corresponding python type --> 
	<xsl:template name="xsd_type_to_python_type">
		<xsl:param name="xstype"/>
		<xsl:choose>
			<xsl:when test="$xstype = 'boolean'">Bool</xsl:when>
			<xsl:when test="$xstype = 'date'">XSDDate</xsl:when>
			<xsl:when test="$xstype = 'dateTime'">XSDDateTime</xsl:when>
			<xsl:when test="$xstype = 'decimal'">Decimal</xsl:when>
			<xsl:when test="$xstype = 'double'">float</xsl:when>
			<xsl:when test="$xstype = 'float'">float</xsl:when>
			<xsl:when test="$xstype = 'integer' or $xstype = 'int' or $xstype = 'short' or $xstype = 'long'">int</xsl:when>
			<xsl:when test="$xstype = 'NCName'">NCName</xsl:when>
			<xsl:when test="$xstype = 'string'">str</xsl:when>	
			<xsl:when test="$xstype = 'time'">XSDTime</xsl:when>		
			<xsl:when test="$xstype = 'anyURI'">URIorCURIE</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$xstype"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="xsd_type_to_linkml_type">
		<xsl:param name="xstype"/>
		<xsl:choose>
			<xsl:when test="$xstype = 'string'">string</xsl:when>
			<xsl:when test="$xstype = 'normalizedString'">string</xsl:when>
			<xsl:when test="$xstype = 'token'">string</xsl:when>
			<xsl:when test="$xstype = 'NMTOKEN'">string</xsl:when>
			<xsl:when test="$xstype = 'anyURI'">uri</xsl:when>
			<xsl:when test="$xstype = 'NCName'">string</xsl:when>
			<xsl:when test="$xstype = 'Name'">string</xsl:when>
			<xsl:when test="$xstype = 'integer' or $xstype = 'int' or $xstype = 'short' or $xstype = 'long'">integer</xsl:when>
			<xsl:when test="$xstype = 'double' or $xstype = 'number'">double</xsl:when>
			<xsl:when test="$xstype = 'float'">float</xsl:when>
			<xsl:when test="$xstype = 'decimal'">decimal</xsl:when>
			<xsl:when test="$xstype = 'boolean'">boolean</xsl:when>
			<xsl:when test="$xstype = 'dateTime'">datetime</xsl:when>
			<xsl:when test="$xstype = 'date'">date</xsl:when>
			<xsl:when test="$xstype = 'time'">time</xsl:when>
			<xsl:when test="$xstype = 'duration'">string</xsl:when>
			<xsl:when test="$xstype = 'gMonthDay'">string</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$xstype"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- This was commented out after discussions with Bart. Originally thought to be needed, but was
		 determined not to be the case . Leaving it in the XSLT due to the complex XPath query should 
		 it ever be needed in the future to determine the InverseReference (or InverseInstance) of the
		 association    
    <xsl:template name="inverse">
		<item><xsl:text>      </xsl:text><xsl:value-of select="@name" />:</item>
		<xsl:text>&#10;</xsl:text>
        <xsl:variable name="description"><xsl:call-template name="linkml-description"/></xsl:variable>
		<xsl:if test="$description != ''">
			<item><xsl:text>        description: </xsl:text>"<xsl:value-of select="$description" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:variable name="comments"><xsl:call-template name="linkml-comments"/></xsl:variable>
		<xsl:if test="$comments != ''">
			<item><xsl:text>    comments: </xsl:text>"<xsl:value-of select="$comments" />"</item>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<item><xsl:text>        slot_uri: </xsl:text><xsl:call-template name="prefix"><xsl:with-param name="uri" select="@baseProperty"/></xsl:call-template></item>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>        range: </xsl:text><xsl:value-of select="@type" /></item>
		<xsl:text>&#10;</xsl:text>
		<item><xsl:text>        minimum_cardinality: </xsl:text><xsl:value-of select="@minOccurs" /></item>
        <xsl:text>&#10;</xsl:text>
        <xsl:choose>
			<xsl:when test="not(@maxOccurs = 'unbounded')">
				<item><xsl:text>        maximum_cardinality: </xsl:text><xsl:value-of select="@maxOccurs" /></item>
				<xsl:text>&#10;</xsl:text>			
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>        multivalued: true</xsl:text>
				<xsl:text>&#10;</xsl:text>		
			</xsl:otherwise>					
		</xsl:choose>
    </xsl:template> -->
    
	<xsl:variable name="prefixes" as="map(xsd:string, xsd:string)">
		<xsl:map>
			<xsl:map-entry key="'https://w3id.org/linkml/'" select="'linkml'"/>
			<xsl:map-entry key="normalize-space(concat($ontologyURI, '#'))" select="'cim'"/>
			<xsl:map-entry key="'http://iec.ch/TC57/CIM100-European#'" select="'eu'"/>
			<xsl:map-entry key="'http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#'" select="'cims'"/>
			<xsl:map-entry key="'http://purl.org/dc/elements/1.1/'" select="'dc'"/>
			<xsl:map-entry key="'http://www.w3.org/ns/dcat#'" select="'dcat'"/>
			<xsl:map-entry key="'http://purl.org/dc/terms/'" select="'dct'"/>
			<xsl:map-entry key="'http://www.w3.org/2002/07/owl#'" select="'owl'"/>
			<xsl:map-entry key="'http://iec.ch/TC57/ns/CIM/prof-cim#'" select="'profcim'"/>
			<xsl:map-entry key="'http://www.w3.org/1999/02/22-rdf-syntax-ns#'" select="'rdf'"/>
			<xsl:map-entry key="'http://www.w3.org/2000/01/rdf-schema#'" select="'rdfs'"/>
			<xsl:map-entry key="'http://www.w3.org/ns/shacl#'" select="'sh'"/>
			<xsl:map-entry key="'http://www.w3.org/2004/02/skos/core#'" select="'skos'"/>
			<xsl:map-entry key="normalize-space($baseURI)" select="'this'"/>
		</xsl:map>
    </xsl:variable>
    
	<xsl:template name="prefix">
		<xsl:param name="uri"/>
		<xsl:value-of select="if (map:contains($prefixes, concat(substring-before($uri, '#'), '#'))) then concat(map:get($prefixes, concat(substring-before($uri, '#'), '#')), ':', substring-after($uri, '#')) else $uri" />	
	</xsl:template>
	
	<!-- ========================================================================================== -->
	<!-- These final templates are for processing documentation. They filter out unwanted characters -->
	<!-- and are intended to be invoked from other templates at the point where class or attribute  -->
	<!-- descriptions are to be placed.                                                             -->
	<!-- ========================================================================================== -->
	
	<!-- The a:Note element in a CIMTool intermediary XML profile definition file corresponds    
		 to the "Profile Description" text entry field in CIMTool (on the description tab). This 
		 allows an end-user to enter profile specific notes or instructions relevant to a class, 
		 enum, attribute, etc. used in a profile exchange. We are mapping these additional notes  
		 that appear in the a:Note element to LinkML's 'comments' metadata attribute. Per the 
		 LinkML documentation the 'comments' metadata attribute is equivalent to skos:note and 
		 defined as:

			'notes and comments about an element intended primarily for external consumption'

		 Here the external consumer is the end-user of the profile that may need additional notes
		 or context on how the class, attribute, or other element is be used in the profile. This 
		 would translate to additional detail beyond what is generically provided in the UML for
		 that element.

		 See:  https://linkml.io/linkml-model/latest/docs/comments/ -->
	<xsl:template name="linkml-comments">
		<xsl:variable name="notes" select="a:Note"/>
		<!-- generate human readable annotation -->
		<list begin="" indent="" delim=" " end="">
			<xsl:for-each select="$notes">
				<!-- Remove double quotes to eliminate broken comments, etc. -->
				<item>
					<xsl:value-of select="translate(., '&quot;', '')"/>
				</item>
			</xsl:for-each>
		</list>
	</xsl:template>
	
	<!-- The a:Comment element in a CIMTool intermediary XML profile definition file corresponds  
		 to the description of the class, attribute, enum, etc. as it appears directly in the CIM   
		 UML/schema. As a result we map the a:Comment element to LinkML's 'description' metadata 
		 attribute. Per the LinkML documentation the 'description' metadata attribute is equivalent 
		 to skos:definition and is defined as:

			'a textual description of the element's purpose and use'

		 See:  https://linkml.io/linkml-model/latest/docs/description/ -->
	<xsl:template name="linkml-description">
		<xsl:variable name="notes" select="a:Comment"/>
		<!-- generate human readable annotation -->
		<list begin="" indent="" delim=" " end="">
			<xsl:for-each select="$notes">
				<!-- Remove double quotes to eliminate broken comments, etc. -->
				<item>
					<xsl:value-of select="translate(., '&quot;', '')"/>
				</item>
			</xsl:for-each>
		</list>
	</xsl:template>
	
	<xsl:template match="text()">
		<!--  dont pass text through  -->
	</xsl:template>

</xsl:stylesheet>