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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;

/**
 * Issuer-side representation of an ISO mdoc credential before it is serialized as IssuerSigned.
 *
 * The input claims are already arranged as namespace -> data element -> value. Signing converts each data element into
 * IssuerSignedItemBytes, hashes those bytes into the Mobile Security Object, optionally embeds the holder DeviceKey,
 * and signs the MSO as the IssuerAuth COSE_Sign1 structure.
 */
public class MdocCredential {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String docType;
    private final Map<String, Object> claims;
    private final MdocValidityInfo validityInfo;
    private MdocDeviceKey deviceKey;

    public MdocCredential(String docType, Map<String, Object> claims, MdocValidityInfo validityInfo) {
        this.docType = Objects.requireNonNull(docType, "docType");
        this.claims = Objects.requireNonNull(claims, "claims");
        this.validityInfo = Objects.requireNonNull(validityInfo, "validityInfo");
    }

    /**
     * Adds holder binding by converting the proof JWK into the COSE_Key stored in DeviceKeyInfo.
     *
     * @param jwk the holder public key from the JWT proof
     */
    public void addKeyBinding(JWK jwk) {
        deviceKey = MdocDeviceKey.fromProofJwk(jwk);
    }

    /**
     * Signs the credential and returns a decoded view of the IssuerSigned structure.
     *
     * @param signerContext signer for the IssuerAuth COSE_Sign1 structure
     * @return decoded view of the signed IssuerSigned structure
     */
    public MdocIssuerSignedDocument signAsIssuerSignedDocument(SignatureSignerContext signerContext) {
        try {
            List<IssuerNameSpace> issuerNameSpaces = buildIssuerNameSpaces();
            MdocAlgorithm algorithm = resolveSigningAlgorithm(signerContext);
            // OID4VCI 1.0 Appendix A.2.4 returns a base64url-encoded ISO mdoc IssuerSigned structure.
            // IssuerAuth is the COSE_Sign1 over the Mobile Security Object inside that structure.
            List<X509Certificate> certificateChain = requireCertificateChain(signerContext.getCertificateChain());
            byte[] payload = buildMobileSecurityObjectBytes(issuerNameSpaces);
            MdocCose.Sign1 issuerAuth = MdocCose.sign1(payload, algorithm, signerContext, certificateChain);
            byte[] issuerSigned = MdocCbor.encode(buildIssuerSigned(issuerNameSpaces, issuerAuth));

            return MdocIssuerSignedDocument.fromIssuerSigned(issuerSigned);
        } catch (MdocException e) {
            throw e;
        } catch (Exception e) {
            throw new MdocException("Could not sign mDoc credential", e);
        }
    }

