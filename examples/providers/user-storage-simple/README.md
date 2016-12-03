Example User Storage Provider
===================================================

This is an example of user storage backed by a simple properties file.  This properties file only contains username/password
key pairs.  To deploy this provider you must have Keycloak running in standalone or standalone-ha mode.  Then type the follow maven command:

        mvn clean install wildfly:deploy


The "readonly-property-file" provider is hardcoded to look within the users.properties file embeded in the deployment jar
THere is one user 'tbrady' with a password of 'superbowl'

The "writeable-property-file" provider can be configured to point to a property file on disk.  It uses federated
storage to augment the property file with any other information the user wants.

Our developer guide walks through the implementation of both of these providers.
