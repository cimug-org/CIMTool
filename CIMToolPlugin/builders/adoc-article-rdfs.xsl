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
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:a="http://langdale.com.au/2005/Message#" xmlns="http://langdale.com.au/2009/Indent">
	
	<xsl:output indent="no" method="text" encoding="utf-8"/>
	<xsl:param name="fileName"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<xsl:variable name="apos">'</xsl:variable>

	<xsl:template match="a:Catalog">
		<!--  Delimiter is CR/LF -->
		<list begin="" indent="" delim="&#xD;&#xA;" end="">
			<item>:pdf-themesdir: {docdir}</item>
			<item>:pdf-theme: adoc-article-rdfs</item>
			<item></item>
			<xsl:choose>
				<xsl:when test="$envelope != 'Profile'">
					<item>= <xsl:value-of select="$envelope"/> Profile Specification</item>
					<item></item>
				</xsl:when>
				<xsl:otherwise>
					<item>= <xsl:value-of select="$fileName"/> Profile Specification</item>
					<item></item>
				</xsl:otherwise>
			</xsl:choose>
			<item>// Settings:</item>
			<item>:doctype: article</item>
			<item>:reproducible:</item>
			<item>:icons: font</item>
			<item>:sectnums:</item>
			<item>:sectnumlevels: 4</item>
			<item>:xrefstyle: short</item>
			<item>:toclevels: 4</item>
			<item>:toc-title: Table of Contents</item>
			<item>:toc:</item>
			<item>:toc-placement: preamble</item>
			<item></item>
			<item>== Overview</item>
			<item></item>
			<item>Profile namespace:  <xsl:value-of select="$baseURI"/></item>
			<item></item>
			<item>plantuml::./Profiles/<xsl:value-of select="$fileName"/>.rdfs-t2b.puml[format=svg, align=center, caption="Figure 1: ", title="<xsl:value-of select="$envelope"/> Profile"]</item>
			<item></item>
			<xsl:apply-templates mode="annotate" select="a:Note"/>
			<item></item>
			<xsl:if test="(count(/.//a:Root) + count(/.//a:Message)) > 0">
				<item>=== Concrete Classes</item>
				<item></item>
				<xsl:apply-templates select="a:Root|a:Message">
				<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</xsl:if>
			<item></item>
			<xsl:if test="count(/.//a:ComplexType) > 0">
				<item>=== Abstract Classes</item>
				<item></item>
				<xsl:apply-templates select="a:ComplexType">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</xsl:if>
			<item></item>
			<xsl:if test="count(/.//a:CompoundType) > 0">
				<item>=== Compound Types </item>
				<item></item>
				<xsl:apply-templates select="a:CompoundType">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</xsl:if>
			<item></item>
			<xsl:if test="count(/.//a:EnumeratedType) > 0">
				<item>=== Enumerations</item>
				<item></item>
				<xsl:apply-templates select="a:EnumeratedType">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</xsl:if>
			<item></item>
			<xsl:if test="count(/.//a:SimpleType) > 0">
				<item>=== Datatypes</item>
				<item></item>
				<xsl:apply-templates select="a:SimpleType">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</xsl:if>
			<item></item>
			<xsl:if test="count(/.//a:PrimitiveType) > 0">
				<item>=== Primitive Types</item>
				<item></item>
				<xsl:apply-templates select="a:PrimitiveType">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</xsl:if>
			<item></item>
		</list>
	</xsl:template>

	<xsl:template match="a:Message">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate"/><xsl:apply-templates/>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:Root">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		<item></item>
		<xsl:call-template name="type_definition"/>
		<item></item>
	</xsl:template>
	
	<xsl:template name="type_definition">
		<xsl:apply-templates mode="annotate"/>
		<item></item>
		<xsl:choose>
			<xsl:when test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:SuperType">
				<xsl:if test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound">
					<item>*Native Members*</item>
					<item></item>
					<item>[%header,width="100%",cols="25%,^10%,20%,45%"]</item>
					<item>|===</item>
					<item>|name |mult |type |description</item>		
					<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound"/>
					<item>|===</item>
				</xsl:if>
				<xsl:if test="a:SuperType">
					<item></item>
					<item>*Inherited Members*</item>
					<item></item>
					<item>[%header,width="100%",cols="25%,^10%,20%,45%"]</item>
					<item>|===</item>
					<item>|name |mult |type |description</item>	
					<item></item>			
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
		<item></item>
		<item>|<xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate"/>
	</xsl:template>

	<xsl:template match="a:Simple">
		<item></item>
		<item>|<xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>,<xsl:value-of select="substring-after(@cimDatatype, '#')"/>&gt;&gt;</item>
		<item>|</item><xsl:apply-templates mode="annotate"/>
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
		<item></item>
		<item>|<xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>,<xsl:value-of select="@type"/>&gt;&gt;</item>
		<item>|see &lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>,<xsl:value-of select="../@name"/>&gt;&gt;</item>
	</xsl:template>
	
	<xsl:template match="a:Simple" mode="inherited">
		<item></item>
		<item>|<xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/></item>
		<item>|<xsl:value-of select="@minOccurs"/>..<xsl:value-of select="@maxOccurs"/></item>
		<item>|&lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>,<xsl:value-of select="substring-after(@cimDatatype, '#')"/>&gt;&gt;</item>
		<item>|see &lt;&lt;<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>,<xsl:value-of select="../@name"/>&gt;&gt;</item>
	</xsl:template>
	
	<xsl:template match="a:ComplexType">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
		<item></item>
		<xsl:call-template name="type_definition"/>
		<item></item>
	</xsl:template>
	
	<xsl:template match="a:CompoundType">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate"/>
		<item>*Members*</item>
		<item></item>
		<item>[%header,width="100%",cols="25%,^10%,20%,45%"]</item>
		<item>|===</item>
		<item>|name |mult |type |description</item>		
		<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound"/>
		<item>|===</item>	
	</xsl:template>
	
	<xsl:template match="a:SimpleType">
		<item>[#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>]</item>
		<item>==== <xsl:value-of select="@name"/></item>
		<item></item>
		<xsl:apply-templates mode="annotate"/>
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
		<xsl:apply-templates mode="annotate"/>
		<item></item>
		<item>[%header,width="100%",cols="25%,75%"]</item>
		<item>|===</item>
		<item>|name |description</item>
		<xsl:apply-templates/>
		<item>|===</item>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedValue">
		<item></item>
		<item>|<xsl:value-of select="@name"/></item>
		<item>|</item><xsl:apply-templates mode="annotate"/>
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
	
	<!-- Default templates for annotations supporting annotations without formatting... -->
	<xsl:template match="a:Comment|a:Note" mode="annotate">
		<item><xsl:call-template name="replace-non-ascii"><xsl:with-param name="text" select="."/></xsl:call-template></item>
		<item></item>
	</xsl:template>
	
	<xsl:template match="node()" mode="annotate">
	</xsl:template>

	<xsl:template match="node()">
	</xsl:template>
	
	<xsl:template name="replace-non-ascii">
		<xsl:param name="text"/>
		<xsl:variable name="ascii"> !"#$%&amp;'()*+,-./0123456789:;=&lt;>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~</xsl:variable>
		<xsl:variable name="non-ascii" select="translate($text, $ascii, '')"/>
		<xsl:choose>
			<xsl:when test="$non-ascii">
				<xsl:variable name="char" select="substring($non-ascii, 1, 1)"/>
				<!-- recursive call -->
				<xsl:call-template name="replace-non-ascii">
					<xsl:with-param name="text" select="translate($text, $char, '')"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>