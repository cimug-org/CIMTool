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
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xs="http://www.w3.org/2001/XMLSchema" 
	xmlns="http://www.w3.org/1999/xhtml" 
	xmlns:a="http://langdale.com.au/2005/Message#">	
<xsl:output indent="no" method="text" encoding="ASCII"/>
<xsl:param name="baseURI"/>
<xsl:param name="ontologyURI"/>
<xsl:param name="envelope">Profile</xsl:param>
<xsl:variable name="apos">'</xsl:variable>
<xsl:template match="a:Catalog">{\rtf1\ansi\ansicpg1252\deff0\deflang1033\deflangfe1033\fs20{\fonttbl{\f0\fswiss\fprq2\fcharset0 Arial;}{\f1\fswiss\fprq2\fcharset0{\*\fname Arial;}Arial CE;}
{\f23\froman\fcharset0\fprq2{\*\panose 02020603050405020304}Arial;}}{\colortbl;\red0\green0\blue238;\red255\green0\blue0;\red25\green150\blue255;\red50\green255\blue50;\red226\green239\blue217;\red255\green242\blue204;\red222\green234\blue246}
\pard\plain \sb120\qj\fs20\lang1033
{\par\b\fs28<xsl:value-of select="$envelope"/><xsl:if test="$envelope != 'Profile'"><text> Profile</text></xsl:if>\par}Profile namespace:  <xsl:value-of select="$baseURI" /> 
\par\pard\plain \sb120\qj\fs20\lang1033 
<xsl:if test="(count(/.//a:Root) + count(/.//a:Message)) > 0">
{\par\b\fs28 Concrete Classes \par}
<xsl:apply-templates select="a:Root|a:Message">
<xsl:sort select="@name"/>
</xsl:apply-templates>
</xsl:if>

<xsl:if test="count(/.//a:ComplexType) > 0">
{\par\b\fs28 Abstract Classes \par}
<xsl:apply-templates select="a:ComplexType">
<xsl:sort select="@name"/>
</xsl:apply-templates>
</xsl:if>

<xsl:if test="count(/.//a:CompoundType) > 0">
{\par\b\fs28 Compound Types \par}
<xsl:apply-templates select="a:CompoundType"  >
	<xsl:sort select="@name"/>
</xsl:apply-templates>
</xsl:if>

<xsl:if test="count(/.//a:EnumeratedType) > 0">
{\par\b\fs28 Enumerations \par}
<xsl:apply-templates select="a:EnumeratedType" >
<xsl:sort select="@name"/>
</xsl:apply-templates>
</xsl:if>

<xsl:if test="count(/.//a:SimpleType) > 0">
{\par\b\fs28 Datatypes \par}
<xsl:apply-templates select="a:SimpleType" >
	<xsl:sort select="@name"/>
</xsl:apply-templates>
</xsl:if>

<xsl:if test="count(/.//a:PrimitiveType) > 0">
{\par\b\fs28 Primitive Types \par}
<xsl:apply-templates select="a:PrimitiveType" >
	<xsl:sort select="@name"/>
</xsl:apply-templates>
</xsl:if>
\par}}}}}}}}
</xsl:template>
	
<xsl:template match="a:Message">
{\par\b\fs24 {\*\bkmkstart <xsl:value-of select="@name"/>}<xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/>}{\fs24 {\*\bkmkend <xsl:value-of select="@name"/>}}<xsl:apply-templates mode="annotate-type" /><xsl:apply-templates />
</xsl:template>

<xsl:template match="a:Root">
{\par\b\fs24 {\*\bkmkstart <xsl:value-of select="@name"/>}<xsl:apply-templates select="a:Stereotype"/><xsl:value-of select="@name"/>}{\fs24 {\*\bkmkend <xsl:value-of select="@name"/>}}<xsl:call-template name="type_definition" />
</xsl:template>

