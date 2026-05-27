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

import java.util.ArrayList;
import java.util.List;

import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class ClientProtocolMapperRegisterContextTest {

    @Test
    public void getEventReturnsRegisterProtocolMapper() {
        ClientProtocolMapperRegisterContext ctx = new ClientProtocolMapperRegisterContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperRepresentation(), ContextTestStubs.stubAdminAuth());
        assertEquals(ClientPolicyEvent.REGISTER_PROTOCOL_MAPPER, ctx.getEvent());
    }

    @Test
    public void getProposedProtocolMapperReturnsConstructorArg() {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        ClientProtocolMapperRegisterContext ctx = new ClientProtocolMapperRegisterContext(
                ContextTestStubs.stubContainer(), rep, ContextTestStubs.stubAdminAuth());
        assertSame(rep, ctx.getProposedProtocolMapper());
    }

    @Test
    public void getProtocolMapperContainerReturnsConstructorArg() {
        var container = ContextTestStubs.stubContainer();
        ClientProtocolMapperRegisterContext ctx = new ClientProtocolMapperRegisterContext(
                container, new ProtocolMapperRepresentation(), ContextTestStubs.stubAdminAuth());
        assertSame(container, ctx.getProtocolMapperContainer());
    }

    @Test
    public void getExistingProtocolMapperIsNullOnRegister() {
        ClientProtocolMapperRegisterContext ctx = new ClientProtocolMapperRegisterContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperRepresentation(), ContextTestStubs.stubAdminAuth());
        assertNull(ctx.getExistingProtocolMapper());
    }

    @Test
    public void authenticatedUserDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        ClientProtocolMapperRegisterContext ctx = new ClientProtocolMapperRegisterContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperRepresentation(), auth);
        assertSame(auth.getUser(), ctx.getAuthenticatedUser());
    }

    @Test
    public void authenticatedClientDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        ClientProtocolMapperRegisterContext ctx = new ClientProtocolMapperRegisterContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperRepresentation(), auth);
        assertSame(auth.getClient(), ctx.getAuthenticatedClient());
    }

    @Test
    public void tokenDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        ClientProtocolMapperRegisterContext ctx = new ClientProtocolMapperRegisterContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperRepresentation(), auth);
        assertSame(auth.getToken(), ctx.getToken());
    }

    @Test
    public void batchRegisterExposesImmutableMapperSnapshot() {
        ProtocolMapperRepresentation first = new ProtocolMapperRepresentation();
        ProtocolMapperRepresentation second = new ProtocolMapperRepresentation();
        ProtocolMapperRepresentation third = new ProtocolMapperRepresentation();
        List<ProtocolMapperRepresentation> reps = new ArrayList<>(List.of(first, second));

        ClientProtocolMapperRegisterContext ctx = new ClientProtocolMapperRegisterContext(
                ContextTestStubs.stubContainer(), reps, ContextTestStubs.stubAdminAuth());
        reps.add(third);

        assertNull(ctx.getProposedProtocolMapper());
        assertEquals(List.of(first, second), ctx.getProposedProtocolMappers());
        assertThrows(UnsupportedOperationException.class, () -> ctx.getProposedProtocolMappers().add(third));
    }
}
