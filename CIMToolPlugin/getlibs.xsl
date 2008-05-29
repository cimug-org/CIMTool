<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
           version="1.0"
           xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
        <xsl:output method="text"/>
	<xsl:template match="archive">
	    <xsl:value-of select="@path"/>
	    <xsl:text>
</xsl:text>
	</xsl:template>
	<xsl:template match="text()"/>
</xsl:stylesheet>
