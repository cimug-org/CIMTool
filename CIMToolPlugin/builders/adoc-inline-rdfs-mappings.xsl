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
	
	<xsl:output indent="no" method="xml" encoding="utf-8" omit-xml-declaration="yes"/>
	<xsl:preserve-space elements="a:AsciiDoc"/>
	<xsl:param name="fileName"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
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
						<item>== <xsl:value-of select="$envelope"/> Profile Specification</item>
						<item></item>
					</xsl:when>
					<xsl:otherwise>
						<item>== <xsl:value-of select="$fileName"/> Profile Specification</item>
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
				<!-- Note that AsciiDoc auto-generates the period below into a prefix of:  "Figure n." -->
				<!-- Allowing for auto-number of the figures throughout a document                     -->
				<item>.<xsl:value-of select="$envelope"/> Profile</item>
				<item>plantuml::./Profiles/<xsl:value-of select="$fileName"/>.rdfs-t2b.puml[format=svg, align=center]</item>
				<item></item>
				<xsl:apply-templates mode="annotate-type"/>
				<xsl:if test="(count(/.//a:Root) + count(/.//a:Message)) > 0">
					<item></item>
					<item>=== Concrete Classes</item>
					<item></item>
					<xsl:apply-templates select="a:Root|a:Message">
					<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:ComplexType) > 0">
					<item></item>
					<item>=== Abstract Classes</item>
					<item></item>
					<xsl:apply-templates select="a:ComplexType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:CompoundType) > 0">
					<item></item>
					<item>=== Compound Types </item>
					<item></item>
					<xsl:apply-templates select="a:CompoundType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:EnumeratedType) > 0">
					<item></item>
					<item>=== Enumerations</item>
					<item></item>
					<xsl:apply-templates select="a:EnumeratedType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:SimpleType) > 0">
					<item></item>
					<item>=== Datatypes</item>
					<item></item>
					<xsl:apply-templates select="a:SimpleType">
						<xsl:sort select="@name"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:PrimitiveType) > 0">
					<item></item>
					<item>=== Primitive Types</item>
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
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<xsl:apply-templates/>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:Root">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		<item></item>
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
					<item>===== Native Members</item>
					<item></item>
					<item>[%header,width="100%",cols="15%,15%,25%,45%a"]</item>
					<item>|===</item>
					<item>|name |type |description |mapping</item>		
					<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound"/>
					<item>|===</item>
				</xsl:if>
				<xsl:if test="a:SuperType">
					<item></item>
					<item>===== Inherited Members</item>
					<item></item>
					<item>[%header,width="100%",cols="15%,15%,25%,45%a"]</item>
					<item>|===</item>
					<item>|name |type |description |mapping</item>
					<xsl:apply-templates select="a:SuperType" mode="inherited"/>
					<item>|===</item>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="baseClass" select="@baseClass"/>
				<xsl:if test="not(child::a:Stereotype[contains(., '#concrete')]) and (count(/.//a:Reference[@baseClass=$baseClass]) > 0)">
					<item>This abstract class is a placeholder for 'By Reference' associations defined within this profile. Such classes have no attributes or associations defined. Rather, 'By Reference' associations of this type reference a corresponding concrete type in external profiles that this one is dependent upon.</item>
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
		<item>|<xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/><xsl:text> </xsl:text>[<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose>]</item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell" select="a:Comment|a:Note"/>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell" select="a:AsciiDoc"/>
	</xsl:template>

	<xsl:template match="a:Simple">
		<item>|<xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/><xsl:text> </xsl:text>[<xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/>]</item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>,<xsl:value-of select="substring-after(@cimDatatype, '#')"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell" select="a:Comment|a:Note"/>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell" select="a:AsciiDoc"/>
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
		<item>|<xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/><xsl:text> </xsl:text>[<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose>]</item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item>|see &lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>,<xsl:value-of select="../@name"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell" select="a:AsciiDoc"/>
	</xsl:template>
	
	<xsl:template match="a:Simple" mode="inherited">
		<item>|<xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/><xsl:text> </xsl:text>[<xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/>]</item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>,<xsl:value-of select="substring-after(@cimDatatype, '#')"/>&gt;&gt;</item>
		<item>|see &lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>,<xsl:value-of select="../@name"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell" select="a:AsciiDoc"/>
	</xsl:template>
	
	<xsl:template match="a:ComplexType">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		<item></item>
		<item>:<xsl:value-of select="@name"/>:</item>
		<xsl:call-template name="type_definition"/>
		<item>:!<xsl:value-of select="@name"/>:</item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:CompoundType">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item>===== Members</item>
		<item></item>
		<item>[%header,width="100%",cols="15%,15%,25%,45%a"]</item>
		<item>|===</item>
		<item>|name |type |description |mapping</item>		
		<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound"/>
		<item>|===</item>	
	</xsl:template>
	
	<xsl:template match="a:SimpleType">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item></item>
		<item>XSD type: <xsl:value-of select="@xstype"/></item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:PrimitiveType">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate"/>
		<item></item>
		<item>XSD type: <xsl:value-of select="@xstype"/></item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedType">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
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
	
	<!-- Templates for annotations supporting complex types... -->
	<xsl:template match="a:Stereotype">
		<xsl:if test="contains(., '#description')">
			<xsl:value-of select="concat('(', @label, ') ')"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="process-attribute-stereotypes">
		<xsl:if test="count(a:Stereotype[not(contains(., '#attribute')) and not(contains(., '#byreference'))]) > 0"> (<xsl:for-each select="a:Stereotype[not(contains(., '#attribute')) and not(contains(., '#byreference'))]">
				<xsl:value-of select="@label"/>
				<xsl:if test="position()!=last()">
					<xsl:value-of select="', '"/>
				</xsl:if>
			</xsl:for-each>)</xsl:if>
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
	<!-- table. This documentation is sources from both the UML and the end-user.         -->
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
	
</xsl:stylesheet>