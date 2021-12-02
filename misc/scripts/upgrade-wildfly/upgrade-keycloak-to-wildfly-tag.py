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

import click, logging, os, sys

import lib.wildfly.upgrade as wu

CONTEXT_SETTINGS  = dict(help_option_names = ['-h', '--help'])
FORCE_OPTION_HELP = """
    Force elements / files updates.

    In common mode of operation (without the "-f" or "--force" options) the
    script upgrades the version of the Keycloak characteristic in question
    (POM property, dependency, or some other XML element shared with Wildfly
    application server) ONLY if the new version is HIGHER than the version of
    the corresponding element currently present in the local copy of the
    Keycloak repository, the script is operating on.

    The -f, --force options instruct the script to allow an upgrade to replace
    newer version of a particular Keycloak characteristic with an older one.
    Useful to perform e.g. Keycloak downgrades to previous Wildfly versions.
"""

RHSSO_ADAPTERS_OPTION_HELP = """
    Update artifacts versions of selected dependencies utilized by various
    RH-SSO adapter license XML files. Also update the location of the
    corresponding license text files within the repository so their names
    reflect the updated artifacts versions.
"""

@click.command(context_settings=CONTEXT_SETTINGS)
@click.argument('tag', required = True, type=click.STRING)
@click.option('-f', '--force', help=FORCE_OPTION_HELP, is_flag=True)
@click.option('-r', '--update-rh-sso-adapters', help=RHSSO_ADAPTERS_OPTION_HELP, is_flag=True)
@click.option('-v', '--verbose', help='Enable verbose output.', is_flag=True)
@click.version_option(prog_name=sys.argv[0], version=wu.__version__)
def processParameters(tag, verbose, force, update_rh_sso_adapters):
    """
    NAME

    upgrade-keycloak-to-wildfly-tag.py - Rebase Keycloak on top of the
    specified Wildfly tag (release)

    DESCRIPTION

        Update the versions of various Keycloak characteristics (versions of
        POM properties, adapter dependencies, and other attributes actually
        binding the Keycloak POM build configuration to the particular Wildfly
        tag) to their corresponding values as used by the Wildfly application
        server of version matching the tag / release, passed to the script as
        argument.

    EXAMPLES

        Upgrade Keycloak to Wildfly 20 (using "20.0.1.Final" Wildfly tag):

            $ python upgrade-keycloak-to-wildfly-tag.py 20.0.1.Final

        Downgrade Keycloak to Wildfly 16 (using "16.0.0.Final" Wildfly tag,
        script verbose mode to display the details about elements being
        updated, and force option to perform the actual downgrade):

            $ python upgrade-keycloak-to-wildfly-tag.py -v -f 16.0.0.Final
    """

    # Set loglevel to debug if '-v' or '--verbose' option was specified
    wu.__loglevel__ = logging.DEBUG if verbose else logging.INFO

    upgradeKeycloakToWildflyTag(tag, forceUpdates = force, ssoAdapters = update_rh_sso_adapters)

def upgradeKeycloakToWildflyTag(tag, forceUpdates = False, ssoAdapters = False):
    wildflyTag = wu.isWellFormedWildflyTag(tag)
    wildflyPomBaseUrl = "https://github.com/wildfly/wildfly/raw/%s/pom.xml" % wildflyTag

    taskLogger = wu.getTaskLogger("Rebase Keycloak on top of Wildfly '%s'" % wildflyTag)
    taskLogger.info("Retrieving Wildfly's pom.xml for tag: %s" % wildflyTag)
    wildflyPomFile = wu.saveUrlToNamedTemporaryFile(wildflyPomBaseUrl)
    wildflyPomXmlRoot = wu.getXmlRoot(wildflyPomFile)

    wildflyCoreTag = wu.isWellFormedWildflyTag( wu.getPomProperty(wildflyPomXmlRoot, "version.org.wildfly.core")[0].text )
    wildflyCorePomBaseUrl = "https://github.com/wildfly/wildfly-core/raw/%s/pom.xml" % wildflyCoreTag
    taskLogger.info("Retrieving Wildfly-Core pom.xml for tag: %s" % wildflyCoreTag)
    wildflyCorePomFile = wu.saveUrlToNamedTemporaryFile(wildflyCorePomBaseUrl)

    if wildflyPomFile != None and wildflyCorePomFile != None:

        # Subtask - Update main Keycloak pom.xml file
        wu.performMainKeycloakPomFileUpdateTask(wildflyPomFile, wildflyCorePomFile, forceUpdates)
        # Subtask - Update adapter-galleon-pack pom.xml file if necessary
        wu.performAdapterGalleonPackPomFileUpdateTask(wildflyCorePomFile, forceUpdates)
        # Subtask - Update Keycloak adapters
        wu.performKeycloakAdapterLicenseFilesUpdateTask(wildflyPomFile, wildflyCorePomFile, forceUpdates)

        if ssoAdapters:
            # Subtask - Update RH-SSO adapters
            wu.performRhssoAdapterLicenseFilesUpdateTask(wildflyPomFile, wildflyCorePomFile, forceUpdates)
        else:
            skipRhSsoAdapterUpdatesMessage = (
                "Skipping RH-SSO adapters updates since their changes weren't requested!",
                "\n\tRerun the script with '-r' or '--update-rh-sso-adapters' option to request them."
            )
            taskLogger.warning(wu._empty_string.join(skipRhSsoAdapterUpdatesMessage))

        # Subtask - Update properties of the deprecated Wildfly testing module if necessary
        wu.performDeprecatedWildflyTestingModuleUpdateTask(forceUpdates)
        # Subtask - Update version of jboss-parent if necessary
        wu.performJbossParentVersionUpdateTask(wildflyTag, wildflyPomFile, wildflyCorePomFile, forceUpdates)
        # Subtask - Synchronize the XML namespace of the 'subsystem' element of the Keycloak
        #           Infinispan subsystem template with its current value as used by Wildfly
        wu.synchronizeInfinispanSubsystemXmlNamespaceWithWildfly(wildflyTag)

        for filename in [wildflyPomFile, wildflyCorePomFile]:
            os.remove(filename)

    rebaseDoneMessage = (
        "Done rebasing Keycloak to Wildfly '%s' release!" % wildflyTag,
        "\n\tRun 'git status' to list the changed files and 'git diff <path>' to inspect changes done to a specific file."
    )
    taskLogger.info(wu._empty_string.join(rebaseDoneMessage))

if __name__ == '__main__':
    processParameters()
