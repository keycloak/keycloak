#
# Copyright 2020 Red Hat, Inc. and/or its affiliates
# and other contributors as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

"""
Keycloak package for Python to assists with upgrading of Keycloak to
particular Wildfly tag / release.

Copyright 2020 Red Hat, Inc. and/or its affiliates
and other contributors as indicated by the @author tags.

To use, simply 'import wildfly.upgrade' and call the necessary routines.
"""

import colorlog, copy, itertools, logging, lxml.etree, os, os.path, re, sys

from importlib import import_module
from shutil import copyfileobj
from subprocess import check_call, check_output
from tempfile import NamedTemporaryFile
from urllib.request import HTTPError, urlopen

__all__ = [
    'getElementsByXPath',
    'getElementsByXPathFromRemoteXml',
    'getKeycloakGitRepositoryRoot',
    'getModuleLogger',
    'getStepLogger',
    'getTaskLogger',
    'getPomDependencyByArtifactId',
    'getPomProperty',
    'getPomPropertyFromRemoteXml',
    'getVersionOfPomDependency',
    'getXmlRoot',
    'isWellFormedWildflyTag',
    'loadGavDictionaryFromGavFile',
    'loadGavDictionaryFromXmlFile',
    'saveUrlToNamedTemporaryFile',
    'updateAdapterLicenseFile',
    'performMainKeycloakPomFileUpdateTask',
    'performAdapterGalleonPackPomFileUpdateTask',
    'performKeycloakAdapterLicenseFilesUpdateTask',
    'synchronizeInfinispanSubsystemXmlNamespaceWithWildfly'
]

__author__   = "Jan Lieskovsky <jlieskov@redhat.com>"
__loglevel__ = logging.INFO
__status__   = "Alpha"
__version__  = "0.0.2"

#
# Various constants / data structures for the module
#

# Empty string
_empty_string                             = ''
# Base URL of the Keycloak's main pom.xml file from GitHub master branch
_keycloak_github_master_main_pom_base_url = "https://raw.githubusercontent.com/keycloak/keycloak/master/pom.xml"
# Python implementation of the Maven artifact versions comparator
_maven_version_comparator_                = import_module(
    'lib.wildfly.upgrade.dependencies.3rd_party.fabric8-analytics-version-comparator.f8a_version_comparator'
)
# GitHub raw form of the base URL to the specific Wildfly tag repository root
# Note: To get the final version of the URL '%s' format specifier needs to be
#       expanded to a particular Wildfly tag / release!
_wildfly_github_raw_base_url_for_tag      = (
    'https://raw.githubusercontent.com/wildfly/wildfly/%s/'
)
# Base URL of the Wildfly's main pom.xml XML file from GitHub tag branch
_wildfly_github_tag_main_pom_base_url     = (
    _wildfly_github_raw_base_url_for_tag +
    'pom.xml'
)
# Base URL of the Wildfly's Infinispan subsystem-template XML file from GitHub
# tag branch
_wildfly_github_tag_ispn_subtempl_base_url = (
    _wildfly_github_raw_base_url_for_tag +
    'clustering/infinispan/extension/src/main/resources/subsystem-templates/infinispan.xml'
)

# POM namespace prefix definition for lxml
_pom_ns                                   = "http://maven.apache.org/POM/4.0.0"

# Module loggers
_moduleLoggers                            = {}

# Maven GAV (groupId:artifactId:version) related constants / data structures
_gav_delimiter                            = ':'
_gav_elements                             = ['groupId', 'artifactId', 'version']

#
# Various base helper routines
#

def getKeycloakGitRepositoryRoot():
    """
    Return the absolute path to the Keycloak git repository clone.
    """
    return check_output(['git', 'rev-parse', '--show-toplevel']).decode('utf-8').rstrip()

def isWellFormedWildflyTag(tag):
    """
    Well formed Wildfly & Wildfly Core tag seems to follow the patterns:
    1) First a digit followed by a dot both of them exactly three times.
    2) Followed:
        a) Either by a "Final" suffix, e.g.: "20.0.0.Final",
        b) Or by one of "Alpha", "Beta", "CR" suffices, followed by one digit

    Verifies the tag provided as routine argument follows this schema.

    Exits with error if not.
    """
    if tag and not re.search(r'(\d\.){3}((Alpha|Beta|CR)\d|Final)', tag):
        getModuleLogger().error("Invalid Wildfly tag '%s', exiting!" % tag)
        sys.exit(1)
    else:
        return tag

def saveUrlToNamedTemporaryFile(baseUrl):
    """
    Fetch URL specified as routine argument to named temporary file and
    return the name of that file.

    Otherwise, log an error and exit with failure if HTTP error occurred.
    """
    try:
        with urlopen(baseUrl) as response:
            with NamedTemporaryFile(delete=False) as outfile:
                copyfileobj(response, outfile)
                return outfile.name
    except HTTPError:
        getModuleLogger().error("Failed to download the file from '%s'!. Double-check the URL and retry!" % baseUrl)
        sys.exit(1)

    return None

def _emptyNewLine():
    """
    Print additional new line.
    """
    print()

def _logErrorAndExitIf(errorMessage, condition, logger = None):
    """
    Log particular error message using either the default or custom logger
    specified via the 'logger' parameter, and exit with error if the specified
    condition was met.
    """
    # Use the default logger if a custom one wasn't requested
    if logger is None:
        logger = getModuleLogger()
    if condition:
        _emptyNewLine()
        logger.error(errorMessage)
        _emptyNewLine()
        sys.exit(1)

#
# Logging facility for the module
#

def setupLogger(loggerName = 'upgrade-wildfly', loggerFormatter = '%(log_color)s[%(levelname)s] %(name)s: %(message)s'):
    """
    Initialize logger with custom 'loggerName' and custom 'loggerFormatter'.
    """
    stdOutLogHandler = logging.StreamHandler(sys.stdout)
    loggerFormatter = colorlog.ColoredFormatter(loggerFormatter)
    stdOutLogHandler.setFormatter(loggerFormatter)
    logger = logging.getLogger(loggerName)
    logger.addHandler(stdOutLogHandler)
    logger.setLevel(__loglevel__)

    return logger

def getLogger(loggerName = 'Upgrade Wildfly for Keycloak', loggerFormatter = '%(log_color)s[%(levelname)s] [%(name)s]: %(message)s'):
    """
    Return instance of a logger with custom 'loggerName' and custom
    'loggerFormatter' or setup such a logger if it doesn't exist yet.
    """
    global _moduleLoggers
    if not loggerName in _moduleLoggers:
        _moduleLoggers[loggerName] = setupLogger(loggerName, loggerFormatter)

    return _moduleLoggers[loggerName]

def getModuleLogger():
    """
    Return global logger for the module.
    """
    return getLogger()

def getTaskLogger(taskLoggerName):
    """
    Return custom logger handling (sub)tasks.
    """
    taskLogFormatter = '\n%(log_color)s[%(levelname)s] [%(name)s]\n\n\t%(message)s\n'
    return getLogger(loggerName = taskLoggerName, loggerFormatter = taskLogFormatter)

def getStepLogger():
    """
    Return custom logger handling steps within tasks.
    """
    stepLoggerName = 'step'
    stepLoggerFormatter = '\t%(log_color)s[%(levelname)s]: %(message)s'

    return getLogger(stepLoggerName, stepLoggerFormatter)

#
# Various XML search related helper routines
#

def getElementsByXPath(xmlTree, xPath, nameSpace = { "pom" : "%s" % _pom_ns }):
    """
    Given the XML tree return the list of elements matching the 'xPath' from
    the XML 'nameSpace'.

    'nameSpace' is optional argument. If not specified defaults to the POM XML
    namespace.

    Returns empty list if no such element matching the specified by 'xPath' is
    found.
    """
    return xmlTree.xpath(xPath, namespaces = nameSpace)

