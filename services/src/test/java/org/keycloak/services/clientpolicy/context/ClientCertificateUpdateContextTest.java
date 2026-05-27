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

import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ClientCertificateUpdateContextTest {

    @Test
    public void getEventReturnsUpdateClientCertificate() {
        ClientCertificateUpdateContext ctx = new ClientCertificateUpdateContext(
                ContextTestStubs.stubClient(), "jwt.credential", new CertificateRepresentation(), ContextTestStubs.stubAdminAuth());
        assertEquals(ClientPolicyEvent.UPDATE_CLIENT_CERTIFICATE, ctx.getEvent());
    }

    @Test
    public void getTargetClientReturnsConstructorArg() {
        var client = ContextTestStubs.stubClient();
        ClientCertificateUpdateContext ctx = new ClientCertificateUpdateContext(
                client, "jwt.credential", new CertificateRepresentation(), ContextTestStubs.stubAdminAuth());
        assertSame(client, ctx.getTargetClient());
    }

    @Test
    public void getAttributePrefixReturnsConstructorArg() {
        ClientCertificateUpdateContext ctx = new ClientCertificateUpdateContext(
                ContextTestStubs.stubClient(), "saml.signing", new CertificateRepresentation(), ContextTestStubs.stubAdminAuth());
        assertEquals("saml.signing", ctx.getAttributePrefix());
    }

    @Test
    public void getProposedCertificateReturnsSanitizedCopy() {
        CertificateRepresentation cert = new CertificateRepresentation();
        cert.setCertificate("certificate");
        cert.setPrivateKey("private-key");
        ClientCertificateUpdateContext ctx = new ClientCertificateUpdateContext(
                ContextTestStubs.stubClient(), "jwt.credential", cert, ContextTestStubs.stubAdminAuth());

        CertificateRepresentation proposed = ctx.getProposedCertificate();
        proposed.setCertificate("changed");

        assertNotSame(cert, proposed);
        assertEquals("changed", proposed.getCertificate());
        assertNull(proposed.getPrivateKey());
        assertEquals("certificate", ctx.getProposedCertificate().getCertificate());
        assertNull(ctx.getProposedCertificate().getPrivateKey());
    }

    @Test
    public void authenticatedUserDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        ClientCertificateUpdateContext ctx = new ClientCertificateUpdateContext(
                ContextTestStubs.stubClient(), "jwt.credential", new CertificateRepresentation(), auth);
        assertSame(auth.getUser(), ctx.getAuthenticatedUser());
    }

    @Test
    public void authenticatedClientDelegatesToAdminAuth() {
        var auth = ContextTestStubs.stubAdminAuth();
        ClientCertificateUpdateContext ctx = new ClientCertificateUpdateContext(
                ContextTestStubs.stubClient(), "jwt.credential", new CertificateRepresentation(), auth);
        assertSame(auth.getClient(), ctx.getAuthenticatedClient());
    }
}
