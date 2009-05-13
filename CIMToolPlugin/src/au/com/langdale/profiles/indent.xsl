<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:a="http://langdale.com.au/2005/Message#"
	xmlns:in="http://langdale.com.au/2009/Indent">

	<xsl:output indent="no" method="text" encoding="utf-8" />
	<xsl:strip-space elements="*"/>
	
    <xsl:param name="newline"><xsl:text>
</xsl:text></xsl:param>
	
	<xsl:template match="in:document">
	    <xsl:apply-templates>
            <xsl:with-param name="suffix" select="$newline"/>
	    </xsl:apply-templates>
	</xsl:template>
	
	<xsl:template match="in:list">
        <xsl:param name = "prefix" select = "@prefix"/>
        <xsl:param name = "suffix" select = "@suffix" />
        <xsl:param name = "begin"/>
        <xsl:param name = "end" />
        
        <xsl:variable name = "termin" select="@termin"/>
        <xsl:variable name = "delim" select="@delim"/>
        <xsl:variable name = "contin" select="@contin"/>
        <xsl:variable name = "indent" select = "@indent" />
        <xsl:variable name = "margin" select = "@margin" />
        
        <xsl:variable name = "first" select="concat($begin,@begin)"/>
        <xsl:if test="$first != ''">
            <xsl:value-of select="concat($prefix,$first,$suffix)"/>
        </xsl:if>
        
        <xsl:for-each select="node()">

            <xsl:apply-templates select=".">
                <xsl:with-param name="prefix" select="concat($prefix,$indent)"/>
                <xsl:with-param name="suffix" select="concat($suffix,$margin)"/>
                <xsl:with-param name="begin">
                    <xsl:if test="position() > 1">
                        <xsl:value-of select="$contin"></xsl:value-of>
                    </xsl:if> 
                </xsl:with-param>
                <xsl:with-param name="end">
          	        <xsl:if test="last() > position()">
      	                <xsl:value-of select="$delim"></xsl:value-of>
       	            </xsl:if> 
       	            <xsl:value-of select="$termin"></xsl:value-of>
                </xsl:with-param>
            </xsl:apply-templates>

        </xsl:for-each>
        
        <xsl:variable name = "last" select="concat(@end,$end)"/>
        <xsl:if test="$last != ''">
            <xsl:value-of select="concat($prefix,$last,$suffix)"/>
        </xsl:if>
	</xsl:template>

    <xsl:template match="in:decorate">
        <xsl:param name = "prefix" />
        <xsl:param name = "suffix" />
        <xsl:param name = "begin" />
        <xsl:param name = "end" />
        
        <xsl:for-each select="node()">
        	<xsl:choose>
                <xsl:when test = "position() = last()">
                    <xsl:apply-templates select=".">
                        <xsl:with-param name = "prefix" select = "$prefix" />
                        <xsl:with-param name = "suffix" select = "$suffix" />
                        <xsl:with-param name = "begin" select = "$begin"/>
                        <xsl:with-param name = "end" select = "$end"/>
                    </xsl:apply-templates>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select=".">
                        <xsl:with-param name = "prefix" select = "$prefix" />
                        <xsl:with-param name = "suffix" select = "$suffix" />
                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
        
    </xsl:template>

    
    <xsl:template match="in:item">
        <xsl:param name = "prefix" />
        <xsl:param name = "suffix" />
        <xsl:param name = "begin"/>
        <xsl:param name = "end"/>

        <xsl:value-of select="concat($prefix,$begin)"/>
        <xsl:apply-templates select="node()"/>
        <xsl:value-of select="concat($end,$suffix)"/>
    </xsl:template>
    
    <xsl:template match="in:sp"> 
        <xsl:param name = "prefix" />
        <xsl:param name = "suffix" />
        <xsl:param name = "begin"/>
        <xsl:param name = "end"/>
        <xsl:value-of select="concat($prefix,$begin,' ',$end,$suffix)"/>
    </xsl:template>
    
    <xsl:template match="text()">
        <xsl:param name = "prefix" />
        <xsl:param name = "suffix" />
        <xsl:param name = "begin"/>
        <xsl:param name = "end"/>
        <xsl:value-of select="concat($prefix,$begin,normalize-space(),$end,$suffix)"/>
    </xsl:template>
    
    <xsl:template match="in:wrap" name="wrap">
        <xsl:param name = "prefix" />
        <xsl:param name = "suffix" />
        <xsl:param name = "begin"/>
        <xsl:param name = "end"/>
        <xsl:param name = "body" select="normalize-space()"/>
        <xsl:param name = "line"/>
        <xsl:param name="width" select="@width"/>

        <xsl:variable name="word" select="substring-before($body,' ')"/>
        <xsl:variable name="rest" select="substring-after($body,' ')"/> 

        <xsl:choose>
        	<xsl:when test="$rest = ''">
        		<xsl:value-of select="concat($prefix,$begin,$line,$body,$end,$suffix)"/>
        	</xsl:when>
        	<xsl:when test="string-length($word) + string-length($line) >= number($width)">
        		<xsl:value-of select="concat($prefix,$begin,$line,$word,$end,$suffix)"/>
        		<xsl:call-template name="wrap">
                    <xsl:with-param name = "prefix" select = "$prefix" />
                    <xsl:with-param name = "suffix" select = "$suffix" />
                    <xsl:with-param name = "begin" select = "$begin"/>
                    <xsl:with-param name = "end" select = "$end"/>
                    <xsl:with-param name="width" select="$width" />
                    <xsl:with-param name="body" select="$rest"/>
        		</xsl:call-template>
        	</xsl:when>
        	<xsl:otherwise>
        		<xsl:call-template name="wrap">
                    <xsl:with-param name = "prefix" select = "$prefix" />
                    <xsl:with-param name = "suffix" select = "$suffix" />
                    <xsl:with-param name = "begin" select = "$begin"/>
                    <xsl:with-param name = "end" select = "$end"/>
                    <xsl:with-param name="width" select="$width" />
                    <xsl:with-param name="line" select="concat($line,$word,' ')"/>
                    <xsl:with-param name="body" select="$rest"/>
        		</xsl:call-template>
        	</xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <xsl:template match="in:pre">
        <xsl:copy-of select="."/>
    </xsl:template>
    
</xsl:stylesheet>