def getElementsByXPathFromRemoteXml(xmlUrl, xPath, nameSpace = { "pom" : "%s" % _pom_ns }, errorMessage = None, expectedElementsCount = None):
    """
    Given the URL of the remote XML file as 'xmlUrl' return the list of
    elements matching the 'xPath' from the XML 'nameSpace'.

    'nameSpace' is optional argument. If not specified defaults to the POM XML
    namespace.

    Returns empty list if no such element matching the specified by 'xPath' is
    found.
    """
    xmlFile = saveUrlToNamedTemporaryFile(xmlUrl)
    xmlRoot = getXmlRoot(xmlFile)
    xmlElements = getElementsByXPath(xmlRoot, xPath, nameSpace = nameSpace)
    if None not in (errorMessage, expectedElementsCount) and len(xmlElements) != expectedElementsCount:
        _logErrorAndExitIf(errorMessage, True)

    return xmlElements

def getPomDependencyByArtifactId(xmlTree, artifactIdText):
    """
    Given the XML tree return list of POM dependency elements matching
    'artifactIdText' in the text of the element.

    Returns empty list if no such element with 'artifactIdText' is found.
    """
    return xmlTree.xpath('/pom:project/pom:dependencyManagement/pom:dependencies/pom:dependency/pom:artifactId[text()="%s"]' % artifactIdText, namespaces = { "pom" : "%s" % _pom_ns })

def getPomProperty(xmlTree, propertyText):
    """
    Given the XML tree return list of POM property elements matching
    'propertyText' in the text of the element.

    Returns empty list if no such element with 'propertyText' is found.
    """
    return xmlTree.xpath('/pom:project/pom:properties/pom:%s' % propertyText, namespaces = { "pom" : "%s" % _pom_ns })

def getPomPropertyFromRemoteXml(xmlUrl, propertyText, errorMessage = None, expectedElementsCount = None):
    """
    Given the URL of the remote XML file as 'xmlUrl' and name of the property
    to be retrieved from the XML file as 'propertyText' perform:
    * Save the remote XML to local file,
    * Obtain the XML root from the content,
    * Locate the specified POM property element within the file.

    Moreover, if both 'errorMessage' and 'expectedElementsCount' are provided,
    display the 'errorMessage' error message and exit with failure if the count
    of found elements matching the 'propertyText' doesn't match the
    'expectedElemsCount' value.

    Returns list of matching property elements or exits with error if count
    other than specified in 'expectedElementsCount' is found.
    """

    xmlFile = saveUrlToNamedTemporaryFile(xmlUrl)
    xmlRoot = getXmlRoot(xmlFile)
    propertyElement = getPomProperty(xmlRoot, propertyText)
    if None not in (errorMessage, expectedElementsCount) and len(propertyElement) != expectedElementsCount:
        _logErrorAndExitIf(errorMessage, True)

    return propertyElement

def getVersionOfPomDependency(xmlElem, groupIdText, artifactIdText):
    """
    Given the list of XML POM dependency elements, return the value of
    '<version>' subelement if 'groupIdText' and 'artifactIdText' match the
    value of groupId and artifactId subelements in the dependency.

    Otherwise, return None.
    """
    version = None
    for entry in xmlElem:
        dependencyElem = entry.getparent()
        for subelem in list(dependencyElem):
            if subelem.tag == '{%s}groupId' % _pom_ns and subelem.text != groupIdText:
                break
            if subelem.tag == '{%s}artifactId' % _pom_ns and subelem.text != artifactIdText:
                break
            if subelem.tag == '{%s}version' % _pom_ns:
                version = subelem.text
                break

    return version

def getXmlRoot(filename):
    """
    Return root element of the XML tree by parsing the content of 'filename'.

    Exit with error in the case of a failure.
    """
    try:
        xmlRoot = lxml.etree.parse(filename).getroot()
        return xmlRoot
    except lxml.etree.XMLSyntaxError:
        getXmlRootFailureMessage = (
            "Failed to get the root element of the XML tree from '%s' file! "
            "Ensure the file is not opened in another process, and retry!" %
            filename
        )
        getModuleLogger().error(getXmlRootFailureMessage)
        sys.exit(1)

#
# Common helper routines utilized by various tasks
# performed within a Wildfly upgrade
#

def compareMavenVersions(updatedVersion, currentVersion):
    """
    Compare 'updatedVersion' with 'currentVersion' using the generic Maven
    version comparison method.

    Return a negative integer, zero, or a positive integer as the
    'updatedVersion' is less than, equal to, or greater than the
    'currentVersion'.
    """
    comparableVersion = (
        _maven_version_comparator_
        .comparable_version
        .ComparableVersion(updatedVersion)
    )

    return comparableVersion.compare_to(currentVersion)

def getProductNamesForKeycloakPomProfile(profile = 'community'):
    """
    Return values of <product.name> and <product.name.full> elements
    of the specified Keycloak main pom.xml 'profile'
    """
    (productName, productNameFull) = (None, None)

    _logErrorAndExitIf(
        "Invalid profile name '%s'! It can be only one of 'community' or 'product'!" % profile,
        profile not in ['community', 'product']
    )

    # Absolute path to main Keycloak pom.xml within the repo
    mainKeycloakPomPath = getKeycloakGitRepositoryRoot() + "/pom.xml"
    keycloakPomXmlTreeRoot = getXmlRoot(mainKeycloakPomPath)
    pomProfileIdElem = getElementsByXPath(keycloakPomXmlTreeRoot, '/pom:project/pom:profiles/pom:profile/pom:id[text()="%s"]' % profile)
    _logErrorAndExitIf(
        "Can't locate the '%s' profile in main Keycloak pom.xml file!" % profile,
        len(pomProfileIdElem) != 1
    )

    pomProfileElem = pomProfileIdElem[0].getparent()
    pomProductNameElem = getElementsByXPath(pomProfileElem, './pom:properties/pom:product.name')
    _logErrorAndExitIf(
        "Can't determine product name from '%s' profile of main Keycloak pom.xml file!" % profile,
        len(pomProductNameElem) != 1
    )
    productName = pomProductNameElem[0].text
    pomProductNameFullElem = getElementsByXPath(pomProfileElem, './pom:properties/pom:product.name')
    _logErrorAndExitIf(
        "Can't determine the full product name from '%s' profile of main Keycloak pom.xml file!" % profile,
        len(pomProductNameFullElem) != 1
    )
    productNameFull = pomProductNameFullElem[0].text

    return (productName, productNameFull)

def getNumericArtifactVersion(gavDictionary, gavDictionaryKey):
    """
    Extract the numeric version of the 'gavDictionaryKey' GA artifact
    from 'gavDictionary'.

    1) Return dictionary value of 'gavDictionaryKey' directly
       if it's type is not a dictionary again.

    2) If the 'gavDictionaryKey' value is a child dictionary
       containing exactly one key, namely the name of the POM
       <property> to which the numeric version corresponds
       to, return the numeric artifact version from the
       subdictionary value.
    """
    gavDictionaryValue = gavDictionary[gavDictionaryKey]
    if not isinstance(gavDictionaryValue, dict):
        # First check if obtained artifact version is really numeric
        _logErrorAndExitIf(
            "Extracted '%s' artifact version isn't numeric: '%s'!" % (gavDictionaryKey, gavDictionaryValue),
            not re.match(r'\d.*', gavDictionaryValue)
        )
        return gavDictionaryValue

    else:
        subKey = gavDictionaryValue.keys()
        # Python starting from 3.3.1 returns dict_keys instead of a list when
        # calling dictionary items(). Convert dict_keys back to list if needed
        if not isinstance(subKey, list):
            subKey = list(subKey)
        # Sanity check if there's just one candidate numeric version for
        # the artifact. This shouldn't ever happen, but better to check
        _logErrorAndExitIf(
            "Artifact '%s' can't have more than just one versions!" % gavDictionaryKey,
            len(subKey) != 1
        )
        # Fetch the numeric artifact version from the subdictionary value
        gavDictionaryValue = gavDictionary[gavDictionaryKey][subKey[0]]
        # Finally check if obtained artifact version is really numeric
        _logErrorAndExitIf(
            "Extracted '%s' artifact version isn't numeric: '%s'!" % (gavDictionaryKey, gavDictionaryValue),
            not re.match(r'\d.*', gavDictionaryValue)
        )
        return gavDictionaryValue

