/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.admin.authz.fgap;

import java.util.List;

import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest
public class FeatureV2EnabledTest {

    @InjectRealm
    private ManagedRealm realm;

    @InjectClient(config = AuthzClientConfig.class)
    private ManagedClient testClient;

    @Test
    public void schemaNotAvailableForNonAdminPermissionClient() {
        ResourceServerRepresentation authorizationSettings = testClient.admin().authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());
        assertThat(authorizationSettings.getAuthorizationSchema(), nullValue());
    }

    @Test
    public void schemaAvailableAfterFGAPEnabledForRealm() {
        // admin permissions client should not exist when the switch in not enabled for the realm
        assertThat(realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID), is(empty()));

        // enable admin permissions for the realm
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(Boolean.TRUE);
        realm.admin().update(realmRep);

        List<ClientRepresentation> clients = realm.admin().clients().findByClientId(Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        assertThat(clients, hasSize(1));
        ResourceServerRepresentation authorizationSettings = realm.admin().clients().get(clients.get(0).getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());
        assertThat(authorizationSettings.getAuthorizationSchema(), notNullValue());
    }
}
