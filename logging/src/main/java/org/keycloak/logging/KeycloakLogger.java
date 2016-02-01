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
package org.keycloak.logging;

import java.io.IOException;
import javax.naming.NamingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.FATAL;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Main logger for the Keycloak Services module.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@MessageLogger(projectCode = "KC-ROOT", length = 4)
public interface KeycloakLogger extends BasicLogger {

    final KeycloakLogger.Config CONFIG = Logger.getMessageLogger(KeycloakLogger.Config.class, "keycloak.config");
    final KeycloakLogger.Realm REALM = Logger.getMessageLogger(KeycloakLogger.Realm.class, "keycloak.realm");
    final KeycloakLogger.User USER = Logger.getMessageLogger(KeycloakLogger.User.class, "keycloak.user");
    final KeycloakLogger.Client CLIENT = Logger.getMessageLogger(KeycloakLogger.Client.class, "keycloak.client");
    final KeycloakLogger.Role ROLE = Logger.getMessageLogger(KeycloakLogger.Role.class, "keycloak.role");
    final KeycloakLogger.IDP IDP = Logger.getMessageLogger(KeycloakLogger.IDP.class, "keycloak.idp");
    final KeycloakLogger.Authentication AUTH = Logger.getMessageLogger(KeycloakLogger.Authentication.class, "keycloak.auth");
    final KeycloakLogger.Group GROUP = Logger.getMessageLogger(KeycloakLogger.Group.class, "keycloak.group");
    final KeycloakLogger.Session SESSION = Logger.getMessageLogger(KeycloakLogger.Session.class, "keycloak.session");
    final KeycloakLogger.Event EVENT = Logger.getMessageLogger(KeycloakLogger.Event.class, "keycloak.event");
    final KeycloakLogger.ImportExport IMPORT_EXPORT = Logger.getMessageLogger(KeycloakLogger.ImportExport.class, "keycloak.import-export");
    final KeycloakLogger.Migration MIGRATION = Logger.getMessageLogger(KeycloakLogger.Migration.class, "keycloak.migration");

    @MessageLogger(projectCode = "KC-CONFIG", length = 4)
    static interface Config {

        @LogMessage(level = INFO)
        @Message(id = 1, value = "Loading config from %s")
        void loadingFrom(Object from);

        @LogMessage(level = ERROR)
        @Message(id = 2, value = "Exception during rollback")
        void exceptionDuringRollback(@Cause RuntimeException e);

        @LogMessage(level = INFO)
        @Message(id = 3, value = "Initializing %s realm")
        void initializingAdminRealm(String adminRealmName);

        @LogMessage(level = WARN)
        @Message(id = 4, value = "Failed to format message due to: %s")
        void failedToFormatMessage(String cause);

        @LogMessage(level = WARN)
        @Message(id = 5, value = "Failed to load messages")
        void failedToloadMessages(@Cause IOException ioe);

        @LogMessage(level = WARN)
        @Message(id = 6, value = "Locale not specified for messages.json")
        void localeNotSpecified();

        @LogMessage(level = WARN)
        @Message(id = 7, value = "Message bundle not found for language code '%s'")
        void msgBundleNotFound(String lang);

        @LogMessage(level = FATAL)
        @Message(id = 8, value = "Message bundle not found for language code 'en'")
        void msgBundleNotFoundForEn();

        @LogMessage(level = ERROR)
        @Message(id = 9, value = "Failed to run scheduled task %s")
        void failedToRunScheduledTask(@Cause Throwable t, String taskClass);

        @LogMessage(level = ERROR)
        @Message(id = 10, value = "Failed to close ProviderSession")
        void failedToCloseProviderSession(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 11, value = "Rejected non-local attempt to create initial user from %s")
        void rejectedNonLocalAttemptToCreateInitialUser(String remoteAddr);

        @LogMessage(level = INFO)
        @Message(id = 12, value = "Created initial admin user with username %s")
        void createdInitialAdminUser(String userName);

        @LogMessage(level = WARN)
        @Message(id = 13, value = "Rejected attempt to create initial user as user is already created")
        void initialUserAlreadyCreated();

