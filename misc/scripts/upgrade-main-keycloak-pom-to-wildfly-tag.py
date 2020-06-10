#!/usr/bin/env python

# Purpose: Upgrade versions of artifacts shared with Wildfly and Wildfly Core
#          in main Keycloak pom.xml file to versions used by particular Wildfly
#          tag, specified as script argument

# Usage:   Run as, e.g.:
#          ./upgrade-main-keycloak-pom-to-wildfly-tag.py 20.0.0.Final
#
#          Or call the script without arguments to get the usage help

import sys

from lxml import etree as et
from os import remove
from packaging import version
from re import search
from shutil import copyfileobj
from subprocess import check_output
from tempfile import NamedTemporaryFile
from urllib.request import urlopen, HTTPError

# 'pom' namespace prefix definition for lxml
POM_NS = "http://maven.apache.org/POM/4.0.0"

# Get absolute path to main Keycloak pom.xml within the repo
KEYCLOAK_POM_FILE = check_output(['git', 'rev-parse', '--show-toplevel']).decode('utf-8').rstrip() + "/pom.xml"

# List of blacklisted artifacts to prevent their update even if they would be usually
# applicable for update as a result of the script run. Add new entries here by moving
# them from KEYCLOAK_TO_WILDFLY_ARTIFACT_NAMES as necessary
BLACKLISTED_ARTIFACTS = [
    # Intentionally avoid Apache DS downgrade "2.0.0.AM26" to Wildfly's "2.0.0-M24" from due to recent KEYCLOAK-14162
    "apacheds.version",
    # Intentionally omit upgrade to Infinispan "10.1.8.Final" (IOW perform upgrade just to Wildfly "20.0.0.Final")
    "infinispan.version"
]

# List of Keycloak specific properties, either not representing an artifact version
# or representing an artifact not shared with Wildfly's or Wildfly-Core's POMs
# (the artifact is either not referenced in those at all or explicitly excluded ins some of them)
KEYCLOAK_SPECIFIC = [
    "product.rhsso.version",
    "product.build-time",
    "eap.version",
    "jboss.as.version",
    "jboss.as.subsystem.test.version",
    "jboss.aesh.version",
    "jackson.databind.version",
    "jackson.annotations.version",
    "resteasy.undertow.version",
    "owasp.html.sanitizer.version",
    "sun.xml.ws.version",
    "jetty92.version",
    "jetty93.version",
    "jetty94.version",
    "ua-parser.version",
    "version.com.openshift.openshift-restclient-java",
    "apacheds.codec.version",
    "google.zxing.version",
    "freemarker.version",
    "jetty9.version",
    "liquibase.version",
    "mysql.version",
    "osgi.version",
    "pax.web.version",
    "postgresql.version",
    "mariadb.version",
    "mssql.version",
    "twitter4j.version",
    "jna.version",
    "greenmail.version",
    "jmeter.version",
    "selenium.version",
    "xml-apis.version",
    "subethasmtp.version",
    "replacer.plugin.version",
    "jboss.as.plugin.version",
    "jmeter.plugin.version",
    "jmeter.analysis.plugin.version",
    "minify.plugin.version",
    "osgi.bundle.plugin.version",
    "nexus.staging.plugin.version",
    "frontend.plugin.version",
    "docker.maven.plugin.version",
    "surefire.memory.Xms",
    "surefire.memory.Xmx",
    "surefire.memory.metaspace",
    "surefire.memory.metaspace.max",
    "surefire.memory.settings",
    "tomcat7.version",
    "tomcat8.version",
    "tomcat9.version",
    "spring-boot15.version",
    "spring-boot21.version",
    "spring-boot22.version",
    "webauthn4j.version",
    "org.apache.kerby.kerby-asn1.version"
]

