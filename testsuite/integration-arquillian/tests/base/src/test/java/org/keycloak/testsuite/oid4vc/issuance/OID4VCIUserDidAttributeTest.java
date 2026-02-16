/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.oid4vc.issuance;

import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;

import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest;

import org.junit.Before;
import org.junit.Test;

import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;
import static org.keycloak.common.util.KeyUtils.generateEcKeyPair;
import static org.keycloak.util.DIDUtils.decodeDidKey;
import static org.keycloak.util.DIDUtils.encodeDidKey;

import static org.junit.Assert.assertEquals;

/**
 * Tests the User DID Attribute.
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class OID4VCIUserDidAttributeTest extends OID4VCIssuerEndpointTest {

    static class TestContext {
        String usrDid;
        String username;
        KeyPair keyPair;

        TestContext(String username) {
            this.username = username;
        }
    }

    TestContext ctx;

    @Before
    public void setup() {
        super.setup();

        ctx = new TestContext("alice");

        // Generate the Holder's KeyPair
        ctx.keyPair = generateEcKeyPair(EC_KEY_SECP256R1);

        // Generate the Holder's DID
        ECPublicKey publicKey = (ECPublicKey) ctx.keyPair.getPublic();
        ctx.usrDid = encodeDidKey(publicKey);

        // Update the Holder's DID attribute
        UserRepresentation userRepresentation = testRealm().users().search(ctx.username).get(0);
        userRepresentation.getAttributes().put(UserModel.DID, List.of(ctx.usrDid));
        testRealm().users().get(userRepresentation.getId()).update(userRepresentation);
    }

    @Test
    public void testDidKeyVerification() throws Exception {
        UserRepresentation userRepresentation = testRealm().users().search(ctx.username).get(0);
        Map<String, List<String>> userAttributes = userRepresentation.getAttributes();
        var wasDid = userAttributes.get(UserModel.DID).get(0);
        assertEquals(ctx.usrDid, wasDid);

        ECPublicKey wasPublicKey = decodeDidKey(wasDid);
        assertEquals(wasPublicKey, ctx.keyPair.getPublic());
    }
}