        @LogMessage(level = ERROR)
        @Message(id = 14, value = "SAML assertion consumer url not set up")
        void samlAssertionConsumerUrlNotSetUp();

        @LogMessage(level = INFO)
        @Message(id = 15, value = "No truststore provider found - using default SSLSocketFactory")
        void noTruststoreProviderFound();

        @LogMessage(level = WARN)
        @Message(id = 16, value = "failed to load properties")
        void failedToLoadProperties(@Cause Exception e);

        @LogMessage(level = WARN)
        @Message(id = 17, value = "failed to load messages")
        void failedToLoadMessages(@Cause Exception e);
    }

    @MessageLogger(projectCode = "KC-REALM", length = 4)
    static interface Realm {

        @LogMessage(level = ERROR)
        @Message(id = 1, value = "Failed processing type")
        void failedProcessingType(@Cause Exception e);

        @LogMessage(level = WARN)
        @Message(id = 2, value = "login failure for user %s from ip %s")
        void loginFailure(String user, String ip);

        @LogMessage(level = INFO)
        @Message(id = 3, value = "Added 'kerberos' to required realm credentials")
        void addedKerberosToRealmCredentials();

        @LogMessage(level = WARN)
        @Message(id = 4, value = "Failed to get theme request")
        void failedToGetThemeRequest(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 5, value = "Failed to create theme")
        void failedToCreateTheme(@Cause Exception e);

        @LogMessage(level = WARN)
        @Message(id = 6, value = "Failed to load properties")
        void failedToLoadProperties(@Cause Exception e);

        @LogMessage(level = WARN)
        @Message(id = 7, value = "Failed to load messages")
        void failedToLoadMessages(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 8, value = "Failed to process template")
        void failedToProcessTemplate(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 9, value = "Failed to send verification email")
        void failedToSendVerificationEmail(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 10, value = "Failed to find %s theme %s, using built-in themes")
        void failedToFindBuiltInTheme(String type, String name);

        @LogMessage(level = ERROR)
        @Message(id = 11, value = "%s failed to load theme, type=%s, name=%s")
        void failedToLoadTheme(@Cause Exception e, String className, String type, String name);
    }

    @MessageLogger(projectCode = "KC-USER", length = 4)
    static interface User {

        @LogMessage(level = ERROR)
        @Message(id = 1, value = "")
        void modelDuplicateException(@Cause Exception mde);

        @LogMessage(level = ERROR)
        @Message(id = 2, value = "Error occurred during full sync of users")
        void errorDuringFullUserSync(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 3, value = "Error occurred during sync of changed users")
        void errorDuringChangedUserSync(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 4, value = "Failed to update Password")
        void failedToUpdatePassword(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 5, value = "Failed to send execute actions email")
        void failedToSendActionsEmail(@Cause Exception e);

        @LogMessage(level = WARN)
        @Message(id = 6, value = "JSON field path is not configured for mapper %s")
        void jsonFieldPathNotConfiguredForMapper(String mapper);

        @LogMessage(level = WARN)
        @Message(id = 7, value = "JSON field path is invalid %s")
        void jsonFieldPathInvalid(String jsonField);

        @LogMessage(level = ERROR)
        @Message(id = 8, value = "Attribute is not configured for mapper %s")
        void attributeNotConfiguredForMapper(String mapperName);

        @LogMessage(level = WARN)
        @Message(id = 9, value = "There are more values for attribute '%s' of user '%s' . Will display just first value")
        void moreValuesForAttribute(String attrKey, String userName);

    }

    @MessageLogger(projectCode = "KC-CLIENT", length = 4)
    static interface Client {

        @LogMessage(level = ERROR)
        @Message(id = 1, value = "Error when validating client assertion")
        void errorValidatingAssertion(@Cause Throwable t);

        @LogMessage(level = WARN)
        @Message(id = 2, value = "Multiple values found '%s' for protocol mapper '%s' but expected just single value")
        void multipleValuesForMapper(String attrValue, String mapper);

        @LogMessage(level = ERROR)
        @Message(id = 3, value = "%s")
        void clientRegistrationException(String message);

        @LogMessage(level = WARN)
        @Message(id = 4, value = "Truststore is disabled")
        void trustStoreIsDisabled();

    }

