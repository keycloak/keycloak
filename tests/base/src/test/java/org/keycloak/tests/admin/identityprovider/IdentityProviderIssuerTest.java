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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantIdentityProviderFactory;
import org.keycloak.broker.kubernetes.KubernetesIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.social.google.GoogleIdentityProviderFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = IdentityProviderIssuerTest.TestServerConfig.class)
public class IdentityProviderIssuerTest extends AbstractIdentityProviderTest {

    @Test
    public void testCreateUpdateDuplicateIdentityProvider() {
        String issuer = "http://localhost:8080";

        // JWTAuthorizationGrant idp - JWTAuthorizationGrant idp: not allowed
        testCreateIdentityProviderDuplicateNotAllowed(JWTAuthorizationGrantIdentityProviderFactory.PROVIDER_ID, JWTAuthorizationGrantIdentityProviderFactory.PROVIDER_ID, issuer);

        // Kubernetes idp - Kubernetes idp: not allowed
        testCreateIdentityProviderDuplicateNotAllowed(KubernetesIdentityProviderFactory.PROVIDER_ID, KubernetesIdentityProviderFactory.PROVIDER_ID, issuer);

        // JWTAuthorizationGrant idp - Kubernetes idp: not allowed
        testCreateIdentityProviderDuplicateNotAllowed(JWTAuthorizationGrantIdentityProviderFactory.PROVIDER_ID, KubernetesIdentityProviderFactory.PROVIDER_ID, issuer);

        // JWTAuthorizationGrant idp - OIDC idp: allowed
        testCreateIdentityProviderDuplicateAllowed(JWTAuthorizationGrantIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID, issuer, false, false);

        // JWTAuthorizationGrant idp - OIDC idp: not allowed
        testCreateIdentityProviderDuplicateNotAllowed(JWTAuthorizationGrantIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID, issuer, true, false);

        // Kubernetes idp - OIDC idp: not allowed
        testCreateIdentityProviderDuplicateNotAllowed(KubernetesIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID, issuer, false, true);

        // OIDC idp - OIDC idp: allowed
        testCreateIdentityProviderDuplicateAllowed(OIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID, issuer, false, false);

        // OIDC idp - OIDC idp: not allowed
        testCreateIdentityProviderDuplicateNotAllowed(OIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID, issuer, true, false);
        testCreateIdentityProviderDuplicateNotAllowed(OIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID, issuer, false, true);

        // Google idp - Google idp: allowed
        testCreateIdentityProviderDuplicateAllowed(GoogleIdentityProviderFactory.PROVIDER_ID, GoogleIdentityProviderFactory.PROVIDER_ID, null, false, false);

        // Google idp - Google idp: allowed
        testCreateIdentityProviderDuplicateAllowed(GoogleIdentityProviderFactory.PROVIDER_ID, GoogleIdentityProviderFactory.PROVIDER_ID, null, false, true);

        // Google idp - Google idp: not allowed
        testCreateIdentityProviderDuplicateNotAllowed(GoogleIdentityProviderFactory.PROVIDER_ID, GoogleIdentityProviderFactory.PROVIDER_ID, null, true, false);
    }

    @Test
    public void testCreateUpdateDuplicateIdentityProviderDisabled() {
        String issuer = "http://localhost:8080";

        // test two OIDC adapters not allowed
        testCreateIdentityProviderDuplicateAllowedNoCleanUp(OIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID,
                issuer, true, false, false);

        // disable the first idp1
        IdentityProviderResource idp1 = managedRealm.admin().identityProviders().get("idp1");
        IdentityProviderRepresentation idp1Rep = idp1.toRepresentation();
        idp1Rep.setEnabled(false);
        idp1.update(idp1Rep);

        // now the idp2 could be updated to the same issuer
        IdentityProviderResource idp2 = managedRealm.admin().identityProviders().get("idp2");
        IdentityProviderRepresentation idp2Rep = idp2.toRepresentation();
        idp2Rep.getConfig().put("issuer", issuer);
        idp2.update(idp2Rep);

        // idp1 cannot be enabled now
        idp1Rep.setEnabled(true);
        final IdentityProviderRepresentation idp = idp1Rep;
        BadRequestException e = Assertions.assertThrows(BadRequestException.class, () -> idp1.update(idp));
        assertEquals("Issuer URL already used for IDP 'idp2', Issuer must be unique if the idp supports JWT Authorization Grant or Federated Client Authentication",
                e.getResponse().readEntity(ErrorRepresentation.class).getErrorMessage());

        // disable now idp2
        idp2Rep = idp2.toRepresentation();
        idp2Rep.setEnabled(false);
        idp2.update(idp2Rep);

        // enable idp1
        idp1Rep = idp1.toRepresentation();
        idp1Rep.setEnabled(true);
        idp1.update(idp1Rep);
    }

    public void testCreateIdentityProviderDuplicateNotAllowed(String providerId1, String providerId2, String issuer) {
        testCreateIdentityProviderDuplicateAllowed(providerId1, providerId2, issuer, false, false, false);
    }

