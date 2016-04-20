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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CompositeRolesModelTest extends AbstractModelTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        RealmManager manager = realmManager;
        RealmRepresentation rep = AbstractModelTest.loadJson("model/testcomposites.json");
        rep.setId("TestComposites");
        manager.importRealm(rep);
    }

    @Test
    public void testNoClientID() throws IOException {

        RealmManager manager = realmManager;
        RealmRepresentation rep = AbstractModelTest.loadJson("model/testrealm-noclient-id.json");
        rep.setId("TestNoClientID");
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Unknown client specification in scope mappings: some-client");
        manager.importRealm(rep);

    }

    @Test
    public void testComposites() {
        Set<RoleModel> requestedRoles = getRequestedRoles("APP_COMPOSITE_APPLICATION", "APP_COMPOSITE_USER");
        Assert.assertEquals(2, requestedRoles.size());
        assertContains("APP_ROLE_APPLICATION", "APP_ROLE_1", requestedRoles);
        assertContains("realm", "REALM_ROLE_1", requestedRoles);

        requestedRoles = getRequestedRoles("APP_COMPOSITE_APPLICATION", "REALM_APP_COMPOSITE_USER");
        Assert.assertEquals(1, requestedRoles.size());
        assertContains("APP_ROLE_APPLICATION", "APP_ROLE_1", requestedRoles);

        requestedRoles = getRequestedRoles("REALM_COMPOSITE_1_APPLICATION", "REALM_COMPOSITE_1_USER");
        Assert.assertEquals(1, requestedRoles.size());
        assertContains("realm", "REALM_COMPOSITE_1", requestedRoles);

        requestedRoles = getRequestedRoles("REALM_ROLE_1_APPLICATION", "REALM_COMPOSITE_1_USER");
        Assert.assertEquals(1, requestedRoles.size());
        assertContains("realm", "REALM_ROLE_1", requestedRoles);

        requestedRoles = getRequestedRoles("REALM_COMPOSITE_1_APPLICATION", "REALM_ROLE_1_USER");
        Assert.assertEquals(1, requestedRoles.size());
        assertContains("realm", "REALM_ROLE_1", requestedRoles);
    }

    // Same algorithm as in TokenManager.createAccessCode
    private Set<RoleModel> getRequestedRoles(String applicationName, String username) {
        Set<RoleModel> requestedRoles = new HashSet<RoleModel>();

        RealmModel realm = realmManager.getRealm("TestComposites");
        UserModel user = realmManager.getSession().users().getUserByUsername(username, realm);
        ClientModel application = realm.getClientByClientId(applicationName);

        Set<RoleModel> roleMappings = user.getRoleMappings();
        Set<RoleModel> scopeMappings = application.getScopeMappings();
        Set<RoleModel> appRoles = application.getRoles();
        if (appRoles != null) scopeMappings.addAll(appRoles);

        for (RoleModel role : roleMappings) {
            if (role.getContainer().equals(application)) requestedRoles.add(role);
            for (RoleModel desiredRole : scopeMappings) {
                Set<RoleModel> visited = new HashSet<RoleModel>();
                applyScope(role, desiredRole, visited, requestedRoles);
            }
        }

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

        for (RoleModel contained : scope.getComposites()) {
            applyScope(role, contained, visited, requested);
        }
    }

    private RoleModel getRole(String appName, String roleName) {
        RealmModel realm = realmManager.getRealm("TestComposites");
        if ("realm".equals(appName)) {
            return realm.getRole(roleName);
        }  else {
            return realm.getClientByClientId(appName).getRole(roleName);
        }
    }

    private void assertContains(String appName, String roleName, Set<RoleModel> requestedRoles) {
        RoleModel expectedRole = getRole(appName, roleName);

        Assert.assertTrue(requestedRoles.contains(expectedRole));

        // Check if requestedRole has correct role container
        for (RoleModel role : requestedRoles) {
            if (role.equals(expectedRole)) {
                Assert.assertEquals(role.getContainer(), expectedRole.getContainer());
            }
        }
    }
}
