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
					<list begin="" indent="" delim="" end="">
						<list begin="{concat('class ', cimtool:puml-safe-name(@name), ' &lt;&lt;Root XSDelement&gt;&gt; &lt;&lt;Document Root&gt;&gt; &lt;&lt;(E, #F3F3F3)&gt;&gt;', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<item><xsl:value-of select="$baseURI"/></item>
						</list>			
						<xsl:for-each select="a:Root">
							<!-- Output the association -->	
							<xsl:variable name="targetRoleEndName" select="@name"/>
							<xsl:variable name="targetCardinality">
								<xsl:choose>
									<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:call-template name="association-cardinality"/></xsl:when>
								</xsl:choose>
							</xsl:variable>
							<!-- We filter out character in a class or note name now allowed by PlantUML. This was need      -->
							<!-- after discovering scenarios where end-users had profile names (i.e. $envelope) with dashes  -->
							<!-- (-) in them leading to have to parse out such character.                                    -->
							<xsl:variable name="envelope-name" select="fn:replace($envelope, '[\s:.,\[\]{}\(\)&lt;&gt;\-\+#\*=]', '_')" />
							<item><xsl:value-of select="concat($envelope-name, ' --&gt;  &quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', @name)"/></item>
						</xsl:for-each>		
					</list>
					<item></item>
					<list begin="{concat('skinparam note ', '&#123;')}" indent="  " delim="" end="{'&#125;'}">
						<item>BorderColor #454645</item>
						<item>BorderThickness 1.5</item>
						<item>FontSize 14</item>
						<item>Font Bold</item>
						<item>FontColor #000000</item>
					</list>
					<xsl:if test="a:Note and a:Note[string-length(.) > 0]">
						<item></item>
						<item>' Add a note towards the upper left corner of the diagram</item>
						<list begin="note as NoteInfo {$docRootClassesColor}" indent="   " delim="" end="end note">
								<item>Profile Notes:</item>
								<xsl:apply-templates select="a:Note" mode="profile-notes"/>
						</list>
					</xsl:if>
					<item></item>
				</xsl:if>
				<xsl:apply-templates select="a:Root|a:ComplexType|a:EnumeratedType|a:CompoundType|a:SimpleType|a:PrimitiveType"/>
				<xsl:apply-templates select="//a:Complex|//a:SimpleEnumerated|//a:SimpleCompound" mode="anonymous"/>	
				<!-- Finally, we process all associations: -->
				<xsl:apply-templates select="//a:Complex|//a:Choice|//a:Reference|//a:Instance" mode="associations"/>
				<item></item>
			</list>
		</document>
	</xsl:template>

	<xsl:template match="a:EnumeratedType">
		<xsl:if test="not($hideEnumerations) and not(@hideInDiagrams = 'true')">
			<!-- 
				In XSD or JSON style profiling it can be convention to rename the element name so 
				we derive the enumeration name from the @name attribute and not from the @baseClass
				which will always represent the class as it is named in the canonical model.
			-->
			<xsl:variable name="enumName" select="@name"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="count" select="count(a:EnumeratedValue)"/>
			<list begin="" indent="" delim="" end="">
				<list begin="enum {concat($enumName, ' &lt;&lt;XSDsimpleType&gt;&gt; ', $stereotypes, ' ', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
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
	
	<!--
	  Function: cimtool:unique-id
	  Purpose: Produce a unique, PlantUML-safe identifier for the specified element.
	  Parameters:
		$element - the element (Complex, SimpleEnumerated, SimpleCompound, Choice or Reference) to generate an identifier for.
	  Returns:
		xs:string - unique identifier for the supplied element.
	  Algorithm:
		1) Determine the id prefix based upon the element type.
		2) Normalize this element's @name to [A-Za-z0-9_].
		3) Build a normalized '::'-joined chain of ancestor @name values (also normalized).
		4) Compute an occurrence index k among preceding a:Complex elements with the same @name
		   (document-order tie-breaker).
		5) Concatenate into: <prefix>__<ancestorNames>__<thisName>__n<k>
	-->
	<xsl:function name="cimtool:unique-id" as="xs:string">
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
		<xsl:variable name="ancestorNames" select="string-join($element/ancestor::*[@name]/replace(@name,'[^A-Za-z0-9_]', '_'), '::')"/>
		<xsl:variable name="k" select="count($element/preceding::a:Complex[@name = $element/@name]) + 1"/>
		<xsl:sequence select="concat($prefix, '::', if ($ancestorNames) then $ancestorNames else 'ROOT', '::', $thisName, '::n', $k)"/>
	</xsl:function>
		
	<!--
	This template is to generate an anonymous complex type. To differentiate an anonymous complex from a
	standard complex types the class is colored differently and is annotated with the <<anonymous>> stereotype.
	-->
	<xsl:template match="a:Complex" mode="anonymous">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="anonymousId" select="cimtool:unique-id(.)"/>
			<xsl:variable name="anonymousDispayLabel" select="substring-after(@baseClass, '#')"/>
			<list begin="" indent="" delim="" end="">
				<item>' Anonymous complex type <xsl:value-of select="$anonymousDispayLabel"/></item>
				<list begin="{concat('class ', $anonymousId, ' as &quot;', $anonymousDispayLabel, '&quot; &lt;&lt;anonymous&gt;&gt; ', (if (not($enableDarkMode) and $setAnonymousClassesColorWhite) then '#FFFFFF' else ''), ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:apply-templates select="a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
						</xsl:when>
						<xsl:otherwise>
							<item>[Attributes hidden]</item>
						</xsl:otherwise>
					</xsl:choose>							
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<!--
	This template is to generate an anonymous enumeration type. To differentiate an anonymous enumeration from
	standard enumerations the class is colored differently and is annotated with the <<anonymous>> stereotype.
	-->
	<xsl:template match="a:SimpleEnumerated" mode="anonymous">
		<xsl:if test="not($hideEnumerations) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="anonymousId" select="cimtool:unique-id(.)"/>
			<xsl:variable name="enumName" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="anonEnumName" select="concat(../@name, '::', substring-after(@baseClass, '#'))"/>
			<xsl:variable name="count" select="count(a:EnumeratedValue)"/>
			<list begin="" indent="" delim="" end="">
				<item>' Anonymous enumeration <xsl:value-of select="$enumName"/></item>
				<list begin="enum {concat($anonymousId, ' as &quot;', $anonEnumName, '&quot; &lt;&lt;anonymous&gt;&gt; &lt;&lt;enumeration&gt;&gt; ', (if (not($enableDarkMode) and $setAnonymousClassesColorWhite) then '#FFFFFF' else ''), ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
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
	This template is to generate an anonymous compound type. To differentiate an anonymous compound from
	standard compounds the class is colored differently and is annotated with the <<anonymous>> stereotype.
	-->
	<xsl:template match="a:SimpleCompound" mode="anonymous">
		<xsl:if test="not($hideCompounds) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="anonymousId" select="cimtool:unique-id(.)"/>
			<xsl:variable name="compoundName" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="anonCompoundName" select="concat(../@name, '::', substring-after(@baseClass, '#'))"/>
			<list begin="" indent="" delim="" end="">
				<item>' Anonymous compound <xsl:value-of select="$compoundName"/></item>
				<list begin="{concat('class ', $anonymousId, ' as &quot;', $anonCompoundName, '&quot; &lt;&lt;anonymous&gt;&gt; &lt;&lt;Compound&gt;&gt; ', (if (not($enableDarkMode) and $setAnonymousClassesColorWhite) then '#FFFFFF' else ''), ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:for-each select="a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain">
								<xsl:choose>
									<xsl:when test="self::a:SimpleEnumerated|self::a:SimpleCompound">
										<xsl:apply-templates select="." mode="anonymous"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:apply-templates select="."/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<item>[Attributes hidden]</item>
						</xsl:otherwise>
					</xsl:choose>							
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:SimpleType">
		<xsl:if test="not($hideCIMDatatypes) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@dataType, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<list begin="" indent="" delim="" end="">
				<list begin="{concat('class ', $className, ' &lt;&lt;XSDsimpleType&gt;&gt; ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
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
				<list begin="{concat('class ', $className, ' &lt;&lt;XSDsimpleType&gt;&gt; ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">		
				</list>
			</list>
			<item>hide "<xsl:value-of select="$className"/>" attributes</item>
			<item></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:CompoundType">
		<xsl:if test="not($hideCompounds) and not(@hideInDiagrams = 'true')">
			<!-- 
				XSD profiling is different than RDFS based in that we can define multiple (duplicate)
				classes in the profile that are derived from the same canonical type. In that case we  
				derive the class name from the @name attribute as opposed to how in RDFS profile builders
				we derive it from the @baseClass attribute. 
			-->
			<xsl:variable name="className" select="@name"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<list begin="" indent="" delim="" end="">
				<list begin="{concat('class ', $className, ' &lt;&lt;XSDcomplexType&gt;&gt; ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<xsl:choose>
						<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
							<xsl:apply-templates select="a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
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
			<xsl:variable name="className" select="@name"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:choose>
				<xsl:when test="a:SuperType">
					<xsl:variable name="superClassName" select="a:SuperType/@name"/>
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="$className"/> inherits from <xsl:value-of select="$superClassName"/></item>
						<list begin="{concat(if (not(a:Stereotype[contains(., '#concrete')])) then 'abstract class' else 'class', ' ', $className, ' ', (if (not(a:Stereotype[contains(., '#xsdattributegroup')])) then '&lt;&lt;XSDcomplexType&gt;&gt;' else ''), $stereotypes, ' ', if (not(a:Stereotype[contains(., '#concrete')])) then '&lt;&lt;abstract&gt;&gt;' else '', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
								</xsl:when>
								<xsl:otherwise>
									<item>[Attributes hidden]</item>
								</xsl:otherwise>
							</xsl:choose>
						</list>
						<xsl:if test="not(//node()[@name = $superClassName]/@hideInDiagrams = 'true')">
							<item><xsl:value-of select="concat($superClassName, ' &lt;|-- ', $className)"/></item>
						</xsl:if>
					</list>
				</xsl:when>
				<xsl:otherwise>
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="$className"/></item>
						<list begin="{concat(if (not(a:Stereotype[contains(., '#concrete')])) then 'abstract class' else 'class', ' ', $className, ' ', (if (not(a:Stereotype[contains(., '#xsdattributegroup')])) then '&lt;&lt;XSDcomplexType&gt;&gt;' else ''), $stereotypes, ' ', if (not(a:Stereotype[contains(., '#concrete')])) then '&lt;&lt;abstract&gt;&gt;' else '', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
								</xsl:when>
								<xsl:otherwise>
									<item>[Attributes hidden]</item>
								</xsl:otherwise>
							</xsl:choose>							
						</list>
					</list>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Complex" mode="associations">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="anonymousId" select="cimtool:unique-id(.)"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<xsl:variable name="sourceClass">	
				<xsl:value-of select="if (parent::a:Choice|parent::a:Complex) then cimtool:unique-id(parent::*) else parent::*/@name"/>
			</xsl:variable>
			<xsl:variable name="targetClass"><xsl:value-of select="$anonymousId"/></xsl:variable>
			<xsl:variable name="sourceRoleEndName">
				<xsl:value-of select="substring-after(substring-after(@inverseBaseProperty, '#'), '.')"/>
			</xsl:variable>
			<xsl:variable name="targetCardinality">
				<xsl:choose>
					<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:call-template name="association-cardinality"/></xsl:when>
					<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="targetRoleEndName">
				<xsl:choose>
					<xsl:when test="not(@name = '')"><xsl:value-of select="@name"/></xsl:when>
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
			<item><xsl:value-of select="concat($sourceClass, ' ', $associationType, ' &quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', $targetClass)"/></item>
			<item></item>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Reference|a:Instance" mode="associations">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<xsl:variable name="sourceClass">
				<xsl:choose>
					<xsl:when test="parent::a:Choice|parent::a:Complex">
						<xsl:value-of select="cimtool:unique-id(parent::*)"/>		
					</xsl:when>
					<xsl:when test="parent::a:Root|parent::a:ComplexType">
						<xsl:value-of select="parent::*/@name"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="substring-after(@basePropertyClass, '#')"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="type" select="@type"/>
			<xsl:variable name="targetClass">
				<xsl:choose>
					<xsl:when test="self::a:Reference">
						<xsl:choose>
							<xsl:when test="//(a:Root|a:ComplexType)[@name = $type]">
								<xsl:value-of select="cimtool:unique-id(.)"/>
							</xsl:when>
							<xsl:when test="//a:EnumeratedType[@name = $type]">
								<xsl:variable name="theEnumeration" select="//a:EnumeratedType[@name = $type]"/>
								<xsl:value-of select="if ($hideEnumerations or ($theEnumeration/@hideInDiagrams = 'true')) then concat(cimtool:unique-id($theEnumeration), '::', cimtool:unique-id(.)) else $type"/>
							</xsl:when>
							<xsl:when test="//a:CompoundType[@name = $type]">
								<xsl:variable name="theCompound" select="//a:CompoundType[@name = $type]"/>
								<xsl:value-of select="if ($hideCompounds or ($theCompound/@hideInDiagrams = 'true')) then concat(cimtool:unique-id($theCompound), '::', cimtool:unique-id(.)) else $type"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$type"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$type"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="sourceRoleEndName">
				<xsl:value-of select="substring-after(substring-after(@inverseBaseProperty, '#'), '.')"/>
			</xsl:variable>
			<xsl:variable name="targetCardinality">
				<xsl:choose>
					<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:call-template name="association-cardinality"/></xsl:when>
					<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="targetRoleEndName">
				<xsl:choose>
					<xsl:when test="not(@name = '')"><xsl:value-of select="@name"/></xsl:when>
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
			
			<xsl:if test="self::a:Reference">
				<xsl:choose>
					<xsl:when test="//(a:Root|a:ComplexType)[@name = $type]">
						<xsl:variable name="refClassIdentifier" select="cimtool:unique-id(.)"/>
						<item></item>
						<list begin="" indent="" delim="" end="">
							<item>' Ref placeholder class for <xsl:value-of select="@type"/></item>
							<list begin="{concat('class ', $refClassIdentifier, ' as &quot;@ref  &quot; &lt;&lt;Ref&gt;&gt; &lt;&lt;($ref_icon)&gt;&gt;', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							</list>	
							<item>hide "<xsl:value-of select="$refClassIdentifier"/>" attributes</item>
							<item></item>
						</list>
					</xsl:when>
					<!-- 
						The below two scenarios are invalid profiling errors where an end user has included the enumeration or compound type definition within
						the profile but they have not properly 'pulled it in' as the declared type on the attribute. These appear as a:Reference rather than as
						an a:Enumerated or an a:Compound. Here the PlantUML builder should render it as a red association to the enumeration to indicate it should
						be fixed in the profile. Below further addresses the situation where the enumeration/compound is flagged as hidden and therefore is not 
						defined earlier in the PlantUML diagram and therefore will be auto-generated by PlantUML as a class if not explicitly defined. Therefore,
						instead of letting PlantUML incorrectly define a default class we define a "one-off" definition for the a:EnumerationType or a:CompoundType
						so that the association links to an appropriate representation which better informs the end user of what the issue is.
					-->
					<xsl:when test="//a:EnumeratedType[@name = $type]">
						<xsl:variable name="theEnumeration" select="//a:EnumeratedType[@name = $type]"/>
						<xsl:variable name="anonymousId" select="cimtool:unique-id($theEnumeration)"/>
						<xsl:if test="$hideEnumerations or ($theEnumeration/@hideInDiagrams = 'true')">
							<xsl:variable name="name" select="$theEnumeration/@name"/>
							<xsl:variable name="count" select="count($theEnumeration/a:EnumeratedValue)"/>
							<list begin="" indent="" delim="" end="">
								<list begin="enum {concat($targetClass, ' as &quot;', $name, '&quot; &lt;&lt;enumeration&gt;&gt; ', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
									<xsl:for-each select="$theEnumeration">
										<xsl:for-each select="a:EnumeratedValue[position() &lt;= 20]">
											<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
											<item><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="substring-after(substring-after(@baseResource, '#'), '.')" /></item>
										</xsl:for-each>
									</xsl:for-each>
									<xsl:if test="$count > 20">
										<item>[Remaining <xsl:value-of select="$count - 20"/> literals hidden]</item>
									</xsl:if>
								</list>
							</list>
						</xsl:if>
					</xsl:when>
					<xsl:when test="//a:CompoundType[@name = $type]">
						<xsl:variable name="theCompound" select="//a:CompoundType[@name = $type]"/>
						<xsl:if test="$hideCompounds or ($theCompound/@hideInDiagrams = 'true')">
							<xsl:variable name="name" select="$theCompound/@name"/>
							<list begin="" indent="" delim="" end="">
								<list begin="class {concat($targetClass, ' as &quot;', $name, '&quot; &lt;&lt;Compound&gt;&gt; ', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
									<xsl:choose>
										<xsl:when test="not($theCompound/a:Stereotype[contains(., '#diagramshideallattributes')])">
											<xsl:for-each select="$theCompound">
												<xsl:apply-templates select="a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
											</xsl:for-each>
										</xsl:when>
										<xsl:otherwise>
											<item>[Attributes hidden]</item>
										</xsl:otherwise>
									</xsl:choose>							
								</list>
							</list>
						</xsl:if>
					</xsl:when>
				</xsl:choose>
			</xsl:if>
			
			<!-- Output the association -->
			<item><xsl:value-of select="concat($sourceClass, ' ', $associationType, ' &quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', $targetClass)"/><xsl:if test="a:Stereotype[contains(., '#enumeration')] or a:Stereotype[contains(., '#compound')] or a:Stereotype[contains(., '#cimdatatype')] or a:Stereotype[contains(., '#primitive')] or (self::a:Reference and not (parent::a:Choice) and not(a:Stereotype[contains(., '#byreference')]))"><xsl:value-of select="if ($enableDarkMode) then '#FF2D2D' else ' #red'"/></xsl:if></item>
			
			<!-- If none of the below four types of elements is defined as a top level class for $targetClass then it means that the class has 
				 not yet been pulled into the profile and therefore should be flagged as an error (i.e. expressed as a class in light red) -->
			<xsl:if test="not(//a:ComplexType[@name = $type]|//a:Root[@name = $type]|//a:CompoundType[@name = $type]|//a:EnumeratedType[@name = $type]|//a:PrimitiveType[@name = $type])">
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
	
	<xsl:template match="a:Choice" mode="associations">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<xsl:variable name="choiceTargetRoleEndName" select="@name"/>
			<xsl:variable name="choiceTargetCardinality">
				<xsl:choose>
					<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:call-template name="association-cardinality"/></xsl:when>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="choiceClassName" select="cimtool:unique-id(.)"/>
			<item></item>
			<list begin="" indent="" delim="" end="">
				<item>' Choice placeholder class for <xsl:value-of select="@name"/></item>
				<list begin="{concat('class &quot;', @name, ' [', $choiceTargetCardinality, ']&quot; as ', $choiceClassName, ' &lt;&lt;Choice&gt;&gt; &lt;&lt;($choice_icon)&gt;&gt;', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
				</list>		
				<item>hide "<xsl:value-of select="$choiceClassName"/>" attributes</item>
				<item></item>
				<xsl:variable name="parentClassName">
					<xsl:choose>
						<xsl:when test="parent::a:Choice|parent::a:Complex">
							<xsl:value-of select="cimtool:unique-id(parent::*)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="parent::*/@name"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:value-of select="concat($parentClassName, ' --&gt;  ', $choiceClassName)"/>
				<item></item>
			</list>
			<item></item>
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, Enumerated, and Compound attributes templates)                               -->
	<!-- ============================================================================================================ -->
	
	<xsl:template match="a:Simple">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item><xsl:value-of select="if (a:Stereotype[contains(., '#xsdattribute')] or parent::*[a:Stereotype[contains(., '#xsdattributegroup')]]) then '@ ' else '&lt;$empty_icon&gt;'"/><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="substring-after(@cimDatatype, '#')"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Domain">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item><xsl:value-of select="if (a:Stereotype[contains(., '#xsdattribute')] or parent::*[a:Stereotype[contains(., '#xsdattributegroup')]]) then '@ ' else '&lt;$empty_icon&gt;'"/><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="substring-after(@dataType, '#')"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Compound">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant, ' {readOnly}') else ''"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item><xsl:value-of select="'&lt;$empty_icon&gt;'"/><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="@type"/> <xsl:call-template name="cardinality"/><xsl:value-of select="$constant"/></item>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Enumerated">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant, ' {readOnly}') else ''"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item><xsl:value-of select="if (a:Stereotype[contains(., '#xsdattribute')] or parent::*[a:Stereotype[contains(., '#xsdattributegroup')]]) then '@ ' else '&lt;$empty_icon&gt;'"/><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="@type"/> <xsl:call-template name="cardinality"/><xsl:value-of select="$constant"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:SimpleEnumerated">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="enumName" select="concat(../@name, '::', substring-after(@baseClass, '#'))"/>
			<xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant, ' {readOnly}') else ''"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item><xsl:value-of select="'&lt;$empty_icon&gt;'"/><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="$enumName"/> <xsl:call-template name="cardinality"/><xsl:value-of select="$constant"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Complex">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="complexName" select="concat(../@name, '::', substring-after(@baseClass, '#'))"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item><xsl:value-of select="'&lt;$empty_icon&gt;'"/><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="$complexName"/> <xsl:call-template name="cardinality"/></item>
		</xsl:if>
	</xsl:template>
		
	<xsl:template match="a:SimpleCompound">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="compoundName" select="concat(../@name, '::', substring-after(@baseClass, '#'))"/>
			<xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant, ' {readOnly}') else ''"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item><xsl:value-of select="'&lt;$empty_icon&gt;'"/><xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/>: <xsl:value-of select="$compoundName"/> <xsl:call-template name="cardinality"/><xsl:value-of select="$constant"/></item>
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
					<xsl:when test="fn:ends-with($stereotypes, ',')">
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