<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output xmlns:xalan="http://xml.apache.org/xslt" method="xml" omit-xml-declaration="no" indent="yes" xalan:indent-amount="4" />

    <xsl:param name="newline"><xsl:text>
</xsl:text></xsl:param>

	<xsl:template match="/">
		<xsl:value-of select="$newline"/>
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" mode="body"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="@*|node()" mode="body">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" mode="body"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
