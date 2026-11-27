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

import java.util.List;
import java.util.Set;

import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class RequestedCredentialsTest {

    private static final String CREDENTIALS = """
            [{"id": "pid", "format": "dc+sd-jwt", "types": ["t"], "trustMaterialIdps": ["a"]},
             {"id": "membership", "format": "dc+sd-jwt", "types": ["u"], "trustMaterialIdps": ["a"]},
             {"id": "mdl", "format": "dc+sd-jwt", "types": ["v"], "trustMaterialIdps": ["a"]}]""";

    @Test
    public void parsesCredentialsAndPrincipal() {
        RequestedCredentials requested = RequestedCredentials.parse("""
                {"credentials": [
                  {"id": "pid", "format": "dc+sd-jwt", "types": ["urn:eudi:pid:1"], "trustMaterialIdps": ["pid-trust"]},
                  {"id": "membership", "format": "dc+sd-jwt", "types": ["membership"], "trustMaterialIdps": ["a", "b"]}],
                 "credentialSets": [{"options": [["pid"]]}, {"options": [["membership"]], "required": false}],
                 "principal": {"credential": "pid", "claim": "address.locality"}}""");

        assertEquals(2, requested.credentials().size());
        assertEquals(List.of("a", "b"), requested.credential("membership").orElseThrow().trustMaterialIdps());
        assertEquals(List.of(List.of("membership")), requested.credentialSets().get(1).options());
        assertEquals(Boolean.FALSE, requested.credentialSets().get(1).required());
        assertEquals(List.of("address", "locality"), requested.principal().claimPath().toClaimsPathPointer());
    }

    @Test
    public void serializedRequestedCredentialsParseAgain() throws Exception {
        RequestedCredentials requested = withSets("[{\"options\": [[\"pid\", \"membership\"], [\"pid\", \"mdl\"]]}]", "pid");

        assertEquals(requested, RequestedCredentials.parse(JsonSerialization.writeValueAsString(requested)));
    }

    @Test
    public void withoutCredentialSetsAllCredentialsAreRequired() {
        RequestedCredentials requested = withSets(null, "pid");

        assertTrue(requested.isSatisfiedBy(Set.of("pid", "membership", "mdl")));
        assertFalse(requested.isSatisfiedBy(Set.of("pid", "membership")));
    }

    @Test
    public void credentialSetsRequireOneOptionOfEveryRequiredSet() {
        RequestedCredentials requested = withSets("""
                [{"options": [["pid", "membership"], ["pid", "mdl"]]}, {"options": [["membership"]], "required": false}]""",
                "pid");

        assertTrue(requested.isSatisfiedBy(Set.of("pid", "mdl")));
        assertTrue(requested.isSatisfiedBy(Set.of("pid", "membership")));
        assertFalse(requested.isSatisfiedBy(Set.of("pid")));
        assertFalse(requested.isSatisfiedBy(Set.of("membership", "mdl")));
    }

    @Test
    public void rejectsInvalidCredentialSets() {
        assertRejectedSets("[]", "The credential sets must not be empty if present");
        assertRejectedSets("[{\"options\": []}]", "Every credential set needs at least one option");
        assertRejectedSets("[{\"options\": [[]]}]",
                "Every option of a credential set needs at least one credential and no duplicates");
        assertRejectedSets("[{\"options\": [[\"pid\", \"pid\", \"membership\", \"mdl\"]]}]",
                "Every option of a credential set needs at least one credential and no duplicates");
        assertRejectedSets("[{\"options\": [[\"pid\", \"membership\", \"mdl\", \"other\"]]}]",
                "A credential set refers to the unknown credential 'other'");
        assertRejectedSets("[{\"options\": [[\"pid\", \"membership\"]]}]",
                "The credential 'mdl' is not part of any credential set");
    }

    @Test
    public void requiresAtLeastOneRequiredCredentialSet() {
        assertRejectedSets("[{\"options\": [[\"pid\", \"membership\", \"mdl\"]], \"required\": false}]",
                "At least one credential set must be required");
    }

    @Test
    public void principalIsAlwaysPresentedIfPartOfEveryOptionOfARequiredSet() {
        assertTrue(withSets(null, "pid").isPrincipalAlwaysPresented());
        assertTrue(withSets("[{\"options\": [[\"pid\"]]}, {\"options\": [[\"membership\"], [\"mdl\"]], \"required\": false}]",
                "pid").isPrincipalAlwaysPresented());
        assertFalse(withSets("[{\"options\": [[\"pid\", \"membership\"], [\"pid\", \"mdl\"], [\"membership\", \"mdl\"]]}]",
                "pid").isPrincipalAlwaysPresented());
        assertTrue(withSets("[{\"options\": [[\"pid\"]]}, {\"options\": [[\"membership\"], [\"mdl\"]]}]",
                "pid").isPrincipalAlwaysPresented());
        assertFalse(withSets("[{\"options\": [[\"pid\"], [\"membership\"]]}, {\"options\": [[\"mdl\"]]}]",
                "pid").isPrincipalAlwaysPresented());
    }

    @Test
    public void rejectsMissingJson() {
        assertRejected(null, "The requested credentials are required");
        assertRejected(" ", "The requested credentials are required");
        assertRejected("null", "The requested credentials are required");
    }

    @Test
    public void rejectsMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> RequestedCredentials.parse("{"));
    }

    @Test
    public void rejectsUnknownMembers() {
        assertRejected("""
                {"credentials": [{"id": "pid", "format": "dc+sd-jwt", "types": ["t"], "trustMaterialIdps": ["a"],
                  "vct_values": ["t"]}],
                 "principal": {"credential": "pid", "claim": "sub"}}""",
                "The requested credentials member 'vct_values' is not supported");
    }

    @Test
    public void rejectsInvalidCredentials() {
        assertRejected("""
                {"credentials": [], "principal": {"credential": "pid", "claim": "sub"}}""",
                "At least one credential must be requested");
        assertRejected(credential("{\"id\": \"p i d\", \"format\": \"dc+sd-jwt\", \"types\": [\"t\"], \"trustMaterialIdps\": [\"a\"]}"),
                "The credential id 'p i d' must consist of letters, digits, '_' and '-'");
        assertRejected(credential("{\"id\": \"pid\", \"format\": \"mso_mdoc\", \"types\": [\"t\"], \"trustMaterialIdps\": [\"a\"]}"),
                "The format 'mso_mdoc' of the credential 'pid' is not supported, only dc+sd-jwt is");
        assertRejected(credential("{\"id\": \"pid\", \"format\": \"dc+sd-jwt\", \"types\": [\" \"], \"trustMaterialIdps\": [\"a\"]}"),
                "The credential 'pid' needs at least one type and no blank ones");
        assertRejected(credential("{\"id\": \"pid\", \"format\": \"dc+sd-jwt\", \"types\": [\"t\"]}"),
                "The credential 'pid' needs at least one trust material identity provider and no blank ones");
    }

    @Test
    public void rejectsDuplicateCredentialIds() {
        assertRejected("""
                {"credentials": [
                  {"id": "pid", "format": "dc+sd-jwt", "types": ["t"], "trustMaterialIdps": ["a"]},
                  {"id": "pid", "format": "dc+sd-jwt", "types": ["u"], "trustMaterialIdps": ["a"]}],
                 "principal": {"credential": "pid", "claim": "sub"}}""",
                "The credential id 'pid' is not unique");
    }

    @Test
    public void rejectsInvalidPrincipal() {
        assertRejected("{\"credentials\": " + CREDENTIALS + "}", "The principal credential is required");
        assertRejected("{\"credentials\": " + CREDENTIALS + ", \"principal\": {\"credential\": \"other\", \"claim\": \"sub\"}}",
                "The principal refers to the unknown credential 'other'");
        assertRejected("{\"credentials\": " + CREDENTIALS + ", \"principal\": {\"credential\": \"pid\", \"claim\": \"a[x]\"}}",
                "The principal claim 'a[x]' is not a valid claim path");
    }

    @Test
    public void rejectsPrincipalTypeExtendingAnotherWithHash() {
        String json = """
                {"credentials": [{"id": "pid", "format": "dc+sd-jwt", "types": ["urn:pid", "urn:pid#v2"], "trustMaterialIdps": ["a"]}],
                 "principal": {"credential": "pid", "claim": "sub"}}""";
        assertRejected(json, "The principal credential type 'urn:pid#v2' must not start with the type 'urn:pid' followed by '#'");
        assertEquals(List.of("urn:pid#v2"),
                RequestedCredentials.parse(json.replace("\"urn:pid\", ", "")).credentials().get(0).types());
    }

    private static RequestedCredentials withSets(String credentialSets, String principal) {
        return RequestedCredentials.parse(setsJson(credentialSets, principal));
    }

    private static String setsJson(String credentialSets, String principal) {
        return "{\"credentials\": " + CREDENTIALS
                + (credentialSets == null ? "" : ", \"credentialSets\": " + credentialSets)
                + ", \"principal\": {\"credential\": \"" + principal + "\", \"claim\": \"sub\"}}";
    }

    private static void assertRejectedSets(String credentialSets, String message) {
        assertRejected(setsJson(credentialSets, "pid"), message);
    }

    private static String credential(String credential) {
        return "{\"credentials\": [" + credential + "], \"principal\": {\"credential\": \"pid\", \"claim\": \"sub\"}}";
    }

    private static void assertRejected(String json, String message) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> RequestedCredentials.parse(json));
        assertEquals(message, e.getMessage());
    }
}
