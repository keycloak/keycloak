/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.services;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;


/**
 * Main logger for the Keycloak Services module.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@MessageLogger(projectCode="KC-SERVICES", length=4)
public interface ServicesLogger extends BasicLogger {

    ServicesLogger ROOT_LOGGER = Logger.getMessageLogger(ServicesLogger.class, "org.keycloak.services");

    @LogMessage(level = INFO)
    @Message(id=1, value="Loading config from %s")
    void loadingFrom(Object from);

    @LogMessage(level = ERROR)
    @Message(id=2, value="Failed to migrate datamodel")
    void migrationFailure(@Cause Throwable t);

    @LogMessage(level = INFO)
    @Message(id=3, value="Not importing realm %s from %s.  It already exists.")
    void realmExists(String realmName, String from);

    @LogMessage(level = INFO)
    @Message(id=4, value="Imported realm %s from %s.")
    void importedRealm(String realmName, String from);

    @LogMessage(level = WARN)
    @Message(id=5, value="Unable to import realm %s from %s.")
    void unableToImportRealm(@Cause Throwable t, String realmName, String from);

    @LogMessage(level = INFO)
    @Message(id=6, value="Importing users from '%s'")
    void imprtingUsersFrom(Object from);

    @LogMessage(level = ERROR)
    @Message(id=7, value="Failed to load 'keycloak-add-user.json'")
    void failedToLoadUsers(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id=8, value="Failed to add user %s to realm %s: realm not found")
    void addUserFailedRealmNotFound(String user, String realm);

    @LogMessage(level = INFO)
    @Message(id=9, value="Added user '%s' to realm '%s'")
    void addUserSuccess(String user, String realm);

    @LogMessage(level = ERROR)
    @Message(id=10, value="Failed to add user '%s' to realm '%s': user with username exists")
    void addUserFailedUserExists(String user, String realm);

    @LogMessage(level = ERROR)
    @Message(id=11, value="Failed to add user '%s' to realm '%s'")
    void addUserFailed(@Cause Throwable t, String user, String realm);

    @LogMessage(level = ERROR)
    @Message(id=12, value="Failed to delete '%s'")
    void failedToDeleteFile(String fileName);

    @LogMessage(level = ERROR)
    @Message(id=13, value="failed authentication")
    void failedAuthentication(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id=14, value="Failed client authentication")
    void failedClientAuthentication(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id=15, value="Unexpected error when authenticating client")
    void errorAuthenticatingClient(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id=16, value="Unknown flow to execute with")
    void unknownFlow();

    @LogMessage(level = ERROR)
    @Message(id=17, value="Unknown result status")
    void unknownResultStatus();

    @LogMessage(level = WARN)
    @Message(id=18, value="Client %s doesn't have have authentication method configured. Fallback to %s")
    void authMethodFallback(String clientId, String expectedClientAuthType);
}
