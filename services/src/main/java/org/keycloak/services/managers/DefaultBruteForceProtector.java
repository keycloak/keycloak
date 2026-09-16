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
package org.keycloak.services.managers;


import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.http.FormPartValue;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.ReadOnlyException;

import org.jboss.logging.Logger;

import static org.keycloak.models.UserModel.DISABLED_REASON;

/**
 * A single thread will log failures.  This is so that we can avoid concurrent writes as we want an accurate failure count
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultBruteForceProtector implements BruteForceProtector {
    private static final Logger logger = Logger.getLogger(DefaultBruteForceProtector.class);

    public static final Set<String> ALLOWED_AUTHENTICATION_CATEGORIES = Set.of(
            PasswordCredentialModel.TYPE,
            OTPCredentialModel.TYPE,
            RecoveryAuthnCodesCredentialModel.TYPE
    );

    public static final String OTP_CATEGORY = OTPCredentialModel.TYPE;

    protected int maxDeltaTimeSeconds = 60 * 60 * 12; // 12 hours
    protected KeycloakSessionFactory factory;

    public DefaultBruteForceProtector(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    public void failure(KeycloakSession session, RealmModel realm, String userId, String remoteAddr, long failureTime, Set<String> categories) {
        failure(session, realm, userId, remoteAddr, failureTime, categories, null);
    }

    public void failure(KeycloakSession session, RealmModel realm, String userId, String remoteAddr, long failureTime,
            Set<String> categories, String attemptedIdentifier) {
        UserModel user = session.users().getUserById(realm, userId);
        List<String> failureKeys = user != null
                ? BruteForceUserProperty.getFailureKeysForAttempt(realm, user, attemptedIdentifier)
                : List.of(userId);
        for (String failureKey : failureKeys) {
            failure(session, realm, user, failureKey, remoteAddr, failureTime, categories);
        }
    }

    private void failure(KeycloakSession session, RealmModel realm, UserModel user, String failureKey,
            String remoteAddr, long failureTime, Set<String> categories) {
        UserLoginFailureModel userLoginFailure = getUserFailureModel(session, realm, failureKey);
        if (userLoginFailure == null) {
            userLoginFailure = session.loginFailures().addUserLoginFailure(realm, failureKey);
        }
        // Trigger the pessimistic lock and entity refresh before reading lastFailure,
        // so the delta-time check uses post-lock data and concurrent nodes cannot both
        // decide to clear based on the same stale value.
        userLoginFailure.setLastIPFailure(remoteAddr);
        long last = userLoginFailure.getLastFailure();
        long deltaTime = 0;
        if (last > 0) {
            deltaTime = failureTime - last;
        }

        if (!(realm.isPermanentLockout() && realm.getMaxTemporaryLockouts() == 0) && deltaTime > 0) {
            // if last failure was more than MAX_DELTA clear failures
            if (deltaTime > realm.getMaxDeltaTimeSeconds() * 1000L) {
                userLoginFailure.clearFailures();
            }
        }
        userLoginFailure.setLastIPFailure(remoteAddr);
        userLoginFailure.setLastFailure(failureTime);
        userLoginFailure.incrementFailures();
        logger.debugf("new num failures: %s", userLoginFailure.getNumFailures());

        long waitSeconds = 0L;
        int failureFactor = BruteForceUserProperty.getFailureFactor(realm, failureKey);
        if (!(realm.isPermanentLockout() && realm.getMaxTemporaryLockouts() == 0) && failureFactor > 0) {
            if (RealmRepresentation.BruteForceStrategy.MULTIPLE.equals(realm.getBruteForceStrategy())) {
                waitSeconds = realm.getWaitIncrementSeconds() *  ((long) userLoginFailure.getNumFailures() / failureFactor);
            } else {
                waitSeconds = realm.getWaitIncrementSeconds() * ((long) 1 + userLoginFailure.getNumFailures() - failureFactor);
            }
        }

        logger.debugv("waitSeconds: {0}", waitSeconds);
        logger.debugv("deltaTime: {0}", deltaTime);

        boolean quickLoginFailure = false;
        if (waitSeconds <= 0) {
            if (last > 0 && deltaTime < realm.getQuickLoginCheckMilliSeconds()) {
                logger.debugv("quick login, set min wait seconds");
                waitSeconds = realm.getMinimumQuickLoginWaitSeconds();
                quickLoginFailure = true;
            }
        }
        if (waitSeconds > 0) {
            if(!realm.isPermanentLockout() || realm.getMaxTemporaryLockouts() > 0) {
                waitSeconds = Math.min(realm.getMaxFailureWaitSeconds(), waitSeconds);
            }
            if (!quickLoginFailure) {
                userLoginFailure.incrementTemporaryLockouts();
            }
            if (quickLoginFailure || !realm.isPermanentLockout() || userLoginFailure.getNumTemporaryLockouts() <= realm.getMaxTemporaryLockouts()) {
                long notBefore = (failureTime / 1000) + waitSeconds;
                logger.debugv("set notBefore: {0}", notBefore);
                // Converting to int is workaround for the fact that "failedLoginNotBefore" is int in the model. Should be fine as user would be considered temporarily disabled with Integer.MAX_VALUE
                int notBeforeInt = notBefore > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) notBefore;
                userLoginFailure.setFailedLoginNotBefore(notBeforeInt);
                sendEvent(session, realm, user != null ? user.getId() : failureKey, userLoginFailure,
                        EventType.USER_DISABLED_BY_TEMPORARY_LOCKOUT);
            }
        }

        if (categories != null && categories.contains(OTP_CATEGORY)) {
            int maxSecondaryAuthFailures = realm.getMaxSecondaryAuthFailures();
            boolean lockoutEnabled = maxSecondaryAuthFailures > 0;
            userLoginFailure.incrementSecondaryAuthFailures();
            logger.debugv("new num secondaryAuthFailures: {0}", Integer.valueOf(userLoginFailure.getNumSecondaryAuthFailures()));
            if (lockoutEnabled && userLoginFailure.getNumSecondaryAuthFailures() > maxSecondaryAuthFailures) {
                // permanently lock user account anyway
                permanentUserLockOut(session, realm, user, userLoginFailure);
            }
        }

        if(!realm.isPermanentLockout()) {
            return;
        }

        if(userLoginFailure.getNumTemporaryLockouts() > realm.getMaxTemporaryLockouts() ||
                (realm.getMaxTemporaryLockouts() == 0 && userLoginFailure.getNumFailures() >= realm.getFailureFactor())) {
            permanentUserLockOut(session, realm, user, userLoginFailure);
        }
    }

    private void permanentUserLockOut(KeycloakSession session, RealmModel realm, UserModel user,
            UserLoginFailureModel userLoginFailure) {
        if (user == null) {
            return;
        }
        logger.debugv("user {0} locked permanently due to too many login attempts", user.getUsername());
        user.setEnabled(false);
        try {
            user.setSingleAttribute(DISABLED_REASON, DISABLED_BY_PERMANENT_LOCKOUT);
        }catch (ReadOnlyException e){
            logger.debug("Cannot set disabled reason on read only user");
        }
        // Send event
        sendEvent(session, realm, user.getId(), userLoginFailure, EventType.USER_DISABLED_BY_PERMANENT_LOCKOUT);
    }


    protected UserLoginFailureModel getUserFailureModel(KeycloakSession session, RealmModel realm, String userId) {
        if (realm == null) return null;
        return session.loginFailures().getUserLoginFailure(realm, userId);
    }

    protected void sendEvent(KeycloakSession session, RealmModel realm, String userId,
            UserLoginFailureModel userLoginFailure, EventType type) {
        EventBuilder builder = new EventBuilder(realm, session)
                .ipAddress(userLoginFailure.getLastIPFailure())
                .event(type)
                .detail(Details.REASON, "brute_force_attack detected")
                .detail(Details.NUM_FAILURES, String.valueOf(userLoginFailure.getNumFailures()))
                .user(userId);

        if (type == EventType.USER_DISABLED_BY_TEMPORARY_LOCKOUT) {
            long secondsSinceEpoch = userLoginFailure.getFailedLoginNotBefore();
            Instant instant = Instant.ofEpochSecond(secondsSinceEpoch);
            LocalDateTime timestamp = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            builder.detail(Details.NOT_BEFORE, timestamp.toString());
        }

        // Send event.
        builder.success();
    }

    public void shutdown() {}

    protected void success(KeycloakSession session, RealmModel realm, String userId, Set<String> categories) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debugv("user {0} successfully logged in:", user.getUsername());
        }
        for (String failureKey : BruteForceUserProperty.getFailureKeys(realm, user)) {
            UserLoginFailureModel userLoginFailure = getUserFailureModel(session, realm, failureKey);
            if (userLoginFailure != null) {
                if (categories != null && categories.contains(OTP_CATEGORY)) {
                    logger.debug("clearing primary and secondary (OTP) failures");
                    userLoginFailure.clearPrimaryAndSecondaryAuthFailures();
                } else {
                    logger.debug("clearing primary failures");
                    userLoginFailure.clearFailures();
                }
            }
        }
    }

    @Override
    public void failedLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, Set<String> authenticationCategories) {
        failedLogin(realm, user, clientConnection, uriInfo, authenticationCategories, null);
    }

    @Override
    public void failedLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo,
            Set<String> authenticationCategories, String attemptedIdentifier) {
        if (authenticationCategories != null && Collections.disjoint(ALLOWED_AUTHENTICATION_CATEGORIES, authenticationCategories)) {
            logger.debugf("'%s' authentication category not allowed for brute force", authenticationCategories);
            return;
        }
        processLogin(realm, user, clientConnection, uriInfo, false, authenticationCategories, attemptedIdentifier);
        // wait a minimum of seconds for type to process so that a hacker
        // cannot flood with failed logins and overwhelm the queue and not have notBefore updated to block next requests
        // todo failure HTTP responses should be queued via async HTTP
        //event.latch.await(5, TimeUnit.SECONDS);
        logger.trace("sent failure event");
    }

    @Override
    public void successfulLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, Set<String> authenticationCategories) {
        successfulLogin(realm, user, clientConnection, uriInfo, authenticationCategories, null);
    }

    @Override
    public void successfulLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo,
            Set<String> authenticationCategories, String attemptedIdentifier) {
        if (authenticationCategories == null || Collections.disjoint(ALLOWED_AUTHENTICATION_CATEGORIES, authenticationCategories)) {
            logger.debugf("'%s' authentication category not allowed for brute force", authenticationCategories);
            return;
        }
        processLogin(realm, user, clientConnection, uriInfo, true, authenticationCategories, attemptedIdentifier);
        logger.trace("sent success event");
    }

    protected void processLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, boolean success, Set<String> categories) {
        processLogin(realm, user, clientConnection, uriInfo, success, categories, null);
    }

    protected void processLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo,
            boolean success, Set<String> categories, String attemptedIdentifier) {
        ExecutorService executor = KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
            ExecutorsProvider provider = session.getProvider(ExecutorsProvider.class);
            return provider.getExecutor("bruteforce");
        });
        final HttpRequest bruteForceHttpRequest = new BruteForceHttpRequest(uriInfo);
        final HttpResponse bruteForceHttpResponse = new BruteForceHttpResponse();
        executor.execute(() -> KeycloakModelUtils.runJobInTransaction(factory, s -> {
            RealmModel currentRealm = s.realms().getRealm(realm.getId());
            s.getContext().setRealm(currentRealm);
            s.getContext().setHttpRequest(bruteForceHttpRequest);
            s.getContext().setHttpResponse(bruteForceHttpResponse);
            if (success) {
                success(s, currentRealm, user.getId(), categories);
            } else {
                failure(s, currentRealm, user.getId(), clientConnection.getRemoteHost(), Time.currentTimeMillis(),
                        categories, attemptedIdentifier);
            }
        }));
    }

    @Override
    public boolean isTemporarilyDisabled(KeycloakSession session, RealmModel realm, UserModel user) {
        for (String failureKey : BruteForceUserProperty.getFailureKeys(realm, user)) {
            UserLoginFailureModel userLoginFailure = getUserFailureModel(session, realm, failureKey);
            if (userLoginFailure == null) {
                continue;
            }
            long currTime = Time.currentTimeMillis() / 1000;
            int failedLoginNotBefore = userLoginFailure.getFailedLoginNotBefore();
            if (currTime < failedLoginNotBefore) {
                logger.debugv("Current: {0} notBefore: {1}", currTime, failedLoginNotBefore);
                return true;
            }
        }


        return false;
    }

    @Override
    public boolean isPermanentlyLockedOut(KeycloakSession session, RealmModel realm, UserModel user) {
        if (!user.isEnabled() && DISABLED_BY_PERMANENT_LOCKOUT.equals(user.getFirstAttribute(DISABLED_REASON))) {
            return true;
        }

        if (!realm.isPermanentLockout()) return false;

        // recheck failures just in case we are in a race
        return BruteForceUserProperty.getFailureKeys(realm, user).stream()
                .anyMatch(failureKey -> {
                    UserLoginFailureModel userLoginFailure = getUserFailureModel(session, realm, failureKey);
                    return userLoginFailure != null
                            && (userLoginFailure.getNumTemporaryLockouts() > realm.getMaxTemporaryLockouts()
                            || (realm.getMaxTemporaryLockouts() == 0
                            && userLoginFailure.getNumFailures() >= BruteForceUserProperty.getFailureFactor(realm, failureKey)));
                });
    }

    @Override
    public void cleanUpPermanentLockout(KeycloakSession session, RealmModel realm, UserModel user) {
        if (DISABLED_BY_PERMANENT_LOCKOUT.equals(user.getFirstAttribute(DISABLED_REASON)) || isPermanentlyLockedOut(session, realm, user)) {
            user.removeAttribute(DISABLED_REASON);

            if (!isTemporarilyDisabled(session, realm, user)) {
                BruteForceUserProperty.removeLoginFailures(session, realm, user);
            }
        }
    }

    @Override
    public void close() {}

    private static class BruteForceHttpRequest implements HttpRequest {

        private final UriInfo uriInfo;

        BruteForceHttpRequest(UriInfo uriInfo) {
            this.uriInfo = uriInfo;
        }

        @Override
        public String getHttpMethod() {
            return "";
        }

        @Override
        public MultivaluedMap<String, String> getDecodedFormParameters() {
            return new MultivaluedHashMap<>();
        }

        @Override
        public MultivaluedMap<String, FormPartValue> getMultiPartFormParameters() {
             return new MultivaluedHashMap<>();
        }

        @Override
        public HttpHeaders getHttpHeaders() {
            return null;
        }

        @Override
        public X509Certificate[] getClientCertificateChain() {
            return null;
        }

        @Override
        public UriInfo getUri() {
            return uriInfo;
        }

        @Override
        public boolean isProxyTrusted() {
            return true;
        }
    }

    private static class BruteForceHttpResponse implements HttpResponse {
        @Override
        public int getStatus() {
            return -1;
        }

        @Override
        public void setStatus(int statusCode) {
        }

        @Override
        public void addHeader(String name, String value) {
        }

        @Override
        public void setHeader(String name, String value) {
        }

        @Override
        public void setCookieIfAbsent(NewCookie cookie) {
        }
    }
}