    public void testCreateIdentityProviderDuplicateNotAllowed(String providerId1, String providerId2, String issuer, boolean JWTAuthorizationGrantEnabled, boolean federatedAuthenticationEnabled) {
        testCreateIdentityProviderDuplicateAllowed(providerId1, providerId2, issuer, JWTAuthorizationGrantEnabled, federatedAuthenticationEnabled, false);
    }

    public void testCreateIdentityProviderDuplicateAllowed(String providerId1, String providerId2, String issuer, boolean JWTAuthorizationGrantEnabled, boolean federatedAuthenticationEnabled) {
        testCreateIdentityProviderDuplicateAllowed(providerId1, providerId2, issuer, JWTAuthorizationGrantEnabled, federatedAuthenticationEnabled, true);
    }

    public void testCreateIdentityProviderDuplicateAllowed(String providerId1, String providerId2, String issuer, boolean JWTAuthorizationGrantEnabled, boolean federatedAuthenticationEnabled, boolean allowDuplicate) {
        testCreateIdentityProviderDuplicateAllowedNoCleanUp(providerId1, providerId2, issuer, JWTAuthorizationGrantEnabled, federatedAuthenticationEnabled, allowDuplicate);
        managedRealm.runCleanup();
    }

    public void testCreateIdentityProviderDuplicateAllowedNoCleanUp(String providerId1, String providerId2, String issuer, boolean JWTAuthorizationGrantEnabled, boolean federatedAuthenticationEnabled, boolean allowDuplicate) {
        String idp1 = "idp1";
        String idp2 = "idp2";
        IdentityProviderRepresentation identityProvider1 = createRep(idp1, providerId1, issuer, JWTAuthorizationGrantEnabled, federatedAuthenticationEnabled);
        IdentityProviderRepresentation identityProvider2 = createRep(idp2, providerId2, issuer, JWTAuthorizationGrantEnabled, federatedAuthenticationEnabled);

        try (Response response = managedRealm.admin().identityProviders().create(identityProvider1)) {
            Assertions.assertEquals(201, response.getStatus());
        }

        managedRealm.cleanup().add(r -> r.identityProviders().get(idp1).remove());

        Response response = managedRealm.admin().identityProviders().create(identityProvider2);
        if (allowDuplicate) {
            Assertions.assertEquals(201, response.getStatus());
            managedRealm.cleanup().add(r -> r.identityProviders().get(idp2).remove());
        } else {
            Assertions.assertEquals(400, response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("Issuer URL already used for IDP '" + idp1 + "', Issuer must be unique if the idp supports JWT Authorization Grant or Federated Client Authentication", error.getErrorMessage());


            //create with different issuer only if issuer is present
            if (issuer != null) {
                identityProvider2.getConfig().put("issuer", "https://localhost2");
                response = managedRealm.admin().identityProviders().create(identityProvider2);
                Assertions.assertEquals(201, response.getStatus());

                managedRealm.cleanup().add(r -> r.identityProviders().get(idp2).remove());

                IdentityProviderResource idpResource = managedRealm.admin().identityProviders().get(idp2);
                identityProvider2 = idpResource.toRepresentation();
                identityProvider2.getConfig().put("issuer", issuer);

                try {
                    idpResource.update(identityProvider2);
                    Assertions.fail("Duplicate issuer URL not detected");
                } catch (WebApplicationException ex) {
                    Assertions.assertEquals(400, ex.getResponse().getStatus());
                    error = ex.getResponse().readEntity(ErrorRepresentation.class);
                    assertEquals("Issuer URL already used for IDP '" + idp1 + "', Issuer must be unique if the idp supports JWT Authorization Grant or Federated Client Authentication", error.getErrorMessage());
                }
            }
        }
    }

    public IdentityProviderRepresentation createRep(String alias, String providerId, String issuer, boolean JWTAuthorizationGrantEnabled, boolean federatedAuthenticationEnabled) {
        IdentityProviderRepresentation identityProvider = createRep(alias, providerId);

        // Use the passed issuer if not null, otherwise default to localhost if not Google/Social
        if (issuer != null) {
            identityProvider.getConfig().put("issuer", issuer);
        } else if (!providerId.equals(GoogleIdentityProviderFactory.PROVIDER_ID)) {
            // Default for generic tests if null passed
            identityProvider.getConfig().put("issuer", issuer);
        }

        identityProvider.getConfig().put("jwtAuthorizationGrantEnabled", String.valueOf(JWTAuthorizationGrantEnabled));
        identityProvider.getConfig().put("supportsClientAssertions", String.valueOf(federatedAuthenticationEnabled));

        identityProvider.getConfig().put("useJwksUrl", "true");
        identityProvider.getConfig().put("jwksUrl", issuer);

        return identityProvider;
    }

    public static class TestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.KUBERNETES_SERVICE_ACCOUNTS, Profile.Feature.JWT_AUTHORIZATION_GRANT);
        }
    }
}