<xsl:template name="type_definition"><xsl:if test="a:SuperType">\par\fs20 Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy" /></xsl:if><xsl:apply-templates mode="annotate-type" />
\par\pard\plain \sb120\qj\fs20\lang1033 
<xsl:choose>
<xsl:when test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:Choice|a:SuperType">
<xsl:if test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:Choice">{\par\b\fs20 Native Members\par}
\par
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\fs16\qc\b name \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b mult \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b type \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b description \cell}
\row
}
<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:Choice" />
</xsl:if>
<xsl:if test="a:SuperType">{\par\b\fs20 Inherited Members\par}\par
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\fs16\qc\b name \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b mult \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b type \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b description \cell}
\row
}
<xsl:apply-templates select="a:SuperType" mode="inherited"/>
</xsl:if>
</xsl:when>
<xsl:otherwise><xsl:variable name="baseClass" select="@baseClass"/>
<xsl:if test="not(child::a:Stereotype[contains(., '#concrete')]) and (count(/.//a:Reference[@baseClass=$baseClass]) > 0)">{\i This abstract class is a placeholder for 'By Reference' associations defined within this profile. Such classes have no attributes or associations defined. Rather, 'By Reference' associations of this type reference a corresponding concrete type in external profiles that this one is dependent upon.}</xsl:if>
\par
</xsl:otherwise>
</xsl:choose>	
</xsl:template>

<xsl:template match="a:SuperType" mode="inheritance_hierarchy">{\field{\*\fldinst {\fs20 HYPERLINK  \\l "<xsl:value-of select="@name"/>"}}{\fldrslt {\fs20\ul\cf1 <xsl:value-of select="@name"/>}}}
<xsl:variable name="supertype_name" select="@name"/><xsl:if test="/*/node()[@name = $supertype_name]/a:SuperType"> :: <xsl:apply-templates select="/*/node()[@name = $supertype_name]/a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
</xsl:template>

<xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Compound|a:Domain">
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/> \cell}
{\pard \intbl \sb120\sa120\qc\fs16 <xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'"><item>* </item></xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose> \cell}
{\pard \intbl \sb120\sa120\ql\fs16 {\field{\*\fldinst {\fs16 HYPERLINK  \\l "<xsl:value-of select="@type"/>"}}{\fldrslt {\fs16\ul\cf1 <xsl:value-of select="@type"/>}}} \cell}
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:apply-templates mode="annotate-table-row" /> \cell}
\row
}
</xsl:template>
	
<xsl:template match="a:Simple">
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/> \cell}
{\pard \intbl \sb120\sa120\qc\fs16 <xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose> \cell}
{\pard \intbl \sb120\sa120\ql\fs16 {\field{\*\fldinst {\fs16 HYPERLINK  \\l "<xsl:value-of select="substring-after(@cimDatatype, '#')"/>"}}{\fldrslt {\fs16\ul\cf1 <xsl:value-of select="substring-after(@cimDatatype, '#')"/>}}} \cell}
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:apply-templates mode="annotate-table-row" /> \cell}
\row
}
</xsl:template>

<xsl:template match="a:Choice">
<xsl:variable name="choices">
<xsl:if test="count(a:Instance|a:Reference) > 0">Choices: <xsl:for-each select="a:Instance|a:Reference">{\field{\*\fldinst {\fs16 HYPERLINK  \\l "<xsl:value-of select="@type"/>"}}{\fldrslt {\fs16\ul\cf1 <xsl:value-of select="@type"/>}}}<xsl:if test="position()!=last()"><xsl:value-of select="' or '"/></xsl:if></xsl:for-each></xsl:if>
</xsl:variable>
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/> \cell}
{\pard \intbl \sb120\sa120\qc\fs16 <xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose> \cell}
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:value-of select="$choices"/> \cell}
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:apply-templates mode="annotate-table-row" /> \cell}
\row
}
</xsl:template>

<xsl:template match="a:SuperType" mode="inherited">
	<xsl:apply-templates select="//a:ComplexType[@name=current()/@name]" mode="inherited"/>
	<xsl:apply-templates select="//a:Root[@name=current()/@name]" mode="inherited"/>
</xsl:template>

<xsl:template match="a:ComplexType|a:Root" mode="inherited">
	<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:Choice" mode="inherited"/>
	<xsl:apply-templates select="a:SuperType" mode="inherited"/>
</xsl:template>
	
<xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Compound|a:Domain" mode="inherited">
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/> \cell}
{\pard \intbl \sb120\sa120\qc\fs16 <xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose> \cell}
{\pard \intbl \sb120\sa120\ql\fs16 {\field{\*\fldinst {\fs16 HYPERLINK  \\l "<xsl:value-of select="@type"/>"}}{\fldrslt {\fs16\ul\cf1 <xsl:value-of select="@type"/>}}} \cell}
{\pard \intbl \sb120\sa120\ql\fs16 see {\field{\*\fldinst {\fs16 HYPERLINK  \\l "<xsl:value-of select="../@name"/>"}}{\fldrslt {\fs16\ul\cf1 <xsl:value-of select="../@name"/>}}} \cell}
\row
}
</xsl:template>
	
