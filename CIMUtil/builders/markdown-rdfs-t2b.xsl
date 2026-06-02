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
<xsl:stylesheet version="3.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:map="http://www.w3.org/2005/xpath-functions/map" 
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:a="http://langdale.com.au/2005/Message#" 
                xmlns:local="http://local-functions"
                xmlns="http://langdale.com.au/2009/Indent">
  
  <xsl:output indent="no" method="xml" encoding="UTF-8" omit-xml-declaration="yes"/>
  <xsl:preserve-space elements="a:AsciiDoc"/>
  <xsl:param name="fileName"/>
  <xsl:param name="baseURI"/>
  <xsl:param name="ontologyURI"/>
  <xsl:param name="envelope">Profile</xsl:param>
  <xsl:param name="copyright-single-line" />
  <xsl:param name="version"/>
  
  <!-- Mermaid diagram parameters -->
  <xsl:param name="builderParameters"/>
  
  <!-- XSLT 3.0 map for parameter parsing - simplified syntax -->
  <xsl:variable name="paramMap" as="map(xs:string, xs:string)">
    <xsl:map>
      <xsl:for-each select="tokenize($builderParameters, '\|')[normalize-space()]">
        <xsl:variable name="parts" select="tokenize(., '=')"/>
        <xsl:map-entry key="$parts[1]" select="string-join(subsequence($parts, 2), '=')"/>
      </xsl:for-each>
    </xsl:map>
  </xsl:variable>
  
  <!-- Extract parameters using XSLT 3.0 map functions -->
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
  <xsl:param name="errorsColor" select="map:get($paramMap, 'errorsColor')"/>
  <xsl:param name="errorsFontColor" select="map:get($paramMap, 'errorsFontColor')"/>
  <xsl:param name="mermaidTheme" select="map:get($paramMap, 'mermaidTheme')"/>
  <xsl:param name="primitivesColor" select="map:get($paramMap, 'primitivesColor')"/>
  <xsl:param name="primitivesFontColor" select="map:get($paramMap, 'primitivesFontColor')"/>
  
  <!-- Boolean parameters with XSLT 3.0 safe handling -->
  <xsl:param name="enableDarkMode" as="xs:boolean" select="map:get($paramMap, 'enableDarkMode') = 'true'"/>
  <xsl:param name="enableShadowing" as="xs:boolean" select="map:get($paramMap, 'enableShadowing') = 'true'"/>
  <xsl:param name="hideCardinalityForRequiredAttributes" as="xs:boolean" select="map:get($paramMap, 'hideCardinalityForRequiredAttributes') = 'true'"/>
  <xsl:param name="hideCIMDatatypes" as="xs:boolean" select="map:get($paramMap, 'hideCIMDatatypes') = 'true'"/>
  <xsl:param name="hideCompounds" as="xs:boolean" select="map:get($paramMap, 'hideCompounds') = 'true'"/>
  <xsl:param name="hideEnumerations" as="xs:boolean" select="map:get($paramMap, 'hideEnumerations') = 'true'"/>
  <xsl:param name="hidePrimitives" as="xs:boolean" select="map:get($paramMap, 'hidePrimitives') = 'true'"/>
  
  <!-- XSLT 3.0 map for character replacement -->
  <xsl:variable name="asciidoc-restricted" as="map(xs:string, xs:string)">
    <xsl:map>
      <xsl:map-entry key="'|'" select="'vbar'"/>
      <xsl:map-entry key="'['" select="'startsb'"/> 
      <xsl:map-entry key="']'" select="'endsb'"/>
      <xsl:map-entry key="'^'" select="'caret'"/>
      <xsl:map-entry key="'*'" select="'asterisk'"/>
      <xsl:map-entry key="'&amp;'" select="'amp'"/>
      <xsl:map-entry key="'`'" select="'backtick'"/>
      <xsl:map-entry key="'‘'" select="'lsquo'"/>
      <xsl:map-entry key="'’'" select="'rsquo'"/>
      <xsl:map-entry key="'“'" select="'ldquo'"/>
      <xsl:map-entry key="'”'" select="'rdquo'"/>
      <xsl:map-entry key="'°'" select="'deg'"/>
      <xsl:map-entry key="'¦'" select="'brvbar'"/>
      <xsl:map-entry key="'&lt;'" select="'lt'"/>
      <xsl:map-entry key="'>'" select="'gt'"/>
      <xsl:map-entry key="'~'" select="'tilde'"/> 
      <xsl:map-entry key="'\'" select="'backslash'"/>
    </xsl:map>
  </xsl:variable>
      
      <xsl:variable name="asciidoc-table-sensitive" as="map(xs:string, xs:string)">
        <xsl:map>
          <xsl:map-entry key="'|'" select="'vbar'"/>
        </xsl:map>
      </xsl:variable>
      
      <!-- Helper functions using XSLT 3.0 -->
      <xsl:function name="local:format-cardinality" as="xs:string">
        <xsl:param name="minOccurs"/>
        <xsl:param name="maxOccurs"/>
        <xsl:choose>
          <xsl:when test="$minOccurs = $maxOccurs">
            <xsl:value-of select="$minOccurs"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$minOccurs || '..' || replace(replace($maxOccurs, 'unbounded', '*'), 'n', '*')"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:function>
      
      <xsl:function name="local:get-class-name" as="xs:string">
        <xsl:param name="baseClass"/>
        <xsl:value-of select="substring-after($baseClass, '#')"/>
      </xsl:function>
  
      <xsl:function name="local:replace-chars" as="xs:string">
        <xsl:param name="text" as="xs:string"/>
        <xsl:param name="replacementMap" as="map(xs:string, xs:string)"/>
        <xsl:choose>
          <xsl:when test="map:contains($replacementMap, '|')">
            <xsl:value-of select="replace($text, '\|', '{vbar}')"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$text"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:function>
      
      <!-- Additional XSLT 3.0 helper function for cardinality display -->
      <xsl:function name="local:cardinality-display" as="xs:string">
        <xsl:param name="minOccurs"/>
        <xsl:param name="maxOccurs"/>
        <xsl:choose>
          <xsl:when test="$hideCardinalityForRequiredAttributes and ($minOccurs = 1)">
            <xsl:value-of select="if ($minOccurs != $maxOccurs) then ' [' || local:format-cardinality($minOccurs, $maxOccurs) || ']' else ''"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="' [' || local:format-cardinality($minOccurs, $maxOccurs) || ']'"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:function>
      
      <xsl:template match="a:Catalog">
        <document>
          <list begin="" indent="" delim="" end="">
            <item></item>
            <xsl:choose>
              <xsl:when test="$envelope != 'Profile'">
                <item># <xsl:value-of select="$envelope"/> Profile Specification</item>
              </xsl:when>
              <xsl:otherwise>
                <item># <xsl:value-of select="$fileName"/> Profile Specification</item>
              </xsl:otherwise>
            </xsl:choose>
            <item></item>
            <item>Profile namespace:  `<xsl:value-of select="$baseURI"/>`</item>
            <item></item>
            <!-- Generate embedded Mermaid diagram -->
            <item>## <xsl:value-of select="$envelope"/> Profile</item>
            <item></item>
            <item>```mermaid</item>
            <xsl:call-template name="generate-mermaid-diagram"/>
            <item>```</item>
            <item></item>
            <xsl:apply-templates mode="annotate-type"/>
            
            <!-- Concrete Classes Section -->
            <xsl:if test="exists(a:Root|a:Message)">
              <item></item>
              <item>## Concrete Classes</item>
              <item></item>
              <xsl:apply-templates select="a:Root|a:Message">
                <xsl:sort select="@name"/>
              </xsl:apply-templates>
            </xsl:if>
            
            <!-- Abstract Classes Section -->
            <xsl:if test="exists(a:ComplexType)">
              <item></item>
              <item>## Abstract Classes</item>
              <item></item>
              <xsl:apply-templates select="a:ComplexType">
                <xsl:sort select="@name"/>
              </xsl:apply-templates>
            </xsl:if>
            
            <!-- Compound Types Section -->
            <xsl:if test="exists(a:CompoundType)">
              <item></item>
              <item>## Compound Types </item>
              <item></item>
              <xsl:apply-templates select="a:CompoundType">
                <xsl:sort select="@name"/>
              </xsl:apply-templates>
            </xsl:if>
            
            <!-- Enumerations Section -->
            <xsl:if test="exists(a:EnumeratedType)">
              <item></item>
              <item>## Enumerations</item>
              <item></item>
              <xsl:apply-templates select="a:EnumeratedType">
                <xsl:sort select="@name"/>
              </xsl:apply-templates>
            </xsl:if>
            
            <!-- Datatypes Section -->
            <xsl:if test="exists(a:SimpleType)">
              <item></item>
              <item>## Datatypes</item>
              <item></item>
              <xsl:apply-templates select="a:SimpleType">
                <xsl:sort select="@name"/>
              </xsl:apply-templates>
            </xsl:if>
            
            <!-- Primitive Types Section -->
            <xsl:if test="exists(a:PrimitiveType)">
              <item></item>
              <item>## Primitive Types</item>
              <item></item>
              <xsl:apply-templates select="a:PrimitiveType">
                <xsl:sort select="@name"/>
              </xsl:apply-templates>
            </xsl:if>
            <item></item>
          </list>
        </document>
      </xsl:template>
      
      <!-- Template to generate the Mermaid diagram inline -->
      <xsl:template name="generate-mermaid-diagram">
        <item>classDiagram</item>
        <item>  direction TB</item>
        <item></item>
        
        <!-- Add profile information as a note -->
        <xsl:if test="not(@hideInDiagrams = 'true')">
          <xsl:variable name="notename" select="replace($envelope, '[\s:.,\[\]{}\(\)&lt;&gt;\-\+#\*=]', '_') || 'Note'" />
          <item>  note "Profile: <xsl:value-of select="$envelope"/>&#10;Namespace: <xsl:value-of select="$baseURI"/><xsl:if test="a:Note[normalize-space()]">&#10;&#10;Profile Notes:&#10;<xsl:apply-templates select="a:Note" mode="profile-notes"/></xsl:if>"</item>
          <item></item>
        </xsl:if>
        
        <!-- Process all types -->
        <xsl:apply-templates select="a:Root|a:ComplexType|a:EnumeratedType" mode="mermaid"/>
        
        <!-- Process inheritance relationships -->
        <xsl:apply-templates select="a:Root|a:ComplexType" mode="mermaid-inheritance"/>
        
        <!-- Process associations -->
        <xsl:apply-templates select="a:Root|a:ComplexType|a:CompoundType" mode="mermaid-associations"/>
        
        <!-- Add CSS styling if colors are specified -->
        <xsl:variable name="hasColors" select="exists(($concreteClassesColor, $abstractClassesColor, $enumerationsColor, $cimDatatypesColor, $compoundsColor, $primitivesColor, $errorsColor)[normalize-space()])"/>
        <xsl:if test="$hasColors">
          <item></item>
          <item>  %% CSS Styling</item>
          <xsl:if test="normalize-space($concreteClassesColor)">
            <item>  classDef concreteClass fill:<xsl:value-of select="$concreteClassesColor"/><xsl:if test="normalize-space($concreteClassesFontColor)">,color:<xsl:value-of select="$concreteClassesFontColor"/></xsl:if></item>
          </xsl:if>
          <xsl:if test="normalize-space($abstractClassesColor)">
            <item>  classDef abstractClass fill:<xsl:value-of select="$abstractClassesColor"/><xsl:if test="normalize-space($abstractClassesFontColor)">,color:<xsl:value-of select="$abstractClassesFontColor"/></xsl:if></item>
          </xsl:if>
          <xsl:if test="normalize-space($enumerationsColor)">
            <item>  classDef enumClass fill:<xsl:value-of select="$enumerationsColor"/><xsl:if test="normalize-space($enumerationsFontColor)">,color:<xsl:value-of select="$enumerationsFontColor"/></xsl:if></item>
          </xsl:if>
          <xsl:if test="normalize-space($cimDatatypesColor)">
            <item>  classDef cimDatatypeClass fill:<xsl:value-of select="$cimDatatypesColor"/><xsl:if test="normalize-space($cimDatatypesFontColor)">,color:<xsl:value-of select="$cimDatatypesFontColor"/></xsl:if></item>
          </xsl:if>
          <xsl:if test="normalize-space($compoundsColor)">
            <item>  classDef compoundClass fill:<xsl:value-of select="$compoundsColor"/><xsl:if test="normalize-space($compoundsFontColor)">,color:<xsl:value-of select="$compoundsFontColor"/></xsl:if></item>
          </xsl:if>
          <xsl:if test="normalize-space($primitivesColor)">
            <item>  classDef primitiveClass fill:<xsl:value-of select="$primitivesColor"/><xsl:if test="normalize-space($primitivesFontColor)">,color:<xsl:value-of select="$primitivesFontColor"/></xsl:if></item>
          </xsl:if>
          <xsl:if test="normalize-space($errorsColor)">
            <item>  classDef errorClass fill:<xsl:value-of select="$errorsColor"/><xsl:if test="normalize-space($errorsFontColor)">,color:<xsl:value-of select="$errorsFontColor"/></xsl:if></item>
          </xsl:if>
        </xsl:if>
      </xsl:template>
  
  <!-- Individual class diagram generation -->
  <xsl:template name="generate-individual-class-diagram">
    <xsl:param name="className"/>
    <xsl:variable name="currentClass" select="."/>
    
    <item>```mermaid</item>
    <item>classDiagram</item>
    <item>  direction TB</item>
    <item></item>
    
    <!-- Generate the main class with attributes -->
    <xsl:apply-templates select="." mode="individual-class"/>
    
    <!-- Generate related classes (inheritance and associations) -->
    <xsl:if test="a:SuperType">
      <xsl:variable name="superClassName" select="a:SuperType/@name"/>
      <xsl:apply-templates select="//a:ComplexType[@name = $superClassName]|//a:Root[@name = $superClassName]" mode="related-class"/>
      
      <!-- Inheritance relationship -->
      <item>  <xsl:value-of select="$superClassName"/> &lt;|-- <xsl:value-of select="$className"/> : inherits from</item>
    </xsl:if>
    
    <!-- Generate association relationships -->
    <xsl:apply-templates select="a:Reference|a:Instance" mode="individual-associations">
      <xsl:with-param name="sourceClass" select="$className"/>
    </xsl:apply-templates>
    
    <!-- Add CSS styling if colors are specified -->
    <xsl:if test="$concreteClassesColor or $abstractClassesColor or $enumerationsColor or $cimDatatypesColor">
      <item></item>
      <item>  %% CSS Styling</item>
      <xsl:if test="$concreteClassesColor">
        <item>  classDef concreteClass fill:<xsl:value-of select="$concreteClassesColor"/><xsl:if test="$concreteClassesFontColor">,color:<xsl:value-of select="$concreteClassesFontColor"/></xsl:if></item>
      </xsl:if>
      <xsl:if test="$abstractClassesColor">
        <item>  classDef abstractClass fill:<xsl:value-of select="$abstractClassesColor"/><xsl:if test="$abstractClassesFontColor">,color:<xsl:value-of select="$abstractClassesFontColor"/></xsl:if></item>
      </xsl:if>
      <xsl:if test="$enumerationsColor">
        <item>  classDef enumClass fill:<xsl:value-of select="$enumerationsColor"/><xsl:if test="$enumerationsFontColor">,color:<xsl:value-of select="$enumerationsFontColor"/></xsl:if></item>
      </xsl:if>
    </xsl:if>
    
    <item>```</item>
    <item></item>
  </xsl:template>
  
  <!-- Template for individual class with attributes -->
  <xsl:template match="a:Root|a:ComplexType" mode="individual-class">
    <xsl:variable name="className" select="@name"/>
    <xsl:variable name="isAbstract" select="not(a:Stereotype[@label='Concrete'])"/>
    
    <item>  class <xsl:value-of select="$className"/> {</item>
    <xsl:if test="$isAbstract">
      <item>    &lt;&lt;abstract&gt;&gt;</item>
    </xsl:if>
    
    <!-- Always show attributes in individual diagrams -->
    <xsl:apply-templates select="a:Simple|a:Domain|a:Enumerated|a:Compound" mode="individual-attributes"/>
    
    <item>  }</item>
    
    <!-- Apply styling -->
    <xsl:choose>
      <xsl:when test="$isAbstract and $abstractClassesColor">
        <item>  class <xsl:value-of select="$className"/>:::abstractClass</item>
      </xsl:when>
      <xsl:when test="not($isAbstract) and $concreteClassesColor">
        <item>  class <xsl:value-of select="$className"/>:::concreteClass</item>
      </xsl:when>
    </xsl:choose>
    <item></item>
  </xsl:template>
  
  <!-- Template for related classes (shown without attributes) -->
  <xsl:template match="a:Root|a:ComplexType" mode="related-class">
    <xsl:variable name="className" select="@name"/>
    <xsl:variable name="isAbstract" select="not(a:Stereotype[@label='Concrete'])"/>
    
    <item>  class <xsl:value-of select="$className"/> {</item>
    <xsl:if test="$isAbstract">
      <item>    &lt;&lt;abstract&gt;&gt;</item>
    </xsl:if>
    <item>  }</item>
    
    <!-- Apply styling -->
    <xsl:choose>
      <xsl:when test="$isAbstract and $abstractClassesColor">
        <item>  class <xsl:value-of select="$className"/>:::abstractClass</item>
      </xsl:when>
      <xsl:when test="not($isAbstract) and $concreteClassesColor">
        <item>  class <xsl:value-of select="$className"/>:::concreteClass</item>
      </xsl:when>
    </xsl:choose>
    <item></item>
  </xsl:template>
  
  <!-- Attributes for individual class diagrams -->
  <xsl:template match="a:Simple" mode="individual-attributes">
    <xsl:variable name="dataType" select="substring-after(@cimDatatype, '#')"/>
    <item>    + <xsl:value-of select="@name"/> : <xsl:value-of select="$dataType"/><xsl:call-template name="cardinality"/></item>
  </xsl:template>
  
  <xsl:template match="a:Domain" mode="individual-attributes">
    <xsl:variable name="dataType" select="substring-after(@dataType, '#')"/>
    <item>    + <xsl:value-of select="@name"/> : <xsl:value-of select="$dataType"/><xsl:call-template name="cardinality"/></item>
  </xsl:template>
  
  <xsl:template match="a:Enumerated" mode="individual-attributes">
    <item>    + <xsl:value-of select="@name"/> : enum:<xsl:value-of select="@type"/><xsl:call-template name="cardinality"/></item>
  </xsl:template>
  
  <xsl:template match="a:Compound" mode="individual-attributes">
    <item>    + <xsl:value-of select="@name"/> : <xsl:value-of select="@type"/><xsl:call-template name="cardinality"/></item>
  </xsl:template>
  
  <!-- Individual associations -->
  <xsl:template match="a:Reference|a:Instance" mode="individual-associations">
    <xsl:param name="sourceClass"/>
    <xsl:if test="not(@hideInDiagrams = 'true')">
      <xsl:variable name="targetClass" select="@type"/>
      <xsl:variable name="targetCardinality">
        <xsl:call-template name="format-cardinality">
          <xsl:with-param name="minOccurs" select="@minOccurs"/>
          <xsl:with-param name="maxOccurs" select="@maxOccurs"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="relationshipLabel" select="@name"/>
      
      <!-- Generate target class if it exists -->
      <xsl:apply-templates select="//a:ComplexType[@name = $targetClass]|//a:Root[@name = $targetClass]|//a:EnumeratedType[@name = $targetClass]" mode="related-class"/>
      
      <!-- Generate relationship -->
      <item>    <xsl:value-of select="$sourceClass"/> --> "<xsl:value-of select="$targetCardinality"/>" <xsl:value-of select="$targetClass"/> : <xsl:value-of select="$relationshipLabel"/> </item>
    </xsl:if>
  </xsl:template>
      
      <xsl:template match="a:SimpleType" mode="mermaid">
        <xsl:if test="not($hideCIMDatatypes) and not(@hideInDiagrams = 'true')">
          <xsl:variable name="className" select="local:get-class-name(@dataType)"/>
          <item>  class <xsl:value-of select="$className"/> {</item>
          <item>    &lt;&lt;CIMDatatype&gt;&gt;</item>
          <xsl:choose>
            <xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
              <xsl:apply-templates select="a:Value|a:Unit|a:Multiplier" mode="mermaid"/>
            </xsl:when>
            <xsl:otherwise>
              <item>    [Attributes hidden]</item>
            </xsl:otherwise>
          </xsl:choose>
          <item>  }</item>
          <xsl:if test="normalize-space($cimDatatypesColor)">
            <item>  class <xsl:value-of select="$className"/>:::cimDatatypeClass</item>
          </xsl:if>
          <item></item>
        </xsl:if>
      </xsl:template>
      
      <xsl:template match="a:PrimitiveType" mode="mermaid">
        <xsl:if test="not($hidePrimitives) and not(@hideInDiagrams = 'true')">
          <xsl:variable name="className" select="local:get-class-name(@dataType)"/>
          <item>  class <xsl:value-of select="$className"/> {</item>
          <item>    &lt;&lt;Primitive&gt;&gt;</item>
          <item>  }</item>
          <xsl:if test="normalize-space($primitivesColor)">
            <item>  class <xsl:value-of select="$className"/>:::primitiveClass</item>
          </xsl:if>
          <item></item>
        </xsl:if>
      </xsl:template>
      
      <xsl:template match="a:CompoundType" mode="mermaid">
        <xsl:if test="not($hideCompounds) and not(@hideInDiagrams = 'true')">
          <xsl:variable name="className" select="local:get-class-name(@baseClass)"/>
          <item>  class <xsl:value-of select="$className"/> {</item>
          <item>    &lt;&lt;Compound&gt;&gt;</item>
          <xsl:choose>
            <xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
              <xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain" mode="mermaid"/>
            </xsl:when>
            <xsl:otherwise>
              <item>    [Attributes hidden]</item>
            </xsl:otherwise>
          </xsl:choose>
          <item>  }</item>
          <xsl:if test="normalize-space($compoundsColor)">
            <item>  class <xsl:value-of select="$className"/>:::compoundClass</item>
          </xsl:if>
          <item></item>
        </xsl:if>
      </xsl:template>
  
      <xsl:template match="a:EnumeratedType" mode="mermaid">
        <xsl:if test="not($hideEnumerations) and not(@hideInDiagrams = 'true')">
          <xsl:variable name="enumName" select="@name"/>
          <xsl:variable name="count" select="count(a:EnumeratedValue)"/>
          <item>  class <xsl:value-of select="$enumName"/> {</item>
          <item>    &lt;&lt;enumeration&gt;&gt;</item>
          <xsl:for-each select="a:EnumeratedValue[position() &lt;= 20 and not(@hideInDiagrams = 'true')]">
            <item>    <xsl:value-of select="@name"/></item>
          </xsl:for-each>
          <xsl:if test="$count > 20">
            <item>    [Remaining <xsl:value-of select="$count - 20"/> literals hidden]</item>
          </xsl:if>
          <item>  }</item>
          <xsl:if test="$enumerationsColor">
            <item>  class <xsl:value-of select="$enumName"/>:::enumClass</item>
          </xsl:if>
          <item></item>
        </xsl:if>
      </xsl:template>
      
      <xsl:template match="a:Root|a:ComplexType" mode="mermaid">
        <xsl:if test="not(@hideInDiagrams = 'true')">
          <xsl:variable name="className" select="local:get-class-name(@baseClass)"/>
          <xsl:variable name="isAbstract" select="not(a:Stereotype[@label='Concrete'])"/>
          <item>  class <xsl:value-of select="$className"/> {</item>
          <xsl:if test="$isAbstract">
            <item>    &lt;&lt;abstract&gt;&gt;</item>
          </xsl:if>
          <xsl:choose>
            <xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
              <xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain" mode="mermaid"/>
            </xsl:when>
            <xsl:otherwise>
              <item>    [Attributes hidden]</item>
            </xsl:otherwise>
          </xsl:choose>
          <item>  }</item>
          <xsl:choose>
            <xsl:when test="$isAbstract and normalize-space($abstractClassesColor)">
              <item>  class <xsl:value-of select="$className"/>:::abstractClass</item>
            </xsl:when>
            <xsl:when test="not($isAbstract) and normalize-space($concreteClassesColor)">
              <item>  class <xsl:value-of select="$className"/>:::concreteClass</item>
            </xsl:when>
          </xsl:choose>
          <item></item>
        </xsl:if>
      </xsl:template>
      
      <!-- Inheritance relationships for Mermaid -->
      <xsl:template match="a:Root|a:ComplexType" mode="mermaid-inheritance">
        <xsl:if test="not(@hideInDiagrams = 'true') and a:SuperType">
          <xsl:variable name="className" select="local:get-class-name(@baseClass)"/>
          <xsl:variable name="superClassName" select="local:get-class-name(a:SuperType/@baseClass)"/>
          <xsl:if test="not(//node()[@name = a:SuperType/@name]/@hideInDiagrams = 'true')">
            <item>  <xsl:value-of select="$superClassName"/> &lt;|-- <xsl:value-of select="$className"/></item>
          </xsl:if>
        </xsl:if>
      </xsl:template>
      
      <!-- Associations for Mermaid -->
      <xsl:template match="a:Root|a:ComplexType|a:CompoundType" mode="mermaid-associations">
        <xsl:apply-templates select="a:Reference|a:Instance" mode="mermaid-associations"/>
      </xsl:template>
      
      <xsl:template match="a:Reference|a:Instance" mode="mermaid-associations">
        <xsl:if test="not(@hideInDiagrams = 'true')">
          <xsl:variable name="baseProperty" select="@baseProperty" />
          <xsl:variable name="inverse" select="//a:InverseReference[@inverseBaseProperty = $baseProperty]|//a:InverseInstance[@inverseBaseProperty = $baseProperty]"/>
          <xsl:variable name="sourceClass" select="if ($inverse) then $inverse[1]/@type else local:get-class-name(@inverseBasePropertyClass)"/>
          <xsl:variable name="targetClass" select="@type"/>
          <xsl:variable name="sourceCardinality" select="if ($inverse) then local:format-cardinality($inverse[1]/@minOccurs, $inverse[1]/@maxOccurs) else ''"/>
          <xsl:variable name="sourceLabel" select="if ($inverse) then $inverse[1]/@name else ''"/>
          <xsl:variable name="targetCardinality" select="if (@minOccurs and @maxOccurs) then local:format-cardinality(@minOccurs, @maxOccurs) else ''"/>
          <xsl:variable name="targetLabel" select="if (@name) then @name else ''"/>
          
          <!-- Determine association type for Mermaid -->
          <xsl:variable name="associationType" select="
            if (a:Stereotype[ends-with(., '#ofAggregate') or ends-with(., '#aggregateOf')]) then 'o--'
            else if (a:Stereotype[ends-with(., '#ofComposite') or ends-with(., '#compositeOf')]) then '*--'
            else '--&gt;'
                          "/>
          
          <!-- Build the relationship string using XSLT 3.0 string concatenation -->
          <xsl:variable name="sourceLabelFormatted" select="if ($sourceCardinality || $sourceLabel) then '&quot;' || string-join(($sourceCardinality, $sourceLabel)[normalize-space()], ' ') || '&quot;' else ''"/>
          <xsl:variable name="targetLabelFormatted" select="if ($targetCardinality || $targetLabel) then '&quot;' || string-join(($targetCardinality, $targetLabel)[normalize-space()], ' ') || '&quot;' else ''"/>
          
          <item>  <xsl:value-of select="string-join(($sourceClass, $sourceLabelFormatted, $associationType, $targetLabelFormatted, $targetClass)[normalize-space()], ' ')"/></item>
          
          <!-- If target class doesn't exist, create an error class -->
          <xsl:if test="not(//a:ComplexType[@name = $targetClass]|//a:Root[@name = $targetClass]|//a:CompoundType[@name = $targetClass]|//a:EnumeratedType[@name = $targetClass]|//a:PrimitiveType[@name = $targetClass])">
            <item></item>
            <item>  class <xsl:value-of select="$targetClass"/> {</item>
            <item>    &lt;&lt;error&gt;&gt;</item>
            <item>    [Missing class definition]</item>
            <item>  }</item>
            <xsl:if test="normalize-space($errorsColor)">
              <item>  class <xsl:value-of select="$targetClass"/>:::errorClass</item>
            </xsl:if>
          </xsl:if>
        </xsl:if>
      </xsl:template>
      
      <!-- Individual class associations template -->
      <xsl:template match="a:Reference|a:Instance" mode="individual-class-associations">
        <xsl:param name="sourceClass"/>
        <xsl:if test="not(@hideInDiagrams = 'true')">
          <xsl:variable name="targetClass" select="@type"/>
          <xsl:variable name="targetCardinality" select="local:format-cardinality(@minOccurs, @maxOccurs)"/>
          <xsl:variable name="targetLabel" select="@name"/>
          <item>    <xsl:value-of select="$sourceClass"/> --&gt; "<xsl:value-of select="$targetCardinality"/>" <xsl:value-of select="$targetClass"/> : <xsl:value-of select="$targetLabel"/> </item>
        </xsl:if>
      </xsl:template>
      
      <!-- Attribute templates for Mermaid -->
      <xsl:template match="a:Simple" mode="mermaid">	
        <xsl:if test="not(@hideInDiagrams = 'true')">
          <xsl:variable name="cardinality" select="local:cardinality-display(@minOccurs, @maxOccurs)"/>
          <item>    +<xsl:value-of select="@name"/> : <xsl:value-of select="local:get-class-name(@cimDatatype)"/><xsl:value-of select="$cardinality"/></item>
        </xsl:if>
      </xsl:template>
      
      <xsl:template match="a:Domain" mode="mermaid">	
        <xsl:if test="not(@hideInDiagrams = 'true')">
          <xsl:variable name="cardinality" select="local:cardinality-display(@minOccurs, @maxOccurs)"/>
          <item>    +<xsl:value-of select="@name"/> : <xsl:value-of select="local:get-class-name(@dataType)"/><xsl:value-of select="$cardinality"/></item>
        </xsl:if>
      </xsl:template>
      
      <xsl:template match="a:Enumerated|a:Compound" mode="mermaid">
        <xsl:if test="not(@hideInDiagrams = 'true')">
          <xsl:variable name="cardinality" select="local:cardinality-display(@minOccurs, @maxOccurs)"/>
          <item>    +<xsl:value-of select="@name"/> : <xsl:value-of select="@type"/><xsl:value-of select="$cardinality"/></item>
        </xsl:if>
      </xsl:template>
      
      <xsl:template match="a:Value|a:Unit|a:Multiplier" mode="mermaid">	
        <xsl:if test="not(@hideInDiagrams = 'true')">
          <xsl:variable name="constant" select="if (@constant and normalize-space(@constant)) then ' = ' || @constant else ''"/>
          <xsl:variable name="cardinality" select="local:cardinality-display(@minOccurs, @maxOccurs)"/>
          <item>    +<xsl:value-of select="@name"/> : <xsl:value-of select="local:get-class-name(@baseClass)"/><xsl:value-of select="$cardinality"/><xsl:value-of select="$constant"/></item>
        </xsl:if>
      </xsl:template>
      
      <!-- Markdown documentation templates -->
      <xsl:template match="a:Message">
        <item>## <xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/></item>
        <item>### <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
        <item></item>
        <xsl:apply-templates mode="annotate-type"/>
        <xsl:apply-templates/>
        <item></item>
      </xsl:template>
  
  <xsl:template match="a:Root">
    <item>{#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>}</item>
    <item>### <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
    <item></item>
    <xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
    <item></item>
    
    <!-- Generate individual class diagram -->
    <xsl:call-template name="generate-individual-class-diagram">
      <xsl:with-param name="className" select="@name"/>
    </xsl:call-template>
    
    <!-- Rest of the documentation -->
    <xsl:if test="a:Stereotype[contains(., '#description')]">
      <item>&gt; **Note:** This class is tagged in this profile with the 'Description' tag. To refer to the full definition of this class as defined in the profile this one depends on visit [<xsl:value-of select="@name"/>](#{<xsl:value-of select="$fileName"/>-description-profile}-<xsl:value-of select="@name"/>).</item>
      <item></item>
    </xsl:if>
    <xsl:call-template name="type_definition"/>
    <item></item>
  </xsl:template>
      
      <xsl:template name="type_definition">
        <xsl:apply-templates mode="annotate-type"/>
        <xsl:choose>
          <xsl:when test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound|a:SuperType">
            <xsl:if test="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound">
              <item></item>
              <item>#### Native Members</item>
              <item></item>
              <item>| name | type | description | mapping |</item>
              <item>|------|------|-------------|---------|</item>
              <xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound"/>
            </xsl:if>
            <xsl:if test="a:SuperType">
              <item></item>
              <item>#### Inherited Members</item>
              <item></item>
              <item>| name | type | description | mapping |</item>
              <item>|------|------|-------------|---------|</item>
              <xsl:apply-templates select="a:SuperType" mode="inherited"/>
            </xsl:if>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="baseClass" select="@baseClass"/>
            <xsl:if test="not(child::a:Stereotype[contains(., '#concrete')]) and (count(/.//a:Reference[@baseClass=$baseClass]) gt 0)">
              <item>&gt; **Note:** This abstract class serves as a 'By Reference' association within this profile and as such has no attributes or associations defined. Rather, it is used to reference a corresponding concrete class defined in an external profile that this one depends upon.</item>
              <item></item>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:template>
      
      <xsl:template match="a:SuperType" mode="inheritance_hierarchy">
        <xsl:variable name="inheritance">
          [<xsl:value-of select="@name"/>](#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>)<xsl:variable name="supertype_name" select="@name"/><xsl:if test="/*/node()[@name = $supertype_name]/a:SuperType"> => <xsl:apply-templates select="/*/node()[@name = $supertype_name]/a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
        </xsl:variable>
        <xsl:value-of select="$inheritance"/>
      </xsl:template>
      
      <xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Compound|a:Domain">
        <xsl:variable name="roles">
          <xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
          <xsl:call-template name="attribute-stereotype-roles"/> 
        </xsl:variable>
        <item>| <xsl:if test="$roles != ''">`</xsl:if><xsl:value-of select="@name"/> [<xsl:value-of select="local:format-cardinality(@minOccurs, @maxOccurs)"/>]<xsl:if test="$roles != ''">`</xsl:if><xsl:text> </xsl:text><xsl:call-template name="process-attribute-stereotypes"/> | [<xsl:value-of select="@type"/>](#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>) | <xsl:apply-templates mode="annotate-table-cell" select="a:Comment|a:Note"/> | <xsl:apply-templates mode="annotate-table-cell" select="a:AsciiDoc"/> |</item>
      </xsl:template>
      
      <xsl:template match="a:Simple">
        <xsl:variable name="roles">
          <xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
          <xsl:call-template name="attribute-stereotype-roles"/> 
        </xsl:variable>
        <item>| <xsl:if test="$roles != ''">`</xsl:if><xsl:value-of select="@name"/> [<xsl:value-of select="local:format-cardinality(@minOccurs, @maxOccurs)"/>]<xsl:if test="$roles != ''">`</xsl:if><xsl:text> </xsl:text><xsl:call-template name="process-attribute-stereotypes"/> | [<xsl:value-of select="substring-after(@cimDatatype, '#')"/>](#<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>) | <xsl:apply-templates mode="annotate-table-cell" select="a:Comment|a:Note"/> | <xsl:apply-templates mode="annotate-table-cell" select="a:AsciiDoc"/> |</item>
      </xsl:template>
      
      <xsl:template match="a:SuperType" mode="inherited">
        <xsl:apply-templates select="//a:ComplexType[@name=current()/@name]" mode="inherited"/>
        <xsl:apply-templates select="//a:Root[@name=current()/@name]" mode="inherited"/>
      </xsl:template>
      
      <xsl:template match="a:ComplexType|a:Root" mode="inherited">
        <xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound" mode="inherited"/>
        <xsl:apply-templates select="a:SuperType" mode="inherited"/>
      </xsl:template>
      
      <xsl:template match="a:Instance|a:Reference|a:Enumerated|a:Compound|a:Domain" mode="inherited">
        <xsl:variable name="roles">
          <xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
          <xsl:call-template name="attribute-stereotype-roles"/> 
        </xsl:variable>
        <item>| <xsl:if test="$roles != ''">`</xsl:if><xsl:value-of select="@name"/> [<xsl:value-of select="local:format-cardinality(@minOccurs, @maxOccurs)"/>]<xsl:if test="$roles != ''">`</xsl:if><xsl:text> </xsl:text><xsl:call-template name="process-attribute-stereotypes"/> | [<xsl:value-of select="@type"/>](#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@type"/>) | see [<xsl:value-of select="../@name"/>](<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>) | <xsl:apply-templates mode="annotate-table-cell" select="a:AsciiDoc"/> |</item>
      </xsl:template>
      
      <xsl:template match="a:Simple" mode="inherited">
        <xsl:variable name="roles">
          <xsl:if test="not(starts-with(@baseProperty, concat($ontologyURI, '#')))">.extension</xsl:if>
          <xsl:call-template name="attribute-stereotype-roles"/> 
        </xsl:variable>
        <item>| <xsl:if test="$roles != ''">`</xsl:if><xsl:value-of select="@name"/> [<xsl:value-of select="local:format-cardinality(@minOccurs, @maxOccurs)"/>]<xsl:if test="$roles != ''">`</xsl:if><xsl:text> </xsl:text><xsl:call-template name="process-attribute-stereotypes"/> | [<xsl:value-of select="substring-after(@cimDatatype, '#')"/>](#<xsl:value-of select="$fileName"/>-<xsl:value-of select="substring-after(@cimDatatype, '#')"/>) | see [<xsl:value-of select="../@name"/>](#<xsl:value-of select="$fileName"/>-<xsl:value-of select="../@name"/>) | <xsl:apply-templates mode="annotate-table-cell" select="a:AsciiDoc"/> |</item>
      </xsl:template>
  
      <xsl:template match="a:ComplexType">
        <item>{#<xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/>}</item>
        <item>### <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
        <item></item>
        <xsl:if test="a:SuperType">Inheritance path = <xsl:apply-templates select="a:SuperType" mode="inheritance_hierarchy"/></xsl:if>
        <item></item>
        
        <!-- Generate individual class diagram -->
        <xsl:call-template name="generate-individual-class-diagram">
          <xsl:with-param name="className" select="@name"/>
        </xsl:call-template>
        
        <!-- Rest of the documentation -->
        <xsl:if test="a:Stereotype[contains(., '#description')]">
          <item>&gt; **Note:** This class is tagged in this profile with the 'Description' tag. To refer to the full definition of this class as defined in the profile this one depends on visit [<xsl:value-of select="@name"/>](#{<xsl:value-of select="$fileName"/>-description-profile}-<xsl:value-of select="@name"/>).</item>
          <item></item>
        </xsl:if>
        <xsl:call-template name="type_definition"/>
        <item></item>
      </xsl:template>
      
      <xsl:template match="a:CompoundType">
        <item>## <xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/></item>
        <item>### <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
        <item></item>
        <xsl:apply-templates mode="annotate-type"/>
        <!-- Add individual class diagram -->
        <xsl:call-template name="generate-individual-class-diagram">
          <xsl:with-param name="className" select="@name"/>
        </xsl:call-template>
        <item></item>
        <item>#### Members</item>
        <item></item>
        <item>| name | type | description | mapping |</item>
        <item>|------|------|-------------|---------|</item>		
        <xsl:apply-templates select="a:Domain|a:Simple|a:Instance|a:Reference|a:Enumerated|a:Compound"/>
        <item></item>
      </xsl:template>
      
      <xsl:template match="a:SimpleType">
        <item>## <xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/></item>
        <item>### <xsl:value-of select="@name"/></item>
        <item></item>
        <xsl:apply-templates mode="annotate-type"/>
        <item></item>
        <item>XSD type: `<xsl:value-of select="@xstype"/>`</item>
        <item></item>
      </xsl:template>
      
      <xsl:template match="a:PrimitiveType">
        <item>## <xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/></item>
        <item>### <xsl:value-of select="@name"/></item>
        <item></item>
        <xsl:apply-templates mode="annotate-type"/>
        <item></item>
        <item>XSD type: `<xsl:value-of select="@xstype"/>`</item>
        <item></item>
      </xsl:template>
      
      <xsl:template match="a:EnumeratedType">
        <item><xsl:value-of select="$fileName"/>-<xsl:value-of select="@name"/></item>
        <item>### <xsl:call-template name="process-class-stereotypes"/><xsl:value-of select="@name"/></item>
        <item></item>
        <xsl:apply-templates mode="annotate-type"/>
        <item></item>
        <item>| name | description |</item>
        <item>|------|-------------|</item>
        <xsl:apply-templates/>
        <item></item>
      </xsl:template>
      
      <xsl:template match="a:EnumeratedValue">
        <item>| <xsl:value-of select="@name"/> | <xsl:apply-templates select="a:Comment|a:Note" mode="annotate-table-cell"/> |</item>
      </xsl:template>
      
      <!-- Template to match Note elements -->
      <xsl:template match="a:Note" mode="profile-notes">
        <xsl:variable name="paragraph" select="translate(., '&quot;', '')"/>
        <xsl:call-template name="parse-notes">
          <xsl:with-param name="notes" select="$paragraph"/>
        </xsl:call-template>
      </xsl:template>
      
      <xsl:template name="parse-notes">
        <xsl:param name="notes" />
        <xsl:choose>
          <xsl:when test="string-length($notes) le 80">
            <xsl:value-of select="$notes"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="cutPos" select="string-length(substring($notes, 1, 80)) + string-length(substring-before(substring($notes, 81), ' ')) + 1"/>
            <xsl:value-of select="substring($notes, 1, $cutPos)"/>
            <xsl:text>&#10;</xsl:text>
            <xsl:call-template name="parse-notes">
              <xsl:with-param name="notes" select="substring($notes, $cutPos + 1)"/>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:template>
      
      <xsl:template name="process-class-stereotypes">
        <xsl:variable name="excludedStereotypes" select="('#concrete', '#byreference', '#enumeration', '#compound', '#cimdatatype', '#primitive', '#compositeOf', '#ofComposite', '#aggregateOf', '#ofAggregate', '#hideondiagrams', '#shadowextension')"/>
        <xsl:variable name="relevantStereotypes" select="a:Stereotype[not(some $excluded in $excludedStereotypes satisfies contains(., $excluded))]"/>
        <xsl:if test="exists($relevantStereotypes)"> (<xsl:value-of select="string-join($relevantStereotypes/@label, ', ')"/>)<xsl:text> </xsl:text></xsl:if>
      </xsl:template>
      
      <xsl:template name="process-attribute-stereotypes">
        <xsl:variable name="excludedStereotypes" select="('#attribute', '#byreference', '#enumeration', 'compound', 'cimdatatype')"/>
        <xsl:variable name="relevantStereotypes" select="a:Stereotype[not(some $excluded in $excludedStereotypes satisfies contains(., $excluded))]"/>
        <xsl:if test="exists($relevantStereotypes)"> (<xsl:value-of select="string-join($relevantStereotypes/@label, ', ')"/>)</xsl:if>
      </xsl:template>
      
      <xsl:template name="attribute-stereotype-roles">
        <xsl:variable name="excludedStereotypes" select="('#attribute', '#byreference', '#enumeration', 'compound', 'cimdatatype')"/>
        <xsl:variable name="relevantStereotypes" select="a:Stereotype[not(some $excluded in $excludedStereotypes satisfies contains(., $excluded))]"/>
        <xsl:if test="exists($relevantStereotypes)"><xsl:value-of select="string-join(for $s in $relevantStereotypes return '.' || $s/@label, '')"/></xsl:if>
      </xsl:template>
      
      <xsl:template match="a:Comment|a:Note" mode="annotate-type">
        <item><xsl:value-of select="local:replace-chars(., $asciidoc-restricted)"/></item>
        <!-- Below accounts for extra line spacing between paragraphs - DO NOT REMOVE -->
        <item></item>
      </xsl:template>
      
      <xsl:template match="a:AsciiDoc" mode="annotate-type">
        <item><xsl:value-of select="."/></item>
      </xsl:template>
      
      <xsl:template match="a:Comment|a:Note" mode="annotate-table-cell">
        <xsl:value-of select="local:replace-chars(., $asciidoc-table-sensitive)"/>
      </xsl:template>
      
      <xsl:template match="a:AsciiDoc" mode="annotate-table-cell">
        <xsl:value-of select="local:replace-chars(., $asciidoc-table-sensitive)"/>
      </xsl:template>
      
      <xsl:template match="node()" mode="annotate-type">
      </xsl:template>
      
      <xsl:template match="node()" mode="annotate-table-cell">
      </xsl:template>
      
      <xsl:template match="node()">
      </xsl:template>
      
      <xsl:template match="text()">
        <!--  dont pass text through  -->
      </xsl:template>
  
  
  <!-- Helper template for cardinality formatting in attributes -->
  <xsl:template name="cardinality">
    <xsl:choose>
      <xsl:when test="$hideCardinalityForRequiredAttributes and (@minOccurs = 1)">
        <xsl:if test="not(@minOccurs = @maxOccurs)">
          <xsl:text> [</xsl:text>
          <xsl:value-of select="@minOccurs"/>
          <xsl:text>..</xsl:text>
          <xsl:value-of select="replace(replace(@maxOccurs, 'unbounded', '*'), 'n', '*')"/>
          <xsl:text>]</xsl:text>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text> [</xsl:text>
        <xsl:choose>
          <xsl:when test="@minOccurs = @maxOccurs">
            <xsl:value-of select="@minOccurs"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@minOccurs"/>
            <xsl:text>..</xsl:text>
            <xsl:value-of select="replace(replace(@maxOccurs, 'unbounded', '*'), 'n', '*')"/>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text>]</xsl:text>
      </xsl:otherwise>
    </xsl:choose>	
  </xsl:template>
  
  <!-- Helper template for format-cardinality with parameters -->
  <xsl:template name="format-cardinality">
    <xsl:param name="minOccurs"/>
    <xsl:param name="maxOccurs"/>
    <xsl:choose>
      <xsl:when test="$minOccurs = $maxOccurs">
        <xsl:value-of select="$minOccurs"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$minOccurs"/>
        <xsl:text>..</xsl:text>
        <xsl:value-of select="replace(replace($maxOccurs, 'unbounded', '*'), 'n', '*')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>