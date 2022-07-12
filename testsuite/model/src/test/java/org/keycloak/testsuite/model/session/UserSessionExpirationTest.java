/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.map.userSession.MapUserSessionProviderFactory;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RequireProvider(value = UserSessionProvider.class, only = MapUserSessionProviderFactory.PROVIDER_ID)
@RequireProvider(RealmProvider.class)
public class UserSessionExpirationTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testClientSessionIdleTimeout() {

        // Set low ClientSessionIdleTimeout
        withRealm(realmId, (session, realm) -> {
            realm.setSsoSessionIdleTimeout(1800);
            realm.setSsoSessionMaxLifespan(36000);
            realm.setClientSessionIdleTimeout(5);
            return null;
        });

        String uSId= withRealm(realmId, (session, realm) -> session.sessions().createUserSession(realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null).getId());

        assertThat(withRealm(realmId, (session, realm) -> session.sessions().getUserSession(realm, uSId)), notNullValue());

        Time.setOffset(5);

        assertThat(withRealm(realmId, (session, realm) -> session.sessions().getUserSession(realm, uSId)), nullValue());

    }
}