<xsl:template match="a:Simple" mode="inherited">
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/> \cell}
{\pard \intbl \sb120\sa120\qc\fs16 <xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose> \cell}
{\pard \intbl \sb120\sa120\ql\fs16 {\field{\*\fldinst {\fs16 HYPERLINK  \\l "<xsl:value-of select="substring-after(@cimDatatype, '#')"/>"}}{\fldrslt {\fs16\ul\cf1 substring-after(@cimDatatype, '#')}}} \cell}
{\pard \intbl \sb120\sa120\ql\fs16 see {\field{\*\fldinst {\fs16 HYPERLINK  \\l "<xsl:value-of select="../@name"/>"}}{\fldrslt {\fs16\ul\cf1 <xsl:value-of select="../@name"/>}}} \cell}
\row
}
</xsl:template>

<xsl:template match="a:Choice" mode="inherited">
<xsl:variable name="choices">
<xsl:if test="count(a:Instance|a:Reference) > 0">Choices: <xsl:for-each select="a:Instance|a:Reference">{\field{\*\fldinst {\fs16 HYPERLINK  \\l "<xsl:value-of select="@type"/>"}}{\fldrslt {\fs16\ul\cf1 <xsl:value-of select="@type"/>}}}<xsl:if test="position()!=last()"><xsl:value-of select="' or '"/></xsl:if></xsl:for-each></xsl:if>
</xsl:variable>
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:value-of select="@name"/><xsl:call-template name="process-attribute-stereotypes"/> \cell}
{\pard \intbl \sb120\sa120\qc\fs16 <xsl:value-of select="@minOccurs"/>..<xsl:choose><xsl:when test="@maxOccurs = 'unbounded'">*</xsl:when><xsl:otherwise><xsl:value-of select="@maxOccurs"/></xsl:otherwise></xsl:choose> \cell}
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:value-of select="$choices"/> \cell}
{\pard \intbl \sb120\sa120\ql\fs16 <xsl:apply-templates mode="annotate-table-row" /> \cell}
\row
}
</xsl:template>

<xsl:template match="a:ComplexType">
{\par\b\fs24 {\*\bkmkstart <xsl:value-of select="@name"/>}<xsl:value-of select="@name"/>}{\fs32 {\*\bkmkend <xsl:value-of select="@name"/>}}<xsl:call-template name="type_definition" />
</xsl:template>

<xsl:template match="a:CompoundType">
{\par\b\fs24 {\*\bkmkstart <xsl:value-of select="@name"/>}<xsl:value-of select="@name"/>}{\fs32 {\*\bkmkend <xsl:value-of select="@name"/>}}
<xsl:apply-templates mode="annotate-type" />
\par\pard\plain \sb120\qj\fs20\lang1033 
{\par\b\fs20 Members\par}
\par
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2000
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx2720
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx4125
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\fs16\qc\b name \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b mult \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b type \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b description \cell}
\row
}
<xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound" />
</xsl:template>

<xsl:template match="a:SimpleType">
{\par\b\fs24 {\*\bkmkstart <xsl:value-of select="@name"/>}<xsl:value-of select="@name"/>}{\fs32 {\*\bkmkend <xsl:value-of select="@name"/>} \par}
<xsl:apply-templates mode="annotate" />
\par\pard\plain \sb120\qj\fs20\lang1033 XSD type: <xsl:value-of select="@xstype"/> 
\par\pard\plain \sb120\qj\fs20\lang1033 
</xsl:template>

<xsl:template match="a:PrimitiveType">
{\par\b\fs24 {\*\bkmkstart <xsl:value-of select="@name"/>}<xsl:value-of select="@name"/>}{\fs32 {\*\bkmkend <xsl:value-of select="@name"/>} \par}
<xsl:apply-templates mode="annotate" />
\par\pard\plain \sb120\qj\fs20\lang1033 XSD type: <xsl:value-of select="@xstype"/> 
\par\pard\plain \sb120\qj\fs20\lang1033 
</xsl:template>

