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
package org.keycloak.tests.admin.identityprovider;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Config/validation coverage for the {@code tls_client_auth} (RFC 8705 mTLS) client authentication method
 * on OIDC identity-provider brokering (issue #38310), migrated from the deprecated Arquillian testsuite to
 * the new test framework.
 *
 * <p>This test exercises the CONFIG + VALIDATION contract through the real Admin REST API. It does not drive
 * a live mTLS handshake — presenting the selected client certificate during the broker token exchange
 * requires an external HTTPS endpoint that requests and validates a client certificate. The new test
 * framework does not yet provide such an endpoint ({@code @InjectHttpServer} is plain HTTP), so a real
 * handshake test needs additional framework plumbing (an {@code HttpsServer} with
 * {@code setNeedClientAuth(true)} fed from {@code ManagedCertificates}). That is tracked as a follow-up;
 * the server-side certificate selection and SSL-context construction are unit-tested in
 * {@code IdpMtlsSslContextProviderTest} and {@code OAuth2IdentityProviderConfigTest}.
 */
@KeycloakIntegrationTest
public class IdentityProviderOidcTlsClientAuthTest extends AbstractIdentityProviderTest {

    private static final String TLS_IDP_ALIAS = "kc-oidc-tls-idp";

    /**
     * A {@code tls_client_auth} IdP that references a usable client-certificate key provider is accepted:
     * the Admin API returns HTTP 201 and reading the IdP back confirms the persisted config.
     */
    @Test
    public void tlsClientAuthIdpWithKeyProviderIsAccepted() {
        RealmResource realm = managedRealm.admin();

        // Reference one of the realm's default key providers. The realm's generated RSA key provider exposes
        // an enabled key with a private key and a certificate, which satisfies the create-time validation.
        String keyProviderId = resolveCertKeyProviderId(realm);
        assertThat("Realm must have a key provider exposing a certificate", keyProviderId, notNullValue());

        IdentityProviderRepresentation idp = createRep(TLS_IDP_ALIAS, "oidc");
        Map<String, String> config = idp.getConfig();
        config.put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        config.put("clientId", "clientId");
        config.put("authorizationUrl", "https://idp.example.com/auth");
        config.put(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, "https://idp.example.com/token");
        config.put("clientAuthMethod", OIDCLoginProtocol.TLS_CLIENT_AUTH);
        config.put(OAuth2IdentityProviderConfig.CLIENT_CERT_KEY_PROVIDER_ID, keyProviderId);

        try (Response response = realm.identityProviders().create(idp)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                    "tls_client_auth IdP with a usable key provider must be accepted");
        }
        managedRealm.cleanup().add(r -> r.identityProviders().get(TLS_IDP_ALIAS).remove());

        IdentityProviderRepresentation persisted = realm.identityProviders().get(TLS_IDP_ALIAS).toRepresentation();
        assertEquals(OIDCLoginProtocol.TLS_CLIENT_AUTH, persisted.getConfig().get("clientAuthMethod"));
        assertEquals(keyProviderId, persisted.getConfig().get(OAuth2IdentityProviderConfig.CLIENT_CERT_KEY_PROVIDER_ID));
    }

    /**
     * A {@code tls_client_auth} IdP WITHOUT a client-certificate key provider is rejected by the Admin API
     * with HTTP 400, per {@link OAuth2IdentityProviderConfig#validate}.
     */
    @Test
    public void tlsClientAuthIdpWithoutKeyProviderIsRejected() {
        RealmResource realm = managedRealm.admin();

        String alias = TLS_IDP_ALIAS + "-invalid";
        IdentityProviderRepresentation idp = createRep(alias, "oidc");
        Map<String, String> config = idp.getConfig();
        config.put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        config.put("clientId", "clientId");
        config.put("authorizationUrl", "https://idp.example.com/auth");
        config.put(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, "https://idp.example.com/token");
        config.put("clientAuthMethod", OIDCLoginProtocol.TLS_CLIENT_AUTH);
        // no clientCertKeyProviderId on purpose

        try (Response response = realm.identityProviders().create(idp)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
                    "tls_client_auth IdP without a key provider must be rejected");
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertThat("Validation error should reference the certificate/key provider requirement",
                    error.getErrorMessage().toLowerCase(), containsString("certificate"));
        }

        // And it must not have been persisted.
        List<IdentityProviderRepresentation> all = realm.identityProviders().findAll();
        assertFalse(all.stream().anyMatch(i -> alias.equals(i.getAlias())),
                "The rejected IdP must not be persisted");
    }

    private static String resolveCertKeyProviderId(RealmResource realm) {
        List<ComponentRepresentation> keyProviders = realm.components().query(null, KeyProvider.class.getName());
        Assertions.assertNotNull(keyProviders, "No key provider components found in realm");
        Assertions.assertFalse(keyProviders.isEmpty(), "No key provider components found in realm");
        // Prefer the generated RSA provider, which has both a private key and a certificate.
        return keyProviders.stream()
                .filter(c -> "rsa-generated".equals(c.getProviderId()))
                .map(ComponentRepresentation::getId)
                .findFirst()
                .orElseGet(() -> keyProviders.get(0).getId());
    }
}
