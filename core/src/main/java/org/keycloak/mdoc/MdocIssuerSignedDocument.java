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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed view of an ISO mdoc IssuerSigned structure.
 *
 * OID4VCI returns mso_mdoc credentials as base64url-encoded IssuerSigned CBOR. This class keeps that encoded value for
 * the credential response and exposes decoded namespaces and Mobile Security Object fields.
 */
public final class MdocIssuerSignedDocument {

    private static final int COSE_X5CHAIN_HEADER = 33;

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
        this.issuerAuthCertificateChain = issuerAuthCertificateChain == null ? Collections.emptyList() : issuerAuthCertificateChain;
    }

    /**
     * Decodes a base64url IssuerSigned payload and exposes its namespaces and Mobile Security Object.
     *
     * @param encodedIssuerSigned base64url-encoded IssuerSigned CBOR
     * @return decoded IssuerSigned view
     */
    public static MdocIssuerSignedDocument parse(String encodedIssuerSigned) {
        byte[] issuerSignedBytes = Base64.getUrlDecoder().decode(encodedIssuerSigned);
        return parse(encodedIssuerSigned, issuerSignedBytes);
    }

    static MdocIssuerSignedDocument fromIssuerSigned(byte[] issuerSigned) {
        String encodedIssuerSigned = Base64.getUrlEncoder().withoutPadding().encodeToString(issuerSigned);
        return parse(encodedIssuerSigned, issuerSigned);
    }

    private static MdocIssuerSignedDocument parse(String encodedIssuerSigned, byte[] issuerSignedBytes) {
        try {
            Map<Object, Object> issuerSigned = CborUtil.asMap(CborUtil.decode(issuerSignedBytes), "issuerSigned");
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

    private static Map<String, Object> readNamespaces(Map<Object, Object> issuerSigned) {
        Map<String, Object> namespaces = new LinkedHashMap<>();
        Map<Object, Object> namespacePairs = CborUtil.asMap(issuerSigned.get("nameSpaces"), "nameSpaces");
        for (Map.Entry<Object, Object> namespacePair : namespacePairs.entrySet()) {
            String namespace = CborUtil.asString(namespacePair.getKey(), "namespace");
            List<Object> namespaceElements = CborUtil.asList(namespacePair.getValue(), "namespace items");

            Map<String, Object> namespaceClaims = new LinkedHashMap<>();
            for (Object namespaceElement : namespaceElements) {
                Map<Object, Object> issuerSignedItem = CborUtil.asMap(CborUtil.unwrapEncodedCbor(namespaceElement), "issuerSignedItem");
                String elementIdentifier = CborUtil.asString(issuerSignedItem.get("elementIdentifier"), "elementIdentifier");
                namespaceClaims.put(elementIdentifier, issuerSignedItem.get("elementValue"));
            }
            namespaces.put(namespace, namespaceClaims);
        }
        return namespaces;
    }

    private static Map<String, Object> readMobileSecurityObject(Map<Object, Object> issuerSigned) {
        MdocCose.ParsedSign1 issuerAuth = readIssuerAuth(issuerSigned);
        return CborUtil.asStringKeyMap(CborUtil.unwrapEncodedCbor(CborUtil.decode(issuerAuth.payload())), "mobileSecurityObject");
    }

    private static List<X509Certificate> readIssuerAuthCertificateChain(Map<Object, Object> issuerSigned) {
        MdocCose.ParsedSign1 issuerAuth = readIssuerAuth(issuerSigned);
        Object x5chain = issuerAuth.header(COSE_X5CHAIN_HEADER);
        if (x5chain == null) {
            return Collections.emptyList();
        }
        if (x5chain instanceof byte[]) {
            return Collections.singletonList(decodeCertificate((byte[]) x5chain));
        }
        List<X509Certificate> certificates = new ArrayList<>();
        for (Object certificate : CborUtil.asList(x5chain, "issuerAuth x5chain")) {
            certificates.add(decodeCertificate(CborUtil.asByteArray(certificate, "issuerAuth x5chain certificate")));
        }
        return certificates;
    }

    private static MdocCose.ParsedSign1 readIssuerAuth(Map<Object, Object> issuerSigned) {
        return MdocCose.Sign1.fromCbor(issuerSigned.get("issuerAuth"), "issuerAuth");
    }

    private static X509Certificate decodeCertificate(byte[] encoded) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(encoded));
        } catch (CertificateException e) {
            throw new MdocException("Could not decode mDoc issuer certificate", e);
        }
    }

}
