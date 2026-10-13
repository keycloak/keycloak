# Dependency License Information

## Why should we track license info?

We need to keep track of the licenses that apply to each third party (non-Keycloak) dependency (maven or otherwise) that Keycloak uses. This information can be used to check if there are conflicts or other issues that could create a legal encumberance for the users or developers of Keycloak.

## How to determine a dependency's license info

Since the maven ecosystem does not maintain high quality license metadata, no automated process (like license-maven-plugin for example) is acceptable for making the determination of what license applies to a particular maven dependency. The licenses of non-maven dependencies (JavaScript, images, fonts, etc.) must also be determined manually.

To manually determine a license, clone/checkout the source code at the tag or commit that applies to the version of the dependency you're adding or updating (licenses do sometimes change between versions). This is a lot easier to do immediately, especially for non-maven dependencies, as later it may not be clear where the files came from, or what version they were. Once you have the source, look at the readme and search around for license files. For maven projects, you can look at the pom too, but its license information is not always present or correct. It's usually obvious what the license is, but sometimes one part of the source has a different license, or there may be more than one license that applies to your dependency.

## How to store license info

Typically, each zip that gets distributed to users needs to contain a license XML and individual license files, plus an html file generated at build time.

The XML and individual files are maintained in git. When you change or add a dependency that is a part of:

- the server, modify `distribution/feature-packs/server-feature-pack/src/main/resources/licenses/rh-sso/license.xml`.
- an adapter, modify `distribution/{saml-adapters,adapters}/*/*/src/main/resources/licenses/rh-sso/licenses.xml`, for example `distribution/saml-adapters/as7-eap6-adapter/eap6-adapter-zip/src/main/resources/licenses/rh-sso/licenses.xml`.

Maven dependencies go into a `licenseSummary/dependencies/dependency` element, and non-maven dependencies go into a `licenseSummary/others/other` element.

Here are some examples of maven dependencies:

```xml
    <dependency>
      <groupId>org.sonatype.plexus</groupId>
      <artifactId>plexus-sec-dispatcher</artifactId>
      <version>1.3</version>
      <licenses>
        <license>
          <name>Apache Software License 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url> <!-- Source repo contains no license file -->
        </license>
      </licenses>
    </dependency>
    <dependency>
      <groupId>org.antlr</groupId>
      <artifactId>antlr-runtime</artifactId>
      <version>3.5</version>
      <licenses>
        <license>
          <name>BSD 3-clause New or Revised License</name>
          <url>https://raw.githubusercontent.com/antlr/antlr3/antlr-3.5/runtime/Python/LICENSE</url>
        </license>
      </licenses>
    </dependency>
```

and non-maven dependencies:

```xml
    <other>
      <description>jQuery</description>
      <locations>
        <file>themes/keycloak/common/resources/lib/jquery/jquery-1.10.2.js</file>
      </locations>
      <licenses>
        <license>
          <name>MIT License</name>
          <url>https://raw.githubusercontent.com/jquery/jquery/1.10.2/MIT-LICENSE.txt</url>
        </license>
      </licenses>
    </other>
    <other>
      <description>AngularJS</description>
      <locations>
        <directory>themes/keycloak/common/resources/lib/angular</directory>
      </locations>
      <licenses>
        <license>
          <name>MIT License</name>
          <url>https://raw.githubusercontent.com/angular/angular.js/v1.4.4/LICENSE</url>
        </license>
      </licenses>
    </other>
```

Look at the licenses.xml files in this repository for more examples.

After modifying a license XML, you must run `download-license-files.sh` against it. This script will update the individual license files that are stored in the same directory as the XML.

Example command line:

```
$ distribution/licenses-common/download-license-files.sh distribution/feature-packs/server-feature-pack/src/main/resources/licenses/rh-sso/licenses.xml
```

The following shell commands must be available for the script to work:

- curl
- dos2unix
- sha256sum
- xmlstarlet

### Product builds

RH-SSO is built on an internal build system. If you've added or updated license data for the product, you might need to import the corresponding dependency artifact into this internal build system.
