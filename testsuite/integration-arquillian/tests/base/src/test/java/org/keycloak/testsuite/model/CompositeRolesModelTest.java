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

package org.keycloak.testsuite.model;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CompositeRolesModelTest extends AbstractTestRealmKeycloakTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public static Set<RoleModel> getRequestedRoles(ClientModel application, UserModel user) {

        Set<RoleModel> requestedRoles = new HashSet<>();

        Set<RoleModel> roleMappings = user.getRoleMappingsStream().collect(Collectors.toSet());
        Stream<RoleModel> scopeMappings = Stream.concat(application.getScopeMappingsStream(), application.getRolesStream());

        scopeMappings.forEach(scope -> roleMappings.forEach(role -> {
            if (role.getContainer().equals(application)) requestedRoles.add(role);

            Set<RoleModel> visited = new HashSet<>();
            applyScope(role, scope, visited, requestedRoles);
        }));
        return requestedRoles;
    }



    private static void applyScope(RoleModel role, RoleModel scope, Set<RoleModel> visited, Set<RoleModel> requested) {
        if (visited.contains(scope)) return;
        visited.add(scope);
        if (role.hasRole(scope)) {
            requested.add(scope);
            return;
        }
        if (!scope.isComposite()) return;

        scope.getCompositesStream().forEach(contained -> applyScope(role, contained, visited, requested));
    }

    private static RoleModel getRole(RealmModel realm, String appName, String roleName) {
        if ("realm".equals(appName)) {
            return realm.getRole(roleName);
        } else {
            return realm.getClientByClientId(appName).getRole(roleName);
        }
    }

    private static void assertContains(RealmModel realm, String appName, String roleName, Set<RoleModel> requestedRoles) {
        RoleModel expectedRole = getRole(realm, appName, roleName);

        Assert.assertTrue(requestedRoles.contains(expectedRole));

        // Check if requestedRole has correct role container
        for (RoleModel role : requestedRoles) {
            if (role.equals(expectedRole)) {
                Assert.assertEquals(role.getContainer(), expectedRole.getContainer());
            }
        }
    }

    @Test
    @ModelTest
    public void testNoClientID(KeycloakSession session) {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Unknown client specification in scope mappings: some-client");

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session1) -> {
            try {
                //RealmManager manager = new RealmManager(session1);
                RealmRepresentation rep = loadJson(getClass().getResourceAsStream("/model/testrealm-noclient-id.json"), RealmRepresentation.class);
                rep.setId("TestNoClientID");
                //manager.importRealm(rep);
                adminClient.realms().create(rep);
            } catch (RuntimeException e) {
            }

        });
    }

    @Test
    @ModelTest
    public void testComposites(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session5) -> {

            RealmModel realm = session5.realms().getRealmByName("TestComposites");
            session5.getContext().setRealm(realm);

            Set<RoleModel> requestedRoles = getRequestedRoles(realm.getClientByClientId("APP_COMPOSITE_APPLICATION"), session.users().getUserByUsername(realm, "APP_COMPOSITE_USER"));

            Assert.assertEquals(5, requestedRoles.size());
            assertContains(realm, "APP_COMPOSITE_APPLICATION", "APP_COMPOSITE_ROLE", requestedRoles);
            assertContains(realm, "APP_COMPOSITE_APPLICATION", "APP_COMPOSITE_CHILD", requestedRoles);
            assertContains(realm, "APP_COMPOSITE_APPLICATION", "APP_ROLE_2", requestedRoles);
            assertContains(realm, "APP_ROLE_APPLICATION", "APP_ROLE_1", requestedRoles);
            assertContains(realm, "realm", "REALM_ROLE_1", requestedRoles);

            Set<RoleModel> requestedRoles2 = getRequestedRoles(realm.getClientByClientId("APP_COMPOSITE_APPLICATION"), session5.users().getUserByUsername(realm, "REALM_APP_COMPOSITE_USER"));
            Assert.assertEquals(4, requestedRoles2.size());
            assertContains(realm, "APP_ROLE_APPLICATION", "APP_ROLE_1", requestedRoles2);

            requestedRoles = getRequestedRoles(realm.getClientByClientId("REALM_COMPOSITE_1_APPLICATION"), session5.users().getUserByUsername(realm, "REALM_COMPOSITE_1_USER"));
            Assert.assertEquals(1, requestedRoles.size());
            assertContains(realm, "realm", "REALM_COMPOSITE_1", requestedRoles);

            requestedRoles = getRequestedRoles(realm.getClientByClientId("REALM_COMPOSITE_2_APPLICATION"), session5.users().getUserByUsername(realm, "REALM_COMPOSITE_1_USER"));
            Assert.assertEquals(3, requestedRoles.size());
            assertContains(realm, "realm", "REALM_COMPOSITE_1", requestedRoles);
            assertContains(realm, "realm", "REALM_COMPOSITE_CHILD", requestedRoles);
            assertContains(realm, "realm", "REALM_ROLE_4", requestedRoles);

            requestedRoles = getRequestedRoles(realm.getClientByClientId("REALM_ROLE_1_APPLICATION"), session5.users().getUserByUsername(realm, "REALM_COMPOSITE_1_USER"));
            Assert.assertEquals(1, requestedRoles.size());
            assertContains(realm, "realm", "REALM_ROLE_1", requestedRoles);

            requestedRoles = getRequestedRoles(realm.getClientByClientId("REALM_COMPOSITE_1_APPLICATION"), session5.users().getUserByUsername(realm, "REALM_ROLE_1_USER"));
            Assert.assertEquals(1, requestedRoles.size());
            assertContains(realm, "realm", "REALM_ROLE_1", requestedRoles);
        });

    }


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        log.infof("testcomposites imported");
        RealmRepresentation newRealm = loadJson(getClass().getResourceAsStream("/model/testcomposites2.json"), RealmRepresentation.class);
        adminClient.realms().create(newRealm);

    }

}
