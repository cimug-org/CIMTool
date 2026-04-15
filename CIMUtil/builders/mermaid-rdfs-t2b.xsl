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
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:a="http://langdale.com.au/2005/Message#" xmlns="http://langdale.com.au/2009/Indent">
    
    <xsl:output indent="no" method="xml" encoding="UTF-8" omit-xml-declaration="yes"/>
    <xsl:param name="copyright-single-line" />
    <xsl:param name="version"/>
    <xsl:param name="baseURI"/>
    <xsl:param name="ontologyURI"/>
    <xsl:param name="envelope">Profile</xsl:param>
    <!-- All of the following params correspond to Mermaid preferences -->
    <xsl:param name="builderParameters"/>
    <!-- Tokenize by '|' to get name=value parameter pairs -->
    <xsl:variable name="pairs" select="tokenize($builderParameters, '\|')"/>
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
    
    <!-- Boolean parameters with safe handling -->
    <xsl:variable name="enableDarkModeValue" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'enableDarkMode=')) then $pair else ())[1], '=')"/>
    <xsl:param name="enableDarkMode" as="xs:boolean" select="if ($enableDarkModeValue != '' and $enableDarkModeValue != 'false') then xs:boolean($enableDarkModeValue) else false()"/>
    
    <xsl:variable name="enableShadowingValue" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'enableShadowing=')) then $pair else ())[1], '=')"/>
    <xsl:param name="enableShadowing" as="xs:boolean" select="if ($enableShadowingValue != '' and $enableShadowingValue != 'false') then xs:boolean($enableShadowingValue) else false()"/>
    
    <xsl:param name="enumerationsColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'enumerationsColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="enumerationsFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'enumerationsFontColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="errorsColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'errorsColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="errorsFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'errorsFontColor=')) then $pair else ())[1], '=')"/>
    
    <xsl:variable name="hideCardinalityForRequiredAttributesValue" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'hideCardinalityForRequiredAttributes=')) then $pair else ())[1], '=')"/>
    <xsl:param name="hideCardinalityForRequiredAttributes" as="xs:boolean" select="if ($hideCardinalityForRequiredAttributesValue != '' and $hideCardinalityForRequiredAttributesValue != 'false') then xs:boolean($hideCardinalityForRequiredAttributesValue) else false()"/>
    
    <xsl:variable name="hideCIMDatatypesValue" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'hideCIMDatatypes=')) then $pair else ())[1], '=')"/>
    <xsl:param name="hideCIMDatatypes" as="xs:boolean" select="if ($hideCIMDatatypesValue != '' and $hideCIMDatatypesValue != 'false') then xs:boolean($hideCIMDatatypesValue) else false()"/>
    
    <xsl:variable name="hideCompoundsValue" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'hideCompounds=')) then $pair else ())[1], '=')"/>
    <xsl:param name="hideCompounds" as="xs:boolean" select="if ($hideCompoundsValue != '' and $hideCompoundsValue != 'false') then xs:boolean($hideCompoundsValue) else false()"/>
    
    <xsl:variable name="hideEnumerationsValue" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'hideEnumerations=')) then $pair else ())[1], '=')"/>
    <xsl:param name="hideEnumerations" as="xs:boolean" select="if ($hideEnumerationsValue != '' and $hideEnumerationsValue != 'false') then xs:boolean($hideEnumerationsValue) else false()"/>
    
    <xsl:variable name="hidePrimitivesValue" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'hidePrimitives=')) then $pair else ())[1], '=')"/>
    <xsl:param name="hidePrimitives" as="xs:boolean" select="if ($hidePrimitivesValue != '' and $hidePrimitivesValue != 'false') then xs:boolean($hidePrimitivesValue) else false()"/>
    
    <xsl:param name="mermaidTheme" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'mermaidTheme=')) then $pair else ())[1], '=')"/>
    <xsl:param name="primitivesColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'primitivesColor=')) then $pair else ())[1], '=')"/>
    <xsl:param name="primitivesFontColor" select="substring-after((for $pair in $pairs return if (starts-with($pair, 'primitivesFontColor=')) then $pair else ())[1], '=')"/> 
        
    <xsl:template match="a:Catalog">
        <document>
            <list begin="classDiagram" indent="" delim="" end="">
                <item>  direction TB</item>
                <item></item>
                <!-- Add profile information as a note -->
                <xsl:if test="not(@hideInDiagrams = 'true')">
                    <xsl:variable name="notename" select="concat(replace($envelope, '[\s:.,\[\]{}\(\)&lt;&gt;\-\+#\*=]', '_'), 'Note')" />
                    <item>  note "Profile: <xsl:value-of select="$envelope"/>&#10;Namespace: <xsl:value-of select="$baseURI"/><xsl:if test="a:Note and a:Note[string-length(.) > 0]">&#10;&#10;Profile Notes:&#10;<xsl:apply-templates select="a:Note" mode="profile-notes"/></xsl:if>"</item>
                    <item></item>
                </xsl:if>
                
                <!-- Process all types -->
                <xsl:apply-templates select="a:Root|a:ComplexType|a:EnumeratedType|a:CompoundType|a:SimpleType|a:PrimitiveType"/>
                
                <!-- Process inheritance relationships -->
                <xsl:apply-templates select="a:Root|a:ComplexType" mode="inheritance"/>
                
                <!-- Process associations -->
                <xsl:apply-templates select="a:Root|a:ComplexType|a:CompoundType" mode="associations"/>
                
                <!-- Add CSS styling if colors are specified -->
                <xsl:if test="$concreteClassesColor or $abstractClassesColor or $enumerationsColor or $cimDatatypesColor or $compoundsColor or $primitivesColor or $errorsColor">
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
                    <xsl:if test="$cimDatatypesColor">
                        <item>  classDef cimDatatypeClass fill:<xsl:value-of select="$cimDatatypesColor"/><xsl:if test="$cimDatatypesFontColor">,color:<xsl:value-of select="$cimDatatypesFontColor"/></xsl:if></item>
                    </xsl:if>
                    <xsl:if test="$compoundsColor">
                        <item>  classDef compoundClass fill:<xsl:value-of select="$compoundsColor"/><xsl:if test="$compoundsFontColor">,color:<xsl:value-of select="$compoundsFontColor"/></xsl:if></item>
                    </xsl:if>
                    <xsl:if test="$primitivesColor">
                        <item>  classDef primitiveClass fill:<xsl:value-of select="$primitivesColor"/><xsl:if test="$primitivesFontColor">,color:<xsl:value-of select="$primitivesFontColor"/></xsl:if></item>
                    </xsl:if>
                    <xsl:if test="$errorsColor">
                        <item>  classDef errorClass fill:<xsl:value-of select="$errorsColor"/><xsl:if test="$errorsFontColor">,color:<xsl:value-of select="$errorsFontColor"/></xsl:if></item>
                    </xsl:if>
                </xsl:if>
            </list>
        </document>
    </xsl:template>
    
    <xsl:template match="a:EnumeratedType">
        <xsl:if test="not($hideEnumerations) and not(@hideInDiagrams = 'true')">
            <xsl:variable name="enumName" select="substring-after(@baseClass, '#')"/>
            <xsl:variable name="count" select="count(a:EnumeratedValue)"/>
            <list begin="  class {$enumName} {{" indent="    " delim="" end="  }}">
                <item>&lt;&lt;enumeration&gt;&gt;</item>
                <xsl:for-each select="a:EnumeratedValue[position() &lt;= 20]">
                    <item><xsl:value-of select="substring-after(substring-after(@baseResource, '#'), '.')" /></item>
                </xsl:for-each>
                <xsl:if test="$count > 20">
                    <item>[Remaining <xsl:value-of select="$count - 20"/> literals hidden]</item>
                </xsl:if>
            </list>
            <xsl:if test="$enumerationsColor">
                <item>  class <xsl:value-of select="$enumName"/>:::enumClass</item>
            </xsl:if>
            <item></item>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="a:SimpleType">
        <xsl:if test="not($hideCIMDatatypes) and not(@hideInDiagrams = 'true')">
            <xsl:variable name="className" select="substring-after(@dataType, '#')"/>
            <list begin="  class {$className} {{" indent="    " delim="" end="  }}">
                <item>&lt;&lt;CIMDatatype&gt;&gt;</item>
                <xsl:choose>
                    <xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
                        <xsl:apply-templates select="a:Value|a:Unit|a:Multiplier"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <item>[Attributes hidden]</item>
                    </xsl:otherwise>
                </xsl:choose>
            </list>
            <xsl:if test="$cimDatatypesColor">
                <item>  class <xsl:value-of select="$className"/>:::cimDatatypeClass</item>
            </xsl:if>
            <item></item>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="a:PrimitiveType">
        <xsl:if test="not($hidePrimitives) and not(@hideInDiagrams = 'true')">
            <xsl:variable name="className" select="substring-after(@dataType, '#')"/>
            <list begin="  class {$className} {{" indent="    " delim="" end="  }}">
                <item>&lt;&lt;Primitive&gt;&gt;</item>
            </list>
            <xsl:if test="$primitivesColor">
                <item>  class <xsl:value-of select="$className"/>:::primitiveClass</item>
            </xsl:if>
            <item></item>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="a:CompoundType">
        <xsl:if test="not($hideCompounds) and not(@hideInDiagrams = 'true')">
            <xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
            <list begin="  class {$className} {{" indent="    " delim="" end="  }}">
                <item>&lt;&lt;Compound&gt;&gt;</item>
                <xsl:choose>
                    <xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
                        <xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <item>[Attributes hidden]</item>
                    </xsl:otherwise>
                </xsl:choose>							
            </list>
            <xsl:if test="$compoundsColor">
                <item>  class <xsl:value-of select="$className"/>:::compoundClass</item>
            </xsl:if>
            <item></item>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="a:Root|a:ComplexType">
        <xsl:if test="not(@hideInDiagrams = 'true')">
            <xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
            <xsl:variable name="isAbstract" select="not(a:Stereotype[@label='Concrete'])"/>
            <list begin="  class {$className} {{" indent="    " delim="" end="  }}">
                <xsl:if test="$isAbstract">
                    <item>&lt;&lt;abstract&gt;&gt;</item>
                </xsl:if>
                <xsl:choose>
                    <xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
                        <xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:SimpleCompound|a:Simple|a:Domain"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <item>[Attributes hidden]</item>
                    </xsl:otherwise>
                </xsl:choose>
            </list>
            <xsl:choose>
                <xsl:when test="$isAbstract and $abstractClassesColor">
                    <item>  class <xsl:value-of select="$className"/>:::abstractClass</item>
                </xsl:when>
                <xsl:when test="not($isAbstract) and $concreteClassesColor">
                    <item>  class <xsl:value-of select="$className"/>:::concreteClass</item>
                </xsl:when>
            </xsl:choose>
            <item></item>
        </xsl:if>
    </xsl:template>
    
    <!-- Inheritance relationships -->
    <xsl:template match="a:Root|a:ComplexType" mode="inheritance">
        <xsl:if test="not(@hideInDiagrams = 'true') and a:SuperType">
            <xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
            <xsl:variable name="superClassName" select="substring-after(a:SuperType/@baseClass, '#')"/>
            <xsl:if test="not(//node()[@name = $superClassName]/@hideInDiagrams = 'true')">
                <item>  <xsl:value-of select="$superClassName"/> &lt;|-- <xsl:value-of select="$className"/></item>
            </xsl:if>
        </xsl:if>
    </xsl:template>
    
    <!-- Associations -->
    <xsl:template match="a:Root|a:ComplexType|a:CompoundType" mode="associations">
        <xsl:apply-templates select="a:Reference|a:Instance" mode="associations"/>
    </xsl:template>
    
    <xsl:template match="a:Reference|a:Instance" mode="associations">
        <xsl:if test="not(@hideInDiagrams = 'true')">
            <xsl:variable name="baseProperty" select="@baseProperty" />
            <xsl:variable name="inverse" select="//a:InverseReference[@inverseBaseProperty = $baseProperty]|//a:InverseInstance[@inverseBaseProperty = $baseProperty]"/>
            <xsl:variable name="sourceClass">
                <xsl:choose>
                    <xsl:when test="$inverse"><xsl:value-of select="$inverse[1]/@type"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="substring-after(@inverseBasePropertyClass, '#')"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="targetClass"><xsl:value-of select="@type"/></xsl:variable>
            <xsl:variable name="sourceCardinality">
                <xsl:choose>
                    <xsl:when test="$inverse">
                        <xsl:call-template name="format-cardinality">
                            <xsl:with-param name="minOccurs" select="$inverse[1]/@minOccurs"/>
                            <xsl:with-param name="maxOccurs" select="$inverse[1]/@maxOccurs"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
                </xsl:choose>
            </xsl:variable> 
            <xsl:variable name="sourceLabel">
                <xsl:choose>
                    <xsl:when test="$inverse"><xsl:value-of select="$inverse[1]/@name"/></xsl:when>
                    <xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="targetCardinality">
                <xsl:choose>
                    <xsl:when test="not(@minOccurs = '') and not(@maxOccurs = '')">
                        <xsl:call-template name="format-cardinality">
                            <xsl:with-param name="minOccurs" select="@minOccurs"/>
                            <xsl:with-param name="maxOccurs" select="@maxOccurs"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="targetLabel">
                <xsl:choose>
                    <xsl:when test="not(@name = '')"><xsl:value-of select="@name"/></xsl:when>
                    <xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            
            <!-- Determine association type for Mermaid -->
            <xsl:variable name="associationType">
                <xsl:choose>
                    <xsl:when test="a:Stereotype[substring-after(., '#') = 'ofAggregate'] or a:Stereotype[substring-after(., '#') = 'aggregateOf']">o--</xsl:when>
                    <xsl:when test="a:Stereotype[substring-after(., '#') = 'ofComposite'] or a:Stereotype[substring-after(., '#') = 'compositeOf']">*--</xsl:when>
                    <xsl:otherwise>--&gt;</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            
            <!-- Build the relationship string -->
            <xsl:variable name="relationshipString">
                <xsl:value-of select="$sourceClass"/>
                <xsl:text> </xsl:text>
                <xsl:if test="$sourceCardinality != '' or $sourceLabel != ''">
                    <xsl:text>"</xsl:text>
                    <xsl:if test="$sourceCardinality != ''"><xsl:value-of select="$sourceCardinality"/></xsl:if>
                    <xsl:if test="$sourceLabel != ''">
                        <xsl:if test="$sourceCardinality != ''"><xsl:text> </xsl:text></xsl:if>
                        <xsl:value-of select="$sourceLabel"/>
                    </xsl:if>
                    <xsl:text>" </xsl:text>
                </xsl:if>
                <xsl:value-of select="$associationType"/>
                <xsl:if test="$targetCardinality != '' or $targetLabel != ''">
                    <xsl:text> "</xsl:text>
                    <xsl:if test="$targetCardinality != ''"><xsl:value-of select="$targetCardinality"/></xsl:if>
                    <xsl:if test="$targetLabel != ''">
                        <xsl:if test="$targetCardinality != ''"><xsl:text> </xsl:text></xsl:if>
                        <xsl:value-of select="$targetLabel"/>
                    </xsl:if>
                    <xsl:text>"</xsl:text>
                </xsl:if>
                <xsl:text> </xsl:text>
                <xsl:value-of select="$targetClass"/>
            </xsl:variable>
            
            <item>  <xsl:value-of select="$relationshipString"/></item>
            
            <!-- If target class doesn't exist, create an error class -->
            <xsl:if test="not(//a:ComplexType[@name = $targetClass]|//a:Root[@name = $targetClass]|//a:CompoundType[@name = $targetClass]|//a:EnumeratedType[@name = $targetClass]|//a:PrimitiveType[@name = $targetClass])">
                <item></item>
                <list begin="  class {$targetClass} {{" indent="    " delim="" end="  }}">
                    <item>&lt;&lt;error&gt;&gt;</item>
                    <item>[Missing class definition]</item>
                </list>
                <xsl:if test="$errorsColor">
                    <item>  class <xsl:value-of select="$targetClass"/>:::errorClass</item>
                </xsl:if>
            </xsl:if>
        </xsl:if>
    </xsl:template>
    
    <!-- Attribute templates -->
    <xsl:template match="a:Simple">	
        <xsl:if test="not(@hideInDiagrams = 'true')">
            <item>+<xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@cimDatatype, '#')"/><xsl:call-template name="cardinality"/></item>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="a:Domain">	
        <xsl:if test="not(@hideInDiagrams = 'true')">
            <item>+<xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@dataType, '#')"/><xsl:call-template name="cardinality"/></item>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="a:Enumerated|a:Compound">
        <xsl:if test="not(@hideInDiagrams = 'true')">
            <item>+<xsl:value-of select="@name"/> : <xsl:value-of select="@type"/><xsl:call-template name="cardinality"/></item>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="a:Value|a:Unit|a:Multiplier">	
        <xsl:if test="not(@hideInDiagrams = 'true')">
            <xsl:variable name="constant" select="if (@constant and not(@constant = '')) then concat(' = ', @constant) else ''"/>
            <item>+<xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@baseClass, '#')"/><xsl:call-template name="cardinality"/><xsl:value-of select="$constant"/></item>
        </xsl:if>
    </xsl:template>
    
    <!-- Helper templates -->
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
            <xsl:when test="(string-length($notes) &lt;= 80)">
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
    
    <xsl:template match="text()">
        <!--  dont pass text through  -->
    </xsl:template>
    
</xsl:stylesheet>