# Mapping of artifact name as used in Keycloak's pom.xml to the name of same artifact listed
# in Wildfly's or Wildfly-Core's pom.xml
KEYCLOAK_TO_WILDFLY_ARTIFACT_NAMES = {
    "wildfly.version"                                             : "version",
    "wildfly.build-tools.version"                                 : "version.org.wildfly.build-tools",
    # Skip "eap.version" since Keycloak specific
    "wildfly.core.version"                                        : "version.org.wildfly.core",
    # Skip "jboss.as.version" since Keycloak specific
    # Skip "jboss.as.subsystem.test.version" since Keycloak specific
    # Skip "jboss.aesh.version" since Keycloak specific
    "aesh.version"                                                : "version.org.aesh",
    "apache.httpcomponents.version"                               : "version.org.apache.httpcomponents.httpclient",
    "apache.httpcomponents.httpcore.version"                      : "version.org.apache.httpcomponents.httpcore",
    "apache.mime4j.version"                                       : "version.org.apache.james.apache-mime4j",
    "jboss.dmr.version"                                           : "version.org.jboss.jboss-dmr",
    "bouncycastle.version"                                        : "version.org.bouncycastle",
    "cxf.version"                                                 : "version.org.apache.cxf",
    "cxf.jetty.version"                                           : "version.org.apache.cxf",
    "cxf.jaxrs.version"                                           : "version.org.apache.cxf",
    "cxf.undertow.version"                                        : "version.org.apache.cxf",
    "dom4j.version"                                               : "version.dom4j",
    "h2.version"                                                  : "version.com.h2database",
    "jakarta.persistence.version"                                 : "version.jakarta.persistence",
    "hibernate.core.version"                                      : "version.org.hibernate",
    "hibernate.c3p0.version"                                      : "version.org.hibernate",
    "infinispan.version"                                          : "version.org.infinispan",
    "jackson.version"                                             : "version.com.fasterxml.jackson",
    # Skip "jackson.databind.version" and "jackson.annotations.version" since they are derived from ${jackson.version}" above
    "jakarta.mail.version"                                        : "version.jakarta.mail",
    "jboss.logging.version"                                       : "version.org.jboss.logging.jboss-logging",
    "jboss.logging.tools.version"                                 : "version.org.jboss.logging.jboss-logging-tools",
    "jboss-jaxrs-api_2.1_spec"                                    : "version.org.jboss.spec.javax.ws.jboss-jaxrs-api_2.1_spec",
    "jboss-transaction-api_1.3_spec"                              : "version.org.jboss.spec.javax.transaction.jboss-transaction-api_1.3_spec",
    "jboss.spec.javax.xml.bind.jboss-jaxb-api_2.3_spec.version"   : "version.org.jboss.spec.javax.xml.bind.jboss-jaxb-api_2.3_spec",
    "jboss.spec.javax.servlet.jsp.jboss-jsp-api_2.3_spec.version" : "version.org.jboss.spec.javax.servlet.jsp.jboss-jsp-api_2.3_spec",
    "log4j.version"                                               : "version.log4j",
    "resteasy.version"                                            : "version.org.jboss.resteasy",
    # Skip "resteasy.undertow.version" since it's derived from ${resteasy.version} above
    # Skip "owasp.html.sanitizer.version" since Keycloak specific
    "slf4j-api.version"                                           : "version.org.slf4j",
    "slf4j.version"                                               : "version.org.slf4j",
    "sun.istack.version"                                          : "version.com.sun.istack",
    "sun.xml.bind.version"                                        : "version.sun.jaxb",
    "javax.xml.bind.jaxb.version"                                 : "version.javax.xml.bind.jaxb-api",
    # Skip "sun.xml.ws.version" since Keycloak specific
    "sun.activation.version"                                      : "version.com.sun.activation.jakarta.activation",
    "sun.xml.bind.version"                                        : "version.sun.jaxb",
    "org.glassfish.jaxb.xsom.version"                             : "version.sun.jaxb",
    "undertow.version"                                            : "version.io.undertow",
    "elytron.version"                                             : "version.org.wildfly.security.elytron",
    "elytron.undertow-server.version"                             : "version.org.wildfly.security.elytron-web",
    # Skip "jetty92.version", "jetty93.version", and "jetty94.version" since Keycloak specific
    "woodstox.version"                                            : "version.org.codehaus.woodstox.woodstox-core",
    "xmlsec.version"                                              : "version.org.apache.santuario",
    "glassfish.json.version"                                      : "version.org.glassfish.jakarta.json",
    "wildfly.common.version"                                      : "version.org.wildfly.common",
    # Skip "ua-parser.version" since Keycloak specific
    "picketbox.version"                                           : "version.org.picketbox",
    "google.guava.version"                                        : "version.com.google.guava",
    # Skip "version.com.openshift.openshift-restclient-java" since Keycloak specific
    "commons-lang.version"                                        : "version.commons-lang",
    "commons-lang3.version"                                       : "version.commons-lang3",
    "commons-io.version"                                          : "version.commons-io",
    "apacheds.version"                                            : "version.org.apache.ds",
    # Skip "apacheds.codec.version" since Keycloak specific
    # Skip "google.zxing.version" since Keycloak specific
    # Skip "freemarker.version" since Keycloak specific
    # Skip "jetty9.version" since Keycloak specific
    # Skip "liquibase.version" since Keycloak specific
    # Skip "mysql.version" since Keycloak specific
    # Skip "osgi.version" since Keycloak specific
    # Skip "pax.web.version" since Keycloak specific
    # Skip "postgresql.version" since Keycloak specific
    # Skip "mariadb.version" since Keycloak specific
    # Skip "mssql.version" since Keycloak specific
    "servlet.api.30.version"                                      : "version.org.jboss.spec.javax.xml.soap.jboss-saaj-api_1.4_spec",
    "servlet.api.40.version"                                      : "version.org.jboss.spec.javax.servlet.jboss-servlet-api_4.0_spec",
    # Skip "twitter4j.version" since Keycloak specific
    # Skip "jna.version" since Keycloak specific
    # Skip "greenmail.version" since Keycloak specific
    "hamcrest.version"                                            : "version.org.hamcrest",
    # Skip "jmeter.version" since Keycloak specific
    "junit.version"                                               : "version.junit",
    "picketlink.version"                                          : "version.org.picketlink",
    # Skip "selenium.version" since Keycloak specific
    # Skip "xml-apis.version" since intentionally excluded in Wildfly
    # Skip "subethasmtp.version" since Keycloak specific
    # Skip "replacer.plugin.version" since Keycloak specific
    # Skip "jboss.as.plugin.version" since Keycloak specific
    # Skip "jmeter.plugin.version" since Keycloak specific
    # Skip "jmeter.analysis.plugin.version" since Keycloak specific
    # Skip "minify.plugin.version" since Keycloak specific
    # Skip "osgi.bundle.plugin.version" since Keycloak specific
    "wildfly.plugin.version"                                      : "version.org.wildfly.maven.plugins",
    # Skip "nexus.staging.plugin.version" since Keycloak specific
    # Skip "frontend.plugin.version" since Keycloak specific
    # Skip "docker.maven.plugin.version" since Keycloak specific
    # Skip "tomcat7.version", "tomcat8.version", and "tomcat9.version" since Keycloak specific
    # Skip "spring-boot15.version", "spring-boot21.version", and "spring-boot22.version" since Keycloak specific
    # Skip "webauthn4j.version" since Keycloak specific
    # Skip "org.apache.kerby.kerby-asn1.version" since Keycloak specific
}