def loadGavDictionaryFromGavFile(gavFile):
    """
    Load the content of 'gavFile' into Maven GAV Python dictionary, where
    dictionary key is reppresented by 'groupId:artifactId' part of the GAV
    entry, and value is represented by the 'version' field of the GAV entry.
    """
    gavDictionary = {}
    with open(gavFile) as inputFile:
        for line in inputFile:
            try:
                groupId, artifactId, version = line.rstrip().split(_gav_delimiter, 3)
                gavDictionaryKey = groupId + _gav_delimiter + artifactId
                gavDictionaryValue = version
                # Exit with error if obtained artifact version doesn't start
                # with a number
                _logErrorAndExitIf(
                    "Extracted '%s' artifact version isn't numeric: '%s'!" % (gavDictionaryKey, gavDictionaryValue),
                    not re.match(r'\d.*', gavDictionaryValue)
                )
                gavDictionary[gavDictionaryKey] = gavDictionaryValue
            except ValueError:
                # Ignore malformed GAV entries containing more than three
                # fields separated by the ':' character
                continue

    return gavDictionary

def loadGavDictionaryFromXmlFile(xmlFile, xPathPrefix = '/pom:project/pom:dependencyManagement/pom:dependencies/pom:dependency/pom:', nameSpace = { "pom" : "%s" % _pom_ns }):
    """
    Convert XML dependencies from 'xmlFile' into Maven GAV
    (groupId:artifactId:version) Python dictionary, where
    dictionary key is represented by 'groupId:artifactId'
    part of the GAV entry, and value is:

    * Either 'version' field of the GAV entry directly,
      if the version is numeric,

    * Or another child dictionary in the case the 'version' field
      of the GAV entry represents a property within the
      XML file. In this case, the key of the child dictionary
      item is the name of such a XML <property> element.
      The value of the child dictionary item is the
      value of the <property> itself.

    Returns GAV dictionary corresponding to 'xmlFile'
    or exits with error in case of a failure
    """
    xmlRoot = getXmlRoot(xmlFile)
    # Construct the final union xPath query returning all three
    # (GAV) subelements of a particular dependency element
    gavXPathQuery = '|'.join(map(lambda x: xPathPrefix + x, _gav_elements))
    xmlDependencyElements = getElementsByXPath(xmlRoot, gavXPathQuery, nameSpace)
    _logErrorAndExitIf(
        "Failed to load dependencies from XML file '%s'!" % xmlFile,
        len(xmlDependencyElements) == 0
    )
    gavDictionary = {}
    # Divide original list into sublists by three elements -- one sublist per GAV entry
    for gavEntry in [xmlDependencyElements[i:i + 3] for i in range(0, len(xmlDependencyElements), 3)]:
        (groupIdElem, artifactIdElem, versionElem) = (gavEntry[0], gavEntry[1], gavEntry[2])
        _logErrorAndExitIf(
            "Failed to load '%s' dependency from XML file!" % gavEntry,
            groupIdElem is None or artifactIdElem is None or versionElem is None
        )
        gavDictKey = groupIdElem.text + _gav_delimiter + artifactIdElem.text
        gavDictValue = versionElem.text
        if re.match(r'\d.*', gavDictValue):
            # Store the numeric artifact version into GAV dictionary
            gavDictionary[gavDictKey] = gavDictValue
        else:
            childDictKey = gavDictValue
            while not re.match(r'\d.*', gavDictValue):
                gavDictValue = re.sub(r'^\${', '', gavDictValue)
                gavDictValue = re.sub(r'}$', '', gavDictValue)
                propertyElem = getPomProperty(xmlRoot, gavDictValue)
                # Handle corner case when artifact version isn't value of some POM <property> element,
                # but rather value of some xPath within the XML file. Like for example the case of
                # 'project.version' value. Create a custom XPath query to fetch the actual numeric value
                if not propertyElem:
                    # Build xpath from version value, turn e.g. 'project.version' to '/pom:project/pom:version'
                    customXPath = _empty_string.join(list(map(lambda x: '/pom:' + x, gavDictValue.split('.'))))
                    # Fetch the numeric version
                    propertyElem = getElementsByXPath(xmlRoot, customXPath)
                    # Exit with error if it wasn't possible to determine the artifact version even this way
                    _logErrorAndExitIf(
                        "Unable to determine the version of the '%s' GA artifact, exiting!" % gavDictKey,
                        len(propertyElem) != 1
                    )
                # Assign the value of POM <property> or result of custom XPath
                # back to 'gavDictValue' field and check again
                gavDictValue = propertyElem[0].text

            # Store the numeric artifact version into GAV dictionary, keeping
            # the original POM <property> name as the key of the child dictionary
            gavDictionary[gavDictKey] = { '%s' % childDictKey : '%s' % gavDictValue }

    return gavDictionary

def mergeTwoGavDictionaries(firstGavDictionary, secondGavDictionary):
    """
    Return a single output GAV dictionary containing the united content of
    'firstGavDictionary' and 'secondGavDictionary' input GAV dictionaries.

    The process of merge is performed as follows:

    1) Distinct keys from both GAV dictionaries are copied into the output
       dictionary.

    2) If the key is present in both input GAV dictionaries (IOW it's shared),
       the value of the higher version from both input dictionaries is used
       as the final value for the united dictionary entry.
    """
    unitedGavDictionary = copy.deepcopy(firstGavDictionary)
    for secondDictKey in secondGavDictionary.keys():
        try:
            # Subcase when dictionary key from second GAV dictionary is
            # already present in the resulting GAV dictionary

            # Value of the key from resulting GAV dictionary might be a child
            # dictionary again. Get the numeric version of the artifact first
            currentValue = getNumericArtifactVersion(unitedGavDictionary, secondDictKey)
            # Vaue of the key from second GAV dictionary might be a child
            # dictionary again. Get the numeric version of the artifact first
            secondDictValue = getNumericArtifactVersion(secondGavDictionary, secondDictKey)

            # Update the artifact version in resulting GAV dictionary only if
            # the value from the second dictionary is higher than the current
            # one
            if compareMavenVersions(secondDictValue, currentValue) > 0:
                unitedGavDictionary[secondDictKey] = secondDictValue

        except KeyError:
            # Subcase when dictionary key from the second GAV dictionary is
            # not present in the resulting GAV dictionary. Insert it
            unitedGavDictionary[secondDictKey] = secondGavDictionary[secondDictKey]

    return unitedGavDictionary

#
# Data structures and routines to assist with the updates of
# the main Keycloak pom.xml necessary for Wildfly upgrade
#

# List of artifacts from main Keycloak pom.xml excluded from upgrade even though they would
# be usually applicable for the update. This allows to handle special / corner case like for
# example the ones below:
#
# * The version / release tag of specific artifact, as used by upstream of that artifact is
#   actually higher than the version, currently used in Wildfly / Wildfly Core. But the Python
#   version comparing algorithm used by this script, treats it as a lower one
#   (the cache of ApacheDS artifact below),
# * Explicitly avoid the update of certain artifact due whatever reason
#
# Add new entries to this list by moving them out of the _keycloakToWildflyProperties
# dictionary as necessary
_excludedProperties = [
    # Intentionally avoid Apache DS downgrade from "2.0.0.AM26" to Wildfly's current
    # "2.0.0-M24" version due to recent KEYCLOAK-14162
    "apacheds.version",
    # KEYCLOAK-17585 Prevent microprofile-metrics-api upgrades from version "2.3" due to:
    # https://issues.redhat.com/browse/KEYCLOAK-17585?focusedCommentId=16002705&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-16002705
    "microprofile-metrics-api.version"
]