    public String getDocType() {
        return docType;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public MdocValidityInfo getValidityInfo() {
        return validityInfo;
    }

    private List<IssuerNameSpace> buildIssuerNameSpaces() {
        List<IssuerNameSpace> entries = new ArrayList<>();
        int digestId = 0;

        // OID4VCI mDoc paths are namespace -> data element -> optional nested value; ISO mdoc signs each top-level
        // data element separately and references it from the MSO by digest ID.
        for (Map.Entry<String, Object> namespaceEntry : claims.entrySet()) {
            List<IssuerSignedItemBytes> itemBytesList = buildItemBytesList(
                    namespaceEntry.getKey(),
                    namespaceEntry.getValue(),
                    digestId
            );
            entries.add(new IssuerNameSpace(namespaceEntry.getKey(), itemBytesList));
            digestId += itemBytesList.size();
        }

        return entries;
    }

    private List<IssuerSignedItemBytes> buildItemBytesList(String namespace, Object value, int startingDigestId) {
        if (!(value instanceof Map<?, ?> namespaceClaims)) {
            throw new MdocException("The value for the name space '" + namespace + "' is not a JSON object.");
        }

        List<IssuerSignedItemBytes> itemBytes = new ArrayList<>();
        int digestId = startingDigestId;
        for (Map.Entry<?, ?> claimEntry : namespaceClaims.entrySet()) {
            if (!(claimEntry.getKey() instanceof String elementIdentifier)) {
                throw new MdocException("The element identifier for the name space '" + namespace + "' is not a string.");
            }

            itemBytes.add(buildIssuerSignedItemBytes(digestId++, elementIdentifier, claimEntry.getValue()));
        }
        return itemBytes;
    }

    private IssuerSignedItemBytes buildIssuerSignedItemBytes(int digestId, String elementIdentifier, Object elementValue) {
        // IssuerSignedItemBytes is tag 24 around an encoded IssuerSignedItem; the digest covers those tagged bytes.
        Map<String, Object> issuerSignedItem = new LinkedHashMap<>();
        issuerSignedItem.put("digestID", digestId);
        issuerSignedItem.put("random", generateRandom());
        issuerSignedItem.put("elementIdentifier", elementIdentifier);
        issuerSignedItem.put("elementValue", elementValue);
        MdocCbor.Tagged taggedItem = MdocCbor.encodedCbor(issuerSignedItem);
        return new IssuerSignedItemBytes(digestId, taggedItem, MdocCbor.encode(taggedItem));
    }

    private byte[] buildMobileSecurityObjectBytes(List<IssuerNameSpace> issuerNameSpaces) {
        // MobileSecurityObjectBytes is tag 24 around the encoded MobileSecurityObject.
        return MdocCbor.encode(MdocCbor.encodedCbor(buildMobileSecurityObject(issuerNameSpaces)));
    }

    private Map<String, Object> buildMobileSecurityObject(List<IssuerNameSpace> issuerNameSpaces) {
        // MobileSecurityObject binds value digests, document type, validity, and optional holder key authorization.
        Map<String, Object> mobileSecurityObject = new LinkedHashMap<>();
        mobileSecurityObject.put("version", "1.0");
        mobileSecurityObject.put("digestAlgorithm", "SHA-256");
        mobileSecurityObject.put("valueDigests", buildValueDigests(issuerNameSpaces));
        if (deviceKey != null) {
            mobileSecurityObject.put("deviceKeyInfo", buildDeviceKeyInfo(issuerNameSpaces));
        }
        mobileSecurityObject.put("docType", docType);
        mobileSecurityObject.put("validityInfo", validityInfo.toCborMap());
        return mobileSecurityObject;
    }

    private Map<String, Map<Integer, byte[]>> buildValueDigests(List<IssuerNameSpace> issuerNameSpaces) {
        // valueDigests maps each namespace to digest IDs over IssuerSignedItemBytes.
        Map<String, Map<Integer, byte[]>> valueDigests = new LinkedHashMap<>();
        for (IssuerNameSpace namespaceEntry : issuerNameSpaces) {
            valueDigests.put(namespaceEntry.nameSpace(), buildDigestIds(namespaceEntry.itemBytesList()));
        }
        return valueDigests;
    }

    private Map<Integer, byte[]> buildDigestIds(List<IssuerSignedItemBytes> itemBytesList) {
        // DigestIDs uses integer CBOR map keys, not text keys.
        Map<Integer, byte[]> digestIds = new LinkedHashMap<>();
        for (IssuerSignedItemBytes itemBytes : itemBytesList) {
            digestIds.put(itemBytes.digestId(), computeDigest(itemBytes.encoded()));
        }
        return digestIds;
    }

    private Map<String, Object> buildDeviceKeyInfo(List<IssuerNameSpace> issuerNameSpaces) {
        // OID4VCI 1.0 Section 12.2.4 advertises "cose_key" for credentials bound to COSE_Key. The JWT proof
        // from Appendix F.1 can still carry the wallet key as JOSE; mDoc stores it here as DeviceKeyInfo.COSE_Key.
        Map<String, Object> keyAuthorizations = new LinkedHashMap<>();
        keyAuthorizations.put("nameSpaces", issuerNameSpaces.stream()
                .map(IssuerNameSpace::nameSpace)
                .toList());

        Map<String, Object> deviceKeyInfo = new LinkedHashMap<>();
        deviceKeyInfo.put("deviceKey", deviceKey.toCoseKey());
        deviceKeyInfo.put("keyAuthorizations", keyAuthorizations);
        return deviceKeyInfo;
    }

    private static Map<String, Object> buildIssuerSigned(List<IssuerNameSpace> issuerNameSpaces, MdocCose.Sign1 issuerAuth) {
        // IssuerSigned combines the namespace item arrays with IssuerAuth COSE_Sign1.
        Map<String, Object> issuerSigned = new LinkedHashMap<>();
        Map<String, Object> nameSpaces = new LinkedHashMap<>();
        for (IssuerNameSpace issuerNameSpace : issuerNameSpaces) {
            nameSpaces.put(
                    issuerNameSpace.nameSpace(),
                    issuerNameSpace.itemBytesList().stream()
                            .map(IssuerSignedItemBytes::tagged)
                            .toList()
            );
        }
        issuerSigned.put("nameSpaces", nameSpaces);
        issuerSigned.put("issuerAuth", issuerAuth);
        return issuerSigned;
    }

    private static MdocAlgorithm resolveSigningAlgorithm(SignatureSignerContext signerContext) {
        return Optional.ofNullable(signerContext.getAlgorithm())
                .map(MdocAlgorithm::fromJoseAlgorithm)
                .orElseThrow(() -> new MdocException("mDoc signing key is missing alg"));
    }

    private static List<X509Certificate> requireCertificateChain(List<X509Certificate> certificateChain) {
        if (certificateChain == null || certificateChain.isEmpty()) {
            throw new MdocException("mDoc signing key is missing certificate chain");
        }
        return certificateChain;
    }

    private static byte[] generateRandom() {
        byte[] output = new byte[16];
        RANDOM.nextBytes(output);
        return output;
    }

    private static byte[] computeDigest(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new MdocException("Could not compute SHA-256 digest for mDoc", e);
        }
    }

    private record IssuerNameSpace(String nameSpace, List<IssuerSignedItemBytes> itemBytesList) {
    }

    private record IssuerSignedItemBytes(int digestId, MdocCbor.Tagged tagged, byte[] encoded) {
    }
}
