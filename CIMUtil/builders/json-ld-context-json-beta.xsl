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
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:a="http://langdale.com.au/2005/Message#" xmlns:sawsdl="http://www.w3.org/ns/sawsdl" xmlns="http://langdale.com.au/2009/Indent" xmlns:map="http://www.w3.org/2005/xpath-functions/map" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:cimtool="http://cimtool.ucaiug.io/functions">
	
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
	
	<xsl:template name="generate_properties">
		<xsl:choose>
			<xsl:when test="a:SuperType">
				<xsl:variable name="supertype_name" select="a:SuperType/@name"/>
				<xsl:for-each select="/*/node()[@name = $supertype_name]">
					<xsl:call-template name="generate_properties"/>
				</xsl:for-each>
				<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- START THE ROOT ELEMENT OF THE PROFILE: "top-level" Catalog element                                           -->
	<!-- ============================================================================================================ -->
	
	<!-- IMPORTANT:  This use of the XSL 1.0 key is needed to ensure we create only a single unique *Ref class for each class type.  -->
	<xsl:key name="references" match="a:Reference" use="@type"/>
	
	<xsl:template match="a:Catalog">
		<!-- the top level template -->
		<document>
			<list begin="{{" indent="     " delim="," end="}}">
				<list begin="&quot;@context&quot;: {{" indent="    " delim="," end="}}">
					<item>"@version": <xsl:value-of select="'1.1'"/></item>
					<item>"@base": "<xsl:value-of select="$baseURI"/>"</item>							
					<item>"@vocab": "<xsl:value-of select="concat($ontologyURI, '#')"/>"</item>
					<item>"xsd": "http://www.w3.org/2001/XMLSchema#"</item>
					<item>"rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#"</item>
					<item>"cim": "<xsl:value-of select="concat($ontologyURI, '#')"/>"</item>
					<!-- Generate entries for all discovered extension namespaces, sorted by prefix -->
					<xsl:for-each select="map:keys($extensionNamespaces)">
						<xsl:sort select="$extensionNamespaces(.)"/>
						<item>"<xsl:value-of select="$extensionNamespaces(.)"/>": "<xsl:value-of select="concat(., '#')"/>"</item>
					</xsl:for-each>
					<item>"profile": "<xsl:value-of select="$baseURI"/>"</item>
					<xsl:if test=".//a:Simple[@name = 'mRID']">				
						<item>"mRID": "@id"</item>
					</xsl:if>
					<xsl:if test=".//a:Reference">
						<item>"ref": "@id"</item>
					</xsl:if>
					<!-- We cycle through all "Root" classes -->
					<xsl:for-each select="a:Root|a:ComplexType|a:CompoundType|a:EnumeratedType">
						<xsl:choose>
							<xsl:when test="self::a:EnumeratedType">
								<list begin="&quot;{@name}&quot;: {{" indent="    " delim="," end="}}">
									<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseClass, '#')), ':'), substring-after(@baseClass, '#'))"/>"</item>
								</list>
							</xsl:when>
							<xsl:otherwise>
								<xsl:variable name="super" select="a:SuperType"/>
								<list begin="&quot;{@name}&quot;: {{" indent="     " delim="," end="}}">
									<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseClass, '#')), ':'), @name)"/>"</item>
									<list begin="&quot;@context&quot;: {{" indent="    " delim="," end="}}">
										<xsl:choose>
											<xsl:when test="$super">
												<xsl:call-template name="generate_properties"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:apply-templates/>
											</xsl:otherwise>
										</xsl:choose>
									</list>
								</list>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
					<xsl:for-each select=".//a:Reference[generate-id() = generate-id(key('references', @type)[1])]">
						<xsl:call-template name="generate_ref_class_context"/>
					</xsl:for-each>
				</list>
			</list>
		</document>
	</xsl:template>
	
	<xsl:template match="a:Domain|a:Simple">
		<xsl:choose>
			<xsl:when test="@name = 'mRID'">
				<item>"<xsl:value-of select="@name"/>": "@id"</item>
			</xsl:when>
			<xsl:otherwise>
				<list begin="&quot;{@name}&quot;: {{" indent="    " delim="," end="}}">
					<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
					<item>"@type": "<xsl:value-of select="concat('xsd:', @xstype)"/>"</item>
				</list>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="a:Compound">
		<item>"<xsl:value-of select="@name"/>": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
	</xsl:template>
		
	<xsl:template match="a:Enumerated">
		<xsl:variable name="type" select="@type"/>
		<xsl:variable name="theEnumeration" select="//a:EnumeratedType[@name = $type]"/>
		<xsl:if test="$theEnumeration">
			<list begin="&quot;{@name}&quot;: {{" indent="    " delim="," end="}}">
				<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
				<item>"@type": "@id"</item>
				<list begin="&quot;@context&quot;: {{" indent="    " delim="," end="}}">
					<xsl:for-each select="$theEnumeration/a:EnumeratedValue">
						<item>"<xsl:value-of select="@name"/>": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseResource, '#')), ':'), substring-after(@baseResource, '#'))"/>"</item>
					</xsl:for-each>
				</list>
			</list>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Instance|a:Reference|a:Choice">
		<xsl:choose>
			<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
				<list begin="&quot;{@name}&quot;: {{" indent="    " delim="," end="}}">
					<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
					<item>"@container": "@set"</item>
				</list>
			</xsl:when>
			<xsl:otherwise>
				<item>"<xsl:value-of select="@name"/>": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- THIS NEEDS TO BE TESTED AND FIXED -->
	<xsl:template match="a:Complex|a:SimpleCompound">
		<xsl:choose>
			<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
				<list begin="&quot;{@name}&quot;: {{" indent="    " delim="," end="}}">
					<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
					<item>"@container": "@set"</item>
				</list>
			</xsl:when>
			<xsl:otherwise>
				<item>"<xsl:value-of select="@name"/>": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- THIS NEEDS TO BE TESTED AND FIXED -->
	<!--  Generates a nested JSON object definition.  Equivalent to an XSD inline anonymous complex or compound type declaration  -->
	<xsl:template match="a:SimpleEnumerated">
		<xsl:choose>
			<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
				<list begin="&quot;{@name}&quot;: {{" indent="    " delim="," end="}}">
					<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
					<item>"@container": "@set"</item>
				</list>
			</xsl:when>
			<xsl:otherwise>
				<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before(@baseProperty, '#')), ':'), substring-after(@baseProperty, '#'))"/>"</item>
				<item>"@type": "<xsl:value-of select="concat('xsd:', @xstype)"/>"</item>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="generate_ref_class_context">
		<list begin="&quot;{@type}Ref&quot;: {{" indent="    " delim="," end="}}">
			<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before($baseURI, '#')), ':'), @type, 'Ref')"/>"</item>
			<list begin="&quot;@context&quot;: {{" indent="    " delim="," end="}}">
				<item>"ref": "@id"</item>
				<list begin="&quot;referenceType&quot;: {{" indent="    " delim="," end="}}">
					<item>"@id": "<xsl:value-of select="concat(concat(cimtool:get-namespace-prefix(substring-before($baseURI, '#')), ':'), @type, 'Ref', '.referenceType')"/>"</item>
					<item>"@type": "xsd:string"</item>
				</list>
			</list>
		</list>
	</xsl:template>
	
	<xsl:template match="text()">
		<!--  dont pass text through  -->
	</xsl:template>
	
	<xsl:template match="node()" mode="declare">
		<!-- dont pass any defaults in declare mode -->
	</xsl:template>
	
</xsl:stylesheet>