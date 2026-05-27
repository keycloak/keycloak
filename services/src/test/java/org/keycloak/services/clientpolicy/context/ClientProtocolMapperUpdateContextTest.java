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

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ClientProtocolMapperUpdateContextTest {

    @Test
    public void getEventReturnsUpdateProtocolMapper() {
        ClientProtocolMapperUpdateContext ctx = new ClientProtocolMapperUpdateContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperRepresentation(), new ProtocolMapperModel(), ContextTestStubs.stubAdminAuth());
        assertEquals(ClientPolicyEvent.UPDATE_PROTOCOL_MAPPER, ctx.getEvent());
    }

    @Test
    public void getProposedProtocolMapperReturnsConstructorArg() {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        ClientProtocolMapperUpdateContext ctx = new ClientProtocolMapperUpdateContext(
                ContextTestStubs.stubContainer(), rep, new ProtocolMapperModel(), ContextTestStubs.stubAdminAuth());
        assertSame(rep, ctx.getProposedProtocolMapper());
    }

    @Test
    public void getExistingProtocolMapperReturnsConstructorArg() {
        ProtocolMapperModel existing = new ProtocolMapperModel();
        ClientProtocolMapperUpdateContext ctx = new ClientProtocolMapperUpdateContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperRepresentation(), existing, ContextTestStubs.stubAdminAuth());
        assertSame(existing, ctx.getExistingProtocolMapper());
    }

    @Test
    public void getProtocolMapperContainerReturnsConstructorArg() {
        var container = ContextTestStubs.stubContainer();
        ClientProtocolMapperUpdateContext ctx = new ClientProtocolMapperUpdateContext(
                container, new ProtocolMapperRepresentation(), new ProtocolMapperModel(), ContextTestStubs.stubAdminAuth());
        assertSame(container, ctx.getProtocolMapperContainer());
    }

    @Test
    public void authenticatedUserDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        ClientProtocolMapperUpdateContext ctx = new ClientProtocolMapperUpdateContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperRepresentation(), new ProtocolMapperModel(), auth);
        assertSame(auth.getUser(), ctx.getAuthenticatedUser());
    }
}
