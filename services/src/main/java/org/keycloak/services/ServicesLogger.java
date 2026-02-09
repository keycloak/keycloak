/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.services;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import javax.naming.NamingException;

import org.keycloak.email.EmailException;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.models.ModelDuplicateException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Once;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.FATAL;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;


/**
 * Main logger for the Keycloak Services module.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@MessageLogger(projectCode="KC-SERVICES", length=4)
public interface ServicesLogger extends BasicLogger {

    ServicesLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), ServicesLogger.class, "org.keycloak.services");

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

    @LogMessage(level = WARN)
    @Message(id=13, value="Failed authentication")
    void failedAuthentication(@Cause Throwable t);

    @LogMessage(level = DEBUG)
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

    @LogMessage(level = WARN)
    @Message(id=19, value="No duplication detected.")
    void noDuplicationDetected();

    @LogMessage(level = WARN)
    @Message(id=20, value="%s is null. Reset flow and enforce showing reviewProfile page")
    void resetFlow(String emailOrUserName);

    @LogMessage(level = ERROR)
    @Message(id=21, value="Failed to send email to confirm identity broker linking")
    void confirmBrokerEmailFailed(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id=22, value="Key parameter don't match with the expected value from client session")
    void keyParamDoesNotMatch();

    @LogMessage(level = WARN)
    @Message(id=23, value="Smtp is not configured for the realm. Ignoring email verification authenticator")
    void smtpNotConfigured();

    @LogMessage(level = ERROR)
    @Message(id=24, value="")
    void modelDuplicateException(@Cause ModelDuplicateException mde);

    @LogMessage(level = ERROR)
    @Message(id=25, value="Error when validating client assertion")
    void errorValidatingAssertion(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id=26, value="Failed to send password reset email")
    void failedToSendPwdResetEmail(@Cause EmailException e);

    @LogMessage(level = ERROR)
    @Message(id=28, value="Recaptcha failed")
    void recaptchaFailed(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id=29, value="Failed to send email")
    void failedToSendEmail(@Cause Exception e);

    @LogMessage(level = INFO)
    @Message(id=30, value="Full model import requested. Strategy: %s")
    void fullModelImport(String strategy);

    @LogMessage(level = INFO)
    @Message(id=31, value="Import of realm '%s' requested. Strategy: %s")
    void realmImportRequested(String realmName, String strategy);

    @LogMessage(level = INFO)
    @Message(id=32, value="Import finished successfully")
    void importSuccess();

    @LogMessage(level = INFO)
    @Message(id=33, value="Full model export requested")
    void fullModelExportRequested();

    @LogMessage(level = INFO)
    @Message(id=34, value="Export of realm '%s' requested.")
    void realmExportRequested(String realmName);

    @LogMessage(level = INFO)
    @Message(id=35, value="Export finished successfully")
    void exportSuccess();

    @LogMessage(level = ERROR)
    @Message(id=36, value="Error overwriting %s")
    void overwriteError(@Cause Exception e, String name);

    @LogMessage(level = ERROR)
    @Message(id=37, value="Error creating %s")
    void creationError(@Cause Exception e, String name);

    @LogMessage(level = ERROR)
    @Message(id=38, value="Error importing roles")
    void roleImportError(@Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id=39, value="Untranslated protocol Error: %s so we return default SAML error")
    void untranslatedProtocol(String errorName);

    @LogMessage(level = WARN)
    @Message(id=40, value="Using deprecated 'directGrantsOnly' configuration in JSON representation. It will be removed in future versions")
    void usingDeprecatedDirectGrantsOnly();

    @LogMessage(level = WARN)
    @Message(id=41, value="Invoking deprecated endpoint %s")
    void invokingDeprecatedEndpoint(URI requestUri);

    @LogMessage(level = ERROR)
    @Message(id=42, value="Response_mode 'query' not allowed for implicit or hybrid flow")
    void responseModeQueryNotAllowed();

    @LogMessage(level = ERROR)
    @Message(id=43, value="Client session is null")
    void clientSessionNull();

    @LogMessage(level = ERROR)
    @Message(id=44, value="Client model in client session is null")
    void clientModelNull();

    @LogMessage(level = ERROR)
    @Message(id=45, value="Invalid token. Token verification failed.")
    void invalidToken();

    @LogMessage(level = WARN)
    @Message(id=46, value="Multiple values found '%s' for protocol mapper '%s' but expected just single value")
    void multipleValuesForMapper(String attrValue, String mapper);

    @LogMessage(level = WARN)
    @Message(id=47, value="%s (%s) is implementing the internal SPI %s. This SPI is internal and may change without notice")
    void spiMayChange(String factoryId, String factoryClass, String spiName);

    @LogMessage(level = ERROR)
    @Message(id=48, value="Exception during rollback")
    void exceptionDuringRollback(@Cause RuntimeException e);

    @LogMessage(level = ERROR)
    @Message(id=49, value="%s")
    void clientRegistrationException(String message);

    @LogMessage(level = INFO)
    @Message(id=50, value="Initializing %s realm")
    void initializingAdminRealm(String adminRealmName);

    @LogMessage(level = WARN)
    @Message(id=51, value="Failed to logout client, continuing")
    void failedToLogoutClient(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id=52, value="Failed processing type")
    void failedProcessingType(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id=54, value="Unknown action: %s")
    void unknownAction(String action);

    @LogMessage(level = ERROR)
    @Message(id=55, value="%s")
    void errorAuthenticating(@Cause Exception e, String message);

    @LogMessage(level = WARN)
    @Message(id=56, value="Error when closing LDAP connection")
    void errorClosingLDAP(@Cause NamingException ne);

    @LogMessage(level = WARN)
    @Message(id=57, value="Logout for client '%s' failed")
    void logoutFailed(@Cause IOException ioe, String clientId);

    @LogMessage(level = WARN)
    @Message(id=58, value="Failed to send revocation request")
    void failedToSendRevocation(@Cause IOException ioe);

    @LogMessage(level = WARN)
    @Message(id=59, value="Availability test failed for uri '%s'")
    void availabilityTestFailed(String managementUrl);

    @LogMessage(level = WARN)
    @Message(id=60, value="Role '%s' not available in realm")
    void roleNotInRealm(String offlineAccessRole);

    @LogMessage(level = ERROR)
    @Message(id=61, value="Error occurred during full sync of users")
    void errorDuringFullUserSync(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id=62, value="Error occurred during sync of changed users")
    void errorDuringChangedUserSync(@Cause Throwable t);

    @LogMessage(level = WARN)
    @Message(id=63, value="Failed to format message due to: %s")
    void failedToFormatMessage(String cause);

    @LogMessage(level = WARN)
    @Message(id=64, value="Failed to load messages")
    void failedToloadMessages(@Cause IOException ioe);

    @LogMessage(level = ERROR)
    @Message(id=65, value="Failed to update Password")
    void failedToUpdatePassword(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id=66, value="Could not fire event.")
    void couldNotFireEvent(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id=67, value="failed to parse RestartLoginCookie")
    void failedToParseRestartLoginCookie(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id=68, value="Not found serialized context in clientSession under note '%s'")
    void notFoundSerializedCtxInClientSession(String noteKey);

    @LogMessage(level = ERROR)
    @Message(id=69, value="Flow not configured for identity provider '%s'")
    void flowNotConfigForIDP(String identityProviderAlias);

    @LogMessage(level = ERROR)
    @Message(id=70, value="Not found configured flow with ID '%s' for identity provider '%s'")
    void flowNotFoundForIDP(String flowId, String identityProviderAlias);

    @LogMessage(level = ERROR)
    @Message(id=71, value="required action doesn't match current required action")
    void reqdActionDoesNotMatch();

    @LogMessage(level = ERROR)
    @Message(id=72, value="Invalid key for email verification")
    void invalidKeyForEmailVerification();

    @LogMessage(level = ERROR)
    @Message(id=73, value="User session was null")
    void userSessionNull();

    @LogMessage(level = ERROR)
    @Message(id=74, value="Required action provider was null")
    void actionProviderNull();

    @LogMessage(level = WARN)
    @Message(id=75, value="Failed to get theme request")
    void failedToGetThemeRequest(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id=76, value="Rejected non-local attempt to create initial user from %s")
    void rejectedNonLocalAttemptToCreateInitialUser(String remoteAddr);

    @LogMessage(level = INFO)
    @Message(id=77, value="Created temporary admin user with username %s")
    void createdTemporaryAdminUser(String userName);

    @LogMessage(level = INFO)
    @Message(id=78, value="Created temporary admin service account with client id %s")
    void createdTemporaryAdminService(String clientId);

    @LogMessage(level = WARN)
    @Message(id=79, value="Locale not specified for messages.json")
    void localeNotSpecified();

    @LogMessage(level = WARN)
    @Message(id=80, value="Message bundle not found for language code '%s'")
    void msgBundleNotFound(String lang);

    @LogMessage(level = FATAL)
    @Message(id=81, value="Message bundle not found for language code 'en'")
    void msgBundleNotFoundForEn();

    @LogMessage(level = ERROR)
    @Message(id=82, value="Admin Events enabled, but no event store provider configured")
    void noEventStoreProvider();

    @LogMessage(level = ERROR)
    @Message(id=83, value="Event listener '%s' registered, but provider not found")
    void providerNotFound(String id);

    @LogMessage(level = ERROR)
    @Message(id=84, value="Failed to save event")
    void failedToSaveEvent(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id=85, value="Failed to send type to %s")
    void failedToSendType(@Cause Throwable t, EventListenerProvider listener);

    @LogMessage(level = INFO)
    @Message(id=86, value="Added 'kerberos' to required realm credentials")
    void addedKerberosToRealmCredentials();

    @LogMessage(level = INFO)
    @Message(id=87, value="Syncing data for mapper '%s' of type '%s'. Direction: %s")
    void syncingDataForMapper(String modelName, String mapperType, String direction);

    @LogMessage(level = ERROR)
    @Message(id=88, value="Failed to send execute actions email")
    void failedToSendActionsEmail(@Cause EmailException e);

    @LogMessage(level = ERROR)
    @Message(id=89, value="Failed to run scheduled task %s")
    void failedToRunScheduledTask(@Cause Throwable t, String taskClass);

    @LogMessage(level = ERROR)
    @Message(id=90, value="Failed to close ProviderSession")
    void failedToCloseProviderSession(@Cause Throwable t);

    @LogMessage(level = DEBUG)
    @Message(id=91, value="Request is missing scope 'openid' so it's not treated as OIDC, but just pure OAuth2 request.")
    @Once
    void oidcScopeMissing();

    @LogMessage(level = ERROR)
    @Message(id=92, value="Missing parameter: %s")
    void missingParameter(String paramName);

    @LogMessage(level = ERROR)
    @Message(id=93, value="Invalid parameter value for: %s")
    void invalidParameter(String paramName);

    @LogMessage(level = ERROR)
    @Message(id=94, value="Unsupported parameter: %s")
    void unsupportedParameter(String paramName);

    @LogMessage(level = ERROR)
    @Message(id=95, value="Client is not allowed to initiate browser login with given response_type. %s flow is disabled for the client.")
    void flowNotAllowed(String flowName);

    @LogMessage(level = WARN)
    @Message(id=96, value="Not found JWK of supported keyType under jwks_uri for usage: %s")
    void supportedJwkNotFound(String usage);

    @LogMessage(level = WARN)
    @Message(id=97, value="Invalid request")
    void invalidRequest(@Cause Throwable t);


    @LogMessage(level = WARN)
    @Message(id=99, value="Operation '%s' rejected. %s")
    void clientRegistrationRequestRejected(String opDescription, String detailedMessage);

    @LogMessage(level = WARN)
    @Message(id=100, value= "ProtocolMapper '%s' of type '%s' not allowed")
    void clientRegistrationMapperNotAllowed(String mapperName, String mapperType);

    @LogMessage(level = WARN)
    @Message(id=101, value= "Failed to verify remote host : %s")
    void failedToVerifyRemoteHost(String hostname);

    @LogMessage(level = WARN)
    @Message(id=102, value= "URL '%s' doesn't match any trustedHost or trustedDomain")
    void urlDoesntMatch(String url);

    @LogMessage(level = DEBUG)
    @Message(id=103, value="Failed to reset password. User is temporarily disabled")
    void passwordResetFailed(@Cause Throwable t);

    @LogMessage(level = WARN)
    @Message(id=104, value="Not creating user %s. It already exists.")
    void notCreatingExistingUser(String userName);

    @LogMessage(level = ERROR)
    @Message(id=105, value="Response_mode 'query.jwt' is allowed only when the authorization response token is encrypted")
    void responseModeQueryJwtNotAllowed();

    @LogMessage(level = INFO)
    @Message(id=106, value="Created script engine '%s', version '%s' for the mime type '%s'")
    @Once
    void scriptEngineCreated(String engineName, String engineVersion, String mimeType);

    @LogMessage(level = DEBUG)
    @Message(id=107, value="Skipping create admin user. User(s) already exist in realm '%s'.")
    void addAdminUserFailedUsersExist(String realm);

    @LogMessage(level = WARN)
    @Message(id=108, value="URI '%s' doesn't match any trustedHost or trustedDomain")
    void uriDoesntMatch(String uri);

    @LogMessage(level = ERROR)
    @Message(id=109, value="Failed to add client '%s' to realm '%s': client with client ID exists")
    void addClientFailedClientExists(String clientId, String realm);

    @LogMessage(level = WARN)
    @Message(id=110, value="Environment variable '%s' is deprecated, use '%s' instead")
    void usingDeprecatedEnvironmentVariable(String deprecated, String supported);

    @LogMessage(level = INFO)
    @Message(id=111, value="Created initial admin user with username %s")
    void createdInitialAdminUser(String userName);

}
