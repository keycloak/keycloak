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
package org.keycloak.services.clientpolicy.context.admin;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class ClientScopeMappingRegisterContextTest {

    @Test
    public void getEventReturnsRegisterScopeMapping() {
        ClientScopeMappingRegisterContext ctx = new ClientScopeMappingRegisterContext(
                ContextTestStubs.stubScopeContainer(), null, List.of(), ContextTestStubs.stubAdminAuth());
        assertEquals(ClientPolicyEvent.REGISTER_SCOPE_MAPPING, ctx.getEvent());
    }

    @Test
    public void getScopeContainerReturnsConstructorArg() {
        var container = ContextTestStubs.stubScopeContainer();
        ClientScopeMappingRegisterContext ctx = new ClientScopeMappingRegisterContext(
                container, null, List.of(), ContextTestStubs.stubAdminAuth());
        assertSame(container, ctx.getScopeContainer());
    }

    @Test
    public void getRoleContainerClientReturnsConstructorArg() {
        var client = ContextTestStubs.stubClient();
        ClientScopeMappingRegisterContext ctx = new ClientScopeMappingRegisterContext(
                ContextTestStubs.stubScopeContainer(), client, List.of(), ContextTestStubs.stubAdminAuth());
        assertSame(client, ctx.getRoleContainerClient());
    }

    @Test
    public void getRoleContainerClientIsNullForRealmRole() {
        ClientScopeMappingRegisterContext ctx = new ClientScopeMappingRegisterContext(
                ContextTestStubs.stubScopeContainer(), null, List.of(), ContextTestStubs.stubAdminAuth());
        assertNull(ctx.getRoleContainerClient());
    }

    @Test
    public void getRolesReturnsImmutableSnapshot() {
        List<RoleRepresentation> roles = new ArrayList<>(List.of(new RoleRepresentation()));
        ClientScopeMappingRegisterContext ctx = new ClientScopeMappingRegisterContext(
                ContextTestStubs.stubScopeContainer(), null, roles, ContextTestStubs.stubAdminAuth());

        roles.add(new RoleRepresentation());

        assertEquals(1, ctx.getRoles().size());
        assertThrows(UnsupportedOperationException.class, () -> ctx.getRoles().add(new RoleRepresentation()));
    }

    @Test
    public void authenticatedUserDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        ClientScopeMappingRegisterContext ctx = new ClientScopeMappingRegisterContext(
                ContextTestStubs.stubScopeContainer(), null, List.of(), auth);
        assertSame(auth.getUser(), ctx.getAuthenticatedUser());
    }
}