    @MessageLogger(projectCode = "KC-ROLE", length = 4)
    static interface Role {
    }

    @MessageLogger(projectCode = "KC-IDP", length = 4)
    static interface IDP {

        @LogMessage(level = WARN)
        @Message(id = 1, value = "No duplication detected.")
        void noDuplicationDetected();

        @LogMessage(level = WARN)
        @Message(id = 2, value = "%s is null. Reset flow and enforce showing reviewProfile page")
        void resetFlow(String emailOrUserName);

        @LogMessage(level = ERROR)
        @Message(id = 3, value = "Failed to send email to confirm identity broker linking")
        void confirmBrokerEmailFailed(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 4, value = "Key parameter don't match with the expected value from client session")
        void keyParamDoesNotMatch();

        @LogMessage(level = WARN)
        @Message(id = 5, value = "Smtp is not configured for the realm. Ignoring email verification authenticator")
        void smtpNotConfigured();

        @LogMessage(level = ERROR)
        @Message(id = 6, value = "Unknown action: %s")
        void unknownAction(String action);

        @LogMessage(level = ERROR)
        @Message(id = 7, value = "%s")
        void errorAuthenticating(@Cause Exception e, String message);

        @LogMessage(level = WARN)
        @Message(id = 8, value = "Error when closing LDAP connection")
        void errorClosingLDAP(@Cause NamingException ne);

        @LogMessage(level = ERROR)
        @Message(id = 9, value = "Could not fire event.")
        void couldNotFireEvent(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 10, value = "%s")
        void eventError(String message);

        @LogMessage(level = ERROR)
        @Message(id = 11, value = "%s")
        void errorEvent(@Cause Throwable t, String message);

        @LogMessage(level = INFO)
        @Message(id = 12, value = "Syncing data for mapper '%s' of type '%s'. Direction: %s")
        void syncingDataForMapper(String modelName, String mapperType, String direction);

        @LogMessage(level = ERROR)
        @Message(id = 13, value = "Failed to make identity provider oauth callback")
        void failedToMakeIDPCallback(@Cause Throwable t);

        @LogMessage(level = WARN)
        @Message(id = 14, value = "Untranslated protocol Error: %s so we return default SAML error")
        void untranslatedProtocolError(String errorName);

        @LogMessage(level = WARN)
        @Message(id = 15, value = "LinkedIn profile URL is without second part: %s")
        void linkedInProfileUrlIsWithoutSecondPart(String profileUrl);

        @LogMessage(level = WARN)
        @Message(id = 16, value = "LinkedIn profile URL is without path part: %s")
        void linkedInProfileUrlIsWithoutPath(String profileUrl);

        @LogMessage(level = WARN)
        @Message(id = 17, value = "LinkedIn profile URL is malformed: %s")
        void linkedInProfileUrlIsMalformed(String profileUrl);

        @LogMessage(level = WARN)
        @Message(id = 18, value = "LinkedIn profile URL %s username extraction failed: ")
        void linkedInProfileUsernameExtractionFailed(@Cause Exception e, String profileUrl);

        @LogMessage(level = WARN)
        @Message(id = 19, value = "Stackoverflow profile URL is without third part: %s")
        void stackoverflowProfileUrlIsWithoutThirdPart(String profileUrl);

        @LogMessage(level = WARN)
        @Message(id = 20, value = "Stackoverflow profile URL is without path part: %s")
        void stackoverflowProfileUrlIsWithoutPath(String profileUrl);

        @LogMessage(level = WARN)
        @Message(id = 21, value = "Stackoverflow profile URL is malformed: %s")
        void stackoverflowProfileUrlIsMalformed(String profileUrl);

        @LogMessage(level = WARN)
        @Message(id = 22, value = "Stackoverflow profile URL %s username extraction failed: ")
        void stackoverflowProfileUsernameExtractionFailed(@Cause Exception e, String profileUrl);

    }

    @MessageLogger(projectCode = "KC-AUTH", length = 4)
    static interface Authentication {

