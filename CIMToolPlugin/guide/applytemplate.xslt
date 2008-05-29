<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
           version="1.0"
           xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	   xmlns:svg="http://www.w3.org/2000/svg"
	   xmlns:xlink="http://www.w3.org/1999/xlink"
   	   xmlns:sodipodi="http://inkscape.sourceforge.net/DTD/sodipodi-0.dtd"
	   xmlns:str="http://exslt.org/strings"
	   xmlns:dyn="http://exslt.org/dynamic"
           extension-element-prefixes="str dyn"
>
        <xsl:output method="xml" indent="yes"/>

	<xsl:param name="templatedoc" >template.svg</xsl:param>

	<xsl:variable name="template" select="document($templatedoc)"/>

	<xsl:template match="/svg:svg">
		<xsl:copy>
			<xsl:copy-of select="$template/svg:svg/svg:defs"/>
			<xsl:copy-of select="$template/svg:svg/svg:g[starts-with(@id,'bg')]"/>
			<xsl:apply-templates select="@*|*"/>
			<xsl:copy-of select="$template/svg:svg/svg:g[not(starts-with(@id,'bg'))]"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="sodipodi:namedview">
		<xsl:copy>
			<xsl:apply-templates select="@*|*"/>
			<xsl:copy-of select="$template/svg:svg/sodipodi:namedview/sodipodi:guide"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="sodipodi:guide"/>

	<xsl:template match="@width">
		<xsl:copy-of select="$template/svg:svg/@width"/>
	</xsl:template>

	<xsl:template match="@height">
		<xsl:copy-of select="$template/svg:svg/@height"/>
	</xsl:template>
	
	<xsl:template match="svg:g|svg:defs">
		<xsl:if test="not($template/svg:svg/*[@id=current()/@id])">
			<xsl:copy-of select="."/>
		</xsl:if>
	</xsl:template>

	<xsl:template match="@*|*">
		<xsl:copy-of select="."/>
	</xsl:template>
</xsl:stylesheet>