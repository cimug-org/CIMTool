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
<xsl:stylesheet exclude-result-prefixes="a" version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cims="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:a="http://langdale.com.au/2005/Message#" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:uml="http://langdale.com.au/2005/UML#" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
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
		<rdf:Description rdf:about="{if (starts-with(@basePackage, concat($ontologyURI, '#'))) then concat('#', substring-after(@basePackage, '#')) else @basePackage }">
			<xsl:for-each select="a:ParentPackage">
				<cims:belongsToCategory rdf:resource="{if (starts-with(@basePackage, concat($ontologyURI, '#'))) then concat('#', substring-after(@basePackage, '#')) else @basePackage }"/>
			</xsl:for-each>
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>
			<rdf:type rdf:resource="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#ClassCategory"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:Root|a:ComplexType|a:EnumeratedType">
		<rdf:Description rdf:about="{if (starts-with(@baseClass, concat($ontologyURI, '#'))) then concat('#', substring-after(@baseClass, '#')) else @baseClass }">
			<cims:belongsToCategory rdf:resource="{if (starts-with(@packageURI, concat($ontologyURI, '#'))) then concat('#', substring-after(@packageURI, '#')) else @packageURI }"/>
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>
			<rdf:type rdf:resource="http://www.w3.org/2000/01/rdf-schema#Class"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<!-- For top level classes (i.e. a:Root, a:ComplexType, a:EnumeratedType) we obtain the name 
				 of the class for the rdfs:label using the @baseClass attribute. This is different than 
				 what appears later in this XSLT for attributes and associations where the rdfs:label is 
				 determined using the @name attribute -->
			<rdfs:label><xsl:value-of select="substring-after(@baseClass, '#')"/></rdfs:label>
			<xsl:if test="a:SuperType">
				<!-- The name of the superclass is determined using the @baseClass attribute 
					 (and not the a:SuperType/@name) -->
				<rdfs:subClassOf rdf:resource="{if (starts-with(a:SuperType/@baseClass, concat($ontologyURI, '#'))) then concat('#', substring-after(a:SuperType/@baseClass, '#')) else a:SuperType/@baseClass }"/>
			</xsl:if>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:SimpleType">	
		<rdf:Description rdf:about="{if (starts-with(@dataType, concat($ontologyURI, '#'))) then concat('#', substring-after(@dataType, '#')) else @dataType }">
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>
			<rdf:type rdf:resource="http://www.w3.org/2000/01/rdf-schema#Class"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
		</rdf:Description>
  	</xsl:template>
	<!-- ============================================================================================================ -->
	<!-- END SECTION:  Package, Complex, ComplexType, Root, & CompoundType(s) TYPE DEFINITION templates               -->
	<!-- ============================================================================================================ -->	
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, Instance, Reference attribute & association templates)                      -->
	<!-- ============================================================================================================ -->
	<xsl:template match="a:Simple">
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, concat($ontologyURI, '#'))) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<cims:dataType rdf:resource="{if (starts-with(@dataType, concat($ontologyURI, '#'))) then concat('#', substring-after(@dataType, '#')) else @dataType }"/>
			<cims:multiplicity rdf:resource="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#M:{concat(@minOccurs, '..', replace(@maxOccurs, 'unbounded', 'n'))}"/>
			<!-- This conditional is a "safety check" in the event that a a:Stereotype of type #attribute doesn't already 
				 exist. In that case we ensure we add it. This since all a:Domain elements are attributes. -->
			<xsl:if test="not(a:Stereotype[contains(., '#attribute')])">
				<cims:stereotype rdf:resource="http://langdale.com.au/2005/UML#attribute"/>
			</xsl:if>
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>
			<rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@basePropertyClass, concat($ontologyURI, '#'))) then concat('#', substring-after(@basePropertyClass, '#')) else @basePropertyClass }"/>
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
		</rdf:Description>
  	</xsl:template>
  	
	<xsl:template match="a:Domain">
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, concat($ontologyURI, '#'))) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<cims:dataType rdf:resource="{if (starts-with(@dataType, concat($ontologyURI, '#'))) then concat('#', substring-after(@dataType, '#')) else @dataType }"/>
			<cims:multiplicity rdf:resource="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#M:{concat(@minOccurs, '..', replace(@maxOccurs, 'unbounded', 'n'))}"/>
			<!-- This conditional is a "safety check" in the event that a a:Stereotype of type #attribute doesn't already 
				 exist. In that case we ensure we add it. This since all a:Domain elements are attributes. -->
			<xsl:if test="not(a:Stereotype[contains(., '#attribute')])">
				<cims:stereotype rdf:resource="http://langdale.com.au/2005/UML#attribute"/>
			</xsl:if>
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>
			<rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@basePropertyClass, concat($ontologyURI, '#'))) then concat('#', substring-after(@basePropertyClass, '#')) else @basePropertyClass }"/>
			<!-- Note: this is intentionally using the @name attribute to source the label for a 
				 domain attribute defined for the class. -->
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
		</rdf:Description>
	</xsl:template>

	<!-- ======================================================================= -->
	<!-- Elements of type a:Instance are always associations and not attributes. -->
	<!-- ======================================================================= -->
	<xsl:template match="a:Instance">
		<!-- Create the association -->
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, concat($ontologyURI, '#'))) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<cims:multiplicity rdf:resource="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#M:{concat(@minOccurs, '..', replace(@maxOccurs, 'unbounded', 'n'))}"/>
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>	
			<rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@inverseBasePropertyClass, concat($ontologyURI, '#'))) then concat('#', substring-after(@inverseBasePropertyClass, '#')) else @inverseBasePropertyClass }"/>
			<!-- Note: this is intentionally using the @name attribute to source the label for an 
				 association or attribute within the class. -->
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
			<rdfs:range rdf:resource="#{@type}"/>
		</rdf:Description>
	</xsl:template>

	<xsl:template match="a:Reference">
		<!-- Create the association -->
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, concat($ontologyURI, '#'))) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<cims:multiplicity rdf:resource="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#M:{concat(@minOccurs, '..', replace(@maxOccurs, 'unbounded', 'n'))}"/>
			<!-- When an @inverseBaseProperty is missing it is a clear indicator that this is not actually an association but
				 rather is an attribute that the end user has not pulled in the type for in CIMTool while constructing
				 the profile. When that happens it appears as an a:Reference element and therefore the below conditional is 
				 required.  
			-->
			<xsl:if test="not(@inverseBaseProperty) and not(a:Stereotype[contains(., '#attribute')])">
				<cims:stereotype rdf:resource="http://langdale.com.au/2005/UML#attribute"/>
			</xsl:if>
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>			
			<rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@inverseBasePropertyClass, concat($ontologyURI, '#'))) then concat('#', substring-after(@inverseBasePropertyClass, '#')) else @inverseBasePropertyClass }"/>
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
			<rdfs:range rdf:resource="#{@type}"/>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:Enumerated">	
		<rdf:Description rdf:about="{if (starts-with(@baseProperty, concat($ontologyURI, '#'))) then concat('#', substring-after(@baseProperty, '#')) else @baseProperty }">
			<cims:multiplicity rdf:resource="http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#M:{concat(@minOccurs, '..', replace(@maxOccurs, 'unbounded', 'n'))}"/>
			<xsl:if test="not(a:Stereotype[contains(., '#attribute')])">
				<cims:stereotype rdf:resource="http://langdale.com.au/2005/UML#attribute"/>
			</xsl:if>
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>
			<rdf:type rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<rdfs:domain rdf:resource="{if (starts-with(@basePropertyClass, concat($ontologyURI, '#'))) then concat('#', substring-after(@basePropertyClass, '#')) else @basePropertyClass }"/>
			<!-- Note: this is intentionally using the @name attribute to source the label for an 
				 enumerated attribute within a class. -->
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
			<rdfs:range rdf:resource="#{@type}"/>
		</rdf:Description>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedValue">
		<rdf:Description rdf:about="{if (starts-with(@baseResource, concat($ontologyURI, '#'))) then concat('#', substring-after(@baseResource, '#')) else @baseResource }">
			<xsl:for-each select="a:Stereotype">
				<xsl:sort select="." order="ascending" />
				<xsl:variable name="stereotype" select="."/>
				<cims:stereotype rdf:resource="{$stereotype}"/>
			</xsl:for-each>	
			<xsl:variable name="type" select="substring-before(@baseResource, concat('.', @name))"/>
			<rdf:type rdf:resource="{if (starts-with($type, concat($ontologyURI, '#'))) then concat('#', substring-after($type, '#')) else $type }"/>
			<xsl:if test="a:Comment|a:Note"><rdfs:comment><xsl:apply-templates select="." mode="comments"/></rdfs:comment></xsl:if>
			<rdfs:label><xsl:value-of select="@name"/></rdfs:label>
		</rdf:Description>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<!-- END SECTION:  (ComplexType, Root, and EnumeratedType attribute & association templates)                    -->
	<!-- ============================================================================================================ -->