        @LogMessage(level = ERROR)
        @Message(id = 1, value = "failed authentication")
        void failedAuthentication(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 2, value = "Failed client authentication")
        void failedClientAuthentication(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 3, value = "Unexpected error when authenticating client")
        void errorAuthenticatingClient(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 4, value = "Unknown flow to execute with")
        void unknownFlow();

        @LogMessage(level = ERROR)
        @Message(id = 5, value = "Unknown result status")
        void unknownResultStatus();

        @LogMessage(level = WARN)
        @Message(id = 6, value = "Client %s doesn't have have authentication method configured. Fallback to %s")
        void authMethodFallback(String clientId, String expectedClientAuthType);

        @LogMessage(level = ERROR)
        @Message(id = 7, value = "Failed to send password reset email")
        void failedToSendPwdResetEmail(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 8, value = "Recaptcha failed")
        void recaptchaFailed(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 9, value = "Failed to send email")
        void failedToSendEmail(@Cause Exception e);

        @LogMessage(level = WARN)
        @Message(id = 10, value = "Untranslated protocol Error: %s so we return default SAML error")
        void untranslatedProtocol(String errorName);

        @LogMessage(level = WARN)
        @Message(id = 11, value = "Using deprecated 'directGrantsOnly' configuration in JSON representation. It will be removed in future versions")
        void usingDeprecatedDirectGrantsOnly();

        @LogMessage(level = ERROR)
        @Message(id = 12, value = "Response_mode 'query' not allowed for implicit or hybrid flow")
        void responseModeQueryNotAllowed();

        @LogMessage(level = WARN)
        @Message(id = 13, value = "Failed to logout client, continuing")
        void failedToLogoutClient(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 14, value = "failed to parse RestartLoginCookie")
        void failedToParseRestartLoginCookie(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 15, value = "Not found serialized context in clientSession under note '%s'")
        void notFoundSerializedCtxInClientSession(String noteKey);

        @LogMessage(level = ERROR)
        @Message(id = 16, value = "Flow not configured for identity provider '%s'")
        void flowNotConfigForIDP(String identityProviderAlias);

        @LogMessage(level = ERROR)
        @Message(id = 17, value = "Not found configured flow with ID '%s' for identity provider '%s'")
        void flowNotFoundForIDP(String flowId, String identityProviderAlias);

        @LogMessage(level = ERROR)
        @Message(id = 18, value = "required action doesn't match current required action")
        void reqdActionDoesNotMatch();

        @LogMessage(level = ERROR)
        @Message(id = 19, value = "Invalid key for email verification")
        void invalidKeyForEmailVerification();

        @LogMessage(level = ERROR)
        @Message(id = 20, value = "User session was null")
        void userSessionNull();

        @LogMessage(level = ERROR)
        @Message(id = 21, value = "Required action provider was null")
        void actionProviderNull();

        @LogMessage(level = ERROR)
        @Message(id = 22, value = "Could not get user profile from twitter.")
        void couldNotGetProfileFromTwitter(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 23, value = "Request validation failed.")
        void requestValidationFailed(@Cause Exception e);

        @LogMessage(level = ERROR)
        @Message(id = 24, value = "validation failed.")
        void validationFailed(@Cause Exception e);

    }

    @MessageLogger(projectCode = "KC-GROUP", length = 4)
    static interface Group {
    }

    @MessageLogger(projectCode = "KC-SESSION", length = 4)
    static interface Session {

        @LogMessage(level = ERROR)
        @Message(id = 1, value = "Client session is null")
        void clientSessionNull();

        @LogMessage(level = ERROR)
        @Message(id = 2, value = "Client model in client session is null")
        void clientModelNull();

        @LogMessage(level = WARN)
        @Message(id = 3, value = "%s (%s) is implementing the internal SPI %s. This SPI is internal and may change without notice")
        void spiMayChange(String factoryId, String factoryClass, String spiName);

        @LogMessage(level = WARN)
        @Message(id = 4, value = "Logout for client '%s' failed")
        void logoutFailed(@Cause IOException ioe, String clientId);

        @LogMessage(level = WARN)
        @Message(id = 5, value = "Failed to send revocation request")
        void failedToSendRevocation(@Cause IOException ioe);

        @LogMessage(level = WARN)
        @Message(id = 6, value = "Availability test failed for uri '%s'")
        void availabilityTestFailed(String managementUrl);

