<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.keycloak</groupId>
    <version>{{version}}</version>
    <artifactId>my-account-ui</artifactId>
    <name>My Account UI</name>
    <description>The user inferface to manage an account on the Keycloak server.</description>

    <properties>
        <node.version>v20.13.0</node.version>
    </properties>
    <build>
        <resources>
            <resource>
                <directory>maven-resources</directory>
            </resource>
            <resource>
                <directory>dist</directory>
                <targetPath>theme/my-account/account/resources</targetPath>
            </resource>
        </resources>

        <plugins>
            <plugin>
                <groupId>com.github.eirslett</groupId>
                <artifactId>frontend-maven-plugin</artifactId>
                <version>1.15.0</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>install-node-and-npm</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>npm-install</id>
                        <goals>
                            <goal>npm</goal>
                        </goals>
                        <configuration>
                            <arguments>install</arguments>
                            <workingDirectory>${maven.multiModuleProjectDirectory}</workingDirectory>
                        </configuration>
                    </execution>
                    <execution>
                        <id>npm-build</id>
                        <goals>
                            <goal>npm</goal>
                        </goals>
                        <configuration>
                            <arguments>run build</arguments>
                        </configuration>
                    </execution>
                </executions>
                <configuration>
                    <nodeVersion>${node.version}</nodeVersion>
                    <installDirectory>${maven.multiModuleProjectDirectory}</installDirectory>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