# List of Keycloak specific properties listed in main Keycloak pom.xml file. These entries:
#
# * Either don't represent an artifact version (e.g. "product.rhsso.version" below),
# * Or represent an artifact version, but aren't used listed in Wildfly's or
#   Wildfly-Core's POMs (the artifact is either not referenced in those POM files at all
#   or explicitly excluded in some of them)
_keycloakSpecificProperties = [
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
    "tomcat8.version",
    "tomcat9.version",
    "spring-boot24.version",
    "spring-boot26.version",
    "webauthn4j.version",
    "org.apache.kerby.kerby-asn1.version",
]

# Mapping of artifact name as used in the main Keycloak pom.xml file to the name
# of the same artifact listed in Wildfly's or Wildfly-Core's pom.xml file
_keycloakToWildflyProperties = {
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
    "jboss.marshalling.version"                                   : "version.org.jboss.marshalling.jboss-marshalling",
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
    "microprofile-metrics-api.version"                            : "version.org.eclipse.microprofile.metrics.api",
    # Skip "replacer.plugin.version" since Keycloak specific
    # Skip "jboss.as.plugin.version" since Keycloak specific
    # Skip "jmeter.plugin.version" since Keycloak specific
    # Skip "jmeter.analysis.plugin.version" since Keycloak specific
    # Skip "minify.plugin.version" since Keycloak specific
    # Skip "osgi.bundle.plugin.version" since Keycloak specific
    "wildfly.plugin.version"                                      : "version.org.wildfly.plugin",
    # Skip "nexus.staging.plugin.version" since Keycloak specific
    # Skip "frontend.plugin.version" since Keycloak specific
    # Skip "docker.maven.plugin.version" since Keycloak specific
    # Skip "tomcat8.version", and "tomcat9.version" since Keycloak specific
    # Skip "spring-boot24.version" and "spring-boot26.version" since Keycloak specific
    # Skip "webauthn4j.version" since Keycloak specific
    # Skip "org.apache.kerby.kerby-asn1.version" since Keycloak specific
}

def _scanMainKeycloakPomFileForUnknownArtifacts():
    """
    Verify each artifact listed as property in the main Keycloak pom.xml file is present one of the:

    * _excludedProperties list -- explicitly requesting the update to be skipped due some reason,
    * _keycloakSpecificProperties list -- artifact is Keycloak specific,
    * _keycloakToWildflyProperties dictionary -- there's a clear mapping of Keycloak
      artifact property name to corresponding artifact property name as used in Wildfly /
      Wildfly Core

    Logs error message and exits with error if action for a particular artifact is unknown.
    """
    # Absolute path to main Keycloak pom.xml within the repo
    mainKeycloakPomPath = getKeycloakGitRepositoryRoot() + "/pom.xml"

    unknownArtifactMessage = (
            "Found so far unknown '%s' artifact in the main Keycloak pom.xml file!\n"
            "There's no clearly defined action on how to process this artifact yet!\n"
            "It's not an excluded one, not listed as Keycloak specific one, and not\n"
            "present in the set of those to be processed. Add it to one of:\n\n"
            " * _excludedProperties,\n"
            " * _keycloakSpecificProperties,\n"
            " * or _keycloakToWildflyProperties \n\n"
            "data structures in \"wildfly/upgrade/__init__.py\" to dismiss this error!\n"
            "Rerun the script once done."
    )
    for xmlTag in getElementsByXPath(getXmlRoot(mainKeycloakPomPath), '//pom:project/pom:properties/pom:*'):
        artifactName = xmlTag.tag.replace("{%s}" % _pom_ns, "")
        _logErrorAndExitIf (
            unknownArtifactMessage % artifactName,
            artifactName not in itertools.chain(_excludedProperties, _keycloakSpecificProperties, _keycloakToWildflyProperties.keys())
        )


def performMainKeycloakPomFileUpdateTask(wildflyPomFile, wildflyCorePomFile, forceUpdates = False):
    """
    Synchronize the versions of artifacts listed as properties in the main
    Keycloak pom.xml file with their counterparts taken from 'wildflyPomFile'
    or 'wildflyCorePomFile'.
    """
    wildflyXmlTreeRoot = getXmlRoot(wildflyPomFile)
    wildflyCoreXmlTreeRoot = getXmlRoot(wildflyCorePomFile)

    # Absolute path to main Keycloak pom.xml within the repo
    mainKeycloakPomPath = getKeycloakGitRepositoryRoot() + "/pom.xml"
    keycloakXmlTreeRoot = getXmlRoot(mainKeycloakPomPath)

    taskLogger = getTaskLogger('Update main Keycloak pom.xml')
    taskLogger.info('Synchronizing Wildfly (Core) artifact versions to the main Keycloak pom.xml file...')

    stepLogger = getStepLogger()

    _scanMainKeycloakPomFileForUnknownArtifacts()

    for keycloakElemName, wildflyElemName in _keycloakToWildflyProperties.items():

        if keycloakElemName == "wildfly.version":
            wildflyElem = getElementsByXPath(wildflyXmlTreeRoot, '/pom:project/pom:version')
        else:
            # Try to fetch updated artifact version from Wildfly Core's pom.xml first
            wildflyElem = getPomProperty(wildflyCoreXmlTreeRoot, wildflyElemName)
            # If not found, fetch it from Wildfly's pom.xml file
            if not wildflyElem:
                wildflyElem = getPomProperty(wildflyXmlTreeRoot, wildflyElemName)

        if wildflyElem:
            keycloakElem = getPomProperty(keycloakXmlTreeRoot, keycloakElemName)
            if keycloakElem:
                if keycloakElemName in _excludedProperties:
                    stepLogger.debug(
                        "Not updating version of '%s' from '%s' to '%s' since the artifact is excluded!" %
                        (keycloakElemName, keycloakElem[0].text, wildflyElem[0].text)
                    )
                elif (
                        forceUpdates or
                        compareMavenVersions(wildflyElem[0].text, keycloakElem[0].text) > 0
                ):
                    stepLogger.debug(
                        "Updating version of '%s' artifact to '%s'. Current '%s' version is less than that." %
                        (keycloakElemName, wildflyElem[0].text, keycloakElem[0].text)
                    )
                    keycloakElem[0].text = wildflyElem[0].text
                else:
                    stepLogger.debug(
                        "Not updating version of '%s' artifact to '%s'. Current '%s' version is already up2date." %
                        (keycloakElemName, wildflyElem[0].text, keycloakElem[0].text)
                    )
        else:
            stepLogger.error(
                "Unable to locate element with name: '%s' in '%s' or '%s'" %
                (wildflyElemName, wildflyPomFile, wildflyCorePomFile)
            )
            sys.exit(1)

    lxml.etree.ElementTree(keycloakXmlTreeRoot).write(mainKeycloakPomPath, encoding = "UTF-8", pretty_print = True, xml_declaration = True)
    stepLogger.info("Done syncing artifact version changes to: '%s'!" % mainKeycloakPomPath.replace(getKeycloakGitRepositoryRoot(), '.'))
    stepLogger.debug("Wrote updated main Keycloak pom.xml file to: '%s'" % mainKeycloakPomPath)


