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
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:map="http://www.w3.org/2005/xpath-functions/map" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:a="http://langdale.com.au/2005/Message#" xmlns="http://langdale.com.au/2009/Indent" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:cimtool="http://cimtool.ucaiug.io/functions">
	
	<xsl:output indent="no" method="xml" encoding="UTF-8" omit-xml-declaration="yes"/>
    <xsl:param name="copyright-single-line" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	
	<!-- All of the following params correspond to PlantUML preferences in the CIMTool preferences -->
	<!-- screen. They are passed in via a single string parameter $builderParameters that consist   -->
	<!-- of parameter name/value pairs that are delimited using the pipe character ("|"). This       -->
	<!-- allows for more dynamically adding parameters within the core CIMTool codebase.  -->
	
	<xsl:param name="builderParameters"/>
	
	<!-- XSLT 3.0 map for parameter parsing - simplified syntax -->
	<xsl:variable name="paramMap" as="map(xs:string, xs:string)">
		<xsl:map>
			<xsl:for-each select="fn:tokenize($builderParameters, '\|')[normalize-space()]">
				<xsl:variable name="parts" select="tokenize(., '=')"/>
				<xsl:map-entry key="$parts[1]" select="fn:string-join(fn:subsequence($parts, 2), '=')"/>
			</xsl:for-each>
		</xsl:map>
	</xsl:variable>
	
	<!-- Extract parameters using XSLT 3.0 map functions -->
	<xsl:param name="anonymousCompoundsColor" select="map:get($paramMap, 'anonymousCompoundsColor')"/>
	<xsl:param name="anonymousEnumerationsColor" select="map:get($paramMap, 'anonymousEnumerationsColor')"/>
	<xsl:param name="anonymousComplexTypesColor" select="map:get($paramMap, 'anonymousComplexTypesColor')"/>
	<xsl:param name="abstractClassesColor" select="map:get($paramMap, 'abstractClassesColor')"/>
	<xsl:param name="abstractClassesFontColor" select="map:get($paramMap, 'abstractClassesFontColor')"/>
	<xsl:param name="cimDatatypesColor" select="map:get($paramMap, 'cimDatatypesColor')"/>
	<xsl:param name="cimDatatypesFontColor" select="map:get($paramMap, 'cimDatatypesFontColor')"/>
	<xsl:param name="compoundsColor" select="map:get($paramMap, 'compoundsColor')"/>
	<xsl:param name="compoundsFontColor" select="map:get($paramMap, 'compoundsFontColor')"/>
	<xsl:param name="concreteClassesColor" select="map:get($paramMap, 'concreteClassesColor')"/>
	<xsl:param name="concreteClassesFontColor" select="map:get($paramMap, 'concreteClassesFontColor')"/>
	<xsl:param name="docRootClassesColor" select="map:get($paramMap, 'docRootClassesColor')"/>
	<xsl:param name="docRootClassesFontColor" select="map:get($paramMap, 'docRootClassesFontColor')"/>
	<xsl:param name="enumerationsColor" select="map:get($paramMap, 'enumerationsColor')"/>
	<xsl:param name="enumerationsFontColor" select="map:get($paramMap, 'enumerationsFontColor')"/>
	<xsl:param name="primitivesColor" select="map:get($paramMap, 'primitivesColor')"/>
	<xsl:param name="primitivesFontColor" select="map:get($paramMap, 'primitivesFontColor')"/>
	<xsl:param name="choicesColor" select="map:get($paramMap, 'choicesColor')"/>
	<xsl:param name="choicesFontColor" select="map:get($paramMap, 'choicesFontColor')"/>
	<xsl:param name="refsColor" select="map:get($paramMap, 'refsColor')"/>
	<xsl:param name="refsFontColor" select="map:get($paramMap, 'refsFontColor')"/>
	<xsl:param name="errorsColor" select="map:get($paramMap, 'errorsColor')"/>
	<xsl:param name="errorsFontColor" select="map:get($paramMap, 'errorsFontColor')"/>
	<xsl:param name="plantUMLTheme" select="map:get($paramMap, 'plantUMLTheme')"/>
	<xsl:param name="horizontalSpacing" select="map:get($paramMap, 'horizontalSpacing')"/>
	<xsl:param name="verticalSpacing" select="map:get($paramMap, 'verticalSpacing')"/>
	
	<!-- Boolean parameters with XSLT 3.0 safe handling -->
	<xsl:param name="setAnonymousClassesColorWhite" as="xs:boolean" select="map:get($paramMap, 'setAnonymousClassesColorWhite') = 'true'"/>
	<xsl:param name="enableDarkMode" as="xs:boolean" select="map:get($paramMap, 'enableDarkMode') = 'true'"/>
	<xsl:param name="enableShadowing" as="xs:boolean" select="map:get($paramMap, 'enableShadowing') = 'true'"/>
	<xsl:param name="hideCardinalityForRequiredAttributes" as="xs:boolean" select="map:get($paramMap, 'hideCardinalityForRequiredAttributes') = 'true'"/>
	<xsl:param name="hideCIMDatatypes" as="xs:boolean" select="map:get($paramMap, 'hideCIMDatatypes') = 'true'"/>
	<xsl:param name="hideCompounds" as="xs:boolean" select="map:get($paramMap, 'hideCompounds') = 'true'"/>
	<xsl:param name="hideEnumerations" as="xs:boolean" select="map:get($paramMap, 'hideEnumerations') = 'true'"/>
	<xsl:param name="hidePrimitives" as="xs:boolean" select="map:get($paramMap, 'hidePrimitives') = 'true'"/>
	
	<xsl:template match="a:Catalog">
		<document>
			<list begin="@startuml" indent="" delim="" end="@enduml">
				<item></item>
				<item>sprite $choice_icon [32x32/16] {</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000FFFFF0000000000000</item>
				<item>00000000000000FFFFF0000000000000</item>
				<item>0000000000A000FFFFF00FFFFF000000</item>
				<item>000000000AFA00FFFFF000000F000000</item>
				<item>00000000AFA000FFFFF000000F000000</item>
				<item>0000000AFA000000000000000F000000</item>
				<item>000000AFA00000FFFFF000000F000000</item>
				<item>00000AFA000000FFFFF000000F000000</item>
				<item>0FFFFFA0000000FFFFF00FFFFFFFFFF0</item>
				<item>00000000000000FFFFF000000F000000</item>
				<item>00000000000000FFFFF000000F000000</item>
				<item>0000000000000000000000000F000000</item>
				<item>00000000000000FFFFF000000F000000</item>
				<item>00000000000000FFFFF000000F000000</item>
				<item>00000000000000FFFFF00FFFFF000000</item>
				<item>00000000000000FFFFF0000000000000</item>
				<item>00000000000000FFFFF0000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>00000000000000000000000000000000</item>
				<item>}</item>
				<item></item>
				<!-- Defines a 1×1 transparent sprite (used to replace the default icons on Ref classes) -->
				<item>sprite $ref_icon {</item>
				<item>0</item>
				<item>}</item>
				<item></item>
				<item>sprite $empty_icon [16x16/16] {</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>0000000000000000</item>
				<item>}</item>
				<item></item>
				<item>left to right direction</item>
				<item>hide empty methods</item>
				<item></item>
				<xsl:if test="$horizontalSpacing and $horizontalSpacing != ''">
					<item>' Adjust the horizontal spacing for better spatial rendering (the PlantUML default is ~20)</item>
					<item>skinparam nodesep <xsl:value-of select="$horizontalSpacing"/></item>
				</xsl:if>
				<xsl:if test="$verticalSpacing and $verticalSpacing != ''">
					<item>' Adjust the vertical spacing for better spatial rendering (the PlantUML default is ~30)</item>
					<item>skinparam ranksep <xsl:value-of select="$verticalSpacing"/></item>
				</xsl:if>
				<item></item>
				<xsl:choose>
					<xsl:when test="$enableDarkMode">
						<item>skinparam BackgroundColor #2e2e2e</item>
						<item>skinparam ArrowColor #FFFFFF</item>
						<item>skinparam ArrowFontColor #FFFFFF</item>
						<item>skinparam ArrowThickness 1</item>
						<list begin="{concat('skinparam class ', '&#123;')}" indent="  " delim="" end="{'&#125;'}">
							<!-- Document Root definition -->
							<item>' Document Root element style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>#3e3e3e</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>#6a6a6a</item>
							<item>FontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> 18</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>#FFFFFF</item>
							<item>AttributeFontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>18</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>#FFFFFF</item>
							<item>StereotypeFontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>14</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>#FFFFFF</item>
							<item></item>
							<!-- Choice definition -->
							<item>' Choice element style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/>#3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/>#6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/>#FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/>#FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/>#FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/>#FFFFFF</item>
							<item></item>
							<!-- Ref definition -->
							<item>' Ref element style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Ref&gt;&gt; '"/>#3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;Ref&gt;&gt; '"/>#6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;Ref&gt;&gt; '"/>#FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Ref&gt;&gt; '"/>#FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Ref&gt;&gt; '"/>#FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Ref&gt;&gt; '"/>#FFFFFF</item>
							<item></item>
							<!-- Concrete classes definition -->
							<item>' Concrete classes style definition</item>
							<item>BackgroundColor #3e3e3e</item>
							<item>BorderColor #6a6a6a</item>
							<item>FontColor #FFFFFF</item>
							<item>AttributeFontColor #FFFFFF</item>
							<item>StereotypeFontColor #FFFFFF</item>
							<item>HeaderFontColor  #FFFFFF</item>
							<item></item>
							<!-- Abstract classes <<abstract>> definition -->
							<item>' Abstract classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/>#3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/>#6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/>#FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/>#FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/>#FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/>#FFFFFF</item>
							<item></item>
							<!-- Enumerations <<enumeration>> definition -->
							<item>' Enumerations style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/>#3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/>#6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/>#FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/>#FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/>#FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/>#FFFFFF</item>
							<item></item>
							<!-- CIMDatatype classes <<CIMDatatype>> definition -->
							<item>' CIMDatatypes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/>#3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/>#6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/>#FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/>#FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/>#FFFFFF</item>
							<item>HeaderFontColor <xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/>#FFFFFF</item>
							<item></item>						
							<!-- Compound classes <<Compound>> definition -->
							<item>' Compound classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/>#3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/>#6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/>#FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/>#FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/>#FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/>#FFFFFF</item>	
							<item></item>	
							<!-- Primitive classes <<Primitive>> definition -->
							<item>' Primitive classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/>#3e3e3e</item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/>#6a6a6a</item>
							<item>FontColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/>#FFFFFF</item>
							<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/>#FFFFFF</item>
							<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/>#FFFFFF</item>
							<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/>#FFFFFF</item>
							<item></item>		
							<!-- Errors classes <<error>> definition -->
							<item>' Errors classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
						</list>
					</xsl:when>
					<xsl:when test="$plantUMLTheme and not($plantUMLTheme = '') and not($plantUMLTheme = '_none_')">
						<item><xsl:value-of select="concat('!theme ', $plantUMLTheme)"/></item>
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
							<!-- Document Root definition -->
							<item>' Document root element style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesFontColor)"/></item>
							<item>FontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>18</item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesFontColor)"/></item>
							<item>AttributeFontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>18</item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesFontColor)"/></item>
							<item>StereotypeFontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/>14</item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesFontColor)"/></item>
							<item></item>
							<!-- Choice definition -->
							<item>' Choice element style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', $choicesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', $choicesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', $choicesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', $choicesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', $choicesFontColor)"/></item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/>#454645</item>
							<item>BorderThickness<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/>1</item>
							<item></item>
							<!-- Ref definition -->
							<item>' Ref element style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Ref&gt;&gt; ', $refsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;Ref&gt;&gt; ', $refsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Ref&gt;&gt; ', $refsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Ref&gt;&gt; ', $refsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Ref&gt;&gt; ', $refsFontColor)"/></item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;Ref&gt;&gt; '"/>#454645</item>
							<item>BorderThickness<xsl:value-of select="'&lt;&lt;Ref&gt;&gt; '"/>1</item>
							<item></item>
							<!-- Concrete classes definition -->
							<item>' Concrete classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat(' ', $concreteClassesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat(' ', $concreteClassesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat(' ', $concreteClassesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat(' ', $concreteClassesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat(' ', $concreteClassesFontColor)"/></item>
							<item>BorderColor #454645</item>
							<item>BorderThickness 1</item>
							<item></item>
							<!-- Abstract classes <<abstract>> definition -->
							<item>' Abstract classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;abstract&gt;&gt; ', $abstractClassesFontColor)"/></item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/>#454645</item>
							<item>BorderThickness<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> 1</item>
							<item></item>
							<!-- Enumerations <<enumeration>> definition -->
							<item>' Enumerations style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;enumeration&gt;&gt; ', $enumerationsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;enumeration&gt;&gt; ', $enumerationsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;enumeration&gt;&gt; ', $enumerationsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;enumeration&gt;&gt; ', $enumerationsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat(' &lt;&lt;enumeration&gt;&gt; ', $enumerationsFontColor)"/></item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/>#454645</item>
							<item>BorderThickness<xsl:value-of select="'&lt;&lt;enumeration&gt;&gt; '"/> 1</item>
							<item></item>
							<!-- CIMDatatype classes <<CIMDatatype>> definition -->
							<item>' CIMDatatypes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;CIMDatatype&gt;&gt; ', $cimDatatypesFontColor)"/></item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/>#454645</item>
							<item>BorderThickness<xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt; '"/> 1</item>
							<item></item>				
							<!-- Compound classes <<Compound>> definition -->
							<item>' Compound classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Compound&gt;&gt; ', $compoundsFontColor)"/></item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/>#454645</item>
							<item>BorderThickness<xsl:value-of select="'&lt;&lt;Compound&gt;&gt; '"/> 1</item>
							<item></item>
							<!-- Primitive classes <<Primitive>> definition -->
							<item>' Primitive classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Primitive&gt;&gt; ', $primitivesFontColor)"/></item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/>#454645</item>
							<item>BorderThickness<xsl:value-of select="'&lt;&lt;Primitive&gt;&gt; '"/> 1</item>
							<item></item>		
							<!-- Errors classes <<error>> definition -->
							<item>' Errors classes style definition</item>
							<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsColor)"/></item>
							<item>FontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
							<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
							<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
							<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;error&gt;&gt; ', $errorsFontColor)"/></item>
							<item>BorderColor<xsl:value-of select="'&lt;&lt;error&gt;&gt; '"/>#454645</item>
							<item>BorderThickness<xsl:value-of select="'&lt;&lt;error&gt;&gt; '"/> 1</item>
						</list>
					</xsl:otherwise>
				</xsl:choose>
				<item></item>
				<item>skinparam shadowing <xsl:value-of select="if ($enableShadowing) then 'true' else 'false'"/></item>
				<item></item>
				<item><xsl:value-of select="'hide &lt;&lt;Document Root&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;Choice&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;Ref&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;abstract&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;error&gt;&gt; stereotype'"/></item>
				<item></item>
				<xsl:if test="not(@hideInDiagrams = 'true')">
					<!-- 
						We filter out characters in a class or note name not allowed by PlantUML. This was needed
						after discovering scenarios where end-users had profile names (i.e. $envelope) with dashes 
						(-) in them leading to have to parse out such character.
					-->
					<xsl:variable name="notename" select="concat(cimtool:puml-safe-name($envelope), 'Note')" />
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
						<!-- For now we have opted to not display profile notes in the PlantUML diagrams -->
						<!--
						<xsl:if test="a:Note and a:Note[string-length(.) > 0]">
							<item></item> 
							<item>Profile Notes:</item>
							<xsl:apply-templates select="a:Note" mode="profile-notes"/>
						</xsl:if>
						-->
					</list>
					<item></item>
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
				<list begin="enum {concat($enumName, ' ', $stereotypes, ' ', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:for-each select="a:EnumeratedValue[position() &lt;= 20]">
						<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
						<item><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="substring-after(substring-after(@baseResource, '#'), '.')" /></item>
					</xsl:for-each>
					<xsl:if test="$count > 20">
						<item>[Remaining <xsl:value-of select="$count - 20"/> literals hidden]</item>
					</xsl:if>
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<!--
	  Function: cimtool:puml-safe-name
	  Purpose: Produce a unique, PlantUML-safe name for the specified element.
	  Parameters:
		$name - the name of the class to
	  Returns:
		xs:string - the PlantUML safe class name
	  Algorithm:
		1) Process the name passed in and normalize it to contain only PlantUML safe characters.
	-->
	<xsl:function name="cimtool:puml-safe-name" as="xs:string">
		<xsl:param name="name" as="xs:string"/>
		<xsl:variable name="result" select="replace($name, '[\s:.,\[\]{}\(\)&lt;&gt;\-\+#\*=]', '_')"/>
		<xsl:sequence select="$result"/>
	</xsl:function>
	
	<xsl:template match="a:SimpleType">
		<xsl:if test="not($hideCIMDatatypes) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@dataType, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<list begin="" indent="" delim="" end="">
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:apply-templates select="a:Simple|a:Enumerated"/>
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
			<list begin="" indent="" delim="" end="">
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">		
				</list>
			</list>
			<item>hide "<xsl:value-of select="$className"/>" attributes</item>
			<item></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:CompoundType">
		<xsl:if test="not($hideCompounds) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="$className"/></item>
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
						</xsl:when>
						<xsl:otherwise>
							<item>[Attributes hidden]</item>
						</xsl:otherwise>
					</xsl:choose>							
				</list>
				<!-- Now process all associations: -->
				<xsl:choose>
					<xsl:when test="a:Reference|a:Instance">
						<xsl:apply-templates select="a:Reference|a:Instance"/>
						<item></item>
					</xsl:when>
					<xsl:otherwise>
						<item></item>
					</xsl:otherwise>
				</xsl:choose>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Root|a:ComplexType">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<!-- ================================================================================================================= -->
			<!-- This next set of variables is purely for determining whether a:ComplexType (i.e. an abstract class) should be     -->
			<!-- tagged with the <<error>> stereotype.                                                                             -->			
			<xsl:variable name="currentClass" select="@baseClass"/>
			<xsl:variable name="fieldsCount">
				<xsl:call-template name="count_fields">
					<xsl:with-param name="count" select="0"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:variable name="superCount" select="count(a:SuperType)"/>
			<xsl:variable name="subCount" select="count(//a:Root[a:SuperType[@baseClass = $currentClass]]|//a:ComplexType[a:SuperType[@baseClass = $currentClass]])"/>
			<xsl:variable name="refsCount" select="count(//a:Root[@baseClass != $currentClass and (a:Instance|a:Reference)[@baseClass = $currentClass]]|//a:ComplexType[@baseClass != $currentClass and (a:Instance|a:Reference)[@baseClass = $currentClass]])"/>
			<xsl:variable name="error" select="if ((self::a:ComplexType and (($subCount = 0 and $superCount = 0 and $refsCount > 0 and $fieldsCount > 0) or ($subCount = 0 and $superCount = 0 and $refsCount = 0) or ($subCount = 0 and $superCount > 0 and $refsCount = 0))) or (self::a:Root and ($subCount = 0 and $superCount = 0 and $refsCount > 0 and $fieldsCount = 0))) then true() else false()" />
			<!-- ================================================================================================================= -->
			<xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:choose>
				<xsl:when test="a:SuperType">
					<xsl:variable name="superClassName" select="substring-after(a:SuperType/@baseClass, '#')"/>
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="$className"/> inherits from <xsl:value-of select="$superClassName"/></item>
						<list begin="{concat(if (not(a:Stereotype[contains(., '#concrete')])) then 'abstract class' else 'class', ' ', $className, if ($error) then ' &lt;&lt;error&gt;&gt; ' else ' ', $stereotypes, ' ', if (not(a:Stereotype[contains(., '#concrete')])) then '&lt;&lt;abstract&gt;&gt;' else '', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
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
								<xsl:apply-templates select="a:Reference|a:Instance"/>
								<item></item>
							</xsl:when>
							<xsl:otherwise>
								<item></item>
							</xsl:otherwise>
						</xsl:choose>
					</list>
				</xsl:when>
				<xsl:otherwise>
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="$className"/></item>
						<list begin="{concat(if (not(a:Stereotype[contains(., '#concrete')])) then 'abstract class' else 'class', ' ', $className, if ($error) then ' &lt;&lt;error&gt;&gt; ' else ' ', $stereotypes, ' ', if (not(a:Stereotype[contains(., '#concrete')])) then '&lt;&lt;abstract&gt;&gt;' else '', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
								</xsl:when>
								<xsl:otherwise>
									<item>[Attributes hidden]</item>
								</xsl:otherwise>
							</xsl:choose>							
						</list>
						<!-- Now process all associations: -->
						<xsl:choose>
							<xsl:when test="a:Reference|a:Instance">
								<xsl:apply-templates select="a:Reference|a:Instance"/>
								<item></item>
							</xsl:when>
							<xsl:otherwise>
								<item></item>
							</xsl:otherwise>
						</xsl:choose>
					</list>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:if test="$error">
				<item></item>
				<item>note as Error<xsl:value-of select="$className"/></item>
				<xsl:choose>
					<xsl:when test="self::a:Root and ($subCount = 0 and $superCount = 0 and $refsCount > 0 and $fieldsCount = 0)">
						<item>Class <xsl:value-of select="$className"/> is 'concrete' but has no fields or associations </item>
						<item>declared. If it is intended to serve as a reference to an external </item>
						<item>object then it should be declared as 'abstract'. Otherwise, it should </item>
						<item>have fields or associations declared or alternately inherit from a </item>
						<item>parent class that does.</item>
					</xsl:when>
					<xsl:when test="self::a:ComplexType and ($subCount = 0 and $superCount = 0 and $refsCount = 0)">
						<item>Class <xsl:value-of select="$className"/> is 'abstract' and has no parent class,</item>
						<item>child class, or association references to it. It should either be </item>
						<item>removed from the profile or declared as 'concrete'.</item>
					</xsl:when>
					<xsl:when test="self::a:ComplexType and ($subCount = 0 and $superCount > 0 and $refsCount = 0)">
						<item>Class <xsl:value-of select="$className"/> is 'abstract' and has no child classes. </item>
						<item>It should either be removed, declared as 'concrete', or have a </item>
						<item>child class that is declared as 'concrete'.</item>
					</xsl:when>
					<xsl:when test="self::a:ComplexType and ($subCount = 0 and $superCount = 0 and $refsCount > 0 and $fieldsCount > 0)">
						<item>Class <xsl:value-of select="$className"/> is 'abstract'. If it is serving as</item>
						<item>a reference to an external object then no fields should be defined in </item>
						<item>it. Otherwise, it should be declared as 'concrete'.</item>
					</xsl:when>
				</xsl:choose>
				<item>end note</item>
				<item></item>
				<item>Error<xsl:value-of select="$className"/> ..> <xsl:value-of select="$className"/> #red</item>
				<item></item>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Reference|a:Instance">
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
			<!-- Determine if the association is either an aggregate, a composite, or a standard association: -->
			<xsl:variable name="associationType">
				<xsl:choose>
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
				 not yet been pulled into the profile and therefore should be flagged as an error (i.e. expressed as class in light red) -->
			<xsl:if test="not(//a:ComplexType[@name = $targetClass]|//a:Root[@name = $targetClass]|//a:CompoundType[@name = $targetClass]|//a:EnumeratedType[@name = $targetClass]|//a:PrimitiveType[@name = $targetClass])">
				<xsl:variable name="stereotype">
					<xsl:choose>
						<xsl:when test="a:Stereotype[contains(., '#enumeration')]"><xsl:value-of select="'&lt;&lt;enumeration&gt;&gt;'"/></xsl:when>
						<xsl:when test="a:Stereotype[contains(., '#cimdatatype')]"><xsl:value-of select="'&lt;&lt;CIMDatatype&gt;&gt;'"/></xsl:when>
						<xsl:when test="a:Stereotype[contains(., '#compound')]"><xsl:value-of select="'&lt;&lt;Compound&gt;&gt;'"/></xsl:when>
						<xsl:when test="a:Stereotype[contains(., '#constrainedprimitive')]"><xsl:value-of select="'&lt;&lt;ConstrainedPrimitive&gt;&gt;'"/></xsl:when>
						<xsl:when test="a:Stereotype[contains(., '#primitive')]"><xsl:value-of select="'&lt;&lt;Primitive&gt;&gt;'"/></xsl:when>
						<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="classType">
					<xsl:choose>
						<xsl:when test="a:Stereotype[contains(., '#enumeration')]"><xsl:value-of select="'enum'"/></xsl:when>
						<xsl:otherwise><xsl:value-of select="'class'"/></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<item></item>
				<list begin="" indent="" delim="" end="">
					<item>' This class represents an "orphan" reference on an invalid Reference/Instance that must be fixed in the profile</item>
					<item>' We highlight it by generating a color indicating it is invalid and that the user should add in the orphaned type</item>
					<list begin="{concat($classType, ' ', $targetClass, ' &lt;&lt;error&gt;&gt; ', (if (not($stereotype = null)) then $stereotype else ''), $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
						<item>' nothing to generate</item>
					</list>
				</list>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, Enumerated, and Compound attributes templates)                               -->
	<!-- ============================================================================================================ -->
	
	<xsl:template match="a:Simple">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="substring-after(@cimDatatype, '#')"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Domain">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="substring-after(@dataType, '#')"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Compound|a:Enumerated">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant, ' {readOnly}') else ''"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="substring-after(@baseClass, '#')"/> <xsl:call-template name="cardinality"/><xsl:value-of select="$constant"/></item>
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- END SECTION:  (Simple, Domain, and Enumerated attributes templates)                           -->
	<!-- ============================================================================================================ -->

	<xsl:template name="count_fields">
		<xsl:param name="count"/>
		<xsl:variable name="total_count" select="$count + count(a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice)"/>
		<xsl:choose>
			<xsl:when test="a:SuperType">
				<xsl:variable name="baseClass" select="a:SuperType/@baseClass"/>
				<xsl:for-each select="/*/node()[@baseClass = $baseClass]">
					<xsl:call-template name="count_fields">
						<xsl:with-param name="count" select="$total_count"/>
					</xsl:call-template>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$total_count"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="stereotypes">
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
						<xsl:when test="not(($stereotype = 'xsdelement') or ($stereotype = 'xsdattribute') or ($stereotype = 'compound') or ($stereotype = 'enumeration') or ($stereotype = 'attribute') or ($stereotype = 'byreference') or ($stereotype = 'enum') or ($stereotype = 'concrete') or ($stereotype = 'ofAggregate') or ($stereotype = 'aggregateOf') or ($stereotype = 'ofComposite') or ($stereotype = 'compositeOf'))">
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