<xsl:template match="a:EnumeratedType">
{\par\b\fs24 {\*\bkmkstart <xsl:value-of select="@name"/>}<xsl:value-of select="@name"/>}{\fs32 {\*\bkmkend <xsl:value-of select="@name"/>} \par}
<xsl:apply-templates mode="annotate" />
\par\pard\plain \sb120\qj\fs20\lang1033 
\par
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx3750
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\fs16\qc\b name \cell}
{\pard \intbl \sb120\sa120\fs16\qc\b description \cell}
\row
}
<xsl:apply-templates />
</xsl:template>

<xsl:template match="a:EnumeratedValue">
{
\trowd \trgaph70 
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx3750
\clbrdrt\brdrs\clbrdrl\brdrs\clbrdrb\brdrs\clbrdrr\brdrs
\cellx9500
{\pard \intbl \sb120\sa120\fs16 <xsl:value-of select="@name"/> \cell}
{\pard \intbl \sb120\sa120\fs16 <xsl:apply-templates mode="annotate-table-row" /> \cell}
\row
}
</xsl:template>

<!-- Templates for annotations supporting complex types... -->
<xsl:template match="a:Stereotype">
<xsl:if test="contains(., '#description')"><xsl:value-of select="concat('(', @label, ') ')"/></xsl:if>
</xsl:template>

<xsl:template name="process-attribute-stereotypes">
<xsl:if test="count(a:Stereotype[not(contains(., '#attribute')) and not(contains(., '#byreference'))]) > 0"> (<xsl:for-each select="a:Stereotype[not(contains(., '#attribute')) and not(contains(., '#byreference'))]"><xsl:value-of select="@label"/><xsl:if test="position()!=last()"><xsl:value-of select="', '"/></xsl:if></xsl:for-each>)</xsl:if>
</xsl:template>

<xsl:template match="a:Comment" mode="annotate-type">
{\par\pard\plain \sb120\qj\fs20\lang1033 <xsl:call-template name="replace-non-ascii"><xsl:with-param name="text" select="." /></xsl:call-template>}
</xsl:template>

<xsl:template match="a:Note" mode="annotate-type">
{\par\pard\plain \sb120\qj\fs20\lang1033 <xsl:call-template name="replace-non-ascii"><xsl:with-param name="text" select="." /></xsl:call-template>}
</xsl:template>

<xsl:template match="node()" mode="annotate-type">
</xsl:template>

<!-- Templates for annotations supporting standard table rows... -->
<xsl:template match="a:Comment" mode="annotate-table-row">
{\sb120\sa120 <xsl:call-template name="replace-non-ascii"><xsl:with-param name="text" select="." /></xsl:call-template>}
</xsl:template>

<xsl:template match="a:Note" mode="annotate-table-row">
{\sb120\sa120 <xsl:call-template name="replace-non-ascii"><xsl:with-param name="text" select="." /></xsl:call-template>}
</xsl:template>

<xsl:template match="node()" mode="annotate-table-row">
</xsl:template>

<!-- Default templates for annotations supporting annotations without formatting... -->
<xsl:template match="a:Comment" mode="annotate">
<xsl:call-template name="replace-non-ascii"><xsl:with-param name="text" select="." /></xsl:call-template>
</xsl:template>

<xsl:template match="a:Note" mode="annotate">
<xsl:call-template name="replace-non-ascii"><xsl:with-param name="text" select="." /></xsl:call-template>
</xsl:template>

<xsl:template match="node()" mode="annotate">
</xsl:template>

<xsl:template match="node()">
</xsl:template>

<xsl:template name="replace-non-ascii">
    <xsl:param name="text"/>
    <xsl:variable name="ascii"> !"#$%&amp;'()*+,-./0123456789:;=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~</xsl:variable>
    <xsl:variable name="non-ascii" select="translate($text, $ascii, '')" />
    <xsl:choose>
        <xsl:when test="$non-ascii">
            <xsl:variable name="char" select="substring($non-ascii, 1, 1)" />
            <!-- <xsl:variable name="hex" select="java:java.lang.Integer.toHexString(java:java.lang.Integer.parseInt(java:java.lang.String.valueOf($char)))" /> -->
            <!-- <xsl:variable name="escaped" select="concat('\', $apos, $hex)" /> -->
            <xsl:variable name="escaped" select="concat('\', $apos, 'e2')" />
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