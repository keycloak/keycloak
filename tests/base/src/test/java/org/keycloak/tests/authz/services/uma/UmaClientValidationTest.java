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
package org.keycloak.tests.authz.services.uma;

import java.io.IOException;
import java.util.Map;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.authz.services.Permission;
import org.keycloak.tests.authz.services.config.DefaultAuthzServicesServerConfig;
import org.keycloak.tests.authz.services.config.DefaultResourceServerConfig;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = DefaultAuthzServicesServerConfig.class)
public class UmaClientValidationTest {

    private static final String RESOURCE_NAME = "client-check-resource";

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "resource-server")
    ManagedClient resourceServer;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "requesting-client")
    ManagedClient requestingClient;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.authz.services.uma")
    RunOnServerClient runOnServer;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void rejectNonOidcRequestingClient(boolean bearerToken) throws IOException {
        AuthorizationRequest request = createGrantedRequest();
        assertNotNull(authorize(request, bearerToken).getToken());

        requestingClient.updateWithCleanup(client -> client.protocol(SamlProtocol.LOGIN_PROTOCOL));

        assertRejected(request, bearerToken, "invalid_client", "Wrong client protocol.");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void rejectBearerOnlyRequestingClient(boolean bearerToken) throws IOException {
        AuthorizationRequest request = createGrantedRequest();
        assertNotNull(authorize(request, bearerToken).getToken());

        requestingClient.updateWithCleanup(client -> client.bearerOnly(true));

        assertRejected(request, bearerToken, "invalid_client", "Bearer-only not allowed");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void rejectNonOidcResourceServer(boolean bearerToken) throws IOException {
        AuthorizationRequest request = createGrantedRequest();
        assertNotNull(authorize(request, bearerToken).getToken());

        resourceServer.updateWithCleanup(client -> client.protocol(SamlProtocol.LOGIN_PROTOCOL));

        assertRejected(request, bearerToken, "invalid_request",
                "Resource server client does not use the OpenID Connect protocol: [" + resourceServer.getClientId() + "]");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void allowUnspecifiedClientProtocols(boolean bearerToken) {
        AuthorizationRequest request = createGrantedRequest();
        // Admin REST treats a null protocol as an omitted update; set it directly to exercise legacy clients.
        runOnServer.run(new ClearClientProtocols(realm.getId(), requestingClient.getId(), resourceServer.getId()));

        assertNotNull(authorize(request, bearerToken).getToken());
    }

    private AuthorizationRequest createGrantedRequest() {
        Permission.create(resourceServer).resource(RESOURCE_NAME).grant();
        ResourceRepresentation resource = resourceServer.admin().authorization().resources().findByName(RESOURCE_NAME).get(0);
        String ticket = authzClient(resourceServer).protection().permission()
                .create(new PermissionRequest(resource.getId())).getTicket();

        AccessTokenResponse token = oauth.client(requestingClient.getClientId(), requestingClient.getSecret())
                .doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertEquals(200, token.getStatusCode());
        assertNotNull(token.getAccessToken());

        AuthorizationRequest request = new AuthorizationRequest(ticket);
        request.setSubjectToken(token.getAccessToken());
        return request;
    }

    private AuthorizationResponse authorize(AuthorizationRequest request, boolean bearerToken) {
        AuthzClient client = authzClient(requestingClient);
        return (bearerToken ? client.authorization(request.getSubjectToken()) : client.authorization()).authorize(request);
    }

    private void assertRejected(AuthorizationRequest request, boolean bearerToken, String error, String description) throws IOException {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authorize(request, bearerToken),
                "UMA token issuance must reject an invalid client configuration");
        HttpResponseException response = assertInstanceOf(HttpResponseException.class, exception.getCause());
        assertEquals(400, response.getStatusCode());
        OAuth2ErrorRepresentation body = JsonSerialization.readValue(response.getBytes(), OAuth2ErrorRepresentation.class);
        assertEquals(error, body.getError());
        assertEquals(description, body.getErrorDescription());
    }

    private AuthzClient authzClient(ManagedClient client) {
        return AuthzClient.create(new Configuration(keycloakUrls.getBase(), realm.getName(), client.getClientId(),
                Map.of("secret", client.getSecret()), httpClient));
    }

    private record ClearClientProtocols(String realmId, String requestingClientId, String resourceServerId) implements RunOnServer {

        @Override
        public void run(KeycloakSession session) {
            var realmModel = session.realms().getRealm(realmId);
            realmModel.getClientById(requestingClientId).setProtocol(null);
            realmModel.getClientById(resourceServerId).setProtocol(null);
        }
    }
}
