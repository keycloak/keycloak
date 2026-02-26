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
package org.keycloak.tests.oid4vc;

import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;

import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;
import static org.keycloak.common.util.KeyUtils.generateEcKeyPair;
import static org.keycloak.util.DIDUtils.decodeDidKey;
import static org.keycloak.util.DIDUtils.encodeDidKey;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the User DID Attribute.
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
@KeycloakIntegrationTest(config = VCTestServerConfig.class)
public class OID4VCUserDidAttributeTest extends OID4VCIssuerTestBase {

    KeyPair subjectKeyPair;
    UserRepresentation holderRep;

    @BeforeEach
    void beforeEach() {

        // Generate the Holder's KeyPair
        subjectKeyPair = generateEcKeyPair(EC_KEY_SECP256R1);

        // Generate the Holder's DID
        ECPublicKey publicKey = (ECPublicKey) subjectKeyPair.getPublic();
        String appUserDid = encodeDidKey(publicKey);

        // Update the Holder's DID attribute
        holderRep = testRealm.admin().users().search("alice").get(0);
        holderRep.getAttributes().put(UserModel.DID, List.of(appUserDid));
        testRealm.admin().users().get(holderRep.getId()).update(holderRep);
    }

    @Test
    public void testDidKeyVerification() {
        holderRep = testRealm.admin().users().search("alice").get(0);
        Map<String, List<String>> holderAttributes = holderRep.getAttributes();
        var appUserDid = holderAttributes.get(UserModel.DID).get(0);
        ECPublicKey publicKey = decodeDidKey(appUserDid);
        assertEquals(subjectKeyPair.getPublic(), publicKey);
    }
}