def performAdapterGalleonPackPomFileUpdateTask(wildflyCorePomFile, forceUpdates = False):
    """
    Synchronize Keycloak's version of 'version.org.wildfly.galleon-plugins' artifact in the adapter Galleon pack
    with its corresponding version from Wildfly Core
    """
    wildflyGalleonMavenPluginProperty = "version.org.wildfly.galleon-plugins"

    wildflyCoreXmlTreeRoot = getXmlRoot(wildflyCorePomFile)
    wildflyGalleonMavenPluginWildflyCoreElem = getPomProperty(wildflyCoreXmlTreeRoot, wildflyGalleonMavenPluginProperty)
    wildflyGalleonMavenPluginWildflyCoreVersion = wildflyGalleonMavenPluginWildflyCoreElem[0].text

    # Absolute path to the pom.xml file of the adapter Galleon pack within the repo
    adapterGalleonPackPomPath = getKeycloakGitRepositoryRoot() + "/distribution/galleon-feature-packs/adapter-galleon-pack/pom.xml"
    adapterGalleonPackXmlTreeRoot = getXmlRoot(adapterGalleonPackPomPath)
    wildflyGalleonMavenPluginAdapterGalleonPackElem = getPomProperty(adapterGalleonPackXmlTreeRoot, wildflyGalleonMavenPluginProperty)
    wildflyGalleonMavenPluginKeycloakVersion = wildflyGalleonMavenPluginAdapterGalleonPackElem[0].text

    taskLogger = getTaskLogger('Update pom.xml of adapter Galleon pack')
    taskLogger.info('Synchronizing Wildfly Core artifact versions to the pom.xml file of Keycloak adapter Galleon pack...')
    stepLogger = getStepLogger()
    if (
            forceUpdates or
            compareMavenVersions(wildflyGalleonMavenPluginWildflyCoreVersion, wildflyGalleonMavenPluginKeycloakVersion) > 0
    ):
        stepLogger.debug(
            "Updating version of '%s' artifact to '%s'. Current '%s' version is less than that." %
            (wildflyGalleonMavenPluginProperty, wildflyGalleonMavenPluginWildflyCoreVersion, wildflyGalleonMavenPluginKeycloakVersion)
        )
        wildflyGalleonMavenPluginAdapterGalleonPackElem[0].text = wildflyGalleonMavenPluginWildflyCoreElem[0].text
        lxml.etree.ElementTree(adapterGalleonPackXmlTreeRoot).write(adapterGalleonPackPomPath, encoding = "UTF-8", pretty_print = True, xml_declaration = True)
        stepLogger.info("Done syncing artifact version changes to: '%s'!" % adapterGalleonPackPomPath.replace(getKeycloakGitRepositoryRoot(), '.'))
        stepLogger.debug("Wrote updated pom.xml file to: '%s'" % adapterGalleonPackPomPath)
    else:
        stepLogger.debug(
            "Not updating version of '%s' artifact to '%s'. Current '%s' version is already up2date." %
            (wildflyGalleonMavenPluginProperty, wildflyGalleonMavenPluginWildflyCoreVersion, wildflyGalleonMavenPluginKeycloakVersion)
        )


#
# Routing handling necessary updates of various
# adapter license files related with a Wildfly upgrade
#

def updateAdapterLicenseFile(gavDictionary, xPathPrefix, nameSpace, licenseFile, forceLicenseFileUpdates = False):
    """
    Save GAV dictionary 'gavDictionary' back to XML 'licenseFile'.
    """
    licenseFileXmlTreeRoot = getXmlRoot(licenseFile)
    LICENSE_FILE_PARENT_DIR = os.path.dirname(licenseFile)
    stepLogger = getStepLogger()

    if not nameSpace:
        nsPrefix = ''
        dependencyElemXPath = '|'.join(map(lambda e: xPathPrefix + '/%s' % e, _gav_elements))
    else:
        nsPrefix = nameSpace.keys()
        dependencyElemXPath = '|'.join(map(lambda e: xPathPrefix + '/%s:%s' % (nsPrefix, e), _gav_elements))

    xmlDependencyElements = getElementsByXPath(licenseFileXmlTreeRoot, dependencyElemXPath, nameSpace)
    # Divide original list into sublists by three elements -- one sublist per GAV entry
    for gavEntry in [xmlDependencyElements[i:i + 3] for i in range(0, len(xmlDependencyElements), 3)]:
        currentArtifactVersion = expectedArtifactVersion = None
        groupIdElem, artifactIdElem, versionElem = gavEntry[0], gavEntry[1], gavEntry[2]
        _logErrorAndExitIf(
            "Failed to update '%s' XML dependency!" % gavEntry,
            groupIdElem is None or artifactIdElem is None or versionElem is None,
            logger = stepLogger
        )
        # KEYCLOAK-16202 Per:
        #
        # * https://github.com/keycloak/keycloak/pull/7463#discussion_r517346730 and
        # * https://github.com/keycloak/keycloak/pull/7463#discussion_r517346766
        #
        # skip automated updates of versions of "org.apache.httpcomponents:httpclient"
        # and "org.apache.httpcomponents:httpcore" dependencies in the Fuse adapter
        # license file(s) as part of the upgrade script run
        if (
                'fuse-adapter-zip' in licenseFile and
                groupIdElem.text == 'org.apache.httpcomponents' and
                artifactIdElem.text in ['httpclient', 'httpcore']
        ):

            httpComponentsFuseAdapterSpecificMessage = (
                "Not updating version of '%s:%s' artifact in the Fuse adapter license file," %
                 (groupIdElem.text, artifactIdElem.text),
                " since this adapter can work properly only with a specific,\n\t\tpreviously approved version!"
                " See '<apache.httpcomponents.fuse.version>' and '<apache.httpcomponents.httpcore.fuse.version>'",
                " properties in the main Keycloak pom.xml file\n\t\tfor further details."
            )
            stepLogger.info(_empty_string.join(httpComponentsFuseAdapterSpecificMessage))
            continue

        currentArtifactVersion = versionElem.text
        gavDictKey = groupIdElem.text + _gav_delimiter + artifactIdElem.text
        try:
            # Value of the artifact version might be a child dictionary again.
            # Get numeric artifact version first
            expectedArtifactVersion =  getNumericArtifactVersion(gavDictionary, gavDictKey)

            # Update the version of artifact if version from GAV dictionary is higher
            if (
                    expectedArtifactVersion != currentArtifactVersion and
                    forceLicenseFileUpdates or
                    compareMavenVersions(expectedArtifactVersion, versionElem.text) > 0
            ):

                updatingArtifactVersionMessage = (
                    "Updating the version of '%s, %s' artifact in the '%s' license file from: '%s' to: '%s'" %
                    (groupIdElem.text, artifactIdElem.text, licenseFile, currentArtifactVersion, expectedArtifactVersion)
                )
                stepLogger.debug(updatingArtifactVersionMessage)
                versionElem.text = expectedArtifactVersion
                # Subtask: Rename existing license text files tracked in this repository to the filename with the updated artifact version
                repositoryRoot = getKeycloakGitRepositoryRoot()
                for root, dirs, files in os.walk(LICENSE_FILE_PARENT_DIR):
                    for filename in files:
                        if re.search(re.escape(artifactIdElem.text) + r',' + re.escape(currentArtifactVersion), filename):
                            currentFilename = filename
                            currentFileName = currentFilename.replace(repositoryRoot, '').rstrip()
                            newFilename = currentFilename.replace(currentArtifactVersion, expectedArtifactVersion)
                            # Delete & recreate the TXT file if it previously existed (be idempotent)
                            if os.path.isfile(os.path.join(root, newFilename)):
                                os.remove(os.path.join(root, newFilename))
                            check_call(['git', 'mv', "%s" % os.path.join(root, currentFilename), "%s" % os.path.join(root, newFilename)], cwd = repositoryRoot)
                # Subtask: Update artifact version in license URL to the expected one
                dependencyElem = groupIdElem.getparent()
                urlElements = getElementsByXPath(dependencyElem, './licenses/license/url', nameSpace)
                _logErrorAndExitIf(
                    "Failed to retrieve <url> element of the '%s' artifact!" % gavDictKey,
                    len(urlElements) != 1,
                    logger = stepLogger
                )
                urlElem = urlElements[0]
                # Strip the '.redhat-\d+' suffix from artifact versions when processing RH-SSO adapters
                # since upstream URLs don't contain those
                if 'rh-sso' in licenseFile:
                    expectedArtifactVersion = re.sub(r'.redhat-\d+$', '', expectedArtifactVersion)
                # First handle special form of version numbers in release URLs used by org.bouncycastle artifacts
                if artifactIdElem.text.endswith('jdk15on'):
                    bouncyCastleMajorVersion = re.match(r'^(\d)\.', expectedArtifactVersion).group(1)
                    bouncyCastleMinorVersion = re.match(r'^\d+\.(\d+)', expectedArtifactVersion).group(1)
                    if bouncyCastleMajorVersion and bouncyCastleMinorVersion:
                        urlNotationOfExpectedBouncyCastleVersion = 'r' + bouncyCastleMajorVersion + 'rv' + bouncyCastleMinorVersion
                        try:
                            # Extract older (even archaic) 'major.minor.micro' artifact version substring from the URL
                            oldMajorMinorMicroVersion = re.search(r'(r\d+rv\d{2,})', urlElem.text).group(1)
                            if oldMajorMinorMicroVersion:
                                stepLogger.debug(
                                    "Replacing former '%s' of '%s' artifact version in the URL with the new '%s' version" %
                                    (oldMajorMinorMicroVersion, gavDictKey, expectedArtifactVersion)
                                )
                                urlElem.text = re.sub(r'r\d+rv\d{2,}', urlNotationOfExpectedBouncyCastleVersion, urlElem.text)
                        except AttributeError:
                            # Ignore generic URLs not containing 'major.minor.micro' information of this specific artifact
                            pass
                    else:
                        _logErrorAndExitIf(
                            "Unable to locate previous '%s' artifact version in the URL!" % gavDictKey,
                            True,
                            logger = stepLogger
                        )
                else:
                    try:
                        # Extract older (even archaic) 'major.minor.micro' artifact version substring from the URL
                        oldMajorMinorMicroVersion = re.search(r'(\d+\.\d+\.\d+)', urlElem.text).group(1)
                        if oldMajorMinorMicroVersion:
                            stepLogger.debug(
                                "Replacing former '%s' version of the '%s' artifact in the URL with the new '%s' version" %
                                (oldMajorMinorMicroVersion, gavDictKey, expectedArtifactVersion)
                            )
                            urlElem.text = re.sub(oldMajorMinorMicroVersion, expectedArtifactVersion, urlElem.text)
                        else:
                            _logErrorAndExitIf(
                                "Unable to locate previous '%s' artifact version in the URL!" % gavDictKey,
                                True,
                                logger = stepLogger
                            )
                    except AttributeError:
                        # Ignore generic URLs not containing 'major.minor.micro' information of this specific artifact
                        pass
            else:
                artifactVersionAlreadyHigherMessage = (
                    "Not updating version of '%s, %s' artifact to '%s'. Current '%s' version is already up2date." %
                    (groupIdElem.text, artifactIdElem.text, expectedArtifactVersion, currentArtifactVersion)
                )
                stepLogger.debug(artifactVersionAlreadyHigherMessage)

        except KeyError:
            # Cover the case when particular Keycloak / RH-SSO dependency isn't present in the GAV
            # file created from the list of all Maven artifacts used by Wildfly (Core) / JBoss EAP
            if 'keycloak' in licenseFile:
                productName = 'Keycloak'
                parentProductName = 'Wildfly (Core)'
            elif 'rh-sso' in licenseFile:
                productName = 'RH-SSO'
                parentProductName = 'JBoss EAP'
            else:
                productName = parentProductName = None
            _logErrorAndExitIf(
                "Failed to determine the product name while updating the '%s' license file!" % licenseFile,
                productName is None,
                logger = stepLogger
            )
            # Ignore artifacts not found in the Gav dictionary
            stepLogger.debug(
                "Skipping '%s' specific '%s' license dependency since not present in '%s' list of all Maven artifacts!" %
                (productName, gavDictKey, parentProductName)
            )
            pass

    lxml.etree.ElementTree(licenseFileXmlTreeRoot).write(licenseFile, encoding = "UTF-8", pretty_print = True, xml_declaration = True)
    relativeLicenseFilePath = licenseFile.replace(getKeycloakGitRepositoryRoot(), '.')
    stepLogger.info("Done syncing artifact version changes to: '%s'!" % relativeLicenseFilePath)
    stepLogger.debug("Wrote updated license file to: '%s'" % licenseFile)

