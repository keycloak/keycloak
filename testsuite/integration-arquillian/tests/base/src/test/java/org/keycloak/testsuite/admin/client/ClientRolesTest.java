/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.admin.client;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.RoleBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ClientRolesTest extends AbstractClientTest {

    private ClientResource clientRsc;
    private String clientDbId;
    private RolesResource rolesRsc;

    @Before
    public void init() {
        clientDbId = createOidcClient("roleClient");
        clientRsc = findClientResource("roleClient");
        rolesRsc = clientRsc.roles();
    }

    @After
    public void tearDown() {
        clientRsc.remove();
    }

    private RoleRepresentation makeRole(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }

    private boolean hasRole(RolesResource rolesRsc, String name) {
        for (RoleRepresentation role : rolesRsc.list()) {
            if (role.getName().equals(name)) return true;
        }

        return false;
    }

    @Test
    public void testAddRole() {
        rolesRsc.create(makeRole("role1"));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, Matchers.startsWith(AdminEventPaths.clientRolesResourcePath(clientDbId)));
        assertTrue(hasRole(rolesRsc, "role1"));
    }

    @Test
    public void testRemoveRole() {
        rolesRsc.create(makeRole("role2"));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, Matchers.startsWith(AdminEventPaths.clientRolesResourcePath(clientDbId)));

        rolesRsc.deleteRole("role2");
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientRoleResourcePath(clientDbId, "role2"));

        assertFalse(hasRole(rolesRsc, "role2"));
    }

    @Test
    public void testComposites() {
        rolesRsc.create(makeRole("role-a"));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, Matchers.startsWith(AdminEventPaths.clientRolesResourcePath(clientDbId)));

        assertFalse(rolesRsc.get("role-a").toRepresentation().isComposite());
        assertEquals(0, rolesRsc.get("role-a").getRoleComposites().size());

        rolesRsc.create(makeRole("role-b"));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, Matchers.startsWith(AdminEventPaths.clientRolesResourcePath(clientDbId)));

        testRealmResource().roles().create(makeRole("role-c"));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, Matchers.startsWith(AdminEventPaths.rolesResourcePath()));

        List<RoleRepresentation> l = new LinkedList<>();
        l.add(rolesRsc.get("role-b").toRepresentation());
        l.add(testRealmResource().roles().get("role-c").toRepresentation());
        rolesRsc.get("role-a").addComposites(l);
        // TODO adminEvents: Fix once composite roles events will be fixed...
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, Matchers.startsWith(AdminEventPaths.clientRoleResourceCompositesPath(clientDbId, "role-a")));
        assertAdminEvents.assertEvent(getRealmId(), OperationType.CREATE, Matchers.startsWith(AdminEventPaths.clientRoleResourceCompositesPath(clientDbId, "role-a")));

        Set<RoleRepresentation> composites = rolesRsc.get("role-a").getRoleComposites();

        assertTrue(rolesRsc.get("role-a").toRepresentation().isComposite());
        Assert.assertNames(composites, "role-b", "role-c");

        Set<RoleRepresentation> realmComposites = rolesRsc.get("role-a").getRealmRoleComposites();
        Assert.assertNames(realmComposites, "role-c");

        Set<RoleRepresentation> clientComposites = rolesRsc.get("role-a").getClientRoleComposites(clientRsc.toRepresentation().getId());
        Assert.assertNames(clientComposites, "role-b");

        rolesRsc.get("role-a").deleteComposites(l);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientRoleResourceCompositesPath(clientDbId, "role-a"));

        assertFalse(rolesRsc.get("role-a").toRepresentation().isComposite());
        assertEquals(0, rolesRsc.get("role-a").getRoleComposites().size());
    }

}
