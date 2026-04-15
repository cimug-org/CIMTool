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
<xsl:stylesheet exclude-result-prefixes="a map fn cimtool" version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:a="http://langdale.com.au/2005/Message#" xmlns="http://langdale.com.au/2009/Indent" xmlns:map="http://www.w3.org/2005/xpath-functions/map" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:cimtool="http://cimtool.ucaiug.io/functions">

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

    <!-- Tokenize by '|' to get name=value parameter pairs -->
    <xsl:variable name="pairs" select="fn:tokenize($builderParameters, '\|')"/>

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
	
	<xsl:function name="cimtool:association-cardinality" as="xs:string">
		<xsl:param name="element" as="element()"/>
		<xsl:sequence select="if ($element/@minOccurs = $element/@maxOccurs) then $element/@minOccurs else concat($element/@minOccurs, '..', replace(replace($element/@maxOccurs, 'unbounded', '*'), 'n', '*'))"/>
	</xsl:function>
	
	<xsl:function name="cimtool:cardinality" as="xs:string">
		<xsl:param name="element" as="element()"/>
		<xsl:variable name="result">
			<xsl:choose>
				<xsl:when test="$hideCardinalityForRequiredAttributes and (@minOccurs = 1)">
					<xsl:value-of select="if (@minOccurs = @maxOccurs) then '' else concat(' [', @minOccurs, '..', replace(replace(@maxOccurs, 'unbounded', '*'), 'n', '*'), ']')"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="if (@minOccurs = @maxOccurs) then concat(' [', @minOccurs, ']') else concat(' [', @minOccurs, '..', replace(replace(@maxOccurs, 'unbounded', '*'), 'n', '*'), ']')"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:sequence select="$result"/>
	</xsl:function>
	
	<xsl:template match="a:Catalog">
		<document>
			<list begin="@startuml" indent="" delim="" end="@enduml">
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
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
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<item>top to bottom direction</item>
				<item>hide empty methods</item>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<xsl:if test="$horizontalSpacing and $horizontalSpacing != ''">
					<item>' Here we adjust the horizontal spacing for better spatial rendering (the PlantUML default is ~20)</item>
					<item>skinparam nodesep <xsl:value-of select="$horizontalSpacing"/></item>
				</xsl:if>
				<xsl:if test="$verticalSpacing and $verticalSpacing != ''">
					<item>' Here we adjust the vertical spacing for better spatial rendering (the PlantUML default is ~30)</item>
					<item>skinparam ranksep <xsl:value-of select="$verticalSpacing"/></item>
				</xsl:if>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
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
								<!-- Document Root definition -->
								<item>' Document Root elment style definition</item>
								<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> #3e3e3e</item>
								<item>FontColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> #6a6a6a</item>
								<item>FontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> 18</item>
								<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> #FFFFFF</item>
								<item>AttributeFontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> 18</item>
								<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> #FFFFFF</item>
								<item>StereotypeFontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> 14</item>
								<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> #FFFFFF</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- Choice definition -->
								<item>' Choice elment style definition</item>
								<item>BackgroundColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/> #E6E6FF</item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/> #6a6a6a</item>
								<item>FontColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/> #FFFFFF</item>
								<item>AttributeFontColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/> #FFFFFF</item>
								<item>StereotypeFontColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/> #FFFFFF</item>
								<item>HeaderFontColor<xsl:value-of select="'&lt;&lt;Choice&gt;&gt; '"/> #FFFFFF</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
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
								<!-- Document Root definition -->
								<item>' Document root elment style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesColor)"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesFontColor)"/></item>
								<item>FontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> 18</item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesFontColor)"/></item>
								<item>AttributeFontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> 18</item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesFontColor)"/></item>
								<item>StereotypeFontSize<xsl:value-of select="'&lt;&lt;Document Root&gt;&gt; '"/> 14</item>
								<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Document Root&gt;&gt; ', $docRootClassesFontColor)"/></item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
								<!-- Choice definition -->
								<item>' Choice elment style definition</item>
								<item>BackgroundColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', '#E6E6FF')"/></item>
								<item>FontColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', '#000000')"/></item>
								<item>AttributeFontColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', '#000000')"/></item>
								<item>StereotypeFontColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', '#000000')"/></item>
								<item>HeaderFontColor<xsl:value-of select="concat('&lt;&lt;Choice&gt;&gt; ', '#000000')"/></item>
								<item>BorderColor<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> #454645</item>
								<item>BorderThickness<xsl:value-of select="'&lt;&lt;abstract&gt;&gt; '"/> 1</item>
								<item>&#xD;&#xA;</item> <!-- CR/LF -->
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
				<item><xsl:value-of select="'hide &lt;&lt;Choice&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;abstract&gt;&gt; stereotype'"/></item>
				<item><xsl:value-of select="'hide &lt;&lt;error&gt;&gt; stereotype'"/></item>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				
				<xsl:if test="not(@hideInDiagrams = 'true')">
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="@name"/></item>
						<list begin="{concat('class ', @name, ' &lt;&lt;Document Root&gt;&gt; &lt;&lt;(R, #F3F3F3)&gt;&gt;', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<item><xsl:value-of select="$baseURI"/></item>
						</list>			
						<xsl:for-each select="a:Root">
							<!-- Output the association -->	
							<xsl:variable name="targetRoleEndName" select="concat('+', @name)"/>
							<xsl:variable name="targetCardinality">
								<xsl:choose>
									<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:value-of select="cimtool:association-cardinality(.)"/></xsl:when>
								</xsl:choose>
							</xsl:variable>
							<!-- We filter out character in a class or note name now allowed by PlantUML. This was need      -->
							<!-- after discovering scenarios where end-users had profile names (i.e. $envelope) with dashes  -->
							<!-- (-) in them leading to have to parse out such character.                                    -->
							<xsl:variable name="envelope-name" select="replace($envelope, '[\s:.,\[\]{}\(\)&lt;&gt;\-\+#\*=]', '_')" />
							<item><xsl:value-of select="concat($envelope-name, ' --&gt;  &quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', @name)"/></item>
						</xsl:for-each>		
					</list>
					<item>&#xD;&#xA;</item> <!-- CR/LF -->
					<list begin="{concat('skinparam note ', '&#123;')}" indent="  " delim="" end="{'&#125;'}">
						<item>BorderColor #454645</item>
						<item>BorderThickness 1.5</item>
						<item>FontSize 14</item>
						<item>Font Bold</item>
						<item>FontColor #000000</item>
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
				
				<list begin="{{" indent="     " delim="," end="}}">
					<item>"name": "header"</item>
					<item>"type": "Header"</item>
					<item>"doc": "The standardized messaging header that must be included with each message compliant with this profile."</item>
				</list>
				
				<!-- Get all Root elements -->
				<xsl:variable name="all-roots" select="//a:Root"/>
				
				<!-- Find true root fields (never referenced) -->
				<xsl:variable name="root-fields" select="cimtool:get-root-fields($all-roots)"/>

				<xsl:for-each select="$root-fields">
					<list begin="{{" indent="     " delim="," end="}}">
						<item>"name": "<xsl:value-of select="@name"/>"</item>
						<xsl:choose>
							<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
								<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
									<item>"type": "array"</item>
									<item>"items": "'Class'"</item>
								</list>
								<xsl:if test="@minOccurs = 0">
									<item>"default": []</item>
								</xsl:if>
							</xsl:when>	
							<xsl:otherwise>
								<item>"type": "'Class'"</item>
							</xsl:otherwise>
						</xsl:choose>
						<item>"doc": ""</item>
						<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
							<xsl:if test="@minOccurs != 0">
								<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
							</xsl:if>
							<xsl:if test="not(@maxOccurs = 'unbounded')">
								<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
							</xsl:if>
						</xsl:if>	
					</list>
				</xsl:for-each>
							
				<xsl:apply-templates select="a:Root|a:ComplexType|a:EnumeratedType|a:CompoundType|a:SimpleType|a:PrimitiveType"/>			
			</list>
		</document>
	</xsl:template>
	
	<xsl:template match="a:Catalog">
		<!-- the top level template -->
		<document>
			<list begin="[" indent="     " delim="," end="]">
			
				<!-- Apache Avro instance data header definition. -->
				<list begin="{{" indent="     " delim="," end="}}">
					<item>"type": "record"</item>
					<item>"name": "Header"</item>
					<item>"namespace": "''"</item>
					<list begin="&quot;fields&quot;: [" indent="     " delim="," end="]">
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "profProfile"</item>
							<item>"type": "string"</item>
							<item>"doc": "URI of the DX-PROF profile this dataset conforms to, e.g. https://ap.cim4.eu/StateVariables/3.0"</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "identifier"</item>
							<item>"type": "string"</item>
							<item>"doc": "Dataset identifier, aligned with dcterms:identifier / dcat:Dataset.@about."</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "isVersionOf"</item>
							<item>"type": [ "null", "string" ]</item>
							<item>"doc": "URI of the logical dataset or model this is a version of (dct:isVersionOf)."</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "version"</item>
							<item>"type": [ "null", "string" ]</item>
							<item>"doc": "Version label for this dataset instance (aligned with dcat:version). Useful when multiple messages represent different versions of the same time slice."</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "startDate"</item>
							<item>"type": "string"</item>
							<item>"doc": "Start of the validity interval for this dataset, aligned with dcat:startDate (ISO-8601). Typically the case time of the power system state."</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "schemaRef"</item>
							<item>"type": "string"</item>
							<item>"doc": "Dereferenceable URI or Schema Registry URL for the Avro schema used to encode this dataset."</item>
						</list>					
					</list>
				</list>
			
				<!-- Enumerations can be generated first as they should have no dependencies -->
				<xsl:apply-templates select="a:EnumeratedType"/>
				
				<!-- Step 1: Create exclusion map for EnumeratedTypes ONLY -->
				<xsl:variable name="compound-exclusions" select="cimtool:create-exclusions-map(//a:EnumeratedType)"/>
				
				<!-- Step 2: Create dependency map for CompoundTypes. Note we exclude any dependencies to enumerations since they've been processed. -->
				<xsl:variable name="compound-deps-map" select="cimtool:build-dependencies-map(//a:CompoundType, $compound-exclusions)"/>

				<!-- Step 3: Process CompoundTypes in correct dependency order. -->
				<xsl:variable name="sorted-compound-types" select="cimtool:topological-sort(//a:CompoundType, $compound-deps-map)"/>
				<xsl:apply-templates select="$sorted-compound-types"/>
				
				<!-- Step 4: Create exclusion map for EnumeratedTypes AND CompoundTypes (already processed) -->
				<xsl:variable name="root-exclusions" select="cimtool:create-exclusions-map(//a:EnumeratedType|//a:CompoundType)"/>
				
				<!-- Step 5: Finally, we create dependency map for Root types excluding EnumeratedTypes and CompoundTypes -->
				<!-- Note that we do not include a:ComplexType as they are abstract and Avro does not support inheritance -->
				<!-- and instead the inheritance hierarchy is "flattened" and all attributes and associations are in the  -->
				<!-- concrete (i.e. a:Root) classes.                                                                      -->
				<xsl:variable name="root-deps-map" select="cimtool:build-dependencies-map(//a:Root, $root-exclusions)"/>
				
				<!-- Step 6: Process Root elements in correct dependency order -->
				<xsl:variable name="sorted-root-types" select="cimtool:topological-sort(//a:Root, $root-deps-map)"/>
				<xsl:apply-templates select="$sorted-root-types"/>
				
				<!-- The final step is to create the 'document wrapper' derived from the name of the profile -->
				<list begin="{{" indent="     " delim="," end="}}">
					<xsl:if test="$copyright-single-line and $copyright-single-line != ''">
						<item>"copyright": "<xsl:value-of select="$copyright-single-line" disable-output-escaping="yes"/>"</item>			
					</xsl:if>
					<item>"generator": "Generated by CIMTool https://cimtool.ucaiug.io"</item>
					<list begin="&quot;header&quot;: {{" indent="    " delim="," end="}}">
						<item>"metaDoc": "Abstract profile this schema implements (DX-PROF prof:Profile):"</item>
						<item>"profProfile": "<xsl:value-of select="$baseURI"/>"</item>
						<item>"metaDoc": "Underlying standards the profile/schema conforms to (IEC etc.):"</item>
						<item>"metaDoc": "Currently hardcoded until consensus is reached on how/where this will be specified/sourced from in tooling:"</item>
						<list begin="&quot;dctConformsTo&quot;: [" indent="     " delim="," end="]">
							<item>"urn:iso:std:iec:61970-301:ed-7:amd1"</item>
							<item>"urn:iso:std:iec:61970-600-2:ed-1"</item>
						</list>
						<item>"metaDoc": "Where this schema was generated from (OWL/RDFS/SHACL/LinkML, etc.):"</item>
						<list begin="&quot;dctSource&quot;: [" indent="     " delim="," end="]">
							<item>"<xsl:value-of select="concat(substring-before($baseURI, '#'), '/owl')"/>"</item>
						</list>
						<item>"metaDoc": "Identity + version of this Avro schema itself:"</item>
						<item>"metaDoc": "Currently hardcoded until consensus is reached on how/where this will be specified/sourced from in tooling:"</item>
						<item>"schemaId": "https://schema-registry.example.com/subjects/cim-sv-dataset-value/versions/5"</item>
						<item>"metaDoc": "Currently hardcoded to 1.0.0 until consensus is reached on how/where this will be specified/sourced from in tooling:"</item>
						<item>"schemaVersion": "<xsl:value-of select="'1.0.0'"/>"</item>
					</list>
				
					<item>"type": "record"</item>
					<item>"name": "<xsl:value-of select="$envelope"/>"</item>
					<item>"namespace": "''"</item>
					<item>"doc": ""</item>

						<list begin="&quot;fields&quot;: [" indent="     " delim="," end="]">
							<list begin="{{" indent="     " delim="," end="}}">
								<item>"name": "header"</item>
								<item>"type": "Header"</item>
								<item>"doc": "The standardized messaging header that must be included with each message compliant with this profile."</item>
							</list>
							
							<!-- Get all Root elements -->
							<xsl:variable name="all-roots" select="//a:Root"/>
							
							<!-- Find true root fields (never referenced) -->
							<xsl:variable name="root-fields" select="cimtool:get-root-fields($all-roots)"/>

							<xsl:for-each select="$root-fields">
								<list begin="{{" indent="     " delim="," end="}}">
									<item>"name": "<xsl:value-of select="@name"/>"</item>
									<xsl:choose>
										<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
											<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
												<item>"type": "array"</item>
												<item>"items": "'Class'"</item>
											</list>
											<xsl:if test="@minOccurs = 0">
												<item>"default": []</item>
											</xsl:if>
										</xsl:when>	
										<xsl:otherwise>
											<item>"type": "'Class'"</item>
										</xsl:otherwise>
									</xsl:choose>
									<item>"doc": ""</item>
									<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
										<xsl:if test="@minOccurs != 0">
											<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
										</xsl:if>
										<xsl:if test="not(@maxOccurs = 'unbounded')">
											<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
										</xsl:if>
									</xsl:if>	
								</list>
							</xsl:for-each>
						</list>
						
				</list>		
							
			</list>
		</document>
	</xsl:template>
	
	<xsl:template match="a:Root">
	<!-- Note that for AVRO schemas we don't generate Root classes that don't have any attributes. These are considered association references to "external" entities. -->
		<xsl:variable name="fieldCount" select="cimtool:count-fields(.)"/>
		<xsl:if test="$fieldCount > 0">
			<xsl:if test="not(@hideInDiagrams = 'true')">
				<xsl:variable name="className" select="@name"/>
				<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
				<xsl:choose>
					<xsl:when test="a:SuperType">
						<xsl:variable name="superClassName" select="a:SuperType/@name"/>
						<xsl:call-template name="generate-fields"/>
						
						<list begin="" indent="" delim="" end="">
							<item>' <xsl:value-of select="$className"/> inherits from <xsl:value-of select="$superClassName"/></item>
							<list begin="{concat(if (not(a:Stereotype[contains(., '#concrete')])) then 'abstract class' else 'class', ' ', $className, ' ', $stereotypes, ' ', if (not(a:Stereotype[contains(., '#concrete')])) then '&lt;&lt;abstract&gt;&gt;' else '', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
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
								<xsl:when test="a:Reference|a:Instance|a:Choice">
									<xsl:apply-templates select="a:Reference|a:Instance|a:Choice" mode="associations"/>
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
							<list begin="{concat(if (not(a:Stereotype[contains(., '#concrete')])) then 'abstract class' else 'class', ' ', $className, ' ', $stereotypes, ' ', if (not(a:Stereotype[contains(., '#concrete')])) then '&lt;&lt;abstract&gt;&gt;' else '', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
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
								<xsl:when test="a:Reference|a:Instance|a:Choice">
									<xsl:apply-templates select="a:Reference|a:Instance|a:Choice" mode="associations"/>
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
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedType">
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
	
	<xsl:template match="a:CompoundType">
		<xsl:if test="not($hideCompounds) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="@name"/>
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
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:SimpleType">
		<xsl:if test="not($hideCIMDatatypes) and not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@dataType, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="$className"/></item>
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
				<item>' <xsl:value-of select="$className"/></item>
				<list begin="{concat('class ', $className, ' ', $stereotypes, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">		
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Simple|a:Domain">
		<list begin="{{" indent="     " delim="," end="}}">
			<xsl:variable name="type" select="''"/>
			<item>"name": "<xsl:value-of select="@name"/>"</item>
			<xsl:choose>
				<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
					<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
						<item>"type": "array"</item>
						<item>"items": <xsl:value-of select="$type"/></item>
					</list>
					<xsl:if test="@minOccurs = 0">
						<item>"default": []</item>
					</xsl:if>
				</xsl:when>	
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="@minOccurs = 0">
							<item>"type": [ "null", <xsl:value-of select="$type"/> ]</item>
							<item>"default": null</item>
						</xsl:when>
						<xsl:otherwise>
							<item>"type": <xsl:value-of select="$type"/></item>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
			<item>"doc": ""</item>
			<item>"modelReference": ""</item>
			<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
				<xsl:if test="@minOccurs != 0">
					<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
				</xsl:if>
				<xsl:if test="not(@maxOccurs = 'unbounded')">
					<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
				</xsl:if>
			</xsl:if>	
		</list>
	</xsl:template>
	
	<xsl:template match="a:Compound|a:Enumerated">
		<list begin="{{" indent="     " delim="," end="}}">
			<item>"name": "<xsl:value-of select="@name"/>"</item>
			<xsl:choose>
				<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
					<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
						<item>"type": "array"</item>
						<item>"items": "'Class'"</item>
					</list>
					<xsl:if test="@minOccurs = 0">
						<item>"default": []</item>
					</xsl:if>
				</xsl:when>	
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="@minOccurs = 0">
							<list begin="&quot;type&quot;: [" indent="     " delim="," end="]">
								<item>"null"</item>
								<item>"'Class'"</item>
							</list>
							<item>"default": null</item>
						</xsl:when>
						<xsl:otherwise>
							<item>"type": "'Class'"</item>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
			<item>"doc": ""</item>
			<item>"modelReference": ""</item>
			<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
				<xsl:if test="@minOccurs != 0">
					<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
				</xsl:if>
				<xsl:if test="not(@maxOccurs = 'unbounded')">
					<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
				</xsl:if>
			</xsl:if>	
		</list>
	</xsl:template>
	
	<xsl:template match="a:Instance|a:Reference" mode="associations">
		<xsl:variable name="baseClass" select="@baseClass"/>
		<xsl:variable name="theClass" select="//a:Root[@baseClass = $baseClass]|//a:ComplexType[@baseClass = $baseClass]"/>
		<xsl:variable name="fieldCount" select="cimtool:count-fields($theClass)"/>
		
		<!-- Check if this references an abstract class (ComplexType) that has concrete subclasses -->
		<xsl:variable name="isAbstractWithSubclasses">
			<xsl:choose>
				<xsl:when test="//a:ComplexType[@baseClass = $baseClass]">
					<!-- This references a ComplexType - check if there are Root subclasses -->
					<xsl:variable name="concreteSubclasses">
						<xsl:for-each select="//a:Root">
							<xsl:if test="cimtool:inherits-from(., $baseClass)">
								<xsl:value-of select="''"/>
							</xsl:if>
						</xsl:for-each>
					</xsl:variable>
					<xsl:choose>
						<xsl:when test="string-length($concreteSubclasses) > 0">
							<xsl:text>true</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>false</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>false</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
	
		<xsl:choose>
			<xsl:when test="$fieldCount > 0">
				<xsl:choose>
					<xsl:when test="$isAbstractWithSubclasses = 'true'">
						<xsl:for-each select="//a:Root">
							<xsl:if test="cimtool:inherits-from(., $baseClass)">
								<item>"'Class'"</item>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:otherwise>
						<!-- STUFF HERE -->
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<item>"name": "<xsl:value-of select="@name"/>"</item>
				<xsl:choose>
					<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
						<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
							<item>"type": "array"</item>
							<item>"items": "string"</item>
						</list>
						<xsl:if test="@minOccurs = 0">
							<item>"default": []</item>
						</xsl:if>
					</xsl:when>	
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="@minOccurs = 0">
								<item>"type": [ "null", "string" ]</item>
								<item>"default": null</item>
							</xsl:when>
							<xsl:otherwise>
								<item>"type": "string"</item>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>

	</xsl:template>

	<xsl:template match="a:Instance|a:Reference" mode="associations">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			
			<xsl:variable name="sourceClass">
				<xsl:value-of select="substring-after(@inverseBasePropertyClass, '#')"/>
			</xsl:variable>
			<xsl:variable name="targetClass"><xsl:value-of select="@type"/></xsl:variable>
	
			<!-- These were borrowed from a:Choice as it can be similar 
			<xsl:variable name="sourceClass">
				<xsl:value-of select="substring-before(substring-after(@baseProperty, '#'), '.')"/>
			</xsl:variable>
			<xsl:variable name="sourceClassParent">
				<xsl:value-of select="parent::node()/@name"/>
			</xsl:variable>	
			-->
			
			<xsl:variable name="sourceRoleEndName">
				<xsl:value-of select="concat('+', @name)"/>
			</xsl:variable>
			<xsl:variable name="targetCardinality">
				<xsl:choose>
					<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:value-of select="cimtool:association-cardinality(.)"/></xsl:when>
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
					<!-- There is an edge case where the inverse will not be provided in the intermediary profile format. -->
					<!-- In this case a $sourceClass will be empty and we therefore must obtain the $sourceClass from -->
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'ofAggregate']">--o</xsl:when>
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'aggregateOf']">o--&gt;</xsl:when>
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'ofComposite']">--*</xsl:when>
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'compositeOf']">*--&gt;</xsl:when>
					<xsl:otherwise>--&gt;</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>

			<!-- Output the association -->	
			<item><xsl:value-of select="concat($sourceClass, ' ', $associationType, ' &quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', $targetClass)"/><xsl:if test="a:Stereotype[contains(., '#enumeration')] or a:Stereotype[contains(., '#compound')] or a:Stereotype[contains(., '#cimdatatype')] or a:Stereotype[contains(., '#primitive')] or (self::a:Reference and not(a:Stereotype[contains(., '#byreference')]))"><xsl:value-of select="if ($enableDarkMode) then '#FF2D2D' else '#red'"/></xsl:if></item>
			
			<!-- If none of the below four types of elements is defined as a top level class for $targetClass then it means that the class has 
				 not yet been pulled into the profile and therefore should be flagged as an error (i.e. expressed as class in light red) -->
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
		
	<xsl:template match="a:Choice" mode="associations">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<xsl:variable name="sourceClass">
				<xsl:value-of select="substring-before(substring-after(@baseProperty, '#'), '.')"/>
			</xsl:variable>
			<xsl:variable name="sourceClassParent">
				<xsl:value-of select="parent::node()/@name"/>
			</xsl:variable>	
			<xsl:variable name="choiceTargetClass"><xsl:value-of select="concat(@name, 'Choice')"/></xsl:variable>
			<xsl:variable name="choiceTargetRoleEndName" select="concat('+', @name)"/>
			<xsl:variable name="choiceTargetCardinality">
				<xsl:choose>
					<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:value-of select="cimtool:association-cardinality(.)"/></xsl:when>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="aggregateChoiceClassName" select="concat($sourceClassParent, $choiceTargetClass)"/>
			<item>&#xD;&#xA;</item> <!-- CR/LF -->
			<!-- We filter out character in a class or note name now allowed by PlantUML. This was needed    -->
			<!-- after discovering scenarios where end-users had profile names (i.e. $envelope) with dashes  -->
			<!-- (-) in them leading to have to parse out such character.                                    -->
			<list begin="" indent="" delim="" end="">
				<item>' <xsl:value-of select="@name"/></item>
				<list begin="{concat('class &quot;', @name, ' [', $choiceTargetCardinality, ']&quot; as ', $aggregateChoiceClassName, ' &lt;&lt;Choice&gt;&gt; &lt;&lt;($choice_icon)&gt;&gt;', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
					<item>' This is a placeholder class used to represent an XSD choice defined within the profile...</item>
				</list>		
				<item>hide <xsl:value-of select="$aggregateChoiceClassName"/> attributes</item>
				<xsl:value-of select="concat($sourceClassParent, ' --&gt;  ', $aggregateChoiceClassName)"/>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<xsl:for-each select="a:Reference|a:Instance">
					<!-- Output the association -->	
					<xsl:variable name="targetRoleEndName" select="concat('+', @name)"/>
					<xsl:variable name="targetCardinality">
						<xsl:choose>
							<xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')"><xsl:value-of select="cimtool:association-cardinality(.)"/></xsl:when>
						</xsl:choose>
					</xsl:variable>
					<!-- We filter out character in a class or note name now allowed by PlantUML. This was needed    -->
					<!-- after discovering scenarios where end-users had profile names (i.e. $envelope) with dashes  -->
					<!-- (-) in them leading to have to parse out such character.                                    -->
					<item><xsl:value-of select="concat($aggregateChoiceClassName, ' --&gt;  &quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', @name)"/></item>
				</xsl:for-each>		
			</list>
			<item>&#xD;&#xA;</item> <!-- CR/LF -->
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, Enumerated, and Compound attributes templates)                               -->
	<!-- ============================================================================================================ -->
	
	<xsl:template match="a:Simple">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@cimDatatype, '#')"/> <xsl:value-of select="cimtool:cardinality(.)"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Domain">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@dataType, '#')"/> <xsl:value-of select="cimtool:cardinality(.)"/></item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Compound|a:Enumerated">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant, ' {readOnly}') else ''"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@baseClass, '#')"/> <xsl:value-of select="cimtool:cardinality(.)"/><xsl:value-of select="$constant"/></item>
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
	
	<xsl:template name="generate-fields">
		<xsl:choose>
			<xsl:when test="a:SuperType">
				<xsl:variable name="supertype_name" select="a:SuperType/@name"/>
				<xsl:for-each select="/*/node()[@name = $supertype_name]">
					<xsl:call-template name="generate-fields"/>
				</xsl:for-each>
				<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
		
	<!-- Single parameter version (starts count at 0) -->
	<xsl:function name="cimtool:count-fields" as="xs:integer">
		<xsl:param name="element" as="element()"/>
		<xsl:sequence select="cimtool:count-fields($element, 0)"/>
	</xsl:function>
	
	<!-- Two parameter version (for recursion) -->
	<xsl:function name="cimtool:count-fields" as="xs:integer">
		<xsl:param name="element" as="element()"/>
		<xsl:param name="count" as="xs:integer"/>
		
		<xsl:variable name="total-count" select="
			$count + count($element/(a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice))
		"/>
		
		<xsl:variable name="result-count" select="
			if ($element/a:SuperType) then
				let $baseClass := $element/a:SuperType/@baseClass,
					$parent := root($element)/*/node()[@baseClass = $baseClass]
				return 
					if ($parent) 
					then cimtool:count-fields($parent, $total-count)
					else $total-count
			else
				$total-count
		"/>
		
		<xsl:sequence select="$result-count"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:inherits-from
		Purpose: Checks if an element inherits from a given baseClass (recursively checks SuperType hierarchy)
		Parameters: 
			$element - The element to check (Root or ComplexType)
			$targetBaseClass - The baseClass to check for in the inheritance hierarchy
		Returns: xs:boolean - true if element inherits from targetBaseClass, false otherwise
	-->
	<xsl:function name="cimtool:inherits-from" as="xs:boolean">
		<xsl:param name="element" as="element()"/>
		<xsl:param name="targetBaseClass" as="xs:string"/>
		
		<xsl:variable name="result" select="
			if ($element/@baseClass = $targetBaseClass) then
				(: Direct match - this element has the target baseClass :)
				true()
			else if ($element/a:SuperType) then
				(: Has a parent - check if parent inherits from target :)
				let $superBaseClass := $element/a:SuperType/@baseClass,
					$parent := (root($element)//a:Root[@baseClass = $superBaseClass]|root($element)//a:ComplexType[@baseClass = $superBaseClass])[1]
				return 
					if ($parent) 
					then cimtool:inherits-from($parent, $targetBaseClass)
					else false()
			else
				(: No match and no parent - doesn't inherit :)
				false()
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:get-union-dependencies
		Purpose: Gets all concrete subclasses (Root elements) that inherit from an abstract class (ComplexType)
				 These represent the union members in Avro schema.
		Parameters: 
			$abstract-element - The ComplexType element (abstract class)
		Returns: xs:string* - Sequence of baseClass values for all concrete subclasses
	-->
	<xsl:function name="cimtool:get-union-dependencies" as="xs:string*">
		<xsl:param name="abstract-element" as="element()"/>
		
		<xsl:variable name="abstract-baseClass" select="string($abstract-element/@baseClass)"/>
		
		<xsl:variable name="result" select="
			distinct-values(
				for $root in root($abstract-element)//a:Root
				return
					if (cimtool:inherits-from($root, $abstract-baseClass))
					then string($root/@baseClass)
					else ()
			)
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>

	<!-- 
		Function: cimtool:get-dependencies
		Purpose: Gets all distinct @baseClass values from child elements, 
				 including inherited dependencies from SuperType hierarchy.
				 For Instance/Reference to abstract classes (ComplexType), includes union members.
				 Excludes a:SuperType itself and any types in the exclusion map.
		Parameters: 
			$element - The element to examine (CompoundType, Root, etc.)
			$exclusion-map - Map where keys are baseClass values to exclude
		Returns: xs:string* - Sequence of baseClass values (filtered)
	-->
	<xsl:function name="cimtool:get-dependencies" as="xs:string*">
		<xsl:param name="element" as="element()"/>
		<xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>
		
		<xsl:variable name="result" select="
			distinct-values(
				(
					(: Dependencies from non-Instance/Reference/InverseInstance/InverseReference children. These we need to exclude. :)
					$element/*[@baseClass][not(self::a:SuperType)][not(self::a:Instance)][not(self::a:Reference)][not(self::a:InverseInstance)][not(self::a:InverseReference)]/string(@baseClass)[not(map:contains($exclusion-map, .))],
					
					(: Dependencies from Instance/Reference children - handle unions :)
					for $assoc in $element/(a:Instance|a:Reference)[@baseClass]
					return
						let $assoc-baseClass := string($assoc/@baseClass),
							$referenced-element := root($element)/*/node()[@baseClass = $assoc-baseClass]
						return
							if ($referenced-element/self::a:ComplexType) then
								(: Abstract class - get union members (concrete subclasses) :)
								cimtool:get-union-dependencies($referenced-element)[not(map:contains($exclusion-map, .))]
							else if ($referenced-element/self::a:Root) then
								(: Concrete class - use directly :)
								if (not(map:contains($exclusion-map, $assoc-baseClass))) then
									$assoc-baseClass
								else
									()
							else
								(: Referenced element not found or other type - include if not excluded :)
								if (not(map:contains($exclusion-map, $assoc-baseClass))) then
									$assoc-baseClass
								else
									(),
					
					(: If has SuperType, recursively get parent's dependencies :)
					if ($element/a:SuperType) then
						let $supertype_baseClass := $element/a:SuperType/@baseClass,
							$parent := root($element)/*/node()[@baseClass = $supertype_baseClass]
						return
							if ($parent) then
								cimtool:get-dependencies($parent, $exclusion-map)
							else
								()
					else
						()
				)
			)
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>

    <!-- 
        Function: cimtool:build-dependencies-map
        Purpose: Creates a map of type dependencies for the provided elements
        Parameters: 
            $elements - Node-set of elements to process
            $exclusion-map - Map of baseClass values to exclude from dependencies
        Returns: map(xs:string, xs:string*)
                 - Key: baseClass of each element
                 - Value: Sequence of baseClass values from child elements (filtered)
    -->
    <xsl:function name="cimtool:build-dependencies-map" as="map(xs:string, xs:string*)">
        <xsl:param name="elements" as="element()*"/>
        <xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>
        
        <xsl:variable name="result" select="
            map:merge(
                for $type in $elements[@baseClass]
                return map:entry(
                    string($type/@baseClass),
                    cimtool:get-dependencies($type, $exclusion-map)
                )
            )
        "/>
        
        <xsl:sequence select="$result"/>
    </xsl:function>
    
	<!-- 
		Function: cimtool:create-exclusions-map
		Purpose: Creates a map for excluding types based on their @baseClass attribute
		Parameters: 
			$elements - Node-set of elements whose baseClass should be excluded
		Returns: map(xs:string, xs:boolean)
				 - Key: baseClass attribute value
				 - Value: true()
	-->
	<xsl:function name="cimtool:create-exclusions-map" as="map(xs:string, xs:boolean)">
		<xsl:param name="elements" as="element()*"/>
		
		<xsl:variable name="result" select="
			map:merge(
				for $element in $elements[@baseClass]
				return map:entry(string($element/@baseClass), true())
			)
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:is-abstract-with-subclasses
		Purpose: Checks if a baseClass references an abstract class (ComplexType) that has concrete subclasses (Roots)
		Parameters: 
			$baseClass - The baseClass URI to check
		Returns: xs:boolean - true if it's an abstract class with concrete subclasses, false otherwise
	-->
	<xsl:function name="cimtool:is-abstract-with-subclasses" as="xs:boolean">
		<xsl:param name="baseClass" as="xs:string"/>
		
		<xsl:variable name="is-complex-type" select="exists(root()//a:ComplexType[@baseClass = $baseClass])"/>
		
		<xsl:variable name="result" select="
			if ($is-complex-type) then
				(: Count concrete subclasses :)
				let $concrete-count := count(
					for $root in root()//a:Root
					return
						if (cimtool:inherits-from($root, $baseClass))
						then $root
						else ()
				)
				return $concrete-count > 0
			else
				false()
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>

	<!-- 
		Function: cimtool:topological-sort
		Purpose: Sorts elements in topological order based on their dependencies.
				 Elements with no dependencies come first, then elements that depend only on 
				 already-processed elements, and so on.
				 For circular dependencies, falls back to document order.
		Parameters: 
			$elements - The elements to sort (e.g., //a:CompoundType or //a:Root)
			$deps-map - Dependency map created by cimtool:build-dependencies-map
		Returns: element()* - Sequence of elements in topological (dependency) order
	-->
	<xsl:function name="cimtool:topological-sort" as="element()*">
		<xsl:param name="elements" as="element()*"/>
		<xsl:param name="deps-map" as="map(xs:string, xs:string*)"/>
		
		<xsl:variable name="result" select="cimtool:topological-sort-helper($elements, $deps-map, ())"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>

	<!-- 
		Function: cimtool:topological-sort-helper
		Purpose: Recursive helper for topological sort with accumulated results
		Parameters: 
			$remaining - Elements not yet sorted
			$deps-map - Dependency map
			$sorted - Accumulated sorted elements
		Returns: element()* - Fully sorted sequence
	-->
	<xsl:function name="cimtool:topological-sort-helper" as="element()*">
		<xsl:param name="remaining" as="element()*"/>
		<xsl:param name="deps-map" as="map(xs:string, xs:string*)"/>
		<xsl:param name="sorted" as="element()*"/>
		
		<xsl:choose>
			<xsl:when test="fn:empty($remaining)">
				<!-- Base case: all elements sorted -->
				<xsl:sequence select="$sorted"/>
			</xsl:when>
			<xsl:otherwise>
				<!-- Get baseClasses of already sorted elements -->
				<xsl:variable name="sorted-baseClasses" select="
					for $elem in $sorted 
					return string($elem/@baseClass)
				"/>
				
				<!-- Find elements whose dependencies are all satisfied -->
				<xsl:variable name="ready-elements" select="
					for $elem in $remaining
					return
						let $elem-baseClass := fn:string($elem/@baseClass),
							$elem-deps := $deps-map($elem-baseClass)
						return
							if (every $dep in $elem-deps satisfies $dep = $sorted-baseClasses) then
								$elem
							else
								()
				"/>
				
				<xsl:choose>
					<xsl:when test="fn:exists($ready-elements)">
						<!-- Process ready elements and recurse -->
						<xsl:variable name="new-sorted" select="$sorted, $ready-elements"/>
						<xsl:variable name="new-remaining" select="$remaining except $ready-elements"/>
						<xsl:sequence select="cimtool:topological-sort-helper($new-remaining, $deps-map, $new-sorted)"/>
					</xsl:when>
					<xsl:otherwise>
						<!-- Circular dependency detected - fall back to document order -->
						<xsl:sequence select="$sorted"/>
						<xsl:sequence select="$remaining"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:function>

	<!-- 
		Function: cimtool:get-root-fields
		Purpose: Identifies top-level Root classes that are never referenced by other classes.
				 These are the true "message roots" that should appear at the top level of the schema.
				 Handles both direct references and references through abstract classes (unions).
		Parameters: 
			$all-roots - All a:Root elements in the profile
		Returns: element()* - Sequence of Root elements that are never referenced by any other class
	-->
	<xsl:function name="cimtool:get-root-fields" as="element()*">
		<xsl:param name="all-roots" as="element()*"/>
		
		<!-- Build a set of all baseClass values referenced by Instance/Reference in ANY Root or ComplexType -->
		<xsl:variable name="direct-references" select="
			distinct-values(
				for $element in root($all-roots[1])//(a:Root | a:ComplexType)
				return
					for $child in $element/(a:Instance | a:Reference)
					return string($child/@baseClass)
			)
		"/>
		
		<!-- For each direct reference, find all Root classes that inherit from it -->
		<xsl:variable name="indirect-references" select="
			distinct-values(
				for $ref in $direct-references
				return
					(: Get all Root classes that inherit from this reference :)
					for $root in $all-roots
					return
						if (cimtool:inherits-from($root, $ref))
						then string($root/@baseClass)
						else ()
			)
		"/>
		
		<!-- Combine direct and indirect references -->
		<xsl:variable name="all-references" select="($direct-references, $indirect-references)"/>
		
		<!-- Return roots whose baseClass is NOT in the referenced set -->
		<xsl:variable name="result" select="
			for $root in $all-roots
			return
				if (not(string($root/@baseClass) = $all-references))
				then $root
				else ()
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
	
</xsl:stylesheet>
