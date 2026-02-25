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

package org.keycloak.tests.model;

import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmModelDelegate;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import org.junit.jupiter.api.Assertions;

@KeycloakIntegrationTest
public class AlternativeLookupProviderTest {

    @InjectRealm(attachTo = "master")
    ManagedRealm realm;

    @TestOnServer
    public void testDuplicateIssuerFoundInDatabase(KeycloakSession session) {
        try {
            IdentityProviderModel idp = createModel("oidc1", OIDCIdentityProviderFactory.PROVIDER_ID);
            idp.getConfig().put("issuer", "https://localhost");
            idp.getConfig().put(JWTAuthorizationGrantConfig.JWT_AUTHORIZATION_GRANT_ENABLED, Boolean.TRUE.toString());
            session.identityProviders().create(idp);

            idp = createModel("oidc2", OIDCIdentityProviderFactory.PROVIDER_ID);
            idp.getConfig().put("issuer", "https://localhost");
            idp.getConfig().put(JWTAuthorizationGrantConfig.JWT_AUTHORIZATION_GRANT_ENABLED, Boolean.TRUE.toString());
            session.identityProviders().create(idp);

            RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () ->
                    session.getProvider(AlternativeLookupProvider.class).lookupIdentityProviderFromIssuer(session, IdentityProviderType.JWT_AUTHORIZATION_GRANT, "https://localhost")
            );
            Assertions.assertEquals("Multiple IDPs match the same issuer: [oidc1, oidc2]", ex.getMessage());

        } finally {
            session.identityProviders().remove("oidc1");
            session.identityProviders().remove("oidc2");
        }
    }

    @TestOnServer
    public void testDuplicateIssuerCanExistsForDifferentType(KeycloakSession session) {
        try {
            IdentityProviderModel idp = createModel("oidc1", OIDCIdentityProviderFactory.PROVIDER_ID);
            idp.getConfig().put("issuer", "https://localhost");
            idp.getConfig().put(JWTAuthorizationGrantConfig.JWT_AUTHORIZATION_GRANT_ENABLED, Boolean.TRUE.toString());
            session.identityProviders().create(idp);

            idp = createModel("oidc2", OIDCIdentityProviderFactory.PROVIDER_ID);
            idp.getConfig().put("issuer", "https://localhost");
            idp.getConfig().put(OIDCIdentityProviderConfig.SUPPORTS_CLIENT_ASSERTIONS, Boolean.TRUE.toString());
            session.identityProviders().create(idp);

            idp = session.getProvider(AlternativeLookupProvider.class).lookupIdentityProviderFromIssuer(session, IdentityProviderType.JWT_AUTHORIZATION_GRANT, "https://localhost");
            Assertions.assertEquals("oidc1", idp.getAlias());
            idp = session.getProvider(AlternativeLookupProvider.class).lookupIdentityProviderFromIssuer(session, IdentityProviderType.CLIENT_ASSERTION, "https://localhost");
            Assertions.assertEquals("oidc2", idp.getAlias());
        } finally {
            session.identityProviders().remove("oidc1");
            session.identityProviders().remove("oidc2");
        }
    }

    @TestOnServer
    public void testLimitCountOfClientLookupsDuringGetRoleFromString(KeycloakSession session) {
        AtomicInteger counter = new AtomicInteger(0);

        RealmModel realm = new RealmModelDelegate(null) {
            @Override
            public ClientModel getClientByClientId(String clientId) {
                counter.incrementAndGet();
                return null;
            }

            @Override
            public String getId() {
                return "realm";
            }
        };

        String badRoleName = ".";
        for (int i = 0 ; i < 16 ; i++) {
            badRoleName = badRoleName + badRoleName;
        }
        Assertions.assertEquals(65536, badRoleName.length());

        Assertions.assertNull(KeycloakModelUtils.getRoleFromString(session, realm, badRoleName));
        Assertions.assertEquals(KeycloakModelUtils.MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE, counter.get());
    }


    protected IdentityProviderModel createModel(String alias, String providerId) {
        return createModel(alias, providerId,true);
    }

    protected IdentityProviderModel createModel(String alias, String providerId, boolean enabled) {
        return createModel(alias, alias, providerId, enabled);
    }

    protected IdentityProviderModel createModel(String alias, String displayName, String providerId, boolean enabled) {
        IdentityProviderModel idp = new IdentityProviderModel();

        idp.setAlias(alias);
        idp.setDisplayName(displayName);
        idp.setProviderId(providerId);
        idp.setEnabled(enabled);
        return idp;
    }


}