#
# Routines performing particular tasks within a Wildfly upgrade
#

def performKeycloakAdapterLicenseFilesUpdateTask(wildflyPomFile, wildflyCorePomFile, forceUpdates = False):
    """
    Update artifacts versions of selected dependencies utilized by various
    Keycloak adapter license XML files. Also update the location of the
    corresponding license text files within the repository so their names
    reflect the updated artifacts versions.
    """
    # Operate on Keycloak adapters
    PROFILE = 'community'

    # Load XML dependencies from Wildfly (Core) POM files into GAV dictionary
    wildflyCoreXmlDependenciesGav = loadGavDictionaryFromXmlFile(wildflyCorePomFile)
    wildflyXmlDependenciesGav = loadGavDictionaryFromXmlFile(wildflyPomFile)
    # Merge both Wildfly and Wildfly Core GAV dictionaries into a united one,
    # containing all Wildfly (Core) artifacts and their versions
    unitedGavDictionary = mergeTwoGavDictionaries(
        wildflyCoreXmlDependenciesGav,
        wildflyXmlDependenciesGav
    )

    isTaskLogged = False
    (productName, productNameFull) = getProductNamesForKeycloakPomProfile(profile = PROFILE)
    taskLogger = getTaskLogger('Update %s Adapters' % productNameFull)
    gitRepositoryRoot = getKeycloakGitRepositoryRoot()
    for root, dirs, files in os.walk(gitRepositoryRoot):
        if not isTaskLogged:
            taskLabel = (
               "Updating artifacts versions in license XML files and locations of the license TXT files" +
               "\n\tfor the %s adapters in the '%s' directory..." % (productName, root)
            )
            taskLogger.info(taskLabel)
            isTaskLogged = True
        for filename in files:
            if re.search(r'distribution.*/src/main/resources/licenses/%s/licenses.xml' % productName.lower(), os.path.join(root, filename)):
                updateAdapterLicenseFile(
                    unitedGavDictionary,
                    xPathPrefix = '/licenseSummary/dependencies/dependency',
                    nameSpace = {},
                    licenseFile = os.path.join(root, filename),
                    forceLicenseFileUpdates = forceUpdates
                )

def performRhssoAdapterLicenseFilesUpdateTask(wildflyPomFile, wildflyCorePomFile, forceUpdates = False):
    """
    Update artifacts versions of selected dependencies utilized by various
    RH-SSO adapter license XML files. Also update the location of the
    corresponding license text files within the repository so their names
    reflect the updated artifacts versions.
    """
    # Operate on RH-SSO adapters
    PROFILE = 'product'

    isTaskLogged = False
    (productName, productNameFull) = getProductNamesForKeycloakPomProfile(profile = PROFILE)
    taskLogger = getTaskLogger('Update %s Adapters' % productNameFull)

    gavFileUrl = None
    print("\nPlease specify the URL of the GAV file to use for %s adapter updates:" % productNameFull.upper())
    gavFileUrl = sys.stdin.readline().rstrip()

    _logErrorAndExitIf(
        "Invalid URL '%s'! Please provide valid URL to the GAV file and retry!" % gavFileUrl,
        not gavFileUrl or not gavFileUrl.startswith('http://') and not gavFileUrl.startswith('https://')
    )
    gavFile = saveUrlToNamedTemporaryFile(gavFileUrl)
    taskLogger.debug("Downloaded content of provided GAV file to '%s'" % gavFile)
    gavDictionary = loadGavDictionaryFromGavFile(gavFile)

    gitRepositoryRoot = getKeycloakGitRepositoryRoot()
    for root, dirs, files in os.walk(gitRepositoryRoot):
        if not isTaskLogged:
            taskLabel = (
                "Updating artifacts versions in license XML files and locations of the license TXT files" +
                "\n\tfor the %s adapters in the '%s' directory..." % (productName.upper(), root)
            )
            taskLogger.info(taskLabel)
            isTaskLogged = True
        for filename in files:
            if re.search(r'distribution.*/src/main/resources/licenses/%s/licenses.xml' % productName.lower(), os.path.join(root, filename)):
                updateAdapterLicenseFile(
                    gavDictionary,
                    xPathPrefix = '/licenseSummary/dependencies/dependency',
                    nameSpace = {},
                    licenseFile = os.path.join(root, filename),
                    forceLicenseFileUpdates = forceUpdates
                )

