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
	xmlns="http://langdale.com.au/2009/Indent" 
	xmlns:fn="http://www.w3.org/2005/xpath-functions" 
	xmlns:cimtool="http://cimtool.ucaiug.io/functions">
	
	<xsl:output indent="no" method="xml" encoding="UTF-8" omit-xml-declaration="yes"/>
	<xsl:preserve-space elements="a:AsciiDoc"/>
	<xsl:param name="fileName"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<xsl:variable name="zeroWidthSpace" select="'&#x200B;'"/>
	<xsl:param name="namespacePrefixes"/>
	
	<!-- XSLT 3.0 map for namespace to prefix parsing -->
	<xsl:variable name="nsPrefixesMap" as="map(xs:string, xs:string)">
		<xsl:map>
			<xsl:for-each select="fn:tokenize($namespacePrefixes, '\|')[normalize-space()]">
				<xsl:variable name="parts" select="tokenize(., '=')"/>
				<xsl:map-entry key="$parts[1]" select="fn:string-join(fn:subsequence($parts, 2), '=')"/>
			</xsl:for-each>
		</xsl:map>
	</xsl:variable>

	<xsl:function name="cimtool:hasPrefix" as="xs:boolean">
		<xsl:param name="uri" as="xs:string"/>
		<xsl:variable name="namespace" select="concat(substring-before($uri, '#'), '#')"/>
		<xsl:variable name="hasPrefix" select="map:contains($nsPrefixesMap, $namespace)"/>
		<xsl:sequence select="$hasPrefix"/>
	</xsl:function>
	
	<xsl:function name="cimtool:getPrefix" as="xs:string">
		<xsl:param name="uri" as="xs:string"/>
		
		<xsl:variable name="namespace" select="concat(substring-before($uri, '#'), '#')"/>
		<xsl:variable name="isExtension" select="not(starts-with($uri, concat($ontologyURI, '#')))"/>
		<xsl:variable name="hasPrefix" select="map:contains($nsPrefixesMap, $namespace)"/>
		<xsl:variable name="prefix" select="if ($hasPrefix) then $nsPrefixesMap($namespace) else ''"/>
		<xsl:variable name="result" select="if ($isExtension and $hasPrefix) then concat($prefix, ':', $zeroWidthSpace) else ''"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
	
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

	<!--
	  Function: cimtool:unique-anonymous-id
	  Purpose: Produce a unique, identifier for the specified element.
	  Parameters:
		$element - the element (Complex, SimpleEnumerated, SimpleCompound, Choice or Reference) to generate an identifier for.
	  Returns:
		xs:string - unique identifier for the supplied element.
	  Algorithm:
		1) Determine the id prefix based upon the element type.
		2) Normalize this element's @name to [A-Za-z0-9_].
		3) Build a normalized '-' joined chain of ancestor @name values (also normalized).
		4) Compute an occurrence index k among preceding a:Complex elements with the same @name
		   (document-order tie-breaker).
		5) Concatenate into: <prefix>__<ancestorNames>__<thisName>__n<k>
	-->
	<xsl:function name="cimtool:unique-anonymous-id" as="xs:string">
		<xsl:param name="element" as="element()"/>
		<xsl:variable name="prefix">
			<xsl:choose>
				<xsl:when test="$element/self::a:Complex|$element/self::a:SimpleEnumerated|$element/self::a:SimpleCompound">
					<xsl:value-of select="'Anon'"/>
				</xsl:when>
				<xsl:when test="$element/self::a:Reference">
					<xsl:value-of select="'Ref'"/>
				</xsl:when>
				<xsl:when test="$element/self::a:Choice">
					<xsl:value-of select="'Choice'"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="'Root'"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="thisName" select="replace($element/@name, '[^A-Za-z0-9_]', '_')"/>
		<xsl:variable name="ancestorNames" select="string-join($element/ancestor::*[@name]/replace(@name,'[^A-Za-z0-9_]', '_'), '-')"/>
		<xsl:variable name="k" select="count($element/preceding::a:Complex[@name = $element/@name]) + 1"/>
		<xsl:sequence select="concat($prefix, '-', if ($ancestorNames) then $ancestorNames else 'ROOT', '-', $thisName, '-n', $k)"/>
	</xsl:function>
	
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
				<!-- Note that AsciiDoc auto-generates the period below into a prefix of:  "Figure N." -->
				<!-- Allowing for auto-number of the figures throughout a document                     -->
				<item>.<xsl:value-of select="$envelope"/> Profile</item>
				<item>plantuml::./Profiles/<xsl:value-of select="$fileName"/>.json-l2r.puml[format=svg, align=center]</item>
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
					<item>=== Compound Types</item>
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
				<xsl:if test="count(.//a:Complex) > 0">
					<item></item>
					<item>=== Anonymous Inline Classes</item>
					<item></item>
					<xsl:apply-templates select=".//a:Complex" mode="anonymous-class-generation">
						<xsl:sort select="substring-after(@baseClass, '#')"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:SimpleCompound) > 0">
					<item></item>
					<item>=== Anonymous Inline Compound Types</item>
					<item></item>
					<xsl:apply-templates select=".//a:SimpleCompound" mode="anonymous-class-generation">
						<xsl:sort select="substring-after(@baseClass, '#')"/>
					</xsl:apply-templates>
				</xsl:if>
				<xsl:if test="count(/.//a:SimpleEnumerated) > 0">
					<item></item>
					<item>=== Anonymous Inline Enumerations</item>
					<item></item>
					<xsl:apply-templates select=".//a:SimpleEnumerated" mode="anonymous-class-generation">
						<xsl:sort select="substring-after(@baseClass, '#')"/>
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
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>==== <xsl:value-of select="@name"/><xsl:call-template name="process-class-stereotypes"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<xsl:apply-templates/>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:Root">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>==== <xsl:value-of select="@name"/><xsl:call-template name="process-class-stereotypes"/></item>
		<item></item>
		<xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		<item></item>
		<item>:<xsl:value-of select="@name"/>:</item>
		<item></item>
		<xsl:call-template name="type_definition"/>
		<item></item>
		<item>:!<xsl:value-of select="@name"/>:</item>
		<item></item>
	</xsl:template>
	
	<xsl:template name="type_definition">
		<xsl:apply-templates mode="annotate-type"/>
		<xsl:choose>
			<xsl:when test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:Choice|a:Complex|a:SimpleCompound|a:SimpleEnumerated|a:SuperType">
				<xsl:if test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:Choice|a:Complex|a:SimpleCompound|a:SimpleEnumerated">
					<item></item>
					<item>*Native Members*</item>
					<item></item>
					<item>[%header,width="100%",cols="25%,^10%,20%a,45%a"]</item>
					<item>|===</item>
					<item>|name |mult |type |description</item>
					<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:Choice|a:Complex|a:SimpleCompound|a:SimpleEnumerated"/>
					<item>|===</item>
				</xsl:if>
				<xsl:if test="a:SuperType">
					<item></item>
					<item>*Inherited Members*</item>
					<item></item>
					<item>[%header,width="100%",cols="25%,^10%,20%a,45%a"]</item>
					<item>|===</item>
					<item>|name |mult |type |description</item>
					<xsl:apply-templates select="a:SuperType" mode="inherited"/>
					<item>|===</item>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<!-- Currently we do nothing.  In the future we may add here. -->
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="a:SuperType" mode="inheritance_hierarchy">
		<xsl:variable name="inheritance">
			&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>,<xsl:value-of select="@name"/>&gt;&gt;<xsl:variable name="supertype_name" select="@name"/><xsl:if test="/*/node()[@name = $supertype_name]/a:SuperType"> => <xsl:apply-templates select="/*/node()[@name = $supertype_name]/a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		</xsl:variable>
		<xsl:value-of select="$inheritance"/>
	</xsl:template>

	<xsl:template match="a:Choice">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:value-of select="if (cimtool:hasPrefix(@baseProperty)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseProperty), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="if (a:Stereotype[contains(., '#byreference')]) then concat(@name, ' (@ref)') else @name"/><xsl:if test="$roles != ''">##</xsl:if><xsl:text> </xsl:text><!--<xsl:call-template name="process-attribute-stereotypes"/>--></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|</item>
		<item>[cols="1"]</item>
		<item>!====</item>
		<item>! Choices </item>
		<item><xsl:apply-templates select="a:Reference|a:Instance|a:Complex" mode="choice-table-row"/></item>
		<item>!====</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell"/>
	</xsl:template>

	<xsl:template match="a:Reference" mode="choice-table-row">
		<item></item>
		<item>!&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@baseClass, '#')"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:Instance" mode="choice-table-row">
		<item>!&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@baseClass, '#')"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
	</xsl:template>
	
	<xsl:template match="a:Complex" mode="choice-table-row">
		<xsl:variable name="anonymousId" select="cimtool:unique-anonymous-id(.)"/>
		<item>!&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="$anonymousId"/>,<xsl:value-of select="substring-after(@baseClass, '#')"/>&gt;&gt;<xsl:text> (anon)</xsl:text></item>
	</xsl:template>
	
	<xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Compound|a:Domain">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:value-of select="if (cimtool:hasPrefix(@baseProperty)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseProperty), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="if (a:Stereotype[contains(., '#byreference')]) then concat(@name, ' (@ref)') else @name"/><xsl:if test="$roles != ''">##</xsl:if><xsl:text> </xsl:text><!--<xsl:call-template name="process-attribute-stereotypes"/>--></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell"/>
	</xsl:template>
	
	<!-- An anonymous complex type can be specified for an attribute in a Root or ComplexType. This handles the table row generation for this case. -->
	<xsl:template match="a:Complex">
		<xsl:variable name="anonymousId" select="cimtool:unique-anonymous-id(.)"/>
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseClass, concat($ontologyURI, '#')))">.extension</xsl:if>
		</xsl:variable>
		<item>|<xsl:value-of select="if (cimtool:hasPrefix(@baseProperty)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseProperty), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">##</xsl:if><xsl:text> </xsl:text><!--<xsl:call-template name="process-attribute-stereotypes"/>--></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="$anonymousId"/>,<xsl:value-of select="substring-after(@baseClass, '#')"/>&gt;&gt;<xsl:text> (anon)</xsl:text></item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell"/>
	</xsl:template>
	
	<!-- An anonymous compound can be specified for an attribute in a Root, ComplexType, or SimpleCompound. This handles the table row generation for those. -->
	<xsl:template match="a:SimpleCompound">
		<xsl:variable name="anonymousId" select="cimtool:unique-anonymous-id(.)"/>
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
		</xsl:variable>
		<item>|<xsl:value-of select="if (cimtool:hasPrefix(@baseProperty)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseProperty), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">##</xsl:if><xsl:text> </xsl:text><!--<xsl:call-template name="process-attribute-stereotypes"/>--></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="$anonymousId"/>,<xsl:value-of select="substring-after(@baseClass, '#')"/>&gt;&gt;<xsl:text> (anon)</xsl:text></item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell"/>
	</xsl:template>
	
	<!-- An anonymous enumeration can be specified for an attribute in a Root, ComplexType, or SimpleCompound. This handles the table row generation for those. -->
	<xsl:template match="a:SimpleEnumerated">
		<xsl:variable name="anonymousId" select="cimtool:unique-anonymous-id(.)"/>
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
		</xsl:variable>
		<item>|<xsl:value-of select="if (cimtool:hasPrefix(@baseProperty)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseProperty), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">##</xsl:if><xsl:text> </xsl:text><!--<xsl:call-template name="process-attribute-stereotypes"/>--></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="$anonymousId"/>,<xsl:value-of select="substring-after(@baseClass, '#')"/>&gt;&gt;<xsl:text> (anon)</xsl:text></item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell"/>
	</xsl:template>

	<xsl:template match="a:Simple">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:value-of select="if (cimtool:hasPrefix(@baseProperty)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseProperty), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">##</xsl:if><xsl:text> </xsl:text><!--<xsl:call-template name="process-attribute-stereotypes"/>--></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>,<xsl:value-of select="substring-after(@cimDatatype, '#')"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate-table-cell"/>
	</xsl:template>

	<xsl:template match="a:SuperType" mode="inherited">
		<xsl:apply-templates select="//a:ComplexType[@name=current()/@name]" mode="inherited"/>
		<xsl:apply-templates select="//a:Root[@name=current()/@name]" mode="inherited"/>
	</xsl:template>
	
	<xsl:template match="a:ComplexType|a:Root" mode="inherited">
		<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:Choice|a:Complex|a:SimpleCompound|a:SimpleEnumerated" mode="inherited"/>
		<xsl:apply-templates select="a:SuperType" mode="inherited"/>
	</xsl:template>
	
	<xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Compound|a:Domain|a:Complex|a:SimpleCompound|a:SimpleEnumerated" mode="inherited">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:value-of select="if (cimtool:hasPrefix(@baseProperty)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseProperty), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">##</xsl:if><xsl:text> </xsl:text><!--<xsl:call-template name="process-attribute-stereotypes"/>--></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item>|see &lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>,<xsl:value-of select="../@name"/>&gt;&gt;</item>
	</xsl:template>
	
	<xsl:template match="a:Simple" mode="inherited">
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item>|<xsl:value-of select="if (cimtool:hasPrefix(@baseProperty)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseProperty), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="@name"/><xsl:if test="$roles != ''">##</xsl:if><xsl:text> </xsl:text><!--<xsl:call-template name="process-attribute-stereotypes"/>--></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>,<xsl:value-of select="substring-after(@cimDatatype, '#')"/>&gt;&gt;</item>
		<item>|see &lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>,<xsl:value-of select="../@name"/>&gt;&gt;</item>
	</xsl:template>
	
	<xsl:template match="a:ComplexType|a:CompoundType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>==== <xsl:value-of select="@name"/><xsl:call-template name="process-class-stereotypes"/></item>
		<item></item>
		<xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		<item></item>
		<item>:<xsl:value-of select="@name"/>:</item>
		<item></item>
		<xsl:call-template name="type_definition"/>
		<item></item>
		<item>:!<xsl:value-of select="@name"/>:</item>
		<item></item>
	</xsl:template>
	
	<!-- Anonymous inline complex and compound types -->
	<xsl:template match="a:Complex|a:SimpleCompound" mode="anonymous-class-generation">
		<xsl:variable name="anonymousId" select="cimtool:unique-anonymous-id(.)"/>
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="$anonymousId"/>]]</item>
		<item>==== <xsl:value-of select="concat(substring-after(@baseClass, '#'), ' (anonymous)')"/></item>
		<item></item>
		<item>:<xsl:value-of select="@name"/>:</item>
		<item></item>
		<xsl:call-template name="type_definition"/>
		<item></item>
		<item>:!<xsl:value-of select="@name"/>:</item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:SimpleType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>==== <xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<!-- 
			Note that for the definition of the simple type template for XSDs the definition
			is different from that of RDFS in that we only want the actual primitive XSD type
			displayed and not a table breaking out the details of the CIMDatatype itself
		-->
		<item></item>
		<item>XSD type: <xsl:value-of select="@xstype"/></item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:PrimitiveType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>==== <xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item></item>
		<item>XSD type: <xsl:value-of select="@xstype"/></item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedType">
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]]</item>
		<item>==== <xsl:value-of select="@name"/><xsl:call-template name="process-class-stereotypes"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item></item>
		<item>[%header,width="100%",cols="25%,75%a"]</item>
		<item>|===</item>
		<item>|name |description</item>
		<xsl:apply-templates>
			<xsl:sort select="@name" collation="http://www.w3.org/2005/xpath-functions/collation/html-ascii-case-insensitive"/>
		</xsl:apply-templates>
		<item>|===</item>
	</xsl:template>
	
	<!-- Anonymous inline enumeration -->
	<xsl:template match="a:SimpleEnumerated" mode="anonymous-class-generation">
		<xsl:variable name="anonymousId" select="cimtool:unique-anonymous-id(.)"/>
		<item>[[<xsl:value-of select="$fileName"/>-<xsl:value-of select="$anonymousId"/>]]</item>
		<item>==== <xsl:value-of select="concat(substring-after(@baseClass, '#'), ' (anonymous)')"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate-type"/>
		<item></item>
		<item>[%header,width="100%",cols="25%,75%a"]</item>
		<item>|===</item>
		<item>|name |description</item>
		<xsl:apply-templates>
			<xsl:sort select="@name" collation="http://www.w3.org/2005/xpath-functions/collation/html-ascii-case-insensitive"/>
		</xsl:apply-templates>
		<item>|===</item>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedValue">
		<!-- 
			An enumerated value can be a child element of either and standard a:EnumeratedType or an anonymous 
			inline enumerated type and therefore the link needs to be generated differently dependong on which. 
		-->
		<xsl:variable name="link">
			<xsl:choose>
				<xsl:when test="parent::a:SimpleEnumerated">
					<xsl:variable name="parentAnonymousId" select="cimtool:unique-anonymous-id(parent::*)"/>
					<xsl:value-of select="$fileName"/>-<xsl:value-of select="$parentAnonymousId"/>-<xsl:value-of select="@name"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$fileName"/>-<xsl:value-of select="fn:substring-before(fn:substring-after(@baseResource, '#'), '.')"/>-<xsl:value-of select="@name"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="roles">
			<xsl:if test="not(starts-with(@baseResource, concat($ontologyURI, '#')))">.extension</xsl:if>
			<xsl:call-template name="attribute-stereotype-roles"/> 
		</xsl:variable>
		<item></item>
		<item>|<item>[[<xsl:value-of select="$link"/>]]</item><xsl:value-of select="if (cimtool:hasPrefix(@baseResource)) then concat('[.extension-prefix]##', cimtool:getPrefix(@baseResource), '##') else ''"/><xsl:if test="$roles != ''">[<xsl:value-of select="$roles"/>]##</xsl:if><xsl:value-of select="if (@code and @code != '') then concat(@name, '   (code=', @code, ')') else @name"/><xsl:if test="$roles != ''">##</xsl:if></item>
		<item>|</item><xsl:apply-templates select="a:Comment|a:Note|a:AsciiDoc" mode="annotate-table-cell"/>
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