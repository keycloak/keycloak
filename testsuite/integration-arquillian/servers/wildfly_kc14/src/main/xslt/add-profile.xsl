<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:m="http://maven.apache.org/POM/4.0.0" 
                xmlns:xalan="http://xml.apache.org/xalan"
                version="2.0"
                exclude-result-prefixes="m xalan" >
    
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:variable name="profile">
        <profile>
            <id>migration</id>
            <properties>
                <keycloak-1.4.0.Final.home>${containers.home}/keycloak-1.4.0.Final</keycloak-1.4.0.Final.home>
            </properties>
            <build>
                <pluginManagement>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-dependency-plugin</artifactId>
                            <version>2.10</version>
                            <executions>
                                <execution>
                                    <id>unpack-previous</id>
                                    <phase>generate-test-resources</phase>
                                    <goals>
                                        <goal>unpack</goal>
                                    </goals>
                                    <configuration>
                                        <artifactItems>
                                            <artifactItem>
                                                <groupId>org.keycloak.testsuite</groupId>
                                                <artifactId>integration-arquillian-server-wildfly-kc14</artifactId>
                                                <version>${project.version}</version>
                                                <type>zip</type>
                                            </artifactItem>
                                        </artifactItems>
                                        <outputDirectory>${containers.home}</outputDirectory>
                                        <overWriteIfNewer>true</overWriteIfNewer>
                                    </configuration>
                                </execution>
                            </executions>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-surefire-plugin</artifactId>
                            <configuration>
                                <systemPropertyVariables>
                                    <migration>true</migration>
                                    <keycloak-1.4.0.Final.home>${keycloak-1.4.0.Final.home}</keycloak-1.4.0.Final.home>
                                </systemPropertyVariables>
                            </configuration>
                        </plugin>
                    </plugins>
                </pluginManagement>
            </build>
        </profile>
    </xsl:variable>

    <xsl:template match="//m:profiles">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
            <xsl:copy-of select="$profile"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>