def performDeprecatedWildflyTestingModuleUpdateTask(forceUpdates = False):
    """
    Update the properties of the deprecated Wildfly testing module present in
    the Arquillian testsuite if necessary. The properties are needed to be
    updated if and only if the Wildfly and Wildfly Core versions in the main
    Keycloak pom.xml file from the local Keycloak git clone are higher than
    Widfly and Wildfly Core versions in the main Keycloak pom.xml file of the
    master branch of official Keycloak GitHub repository (IOW if and only the
    main Keycloak pom.xml file in the local repository got already updated with
    new Wildfly and Wildfly Core artifact versions from the new tag)
    """
    # Prepare / hold the expected future values of the properties of the
    # deprecated Wildfly testing module present in Arquillian testsuite
    deprecatedWildflyModuleProperties = {}

    deprecatedWildflyModuleProperties['wildfly.deprecated.version'] = getPomPropertyFromRemoteXml(
        _keycloak_github_master_main_pom_base_url,
        'wildfly.version',
        errorMessage = "Unable to locate 'wildfly.version' property element in the remote XML file!",
        expectedElementsCount = 1
    )[0].text

    deprecatedWildflyModuleProperties['wildfly.deprecated.wildfly.core.version'] = getPomPropertyFromRemoteXml(
        _keycloak_github_master_main_pom_base_url,
        'wildfly.core.version',
        errorMessage = "Unable to locate 'wildfly.core.version' property element in the remote XML file!",
        expectedElementsCount = 1
    )[0].text

    deprecatedWildflyModuleProperties['wildfly.deprecated.arquillian.wildfly.container'] = getPomPropertyFromRemoteXml(
        _wildfly_github_tag_main_pom_base_url % deprecatedWildflyModuleProperties['wildfly.deprecated.version'],
        'version.org.wildfly.arquillian',
        errorMessage = "Unable to locate 'version.org.wildfly.arquillian' property element in the remote XML file!",
        expectedElementsCount = 1
    )[0].text

    taskLogger = getTaskLogger('Update Deprecated Wildfly Testing Module')
    taskLogger.info('Updating properties of the deprecated Wildfly testing module...')
    stepLogger = getStepLogger()

    # Absolute path to main Keycloak pom.xml within the local repo
    mainKeycloakPomPath = getKeycloakGitRepositoryRoot() + "/pom.xml"
    mainKeycloakPomXmlRoot = getXmlRoot(mainKeycloakPomPath)
    # Absolute path to pom.xml file of the Arquillian testsuite within the local repo
    arqTestSuitePomPath = getKeycloakGitRepositoryRoot() + "/testsuite/integration-arquillian/pom.xml"
    arqTestSuitePomXmlRoot = getXmlRoot(arqTestSuitePomPath)

    # Determine the current value of the 'wildfly.version' property element
    # from the main pom.xml file of the local Keycloak git repository clone
    currentLocalWildflyVersionElem = getPomProperty(mainKeycloakPomXmlRoot, 'wildfly.version')
    _logErrorAndExitIf(
        "Unable to determine the value of the 'wildfly.version' property element in the main Keycloak pom.xml file!",
        len(currentLocalWildflyVersionElem) != 1,
        logger = stepLogger
    )
    currentLocalWildflyVersion = currentLocalWildflyVersionElem[0].text
    # Determine the current value of the 'wildfly.core.version' property
    # element from the main pom.xml file of the local Keycloak git repository
    # clone
    currentLocalWildflyCoreVersionElem = getPomProperty(mainKeycloakPomXmlRoot, 'wildfly.core.version')
    _logErrorAndExitIf(
        "Unable to determine the value of the 'wildfly.core.version' property element in the main Keycloak pom.xml file!",
        len(currentLocalWildflyCoreVersionElem) != 1,
        logger = stepLogger
    )
    currentLocalWildflyCoreVersion = currentLocalWildflyCoreVersionElem[0].text
    # Update the properties of the deprecated Wildfly testing module present
    # in the Arquillian testsuite if the local Wildfly and Wildfly Core version
    # is higher than their counterparts currently present in the master branch
    # of the Keycloak GitHub repository (IOW only if main Keycloak's pom.xml
    # got previously already updated with artifact versions from the new
    # Wildfly tag)
    if (
           forceUpdates or
           compareMavenVersions(currentLocalWildflyVersion, deprecatedWildflyModuleProperties['wildfly.deprecated.version']) > 0 and
           compareMavenVersions(currentLocalWildflyCoreVersion, deprecatedWildflyModuleProperties['wildfly.deprecated.wildfly.core.version']) > 0
    ):

        for deprecatedProperty in deprecatedWildflyModuleProperties.keys():
            arqTestSuitePropertyElem = getPomProperty(arqTestSuitePomXmlRoot, deprecatedProperty)
            _logErrorAndExitIf(
                "Unable to locate the '%s' element in the pom.xml file of the Arquillian testsuite!",
                len(arqTestSuitePropertyElem) != 1,
                logger = stepLogger
            )
            stepLogger.debug(
                "Updating value of the '%s' property of the deprecated Wildfly module to '%s'" %
                (deprecatedProperty, deprecatedWildflyModuleProperties[deprecatedProperty])
            )
            arqTestSuitePropertyElem[0].text = deprecatedWildflyModuleProperties[deprecatedProperty]
    else:
        updateNotNecessaryMessage = (
            "Not updating the values of the properties of the deprecated Wildfly testing module!"
            "\n\t\t Versions of Wildfly and Wildfly Core artifacts found in the main pom.xml file"
            "\n\t\t of this repository are equal, or lower when compared to their respective versions"
            "\n\t\t currently present in the 'master' branch of the upstream GitHub Keycloak repository."
            "\n\t\t Update is not needed."
        )
        stepLogger.debug(_empty_string.join(updateNotNecessaryMessage))

    lxml.etree.ElementTree(arqTestSuitePomXmlRoot).write(arqTestSuitePomPath, encoding = "UTF-8", pretty_print = True, xml_declaration = True)
    stepLogger.info('Done syncing necessary changes to the deprecated Wildfly testing module!')