        @LogMessage(level = WARN)
        @Message(id = 7, value = "Role '%s' not available in realm")
        void roleNotInRealm(String offlineAccessRole);

        @LogMessage(level = ERROR)
        @Message(id = 8, value = "No valid user session.")
        void noValidUserSession();

        @LogMessage(level = ERROR)
        @Message(id = 9, value = "usersession in different state.")
        void userSessionInDifferentState();

        @LogMessage(level = ERROR)
        @Message(id = 10, value = "Can't finish SAML logout as there is no logout binding set.  Please configure the logout service url in the admin console for your client applications.")
        void canNotFinishSAMLLogout();

        @LogMessage(level = WARN)
        @Message(id = 11, value = "Failed to verify logout request")
        void failedToVerifyLogout();

        @LogMessage(level = WARN)
        @Message(id = 12, value = "admin request failed, not validated %s")
        void adminRequestNotValidated(String action);

        @LogMessage(level = WARN)
        @Message(id = 13, value = "admin request failed, expired token")
        void adminRequestExpiredToken();

        @LogMessage(level = WARN)
        @Message(id = 14, value = "Resource name does not match")
        void resourceNameDoesNotMatch();

        @LogMessage(level = WARN)
        @Message(id = 15, value = "Failed backchannel broker logout to: %s")
        void failedBackchannelBrokerLogout(String url);

        @LogMessage(level = WARN)
        @Message(id = 16, value = "Failed backchannel broker logout to: %s")
        void failedBackchannelBrokerLogoutWithCause(@Cause Exception e, String url);

        @LogMessage(level = WARN)
        @Message(id = 17, value = "failed to do backchannel logout for userSession")
        void failedToDoBackchannelLogoutForUserSession(@Cause Exception e);

        @LogMessage(level = WARN)
        @Message(id = 18, value = "Can't do backchannel logout. No SingleLogoutService POST Binding registered for client: %s")
        void failedToDoBackchannelLogoutNoPostBindingRegistered(String clientId);

        @LogMessage(level = WARN)
        @Message(id = 19, value = "failed to send saml logout")
        void failedToSendSamlLogout();

        @LogMessage(level = WARN)
        @Message(id = 20, value = "failed to send saml logout")
        void failedToSendSamlLogoutWithCause(@Cause Exception e);

        @LogMessage(level = WARN)
        @Message(id = 21, value = "Unknown saml response")
        void unknownSamlResponse();

        @LogMessage(level = WARN)
        @Message(id = 22, value = "UserSession is not tagged as logging out.")
        void userSessionIsNotTaggedAsLoggingOut();

    }

    @MessageLogger(projectCode = "KC-EVENT", length = 4)
    static interface Event {

        @LogMessage(level = ERROR)
        @Message(id = 1, value = "Admin Events enabled, but no event store provider configured")
        void noEventStoreProvider();

        @LogMessage(level = ERROR)
        @Message(id = 2, value = "Event listener '%s' registered, but provider not found")
        void providerNotFound(String id);

        @LogMessage(level = ERROR)
        @Message(id = 3, value = "Failed to save event")
        void failedToSaveEvent(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 4, value = "Failed to send type to %s")
        void failedToSendType(@Cause Throwable t, Object listener);

        @LogMessage(level = ERROR)
        @Message(id = 5, value = "Failed to send type mail")
        void failedToSendTypeMail(@Cause Exception e);
    }

    @MessageLogger(projectCode = "KC-IMPORT-EXPORT", length = 4)
    static interface ImportExport extends KeycloakLogger {

        @LogMessage(level = INFO)
        @Message(id = 1, value = "Not importing realm %s from %s.  It already exists.")
        void realmExists(String realmName, String from);

        @LogMessage(level = INFO)
        @Message(id = 2, value = "Imported realm %s from %s.")
        void importedRealm(String realmName, String from);

        @LogMessage(level = WARN)
        @Message(id = 3, value = "Unable to import realm %s from %s.")
        void unableToImportRealm(@Cause Throwable t, String realmName, String from);

        @LogMessage(level = INFO)
        @Message(id = 4, value = "Importing users from '%s'")
        void importingUsersFrom(Object from);

