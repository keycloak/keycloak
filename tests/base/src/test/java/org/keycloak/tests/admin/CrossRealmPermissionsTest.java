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

package org.keycloak.tests.admin;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class CrossRealmPermissionsTest {

    @InjectRealm(ref = "realm1", config = CrossRealmPermissionsRealmConfig.class)
    ManagedRealm managedRealm1;

    @InjectRealm(ref = "realm2", config = CrossRealmPermissionsRealmConfig.class)
    ManagedRealm managedRealm2;

    @Test
    public void users() {
        UserRepresentation user = UserConfigBuilder.create()
                .username("randomuser-" + Time.currentTimeMillis())
                .build();
        final String userUuid = ApiUtil.getCreatedId(managedRealm1.admin().users().create(user));

        expectNotFound(realm ->
                realm.users().get(userUuid).toRepresentation(), managedRealm2.admin()
        );

        expectNotFound(realm ->
                realm.users().get(userUuid).update(new UserRepresentation()), managedRealm2.admin()
        );

        expectNotFound(realm ->
                realm.users().get(userUuid).remove(), managedRealm2.admin()
        );

        expectNotFound(realm ->
                realm.users().get(userUuid).getUserSessions(), managedRealm2.admin()
        );
    }

    private void expectNotFound(final Invocation invocation, RealmResource realm) {
        expectNotFound((realm1, response) -> invocation.invoke(realm1), realm);
    }

    private void expectNotFound(InvocationWithResponse invocation, RealmResource realm) {
        int statusCode = 0;
        try {
            AtomicReference<Response> responseReference = new AtomicReference<>();
            invocation.invoke(realm, responseReference);
            Response response = responseReference.get();
            if (response != null) {
                statusCode = response.getStatus();
            } else {
                Assertions.fail("Expected failure");
            }
        } catch (ClientErrorException e) {
            statusCode = e.getResponse().getStatus();
        }

        Assertions.assertEquals(404, statusCode);
    }

    private interface Invocation {

        void invoke(RealmResource realm);

    }

    private interface InvocationWithResponse {

        void invoke(RealmResource realm, AtomicReference<Response> response);

    }

    private static class CrossRealmPermissionsRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addUser(AdminRoles.REALM_ADMIN)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                    .password("password");

            return realm;
        }
    }

}
