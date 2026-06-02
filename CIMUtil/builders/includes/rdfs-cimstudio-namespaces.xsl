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
<xsl:stylesheet version="3.0" exclude-result-prefixes="a map xsd"
	xmlns:xsd="http://www.w3.org/2001/XMLSchema" 	
	xmlns:map="http://www.w3.org/2005/xpath-functions/map" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:a="http://langdale.com.au/2005/Message#" 
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
	xmlns:cimschema="http://osii.com/cimschema/v1#">
	
	<xsl:variable name="namespace-uuids" as="map(xsd:string, xsd:string)">
		<xsl:map>
			<!--  <xsl:map-entry key="'http://iec.ch/TC57/CIM100#'" select="'460105d3-297a-46f8-b520-077c0c10d2cc'"/>
			<xsl:map-entry key="'http://iec.ch/TC57/61970-552/ModelDescription/1#'" select="'05b0591b-ac38-43fd-a256-57fdc5a92453'"/>
			<xsl:map-entry key="'http://osii.com/opennet/6.3#'" select="'ad211262-e175-4c9b-b02b-e426e206572e'"/>
			<xsl:map-entry key="'http://iec.ch/TC57/ns/CIM/prof-cim#'" select="'ad2c2335-e38a-4ed9-ac60-56173c15a38c'"/>	
			<xsl:map-entry key="'http://www.w3.org/2006/time#'" select="'67bf2226-d10e-4efe-829c-3548e4d6b144'"/>	
			<xsl:map-entry key="'http://iec.ch/TC57/61970-552/DifferenceModel/1#'" select="'57babe43-9be5-43dd-bf1a-8eafe2c07342'"/>
			<xsl:map-entry key="'http://osii.com/cimstudio/v1#'" select="'ea78cf92-1e8f-4ef9-b75d-83dde5abf21d'"/>
			<xsl:map-entry key="'http://purl.org/dc/terms/#'" select="'dd3302be-85b7-447d-92f0-2297d9a71adf'"/>
			<xsl:map-entry key="'http://www.w3.org/ns/dx/prof/#'" select="'bbf83251-fcee-4030-9c64-7756b8b93d36'"/>
			<xsl:map-entry key="'http://entsoe.eu/ns/Metadata-European#'" select="'c0a7703c-6b1e-40fb-8d9b-8abddc0615ab'"/> -->
			<xsl:map-entry key="'http://iec.ch/TC57/CIM100-European#'" select="'d27929db-3c71-43eb-ba0e-752b47c7763d'"/> 
			<!--  <xsl:map-entry key="'http://www.w3.org/ns/prov#'" select="'1199af80-b79d-4326-bdb3-1d9891f6bcf6'"/>
			<xsl:map-entry key="'http://www.w3.org/ns/dcat#'" select="'12726d17-ad94-41d6-bf75-b46a35973587'"/>
			<xsl:map-entry key="'http://entsoe.eu/ns/nc#'" select="'319f67ed-bec6-448a-b900-af4fe94f14b8'"/> -->
		</xsl:map>
    </xsl:variable>
    
	<xsl:template name="namespace-uuid">
		<xsl:param name="uri"/>
		<xsl:value-of select="if (map:contains($namespace-uuids, concat(substring-before($uri, '#'), '#'))) then map:get($namespace-uuids, concat(substring-before($uri, '#'), '#')) else $uri" />	
	</xsl:template>
	
	<xsl:template name="include-extension">
		<xsl:param name="uri"/>
		<xsl:value-of select="if (map:contains($namespace-uuids, concat(substring-before($uri, '#'), '#'))) then 'true' else 'false'" />	
	</xsl:template>
	
	<xsl:template name="namespaces" >
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://iec.ch/TC57/CIM100#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_460105d3-297a-46f8-b520-077c0c10d2cc">
				<cimschema:IdentifiedObject.name>CIM</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://iec.ch/TC57/CIM100#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>cim</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>17</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://iec.ch/TC57/61970-552/ModelDescription/1#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_05b0591b-ac38-43fd-a256-57fdc5a92453">
				<cimschema:IdentifiedObject.name>md</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://iec.ch/TC57/61970-552/ModelDescription/1#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>md</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://osii.com/opennet/6.3#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_ad211262-e175-4c9b-b02b-e426e206572e">
				<cimschema:IdentifiedObject.name>OSI</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://osii.com/opennet/6.3#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>osi</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>6.3</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://iec.ch/TC57/ns/CIM/prof-cim#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_ad2c2335-e38a-4ed9-ac60-56173c15a38c">
				<cimschema:IdentifiedObject.name>profcim</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://iec.ch/TC57/ns/CIM/prof-cim#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>profcim</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://www.w3.org/2006/time#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_67bf2226-d10e-4efe-829c-3548e4d6b144">
				<cimschema:IdentifiedObject.name>time</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://www.w3.org/2006/time#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>time</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://iec.ch/TC57/61970-552/DifferenceModel/1#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_57babe43-9be5-43dd-bf1a-8eafe2c07342">
				<cimschema:IdentifiedObject.name>dm</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://iec.ch/TC57/61970-552/DifferenceModel/1#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>dm</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://osii.com/cimstudio/v1#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_ea78cf92-1e8f-4ef9-b75d-83dde5abf21d">
				<cimschema:IdentifiedObject.name>CIMStudio</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://osii.com/cimstudio/v1#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>cimstudio</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>1</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://purl.org/dc/terms/#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_dd3302be-85b7-447d-92f0-2297d9a71adf">
				<cimschema:IdentifiedObject.name>dcterms</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://purl.org/dc/terms/#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>dcterms</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://www.w3.org/ns/dx/prof/#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_bbf83251-fcee-4030-9c64-7756b8b93d36">
				<cimschema:IdentifiedObject.name>prof</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://www.w3.org/ns/dx/prof/#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>prof</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://entsoe.eu/ns/Metadata-European#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_c0a7703c-6b1e-40fb-8d9b-8abddc0615ab">
				<cimschema:IdentifiedObject.name>eumnd</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://entsoe.eu/ns/Metadata-European#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>eumd</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://iec.ch/TC57/CIM100-European#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_d27929db-3c71-43eb-ba0e-752b47c7763d">
				<cimschema:IdentifiedObject.name>European</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://iec.ch/TC57/CIM100-European#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>eu</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>	
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://www.w3.org/ns/prov#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_1199af80-b79d-4326-bdb3-1d9891f6bcf6">
				<cimschema:IdentifiedObject.name>prov</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://www.w3.org/ns/prov#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>prov</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://www.w3.org/ns/dcat#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_12726d17-ad94-41d6-bf75-b46a35973587">
				<cimschema:IdentifiedObject.name>dcat</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://www.w3.org/ns/dcat#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>dcat</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
		<xsl:variable name="include">
			<xsl:call-template name="include-extension"><xsl:with-param name="uri" select="'http://entsoe.eu/ns/nc#'"/></xsl:call-template>
		</xsl:variable>
		<xsl:if test="$include = 'true'">
			<cimschema:Namespace rdf:ID="_319f67ed-bec6-448a-b900-af4fe94f14b8">
				<cimschema:IdentifiedObject.name>NC</cimschema:IdentifiedObject.name>
				<cimschema:Namespace.exportURI>http://entsoe.eu/ns/nc#</cimschema:Namespace.exportURI>
				<cimschema:Namespace.exportPrefix>nc</cimschema:Namespace.exportPrefix>
				<cimschema:Namespace.version>-</cimschema:Namespace.version>
			</cimschema:Namespace>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>