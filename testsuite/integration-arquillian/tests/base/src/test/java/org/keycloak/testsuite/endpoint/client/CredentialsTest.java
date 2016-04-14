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

package org.keycloak.testsuite.endpoint.client;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class CredentialsTest extends AbstractClientTest {

    private ClientResource accountClient;

    @Before
    public void init() {
        accountClient = findClientResourceById("account");
    }

    @Test
    public void testGetAndRegenerateSecret() {
        CredentialRepresentation oldCredential = accountClient.getSecret();
        CredentialRepresentation newCredential = accountClient.generateNewSecret();
        assertNotNull(oldCredential);
        assertNotNull(newCredential);
        assertNotEquals(newCredential.getValue(), oldCredential.getValue());
        assertEquals(newCredential.getValue(), accountClient.getSecret().getValue());
    }

    @Test
    public void testGetAndRegenerateRegistrationAccessToken() {
        ClientRepresentation rep = accountClient.toRepresentation();
        String oldToken = rep.getRegistrationAccessToken();
        String newToken = accountClient.regenerateRegistrationAccessToken().getRegistrationAccessToken();
        assertNull(oldToken); // registration access token not saved in ClientRep
        assertNotNull(newToken); // it's only available via regenerateRegistrationAccessToken()
        assertNull(accountClient.toRepresentation().getRegistrationAccessToken());
    }

    @Test
    public void testGetCertificateResource() {
        ClientAttributeCertificateResource certRsc = accountClient.getCertficateResource("jwt.credential");
        CertificateRepresentation cert = certRsc.generate();
        CertificateRepresentation certFromGet = certRsc.getKeyInfo();
        assertEquals(cert.getCertificate(), certFromGet.getCertificate());
        assertEquals(cert.getPrivateKey(), certFromGet.getPrivateKey());
    }
}
