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

package org.keycloak.tests.admin.identityprovider;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = IdentityProviderKubernetesTest.TestServerConfig.class)
public class IdentityProviderKubernetesTest extends AbstractIdentityProviderTest {

    private static final String DEFAULT_KUBERNETES_ISSUER = "https://kubernetes.default.svc.cluster.local";

    @Test
    public void testCreateIdentityProviderUsesDefaultIssuer() {
        IdentityProviderRepresentation identityProvider = createRep("kubernetes", "kubernetes");
        identityProvider.getConfig().remove("issuer");

        try (Response response = managedRealm.admin().identityProviders().create(identityProvider)) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
        }
        managedRealm.cleanup().add(r -> r.identityProviders().get("kubernetes").remove());

        IdentityProviderRepresentation created = managedRealm.admin().identityProviders().get("kubernetes").toRepresentation();
        assertEquals(DEFAULT_KUBERNETES_ISSUER, created.getConfig().get("issuer"));
    }

    @Test
    public void testUpdateIdentityProviderUsesDefaultIssuer() {
        IdentityProviderRepresentation identityProvider = createRep("kubernetes", "kubernetes");
        identityProvider.getConfig().put("issuer", "https://localhost");

        try (Response response = managedRealm.admin().identityProviders().create(identityProvider)) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
        }
        managedRealm.cleanup().add(r -> r.identityProviders().get("kubernetes").remove());

        IdentityProviderResource idpResource = managedRealm.admin().identityProviders().get("kubernetes");
        identityProvider = idpResource.toRepresentation();
        identityProvider.getConfig().remove("issuer");
        idpResource.update(identityProvider);

        IdentityProviderRepresentation updated = idpResource.toRepresentation();
        assertEquals(DEFAULT_KUBERNETES_ISSUER, updated.getConfig().get("issuer"));
    }

    @Test
    public void testCreateIdentityProviderWithDuplicateDefaultIssuer() {
        IdentityProviderRepresentation identityProvider = createRep("kubernetes1", "kubernetes");
        identityProvider.getConfig().remove("issuer");

        try (Response response = managedRealm.admin().identityProviders().create(identityProvider)) {
            assertEquals(201, response.getStatus(), () -> response.readEntity(String.class));
        }
        managedRealm.cleanup().add(r -> r.identityProviders().get("kubernetes1").remove());

        identityProvider.setAlias("kubernetes2");
        try (Response response = managedRealm.admin().identityProviders().create(identityProvider)) {
            Assertions.assertEquals(400, response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("Issuer URL already used for IDP 'kubernetes1', Issuer must be unique if the idp supports JWT Authorization Grant or Federated Client Authentication", error.getErrorMessage());
        }
    }

    @Test
    public void testCreateIdentityProviderWithDuplicateIssuer() {
        IdentityProviderRepresentation identityProvider = createRep("kubernetes1", "kubernetes");
        identityProvider.getConfig().put("issuer", "https://localhost");

        try (Response response = managedRealm.admin().identityProviders().create(identityProvider)) {
            Assertions.assertEquals(201, response.getStatus());
        }

        managedRealm.cleanup().add(r -> r.identityProviders().get("kubernetes1").remove());

        identityProvider.setAlias("kubernetes2");
        try (Response response = managedRealm.admin().identityProviders().create(identityProvider)) {
            Assertions.assertEquals(400, response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("Issuer URL already used for IDP 'kubernetes1', Issuer must be unique if the idp supports JWT Authorization Grant or Federated Client Authentication", error.getErrorMessage());
        }

    }

    @Test
    public void testUpdateIdentityProviderWithDuplicateIssuer() {
        IdentityProviderRepresentation identityProvider = createRep("kubernetes1", "kubernetes");
        identityProvider.getConfig().put("issuer", "https://localhost");

        try (Response response = managedRealm.admin().identityProviders().create(identityProvider)) {
            Assertions.assertEquals(201, response.getStatus());
        }
        managedRealm.cleanup().add(r -> r.identityProviders().get("kubernetes1").remove());

        identityProvider.setAlias("kubernetes2");
        identityProvider.getConfig().put("issuer", "https://localhost2");

        try (Response response = managedRealm.admin().identityProviders().create(identityProvider)) {
            Assertions.assertEquals(201, response.getStatus());
        }
        managedRealm.cleanup().add(r -> r.identityProviders().get("kubernetes2").remove());

        IdentityProviderResource idpResource = managedRealm.admin().identityProviders().get("kubernetes2");
        identityProvider = idpResource.toRepresentation();
        identityProvider.getConfig().put("issuer", "https://localhost");

        try {
            idpResource.update(identityProvider);
            Assertions.fail("Duplicate issuer URL not detected");
        } catch (WebApplicationException ex) {
            Assertions.assertEquals(400, ex.getResponse().getStatus());
            ErrorRepresentation error = ex.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Issuer URL already used for IDP 'kubernetes1', Issuer must be unique if the idp supports JWT Authorization Grant or Federated Client Authentication", error.getErrorMessage());
        }

    }

    public static class TestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.KUBERNETES_SERVICE_ACCOUNTS);
        }
    }
}
