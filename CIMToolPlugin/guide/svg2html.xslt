<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
           version="1.0"
           xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	   xmlns:svg="http://www.w3.org/2000/svg"
	   xmlns="http://www.w3.org/1999/xhtml"
	   xmlns:xlink="http://www.w3.org/1999/xlink"
	   xmlns:str="http://exslt.org/strings"
	   xmlns:dyn="http://exslt.org/dynamic"
	   xmlns:math="http://exslt.org/math"
           extension-element-prefixes="str dyn math"
>
        <xsl:output method="xml" indent="yes"/>
	<xsl:template match="text()"/>
	<xsl:template match="svg:defs"/>
	<xsl:param name="next"/>
	<xsl:param name="back"/>
	<xsl:param name="finish"/>

	<!-- the overall document structure -->
	<xsl:template match="/">
		<html>
			<xsl:copy-of select="document('head.xml')"/>
			<body>
				<xsl:apply-templates/>
			</body>
		</html>
	</xsl:template>

	<!-- pre-render groups whose id ends in .png-->
	<xsl:template match="svg:g[substring(@id,string-length(@id)-3)='.png']">
		<img class="pre-render" src="{@id}">
			<xsl:attribute name="style">
				<xsl:text>position: absolute; </xsl:text>
				<xsl:text>left: </xsl:text>
				<xsl:value-of select="math:min(descendant-or-self::*/@x)"/>
				<xsl:text>; </xsl:text>
				<xsl:text>top: </xsl:text>
				<xsl:value-of select="math:min(descendant-or-self::*/@y)"/>
				<xsl:text>; </xsl:text>
			</xsl:attribute>
		</img>
	</xsl:template>

	<!-- svg elements and unrendered groups become divs -->
	<xsl:template match="svg:svg|svg:g">
		<div class="g" >
			<xsl:call-template name="restyle"/>
			<xsl:apply-templates/>
		</div>
	</xsl:template>

	<!-- images -->
	<xsl:template match="svg:image">
		<img src="{@xlink:href}">
			<xsl:call-template name="restyle"/>
		</img>
	</xsl:template>

	<!-- anchors -->
	<xsl:template match="svg:a">
		<xsl:variable name="href" select="normalize-space(@xlink:href)"/>
		<xsl:variable name="len" select="string-length($href)"/>

		<a name="{@xlink:title}">
			<xsl:call-template name="restyle"/>
			<xsl:attribute name="href">
				<xsl:choose> 
					<xsl:when test="substring($href, $len - 3)='.svg'">
						<xsl:value-of select="concat(substring($href, 1, $len - 4), '.html')"/>
					</xsl:when>
					<xsl:when test="starts-with($href,'{') and substring($href,$len)='}'">
						<xsl:value-of select="dyn:evaluate(substring($href,2,$len - 2))"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$href"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:apply-templates/>
		</a>
	</xsl:template>

	<!-- rectangles become (usually coloured) div's -->
	<xsl:template match="svg:rect">
		<div class="rect">
			<xsl:call-template name="restyle"/>
			<xsl:comment>coloured panel</xsl:comment>
		</div>
	</xsl:template>

	<!-- flows have regions and para's, and can have transform attributes -->
	<xsl:template match="svg:flowRoot">
		<div class="flowRoot">
			<xsl:call-template name="restyle"/>
			<xsl:variable name="content" select="svg:flowPara"/>
			<xsl:for-each select="svg:flowRegion/*[1]">
				<div class="flowRegion">
					<xsl:call-template name="restyle"/>
					<xsl:for-each select="$content">
						<p>
							<xsl:attribute name="style" >
								<xsl:apply-templates select="@*" mode="restyle"/>
							</xsl:attribute>
							<xsl:value-of select="."/>
						</p>
					</xsl:for-each>
				</div>
			</xsl:for-each>
		</div>
	</xsl:template>

	<!-- 
		
		following templates generate CSS from svg attributes

	-->

	<xsl:template name="restyle">
		<xsl:attribute name="style" >
			<xsl:text>position: absolute; </xsl:text>
			<xsl:apply-templates select="@*" mode="restyle"/>
		</xsl:attribute>
	</xsl:template>

	<xsl:template match="@x" mode="restyle">
		<xsl:text>left: </xsl:text><xsl:value-of select="."/><xsl:text>; </xsl:text>
	</xsl:template>

	<xsl:template match="@y" mode="restyle">
		<xsl:text>top: </xsl:text><xsl:value-of select="."/><xsl:text>; </xsl:text>
	</xsl:template>

	<xsl:template match="@height" mode="restyle">
		<xsl:text>height: </xsl:text><xsl:value-of select="."/><xsl:text>; </xsl:text>
	</xsl:template>

	<xsl:template match="@width" mode="restyle">
		<xsl:text>width: </xsl:text><xsl:value-of select="."/><xsl:text>; </xsl:text>
	</xsl:template>

	<xsl:template match="@transform" mode="restyle">
		<xsl:for-each select="str:tokenize(.,'(,)')">
			<xsl:variable name="tx" select="normalize-space(.)"/>
			<xsl:choose>
				<xsl:when test="$tx='translate'">
					<xsl:text>left: </xsl:text>
					<xsl:value-of select="following-sibling::*[1]"/>
					<xsl:text>; </xsl:text>

					<xsl:text>top: </xsl:text>
					<xsl:value-of select="following-sibling::*[2]"/>
					<xsl:text>; </xsl:text>
				</xsl:when>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="@style" mode="restyle">
		<xsl:for-each select="str:tokenize(.,':;')">
			<xsl:variable name="style" select="normalize-space(.)"/>
			<xsl:choose>
				<xsl:when test="$style='fill'">
					<xsl:text>background: </xsl:text>
					<xsl:value-of select="following-sibling::*[1]"/>
					<xsl:text>; </xsl:text>
				</xsl:when>

				<xsl:when test="starts-with($style, 'font-') or starts-with($style, 'text-')">
					<xsl:value-of select="$style"/>
					<xsl:text>: </xsl:text>
					<xsl:value-of select="following-sibling::*[1]"/>
					<xsl:text>; </xsl:text>
				</xsl:when>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="@*" mode="restyle"/>

</xsl:stylesheet>
