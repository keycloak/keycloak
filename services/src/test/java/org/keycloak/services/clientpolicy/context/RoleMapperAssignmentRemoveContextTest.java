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
package org.keycloak.services.clientpolicy.context;

import java.util.List;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class RoleMapperAssignmentRemoveContextTest {

    @Test
    public void getEventReturnsUnregisterRoleMapping() {
        RoleMapperAssignmentRemoveContext ctx = new RoleMapperAssignmentRemoveContext(
                ContextTestStubs.stubRoleMapper(), null, List.of(), ContextTestStubs.stubAdminAuth());
        assertEquals(ClientPolicyEvent.UNREGISTER_ROLE_MAPPING, ctx.getEvent());
    }

    @Test
    public void getRoleMapperReturnsConstructorArg() {
        var roleMapper = ContextTestStubs.stubRoleMapper();
        RoleMapperAssignmentRemoveContext ctx = new RoleMapperAssignmentRemoveContext(
                roleMapper, null, List.of(), ContextTestStubs.stubAdminAuth());
        assertSame(roleMapper, ctx.getRoleMapper());
    }

    @Test
    public void getRolesReturnsConstructorArg() {
        List<RoleRepresentation> roles = List.of(new RoleRepresentation());
        RoleMapperAssignmentRemoveContext ctx = new RoleMapperAssignmentRemoveContext(
                ContextTestStubs.stubRoleMapper(), null, roles, ContextTestStubs.stubAdminAuth());
        assertEquals(roles, ctx.getRoles());
    }

    @Test
    public void authenticatedUserDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        RoleMapperAssignmentRemoveContext ctx = new RoleMapperAssignmentRemoveContext(
                ContextTestStubs.stubRoleMapper(), null, List.of(), auth);
        assertSame(auth.getUser(), ctx.getAuthenticatedUser());
    }
}
