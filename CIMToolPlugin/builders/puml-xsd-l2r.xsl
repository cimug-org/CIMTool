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
	
	<xsl:output indent="no" method="xml" encoding="utf-8" omit-xml-declaration="yes"/>
    <xsl:param name="copyright-single-line" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<!-- All of the following param(s) correspond to PlantUML preferences in the CIMTool preferences screen  -->
	<xsl:param name="docRootClassesColor"/>
	<xsl:param name="docRootClassesFontColor"/>
	<xsl:param name="concreteClassesColor"/>
	<xsl:param name="concreteClassesFontColor"/>
	<xsl:param name="abstractClassesColor"/>
	<xsl:param name="abstractClassesFontColor"/>
	<xsl:param name="enumerationsColor"/>
	<xsl:param name="enumerationsFontColor"/>
	<xsl:param name="cimDatatypesColor"/>
	<xsl:param name="cimDatatypesFontColor"/>
	<xsl:param name="compoundsColor"/>
	<xsl:param name="compoundsFontColor"/>
	<xsl:param name="primitivesColor"/>
	<xsl:param name="primitivesFontColor"/>
	<xsl:param name="errorsColor"/>
	<xsl:param name="errorsFontColor"/>
	<xsl:param name="enableDarkMode" as="xs:boolean"/>
	<xsl:param name="enableShadowing" as="xs:boolean" />
	<xsl:param name="hideEnumerations" as="xs:boolean"/>
	<xsl:param name="hideCIMDatatypes" as="xs:boolean"/>
	<xsl:param name="hideCompounds" as="xs:boolean"/>
	<xsl:param name="hidePrimitives" as="xs:boolean"/>
	<xsl:param name="hideCardinalityForRequiredAttributes" as="xs:boolean"/>

	<xsl:template match="a:Catalog">
		<document>
			<list begin="@startuml" indent="" delim="" end="@enduml">
				<item>left to right direction</item>
				<item>hide empty methods</item>
				<item>skinparam shadowing <xsl:value-of select="if ($enableShadowing) then 'true' else 'false'"/></item>
				<xsl:if test="$enableDarkMode">
					<item>skinparam BackgroundColor #2e2e2e</item>
					<item>skinparam ArrowColor #FFFFFF</item>
					<item>skinparam ArrowFontColor #FFFFFF</item>
					<item>skinparam ArrowThickness 1</item>
				</xsl:if>
				<list begin="{concat('skinparam class ', '&#123;')}" indent="  " delim="" end="{'&#125;'}">
					<xsl:choose>
						<xsl:when test="$enableDarkMode">
							<!-- Concrete classes <<docroot-style>> definition -->
							<item>' Document root elment style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> #3e3e3e</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> #6a6a6a</item>
							<item>FontSize<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> 20</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> #FFFFFF</item>
							<item>AttributeFontSize<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> 20</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> #FFFFFF</item>
							<item>StereotypeFontSize<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> 16</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> #FFFFFF</item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- Concrete classes <<concrete-style>> definition -->
							<item>' Concrete classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;concrete-style&gt;&gt; '"/> #3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;concrete-style&gt;&gt; '"/> #6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;concrete-style&gt;&gt; '"/> #FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;concrete-style&gt;&gt; '"/> #FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;concrete-style&gt;&gt; '"/> #FFFFFF</item>
							<item>HeaderFontColor <xsl:value-of select="'&lt;&lt;concrete-style&gt;&gt; '"/> #FFFFFF</item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- Abstract classes <<abstract-style>> definition -->
							<item>' Abstract classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;abstract-style&gt;&gt; '"/> #3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;abstract-style&gt;&gt; '"/> #6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;abstract-style&gt;&gt; '"/> #FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;abstract-style&gt;&gt; '"/> #FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;abstract-style&gt;&gt; '"/> #FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;abstract-style&gt;&gt; '"/> #FFFFFF</item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- Enumerations <<enumeration-style>> definition -->
							<item>' Enumerations style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;enumeration-style&gt;&gt; '"/> #3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;enumeration-style&gt;&gt; '"/> #6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;enumeration-style&gt;&gt; '"/> #FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;enumeration-style&gt;&gt; '"/> #FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;enumeration-style&gt;&gt; '"/> #FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;enumeration-style&gt;&gt; '"/> #FFFFFF</item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- CIMDatatype classes <<cimdatatype-style>> definition -->
							<item>' CIMDatatypes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;cimdatatype-style&gt;&gt; '"/> #3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;cimdatatype-style&gt;&gt; '"/> #6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;cimdatatype-style&gt;&gt; '"/> #FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;cimdatatype-style&gt;&gt; '"/> #FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;cimdatatype-style&gt;&gt; '"/> #FFFFFF</item>
							<item>HeaderFontColor <xsl:value-of select="'&lt;&lt;cimdatatype-style&gt;&gt; '"/> #FFFFFF</item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->						
							<!-- Compound classes <<compound-style>> definition -->
							<item>' Compound classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;compound-style&gt;&gt; '"/> #3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;compound-style&gt;&gt; '"/> #6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;compound-style&gt;&gt; '"/> #FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;compound-style&gt;&gt; '"/> #FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;compound-style&gt;&gt; '"/> #FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;compound-style&gt;&gt; '"/> #FFFFFF</item>	
							<item>&#xD;&#xA;</item> <!-- CR/LF -->	
							<!-- Primitive classes <<primitive-style>> definition -->
							<item>' Primitive classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;primitive-style&gt;&gt; '"/> #3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;primitive-style&gt;&gt; '"/> #6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;primitive-style&gt;&gt; '"/> #FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;primitive-style&gt;&gt; '"/> #FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;primitive-style&gt;&gt; '"/> #FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;primitive-style&gt;&gt; '"/> #FFFFFF</item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->		
							<!-- Errors classes <<error-style>> definition -->
							<item>' Errors classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsFontColor)"/></item>
						</xsl:when>
						<xsl:otherwise>
							<!-- Concrete classes <<docroot-style>> definition -->
							<item>' Document root elment style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;docroot-style&gt;&gt; ', $docRootClassesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;docroot-style&gt;&gt; ', $docRootClassesFontColor)"/></item>
							<item>FontSize<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> 20</item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;docroot-style&gt;&gt; ', $docRootClassesFontColor)"/></item>
							<item>AttributeFontSize<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> 20</item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;docroot-style&gt;&gt; ', $docRootClassesFontColor)"/></item>
							<item>StereotypeFontSize<xsl:value-of select="'&lt;&lt;docroot-style&gt;&gt; '"/> 16</item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;docroot-style&gt;&gt; ', $docRootClassesFontColor)"/></item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- Concrete classes <<concrete-style>> definition -->
							<item>' Concrete classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;concrete-style&gt;&gt; ', $concreteClassesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;concrete-style&gt;&gt; ', $concreteClassesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;concrete-style&gt;&gt; ', $concreteClassesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;concrete-style&gt;&gt; ', $concreteClassesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;concrete-style&gt;&gt; ', $concreteClassesFontColor)"/></item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- Abstract classes <<abstract-style>> definition -->
							<item>' Abstract classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;abstract-style&gt;&gt; ', $abstractClassesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;abstract-style&gt;&gt; ', $abstractClassesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;abstract-style&gt;&gt; ', $abstractClassesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;abstract-style&gt;&gt; ', $abstractClassesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;abstract-style&gt;&gt; ', $abstractClassesFontColor)"/></item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- Enumerations <<enumeration-style>> definition -->
							<item>' Enumerations style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;enumeration-style&gt;&gt; ', $enumerationsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;enumeration-style&gt;&gt; ', $enumerationsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;enumeration-style&gt;&gt; ', $enumerationsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;enumeration-style&gt;&gt; ', $enumerationsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat(' &lt;&lt;enumeration-style&gt;&gt; ', $enumerationsFontColor)"/></item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- CIMDatatype classes <<cimdatatype-style>> definition -->
							<item>' CIMDatatypes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;cimdatatype-style&gt;&gt; ', $cimDatatypesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;cimdatatype-style&gt;&gt; ', $cimDatatypesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;cimdatatype-style&gt;&gt; ', $cimDatatypesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;cimdatatype-style&gt;&gt; ', $cimDatatypesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;cimdatatype-style&gt;&gt; ', $cimDatatypesFontColor)"/></item>	
							<item>&#xD;&#xA;</item> <!-- CR/LF -->				
							<!-- Compound classes <<compound-style>> definition -->
							<item>' Compound classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;compound-style&gt;&gt; ', $compoundsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;compound-style&gt;&gt; ', $compoundsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;compound-style&gt;&gt; ', $compoundsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;compound-style&gt;&gt; ', $compoundsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;compound-style&gt;&gt; ', $compoundsFontColor)"/></item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->
							<!-- Primitive classes <<primitive-style>> definition -->
							<item>' Primitive classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;primitive-style&gt;&gt; ', $primitivesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;primitive-style&gt;&gt; ', $primitivesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;primitive-style&gt;&gt; ', $primitivesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;primitive-style&gt;&gt; ', $primitivesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;primitive-style&gt;&gt; ', $primitivesFontColor)"/></item>
							<item>&#xD;&#xA;</item> <!-- CR/LF -->		
							<!-- Errors classes <<error-style>> definition -->
							<item>' Errors classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;error-style&gt;&gt; ', $errorsFontColor)"/></item>
						</xsl:otherwise>
					</xsl:choose>
				</list>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<item><xsl:value-of select="'hide &lt;&lt;docroot-style&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;concrete-style&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;abstract-style&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;enumeration-style&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;cimdatatype-style&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;compound-style&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;primitive-style&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;error-style&gt;&gt; stereotype'"/></item>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<xsl:if test="not(@hideInDiagrams = 'true')">
				<list begin="" indent="" delim="" end="">
					<item>' <xsl:value-of select="@name"/></item>
					<list begin="{concat('class ', @name, ' &lt;&lt;docroot-style&gt;&gt; &lt;&lt;Document Root&gt;&gt; &lt;&lt;(R, #F3F3F3)&gt;&gt;', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
						<item><xsl:value-of select="$baseURI"/></item>
					</list>			
						<xsl:for-each select="a:Root">
							<!-- Output the association -->	
							<xsl:variable name="targetRoleEndName" select="concat('+', @name)"/>
							<xsl:variable name="targetCardinality">
								<xsl:choose>
									<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:call-template name="association-cardinality"/></xsl:when>
								</xsl:choose>
							</xsl:variable>
							<item><xsl:value-of select="concat($envelope, ' --&gt;  &quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', @name)"/></item>
						</xsl:for-each>		
					</list>
					<item>&#xD;&#xA;</item> <!-- CR/LF -->
					<list begin="{concat('skinparam note ', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
						<item>FontSize 14</item>
						<item>Font Bold</item>
					</list>
					<xsl:if test="a:Note and a:Note[string-length(.) > 0]">
						<item>&#xD;&#xA;</item> <!-- CR/LF -->
						<item>' Add a note towards the upper left corner of the diagram</item>
						<list begin="note as NoteInfo {$docRootClassesColor}" indent="   " delim="" end="end note">
								<item>Profile Notes:</item>
								<xsl:apply-templates select="a:Note" mode="profile-notes"/>
						</list>
					</xsl:if>
					<item>&#xD;&#xA;</item> <!-- CR/LF -->
				</xsl:if>
				<xsl:apply-templates select="a:Root|a:ComplexType|a:EnumeratedType|a:CompoundType|a:SimpleType|a:PrimitiveType"/>			
			</list>
		</document>
	</xsl:template>

	<xsl:template match="a:EnumeratedType">
		<xsl:if test="not($hideEnumerations) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="enumName" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="count" select="count(a:EnumeratedValue)"/>
			<list begin="" indent="" delim="" end="">
				<item>' Enumeration <xsl:value-of select="$enumName"/></item>
				<list begin="enum {concat($enumName, ' ', ' &lt;&lt;enumeration-style&gt;&gt;', $stereotypes, ' ', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
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
	
	<xsl:template match="a:SimpleType">
		<xsl:if test="not($hideCIMDatatypes) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@dataType, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="colorStyle" select="'&lt;&lt;cimdatatype-style&gt;&gt;'"/>
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="$className"/></item>
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' ', $colorStyle, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:apply-templates select="a:Value|a:Unit|a:Multiplier"/>
						</xsl:when>
						<xsl:otherwise>
							<item>[Attributes hidden]</item>
						</xsl:otherwise>
					</xsl:choose>							
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:PrimitiveType">
		<xsl:if test="not($hidePrimitives) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@dataType, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="colorStyle" select="'&lt;&lt;primitive-style&gt;&gt;'"/>
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="$className"/></item>
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' ', $colorStyle, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">		
				</list>
			</list>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:CompoundType">
		<xsl:if test="not($hideCompounds) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="colorStyle" select="'&lt;&lt;compound-style&gt;&gt;'"/>
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="$className"/></item>
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' ', $colorStyle, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:apply-templates select="a:Complex|a:Enumerated|a:SimpleEnumerated|a:Simple|a:Domain|a:Instance[a:Stereotype[contains(., '#compound')]]"/>
						</xsl:when>
						<xsl:otherwise>
							<item>[Attributes hidden]</item>
						</xsl:otherwise>
					</xsl:choose>							
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Root|a:ComplexType">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="colorStyle">
				<xsl:choose>
					<xsl:when test="a:Stereotype[contains(., '#compound')]"><xsl:value-of select="'&lt;&lt;compound-style&gt;&gt;'"/></xsl:when>
					<xsl:when test="not(a:Stereotype[contains(., '#concrete')])"><xsl:value-of select="'&lt;&lt;abstract-style&gt;&gt;'"/></xsl:when>
					<xsl:otherwise><xsl:value-of select="'&lt;&lt;concrete-style&gt;&gt;'"/></xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="a:SuperType">
					<xsl:variable name="superClassName" select="substring-after(a:SuperType/@baseClass, '#')"/>
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="$className"/> inherits from <xsl:value-of select="$superClassName"/></item>
						<list begin="{concat(if (not(a:Stereotype[@label='Concrete'])) then 'abstract' else 'class', ' ', $className, ' ', $stereotypes, ' ',$colorStyle, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Complex|a:Enumerated|a:SimpleEnumerated|a:Simple|a:Domain|a:Choice|a:Instance[a:Stereotype[contains(., '#compound')]]"/>
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
							<xsl:when test="a:Reference|a:Instance[not(a:Stereotype[contains(., '#compound')])]">
								<xsl:apply-templates select="a:Reference|a:Instance[not(a:Stereotype[contains(., '#compound')])]" mode="associations"/>
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
						<list begin="{concat(if (not(a:Stereotype[@label='Concrete'])) then 'abstract class' else 'class', ' ', $className, ' ', $stereotypes, ' ', $colorStyle, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Complex|a:Enumerated|a:SimpleEnumerated|a:Simple|a:Domain|a:Choice|a:Instance[a:Stereotype[contains(., '#compound')]]"/>
								</xsl:when>
								<xsl:otherwise>
									<item>[Attributes hidden]</item>
								</xsl:otherwise>
							</xsl:choose>							
						</list>
						<!-- Now process all associations: -->
						<xsl:choose>
							<xsl:when test="a:Reference|a:Instance[not(a:Stereotype[contains(., '#compound')])]">
								<xsl:apply-templates select="a:Reference|a:Instance[not(a:Stereotype[contains(., '#compound')])]" mode="associations"/>
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

	<xsl:template match="a:Reference|a:Instance" mode="associations">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<xsl:variable name="sourceClass">
				<xsl:value-of select="substring-after(@inverseBasePropertyClass, '#')"/>
			</xsl:variable>
			<xsl:variable name="targetClass"><xsl:value-of select="@type"/></xsl:variable>
			<xsl:variable name="sourceRoleEndName">
				<xsl:value-of select="concat('+', @name)"/>
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
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'aggregateOf']">o--&gt;</xsl:when>
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'compositOf']">*--&gt;</xsl:when>
					<xsl:otherwise>--&gt;</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>

			<!-- Output the association -->	
			<item><xsl:value-of select="concat($sourceClass, ' ', $associationType, ' &quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', $targetClass)"/><xsl:if test="a:Stereotype[contains(., '#enumeration')] or a:Stereotype[contains(., '#compound')] or a:Stereotype[contains(., '#cimdatatype')] or a:Stereotype[contains(., '#primitive')] or (self::a:Reference and not(a:Stereotype[contains(., '#byreference')]))"><xsl:value-of select="if ($enableDarkMode) then '#FF2D2D' else '#red'"/></xsl:if></item>
			
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
					<list begin="{concat('abstract class ', $targetClass, ' &lt;&lt;error-style&gt;&gt; ', (if (not($stereotype = null)) then $stereotype else ''), $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
						<item>' nothing to generate</item>
					</list>
				</list>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, and Enumerated attributes templates)                                        -->
	<!-- ============================================================================================================ -->
	<xsl:template match="a:Simple">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="@xstype"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Domain">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@dataType, '#')"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Enumerated">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="@type"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<!-- Compounds are treated like normal attributes -->
	<xsl:template match="a:Instance">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@baseClass, '#')"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Value|a:Unit|a:Multiplier">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant, ' {readOnly}') else ''"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@baseClass, '#')"/> <xsl:call-template name="cardinality"/><xsl:value-of select="$constant"/></item>
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- END SECTION:  (Simple, Domain, and Enumerated attributes templates)                           -->
	<!-- ============================================================================================================ -->

	<xsl:template name="stereotypes">
		<xsl:if test="a:Stereotype">
			<xsl:variable name="stereotypes">
				<xsl:for-each select="a:Stereotype">
					<xsl:variable name="currentStereotype" select="."/>
					<xsl:variable name="stereotype" select="substring-after(., '#')"/>
					<xsl:choose>
						<!-- Below is the set of stereotypes that are internal metadata. These we do not display on a class... -->
						<xsl:when test="not(($stereotype = 'byreference') or ($stereotype = 'concrete'))">
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
	
	<xsl:template name="attribute-stereotypes">
		<xsl:if test="a:Stereotype">
			<xsl:variable name="stereotypes">
				<xsl:for-each select="a:Stereotype">
					<xsl:variable name="currentStereotype" select="."/>
					<xsl:variable name="stereotype" select="substring-after(., '#')"/>
					<xsl:choose>
						<!-- Below is the set of stereotypes that are internal metadata. These we do not display on an attribute or association -->
						<xsl:when test="not(($stereotype = 'enumeration') or ($stereotype = 'compound') or ($stereotype = 'attribute') or ($stereotype = 'byreference') or ($stereotype = 'enum') or ($stereotype = 'concrete') or ($stereotype = 'ofAggregate') or ($stereotype = 'aggregateOf') or ($stereotype = 'ofComposite') or ($stereotype = 'compositeOf'))">
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
	<xsl:template match="a:Note" mode="profile-notes">
		<!-- Remove double quotes to eliminate broken comments, etc. -->
		<xsl:variable name="paragraph" select="translate(., '&quot;', '')"/>
		<list begin="" indent="   " delim="&#xD;" end="">
			<xsl:call-template name="parse-notes">
				<xsl:with-param name="notes" select="$paragraph"/>
			</xsl:call-template>
		</list>
	</xsl:template>

	<xsl:template name="parse-notes">
		<xsl:param name="notes" />
		<xsl:choose>
			<xsl:when test="(string-length($notes) &lt;= 80)">
				<item><xsl:value-of select="$notes"/></item>
				<item>&#xD;</item>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="cutPos" select="string-length(substring($notes, 1, 80)) + string-length(substring-before(substring($notes, 81), ' ')) + 1"/>
				<item><xsl:value-of select="substring($notes, 1, $cutPos)"/></item>
				<xsl:call-template name="parse-notes">
					<xsl:with-param name="notes" select="substring($notes, $cutPos + 1)"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template> 
	
	<xsl:template match="text()">
		<!--  dont pass text through  -->
	</xsl:template>

</xsl:stylesheet>