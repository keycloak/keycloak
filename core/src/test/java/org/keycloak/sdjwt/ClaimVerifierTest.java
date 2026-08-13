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
 *
 */

package org.keycloak.sdjwt;

import org.keycloak.common.VerificationException;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pascal Knueppel
 * @since 17.11.2025
 */
public class ClaimVerifierTest {

    @Test
    public void testValidateEmptyHeader() {
        ClaimVerifier claimVerifier = ClaimVerifier.builder().build();
        try {
            claimVerifier.verifyHeaderClaims(JsonNodeFactory.instance.objectNode());
            Assert.fail("Should have failed with message");
        } catch (VerificationException e) {
            Assert.assertEquals("Missing claim 'alg' in token", e.getMessage());
        }
    }

    @Test
    public void testValidateNoneHeader1() {
        ClaimVerifier claimVerifier = ClaimVerifier.builder().build();
        ObjectNode header = JsonNodeFactory.instance.objectNode();
        header.put("alg", "none");
        try {
            claimVerifier.verifyHeaderClaims(header);
            Assert.fail("Should have failed with message");
        } catch (VerificationException e) {
            Assert.assertEquals("Value 'none' is not allowed for claim 'alg'!", e.getMessage());
        }
    }

    @Test
    public void testValidateNoneHeader2() {
        ClaimVerifier claimVerifier = ClaimVerifier.builder().build();
        ObjectNode header = JsonNodeFactory.instance.objectNode();
        header.put("alg", "NONE");
        try {
            claimVerifier.verifyHeaderClaims(header);
            Assert.fail("Should have failed with message");
        } catch (VerificationException e) {
            Assert.assertEquals("Value 'NONE' is not allowed for claim 'alg'!", e.getMessage());
        }
    }

    @Test
    public void testValidateNoneHeader3() {
        ClaimVerifier claimVerifier = ClaimVerifier.builder().build();
        ObjectNode header = JsonNodeFactory.instance.objectNode();
        header.put("alg", "NonE");
        try {
            claimVerifier.verifyHeaderClaims(header);
            Assert.fail("Should have failed with message");
        } catch (VerificationException e) {
            Assert.assertEquals("Value 'NonE' is not allowed for claim 'alg'!", e.getMessage());
        }
    }
}