def usage():
    print("Run as: \n\t%s Wildfly.Tag.To.Upgrade.To \ne.g.:\n\t%s 20.0.0.Final\n" % (sys.argv[0], sys.argv[0]))


def getXmlRoot(filename):
    return et.parse(filename).getroot()


def getPomProperty(root, elem):
    return root.xpath('/pom:project/pom:properties/pom:%s' % elem, namespaces = { "pom" : "%s" % POM_NS })


def getPomElemByXPath(root, xpath):
    return root.xpath(xpath, namespaces = { "pom" : "%s" % POM_NS })


def transferComponentVersionAcrossPoms(wildflyPomFile, wildflyCorePomFile, keycloakPomFile):
    keycloakXmlTreeRoot = getXmlRoot(keycloakPomFile)
    wildflyXmlTreeRoot = getXmlRoot(wildflyPomFile)
    wildflyCoreXmlTreeRoot = getXmlRoot(wildflyCorePomFile)

    for keycloakElemName, wildflyElemName in KEYCLOAK_TO_WILDFLY_ARTIFACT_NAMES.items():

        if keycloakElemName == "wildfly.version":
            wildflyElem = getPomElemByXPath(wildflyXmlTreeRoot, '/pom:project/pom:version')
        # Enumerate artifacts to retrieve from Wildfly-Core's pom.xml rather than from Wildfly's pom.xml
        elif keycloakElemName in \
        [
            "wildfly.build-tools.version",
            "aesh.version",
            "apache.httpcomponents.version",
            "apache.httpcomponents.httpcore.version",
            "jboss.dmr.version",
            "jboss.logging.version",
            "jboss.logging.tools.version",
            "log4j.version",
            "slf4j-api.version",
            "slf4j.version",
            "javax.xml.bind.jaxb.version",
            "undertow.version",
            "elytron.version",
            "elytron.undertow-server.version",
            "woodstox.version",
            "glassfish.json.version",
            "picketbox.version",
            "commons-lang.version",
            "commons-io.version",
            "junit.version"
        ]:
            wildflyElem = getPomProperty(wildflyCoreXmlTreeRoot, wildflyElemName)
        else:
            wildflyElem = getPomProperty(wildflyXmlTreeRoot, wildflyElemName)

        if wildflyElem:
            keycloakElem = getPomProperty(keycloakXmlTreeRoot, keycloakElemName)
            if keycloakElem:
                if keycloakElemName in BLACKLISTED_ARTIFACTS:
                    print("Not updating version of %s from %s to %s because the artifact is blacklisted!" %
                         (keycloakElemName, keycloakElem[0].text, wildflyElem[0].text))
                elif version.parse(wildflyElem[0].text) > version.parse(keycloakElem[0].text):
                    keycloakElem[0].text = wildflyElem[0].text

                else:
                    print("Not updating version of %s to %s because existing Keycloak version is either equal or already higher: %s" %
                         (keycloakElemName, wildflyElem[0].text, keycloakElem[0].text))
        else:
            print("Unable to locate element with name: %s in %s or %s" % (wildflyElemName, wildflyPomFile, wildflyCorePomFile))

    et.ElementTree(keycloakXmlTreeRoot).write(keycloakPomFile, encoding = "UTF-8", pretty_print = True, xml_declaration = True)
    print("\nWrote updated main Keycloak pom.xml file to: %s" % keycloakPomFile)
    print("Inspect the changes, run the tests and submit the PR with the pom.xml changes if all is OK.\n")


