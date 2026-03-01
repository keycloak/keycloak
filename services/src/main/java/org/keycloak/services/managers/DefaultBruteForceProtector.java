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

    protected int maxDeltaTimeSeconds = 60 * 60 * 12; // 12 hours
    protected KeycloakSessionFactory factory;

    public DefaultBruteForceProtector(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    protected void failure(KeycloakSession session, RealmModel realm, String userId, String remoteAddr, long failureTime) {
        logger.debug("failure");

        UserLoginFailureModel userLoginFailure = getUserFailureModel(session, realm, userId);
        if (userLoginFailure == null) {
            userLoginFailure = session.loginFailures().addUserLoginFailure(realm, userId);
        }
        userLoginFailure.setLastIPFailure(remoteAddr);
        long last = userLoginFailure.getLastFailure();
        long deltaTime = 0;
        if (last > 0) {
            deltaTime = failureTime - last;
        }
        userLoginFailure.setLastFailure(failureTime);

        if (!(realm.isPermanentLockout() && realm.getMaxTemporaryLockouts() == 0) && deltaTime > 0) {
            // if last failure was more than MAX_DELTA clear failures
            if (deltaTime > (long) realm.getMaxDeltaTimeSeconds() * 1000L) {
                userLoginFailure.clearFailures();
            }
        }
        userLoginFailure.incrementFailures();
        logger.debugv("new num failures: {0}", userLoginFailure.getNumFailures());

        long waitSeconds = 0L;
        if (!(realm.isPermanentLockout() && realm.getMaxTemporaryLockouts() == 0)) {
            if (RealmRepresentation.BruteForceStrategy.MULTIPLE.equals(realm.getBruteForceStrategy())) {
                waitSeconds = (long) realm.getWaitIncrementSeconds() *  ((long) userLoginFailure.getNumFailures() / realm.getFailureFactor());
            } else {
                waitSeconds = (long) realm.getWaitIncrementSeconds() * ((long) 1 + userLoginFailure.getNumFailures() - realm.getFailureFactor());
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
                sendEvent(session, realm, userLoginFailure, EventType.USER_DISABLED_BY_TEMPORARY_LOCKOUT);
            }
        }

        if(!realm.isPermanentLockout()) {
            return;
        }

        if(userLoginFailure.getNumTemporaryLockouts() > realm.getMaxTemporaryLockouts() ||
                (realm.getMaxTemporaryLockouts() == 0 && userLoginFailure.getNumFailures() >= realm.getFailureFactor())) {
            UserModel user = session.users().getUserById(realm, userId);
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
            sendEvent(session, realm, userLoginFailure, EventType.USER_DISABLED_BY_PERMANENT_LOCKOUT);
        }
    }

    protected UserLoginFailureModel getUserFailureModel(KeycloakSession session, RealmModel realm, String userId) {
        if (realm == null) return null;
        return session.loginFailures().getUserLoginFailure(realm, userId);
    }

    protected void sendEvent(KeycloakSession session, RealmModel realm, UserLoginFailureModel userLoginFailure, EventType type) {
        EventBuilder builder = new EventBuilder(realm, session)
                .ipAddress(userLoginFailure.getLastIPFailure())
                .event(type)
                .detail(Details.REASON, "brute_force_attack detected")
                .detail(Details.NUM_FAILURES, String.valueOf(userLoginFailure.getNumFailures()))
                .user(userLoginFailure.getUserId());

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

    protected void success(KeycloakSession session, RealmModel realm, String userId) {
        UserLoginFailureModel userLoginFailure = getUserFailureModel(session, realm, userId);
        if(userLoginFailure == null) return;
        if (logger.isDebugEnabled()) {
            UserModel model = session.users().getUserById(realm, userId);
            logger.debugv("user {0} successfully logged in, clearing all failures", model.getUsername());
        }
        userLoginFailure.clearFailures();
    }

    @Override
    public void failedLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo) {
        processLogin(realm, user, clientConnection, uriInfo, false);
        // wait a minimum of seconds for type to process so that a hacker
        // cannot flood with failed logins and overwhelm the queue and not have notBefore updated to block next requests
        // todo failure HTTP responses should be queued via async HTTP
        //event.latch.await(5, TimeUnit.SECONDS);
        logger.trace("sent failure event");
    }

    @Override
    public void successfulLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo) {
        processLogin(realm, user, clientConnection, uriInfo, true);
        logger.trace("sent success event");
    }

    protected void processLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, boolean success) {
        ExecutorService executor = KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
            ExecutorsProvider provider = session.getProvider(ExecutorsProvider.class);
            return provider.getExecutor("bruteforce");
        });
        final HttpRequest bruteForceHttpRequest = new BruteForceHttpRequest(uriInfo);
        final HttpResponse bruteForceHttpResponse = new BruteForceHttpResponse();
        executor.execute(() -> KeycloakModelUtils.runJobInTransaction(factory, s -> {
            s.getContext().setRealm(s.realms().getRealm(realm.getId()));
            s.getContext().setHttpRequest(bruteForceHttpRequest);
            s.getContext().setHttpResponse(bruteForceHttpResponse);
            if (success) {
                success(s, realm, user.getId());
            } else {
                failure(s, realm, user.getId(), clientConnection.getRemoteHost(), Time.currentTimeMillis());
            }
        }));
    }

    @Override
    public boolean isTemporarilyDisabled(KeycloakSession session, RealmModel realm, UserModel user) {
        UserLoginFailureModel userLoginFailure = getUserFailureModel(session, realm, user.getId());

        if (userLoginFailure != null) {
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
        UserLoginFailureModel userLoginFailure = getUserFailureModel(session, realm, user.getId());
        return userLoginFailure != null && userLoginFailure.getNumTemporaryLockouts() > realm.getMaxTemporaryLockouts();
    }

    @Override
    public void cleanUpPermanentLockout(KeycloakSession session, RealmModel realm, UserModel user) {
        if (DISABLED_BY_PERMANENT_LOCKOUT.equals(user.getFirstAttribute(DISABLED_REASON)) || isPermanentlyLockedOut(session, realm, user)) {
            user.removeAttribute(DISABLED_REASON);

            if (!isTemporarilyDisabled(session, realm, user)) {
                session.loginFailures().removeUserLoginFailure(realm, user.getId());
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
