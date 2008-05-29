<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
           version="1.0"
           xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	   xmlns:svg="http://www.w3.org/2000/svg"
	   xmlns="http://www.w3.org/1999/xhtml"
	   xmlns:xlink="http://www.w3.org/1999/xlink"
	   xmlns:str="http://exslt.org/strings"
	   xmlns:dyn="http://exslt.org/dynamic"
           extension-element-prefixes="str dyn"
>
	<xsl:output method="text"/>
	
	<xsl:param name="svgfile">$svgfile</xsl:param>
	<xsl:param name="inkopts">$inkopts</xsl:param>
	
	<!-- pre-render groups whose id ends in .png-->
	<xsl:template match="svg:g[substring(@id,string-length(@id)-3)='.png']">
		<xsl:variable name="opts">
			<xsl:choose>
				<xsl:when test="@id='bg.png'">
					<xsl:text>--export-id-only </xsl:text>
					<xsl:value-of select="$inkopts"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$inkopts"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:text>inkscape --export-png=</xsl:text>
		<xsl:value-of select="@id"/>
		<xsl:text> --export-id=</xsl:text>
		<xsl:value-of select="@id"/>
		<xsl:text> --file=</xsl:text>
		<xsl:value-of select="$svgfile"/>
		<xsl:text> </xsl:text>
		<xsl:value-of select="$opts"/>
		<xsl:text>
</xsl:text>
	</xsl:template>
	
	<xsl:template match="@*|node()">
		<xsl:apply-templates select="@*|node()"/>
	</xsl:template>
</xsl:stylesheet>
