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

package org.keycloak.testsuite.broker;


import javax.ws.rs.core.UriBuilder;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCKeycloakServerBrokerWithSignatureTest extends AbstractIdentityProviderTest {

    private static final int PORT = 8082;

    private static Keycloak keycloak1;
    private static Keycloak keycloak2;

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(PORT);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-kc-oidc.json"));
        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-oidc-identity-provider" };
        }
    };

    @BeforeClass
    public static void beforeClazz() {
        keycloak1 = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", org.keycloak.models.Constants.ADMIN_CLI_CLIENT_ID);
        keycloak2 = Keycloak.getInstance("http://localhost:8082/auth", "master", "admin", "admin", org.keycloak.models.Constants.ADMIN_CLI_CLIENT_ID);
    }

    @Override
    public void onBefore() {
        super.onBefore();

        // Enable validate signatures
        IdentityProviderModel idpModel = getIdentityProviderModel();
        OIDCIdentityProviderConfig cfg = new OIDCIdentityProviderConfig(idpModel);
        cfg.setValidateSignature(true);
        getRealm().updateIdentityProvider(cfg);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();
    }

    @Override
    protected String getProviderId() {
        return "kc-oidc-idp";
    }

    @Test
    public void testSignatureVerificationJwksUrl() throws Exception {
        // Configure OIDC identity provider with JWKS URL
        IdentityProviderModel idpModel = getIdentityProviderModel();
        OIDCIdentityProviderConfig cfg = new OIDCIdentityProviderConfig(idpModel);
        cfg.setUseJwksUrl(true);

        UriBuilder b = OIDCLoginProtocolService.certsUrl(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT).port(PORT));
        String jwksUrl = b.build("realm-with-oidc-identity-provider").toString();
        cfg.setJwksUrl(jwksUrl);
        getRealm().updateIdentityProvider(cfg);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        // Check that user is able to login
        assertSuccessfulAuthentication(getIdentityProviderModel(), "test-user", "test-user@localhost", false);

        // Rotate public keys on the parent broker
        rotateKeys("realm-with-oidc-identity-provider");

        RealmRepresentation realm = keycloak2.realm("realm-with-oidc-identity-provider").toRepresentation();
        realm.setPublicKey(org.keycloak.models.Constants.GENERATE);
        keycloak2.realm("realm-with-oidc-identity-provider").update(realm);

        // User not able to login now as new keys can't be yet downloaded (10s timeout)
        loginIDP("test-user");
        assertTrue(errorPage.isCurrent());
        assertEquals("Unexpected error when authenticating with identity provider", errorPage.getError());

        keycloak2.realm("realm-with-oidc-identity-provider").logoutAll();

        // Set time offset. New keys can be downloaded. Check that user is able to login.
        Time.setOffset(20);

        assertSuccessfulAuthentication(getIdentityProviderModel(), "test-user", "test-user@localhost", false);

        Time.setOffset(0);
    }

    @Test
    public void testSignatureVerificationHardcodedPublicKey() throws Exception {
        // Configure OIDC identity provider with publicKeySignatureVerifier
        IdentityProviderModel idpModel = getIdentityProviderModel();
        OIDCIdentityProviderConfig cfg = new OIDCIdentityProviderConfig(idpModel);
        cfg.setUseJwksUrl(false);

        RealmRepresentation realm = keycloak2.realm("realm-with-oidc-identity-provider").toRepresentation();
        cfg.setPublicKeySignatureVerifier(realm.getPublicKey());
        getRealm().updateIdentityProvider(cfg);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        // Check that user is able to login
        assertSuccessfulAuthentication(getIdentityProviderModel(), "test-user", "test-user@localhost", false);

        // Rotate public keys on the parent broker
        rotateKeys("realm-with-oidc-identity-provider");

        // User not able to login now as new keys can't be yet downloaded (10s timeout)
        loginIDP("test-user");
        assertTrue(errorPage.isCurrent());
        assertEquals("Unexpected error when authenticating with identity provider", errorPage.getError());

        keycloak2.realm("realm-with-oidc-identity-provider").logoutAll();

        // Even after time offset is user not able to login, because it uses old key hardcoded in identityProvider config
        Time.setOffset(20);

        loginIDP("test-user");
        assertTrue(errorPage.isCurrent());
        assertEquals("Unexpected error when authenticating with identity provider", errorPage.getError());

        keycloak2.realm("realm-with-oidc-identity-provider").logoutAll();

        Time.setOffset(0);

    }

    private void rotateKeys(String realmName) {
        // Rotate public keys on the parent broker
        String realmId = keycloak2.realm(realmName).toRepresentation().getId();
        ComponentRepresentation keys = new ComponentRepresentation();
        keys.setName("generated");
        keys.setProviderType(KeyProvider.class.getName());
        keys.setProviderId("rsa-generated");
        keys.setParentId(realmId);
        keys.setConfig(new MultivaluedHashMap<>());
        keys.getConfig().putSingle("priority", "9999");
        keycloak2.realm("realm-with-oidc-identity-provider").components().add(keys);

    }

}