<!--
	<xsl:template match="a:Complex">
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
			<xs:complexType sawsdl:modelReference="{@baseClass}">
				<xsl:call-template name="type_body"/>
			</xs:complexType>
		</xs:element>
	</xsl:template>
	
	<xsl:template match="a:Choice[a:Stereotype='http://langdale.com.au/2005/UML#preserve']">
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
			<xs:complexType>
				<xs:choice>
					<xsl:apply-templates/>
				</xs:choice>
			</xs:complexType>
		</xs:element>
	</xsl:template>
	<xsl:template match="a:SimpleEnumerated">
		<xs:element name="{@name}" minOccurs="{@minOccurs}" maxOccurs="{@maxOccurs}" sawsdl:modelReference="{@baseProperty}">
			<xsl:call-template name="annotate"/>
			<xs:simpleType sawsdl:modelReference="{@baseClass}">
				<xs:restriction base="xs:string">
					<xsl:apply-templates/>
				</xs:restriction>
			</xs:simpleType>
		</xs:element>
	</xsl:template>
	<xsl:template match="a:CompoundType" mode="declare">
		<xs:complexType name="{@name}" sawsdl:modelReference="{@baseClass}">
			<xsl:call-template name="annotate"/>
			<xsl:call-template name="type_body"/>
		</xs:complexType>
	</xsl:template>
	-->
	<xsl:template match="." mode="comments">
		<xsl:for-each select="a:Comment|a:Note">
			<xsl:value-of select="."/><xsl:if test="position()!=last()"><xsl:value-of select="$newline"/></xsl:if>
		</xsl:for-each>
	</xsl:template>
	<xsl:template match="text()">
		<!-- dont pass text through -->
	</xsl:template>
</xsl:stylesheet>