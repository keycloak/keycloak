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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
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
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.authz.services.Permission;
import org.keycloak.tests.authz.services.config.DefaultAuthzServicesServerConfig;
import org.keycloak.tests.authz.services.config.DefaultResourceServerConfig;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = DefaultAuthzServicesServerConfig.class)
public class UmaDisabledResourceServerTest {

    private static final String RESOURCE_NAME = "disabled-resource-server-resource";

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "resource-server-foo")
    ManagedClient resourceServer;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "requesting-client")
    ManagedClient requestingClient;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void disabledResourceServerRejectsFreshRpt() {
        Permission.create(resourceServer)
                .resource(RESOURCE_NAME)
                .grant();

        ResourceRepresentation resource = resourceServer.admin()
                .authorization()
                .resources()
                .findByName(RESOURCE_NAME)
                .get(0);

        PermissionResponse ticket = authzClient(resourceServer)
                .protection()
                .permission()
                .create(new PermissionRequest(resource.getId()));

        AccessTokenResponse requesterToken = oauth
                .client(requestingClient.getClientId(), requestingClient.getSecret())
                .doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertNotNull(requesterToken.getAccessToken());

        resourceServer.updateWithCleanup(client -> client.enabled(false));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authzClient(requestingClient)
                .authorization(requesterToken.getAccessToken())
                .authorize(new AuthorizationRequest(ticket.getTicket())),
                "A disabled resource server must not receive a fresh RPT");
        HttpResponseException response = assertInstanceOf(HttpResponseException.class, exception.getCause());
        assertEquals(400, response.getStatusCode());
        assertTrue(new String(response.getBytes(), StandardCharsets.UTF_8)
                .contains("Resource server client is disabled: [" + resourceServer.getClientId() + "]"));
    }

    private AuthzClient authzClient(ManagedClient client) {
        return AuthzClient.create(new Configuration(
                keycloakUrls.getBase(),
                realm.getName(),
                client.getClientId(),
                Map.of("secret", client.getSecret()),
                httpClient
        ));
    }
}
