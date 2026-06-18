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

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ClientProtocolMapperRemoveContextTest {

    @Test
    public void getEventReturnsUnregisterProtocolMapper() {
        ClientProtocolMapperRemoveContext ctx = new ClientProtocolMapperRemoveContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperModel(), ContextTestStubs.stubAdminAuth());
        assertEquals(ClientPolicyEvent.UNREGISTER_PROTOCOL_MAPPER, ctx.getEvent());
    }

    @Test
    public void getExistingProtocolMapperReturnsConstructorArg() {
        ProtocolMapperModel existing = new ProtocolMapperModel();
        ClientProtocolMapperRemoveContext ctx = new ClientProtocolMapperRemoveContext(
                ContextTestStubs.stubContainer(), existing, ContextTestStubs.stubAdminAuth());
        assertSame(existing, ctx.getExistingProtocolMapper());
    }

    @Test
    public void getProposedProtocolMapperIsNullOnRemove() {
        ClientProtocolMapperRemoveContext ctx = new ClientProtocolMapperRemoveContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperModel(), ContextTestStubs.stubAdminAuth());
        assertNull(ctx.getProposedProtocolMapper());
    }

    @Test
    public void getProtocolMapperContainerReturnsConstructorArg() {
        var container = ContextTestStubs.stubContainer();
        ClientProtocolMapperRemoveContext ctx = new ClientProtocolMapperRemoveContext(
                container, new ProtocolMapperModel(), ContextTestStubs.stubAdminAuth());
        assertSame(container, ctx.getProtocolMapperContainer());
    }

    @Test
    public void authenticatedUserDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        ClientProtocolMapperRemoveContext ctx = new ClientProtocolMapperRemoveContext(
                ContextTestStubs.stubContainer(), new ProtocolMapperModel(), auth);
        assertSame(auth.getUser(), ctx.getAuthenticatedUser());
    }
}
