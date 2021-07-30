/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.session;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.keycloak.testsuite.model.session.UserSessionPersisterProviderTest.createClients;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@RequireProvider(value = AuthenticationSessionProvider.class, only = InfinispanAuthenticationSessionProviderFactory.PROVIDER_ID)
public class AuthenticationSessionTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));

        this.realmId = realm.getId();

        createClients(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testLimitAuthSessions() {
        RootAuthenticationSessionModel ras = withRealm(realmId, (session, realm) -> session.authenticationSessions().createRootAuthenticationSession(realm));

        List<String> tabIds = withRealm(realmId, (session, realm) -> {
                ClientModel client = realm.getClientByClientId("test-app");
                return IntStream.range(0, 300)
                        .mapToObj(i -> {
                            Time.setOffset(i);
                            return ras.createAuthenticationSession(client);
                        })
                        .map(AuthenticationSessionModel::getTabId)
                        .collect(Collectors.toList());
        });

        withRealm(realmId, (session, realm) -> {
            ClientModel client = realm.getClientByClientId("test-app");

            // create 301st auth session
            AuthenticationSessionModel as = ras.createAuthenticationSession(client);
            Assert.assertEquals(as, ras.getAuthenticationSession(client, as.getTabId()));

            // assert the first authentication session was deleted
            Assert.assertNull(ras.getAuthenticationSession(client, tabIds.get(0)));

            return null;
        });
    }
}