        @LogMessage(level = ERROR)
        @Message(id = 5, value = "Failed to load 'keycloak-add-user.json'")
        void failedToLoadUsers(@Cause Throwable t);

        @LogMessage(level = ERROR)
        @Message(id = 6, value = "Failed to add user %s to realm %s: realm not found")
        void addUserFailedRealmNotFound(String user, String realm);

        @LogMessage(level = INFO)
        @Message(id = 7, value = "Added user '%s' to realm '%s'")
        void addUserSuccess(String user, String realm);

        @LogMessage(level = ERROR)
        @Message(id = 8, value = "Failed to add user '%s' to realm '%s': user with username exists")
        void addUserFailedUserExists(String user, String realm);

        @LogMessage(level = ERROR)
        @Message(id = 9, value = "Failed to add user '%s' to realm '%s'")
        void addUserFailed(@Cause Throwable t, String user, String realm);

        @LogMessage(level = ERROR)
        @Message(id = 10, value = "Failed to delete '%s'")
        void failedToDeleteFile(String fileName);

        @LogMessage(level = INFO)
        @Message(id = 11, value = "Full model import requested. Strategy: %s")
        void fullModelImport(String strategy);

        @LogMessage(level = INFO)
        @Message(id = 12, value = "Import of realm '%s' requested. Strategy: %s")
        void realmImportRequested(String realmName, String strategy);

        @LogMessage(level = INFO)
        @Message(id = 13, value = "Import finished successfully")
        void importSuccess();

        @LogMessage(level = INFO)
        @Message(id = 14, value = "Full model export requested")
        void fullModelExportRequested();

        @LogMessage(level = INFO)
        @Message(id = 15, value = "Export of realm '%s' requested.")
        void realmExportRequested(String realmName);

        @LogMessage(level = INFO)
        @Message(id = 16, value = "Export finished successfully")
        void exportSuccess();

        @LogMessage(level = ERROR)
        @Message(id = 17, value = "Error overwriting %s")
        void overwriteError(@Cause Exception e, String name);

        @LogMessage(level = ERROR)
        @Message(id = 18, value = "Error creating %s")
        void creationError(@Cause Exception e, String name);

        @LogMessage(level = ERROR)
        @Message(id = 19, value = "Error importing roles")
        void roleImportError(@Cause Exception e);

        @LogMessage(level = INFO)
        @Message(id = 20, value = "Exporting into directory %s")
        void exportingIntoDir(String dirName);

        @LogMessage(level = INFO)
        @Message(id = 21, value = "Importing from directory %s")
        void importingFromDir(String dirName);

        @LogMessage(level = INFO)
        @Message(id = 22, value = "Exporting model into file %s")
        void exportingModelIntoFile(String fileName);

        @LogMessage(level = INFO)
        @Message(id = 23, value = "Exporting realm '%s' into file %s")
        void exportingRealmIntoFile(String realmName, String fileName);

        @LogMessage(level = INFO)
        @Message(id = 24, value = "Full importing from file %s")
        void fullImportingFromFile(String fileName);

        @LogMessage(level = INFO)
        @Message(id = 25, value = "Realm '%s' already exists. Import skipped")
        void realmAlreadyExistsSkipped(String realmName);

        @LogMessage(level = INFO)
        @Message(id = 26, value = "Realm '%s' already exists. Removing it before import")
        void realmAlreadyExistsRemoving(String realmName);

        @LogMessage(level = INFO)
        @Message(id = 27, value = "Realm '%s' imported")
        void realmImported(String realmName);

        @LogMessage(level = INFO)
        @Message(id = 28, value = "Realm '%s' - data exported")
        void realmDataExported(String realmName);

        @LogMessage(level = INFO)
        @Message(id = 29, value = "Users %s-%s exported")
        void usersExported(Integer pageStart, Integer pageEnd);
    }

    @MessageLogger(projectCode = "KC-MIGRATION", length = 4)
    static interface Migration extends KeycloakLogger {

        @LogMessage(level = ERROR)
        @Message(id = 1, value = "Failed to migrate datamodel")
        void migrationFailure(@Cause Throwable t);
    }

}
