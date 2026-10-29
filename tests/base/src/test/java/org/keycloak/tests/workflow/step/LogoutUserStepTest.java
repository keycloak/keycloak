/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.workflow.step;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.workflow.LogoutUserStepProvider;
import org.keycloak.models.workflow.LogoutUserStepProviderFactory;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.events.UserAuthenticatedWorkflowEventFactory;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class LogoutUserStepTest extends AbstractWorkflowTest {

    private static final String BACKCHANNEL_LOGOUT_PATH = "/backchannel-logout";
    private static final String OTHER_CLIENT_ID = "other-app";
    private static final String SAML_LOGOUT_PATH = "/saml-logout";
    private static final String SAML_CLIENT_ID = "saml-app";
    private static final String SAML_IDP_ALIAS = "saml-idp";

    @InjectTimeOffSet
    private TimeOffSet timeOffSet;

    @InjectHttpServer
    private HttpServer httpServer;

    @InjectKeycloakUrls
    private KeycloakUrls keycloakUrls;

    @InjectUser(ref = "alice", config = AliceUserConfig.class, lifecycle = LifeCycle.METHOD, realmRef = DEFAULT_REALM_NAME)
    private ManagedUser userAlice;

    @Test
    public void testLogoutUser() {
        createLogoutWorkflow(null);
        oauth.scope(null);
        AccessTokenResponse tokens = login();

        UserResource user = managedRealm.admin().users().get(userAlice.getId());
        assertThat(user.getUserSessions(), hasSize(1));

        runScheduledSteps(Duration.ofMinutes(2));

        assertThat(user.getUserSessions(), empty());
        assertThat(user.toRepresentation().getNotBefore(), greaterThan(0));
        assertRefreshFails(tokens);
    }

    @Test
    public void testBackchannelLogoutIsSentToClient() {
        BlockingQueue<LogoutToken> logoutTokens = receiveBackchannelLogouts(oauth.getClientId());

        try {
            createLogoutWorkflow(null);
            oauth.scope(null);
            login();

            runScheduledSteps(Duration.ofMinutes(2));

            LogoutToken logoutToken = logoutTokens.poll();
            assertThat(logoutToken, notNullValue());
            assertThat(logoutToken.getSubject(), is(userAlice.getId()));
        } finally {
            httpServer.removeContext(BACKCHANNEL_LOGOUT_PATH);
        }
    }

    @Test
    public void testBackchannelLogoutIsSentToRelativeUrl() {
        BlockingQueue<LogoutToken> logoutTokens = receiveBackchannelLogouts(oauth.getClientId());

        try {
            createLogoutWorkflow(null);
            oauth.scope(null);
            login();

            ClientRepresentation client = managedRealm.admin().clients().findByClientId(oauth.getClientId()).get(0);
            client.setRootUrl(getHttpServerUrl());
            client.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, BACKCHANNEL_LOGOUT_PATH);
            managedRealm.admin().clients().get(client.getId()).update(client);

            runScheduledSteps(Duration.ofMinutes(2));

            LogoutToken logoutToken = logoutTokens.poll();
            assertThat(logoutToken, notNullValue());
            assertThat(logoutToken.getSubject(), is(userAlice.getId()));
        } finally {
            httpServer.removeContext(BACKCHANNEL_LOGOUT_PATH);
        }
    }

    @Test
    public void testBackchannelLogoutIncludingOfflineSessionsIsSentToAllClients() {
        managedRealm.admin().clients().create(ClientBuilder.create()
                .clientId(OTHER_CLIENT_ID)
                .frontchannelLogout(false)
                .build()).close();
        BlockingQueue<LogoutToken> logoutTokens = receiveBackchannelLogouts(oauth.getClientId(), OTHER_CLIENT_ID);

        try {
            createLogoutWorkflow("true");
            oauth.scope(null);
            login();

            // both clients share the login session, but only the test client also has an offline session
            String username = userAlice.getUsername();
            String clientId = oauth.getClientId();
            runOnServer.run((RunOnServer) session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, username);
                UserSessionModel userSession = session.sessions().getUserSessionsStream(realm, user).findFirst().orElseThrow();
                new UserSessionManager(session).createOrUpdateOfflineSession(
                        userSession.getAuthenticatedClientSessionByClient(realm.getClientByClientId(clientId).getId()), userSession);
                session.sessions().createClientSession(realm, realm.getClientByClientId(OTHER_CLIENT_ID), userSession)
                        .setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            });
            assertThat(managedRealm.admin().users().get(userAlice.getId()).getOfflineSessions(getClientUuid()), hasSize(1));

            runScheduledSteps(Duration.ofMinutes(2));

            assertThat(logoutTokens.stream().anyMatch(token -> token.hasAudience(OTHER_CLIENT_ID)), is(true));
        } finally {
            httpServer.removeContext(BACKCHANNEL_LOGOUT_PATH);
        }
    }

    @Test
    public void testSamlBackchannelLogoutIsSentToClient() {
        managedRealm.admin().clients().create(ClientBuilder.create()
                .clientId(SAML_CLIENT_ID)
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .frontchannelLogout(false)
                .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, getHttpServerUrl() + SAML_LOGOUT_PATH)
                .build()).close();
        BlockingQueue<LogoutRequestType> logoutRequests = receiveSamlLogouts();

        try {
            createLogoutWorkflow(null);
            oauth.scope(null);
            login();

            String username = userAlice.getUsername();
            runOnServer.run((RunOnServer) session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, username);
                UserSessionModel userSession = session.sessions().getUserSessionsStream(realm, user).findFirst().orElseThrow();
                AuthenticatedClientSessionModel clientSession = session.sessions()
                        .createClientSession(realm, realm.getClientByClientId(SAML_CLIENT_ID), userSession);
                clientSession.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
                clientSession.setNote(SamlProtocol.SAML_NAME_ID, username);
                clientSession.setNote(SamlProtocol.SAML_NAME_ID_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get());
            });
            // the SAML issuer is the realm URL, which is only known without a request when the hostname is a full URL
            managedRealm.updateWithCleanup(realm -> realm.attribute("frontendUrl", keycloakUrls.getBase()));

            runScheduledSteps(Duration.ofMinutes(2));

            LogoutRequestType logoutRequest = logoutRequests.poll();
            assertThat(logoutRequest, notNullValue());
            assertThat(logoutRequest.getIssuer().getValue(), is(keycloakUrls.getBase() + "/realms/" + managedRealm.getName()));
            assertThat(logoutRequest.getNameID().getValue(), is(username));
        } finally {
            httpServer.removeContext(SAML_LOGOUT_PATH);
        }
    }

    @Test
    public void testSamlBackchannelLogoutIsSentToIdentityProvider() {
        managedRealm.admin().identityProviders().create(IdentityProviderBuilder.create()
                .alias(SAML_IDP_ALIAS)
                .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                .attribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL, getHttpServerUrl() + SAML_LOGOUT_PATH)
                .attribute(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, "true")
                .build()).close();
        BlockingQueue<LogoutRequestType> logoutRequests = receiveSamlLogouts();

        try {
            createLogoutWorkflow(null);
            oauth.scope(null);
            login();

            String username = userAlice.getUsername();
            runOnServer.run((RunOnServer) session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, username);
                UserSessionModel userSession = session.sessions().getUserSessionsStream(realm, user).findFirst().orElseThrow();
                NameIDType nameId = new NameIDType();
                nameId.setValue(username);
                userSession.setNote(Details.IDENTITY_PROVIDER, SAML_IDP_ALIAS);
                userSession.setNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT_NAMEID, nameId.serializeAsString());
                userSession.setNote(SAMLEndpoint.SAML_FEDERATED_SESSION_INDEX, "session-index");
            });
            managedRealm.updateWithCleanup(realm -> realm.attribute("frontendUrl", keycloakUrls.getBase()));

            runScheduledSteps(Duration.ofMinutes(2));

            LogoutRequestType logoutRequest = logoutRequests.poll();
            assertThat(logoutRequest, notNullValue());
            assertThat(logoutRequest.getIssuer().getValue(), is(keycloakUrls.getBase() + "/realms/" + managedRealm.getName()));
            assertThat(logoutRequest.getNameID().getValue(), is(username));
        } finally {
            httpServer.removeContext(SAML_LOGOUT_PATH);
        }
    }

    @Test
    public void testOfflineSessionsAreKeptByDefault() {
        createLogoutWorkflow(null);
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokens = login();

        UserResource user = managedRealm.admin().users().get(userAlice.getId());
        assertThat(user.getOfflineSessions(getClientUuid()), hasSize(1));

        // keep the offset while the step runs, so the tokens are older than the user's not-before time
        timeOffSet.set(Math.toIntExact(Duration.ofMinutes(2).toSeconds()));
        runOnServer.run((RunOnServer) session -> session.getProvider(WorkflowProvider.class).runScheduledSteps());

        // the session record stays, but the offline token is rejected because of the user's not-before time
        assertThat(user.getUserSessions(), empty());
        assertThat(user.getOfflineSessions(getClientUuid()), hasSize(1));
        assertThat(user.toRepresentation().getNotBefore(), greaterThan(0));
        assertRefreshFails(tokens);
    }

    @Test
    public void testLogoutUserIncludingOfflineSessions() {
        createLogoutWorkflow("true");
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokens = login();

        UserResource user = managedRealm.admin().users().get(userAlice.getId());
        assertThat(user.getOfflineSessions(getClientUuid()), hasSize(1));

        runScheduledSteps(Duration.ofMinutes(2));

        assertThat(user.getUserSessions(), empty());
        assertThat(user.getOfflineSessions(getClientUuid()), empty());
        assertRefreshFails(tokens);
    }

    @Test
    public void testCreateFailsWhenIncludeOfflineSessionsIsNotBoolean() {
        try (Response response = managedRealm.admin().workflows().create(WorkflowRepresentation.withName("logout-bad")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .withSteps(
                        WorkflowStepRepresentation.create().of(LogoutUserStepProviderFactory.ID)
                                .withConfig(LogoutUserStepProvider.CONFIG_INCLUDE_OFFLINE_SESSIONS, "yes")
                                .build()
                ).build())) {
            assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    private void createLogoutWorkflow(String includeOfflineSessions) {
        WorkflowStepRepresentation.Builder step = WorkflowStepRepresentation.create()
                .of(LogoutUserStepProviderFactory.ID)
                .after(Duration.ofMinutes(1));

        if (includeOfflineSessions != null) {
            step.withConfig(LogoutUserStepProvider.CONFIG_INCLUDE_OFFLINE_SESSIONS, includeOfflineSessions);
        }

        create(WorkflowRepresentation.withName("logout")
                .onEvent(UserAuthenticatedWorkflowEventFactory.ID)
                .withSteps(step.build())
                .build());
    }

    private AccessTokenResponse login() {
        String code = oauth.doLogin(userAlice.getUsername(), userAlice.getPassword()).getCode();
        AccessTokenResponse tokens = oauth.doAccessTokenRequest(code);
        assertThat(tokens.getStatusCode(), is(Response.Status.OK.getStatusCode()));
        return tokens;
    }

    private void assertRefreshFails(AccessTokenResponse tokens) {
        AccessTokenResponse refreshed = oauth.doRefreshTokenRequest(tokens.getRefreshToken());
        assertThat(refreshed.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(refreshed.getError(), is(OAuthErrorException.INVALID_GRANT));
    }

    private BlockingQueue<LogoutToken> receiveBackchannelLogouts(String... clientIds) {
        BlockingQueue<LogoutToken> logoutTokens = new LinkedBlockingQueue<>();
        httpServer.createContext(BACKCHANNEL_LOGOUT_PATH, exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                logoutTokens.add(new JWSInput(body.substring("logout_token=".length())).readJsonContent(LogoutToken.class));
            } catch (JWSInputException e) {
                throw new IOException(e);
            }
            HttpServerUtil.sendResponse(exchange, 200, Map.of());
        });

        String backchannelLogoutUrl = getHttpServerUrl() + BACKCHANNEL_LOGOUT_PATH;
        for (String clientId : clientIds) {
            ClientRepresentation client = managedRealm.admin().clients().findByClientId(clientId).get(0);
            client.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, backchannelLogoutUrl);
            managedRealm.admin().clients().get(client.getId()).update(client);
        }

        return logoutTokens;
    }

    private BlockingQueue<LogoutRequestType> receiveSamlLogouts() {
        BlockingQueue<LogoutRequestType> logoutRequests = new LinkedBlockingQueue<>();
        httpServer.createContext(SAML_LOGOUT_PATH, exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String samlRequest = Arrays.stream(body.split("&"))
                    .filter(param -> param.startsWith(GeneralConstants.SAML_REQUEST_KEY + "="))
                    .map(param -> URLDecoder.decode(param.substring(GeneralConstants.SAML_REQUEST_KEY.length() + 1), StandardCharsets.UTF_8))
                    .findFirst()
                    .orElseThrow();
            logoutRequests.add((LogoutRequestType) SAMLRequestParser.parseRequestPostBinding(samlRequest).getSamlObject());
            HttpServerUtil.sendResponse(exchange, 200, Map.of());
        });
        return logoutRequests;
    }

    private String getHttpServerUrl() {
        return "http://" + httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort();
    }

    private String getClientUuid() {
        return managedRealm.admin().clients().findByClientId(oauth.getClientId()).get(0).getId();
    }

    private static class AliceUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("alice").password("alice").name("alice", "alice").email("alice@example.org");
        }
    }
}
