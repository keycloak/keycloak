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

package org.keycloak.testsuite.admin;

import org.junit.AfterClass;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.utils.tls.TLSUtils;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CrossRealmPermissionsTest extends AbstractKeycloakTest {

    private static final String REALM_NAME = "crossrealm-test";
    private static final String REALM2_NAME = "crossrealm2-test";

    private static Keycloak adminClient1;
    private static Keycloak adminClient2;

    private RealmResource realm1;
    private RealmResource realm2;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder builder = RealmBuilder.create().name(REALM_NAME).testMail();
        builder.client(ClientBuilder.create().clientId("test-client").publicClient().directAccessGrants());

        builder.user(UserBuilder.create()
                .username(AdminRoles.REALM_ADMIN)
                .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                .addPassword("password"));
        testRealms.add(builder.build());

        adminClient1 = Keycloak.getInstance(getAuthServerContextRoot() + "/auth", REALM_NAME, AdminRoles.REALM_ADMIN, "password", "test-client", "secret", TLSUtils.initializeTLS());
        realm1 = adminClient1.realm(REALM_NAME);

        builder = RealmBuilder.create().name(REALM2_NAME).testMail();
        builder.client(ClientBuilder.create().clientId("test-client").publicClient().directAccessGrants());

        builder.user(UserBuilder.create()
                .username(AdminRoles.REALM_ADMIN)
                .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                .addPassword("password"));

        testRealms.add(builder.build());

        adminClient2 = Keycloak.getInstance(getAuthServerContextRoot() + "/auth", REALM2_NAME, AdminRoles.REALM_ADMIN, "password", "test-client", "secret", TLSUtils.initializeTLS());
        realm2 = adminClient2.realm(REALM2_NAME);
    }


    @AfterClass
    public static void afterClass() {
        adminClient1.close();
        adminClient2.close();
    }


    @Test
    public void users() {
        UserRepresentation user = UserBuilder.create().username("randomuser-" + Time.currentTimeMillis()).build();
        Response response = realm1.users().create(user);
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        realm1.users().get(userId).toRepresentation();

        expectNotFound(new PermissionsTest.Invocation() {
            @Override
            public void invoke(RealmResource realm) {
                realm.users().get(userId).toRepresentation();
            }
        }, realm2);

        expectNotFound(new PermissionsTest.Invocation() {
            @Override
            public void invoke(RealmResource realm) {
                realm.users().get(userId).update(new UserRepresentation());
            }
        }, realm2);

        expectNotFound(new PermissionsTest.Invocation() {
            @Override
            public void invoke(RealmResource realm) {
                realm.users().get(userId).remove();
            }
        }, realm2);

        expectNotFound(new PermissionsTest.Invocation() {
            @Override
            public void invoke(RealmResource realm) {
                realm.users().get(userId).getUserSessions();
            }
        }, realm2);
    }

    private void expectNotFound(final PermissionsTest.Invocation invocation, RealmResource realm) {
        expectNotFound(new PermissionsTest.InvocationWithResponse() {
            public void invoke(RealmResource realm, AtomicReference<Response> response) {
                invocation.invoke(realm);
            }
        }, realm);
    }

    private void expectNotFound(PermissionsTest.InvocationWithResponse invocation, RealmResource realm) {
        int statusCode = 0;
        try {
            AtomicReference<Response> responseReference = new AtomicReference<>();
            invocation.invoke(realm, responseReference);
            Response response = responseReference.get();
            if (response != null) {
                statusCode = response.getStatus();
            } else {
                fail("Expected failure");
            }
        } catch (ClientErrorException e) {
            statusCode = e.getResponse().getStatus();
        }

        assertEquals(404, statusCode);
    }

}