def performJbossParentVersionUpdateTask(wildflyTag, wildflyPomFile, wildflyCorePomFile, forceUpdates = False):
    taskLogger = getTaskLogger('Update Version of jboss-parent')
    taskLogger.info("Checking if the 'jboss-parent' version needs to be updated...")
    stepLogger = getStepLogger()

    # Absolute path to main Keycloak pom.xml within the local repo
    mainKeycloakPomPath = getKeycloakGitRepositoryRoot() + "/pom.xml"
    mainKeycloakPomXmlRoot = getXmlRoot(mainKeycloakPomPath)
    # Absolute path to the Keycloak's 'boms/pom.xml' location within the local repo
    keycloakBomsPomPath = getKeycloakGitRepositoryRoot() + "/boms/pom.xml"
    keycloakBomsPomXmlRoot = getXmlRoot(keycloakBomsPomPath)

    # Retrieve the current versions of the 'jboss-parent' from the:
    jbossParentVersionElems = {}
    # Main pom.xml file of the Wildfly repository
    jbossParentVersionElems['from.main.wildfly.pom']      = getElementsByXPath(getXmlRoot(wildflyPomFile), '/pom:project/pom:parent/pom:version')
    # Main pom.xml file of the Wildfly Core repository
    jbossParentVersionElems['from.main.wildfly.core.pom'] = getElementsByXPath(getXmlRoot(wildflyCorePomFile), '/pom:project/pom:parent/pom:version')
    # Main pom.xml file of the local Keycloak repository
    jbossParentVersionElems['from.main.keycloak.pom']     = getElementsByXPath(mainKeycloakPomXmlRoot, '/pom:project/pom:parent/pom:version')
    # The boms/pom.xml file of the local Keycloak repository
    jbossParentVersionElems['from.keycloak.boms.pom']     = getElementsByXPath(keycloakBomsPomXmlRoot, '/pom:project/pom:parent/pom:version')

    # Sanity check if jboss-parent elements were retrieved correctly from all of the four files
    # (in each case there should be exactly one 'jboss-parent' element present in the pom.xml)
    for key, value in jbossParentVersionElems.items():
        location =  (
                        key
                        .replace('boms.pom', "'boms/pom'")
                        .replace('keycloak', 'Keycloak')
                        .replace('wildfly', 'Wildfly')
                        .replace('core', 'Core')
                        .replace('from', 'from the')
                        .replace('pom', 'pom.xml')
                        .replace('.', ' ')
        )

        _logErrorAndExitIf(
            "Unable to determine the version of the 'jboss-parent' element %s file!" % location,
            len(value) != 1,
            logger = stepLogger
        )
        # Turn list containing one XML element into just the XML element itself
        jbossParentVersionElems[key] = value[0]

    # Synchronize the jboss-parent version in both the main Keycloak pom.xml file and in the 'boms/pom.xml' file
    # (if their version differs from the current versions used by Wildfly / Wildfly Core)
    if (
            forceUpdates or
            jbossParentVersionElems['from.main.wildfly.pom'].text == jbossParentVersionElems['from.main.wildfly.core.pom'].text and
            jbossParentVersionElems['from.main.wildfly.pom'].text != jbossParentVersionElems['from.main.keycloak.pom'].text or
            jbossParentVersionElems['from.main.wildfly.pom'].text != jbossParentVersionElems['from.keycloak.boms.pom'].text
    ):

        updatedJBossParentVersion = jbossParentVersionElems['from.main.wildfly.pom'].text
        stepLogger.info("Updating version of 'jboss-parent' in the main Keycloak pom.xml file...")
        jbossParentVersionElems['from.main.keycloak.pom'].text = updatedJBossParentVersion
        lxml.etree.ElementTree(mainKeycloakPomXmlRoot).write(mainKeycloakPomPath, encoding = "UTF-8", pretty_print = True, xml_declaration = True)
        stepLogger.info("'jboss-parent' version updated to '%s' to match the version used by Wildfly '%s'!" % (updatedJBossParentVersion, wildflyTag))

        stepLogger.info("Updating version of 'jboss-parent' in the Keycloak's 'boms/pom.xml' file...")
        jbossParentVersionElems['from.keycloak.boms.pom'].text = updatedJBossParentVersion
        lxml.etree.ElementTree(keycloakBomsPomXmlRoot).write(keycloakBomsPomPath, encoding = "UTF-8", pretty_print = True, xml_declaration = True)
        stepLogger.info("'jboss-parent' version updated to '%s' to match the version used by Wildfly '%s'!" % (updatedJBossParentVersion, wildflyTag))

    # No update necessary ('jboss-parent' versions are already equal)
    else:
        jbossParentVersionUpdateNotNecessaryMsg = (
            "Update of the 'jboss-parent' version is not necessary!",
            "\n\t\tCurrent '%s' version used by Keycloak already matches the current '%s' version used by Wildfly '%s'." %
            (jbossParentVersionElems['from.main.keycloak.pom'].text, jbossParentVersionElems['from.main.wildfly.pom'].text, wildflyTag)
        )
        stepLogger.info(_empty_string.join(jbossParentVersionUpdateNotNecessaryMsg))

def synchronizeInfinispanSubsystemXmlNamespaceWithWildfly(wildflyTag):
    """
    Update the XML namespace of the 'subsystem' element of the Keycloak
    Infinispan subsystem template with its current value as used by Wildfly.
    """
    taskLogger = getTaskLogger("Update 'urn:jboss:domain:infinispan:*' version")
    taskLogger.info('Synchronizing XML namespace of Infinispan subsystem from Wildfly to Keycloak...')
    stepLogger = getStepLogger()

    wildflyInfinispanSubsystemElementErrorMessage = (
        "Unable to locate 'subsystem' XML element in the remote XML file!"
    )
    # Retrieve 'subsystem' element from the Wildfly's Infinispan
    # subsystem-template XML file
    wildflyInfinispanSubsystemElement = list(filter(
        lambda elem: 'subsystem' in elem.tag,
        getElementsByXPathFromRemoteXml(
            _wildfly_github_tag_ispn_subtempl_base_url % wildflyTag,
            xPath = '/config/*',
            nameSpace = {}
         )
    ))
    # Sanity check
    _logErrorAndExitIf(
        wildflyInfinispanSubsystemElementErrorMessage,
        len(wildflyInfinispanSubsystemElement) != 1
    )
    # Retrieve namespace value of that 'subsystem' element
    wildflyInfinispanSubsystemElementNamespace = (
        lxml.etree.QName(wildflyInfinispanSubsystemElement[0]).namespace
    )
    # Absolute path to the Infinispan subsystem template XML file
    # within local Keycloak git repository
    keycloakInfinispanSubsystemTemplateXmlPath = (
        getKeycloakGitRepositoryRoot() +
        '/wildfly/server-subsystem/src/main/resources/subsystem-templates/keycloak-infinispan.xml'
    )
    # XML root of the Keycloak Infinispan subsystem template XML file
    keycloakInfinispanSubsystemTemplateXmlRoot = getXmlRoot(
        keycloakInfinispanSubsystemTemplateXmlPath
    )
    keycloakInfinispanSubsystemElementErrorMessage = (
        "Unable to locate 'subsystem' XML element in the local XML file!",
    )
    # Retrieve 'subsystem' element from the Keycloak's Infinispan
    # subsystem-template XML file
    keycloakInfinispanSubsystemElement = list(filter(
        lambda elem: 'subsystem' in elem.tag,
        getElementsByXPath(
            keycloakInfinispanSubsystemTemplateXmlRoot,
            xPath = '/config/*',
            nameSpace = {}
        )
    ))
    # Sanity check
    _logErrorAndExitIf(
        keycloakInfinispanSubsystemElementErrorMessage,
        len(keycloakInfinispanSubsystemElement) != 1
    )
    # Update namespace of Keycloak's Infinispan 'subsystem' element in
    # subsystem-template XML file to match namespace value from Wildfly
    keycloakInfinispanSubsystemElementParent = keycloakInfinispanSubsystemElement[0].getparent()
    keycloakInfinispanSubsystemElementIndex = keycloakInfinispanSubsystemElementParent.index(
        keycloakInfinispanSubsystemElement[0]
    )
    keycloakInfinispanSubsystemElementParent.remove(keycloakInfinispanSubsystemElement[0])
    keycloakInfinispanSubsystemElementParent.insert(
        keycloakInfinispanSubsystemElementIndex,
        wildflyInfinispanSubsystemElement[0]
    )
    # Write the changes back to Keycloak Infinispan subsystem-template XML file
    lxml.etree.ElementTree(keycloakInfinispanSubsystemTemplateXmlRoot).write(
        keycloakInfinispanSubsystemTemplateXmlPath,
        encoding = "UTF-8",
        pretty_print = True,
        xml_declaration = True
    )
    stepLogger.info(
        "Updated XML namespace of the Keycloak's Infinispan subsystem to '%s'" %
        lxml.etree.QName(wildflyInfinispanSubsystemElement[0]).namespace
    )
