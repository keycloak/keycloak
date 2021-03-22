#!/usr/bin/env python

# *
# * Copyright 2020 Red Hat, Inc. and/or its affiliates
# * and other contributors as indicated by the @author tags.
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# * http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# *
# *
#
# Purpose: Update various necessary bits of Keycloak to align with the specified Wildfly tag. Perform this by:
#
#          * Incrementing the jboss-parent element version if necessary,
#          * Updating versions of artifacts shared with Wildfly and Wildfly Core in main Keycloak pom.xml file,
#          * Updating versions of artifacts shared with Wildfly and Wildfly Core utilized by Keycloak adapters
#
# Usage:   Run as, e.g.:
#          ./upgrade-keycloak-to-wildfly-tag.py 20.0.0.Final
#
#          Or call the script without arguments to get the further help

import os, sys

import wildfly.upgrade as wu

def usage():
    print("Run as: \n\t%s Wildfly.Tag.To.Upgrade.To \ne.g.:\n\t%s 20.0.0.Final\n" % (sys.argv[0], sys.argv[0]))

if __name__ == '__main__':

    if len(sys.argv) != 2:
        usage()
        sys.exit(1)

    wildflyTag = wu.isWellFormedWildflyTag(sys.argv[1])
    wildflyPomBaseUrl = "https://github.com/wildfly/wildfly/raw/%s/pom.xml" % wildflyTag

    wu.getModuleLogger().info("Retrieving Wildfly's pom.xml for tag: %s" % wildflyTag)
    wildflyPomFile = wu.saveUrlToNamedTemporaryFile(wildflyPomBaseUrl)

    wildflyPomXmlRoot = wu.getXmlRoot(wildflyPomFile)
    wildflyCoreTag = wu.isWellFormedWildflyTag( wu.getPomProperty(wildflyPomXmlRoot, "version.org.wildfly.core")[0].text )
    wildflyCorePomBaseUrl = "https://github.com/wildfly/wildfly-core/raw/%s/pom.xml" % wildflyCoreTag

    wu.getModuleLogger().info("Retrieving Wildfly-Core pom.xml for tag: %s" % wildflyCoreTag)
    wildflyCorePomFile = wu.saveUrlToNamedTemporaryFile(wildflyCorePomBaseUrl)

    if wildflyPomFile != None and wildflyCorePomFile != None:

        # Subtask - Update main Keycloak pom.xml file
        wu.updateMainKeycloakPomFile(wildflyPomFile, wildflyCorePomFile)
        # Subtask - Update Keycloak adapters
        wu.performKeycloakAdapterLicenseFilesUpdateTask(wildflyPomFile, wildflyCorePomFile)
        # Subtask - Update RH-SSO adapters
        wu.performRhssoAdapterLicenseFilesUpdateTask(wildflyPomFile, wildflyCorePomFile)

        for filename in [wildflyPomFile, wildflyCorePomFile]:
            os.remove(filename)
