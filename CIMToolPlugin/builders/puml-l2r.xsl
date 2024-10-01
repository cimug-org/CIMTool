<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:a="http://langdale.com.au/2005/Message#" xmlns="http://langdale.com.au/2009/Indent">
	
	<xsl:output indent="no" method="xml" encoding="utf-8" omit-xml-declaration="yes"/>
    <xsl:param name="copyright-single-line" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="ontologyURI"/>
	<xsl:param name="envelope">Profile</xsl:param>

	<xsl:template match="a:Catalog">
		<document>
			<list begin="@startuml" indent="" delim="" end="@enduml">
				<item>left to right direction</item>
				<item>hide empty methods</item>
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<xsl:if test="not(@hideInDiagrams = 'true')">
					<list begin="{concat('skinparam note ', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
						<item>FontSize 14</item>
						<item>Font Bold</item>
					</list>
					<item>' Add a note towards the upper left corner of the diagram</item>
					<list begin="note as InfoNote #lightyellow" indent="   " delim="" end="end note">
						<item>Profile: <xsl:value-of select="$envelope"/></item>
						<item>Namespace: <xsl:value-of select="$baseURI"/></item>
						<xsl:if test="$copyright-single-line and $copyright-single-line != ''">
							<item>Copyright: <xsl:value-of select="$copyright-single-line" disable-output-escaping="yes"/></item>			
						</xsl:if>
						<xsl:if test="a:Note and a:Note[string-length(.) > 0]">
							<item></item>
							<item>Profile Notes:</item>
							<xsl:apply-templates select="a:Note" mode="profile-notes"/>
						</xsl:if>
					</list>
					<item>&#xD;&#xA;</item> <!-- CR/LF -->
				</xsl:if>
				<xsl:apply-templates select="a:Root|a:ComplexType|a:EnumeratedType"/>			
			</list>
		</document>
	</xsl:template>

	<xsl:template match="a:EnumeratedType">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="enumName" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="count" select="count(a:EnumeratedValue)"/>
			<list begin="" indent="" delim="" end="">
				<item>' Enumeration <xsl:value-of select="$enumName"/></item>
				<list begin="enum {concat($enumName, ' ', $stereotypes, ' ', '#lightgreen', ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
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
	
	<xsl:template match="a:Root|a:ComplexType">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="className" select="substring-after(@baseClass, '#')"/>
			<xsl:variable name="stereotypes"><xsl:call-template name="stereotypes"/></xsl:variable>
			<xsl:variable name="color">
				<xsl:choose>
					<!-- light yellow for classes with the 'Description' stereotype. Include separately to easily updated the color. -->
					<xsl:when test="a:Stereotype[contains(., '#description')]">#lightyellow</xsl:when>
					<!-- light gray for classes that are abstract stereotype -->
					<xsl:when test="not(a:Stereotype[contains(., '#concrete')])">#lightgray</xsl:when>
					<!-- light yellow for all concrete classes -->
					<xsl:otherwise>#lightyellow</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="a:SuperType">
					<xsl:variable name="superClassName" select="substring-after(a:SuperType/@baseClass, '#')"/>
					<list begin="" indent="" delim="" end="">
						<item>' <xsl:value-of select="$className"/> inherits from <xsl:value-of select="$superClassName"/></item>
						<list begin="{concat(if (not(a:Stereotype[@label='Concrete'])) then 'abstract' else 'class', ' ', $className, ' ',$stereotypes, ' ', $color, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Complex|a:Enumerated|a:SimpleEnumerated|a:Simple|a:Domain|a:Choice"/>
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
							<xsl:when test="a:Reference|a:Instance">
								<xsl:apply-templates select="a:Reference|a:Instance"/>
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
						<list begin="{concat(if (not(a:Stereotype[@label='Concrete'])) then 'abstract' else 'class', ' ', $className, ' ',$stereotypes, ' ', $color, ' &#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
							<xsl:choose>
								<xsl:when test="not(a:Stereotype[contains(., '#diagramshideallattributes')])">
									<xsl:apply-templates select="a:Complex|a:Enumerated|a:SimpleEnumerated|a:Simple|a:Domain|a:Choice"/>
								</xsl:when>
								<xsl:otherwise>
									<item>[Attributes hidden]</item>
								</xsl:otherwise>
							</xsl:choose>							
						</list>
						<!-- Now process all associations: -->
						<xsl:choose>
							<xsl:when test="a:Reference|a:Instance">
								<xsl:apply-templates select="a:Reference|a:Instance"/>
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
	</xsl:template>
	
	<xsl:template match="a:Reference|a:Instance">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
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
					<xsl:when test="$inverse"><xsl:value-of select="if ($inverse[1]/@minOccurs = $inverse[1]/@maxOccurs) then $inverse[1]/@minOccurs else concat($inverse[1]/@minOccurs, '..', replace(replace($inverse[1]/@maxOccurs, 'unbounded', '*'), 'n', '*'))"/></xsl:when>
					<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
				</xsl:choose>
			</xsl:variable> 
			<xsl:variable name="sourceRoleEndName">
				<xsl:choose>
					<xsl:when test="$inverse"><xsl:value-of select="concat('+', $inverse[1]/@name)"/></xsl:when>
					<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="targetCardinality">
				<xsl:choose>
					<xsl:when test="not(@minCOccurs = '') and not(@maxOccurs = '')"><xsl:call-template name="cardinality"/></xsl:when>
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
					<!-- there are an edge case where the inverse will not be provided in the intermediary profile format.  If this case a $sourceClass will be empty -->
					<!-- In that case we must obtain the $sourceClass from -->
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'aggregateOf']">o--&gt;</xsl:when>
					<xsl:when test="a:Stereotype[substring-after(., '#') = 'compositOf']">*--&gt;</xsl:when>
					<xsl:otherwise>--&gt;</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>

			<!-- Output the association -->	
			<item><xsl:value-of select="concat($sourceClass, ' ', '&quot;', $sourceRoleEndName, ' ', $sourceCardinality, '&quot;', ' ', $associationType, ' ', '&quot;', $targetRoleEndName, ' ', $targetCardinality, '&quot;', ' ', $targetClass)"/><xsl:if test="a:Stereotype[contains(., '#enumeration')] or a:Stereotype[contains(., '#compound')] or (self::a:Reference and not(a:Stereotype[contains(., '#byreference')]))">#red</xsl:if></item>
			
			<!-- If none of the below four types of elements is defined as a top level class for $targetClass then it means that the class has 
				 not yet been pulled into the profile and therefore should be flagged as an error (i.e. expressed as class in pink) -->
			<xsl:if test="not(//a:ComplexType[@name = $targetClass]|//a:Root[@name = $targetClass]|//a:CompoundType[@name = $targetClass]|//a:EnumeratedType[@name = $targetClass])">
				<item>&#xD;&#xA;</item> <!-- CR/LF -->
				<list begin="" indent="" delim="" end="">
					<item>' This abstract indicates an "orphan" reference on an invalid Reference/Instance that must be fixed in the profile</item>
					<item>' We highlight it by generating a color indicating it is invalid and that the user should add in the type</item>
					<list begin="{concat('abstract ', $targetClass, (if (a:Stereotype[contains(., '#enumeration')]) then ' &lt;&lt;enumeration&gt;&gt; #pink ' else ' #pink '), '&#123;')}" indent="   " delim="" end="{concat('&#125;', '&#xD;', '&#xA;')}">
						<item>' nothing to generate</item>
					</list>
				</list>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<!-- ============================================================================================================ -->
	<!-- START SECTION:  (Simple, Domain, and Enumerated attributes templates)                                        -->
	<!-- ============================================================================================================ -->
	<xsl:template match="a:Simple">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="@xstype"/> [<xsl:call-template name="cardinality"/>]</item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Domain">	
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="substring-after(@dataType, '#')"/> [<xsl:call-template name="cardinality"/>]</item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:Enumerated">
		<xsl:if test="not(@hideInDiagrams = 'true')">
			<xsl:variable name="stereotypes"><xsl:call-template name="attribute-stereotypes"/></xsl:variable>
			<item>+<xsl:choose><xsl:when test="not($stereotypes = '')"><xsl:value-of select="concat($stereotypes, ' ')"/></xsl:when><xsl:otherwise></xsl:otherwise></xsl:choose><xsl:value-of select="@name"/> : <xsl:value-of select="@type"/> [<xsl:call-template name="cardinality"/>]</item>
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
	
	<xsl:template name="attribute-stereotypes">
		<xsl:if test="a:Stereotype">
			<xsl:variable name="stereotypes">
				<xsl:for-each select="a:Stereotype">
					<xsl:variable name="currentStereotype" select="."/>
					<xsl:variable name="stereotype" select="substring-after(., '#')"/>
					<xsl:choose>
						<!-- Below is the set of stereotypes that are internal metadata. These we do not display on an attribute or association -->
						<xsl:when test="not(($stereotype = 'enumeration') or ($stereotype = 'attribute') or ($stereotype = 'byreference') or ($stereotype = 'enum') or ($stereotype = 'concrete') or ($stereotype = 'ofAggregate') or ($stereotype = 'aggregateOf') or ($stereotype = 'ofComposite') or ($stereotype = 'compositeOf'))">
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
	
	<xsl:template name="cardinality">	
		<xsl:value-of select="if (@minOccurs = @maxOccurs) then @minOccurs else concat(@minOccurs, '..', replace(replace(@maxOccurs, 'unbounded', '*'), 'n', '*'))"/>
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

</xsl:stylesheet>