# Well formed Wildfly & Wildfly-Core tags seems to follow the patterns:
# * A digit followed by a dot (both of them exactly three times), followed by a "Final" suffix
#   (e.g.: "20.0.0.Final"), or
# * A digit followed by a dot (both of them exactly three times), followed by:
#   - Either "Alpha" suffix, followed by one digit,
#   - Or "Beta" suffix, followed by one digit,
#   - Or "CR" suffix, followed by one digit
#
# Check the provided Wildfly tag, provided as script argument follows some of these two options
#
def isWellFormedWildflyTag(tag):
    if tag and not search(r'(\d\.){3}((Alpha|Beta|CR)\d|Final)', tag):
        print("Invalid Wildfly tag: \"%s\", exiting!" % tag)
        sys.exit(1)
    else:
        return tag


def getPomXmlForTagFromUrl(baseurl, tag):
    try:
        with urlopen(baseurl) as response:
            with NamedTemporaryFile(delete=False) as outfile:
                copyfileobj(response, outfile)
                return outfile.name
    except HTTPError:
        print("Failed to download pom.xml for tag: %s. Double-check the tag and retry!" % tag)
        sys.exit(1)
    return None


if __name__ == '__main__':

    if len(sys.argv) != 2:
        usage()
        sys.exit(1)

    WILDFLY_TAG = isWellFormedWildflyTag(sys.argv[1])
    WILDFLY_POM_BASE_URL = "https://github.com/wildfly/wildfly/raw/%s/pom.xml" % WILDFLY_TAG

    print("Retrieving Wildfly's pom.xml for tag: %s" % WILDFLY_TAG)
    WILDFLY_POM_FILE = getPomXmlForTagFromUrl(WILDFLY_POM_BASE_URL, WILDFLY_TAG)

    WILDFLY_POM_XML_ROOT = getXmlRoot(WILDFLY_POM_FILE)
    WILDFLY_CORE_TAG = isWellFormedWildflyTag(getPomProperty(WILDFLY_POM_XML_ROOT, "version.org.wildfly.core")[0].text)
    WILDFLY_CORE_POM_BASE_URL = "https://github.com/wildfly/wildfly-core/raw/%s/pom.xml" % WILDFLY_CORE_TAG

    print("Retrieving Wildfly-Core pom.xml for tag: %s" % WILDFLY_CORE_TAG)
    WILDFLY_CORE_POM_FILE = getPomXmlForTagFromUrl(WILDFLY_CORE_POM_BASE_URL, WILDFLY_CORE_TAG)

    # Verify all artifacts listed as properties in Keycloak's main pom.xml file are either
    # blacklisted, or scheduled to be processed, or belong to Keycloak specific set
    KEYCLOAK_POM_XML_ROOT = getXmlRoot(KEYCLOAK_POM_FILE)
    for xmlTag in getPomElemByXPath(KEYCLOAK_POM_XML_ROOT, "//pom:project/pom:properties/pom:*"):
        artifactName = xmlTag.tag.replace("{%s}" % POM_NS, "")
        if artifactName not in BLACKLISTED_ARTIFACTS and artifactName not in KEYCLOAK_SPECIFIC and artifactName not in KEYCLOAK_TO_WILDFLY_ARTIFACT_NAMES.keys():
            print("\nFound so far unknown \"%s\" artifact in Keycloak's pom.xml file!" % artifactName)
            print("It's not a blacklisted one, not listed as Keycloak specific one, and not present in the set of those to be processed.")
            print("Can't continue. Add this artifact to some of those three groups and retry!")
            sys.exit(1)

    if WILDFLY_POM_FILE != None and WILDFLY_CORE_POM_FILE != None:
        transferComponentVersionAcrossPoms(WILDFLY_POM_FILE, WILDFLY_CORE_POM_FILE, KEYCLOAK_POM_FILE)
        for filename in [WILDFLY_POM_FILE, WILDFLY_CORE_POM_FILE]:
            remove(filename)
