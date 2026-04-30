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

import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.authlete.cose.COSESign1;
import com.authlete.mdoc.IssuerSigned;

/**
 * Parsed view of an ISO mdoc IssuerSigned structure.
 *
 * OID4VCI returns mso_mdoc credentials as base64url-encoded IssuerSigned CBOR. This class keeps that encoded value for
 * the credential response and exposes decoded namespaces and Mobile Security Object fields.
 */
public final class MdocIssuerSignedDocument {

    private static final int CBOR_ENCODED_ITEM_TAG = 24;

    private final String encodedIssuerSigned;
    private final Map<String, Object> namespaces;
    private final Map<String, Object> mobileSecurityObject;
    private final List<X509Certificate> issuerAuthCertificateChain;

    private MdocIssuerSignedDocument(String encodedIssuerSigned,
                                     Map<String, Object> namespaces,
                                     Map<String, Object> mobileSecurityObject,
                                     List<X509Certificate> issuerAuthCertificateChain) {
        this.encodedIssuerSigned = Objects.requireNonNull(encodedIssuerSigned, "encodedIssuerSigned");
        this.namespaces = Objects.requireNonNull(namespaces, "namespaces");
        this.mobileSecurityObject = Objects.requireNonNull(mobileSecurityObject, "mobileSecurityObject");
        this.issuerAuthCertificateChain = issuerAuthCertificateChain == null ? List.of() : issuerAuthCertificateChain;
    }

    /**
     * Decodes a base64url IssuerSigned payload and exposes its namespaces and Mobile Security Object.
     *
     * @param encodedIssuerSigned base64url-encoded IssuerSigned CBOR
     * @return decoded IssuerSigned view
     */
    public static MdocIssuerSignedDocument parse(String encodedIssuerSigned) {
        try {
            byte[] issuerSignedBytes = Base64.getUrlDecoder().decode(encodedIssuerSigned);
            CBORPairList issuerSigned = asPairList(new CBORDecoder(issuerSignedBytes).next(), "issuerSigned");
            Map<String, Object> namespaces = readNamespaces(issuerSigned);
            Map<String, Object> mobileSecurityObject = readMobileSecurityObject(issuerSigned);
            List<X509Certificate> issuerAuthCertificateChain = readIssuerAuthCertificateChain(issuerSigned);
            return new MdocIssuerSignedDocument(encodedIssuerSigned, namespaces, mobileSecurityObject, issuerAuthCertificateChain);
        } catch (MdocException e) {
            throw e;
        } catch (Exception e) {
            throw new MdocException("Could not decode mDoc credential", e);
        }
    }

    static MdocIssuerSignedDocument fromIssuerSigned(IssuerSigned issuerSigned) {
        String encodedIssuerSigned = Base64.getUrlEncoder().withoutPadding().encodeToString(issuerSigned.encode());
        return parse(encodedIssuerSigned);
    }

    public String getEncodedIssuerSigned() {
        return encodedIssuerSigned;
    }

    public Map<String, Object> getNamespaces() {
        return namespaces;
    }

    public Map<String, Object> getMobileSecurityObject() {
        return mobileSecurityObject;
    }

    public List<X509Certificate> getIssuerAuthCertificateChain() {
        return issuerAuthCertificateChain;
    }

    public String getDocType() {
        Object docType = mobileSecurityObject.get("docType");
        return docType instanceof String ? (String) docType : null;
    }

    private static Map<String, Object> readNamespaces(CBORPairList issuerSigned) throws Exception {
        Map<String, Object> namespaces = new LinkedHashMap<>();
        CBORPairList namespacePairs = asPairList(issuerSigned.findByKey("nameSpaces").getValue(), "nameSpaces");
        for (CBORPair namespacePair : namespacePairs.getPairs()) {
            String namespace = asString(namespacePair.getKey().parse(), "namespace");
            if (!(namespacePair.getValue() instanceof CBORItemList namespaceElements)) {
                throw new MdocException("Unexpected namespace items structure for " + namespace);
            }

            Map<String, Object> namespaceClaims = new LinkedHashMap<>();
            for (CBORItem namespaceElement : namespaceElements.getItems()) {
                Map<String, Object> issuerSignedItem = asMap(unwrapTag24(namespaceElement).parse(), "issuerSignedItem");
                String elementIdentifier = asString(issuerSignedItem.get("elementIdentifier"), "elementIdentifier");
                namespaceClaims.put(elementIdentifier, issuerSignedItem.get("elementValue"));
            }
            namespaces.put(namespace, namespaceClaims);
        }
        return namespaces;
    }

    private static Map<String, Object> readMobileSecurityObject(CBORPairList issuerSigned) throws Exception {
        COSESign1 issuerAuth = COSESign1.build(issuerSigned.findByKey("issuerAuth").getValue());
        CBORItem payload = issuerAuth.getPayload();
        byte[] payloadBytes = payload instanceof CBORByteArray payloadByteArray ? payloadByteArray.getValue() : payload.encode();
        CBORItem decodedPayload = new CBORDecoder(payloadBytes).next();

        if (decodedPayload instanceof CBORTaggedItem taggedItem
                && taggedItem.getTagNumber().intValue() == CBOR_ENCODED_ITEM_TAG
                && taggedItem.getTagContent() instanceof CBORByteArray taggedPayload) {
            decodedPayload = new CBORDecoder(taggedPayload.getValue()).next();
        }

        return asMap(decodedPayload.parse(), "mobileSecurityObject");
    }

    private static List<X509Certificate> readIssuerAuthCertificateChain(CBORPairList issuerSigned) throws Exception {
        return COSESign1.build(issuerSigned.findByKey("issuerAuth").getValue()).getUnprotectedHeader().getX5Chain();
    }

    private static CBORPairList unwrapTag24(CBORItem item) throws Exception {
        // CBOR tag 24 means the byte string contains an encoded CBOR data item. ISO mdoc wraps
        // IssuerSignedItemBytes and MSO bytes this way, so parser callers need to decode the nested item.
        if (item instanceof CBORTaggedItem taggedItem
                && taggedItem.getTagNumber().intValue() == CBOR_ENCODED_ITEM_TAG
                && taggedItem.getTagContent() instanceof CBORByteArray taggedPayload) {
            return asPairList(new CBORDecoder(taggedPayload.getValue()).next(), "tag24");
        }
        if (item instanceof CBORByteArray byteArray) {
            return asPairList(new CBORDecoder(byteArray.getValue()).next(), "issuerSignedItem");
        }
        return asPairList(item, "issuerSignedItem");
    }

    private static CBORPairList asPairList(CBORItem item, String name) {
        if (item instanceof CBORPairList pairList) {
            return pairList;
        }
        throw new MdocException("Unexpected CBOR structure for " + name);
    }

    private static String asString(Object object, String name) {
        if (object instanceof String string) {
            return string;
        }
        throw new MdocException("Unexpected string structure for " + name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object object, String name) {
        if (object instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new MdocException("Unexpected map structure for " + name);
    }
}
