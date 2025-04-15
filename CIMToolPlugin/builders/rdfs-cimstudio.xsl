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
<xsl:stylesheet exclude-result-prefixes="a map xsd" 
	version="3.0" 
	xmlns:xsd="http://www.w3.org/2001/XMLSchema" 	
	xmlns:map="http://www.w3.org/2005/xpath-functions/map" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:a="http://langdale.com.au/2005/Message#" 
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
	xmlns:cimschema="http://osii.com/cimschema/v1#">
	<xsl:output xmlns:xalan="http://xml.apache.org/xslt" method="xml" omit-xml-declaration="no" indent="yes" xalan:indent-amount="4" />
	<xsl:param name="includesFileAbsolutePath" />
	<xsl:param name="includesFilesAbsoluteBasePath" />
	<xsl:param name="copyright" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<!-- The following <xsl:text> element is our newline representation where needed. -->
	<xsl:param name="newline"><xsl:text>
</xsl:text></xsl:param>
	
	<xsl:include href="file:///D:/CIMTool-2.3.0-beta/configuration/au.com.langdale.cimtoole/builders/includes/rdfs-cimstudio-namespaces.xsl" />
	
	<xsl:variable name="primitive-types" as="map(xsd:string, xsd:string)">
		<xsl:map>
			<!-- Standard domain related CIM <<Primitive>> classes -->
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#boolean'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.Boolean'"/>
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#decimal'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.Double'"/>
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#float'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.Double'"/>
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#integer'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.Int'"/>	
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#string'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.String'"/>
			<!-- Time related CIM <<Primitive>> classes -->
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#date'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.String'"/>
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#time'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.String'"/>
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#duration'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.String'"/>
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#dateTime'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.String'"/>
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#gMonthDay'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.String'"/>
			<!-- The next set I did not identify within the CIM Studio schema -->
			<xsl:map-entry key="'http://www.w3.org/2001/XMLSchema#anyURI'" select="'http://osii.com/cimschema/v1#PrimitiveTypes.URI'"/>
		</xsl:map>
    </xsl:variable>
    
	<xsl:template name="primitive-type">
		<xsl:param name="dataType"/>
		<xsl:value-of select="if (map:contains($primitive-types, $dataType)) then (map:get($primitive-types, $dataType)) else (if (starts-with($dataType, concat($ontologyURI, '#'))) then 'http://osii.com/cimschema/v1#PrimitiveTypes.Double' else 'Unknown Primitive Type')" />	
	</xsl:template>
    
	<xsl:template name="packages-hierarchy">
		<xsl:param name="package"/>
		<xsl:variable name="parentPackage" select="//a:Package[@name = $package/a:ParentPackage/@name]" />
		<xsl:choose>
			<xsl:when test="$parentPackage"><xsl:call-template name="packages-hierarchy"><xsl:with-param name="package" select="$parentPackage"/></xsl:call-template>.<xsl:value-of select="$package/@name"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$package/@name"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="a:Catalog">
		<xsl:value-of select="$newline"/>
		<xsl:if test="$copyright and $copyright != ''">
			<xsl:comment><xsl:value-of select="$newline"/><xsl:value-of select="$copyright" disable-output-escaping="yes"/><xsl:value-of select="$newline"/></xsl:comment>	
		</xsl:if>
		<xsl:value-of select="$newline"/>
		<rdf:RDF>
			<xsl:call-template name="namespaces"/>
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

	<xsl:template match="a:Root|a:ComplexType|a:CompoundType">
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="concat(substring-before(@baseClass, '#'), '#')"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true' and not(a:Stereotype[contains(., '#shadowextension')])">
			<xsl:variable name="packageName" select="@package"/>
			<xsl:variable name="packagesHierarchy">
				<xsl:call-template name="packages-hierarchy"><xsl:with-param name="package" select="//a:Package[@name = $packageName]"/></xsl:call-template>
			</xsl:variable>
			<xsl:variable name="namespace-uuid">
				<xsl:call-template name="namespace-uuid"><xsl:with-param name="uri" select="concat(substring-before(@baseClass, '#'), '#')"/></xsl:call-template>
			</xsl:variable>
			<cimschema:ClassType rdf:ID="_{@ea_guid}">
				<cimschema:IdentifiedObject.description><xsl:value-of select="$packagesHierarchy"/></cimschema:IdentifiedObject.description>
				<cimschema:IdentifiedObject.name><xsl:value-of select="@name" /></cimschema:IdentifiedObject.name>
				<cimschema:NamespaceElement.namespace rdf:resource="#_{$namespace-uuid}" />
				<xsl:variable name="class" select="substring-before(@baseClass, '#')"/> 
				<xsl:for-each select="a:SuperType">
					<xsl:variable name="superType" select="@baseClass"/>
					<!-- 
						The select query for the $baseType variable will filter out any shadow extension 
						classes. To do so it must test for one of two ways that such shadow extensions to 
						normative CIM class can be defined.
						
						The first test is straightforward in that it filters out any super classes 
						from the list of baseTypes that explicitly have the <<ShadowExtension>> stereotype:
						
							not(a:Stereotype[contains(., '#shadowextension')])
						
						The second test is for super classes of a normative CIM class namespaced with the 
						standard CIM namespace (i.e. $ontologyURI) but which have a namespace that is not the
						standard CIM namespace. In this case, even though the <<ShadowExtension>> stereotype 
						is not declared on the super class, per the published CIM Modeling Guidelines. we know  
						that this is not allowed other than for shadow extension classes of the normative CIM 
						class. This part of the select filters out such shadow classes so that the do not get  
						added to the baseTypes list:
						
							not(starts-with($class, concat($ontologyURI, '#')) and not(starts-with(@baseClass, concat($ontologyURI, '#'))))
					-->
					<xsl:variable name="baseType" select="//a:ComplexType[(@baseClass = $superType) and not(a:Stereotype[contains(., '#shadowextension')]) and not(starts-with($class, concat($ontologyURI, '#')) and not(starts-with(@baseClass, concat($ontologyURI, '#'))))]|//a:Root[(@baseClass = $superType) and not(a:Stereotype[contains(., '#shadowextension')]) and not(starts-with($class, concat($ontologyURI, '#')) and not(starts-with(@baseClass, concat($ontologyURI, '#'))))]|//a:CompoundType[(@baseClass = $superType) and not(a:Stereotype[contains(., '#shadowextension')]) and not(starts-with($class, concat($ontologyURI, '#')) and not(starts-with(@baseClass, concat($ontologyURI, '#'))))]"/>
					<xsl:if test="$baseType and $baseType != ''">
						<cimschema:ClassType.baseTypes rdf:resource="#_{$baseType/@ea_guid}" />
					</xsl:if>
				</xsl:for-each>
				<xsl:variable name="baseClass" select="@baseClass"/>
				<xsl:variable name="derived-types" select="//a:ComplexType[a:SuperType/@baseClass = $baseClass]|//a:Root[a:SuperType/@baseClass = $baseClass]|//a:CompoundType[a:SuperType/@baseClass = $baseClass]"/>
				<xsl:for-each select="$derived-types">
					<cimschema:ClassType.derivedTypes rdf:resource="#_{@ea_guid}" />
				</xsl:for-each>
			</cimschema:ClassType>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedType">
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="concat(substring-before(@baseClass, '#'), '#')"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true' and not(a:Stereotype[contains(., '#shadowextension')])">
			<xsl:variable name="ea_guid" select="@ea_guid"/>
			<xsl:variable name="namespace-uuid">
				<xsl:call-template name="namespace-uuid"><xsl:with-param name="uri" select="concat(substring-before(@baseClass, '#'), '#')"/></xsl:call-template>
			</xsl:variable>
			<cimschema:EnumerationType rdf:ID="_{@ea_guid}">
				<cimschema:IdentifiedObject.name><xsl:value-of select="@name" /></cimschema:IdentifiedObject.name>
				<cimschema:NamespaceElement.namespace rdf:resource="#_{$namespace-uuid}" />
			</cimschema:EnumerationType>
			<xsl:for-each select="a:EnumeratedValue">
				<xsl:variable name="namespace-uuid">
					<xsl:call-template name="namespace-uuid"><xsl:with-param name="uri" select="concat(substring-before(@baseResource, '#'), '#')"/></xsl:call-template>
				</xsl:variable>
				<cimschema:EnumerationValue rdf:ID="_{@ea_guid}">
					<cimschema:IdentifiedObject.name><xsl:value-of select="@name" /></cimschema:IdentifiedObject.name>
					<cimschema:NamespaceElement.namespace rdf:resource="#_{$namespace-uuid}" />
					<cimschema:EnumerationValue.enumeration rdf:resource="#_{$ea_guid}" />
				</cimschema:EnumerationValue>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:SimpleType|a:PrimitiveType">
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="concat(substring-before(@baseClass, '#'), '#')"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<xsl:variable name="packageName" select="@package"/>
			<xsl:variable name="packagesHierarchy">
				<xsl:call-template name="packages-hierarchy"><xsl:with-param name="package" select="//a:Package[@name = $packageName]"/></xsl:call-template>
			</xsl:variable>
			<xsl:variable name="namespace-uuid">
				<xsl:call-template name="namespace-uuid"><xsl:with-param name="uri" select="concat(substring-before(@dataType, '#'), '#')"/></xsl:call-template>
			</xsl:variable>
			<cimschema:ValueType rdf:ID="_{@ea_guid}">
				<cimschema:IdentifiedObject.description><xsl:value-of select="$packagesHierarchy"/></cimschema:IdentifiedObject.description>
				<cimschema:IdentifiedObject.name><xsl:value-of select="@name" /></cimschema:IdentifiedObject.name>
				<cimschema:NamespaceElement.namespace rdf:resource="#_{$namespace-uuid}" />
			</cimschema:ValueType>
		</xsl:if>
  	</xsl:template>

	<!-- ============================================================================================================ -->
	<!-- END SECTION:  Package, Complex, ComplexType, Root, & CompoundType(s) TYPE DEFINITION templates               -->
	<!-- ============================================================================================================ -->	
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, Enumerated, Instance, Reference attribute & association templates)          -->
	<!-- ============================================================================================================ -->
	
	<xsl:template match="a:Simple|a:Domain|a:Enumerated|a:Compound">
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="concat(substring-before(@baseProperty, '#'), '#')"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="($include = 'true')">
			<cimschema:Property rdf:ID="_{@ea_guid}">
				<cimschema:IdentifiedObject.name><xsl:value-of select="@name" /></cimschema:IdentifiedObject.name>
				<xsl:variable name="primitiveType">
					<xsl:choose>
						<xsl:when test="self::a:Enumerated">
							<xsl:value-of select="'http://osii.com/cimschema/v1#PrimitiveTypes.Enumeration'" />
						</xsl:when>
						<xsl:otherwise>
							<xsl:call-template name="primitive-type"><xsl:with-param name="dataType" select="@dataType"/></xsl:call-template>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<cimschema:Property.primitiveType rdf:resource="{$primitiveType}" />
				<xsl:variable name="basePropertyClass" select="@basePropertyClass"/>
				<xsl:variable name="definingType" select="//a:ComplexType[@baseClass = $basePropertyClass]|//a:Root[@baseClass = $basePropertyClass]|//a:CompoundType[@baseClass = $basePropertyClass]|//a:EnumeratedType[@baseClass = $basePropertyClass]"/>
				<xsl:if test="$definingType">
					<cimschema:Property.definingType rdf:resource="#_{$definingType/@ea_guid}" />
				</xsl:if>
				<xsl:variable name="namespace-uuid">
					<xsl:call-template name="namespace-uuid"><xsl:with-param name="uri" select="concat(substring-before(@baseProperty, '#'), '#')"/></xsl:call-template>
				</xsl:variable>
				<cimschema:NamespaceElement.namespace rdf:resource="#_{$namespace-uuid}" />
				<xsl:choose>
					<xsl:when test="self::a:Enumerated">
						<xsl:variable name="baseClass" select="@baseClass"/>
						<xsl:variable name="enumerationType" select="//a:EnumeratedType[@baseClass = $baseClass]"/>
						<xsl:if test="$enumerationType">
							<cimschema:Property.enumerationType rdf:resource="#_{$enumerationType/@ea_guid}" />
						</xsl:if>
					</xsl:when>
					<xsl:when test="self::a:Domain">
						<!-- valueType is a reference to the CIM <<Primitive>> types (e.g. String, Boolean, etc.) -->
						<xsl:variable name="cimdatatypeName" select="@dataType"/>
						<xsl:variable name="valueType" select="//a:SimpleType[@dataType = $cimdatatypeName]"/>
						<xsl:if test="$valueType">
							<cimschema:Property.valueType rdf:resource="#_{$valueType/@ea_guid}" />
						</xsl:if>
					</xsl:when>
					<xsl:when test="self::a:Simple">
						<xsl:variable name="cimDatatype" select="@cimDatatype"/>
						<xsl:variable name="ea_guid" select="//a:PrimitiveType[@dataType = $cimDatatype]/@ea_guid"/>
						<cimschema:Property.valueType rdf:resource="#_{$ea_guid}" />
					</xsl:when>
				</xsl:choose>
			</cimschema:Property>
		</xsl:if>
  	</xsl:template>

	<!-- ======================================================================= -->
	<!-- Elements of type a:Instance are always associations and not attributes. -->
	<!-- ======================================================================= -->
	
	<xsl:template match="a:Instance|a:Reference">
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="concat(substring-before(@baseProperty, '#'), '#')"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<xsl:choose>
				<xsl:when test="ends-with(@ea_guid, '-A') or ends-with(@ea_guid, '-B')">
					<cimschema:AssociationPair rdf:ID="_{substring(@ea_guid, 0, string-length(@ea_guid) - 1)}" />
					<cimschema:Association rdf:ID="_{if (ends-with(@ea_guid, '-A')) then concat(substring(@ea_guid, 0, string-length(@ea_guid) - 1), '[1]') else concat(substring(@ea_guid, 0, string-length(@ea_guid) - 1), '[0]')}">
						<cimschema:Association.min><xsl:value-of select="@minOccurs"/></cimschema:Association.min>
						<cimschema:Association.showInTreeView>true</cimschema:Association.showInTreeView>
						<cimschema:IdentifiedObject.name><xsl:value-of select="@name"/></cimschema:IdentifiedObject.name>
						<xsl:if test="not(@maxOccurs = 'unbounded')">
							<cimschema:Association.max><xsl:value-of select="@maxOccurs"/></cimschema:Association.max>
						</xsl:if>
						<xsl:variable name="basePropertyClass" select="@basePropertyClass"/>
						<xsl:variable name="definingType" select="//a:ComplexType[@baseClass = $basePropertyClass]|//a:Root[@baseClass = $basePropertyClass]|//a:CompoundType[@baseClass = $basePropertyClass]"/>
						<xsl:choose>
							<xsl:when test="$definingType">
								<cimschema:Association.definingType rdf:resource="#_{$definingType/@ea_guid}" />
							</xsl:when>
							<xsl:otherwise>
								<cimschema:Association.definingType rdf:resource="Unknown Defining Type" />
							</xsl:otherwise>
						</xsl:choose>
						<xsl:variable name="namespace-uuid">
							<xsl:call-template name="namespace-uuid"><xsl:with-param name="uri" select="concat(substring-before(@baseProperty, '#'), '#')"/></xsl:call-template>
						</xsl:variable>
						<cimschema:NamespaceElement.namespace rdf:resource="#_{$namespace-uuid}" />
						<cimschema:Association.pair rdf:resource="#_{substring(@ea_guid, 0, string-length(@ea_guid) - 1)}" />
					</cimschema:Association>
				</xsl:when>
				<!-- Some properties use Instance -->
				<xsl:otherwise>
					<cimschema:Property rdf:ID="_{@ea_guid}">
						<cimschema:IdentifiedObject.name><xsl:value-of select="@name" /></cimschema:IdentifiedObject.name>
						<cimschema:Property.primitiveType rdf:resource="http://osii.com/cimschema/v1#PrimitiveTypes.String" />
						<xsl:variable name="basePropertyClass" select="@basePropertyClass"/>
						<xsl:variable name="definingType" select="//a:ComplexType[@baseClass = $basePropertyClass]|//a:Root[@baseClass = $basePropertyClass]|//a:CompoundType[@baseClass = $basePropertyClass]|//a:EnumeratedType[@baseClass = $basePropertyClass]"/>
						<xsl:if test="$definingType">
							<cimschema:Property.definingType rdf:resource="#_{$definingType/@ea_guid}" />
						</xsl:if>
						<xsl:variable name="namespace-uuid">
							<xsl:call-template name="namespace-uuid"><xsl:with-param name="uri" select="concat(substring-before(@baseProperty, '#'), '#')"/></xsl:call-template>
						</xsl:variable>
						<cimschema:NamespaceElement.namespace rdf:resource="#_{$namespace-uuid}" />
					</cimschema:Property>
				</xsl:otherwise>
			</xsl:choose>	
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:InverseInstance|a:InverseReference">
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="concat(substring-before(@baseProperty, '#'), '#')"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Association rdf:ID="_{if (ends-with(@ea_guid, '-A')) then concat(substring(@ea_guid, 0, string-length(@ea_guid) - 1), '[1]') else concat(substring(@ea_guid, 0, string-length(@ea_guid) - 1), '[0]')}">
				<cimschema:Association.min><xsl:value-of select="@minOccurs"/></cimschema:Association.min>
				<cimschema:Association.showInTreeView>true</cimschema:Association.showInTreeView>
				<cimschema:IdentifiedObject.name><xsl:value-of select="@name"/></cimschema:IdentifiedObject.name>
				<xsl:if test="not(@maxOccurs = 'unbounded')">
					<cimschema:Association.max><xsl:value-of select="@maxOccurs"/></cimschema:Association.max>
				</xsl:if>
				<xsl:variable name="basePropertyClass" select="@basePropertyClass"/>
				<xsl:variable name="definingType" select="//a:ComplexType[@baseClass = $basePropertyClass]|//a:Root[@baseClass = $basePropertyClass]|//a:CompoundType[@baseClass = $basePropertyClass]"/>
				<xsl:choose>
					<xsl:when test="$definingType">
						<cimschema:Association.definingType rdf:resource="#_{$definingType/@ea_guid}" />
					</xsl:when>
					<xsl:otherwise>
						<cimschema:Association.definingType rdf:resource="Unknown Defining Type" />
					</xsl:otherwise>
				</xsl:choose>
				<xsl:variable name="namespace-uuid">
					<xsl:call-template name="namespace-uuid"><xsl:with-param name="uri" select="concat(substring-before(@baseProperty, '#'), '#')"/></xsl:call-template>
				</xsl:variable>
				<cimschema:NamespaceElement.namespace rdf:resource="#_{$namespace-uuid}" />
				<cimschema:Association.pair rdf:resource="#_{substring(@ea_guid, 0, string-length(@ea_guid) - 1)}" />
			</cimschema:Association>
		</xsl:if>
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
		<!-- Consume text -->
	</xsl:template>
	
</xsl:stylesheet>