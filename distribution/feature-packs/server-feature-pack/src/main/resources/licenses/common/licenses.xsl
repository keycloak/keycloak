<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" encoding="utf-8" standalone="no" media-type="text/html" />
    <xsl:param name="productname"/>
    <xsl:param name="version"/>
    <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
                <link rel="stylesheet" type="text/css" href="licenses.css"/>
            </head>
            <body>
                <h2><xsl:value-of select="$productname"/><xsl:text> </xsl:text><xsl:value-of select="$version"/></h2>
                <p>The following material has been provided for informational purposes only, and should not be relied upon or construed as a legal opinion or legal advice.</p>
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
                                <ul>
                                    <xsl:for-each select="licenses/license">
                                        <li><a href="{./url}"><xsl:value-of select="name"/></a></li>
                                    </xsl:for-each>
                                </ul>
                            </td>
                            <td>
                                <ul>
                                    <xsl:for-each select="licenses/license">
                                        <xsl:variable name="filename" select="concat(../../groupId, ',', ../../artifactId, ',', ../../version, ',', name, '.txt')" />
                                        <li><a href="{$filename}"><xsl:value-of select="name"/></a></li>
                                    </xsl:for-each>
                                </ul>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
                <table>
                    <tr>
                        <th>Description</th>
                        <th>Locations</th>
                        <th>Remote Licenses</th>
                        <th>Local Licenses</th>
                    </tr>
                    <xsl:for-each select="licenseSummary/others/other">
                        <xsl:sort select="description"/>
                        <tr>
                            <td><xsl:value-of select="description"/></td>
                            <td>
                                <ul>
                                    <xsl:for-each select="locations/*[self::file or self::directory]">
                                        <li class="{local-name()}"><xsl:value-of select="."/></li>
                                    </xsl:for-each>
                                    <xsl:for-each select="locations/archive">
                                        <li class="archive">
                                            <p><xsl:value-of select="file"/></p>
                                            <ul>
                                                <xsl:for-each select="innerpath">
                                                    <li><xsl:value-of select="."/></li>
                                                </xsl:for-each>
                                            </ul>
                                        </li>
                                    </xsl:for-each>
                                </ul>
                            </td>
                            <td>
                                <ul>
                                    <xsl:for-each select="licenses/license">
                                        <li><a href="{./url}"><xsl:value-of select="name"/></a></li>
                                    </xsl:for-each>
                                </ul>
                            </td>
                            <td>
                                <ul>
                                    <xsl:for-each select="licenses/license">
                                        <xsl:variable name="filename" select="concat(../../description, ',', name, '.txt')" />
                                        <li><a href="{$filename}"><xsl:value-of select="name"/></a></li>
                                    </xsl:for-each>
                                </ul>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
