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

package org.keycloak.broker.oid4vp;

import java.util.Map;

import org.keycloak.common.VerificationException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OID4VPVpTokenExtractionTest {

    private static final String CREDENTIAL = "eyJhbGciOiJFUzI1NiJ9.eyJ2Y3QiOiJ4In0.sig~WyJzIiwibiIsInYiXQ~";

    private static final RequestedCredentials REQUESTED = RequestedCredentials.parse("""
            {"credentials": [
              {"id": "pid", "format": "dc+sd-jwt", "types": ["urn:eudi:pid:1"], "trustMaterialIdps": ["trust"]},
              {"id": "membership", "format": "dc+sd-jwt", "types": ["membership"], "trustMaterialIdps": ["trust"]}],
             "credentialSets": [{"options": [["pid"]]}, {"options": [["membership"]], "required": false}],
             "principal": {"credential": "pid", "claim": "personal_administrative_number"}}""");

    @Test
    public void extractsPresentationArrays() throws Exception {
        assertEquals(Map.of("pid", CREDENTIAL, "membership", CREDENTIAL), extract(
                "{\"pid\":[\"" + CREDENTIAL + "\"],\"membership\":[\"" + CREDENTIAL + "\"]}"));
    }

    @Test
    public void extractsPresentationStrings() throws Exception {
        assertEquals(Map.of("pid", CREDENTIAL), extract("{\"pid\":\"" + CREDENTIAL + "\"}"));
    }

    @Test
    public void leavesAbsentCredentialsOut() throws Exception {
        assertEquals(Map.of(), extract("{}"));
    }

    @Test(expected = VerificationException.class)
    public void rejectsBareCompactSdJwt() throws Exception {
        extract(CREDENTIAL);
    }

    @Test(expected = VerificationException.class)
    public void rejectsTopLevelArray() throws Exception {
        extract("[\"" + CREDENTIAL + "\"]");
    }

    @Test(expected = VerificationException.class)
    public void rejectsCredentialThatIsNotRequested() throws Exception {
        extract("{\"pid\":\"" + CREDENTIAL + "\",\"other\":\"" + CREDENTIAL + "\"}");
    }

    @Test(expected = VerificationException.class)
    public void rejectsNonTextualPresentation() throws Exception {
        extract("{\"pid\":123}");
    }

    @Test(expected = VerificationException.class)
    public void rejectsMultiplePresentationsOfOneCredential() throws Exception {
        extract("{\"pid\":[\"" + CREDENTIAL + "\",\"" + CREDENTIAL + "\"]}");
    }

    private static Map<String, String> extract(String vpToken) throws VerificationException {
        return OID4VPIdentityProviderEndpoint.extractPresentations(vpToken, REQUESTED);
    }
}
