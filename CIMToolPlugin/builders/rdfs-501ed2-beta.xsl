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
<xsl:stylesheet exclude-result-prefixes="a m" version="3.0" xmlns:m="http://langdale.com.au/2005/Sample#" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cims="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:a="http://langdale.com.au/2005/Message#" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:uml="http://iec.ch/TC57/NonStandard/UML#" xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <xsl:output xmlns:xalan="http://xml.apache.org/xslt" method="xml" omit-xml-declaration="no" indent="yes" xalan:indent-amount="4" />
    <xsl:param name="copyright" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<!-- The following <xsl:text> element is our newline representation where needed. -->
    <xsl:param name="newline"><xsl:text>
</xsl:text></xsl:param>

	<xsl:template match="a:Catalog">
		<xsl:value-of select="$newline"/>
		<xsl:if test="$copyright and $copyright != ''">
			<xsl:comment><xsl:value-of select="$newline"/><xsl:value-of select="$copyright" disable-output-escaping="yes"/><xsl:value-of select="$newline"/></xsl:comment>	
		</xsl:if>
		<xsl:value-of select="$newline"/>
		<rdf:RDF>
			<xsl:attribute name="xml:base" select="$ontologyURI"/>
			<xsl:namespace name="m"><xsl:value-of select="$baseURI"/></xsl:namespace>
			<xsl:variable name="rdf" as="node()*">
				<xsl:apply-templates select=".//*"/>
			</xsl:variable>
			<xsl:perform-sort select="$rdf">
				<xsl:sort select="@rdf:about"/>
			</xsl:perform-sort>
		</rdf:RDF>
	</xsl:template>

	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Packages, ComplexType, Root, EnumeratedType, SimpleType, Complex, & CompoundType(s)         -->
	<!-- templates) i.e. All "top level" element in the XML profile definition file...                                -->
	<!-- ============================================================================================================ -->
	<xsl:template match="a:Package">
		<rdf:Description rdf:about="{if (starts-with(@basePackage, $ontologyURI)) then concat('#', substring-after(@basePackage, '#')) else @basePackage }">
			<xsl:for-each select="a:ParentPackage">
				<cims:belongsToCategory rdf:resource="{if (starts-with(@basePackage, $ontologyURI)) then concat('#', substring-after(@basePackage, '#')) else @basePackage }"/>
			</xsl:for-each>
			<rdf:type rdf:resource="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#ClassCategory"/>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:Root|a:ComplexType">
		<rdf:Description rdf:about="{if (starts-with(@baseClass, $ontologyURI)) then concat('#', substring-after(@baseClass, '#')) else @baseClass }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Class"/>
			<rdfs:label xml:lang="en"><xsl:value-of select="substring-after(@baseClass, '#')"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<xsl:if test="a:SuperType">
				<rdfs:subClassOf rdf:resource="{if (starts-with(a:SuperType/@baseClass, $ontologyURI)) then concat('#', substring-after(a:SuperType/@baseClass, '#')) else a:SuperType/@baseClass }"/>
			</xsl:if>
			<m:Package><xsl:value-of select="if (starts-with(@packageURI, $ontologyURI)) then substring-after(@packageURI, '#') else @packageURI"/></m:Package>
			<xsl:if test="not(a:Stereotype[@label='Concrete'])">
				<m:isAbstract>True</m:isAbstract>
			</xsl:if>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedType">
		<rdf:Description rdf:about="{if (starts-with(@baseClass, $ontologyURI)) then concat('#', substring-after(@baseClass, '#')) else @baseClass }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Class"/>
			<rdfs:label xml:lang="en"><xsl:value-of select="substring-after(@baseClass, '#')"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<m:Package><xsl:value-of select="if (starts-with(@packageURI, $ontologyURI)) then substring-after(@packageURI, '#') else @packageURI"/></m:Package>
			<rdfs:subClassOf rdf:resource="#Enumeration"/>
			<owl:oneOf rdf:parseType="Collection">
				<xsl:for-each select="a:EnumeratedValue">
					<owl:Thing rdf:about="{if (starts-with(@baseResource, $ontologyURI)) then concat('#', substring-after(@baseResource, '#')) else @baseResource }"/>
				</xsl:for-each>
			</owl:oneOf>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:SimpleType">	
		<rdf:Description rdf:about="{if (starts-with(@dataType, $ontologyURI)) then concat('#', substring-after(@dataType, '#')) else @dataType }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Class"/>
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<m:Package><xsl:value-of select="if (starts-with(@packageURI, $ontologyURI)) then substring-after(@packageURI, '#') else @packageURI"/></m:Package>
			<xsl:if test="not(a:Stereotype[@label='CIMDatatype'])">
				<m:isCIMDatatype>True</m:isCIMDatatype>
			</xsl:if>
		</rdf:Description>
  	</xsl:template>
	<!-- ============================================================================================================ -->
	<!-- END SECTION:  Package, Complex, ComplexType, Root, & CompoundType(s) TYPE DEFINITION templates               -->
	<!-- ============================================================================================================ -->	
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, Instance, Reference attribute & association templates)                      -->
	<!-- ============================================================================================================ -->
	
	<xsl:template match="a:Simple">
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, $ontologyURI)) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#DatatypeProperty"/>
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#FunctionalProperty"/>
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<rdfs:range rdf:resource="{if (starts-with(@dataType, $ontologyURI)) then concat('#', substring-after(@dataType, '#')) else @dataType }"/>
			<rdfs:domain rdf:resource="{if (starts-with(@basePropertyClass, $ontologyURI)) then concat('#', substring-after(@basePropertyClass, '#')) else @basePropertyClass }"/>
			<m:Package><xsl:value-of select="if (starts-with(@packageURI, $ontologyURI)) then substring-after(@packageURI, '#') else @packageURI"/></m:Package>
		</rdf:Description>
  	</xsl:template>
  	
	<xsl:template match="a:Domain">
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, $ontologyURI)) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#DatatypeProperty"/>
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#FunctionalProperty"/>
			<!-- Note: this is intentionally using the @name attribute to source the label for a domain attribute defined for the class. -->
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<rdfs:range rdf:resource="{if (starts-with(@dataType, $ontologyURI)) then concat('#', substring-after(@dataType, '#')) else @dataType }"/>
			<rdfs:domain rdf:resource="{if (starts-with(@basePropertyClass, $ontologyURI)) then concat('#', substring-after(@basePropertyClass, '#')) else @basePropertyClass }"/>
		</rdf:Description>
	</xsl:template>

	<!-- ======================================================================= -->
	<!-- Elements of type a:Instance are always associations and not attributes. -->
	<!-- ======================================================================= -->
	<xsl:template match="a:Instance">
		<!-- Create the association -->
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, $ontologyURI)) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#ObjectProperty"/>
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#FunctionalProperty"/>
			<!-- Note: this is intentionally using the @name attribute to source the label for an association or attribute within the class. -->
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@inverseBasePropertyClass, $ontologyURI)) then concat('#', substring-after(@inverseBasePropertyClass, '#')) else @inverseBasePropertyClass }"/>
			<rdfs:range rdf:resource="#{@type}"/>
			<xsl:if test="@inverseBaseProperty">
				<owl:inverseOf rdf:resource="{if (starts-with(@inverseBaseProperty, $ontologyURI)) then concat('#', substring-after(@inverseBaseProperty, '#')) else @inverseBaseProperty }"/>
			</xsl:if>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:Reference">
		<!-- Create the association -->
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, $ontologyURI)) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#ObjectProperty"/>
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#FunctionalProperty"/>
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@inverseBasePropertyClass, $ontologyURI)) then concat('#', substring-after(@inverseBasePropertyClass, '#')) else @inverseBasePropertyClass }"/>
			<rdfs:range rdf:resource="#{@type}"/>
			<xsl:if test="@inverseBaseProperty">
				<owl:inverseOf rdf:resource="{if (starts-with(@inverseBaseProperty, $ontologyURI)) then concat('#', substring-after(@inverseBaseProperty, '#')) else @inverseBaseProperty }"/>
			</xsl:if>
		</rdf:Description>
	</xsl:template>

	<!-- ===========================================================================================================================
		Create the inverse association.  All attributes need to be present to create the rdf:Description entry. Note that in CIMTool
		there can be certain scenarios where an end user may neglect to pull in the type...such as with an attribute that is an 
		Enumeration. When correctly pulled in the entry appears as an a:Enumerated and the template (below) would be used in which 
		case an inverse association is not relevant at all. However, when an end user neglects to do so it appears as an a:Reference
		but such an entry has no @inverse* attributes.  Therefore, we conditionally check for such a scenario and in that case we
		do not generate an inverse association. 
		============================================================================================================================ -->
	<xsl:template match="a:InverseInstance">
		<!-- Create the primary association -->
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, $ontologyURI)) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#ObjectProperty"/>
			<!-- Note: this is intentionally using the @name attribute to source the label for an 
				 association or attribute within the class. -->
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@inverseBasePropertyClass, $ontologyURI)) then concat('#', substring-after(@inverseBasePropertyClass, '#')) else @inverseBasePropertyClass }"/>
			<rdfs:range rdf:resource="#{@type}"/>
			<xsl:if test="@inverseBaseProperty">
				<cims:inverseRoleName rdf:resource="{if (starts-with(@inverseBaseProperty, $ontologyURI)) then concat('#', substring-after(@inverseBaseProperty, '#')) else @inverseBaseProperty }"/>
			</xsl:if>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:InverseReference">
		<!-- Create the association -->
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, $ontologyURI)) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#ObjectProperty"/>
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@inverseBasePropertyClass, $ontologyURI)) then concat('#', substring-after(@inverseBasePropertyClass, '#')) else @inverseBasePropertyClass }"/>
			<rdfs:range rdf:resource="#{@type}"/>
			<xsl:if test="@inverseBaseProperty">
				<cims:inverseRoleName rdf:resource="{if (starts-with(@inverseBaseProperty, $ontologyURI)) then concat('#', substring-after(@inverseBaseProperty, '#')) else @inverseBaseProperty }"/>
			</xsl:if>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:Enumerated">	
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, $ontologyURI)) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#DatatypeProperty"/>
			<!-- Note: this is intentionally using the @name attribute to source the label for an enumerated attribute within a class. -->
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
			<rdfs:domain rdf:resource="{if (starts-with(@basePropertyClass, $ontologyURI)) then concat('#', substring-after(@basePropertyClass, '#')) else @basePropertyClass }"/>
			<rdfs:range rdf:resource="#{@type}"/>
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedValue">
		<rdf:Description rdf:about="{if (starts-with(@baseResource, $ontologyURI)) then concat('#', substring-after(@baseResource, '#')) else @baseResource }">
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#NamedIndividual"/>
			<rdfs:label xml:lang="en"><xsl:value-of select="@name"/></rdfs:label>
			<rdfs:domain rdf:resource="{if (starts-with(@baseResource, $ontologyURI)) then concat('#', substring-before(substring-after(@baseResource, '#'), '.')) else @baseResource }"/>
			<!-- Need to deal with the namespace prefix for any m: in this builder -->
			<m:isenum>True</m:isenum>			
			<xsl:if test="a:Comment|a:Note"><skos:definition xml:lang="en"><xsl:apply-templates select="." mode="comments"/></skos:definition></xsl:if>
		</rdf:Description>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<!-- END SECTION:  (ComplexType, Root, and EnumeratedType attribute & association templates)                    -->
	<!-- ============================================================================================================ -->

	<xsl:template match="." mode="comments">
		<xsl:for-each select="a:Comment|a:Note">
			<xsl:value-of select="."/><xsl:if test="position()!=last()"><xsl:value-of select="$newline"/></xsl:if>
		</xsl:for-each>
	</xsl:template>
	<xsl:template match="text()">
		<!-- dont pass text through -->
	</xsl:template>
</xsl:stylesheet>