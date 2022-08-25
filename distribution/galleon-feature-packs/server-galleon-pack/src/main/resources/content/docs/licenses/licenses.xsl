<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" encoding="utf-8" standalone="no" media-type="text/html" />
    <xsl:param name="version"/>
    <xsl:param name="product.release.name"/>
    <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz '" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ!'" />
    
    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
                <link rel="stylesheet" type="text/css" href="licenses.css"/>
            </head>
            <body>
                <h2><xsl:value-of select="$product.release.name"/><xsl:text> </xsl:text><xsl:value-of select="substring-before($version, '-')"/> - Servlet Feature Pack</h2>
                <p>The following material has been provided for informational purposes only, and should not be relied upon or construed as a legal opinion or legal advice.</p>
                <!-- Read matching templates -->
                <table>
                    <tr>
                        <th>Package Group</th>
                        <th>Package Artifact</th>
                        <th>Package Version</th>
                        <th>Remote Licenses</th>
                        <th>Local Licenses</th>
                    </tr>
                    <xsl:for-each select="licenseSummary/dependencies/dependency">
                        <xsl:sort select="concat(groupId, '.', artifactId)"/>
                        <tr>
                            <td><xsl:value-of select="groupId"/></td>
                            <td><xsl:value-of select="artifactId"/></td>
                            <td><xsl:value-of select="version"/></td>
                            <td>
                                <xsl:for-each select="licenses/license">
                                    <a href="{./url}"><xsl:value-of select="name"/></a><br/>
                                </xsl:for-each>
                            </td>
                            <td>
                                <xsl:for-each select="licenses/license">
                                    <xsl:variable name="filename">
                                        <xsl:call-template name="remap-local-filename">
                                            <xsl:with-param name="name" select="name" />
                                        </xsl:call-template>
                                    </xsl:variable>
                                    <a href="{$filename}"><xsl:value-of select="$filename"/></a><br/>
                                </xsl:for-each>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>

    <xsl:template name="remap-local-filename">
        <xsl:param name="name"/>
        <xsl:choose>
            <xsl:when test="$name = 'GNU General Public License v2.0 only'">
                <xsl:text>gnu general public license v2.0 only.html</xsl:text>
            </xsl:when>
            <xsl:when test="$name = ''">
                <xsl:text>.html</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat(translate($name, $uppercase, $lowercase), '.txt')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
