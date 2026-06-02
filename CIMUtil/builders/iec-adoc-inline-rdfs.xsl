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
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:map="http://www.w3.org/2005/xpath-functions/map" 
	xmlns:xs="http://www.w3.org/2001/XMLSchema" 
	xmlns:a="http://langdale.com.au/2005/Message#" 
	xmlns="http://langdale.com.au/2009/Indent">
	
	<xsl:output indent="no" method="xml" encoding="UTF-8" omit-xml-declaration="yes"/>
	<xsl:preserve-space elements="a:AsciiDoc"/>
	<xsl:param name="fileName"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<!-- All of the following params correspond to PlantUML preferences in the CIMTool preferences -->
	<!-- screen. They are passed in via a single string parameter $builderParameters that consist   -->
	<!-- of parameter name/value pairs that are delimited using the pipe character ("|"). This       -->
	<!-- allows for more dynamically adding parameters within the core CIMTool codebase.  -->
    <xsl:param name="builderParameters"/>

    <!-- Tokenize by '|' to get name=value parameter pairs -->
    <xsl:variable name="pairs" select="tokenize($builderParameters, '\|')"/>

    <!-- Extracting all individual values -->
    <xsl:param name="abstractClassesColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'abstractClassesColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="abstractClassesFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'abstractClassesFontColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="cimDatatypesColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'cimDatatypesColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="cimDatatypesFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'cimDatatypesFontColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="compoundsColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'compoundsColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="compoundsFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'compoundsFontColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="concreteClassesColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'concreteClassesColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="concreteClassesFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'concreteClassesFontColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="docRootClassesColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'docRootClassesColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="docRootClassesFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'docRootClassesFontColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="enableDarkMode" as="xs:boolean" select="xs:boolean(substring-after((for $pair in $pairs return if (starts-with($pair, 'enableDarkMode=')) then $pair else ())[1], '='))"/>
    <xsl:param name="enableShadowing" as="xs:boolean" select="xs:boolean(substring-after((for $pair in $pairs return if (starts-with($pair, 'enableShadowing=')) then $pair else ())[1], '='))"/>
    <xsl:param name="enumerationsColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'enumerationsColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="enumerationsFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'enumerationsFontColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="errorsColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'errorsColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="errorsFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'errorsFontColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="hideCardinalityForRequiredAttributes" as="xs:boolean" select="xs:boolean(substring-after((for $pair in $pairs return if (starts-with($pair, 'hideCardinalityForRequiredAttributes=')) then $pair else ())[1], '='))"/>
    <xsl:param name="hideCIMDatatypes" as="xs:boolean" select="xs:boolean(substring-after((for $pair in $pairs return if (starts-with($pair, 'hideCIMDatatypes=')) then $pair else ())[1], '='))"/>
    <xsl:param name="hideCompounds" as="xs:boolean" select="xs:boolean(substring-after((for $pair in $pairs return if (starts-with($pair, 'hideCompounds=')) then $pair else ())[1], '='))"/>
    <xsl:param name="hideEnumerations" as="xs:boolean" select="xs:boolean(substring-after((for $pair in $pairs return if (starts-with($pair, 'hideEnumerations=')) then $pair else ())[1], '='))"/>
    <xsl:param name="hidePrimitives" as="xs:boolean" select="xs:boolean(substring-after((for $pair in $pairs return if (starts-with($pair, 'hidePrimitives=')) then $pair else ())[1], '='))"/>
    <xsl:param name="plantUMLTheme" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'plantUMLTheme=')) then $pair else ())[1], '=')"/>
    <xsl:param name="primitivesColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'primitivesColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="primitivesFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'primitivesFontColor=')) then $pair else ())[1], '=')"/> 
    <xsl:param name="horizontalSpacing" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'horizontalSpacing=')) then $pair else ())[1], '=')"/>
    <xsl:param name="verticalSpacing" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'verticalSpacing=')) then $pair else ())[1], '=')"/> 
        
	<xsl:variable name="apos">'</xsl:variable>
	<xsl:variable name="asciidoc-restricted" as="map(xs:string, xs:string)">
		<xsl:map>
			<xsl:map-entry key="'|'" select="'vbar'"/>
			<xsl:map-entry key="'['" select="'startsb'"/> 
			<xsl:map-entry key="']'" select="'endsb'"/>
			<xsl:map-entry key="'^'" select="'caret'"/>
			<xsl:map-entry key="'*'" select="'asterisk'"/>
			<xsl:map-entry key="'&amp;'" select="'amp'"/>
			<xsl:map-entry key="'`'" select="'backtick'"/>
			<xsl:map-entry key="'‘'" select="'lsquo'"/>
			<xsl:map-entry key="'’'" select="'rsquo'"/>
			<xsl:map-entry key="'“'" select="'ldquo'"/>
			<xsl:map-entry key="'”'" select="'rdquo'"/>
			<xsl:map-entry key="'°'" select="'deg'"/>
			<xsl:map-entry key="'¦'" select="'brvbar'"/>
			<xsl:map-entry key="'&lt;'" select="'lt'"/>
			<xsl:map-entry key="'>'" select="'gt'"/>
			<xsl:map-entry key="'~'" select="'tilde'"/> 
			<xsl:map-entry key="'\'" select="'backslash'"/>
		</xsl:map>
    </xsl:variable>
    
    <xsl:variable name="asciidoc-table-sensitive" as="map(xs:string, xs:string)">
		<xsl:map>
			<xsl:map-entry key="'|'" select="'vbar'"/>
		</xsl:map>
    </xsl:variable>
    
	<xsl:template match="a:Catalog">
		<document>
			<list begin="" indent="" delim="" end="">
				<item></item>
				<xsl:choose>
					<xsl:when test="$envelope != 'Profile'">
						<item>=== <xsl:value-of select="$envelope"/> Profile Specification</item>
						<item></item>
					</xsl:when>
					<xsl:otherwise>
						<item>=== <xsl:value-of select="$fileName"/> Profile Specification</item>
						<item></item>
					</xsl:otherwise>
				</xsl:choose>
				<item>// Settings:</item>
				<item>:doctype: inline</item>
				<item>:reproducible:</item>
				<item>:icons: font</item>
				<item>:sectnums:</item>
				<item>:sectnumlevels: 4</item>
				<item>:xrefstyle: short</item>
				<item></item>
				<item>Profile namespace:  <xsl:value-of select="$baseURI"/></item>
				<item></item>
				<!-- Note that AsciiDoc auto-generates the period below into a prefix of:  "Figure N." -->
				<!-- Allowing for auto-number of the figures throughout a document                     -->
				<item>.<xsl:value-of select="$envelope"/> Profile</item>
				<item>[plantuml]</item>
				<item>....</item>
				<xsl:call-template name="generate-puml"/>
				<item>....</item>
				<item></item>
				<xsl:apply-templates mode="annotate-type"/>
				<xsl:if test="(count(/.//a:Root) + count(/.//a:Message)) > 0">
					<item></item>
					<item>==== Concrete Classes</item>
					<item></item>
					<xsl:apply-templates select="a:Root|a:Message">
					<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:ComplexType) > 0">
					<item></item>
					<item>==== Abstract Classes</item>
					<item></item>
					<xsl:apply-templates select="a:ComplexType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:CompoundType) > 0">
					<item></item>
					<item>==== Compound Types </item>
					<item></item>
					<xsl:apply-templates select="a:CompoundType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:EnumeratedType) > 0">
					<item></item>
					<item>==== Enumerations</item>
					<item></item>
					<xsl:apply-templates select="a:EnumeratedType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:SimpleType) > 0">
					<item></item>
					<item>==== Datatypes</item>
					<item></item>
					<xsl:apply-templates select="a:SimpleType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:PrimitiveType) > 0">
					<item></item>
					<item>==== Primitive Types</item>
					<item></item>
					<xsl:apply-templates select="a:PrimitiveType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<item></item>
			</list>
		</document>
	</xsl:template>

	<xsl:template match="a:Message">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>===== <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<xsl:apply-templates/>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:Root">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>===== <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		<item></item>
		<!-- 
			This conditional is intended only for RDFS related adoc and for classes tagged 
			with the 'Description' stereotype (i.e. rdf:about) 
		-->
		<xsl:if test="a:Stereotype[contains(., '#description')]">
			<item>ifdef::<xsl:value-of select="$fileName"/>-description-profile[]</item>
			<item>This class is tagged in this profile with the 'Description' tag. To refer to the full definition of this class as defined in the profile this one depends on visit &lt;&lt;{<xsl:value-of select="$fileName"/>-description-profile}-<xsl:value-of select="@name"/>,<xsl:value-of select="@name"/>&gt;&gt;.</item>
			<item>endif::<xsl:value-of select="$fileName"/>-description-profile[]</item>
			<item></item>
		</xsl:if>
		<item>:<xsl:value-of select="@name"/>:</item>
		<xsl:call-template name="type_definition"/>
		<item>:!<xsl:value-of select="@name"/>:</item>
		<item></item>
	</xsl:template>
	
	<xsl:template name="type_definition">
		<xsl:apply-templates mode="annotate-type"/>
		<xsl:choose>
			<xsl:when test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:SuperType">
				<xsl:if test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound">
					<item></item>
					<item>*Native Members*</item>
					<item></item>
					<item>[%header,width="100%",cols="25%,^10%,20%,45%a"]</item>
					<item>|===</item>
					<item>|name |mult |type |description</item>
					<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound"/>
					<item>|===</item>
				</xsl:if>
				<xsl:if test="a:SuperType">
					<item></item>
					<item>*Inherited Members*</item>
					<item></item>
					<item>[%header,width="100%",cols="25%,^10%,20%,45%a"]</item>
					<item>|===</item>
					<item>|name |mult |type |description</item>
					<xsl:apply-templates select="a:SuperType" mode="inherited"/>
					<item>|===</item>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="baseClass" select="@baseClass"/>
				<xsl:if test="not(child::a:Stereotype[contains(., '#concrete')]) and (count(/.//a:Reference[@baseClass=$baseClass]) > 0)">
					<!-- Adoc conditional for when there is a byref profile dependency attribute set -->
					<item>ifdef::<xsl:value-of select="$fileName"/>-byref-profile[]</item>
					<item>This abstract class serves as a 'By Reference' association within this profile and as such has no attributes or associations defined. Rather, it is used to reference a corresponding concrete class defined in an external profile that this one depends upon. To reference this definition in that profile visit &lt;&lt;{<xsl:value-of select="$fileName"/>-byref-profile}-<xsl:value-of select="@name"/>,<xsl:value-of select="@name"/>&gt;&gt;.</item>
					<item></item>
					<item>endif::<xsl:value-of select="$fileName"/>-byref-profile[]</item>	
					<!-- Adoc conditional for when there is no byref profile dependency attribute set -->
					<item>ifndef::<xsl:value-of select="$fileName"/>-byref-profile[]</item>
					<item>This abstract class serves as a 'By Reference' association within this profile and as such has no attributes or associations defined. Rather, it is used to reference a corresponding concrete class defined in an external profile that this one depends upon.</item>
					<item></item>
					<item>endif::<xsl:value-of select="$fileName"/>-byref-profile[]</item>					
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="a:SuperType" mode="inheritance_hierarchy">
		<xsl:variable name="inheritance">
			&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>,<xsl:value-of select="@name"/>&gt;&gt;<xsl:variable name="supertype_name" select="@name"/><xsl:if test="/*/node()[@name = $supertype_name]/a:SuperType"> => <xsl:apply-templates select="/*/node()[@name = $supertype_name]/a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		</xsl:variable>
		<xsl:value-of select="$inheritance"/>
	</xsl:template>
	
	<xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Compound|a:Domain">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]#</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">#</xsl:if><xsl:text> </xsl:text><xsl:call-template name="process-attribute-stereotypes"/></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell"/>
	</xsl:template>

	<xsl:template match="a:Simple">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]#</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">#</xsl:if><xsl:text> </xsl:text><xsl:call-template name="process-attribute-stereotypes"/></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>,<xsl:value-of select="substring-after(@cimDatatype, '#')"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell"/>
	</xsl:template>

	<xsl:template match="a:SuperType" mode="inherited">
		<xsl:apply-templates select="//a:ComplexType[@name=current()/@name]" mode="inherited"/>
		<xsl:apply-templates select="//a:Root[@name=current()/@name]" mode="inherited"/>
	</xsl:template>
	
	<xsl:template match="a:ComplexType|a:Root" mode="inherited">
		<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound" mode="inherited"/>
		<xsl:apply-templates select="a:SuperType" mode="inherited"/>
	</xsl:template>
	
	<xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Compound|a:Domain" mode="inherited">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]#</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">#</xsl:if><xsl:text> </xsl:text><xsl:call-template name="process-attribute-stereotypes"/></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item>|see &lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>,<xsl:value-of select="../@name"/>&gt;&gt;</item>
	</xsl:template>
	
	<xsl:template match="a:Simple" mode="inherited">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]#</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">#</xsl:if><xsl:text> </xsl:text><xsl:call-template name="process-attribute-stereotypes"/></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>,<xsl:value-of select="substring-after(@cimDatatype, '#')"/>&gt;&gt;</item>
		<item>|see &lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>,<xsl:value-of select="../@name"/>&gt;&gt;</item>
	</xsl:template>
	
	<xsl:template match="a:ComplexType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>===== <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		<item></item>
		<!-- 
			This conditional is intended only for RDFS related adoc and for classes tagged 
			with the 'Description' stereotype (i.e. rdf:about) 
		-->
		<xsl:if test="a:Stereotype[contains(., '#description')]">
			<item>ifdef::<xsl:value-of select="$fileName"/>-description-profile[]</item>
			<item>This class is tagged in this profile with the 'Description' tag. To refer to the full definition of this class as defined in the profile this one depends on visit &lt;&lt;{<xsl:value-of select="$fileName"/>-description-profile}-<xsl:value-of select="@name"/>,<xsl:value-of select="@name"/>&gt;&gt;.</item>
			<item>endif::<xsl:value-of select="$fileName"/>-description-profile[]</item>
			<item></item>
		</xsl:if>
		<item>:<xsl:value-of select="@name"/>:</item>
		<xsl:call-template name="type_definition"/>
		<item>:!<xsl:value-of select="@name"/>:</item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:CompoundType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>===== <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item>===== Members</item>
		<item></item>
		<item>[%header,width="100%",cols="25%,^10%,20%,45%a"]</item>
		<item>|===</item>
		<item>|name |mult |type |description</item>		
		<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound"/>
		<item>|===</item>	
	</xsl:template>
	
	<xsl:template match="a:SimpleType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>===== <xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item></item>
		<item>XSD type: <xsl:value-of select="@xstype"/></item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:PrimitiveType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>===== <xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item></item>
		<item>XSD type: <xsl:value-of select="@xstype"/></item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>===== <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item></item>
		<item>[%header,width="100%",cols="25%,75%a"]</item>
		<item>|===</item>
		<item>|name |description</item>
		<xsl:apply-templates/>
		<item>|===</item>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedValue">
		<item></item>
		<item>|<xsl:value-of select="@name"/></item>
		<item>|</item><xsl:apply-templates select="a:Comment|a:Note" mode="annotate-table-cell"/>
	</xsl:template>

	<xsl:template name="process-class-stereotypes">
		<xsl:if test="count(a:Stereotype[
							not(contains(., '#concrete')) and 
							not(contains(., '#byreference')) and 
							not(contains(., '#enumeration')) and 
							not(contains(., '#compound')) and 
							not(contains(., '#cimdatatype')) and 
							not(contains(., '#primitive')) and 
							not(contains(., '#compositeOf')) and 
							not(contains(., '#ofComposite')) and 
							not(contains(., '#aggregateOf')) and 
							not(contains(., '#ofAggregate')) and 
							not(contains(., '#hideondiagrams')) and 
							not(contains(., '#shadowextension'))]) > 0"> (<xsl:for-each select="a:Stereotype[
																								not(contains(., '#concrete')) and 
																								not(contains(., '#byreference')) and 
																								not(contains(., '#enumeration')) and 
																								not(contains(., '#compound')) and 
																								not(contains(., '#cimdatatype')) and 
																								not(contains(., '#primitive')) and 
																								not(contains(., '#compositeOf')) and 
																								not(contains(., '#ofComposite')) and 
																								not(contains(., '#aggregateOf')) and 
																								not(contains(., '#ofAggregate')) and 
																								not(contains(., '#hideondiagrams')) and 
																								not(contains(., '#shadowextension'))]">
				<xsl:value-of select="@label"/>
				<xsl:if test="position()!=last()">
					<xsl:value-of select="', '"/>
				</xsl:if>
			</xsl:for-each>)<xsl:text> </xsl:text></xsl:if>
	</xsl:template>
	
	<xsl:template name="process-attribute-stereotypes">
		<xsl:if test="count(a:Stereotype[not(contains(., '#attribute')) and not(contains(., '#byreference')) and not(contains(., '#enumeration')) and not(contains(., 'compound')) and not(contains(., 'cimdatatype'))]) > 0"> (<xsl:for-each select="a:Stereotype[not(contains(., '#attribute')) and not(contains(., '#byreference')) and not(contains(., '#enumeration')) and not(contains(., 'compound')) and not(contains(., 'cimdatatype'))]">
				<xsl:value-of select="@label"/>
				<xsl:if test="position()!=last()">
					<xsl:value-of select="', '"/>
				</xsl:if>
			</xsl:for-each>)</xsl:if>
	</xsl:template>
	
	<xsl:template name="attribute-stereotype-roles">
		<xsl:if test="count(a:Stereotype[not(contains(., '#attribute')) and not(contains(., '#byreference')) and not(contains(., '#enumeration')) and not(contains(., 'compound')) and not(contains(., 'cimdatatype'))]) > 0"><xsl:for-each select="a:Stereotype[not(contains(., '#attribute')) and not(contains(., '#byreference')) and not(contains(., '#enumeration')) and not(contains(., 'compound')) and not(contains(., 'cimdatatype'))]">
				<xsl:value-of select="concat('.', @label)"/>
			</xsl:for-each></xsl:if>
	</xsl:template>
	
	<!-- Template for a:Comment and a:Note elements. The a:Comment elements will always     -->
	<!-- contain documentation directly defined on a Class or Enumeration in the UML, while -->
	<!-- a:Note elements will contain additional documentation defined by end-users in the  -->
	<!-- "Profile Description" field on the "Description" tab in CIMTool. Such text could   -->
	<!-- potentially have asciidoc sensitive characters that we want to parse and replace.  -->
	<!-- Thus we use the $asciidoc-restricted map and not the $asciidoc-table-sensitive.    -->
	<xsl:template match="a:Comment|a:Note" mode="annotate-type">
		<item><xsl:call-template name="replace"><xsl:with-param name="text" select="."/><xsl:with-param name="map" select="$asciidoc-restricted"/></xsl:call-template></item>
		<!-- Below accounts for extra line spacing between paragraphs - DO NOT REMOVE -->
		<item></item>
	</xsl:template>
	
	<!-- Template for a:AsciiDoc elements. These elements contain documentation defined    -->
	<!-- by end-users in the documentation field on the "Documentation" tab in CIMTool.    -->
	<!-- Such text is expected to contain any type of AsciiDoc formatting that one would   -->
	<!-- To ensure no extra CR/LF is added since AsciiDoc internally processes will handle -->
	<!-- this automatically. If we were to include an empty item element as in the above   -->
	<!-- template it would processes as extra CR/LF.                                       -->
	<xsl:template match="a:AsciiDoc" mode="annotate-type">
		<item><xsl:value-of select="."/></item>
	</xsl:template>
	
	<!-- Specialized template for comments or notes that appear within a table cell. Such -->
	<!-- text within the CIM model may have asciidoc sensitive characters that need to be -->
	<!-- parsed and replaced. For this specific type of "mapping document" builder we are -->
	<!-- putting the a:Comment and a:Note elements into the "description" column of the   -->
	<!-- table. This documentation is sourced from both the UML and the end-user.         -->
	<xsl:template match="a:Comment|a:Note" mode="annotate-table-cell">
		<item><xsl:call-template name="replace"><xsl:with-param name="text" select="."/><xsl:with-param name="map" select="$asciidoc-table-sensitive"/></xsl:call-template></item>
		<!-- Below accounts for extra line spacing between paragraphs - DO NOT REMOVE -->
		<item></item>
	</xsl:template>
	
	<!-- We required a separate template for AsciiDoc when in "annotate-table-cell" mode   -->
	<!-- to ensure no extra CR/LF is added since AsciiDoc internally processes will handle -->
	<!-- this automatically. If we were to include an empty item element as in the above   -->
	<!-- template it would processes as extra CR/LF.                                       -->
	<xsl:template match="a:AsciiDoc" mode="annotate-table-cell">
		<item><xsl:call-template name="replace"><xsl:with-param name="text" select="."/><xsl:with-param name="map" select="$asciidoc-table-sensitive"/></xsl:call-template></item>
	</xsl:template>

	<xsl:template name="replace">
		<xsl:param name="text"/>
		<xsl:param name="map" as="map(xs:string, xs:string)"/>
		<xsl:variable name="final" as="xs:string">
			<xsl:iterate select="string-to-codepoints($text)">
				<xsl:param name="result" select="''"/>
				<xsl:on-completion>
					<xsl:value-of select="$result"/>
				</xsl:on-completion>   
				<xsl:variable name="char" select="codepoints-to-string(.)"/>
				<xsl:next-iteration>
					<xsl:with-param name="result" select="concat($result, if (map:contains($map, $char)) then concat('{', map:get($map, $char), '}') else $char)"/>
				</xsl:next-iteration>
			</xsl:iterate>
		</xsl:variable>
		<xsl:sequence select="$final"/>
	</xsl:template>
	
	<xsl:template match="node()" mode="annotate-type">
	</xsl:template>
	
	<xsl:template match="node()" mode="annotate-table-cell">
	</xsl:template>

	<xsl:template match="node()">
	</xsl:template>
	
	<xsl:template match="text()">
		<!--  dont pass text through  -->
	</xsl:template>
	
	<!--  ===================================================================================== -->
	<!--  All templates that are specific to generation of PlantUML diagrams are here...        -->
	<!--  ===================================================================================== -->	
	<xsl:template name="generate-puml" match="a:Catalog">
		<document>
			<list begin="@startuml" indent="" delim="" end="@enduml">
				<item>top to bottom direction</item>
				<item>hide empty methods</item>
				<item></item>
				<xsl:if test="$horizontalSpacing and $horizontalSpacing != ''">
					<item>' Here we adjust the horizontal spacing for better spatial rendering (the PlantUML default is ~20)</item>
					<item>skinparam nodesep <xsl:value-of select="$horizontalSpacing"/></item>
				</xsl:if>
				<xsl:if test="$verticalSpacing and $verticalSpacing != ''">
					<item>' Here we adjust the vertical spacing for better spatial rendering (the PlantUML default is ~30)</item>
					<item>skinparam ranksep <xsl:value-of select="$verticalSpacing"/></item>
				</xsl:if>
				<item></item>
				<xsl:if test="$plantUMLTheme and not($plantUMLTheme = '') and not($plantUMLTheme = '_none_')">
					<item><xsl:value-of select="concat('!theme ', $plantUMLTheme)"/></item>
				</xsl:if>
				<xsl:if test="$plantUMLTheme and $plantUMLTheme = '_none_'">
					<xsl:choose>
						<xsl:when test="$enableDarkMode">
							<item>skinparam BackgroundColor #2e2e2e</item>
							<item>skinparam ArrowColor #FFFFFF</item>
							<item>skinparam ArrowFontColor #FFFFFF</item>
							<item>skinparam ArrowThickness 1</item>
							<list begin="{concat('skinparam class ', '&#123;')}" indent="  " delim="" end="{'&#125;'}">
								<!-- Concrete classes definition -->
								<item>' Concrete classes style definition</item>
								<item>BackgroundColor #3e3e3e</item>
								<item>BorderColor #6a6a6a</item>
								<item>FontColor #FFFFFF</item>
								<item>AttributeFontColor #FFFFFF</item>
								<item>StereotypeFontColor #FFFFFF</item>
								<item>HeaderFontColor  #FFFFFF</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- Abstract classes <<abstract>> definition -->
								<item>' Abstract classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> #3e3e3e</item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> #6a6a6a</item>
								<item>FontColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> #FFFFFF</item>
								<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> #FFFFFF</item>
								<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> #FFFFFF</item>
								<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> #FFFFFF</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- Enumerations <<enumeration>> definition -->
								<item>' Enumerations style definition</item>
								<item>BackgroundColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> #3e3e3e</item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> #6a6a6a</item>
								<item>FontColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> #FFFFFF</item>
								<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> #FFFFFF</item>
								<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> #FFFFFF</item>
								<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> #FFFFFF</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- CIMDatatype classes <<CIMDatatype>> definition -->
								<item>' CIMDatatypes style definition</item>
								<item>BackgroundColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> #3e3e3e</item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> #6a6a6a</item>
								<item>FontColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> #FFFFFF</item>
								<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> #FFFFFF</item>
								<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> #FFFFFF</item>
								<item>HeaderFontColor <xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> #FFFFFF</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->						
								<!-- Compound classes <<Compound>> definition -->
								<item>' Compound classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> #3e3e3e</item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> #6a6a6a</item>
								<item>FontColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> #FFFFFF</item>
								<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> #FFFFFF</item>
								<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> #FFFFFF</item>
								<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> #FFFFFF</item>	
								<item>&#xD;&#xA;</item> <!-- CR/LF -->	
								<!-- Primitive classes <<Primitive>> definition -->
								<item>' Primitive classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> #3e3e3e</item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> #6a6a6a</item>
								<item>FontColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> #FFFFFF</item>
								<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> #FFFFFF</item>
								<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> #FFFFFF</item>
								<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> #FFFFFF</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->		
								<!-- Errors classes <<error>> definition -->
								<item>' Errors classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsColor)"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
							</list>
						</xsl:when>
						<xsl:otherwise>
							<item>skinparam BackgroundColor #FFFFFF</item>
							<item>skinparam shadowing false</item>
							<item>skinparam RoundCorner 5</item>
							<item>skinparam BorderColor #454645</item>
							<item>skinparam ArrowColor #454645</item>
							<item>skinparam FontColor #000000</item>
							<item></item>
							<list begin="{concat('skinparam class ', '&#123;')}" indent="  " delim="" end="{'&#125;'}">
								<!-- Concrete classes definition -->
								<item>' Concrete classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat(' ', $concreteClassesColor)"/></item>
								<item>FontColor<xsl:value-of select="concat(' ', $concreteClassesFontColor)"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat(' ', $concreteClassesFontColor)"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat(' ', $concreteClassesFontColor)"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat(' ', $concreteClassesFontColor)"/></item>
								<item>BorderColor #454645</item>
								<item>BorderThickness 1</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- Abstract classes <<abstract>> definition -->
								<item>' Abstract classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesColor)"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesFontColor)"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesFontColor)"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesFontColor)"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesFontColor)"/></item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> #454645</item>
								<item>BorderThickness<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> 1</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- Enumerations <<enumeration>> definition -->
								<item>' Enumerations style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;enumeration&gt;&gt; ', $enumerationsColor)"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;enumeration&gt;&gt; ', $enumerationsFontColor)"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;enumeration&gt;&gt; ', $enumerationsFontColor)"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;enumeration&gt;&gt; ', $enumerationsFontColor)"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat(' &lt;&lt;enumeration&gt;&gt; ', $enumerationsFontColor)"/></item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> #454645</item>
								<item>BorderThickness<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> 1</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- CIMDatatype classes <<CIMDatatype>> definition -->
								<item>' CIMDatatypes style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesColor)"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesFontColor)"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesFontColor)"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesFontColor)"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesFontColor)"/></item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> #454645</item>
								<item>BorderThickness<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> 1</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->				
								<!-- Compound classes <<Compound>> definition -->
								<item>' Compound classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsColor)"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsFontColor)"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsFontColor)"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsFontColor)"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsFontColor)"/></item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> #454645</item>
								<item>BorderThickness<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> 1</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- Primitive classes <<Primitive>> definition -->
								<item>' Primitive classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesColor)"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesFontColor)"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesFontColor)"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesFontColor)"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesFontColor)"/></item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> #454645</item>
								<item>BorderThickness<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> 1</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->		
								<!-- Errors classes <<error>> definition -->
								<item>' Errors classes style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsColor)"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;error&gt;&gt; '"/> #454645</item>
								<item>BorderThickness<xsl:value-of select="'&lt;&lt;error&gt;&gt; '"/> 1</item>
							</list>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
				<item>skinparam shadowing <xsl:value-of select="if ($enableShadowing) then 'true' else 'false'"/></item>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<item><xsl:value-of select="'hide &lt;&lt;abstract&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;error&gt;&gt; stereotype'"/></item>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<xsl:if test="not(@hideInDiagrams = 'true')">
					<!-- 
						We filter out characters in a class or note name not allowed by PlantUML. This was needed
						after discovering scenarios where end-users had profile names (i.e. $envelope) with dashes 
						(-) in them leading to have to parse out such character.
					-->
					<xsl:variable name="notename" select="concat(replace($envelope, '[\s:.,\[\]{}\(\)&lt;&gt;\-\+#\*=]', '_'), 'Note')" />
					<item></item>
					<list begin="{concat('skinparam note ', '&#123;')}" indent="  " delim="" end="{'&#125;'}">
						<item>BorderColor #454645</item>
						<item>BorderThickness 1.5</item>
						<item>FontSize 14</item>
						<item>Font Bold</item>
						<item>FontColor #000000</item>
					</list>
					<item>' Add a note towards the upper left corner of the diagram</item>
					<list begin="note as {$notename} #lightyellow" indent="   " delim="" end="end note">
						<item>Profile: <xsl:value-of select="$envelope"/></item>
						<item>Namespace: <xsl:value-of select="$baseURI"/></item>
						<xsl:if test="a:Note and a:Note[string-length(.) > 0]">
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<item>Profile Notes:</item>
							<xsl:apply-templates select="a:Note" mode="puml-profile-notes"/>
						</xsl:if>
					</list>
					<item>&#xD;&#xA;</item> <!-- CR/LF -->
				</xsl:if>
				<xsl:apply-templates select="a:Root|a:ComplexType|a:EnumeratedType|a:CompoundType|a:SimpleType|a:PrimitiveType" mode="puml"/>			
			</list>
		</document>
	</xsl:template>

	<xsl:template match="a:EnumeratedType" mode="puml">
		<xsl:if test="not($hideEnumerations) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="enumName" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="count" select="count(a:EnumeratedValue)"/>
			<list begin="" indent="" delim="" end="">
				<item>' Enumeration <xsl:value-of select="$enumName"/></item>
				<list begin="enum {concat($enumName, ' ', $stereotypes, ' ', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:for-each select="a:EnumeratedValue[position() &lt;= 20]">
						<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
						<item><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="substring-after(substring-after(@baseResource, '#'), '.')" /></item>
					</xsl:for-each>
					<xsl:if test="$count > 20">
						<item>[Remaining <xsl:value-of select="$count - 15"/> literals hidden]</item>
					</xsl:if>
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:SimpleType" mode="puml">
		<xsl:if test="not($hideCIMDatatypes) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@dataType, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="$className"/></item>
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:apply-templates select="a:Value|a:Unit|a:Multiplier" mode="puml"/>
						</xsl:when>
						<xsl:otherwise>
							<item>[Attributes hidden]</item>
						</xsl:otherwise>
					</xsl:choose>							
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:PrimitiveType" mode="puml">
		<xsl:if test="not($hidePrimitives) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@dataType, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="$className"/></item>
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">		
				</list>
			</list>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:CompoundType" mode="puml">
		<xsl:if test="not($hideCompounds) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="$className"/></item>
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain" mode="puml"/>
						</xsl:when>
						<xsl:otherwise>
							<item>[Attributes hidden]</item>
						</xsl:otherwise>
					</xsl:choose>							
				</list>
				<!-- Now process all associations: -->
				<xsl:choose>
					<xsl:when test="a:Reference|a:Instance">
						<xsl:apply-templates select="a:Reference|a:Instance" mode="puml"/>
						<item>&#xD;&#xA;</item> <!-- CR/LF -->
					</xsl:when>
					<xsl:otherwise>
						<item>&#xD;&#xA;</item> <!-- CR/LF -->
					</xsl:otherwise>
				</xsl:choose>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Root|a:ComplexType" mode="puml">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:choose>
				<xsl:when test="a:SuperType">
					<xsl:variable name="superClassName" select="substring-after(a:SuperType/@baseClass, '#')"/>
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="$className"/> inherits from <xsl:value-of select="$superClassName"/></item>
						<list begin="{concat(if (not(a:Stereotype[@label='Concrete'])) then 'abstract class' else 'class', ' ', $className, ' ', $stereotypes, ' ', if (not(a:Stereotype[@label='Concrete'])) then '&lt;&lt;abstract&gt;&gt;' else '', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain" mode="puml"/>
								</xsl:when>
								<xsl:otherwise>
									<item>[Attributes hidden]</item>
								</xsl:otherwise>
							</xsl:choose>
						</list>
						<xsl:if test="not(//node()[@name = $superClassName]/@hideInDiagrams = 'true')">
							<item><xsl:value-of select="concat($superClassName, ' &lt;|-- ', $className)"/></item>
						</xsl:if>
						<!-- Now process all associations: -->
						<xsl:choose>
							<xsl:when test="a:Reference|a:Instance">
								<xsl:apply-templates select="a:Reference|a:Instance" mode="puml"/>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
							</xsl:when>
							<xsl:otherwise>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
							</xsl:otherwise>
						</xsl:choose>
					</list>
				</xsl:when>
				<xsl:otherwise>
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="$className"/></item>
						<list begin="{concat(if (not(a:Stereotype[@label='Concrete'])) then 'abstract class' else 'class', ' ', $className, ' ', $stereotypes, ' ', if (not(a:Stereotype[@label='Concrete'])) then '&lt;&lt;abstract&gt;&gt;' else '', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain" mode="puml"/>
								</xsl:when>
								<xsl:otherwise>
									<item>[Attributes hidden]</item>
								</xsl:otherwise>
							</xsl:choose>							
						</list>
						<!-- Now process all associations: -->
						<xsl:choose>
							<xsl:when test="a:Reference|a:Instance">
								<xsl:apply-templates select="a:Reference|a:Instance" mode="puml"/>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
							</xsl:when>
							<xsl:otherwise>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
							</xsl:otherwise>
						</xsl:choose>
					</list>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Reference|a:Instance" mode="puml">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<xsl:variable name="baseProperty" select="@baseProperty" />
			<xsl:variable name="inverse" select="//a:InverseReference[@inverseBaseProperty = $baseProperty]|//a:InverseInstance[@inverseBaseProperty = $baseProperty]"/>
			<xsl:variable name="sourceClass">
				<xsl:choose>
					<xsl:when test="$inverse"><xsl:value-of select="$inverse[1]/@type"/></xsl:when>
					<xsl:otherwise><xsl:value-of select="substring-after(@inverseBasePropertyClass, '#')"/></xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="targetClass"><xsl:value-of select="@type"/></xsl:variable>
			<xsl:variable name="sourceCardinality">
				<xsl:choose>
					<xsl:when test="$inverse"><xsl:value-of select="if ($inverse[1]/@minOccurs = $inverse[1]/@maxOccurs) then $inverse[1]/@minOccurs else concat($inverse[1]/@minOccurs, '..', replace(replace($inverse[1]/@maxOccurs, 'unbounded', '*'), 'n', '*'))"/></xsl:when>
					<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
				</xsl:choose>
			</xsl:variable> 
			<xsl:variable name="sourceRoleEndName">
				<xsl:choose>
					<xsl:when test="$inverse"><xsl:value-of select="concat('+', $inverse[1]/@name)"/></xsl:when>
					<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="targetCardinality">
				<xsl:choose>
					<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:call-template name="association-cardinality"/></xsl:when>
					<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="targetRoleEndName">
				<xsl:choose>
					<xsl:when test="not(@name = '')"><xsl:value-of select="concat('+', @name)"/></xsl:when>
					<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<!-- Determine if the association is either an aggregate, a composite, or a stanard association: -->
			<xsl:variable name="associationType">
				<xsl:choose>
					<!-- there are an edge case where the inverse will not be provided in the intermediary profile format.  If this case a $sourceClass will be empty -->
					<!-- In that case we must obtain the $sourceClass from -->
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'ofAggregate']">--o</xsl:when>
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'aggregateOf']">o--&gt;</xsl:when>
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'ofComposite']">--*</xsl:when>
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'compositeOf']">*--&gt;</xsl:when>
					<xsl:otherwise>--&gt;</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>

			<!-- Output the association -->	
			<item><xsl:value-of select="concat($sourceClass, ' ', '&quot;', $sourceRoleEndName, ' ', $sourceCardinality, '&quot;', ' ', $associationType, ' ', '&quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', $targetClass)"/><xsl:if test="a:Stereotype[contains(., '#enumeration')] or a:Stereotype[contains(., '#compound')] or a:Stereotype[contains(., '#cimdatatype')] or a:Stereotype[contains(., '#primitive')] or (self::a:Reference and not(a:Stereotype[contains(., '#byreference')]))"><xsl:value-of select="if ($enableDarkMode) then '#FF2D2D' else '#red'"/></xsl:if><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat(' : ', $stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose></item>
			
			<!-- If none of the below four types of elements is defined as a top level class for $targetClass then it means that the class has 
				 not yet been pulled into the profile and therefore should be flagged as an error (i.e. expressed as class in pink) -->
			<xsl:if test="not(//a:ComplexType[@name = $targetClass]|//a:Root[@name = $targetClass]|//a:CompoundType[@name = $targetClass]|//a:EnumeratedType[@name = $targetClass]|//a:PrimitiveType[@name = $targetClass])">
				<xsl:variable name="stereotype">
					<xsl:choose>
						<xsl:when test="a:Stereotype[contains(., '#enumeration')]"><xsl:value-of select="'&lt;&lt;enumeration&gt;&gt;'"/></xsl:when>
						<xsl:when test="a:Stereotype[contains(., '#cimdatatype')]"><xsl:value-of select="'&lt;&lt;cimdatatype&gt;&gt;'"/></xsl:when>
						<xsl:when test="a:Stereotype[contains(., '#compound')]"><xsl:value-of select="'&lt;&lt;compound&gt;&gt;'"/></xsl:when>
						<xsl:when test="a:Stereotype[contains(., '#primitive')]"><xsl:value-of select="'&lt;&lt;primitive&gt;&gt;'"/></xsl:when>
						<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<list begin="" indent="" delim="" end="">
					<item>' This abstract indicates an "orphan" reference on an invalid Reference/Instance that must be fixed in the profile</item>
					<item>' We highlight it by generating a color indicating it is invalid and that the user should add in the orphaned type</item>
					<list begin="{concat('abstract class ', $targetClass, ' &lt;&lt;error&gt;&gt; ', (if (not($stereotype = null)) then $stereotype else ''), $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
						<item>' nothing to generate</item>
					</list>
				</list>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, and Enumerated attributes templates)                                        -->
	<!-- ============================================================================================================ -->
	<xsl:template match="a:Simple" mode="puml">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="@xstype"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Domain" mode="puml">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@dataType, '#')"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Enumerated|a:Compound" mode="puml">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="@type"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Value|a:Unit|a:Multiplier" mode="puml">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant, ' {readOnly}') else ''"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@baseClass, '#')"/> <xsl:call-template name="cardinality"/><xsl:value-of select="$constant"/></item>
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- END SECTION:  (Simple, Domain, and Enumerated attributes templates)                           -->
	<!-- ============================================================================================================ -->

	<xsl:template match="stereotypes" mode="puml">
		<xsl:if test="a:Stereotype">
			<xsl:variable name="stereotypes">
				<xsl:for-each select="a:Stereotype">
					<xsl:variable name="currentStereotype" select="."/>
					<xsl:variable name="stereotype" select="substring-after(., '#')"/>
					<xsl:choose>
						<!-- Below is the set of stereotypes that are internal metadata. These we do not display on a class... -->
						<xsl:when test="not(($stereotype = 'byreference') or ($stereotype = 'concrete'))">
							<xsl:value-of select="concat('&lt;&lt;', $currentStereotype/@label, '&gt;&gt;')" />
						</xsl:when>
						<xsl:otherwise></xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="string-length($stereotypes) > 0">
					<xsl:value-of select="$stereotypes"/>
				</xsl:when>
				<xsl:otherwise></xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="attribute-stereotypes">
		<xsl:if test="a:Stereotype">
			<xsl:variable name="stereotypes">
				<xsl:for-each select="a:Stereotype">
					<xsl:variable name="currentStereotype" select="."/>
					<xsl:variable name="stereotype" select="substring-after(., '#')"/>
					<xsl:choose>
						<!-- Below is the set of stereotypes that are internal metadata. These we do not display on an attribute or association -->
						<xsl:when test="not(($stereotype = 'compound') or ($stereotype = 'enumeration') or ($stereotype = 'attribute') or ($stereotype = 'byreference') or ($stereotype = 'enum') or ($stereotype = 'concrete') or ($stereotype = 'ofAggregate') or ($stereotype = 'aggregateOf') or ($stereotype = 'ofComposite') or ($stereotype = 'compositeOf'))">
							<xsl:value-of select="$currentStereotype/@label" /><xsl:text>,</xsl:text>
						</xsl:when>
						<xsl:otherwise></xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
			</xsl:variable>
			<xsl:if test="string-length($stereotypes) > 0">
				<xsl:choose>
					<xsl:when test="ends-with($stereotypes, ',')">
						<xsl:value-of select="concat('&lt;&lt;', substring($stereotypes, 1, string-length($stereotypes) - 1), '&gt;&gt;')"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('&lt;&lt;', $stereotypes, '&gt;&gt;')"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="cardinality">
		<xsl:choose>
			<xsl:when test="$hideCardinalityForRequiredAttributes and (@minOccurs = 1)">
				<xsl:value-of select="if (@minOccurs = @maxOccurs) then '' else concat(' [', @minOccurs, '..', replace(replace(@maxOccurs, 'unbounded', '*'), 'n', '*'), ']')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="if (@minOccurs = @maxOccurs) then concat(' [', @minOccurs, ']') else concat(' [', @minOccurs, '..', replace(replace(@maxOccurs, 'unbounded', '*'), 'n', '*'), ']')"/>
			</xsl:otherwise>
		</xsl:choose>	
	</xsl:template>
	
	<xsl:template name="association-cardinality">	
		<xsl:value-of select="if (@minOccurs = @maxOccurs) then @minOccurs else concat(@minOccurs, '..', replace(replace(@maxOccurs, 'unbounded', '*'), 'n', '*'))"/>
	</xsl:template>
		
	<!-- Template to match Note elements -->
	<xsl:template match="a:Note" mode="puml-profile-notes">
		<!-- Remove double quotes to eliminate broken comments, etc. -->
		<xsl:variable name="paragraph" select="translate(., '&quot;', '')"/>
		<list begin="" indent="   " delim="&#xD;" end="">
			<xsl:call-template name="parse-notes-puml">
				<xsl:with-param name="notes" select="$paragraph"/>
			</xsl:call-template>
		</list>
	</xsl:template>

	<xsl:template name="parse-notes-puml">
		<xsl:param name="notes" />
		<xsl:choose>
			<xsl:when test="(string-length($notes) &lt;= 80)">
				<item><xsl:value-of select="$notes"/></item>
				<item>&#xD;</item>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="cutPos" select="string-length(substring($notes, 1, 80)) + string-length(substring-before(substring($notes, 81), ' ')) + 1"/>
				<item><xsl:value-of select="substring($notes, 1, $cutPos)"/></item>
				<xsl:call-template name="parse-notes-puml">
					<xsl:with-param name="notes" select="substring($notes, $cutPos + 1)"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template> 
	
	<xsl:template match="node()" mode="puml">
	</xsl:template>
	
	<xsl:template match="text()" mode="puml">
		<!--  dont pass text through  -->
	</xsl:template>
	
</xsl:stylesheet>