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

import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
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
            IdentityProviderModel idp = createModel("kubernetes1", "kubernetes");
            idp.getConfig().put("issuer", "https://localhost");
            session.identityProviders().create(idp);

            idp = createModel("kubernetes2", "kubernetes");
            idp.getConfig().put("issuer", "https://localhost");
            session.identityProviders().create(idp);

            RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () ->
                    session.getProvider(AlternativeLookupProvider.class).lookupIdentityProviderFromIssuer(session, "https://localhost")
            );
            Assertions.assertEquals("Multiple IDPs match the same issuer: [kubernetes1, kubernetes2]", ex.getMessage());

        } finally {
            session.identityProviders().remove("kubernetes1");
            session.identityProviders().remove("kubernetes2");
        }
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
