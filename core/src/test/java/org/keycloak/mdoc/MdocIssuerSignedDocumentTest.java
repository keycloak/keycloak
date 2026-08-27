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

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MdocIssuerSignedDocumentTest {

    private static final String DOC_TYPE = "org.iso.18013.5.1.mDL";
    private static final String NAMESPACE = "org.iso.18013.5.1";

    @Test
    public void shouldParseIssuerSignedRepresentation() {
        String encodedIssuerSigned = encodedIssuerSigned();

        MdocIssuerSignedDocument document = MdocIssuerSignedDocument.parse(encodedIssuerSigned);

        assertEquals(encodedIssuerSigned, document.getEncodedIssuerSigned());
        assertEquals(DOC_TYPE, document.getDocType());
        assertEquals(Collections.emptyList(), document.getIssuerAuthCertificateChain());

        assertTrue(document.getNamespaces().get(NAMESPACE) instanceof Map);
        Map<?, ?> namespaceClaims = (Map<?, ?>) document.getNamespaces().get(NAMESPACE);
        assertEquals("Erika", namespaceClaims.get("given_name"));
        assertTrue((Boolean) namespaceClaims.get("age_over_18"));

        assertEquals("1.0", document.getMobileSecurityObject().get("version"));
        assertEquals("SHA-256", document.getMobileSecurityObject().get("digestAlgorithm"));
        assertEquals(DOC_TYPE, document.getMobileSecurityObject().get("docType"));
    }

    @Test
    public void shouldRejectMalformedIssuerSignedRepresentation() {
        String encodedIssuerSigned = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(CborUtil.encode(Collections.singletonMap("nameSpaces", "not-a-map")));

        MdocException exception = assertThrows(MdocException.class,
                () -> MdocIssuerSignedDocument.parse(encodedIssuerSigned));

        assertEquals("Unexpected map structure for nameSpaces", exception.getMessage());
    }

    private static String encodedIssuerSigned() {
        Map<String, Object> issuerSigned = new LinkedHashMap<>();
        issuerSigned.put("nameSpaces", namespaces());
        issuerSigned.put("issuerAuth", issuerAuth());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(CborUtil.encode(issuerSigned));
    }

    private static Map<String, Object> namespaces() {
        Map<String, Object> namespaceClaims = new LinkedHashMap<>();
        namespaceClaims.put(NAMESPACE, Arrays.asList(
                CborUtil.encodedCbor(issuerSignedItem(0, "given_name", "Erika")),
                CborUtil.encodedCbor(issuerSignedItem(1, "age_over_18", true))
        ));
        return namespaceClaims;
    }

    private static Map<String, Object> issuerSignedItem(int digestId, String elementIdentifier, Object elementValue) {
        Map<String, Object> issuerSignedItem = new LinkedHashMap<>();
        issuerSignedItem.put("digestID", digestId);
        issuerSignedItem.put("random", new byte[] { (byte) digestId });
        issuerSignedItem.put("elementIdentifier", elementIdentifier);
        issuerSignedItem.put("elementValue", elementValue);
        return issuerSignedItem;
    }

    private static MdocCose.Sign1 issuerAuth() {
        return new MdocCose.Sign1(
                new byte[0],
                Collections.emptyMap(),
                CborUtil.encode(CborUtil.encodedCbor(mobileSecurityObject())),
                new byte[] { 1, 2, 3 }
        );
    }

    private static Map<String, Object> mobileSecurityObject() {
        Map<String, Object> mobileSecurityObject = new LinkedHashMap<>();
        mobileSecurityObject.put("version", "1.0");
        mobileSecurityObject.put("digestAlgorithm", "SHA-256");
        mobileSecurityObject.put("valueDigests", Collections.singletonMap(NAMESPACE, Collections.emptyMap()));
        mobileSecurityObject.put("docType", DOC_TYPE);
        return mobileSecurityObject;
    }
}
