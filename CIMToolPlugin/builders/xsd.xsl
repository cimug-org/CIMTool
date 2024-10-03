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
<xsl:stylesheet exclude-result-prefixes="a" version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:a="http://langdale.com.au/2005/Message#" xmlns:sawsdl="http://www.w3.org/ns/sawsdl">
    <xsl:output xmlns:xalan="http://xml.apache.org/xslt" method="xml" omit-xml-declaration="no" indent="yes" xalan:indent-amount="4" />
    <xsl:param name="copyright" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<!-- The following <xsl:text> element is our newline representation where needed. -->
    <xsl:param name="newline"><xsl:text>
</xsl:text></xsl:param>
	<xsl:template match="a:Catalog">
		<xsl:value-of select="$newline"/>
		<!-- the top level template generates the xml schema element -->
		<xs:schema targetNamespace="{$baseURI}" elementFormDefault="qualified" attributeFormDefault="unqualified">
			<!-- copy through namespace declaration needed to reference local types -->
			<xsl:for-each select="namespace::*">
				<xsl:if test="not(contains(., 'langdale.com.au'))">
					<xsl:copy/>
				</xsl:if>
			</xsl:for-each>
			<xsl:value-of select="$newline"/>
			<xsl:if test="$copyright and $copyright != ''">
				<xs:annotation>
					<xs:documentation xml:lang="en">
					<xsl:value-of select="$copyright" disable-output-escaping="yes"/>				
					</xs:documentation>
				</xs:annotation>
			</xsl:if>
			<xsl:call-template name="annotate"/>
			<!-- if the message declares root elements put them in an envelope -->
			<xsl:if test="a:Root">
				<xs:element name="{$envelope}" type="m:{$envelope}">
				</xs:element>
				<xs:complexType name="{$envelope}">
					<xs:sequence>
						<xsl:apply-templates select="a:Root"/>
					</xs:sequence>
				</xs:complexType>
			</xsl:if>
			<xsl:apply-templates select="a:Message"/>
			<xsl:apply-templates mode="declare"/>
		</xs:schema>
	</xsl:template>
	<xsl:template match="a:Message">
		<!-- generates an envelope element -->
		<xs:element name="{@name}">
			<xsl:call-template name="annotate"/>
			<xs:complexType>
				<xs:sequence>
					<xsl:apply-templates/>
				</xs:sequence>
			</xs:complexType>
		</xs:element>
	</xsl:template>
	<xsl:template match="a:Root">
		<!-- generates the payload element definitions -->
		<xs:element name="{@name}" type="m:{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}"/>
	</xsl:template>
	<xsl:template match="a:Complex">
		<!-- generates a nested element with anonymous complex type declared inline -->
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
			<xs:complexType sawsdl:modelReference="{@baseClass}">
				<xsl:call-template name="type_body"/>
			</xs:complexType>
		</xs:element>
	</xsl:template>
	<xsl:template match="a:Choice[a:Stereotype='http://langdale.com.au/2005/UML#preserve']">
		<!-- generates a nested element with choice of sub-elements -->
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
			<xs:complexType>
				<xs:choice>
					<xsl:apply-templates/>
				</xs:choice>
			</xs:complexType>
		</xs:element>
	</xsl:template>
	<xsl:template match="a:Choice">
		<!-- generates a nested element with choice of sub-elements -->
		<xs:choice minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
			<xsl:apply-templates/>
		</xs:choice>
	</xsl:template>
	<xsl:template match="a:Instance|a:Domain|a:Enumerated">
		<!-- generates a nested instance of a type declared elsewhere -->
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" type="m:{@type}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
		</xs:element>
	</xsl:template>
	<xsl:template match="a:SimpleEnumerated">
		<!-- declares a nested element with anonymous enumerated type -->
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
			<xs:simpleType sawsdl:modelReference="{@baseClass}">
				<xs:restriction base="xs:string">
					<xsl:apply-templates/>
				</xs:restriction>
			</xs:simpleType>
		</xs:element>
	</xsl:template>
	<xsl:template match="a:Simple">
		<!-- generates a nested element for an xsd part 2 simple type -->
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" type="xs:{@xstype}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
		</xs:element>
	</xsl:template>
	<xsl:template match="a:Reference">
		<!-- generates a reference to an object in the model -->
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
			<xs:complexType sawsdl:modelReference="{@baseClass}">
				<xs:attribute name="ref" type="xs:string" use="required"/>
				<xs:attribute name="referenceType" type="xs:string" use="optional"/>
			</xs:complexType>
		</xs:element>
	</xsl:template>
	<xsl:template name="type_body">
		<!-- generates the meat of a complex type, including an extension if necessary -->
		<xsl:choose>
			<xsl:when test="a:SuperType">
				<xs:complexContent>
					<xs:extension base="m:{a:SuperType/@name}">
						<xs:sequence>
							<xsl:apply-templates/>
						</xs:sequence>
					</xs:extension>
				</xs:complexContent>
			</xsl:when>
			<xsl:otherwise>
				<xs:sequence>
					<xsl:apply-templates/>
				</xs:sequence>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="a:ComplexType|a:Root|a:CompoundType" mode="declare">
		<xs:complexType name="{@name}" sawsdl:modelReference="{@baseClass}">
			<xsl:call-template name="annotate"/>
			<xsl:call-template name="type_body"/>
		</xs:complexType>
	</xsl:template>
	<xsl:template match="a:SimpleType" mode="declare">
		<!-- declares a a CIM domain type in terms of an xsd part 2 simple type -->
		<xs:simpleType name="{@name}" sawsdl:modelReference="{@dataType}">
			<xsl:call-template name="annotate"/>
			<xs:restriction base="xs:{@xstype}"/>
		</xs:simpleType>
	</xsl:template>
	<xsl:template match="a:EnumeratedType" mode="declare">
		<!-- declares an enumerated type -->
		<xs:simpleType name="{@name}" sawsdl:modelReference="{@baseClass}">
			<xsl:call-template name="annotate"/>
			<xs:restriction base="xs:string">
				<xsl:apply-templates/>
			</xs:restriction>
		</xs:simpleType>
	</xsl:template>
	<xsl:template match="a:EnumeratedValue">
		<!-- declares one value within an enumerated type -->
		<xs:enumeration value="{@name}">
			<xsl:call-template name="annotate"/>
		</xs:enumeration>
	</xsl:template>
	<xsl:template name="annotate">
		<!-- generate and annotation -->
		<xs:annotation>
			<xsl:apply-templates mode="annotate"/>
		</xs:annotation>
	</xsl:template>
	<xsl:template match="a:Comment|a:Note" mode="annotate">
		<!-- generate human readable annotation -->
		<xs:documentation>
			<xsl:value-of select="."/>
		</xs:documentation>
	</xsl:template>
	<xsl:template match="text()">
		<!-- dont pass text through -->
	</xsl:template>
	<xsl:template match="node()" mode="annotate">
		<!-- dont pass any defaults in annotate mode -->
	</xsl:template>
	<xsl:template match="node()" mode="declare">
		<!-- dont pass any defaults in declare mode -->
	</xsl:template>
</xsl:stylesheet>