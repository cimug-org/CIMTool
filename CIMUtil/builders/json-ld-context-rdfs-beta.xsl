<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright 2025 UCAIug

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
<xsl:stylesheet version="3.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xs="http://www.w3.org/2001/XMLSchema" 
	xmlns:a="http://langdale.com.au/2005/Message#" 
	xmlns:sawsdl="http://www.w3.org/ns/sawsdl" 
	xmlns="http://langdale.com.au/2009/Indent" 
	xmlns:map="http://www.w3.org/2005/xpath-functions/map" 
	xmlns:fn="http://www.w3.org/2005/xpath-functions" 
	xmlns:cimtool="http://cimtool.ucaiug.io/functions">
	
	<xsl:output indent="yes" encoding="UTF-8"/>
    <xsl:param name="copyright-single-line" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<xsl:param name="namespacePrefixes"/>
	
	<!-- XSLT 3.0 map for namespace to prefix parsing -->
	<xsl:variable name="nsPrefixesMap" as="map(xs:string, xs:string)">
		<xsl:map>
			<xsl:for-each select="fn:tokenize($namespacePrefixes, '\|')[normalize-space()]">
				<xsl:variable name="parts" select="tokenize(., '=')"/>
				<xsl:map-entry key="$parts[1]" select="fn:string-join(fn:subsequence($parts, 2), '=')"/>
			</xsl:for-each>
		</xsl:map>
	</xsl:variable>
	
	<xsl:variable name="extensionNamespaces" as="map(xs:string, xs:string)">
	    <xsl:map>
	        <xsl:variable name="uniqueNamespaces" select="distinct-values(
	            for $uri in (
	                //@cimDatatype,
	                //@baseClass,
	                //@baseProperty,
	                //@baseResource
	            )
	            return 
	                if (contains($uri, '#'))
	                then substring-before($uri, '#')
	                else $uri
	        )[(. != '') and (. != $ontologyURI) and (. != $baseURI)]"/>
	        
	        <!-- Get sorted list of namespaces that need ext prefixes (not in nsPrefixesMap) -->
	        <xsl:variable name="unmappedNamespaces" as="xs:string*">
	            <xsl:for-each select="$uniqueNamespaces[not(map:contains($nsPrefixesMap, concat(., '#')))]">
	                <xsl:sort select="."/>
	                <xsl:sequence select="."/>
	            </xsl:for-each>
	        </xsl:variable>
	        
	        <xsl:for-each select="$uniqueNamespaces">
	            <xsl:sort select="."/>
	            <xsl:variable name="nsWithHash" select="concat(., '#')"/>
	            <xsl:choose>
	                <!-- If namespace exists in nsPrefixesMap, use that prefix -->
	                <xsl:when test="map:contains($nsPrefixesMap, $nsWithHash)">
	                    <xsl:map-entry key="." select="$nsPrefixesMap($nsWithHash)"/>
	                </xsl:when>
	                <!-- Otherwise, assign ext/ext1/ext2/etc. -->
	                <xsl:otherwise>
	                    <xsl:variable name="extPosition" select="index-of($unmappedNamespaces, .)"/>
	                    <xsl:map-entry key="." select="
	                        if (count($unmappedNamespaces) = 1) 
	                        then 'ext' 
	                        else concat('ext', $extPosition)
	                    "/>
	                </xsl:otherwise>
	            </xsl:choose>
	        </xsl:for-each>
	    </xsl:map>
	</xsl:variable>
	
	<xsl:variable name="allNamespaces" as="map(xs:string, xs:string)">
	    <xsl:map>
	        <xsl:variable name="uniqueNamespaces" select="distinct-values(
	            for $uri in (
	                //@cimDatatype,
	                //@baseClass,
	                //@baseProperty,
	                //@baseResource
	            )
	            return 
	                if (contains($uri, '#'))
	                then substring-before($uri, '#')
	                else $uri
	        )[(. != '')]"/>
	        
	        <!-- Get sorted list of namespaces that need ext prefixes (not in nsPrefixesMap) -->
	        <xsl:variable name="unmappedNamespaces" as="xs:string*">
	            <xsl:for-each select="$uniqueNamespaces[not(map:contains($nsPrefixesMap, concat(., '#')))]">
	                <xsl:sort select="."/>
	                <xsl:sequence select="."/>
	            </xsl:for-each>
	        </xsl:variable>
	        
	        <xsl:for-each select="$uniqueNamespaces">
	            <xsl:sort select="."/>
	            <xsl:variable name="nsWithHash" select="concat(., '#')"/>
	            <xsl:choose>
	                <!-- If namespace exists in nsPrefixesMap, use that prefix -->
	                <xsl:when test="map:contains($nsPrefixesMap, $nsWithHash)">
	                    <xsl:map-entry key="." select="$nsPrefixesMap($nsWithHash)"/>
	                </xsl:when>
	                <!-- Otherwise, assign ext/ext1/ext2/etc. -->
	                <xsl:otherwise>
	                    <xsl:variable name="extPosition" select="index-of($unmappedNamespaces, .)"/>
	                    <xsl:map-entry key="." select="
	                        if (count($unmappedNamespaces) = 1) 
	                        then 'ext' 
	                        else concat('ext', $extPosition)
	                    "/>
	                </xsl:otherwise>
	            </xsl:choose>
	        </xsl:for-each>
	    </xsl:map>
	</xsl:variable>
	
	<!--
		Function: cimtool:get-namespace-prefix
		
		Purpose:
			Returns the appropriate prefix for a given namespace URI. The function first checks
			for standard namespaces (cim, profile, xsd, rdf), then looks up extension namespaces
			in the $extensionNamespaces map, which contains dynamically generated prefixes
			(ext1, ext2, ext3, etc.) for extension namespaces found in the input document.
		
		Parameters:
			$namespaceURI (xs:string) - The full namespace URI to look up
		
		Returns:
			xs:string - The prefix associated with the namespace:
						- 'cim' for the main CIM ontology URI ($ontologyURI)
						- 'profile' for the profile base URI ($baseURI)
						- 'xsd' for XML Schema namespace
						- 'rdf' for RDF syntax namespace
						- 'ext1', 'ext2', etc. for extension namespaces
						- Empty string ('') if namespace is not found
		
		Dependencies:
			- Global variable: $extensionNamespaces (map of namespace URIs to prefixes)
			- Global parameter: $ontologyURI
			- Global parameter: $baseURI
		
		Examples:
			cimtool:get-namespace-prefix('http://iec.ch/TC57/CIM100#')
				Returns: 'cim'
			
			cimtool:get-namespace-prefix('http://iec.ch/TC57/CIM100.2/JTF-extensions#')
				Returns: 'ext1' (or ext2, ext3 depending on sort order in map)
			
			cimtool:get-namespace-prefix('http://www.w3.org/2001/XMLSchema#')
				Returns: 'xsd'
	-->
	<xsl:function name="cimtool:get-namespace-prefix" as="xs:string">
		<xsl:param name="namespaceURI" as="xs:string"/>
		
		<xsl:choose>
			<!-- Handle standard namespaces -->
			<xsl:when test="$namespaceURI = $ontologyURI">
				<xsl:sequence select="'cim'"/>
			</xsl:when>
			<xsl:when test="$namespaceURI = substring-before($baseURI, '#')">
				<xsl:sequence select="'profile'"/>
			</xsl:when>
			<xsl:when test="$namespaceURI = 'http://www.w3.org/2001/XMLSchema'">
				<xsl:sequence select="'xsd'"/>
			</xsl:when>
			<xsl:when test="$namespaceURI = 'http://www.w3.org/1999/02/22-rdf-syntax-ns'">
				<xsl:sequence select="'rdf'"/>
			</xsl:when>
			<!-- Look up extension namespaces in the map -->
			<xsl:when test="map:contains($extensionNamespaces, $namespaceURI)">
				<xsl:sequence select="$extensionNamespaces($namespaceURI)"/>
			</xsl:when>
			<!-- Fallback for unknown namespaces -->
			<xsl:otherwise>
				<xsl:sequence select="''"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
	<!-- ============================================================================================================ -->
	<!-- START THE ROOT ELEMENT OF THE PROFILE: "top-level" Catalog element                                           -->
	<!-- ============================================================================================================ -->
	
	<xsl:template match="a:Catalog">
		<!-- the top level template -->
		<document>
			<list begin="{{" indent="     " delim="," end="}}">
				<list begin="&quot;@context&quot;: {{" indent="    " delim="," end="}}">
					<item>"cim": "<xsl:value-of select="concat($ontologyURI, '#')"/>",</item>
					<item>"eu": "https://cim.ucaiug.io/ns/eu#",</item>
					<item>"nc": "https://cim4.eu/ns/nc#",</item>
					<item>"adms": "http://www.w3.org/ns/adms#",</item>
					<item>"dcat": "http://www.w3.org/ns/dcat#",</item>
					<item>"dcat-cim": "https://cim4.eu/ns/dcat-cim#",</item>
					<item>"dct": "http://purl.org/dc/terms/",</item>
					<item>"dm": "http://iec.ch/TC57/61970-552/DifferenceModel/1#",</item>
					<item>"eumd": "https://cim4.eu/ns/Metadata-European#",</item>
					<item>"euvoc": "http://publications.europa.eu/ontology/euvoc#",</item>
					<item>"md": "http://iec.ch/TC57/61970-552/ModelDescription/1#",</item>
					<item>"prov": "http://www.w3.org/ns/prov#",</item>
					<item>"xsd": "http://www.w3.org/2001/XMLSchema#",</item>					
					<!-- Generate entries for all discovered extension namespaces, sorted by prefix --> 
					<xsl:for-each select="map:keys($extensionNamespaces)">
						<xsl:sort select="$extensionNamespaces(.)"/>
						<item>"HI<xsl:value-of select="$extensionNamespaces(.)"/>": "<xsl:value-of select="concat(., '#')"/>",</item>
					</xsl:for-each>
					<!-- 
					<xsl:for-each select="map:keys($allNamespaces)">
						<xsl:sort select="$allNamespaces(.)"/>
						<item>"<xsl:value-of select="$allNamespaces(.)"/>": "<xsl:value-of select="concat(., '#')"/>",</item>
						
						<xsl:for-each select=".//a:Domain[fn:starts-with(@dataType, 'http://')]">
							<xsl:apply-templates select=".">
								<xsl:sort select="@name"/>
							</xsl:apply-templates>
						</xsl:for-each> 
					</xsl:for-each>-->
				</list>
			</list>
		</document>
	</xsl:template>
	
	<xsl:template match="a:Domain|a:Simple">
		<item>"<xsl:value-of select="$extensionNamespaces(.)"/>:<xsl:value-of select="@name"/>": {"@type": "xsd:float"},</item>
	</xsl:template>
	
	<xsl:template match="text()">
		<!--  dont pass text through  -->
	</xsl:template>
	
</xsl:stylesheet>