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
package org.keycloak.mdoc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MdocCredentialTest {

    private static final String DOC_TYPE = "org.iso.18013.5.1.mDL";
    private static final String NAMESPACE = "org.iso.18013.5.1";

    @Test
    public void shouldExposeNamespacedClaimsFromMapInput() {
        Map<String, Object> namespaceClaims = namespaceClaims();
        Map<String, Object> claims = claims(namespaceClaims);
        MdocValidityInfo validityInfo = validityInfo();

        MdocCredential credential = new MdocCredential(DOC_TYPE, claims, validityInfo);

        assertEquals(DOC_TYPE, credential.getDocType());
        assertEquals(validityInfo, credential.getValidityInfo());
        assertEquals(claims, credential.getClaims());
        assertEquals(1, credential.getNamespacedClaims().size());
        assertEquals(NAMESPACE, credential.getNamespacedClaims().get(0).getNameSpace());
        assertEquals(namespaceClaims, credential.getNamespacedClaims().get(0).getClaims());
    }

    @Test
    public void shouldRejectNamespaceValueThatIsNotObject() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(NAMESPACE, "Erika");

        MdocException exception = assertThrows(MdocException.class,
                () -> new MdocCredential(DOC_TYPE, claims, validityInfo()));

        assertEquals("The value for the name space '" + NAMESPACE + "' is not a JSON object.", exception.getMessage());
    }

    @Test
    public void shouldRejectNonStringElementIdentifier() {
        Map<Object, Object> namespaceClaims = new LinkedHashMap<>();
        namespaceClaims.put(1, "Erika");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(NAMESPACE, namespaceClaims);

        MdocException exception = assertThrows(MdocException.class,
                () -> new MdocCredential(DOC_TYPE, claims, validityInfo()));

        assertEquals("The element identifier for the name space '" + NAMESPACE + "' is not a string.", exception.getMessage());
    }

    @Test
    public void shouldExposeImmutableNamespacedClaims() {
        MdocCredential credential = new MdocCredential(DOC_TYPE, claims(namespaceClaims()), validityInfo());

        assertThrows(UnsupportedOperationException.class, () -> credential.getNamespacedClaims().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> credential.getNamespacedClaims().get(0).getClaims().put("age_over_21", true));
    }

    private static Map<String, Object> claims(Map<String, Object> namespaceClaims) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(NAMESPACE, namespaceClaims);
        return claims;
    }

    private static Map<String, Object> namespaceClaims() {
        Map<String, Object> namespaceClaims = new LinkedHashMap<>();
        namespaceClaims.put("given_name", "Erika");
        namespaceClaims.put("family_name", "Mustermann");
        namespaceClaims.put("age_over_18", true);
        return namespaceClaims;
    }

    private static MdocValidityInfo validityInfo() {
        return MdocValidityInfo.issuedAt(
                Instant.parse("2026-04-24T10:15:30Z"),
                Instant.parse("2027-04-24T10:15:30Z")
        );
    }
}
