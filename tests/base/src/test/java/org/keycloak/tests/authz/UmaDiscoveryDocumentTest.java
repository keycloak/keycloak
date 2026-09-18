/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.authz;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.authorization.config.UmaConfiguration;
import org.keycloak.authorization.config.UmaWellKnownProviderFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.tests.authz.config.DefaultAuthzServerConfig;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.Endpoints;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = DefaultAuthzServerConfig.class)
public class UmaDiscoveryDocumentTest extends AbstractAuthzTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(
                getClass().getResourceAsStream("/org/keycloak/tests/testrealm.json"), RealmRepresentation.class);
        if (realm.getEventsListeners() != null) {
            realm.setEventsListeners(realm.getEventsListeners().stream()
                    .filter(listener -> !"event-queue".equals(listener))
                    .toList());
        }
        testRealms.add(realm);
    }

    @Test
    public void testFetchDiscoveryDocument() {
        Client client = AdminClientUtil.createResteasyClient();
        String authServerRoot = oauth.getBaseUrl();
        int realmSegmentIndex = authServerRoot.indexOf("/realms/");
        if (realmSegmentIndex >= 0) {
            authServerRoot = authServerRoot.substring(0, realmSegmentIndex);
        }

        UriBuilder builder = UriBuilder.fromUri(authServerRoot);
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build("test", UmaWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        try (Response response = oidcDiscoveryTarget.request().get()) {
            assertEquals("no-cache, must-revalidate, no-transform, no-store", response.getHeaders().getFirst("Cache-Control"));

            UmaConfiguration configuration = response.readEntity(UmaConfiguration.class);

            Endpoints endpoints = oauth.newConfig().realm("test").getEndpoints();
            assertEquals(configuration.getAuthorizationEndpoint(), endpoints.getAuthorization());
            assertEquals(configuration.getTokenEndpoint(), endpoints.getToken());
            assertEquals(configuration.getJwksUri(), endpoints.getJwks());
            assertEquals(configuration.getIntrospectionEndpoint(), endpoints.getIntrospection());

            String registrationUri = UriBuilder
                    .fromUri(authServerRoot)
                    .path(RealmsResource.class).path(RealmsResource.class, "getRealmResource")
                    .build(adminClient.realm("test").toRepresentation().getRealm()).toString();

            assertEquals(registrationUri + "/authz/protection/permission", configuration.getPermissionEndpoint().toString());
            assertEquals(registrationUri + "/authz/protection/resource_set", configuration.getResourceRegistrationEndpoint().toString());
        }
    }

    @Test
    public void testFetchDiscoveryDocumentUsingFrontEndUrl() {
        RealmRepresentation test = adminClient.realm("test").toRepresentation();

        if (test.getAttributes() == null) {
            test.setAttributes(new HashMap<>());
        }

        final String frontendUrl = "https://mykeycloak/auth";

        test.getAttributes().put("frontendUrl", frontendUrl);

        adminClient.realm("test").update(test);

        Client client = AdminClientUtil.createResteasyClient();
        String authServerRoot = oauth.getBaseUrl();
        int realmSegmentIndex = authServerRoot.indexOf("/realms/");
        if (realmSegmentIndex >= 0) {
            authServerRoot = authServerRoot.substring(0, realmSegmentIndex);
        }

        UriBuilder builder = UriBuilder.fromUri(authServerRoot);
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build("test", UmaWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        try (Response response = oidcDiscoveryTarget.request().get()) {
            assertEquals("no-cache, must-revalidate, no-transform, no-store", response.getHeaders().getFirst("Cache-Control"));

            UmaConfiguration configuration = response.readEntity(UmaConfiguration.class);

            String baseBackendUri = UriBuilder
                    .fromUri(frontendUrl)
                    .path(RealmsResource.class).path(RealmsResource.class, "getRealmResource")
                    .build(adminClient.realm("test").toRepresentation().getRealm()).toString();
            String baseFrontendUri = UriBuilder
                    .fromUri(frontendUrl)
                    .path(RealmsResource.class).path(RealmsResource.class, "getRealmResource")
                    .scheme("https").host("mykeycloak").port(-1)
                    .build(adminClient.realm("test").toRepresentation().getRealm()).toString();

            assertEquals(baseBackendUri + "/authz/protection/permission", configuration.getPermissionEndpoint());
            assertEquals(baseBackendUri + "/authz/protection/permission", configuration.getPermissionEndpoint());
            assertEquals(baseFrontendUri + "/protocol/openid-connect/auth", configuration.getAuthorizationEndpoint());
        }
    }
}
