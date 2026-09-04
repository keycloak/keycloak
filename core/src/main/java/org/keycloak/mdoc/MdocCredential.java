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

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.crypto.HashUtils;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representation of an ISO mdoc credential before it is serialized as IssuerSigned.
 *
 * The input claims are already arranged as mdoc namespaces. Signing converts each data element into
 * IssuerSignedItemBytes, hashes those bytes into the Mobile Security Object, optionally embeds the holder DeviceKey,
 * and signs the MSO as the IssuerAuth COSE_Sign1 structure.
 */
public class MdocCredential {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String docType;
    private final List<MdocNamespacedClaims> nameSpaces;
    private final MdocValidityInfo validityInfo;
    private MdocDeviceKey deviceKey;

    public MdocCredential(String docType, Map<String, Object> claims, MdocValidityInfo validityInfo) {
        this(docType, toNamespacedClaims(claims), validityInfo);
    }

    public MdocCredential(String docType, List<MdocNamespacedClaims> nameSpaces, MdocValidityInfo validityInfo) {
        this.docType = Objects.requireNonNull(docType, "docType");
        this.nameSpaces = new ArrayList<>(Objects.requireNonNull(nameSpaces, "nameSpaces"));
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
            byte[] issuerSigned = CborUtil.encode(buildIssuerSigned(issuerNameSpaces, issuerAuth));

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
        Map<String, Object> claims = new LinkedHashMap<>();
        for (MdocNamespacedClaims nameSpace : nameSpaces) {
            claims.put(nameSpace.getNameSpace(), nameSpace.getClaims());
        }
        return claims;
    }

    public List<MdocNamespacedClaims> getNamespacedClaims() {
        return Collections.unmodifiableList(nameSpaces);
    }

    public MdocValidityInfo getValidityInfo() {
        return validityInfo;
    }

    private List<IssuerNameSpace> buildIssuerNameSpaces() {
        List<IssuerNameSpace> entries = new ArrayList<>();
        int digestId = 0;

        // OID4VCI mDoc paths are namespace -> data element -> optional nested value; ISO mdoc signs each top-level
        // data element separately and references it from the MSO by digest ID.
        for (MdocNamespacedClaims nameSpace : nameSpaces) {
            List<IssuerSignedItemBytes> itemBytesList = buildItemBytesList(
                    nameSpace,
                    digestId
            );
            entries.add(new IssuerNameSpace(nameSpace.getNameSpace(), itemBytesList));
            digestId += itemBytesList.size();
        }

        return entries;
    }

    private List<IssuerSignedItemBytes> buildItemBytesList(MdocNamespacedClaims nameSpace, int startingDigestId) {
        List<IssuerSignedItemBytes> itemBytes = new ArrayList<>();
        int digestId = startingDigestId;
        for (Map.Entry<String, Object> claimEntry : nameSpace.getClaims().entrySet()) {
            String elementIdentifier = claimEntry.getKey();
            itemBytes.add(buildIssuerSignedItemBytes(digestId++, elementIdentifier, claimEntry.getValue()));
        }
        return itemBytes;
    }

    private IssuerSignedItemBytes buildIssuerSignedItemBytes(int digestId, String elementIdentifier, Object elementValue) {
        // IssuerSignedItemBytes is tag 24 around an encoded IssuerSignedItem; the digest covers those tagged bytes.
        IssuerSignedItem issuerSignedItem = new IssuerSignedItem(digestId, generateRandom(), elementIdentifier, elementValue);
        CborUtil.Tagged taggedItem = CborUtil.encodedCbor(issuerSignedItem);
        return new IssuerSignedItemBytes(digestId, taggedItem, CborUtil.encode(taggedItem));
    }

    private byte[] buildMobileSecurityObjectBytes(List<IssuerNameSpace> issuerNameSpaces) {
        // MobileSecurityObjectBytes is tag 24 around the encoded MobileSecurityObject.
        return CborUtil.encode(CborUtil.encodedCbor(buildMobileSecurityObject(issuerNameSpaces)));
    }

    private MobileSecurityObject buildMobileSecurityObject(List<IssuerNameSpace> issuerNameSpaces) {
        // MobileSecurityObject binds value digests, document type, validity, and optional holder key authorization.
        return new MobileSecurityObject(
                buildValueDigests(issuerNameSpaces),
                deviceKey == null ? null : buildDeviceKeyInfo(issuerNameSpaces),
                docType,
                validityInfo
        );
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

    private DeviceKeyInfo buildDeviceKeyInfo(List<IssuerNameSpace> issuerNameSpaces) {
        // OID4VCI 1.0 Section 12.2.4 advertises "cose_key" for credentials bound to COSE_Key. The JWT proof
        // from Appendix F.1 can still carry the wallet key as JOSE; mDoc stores it here as DeviceKeyInfo.COSE_Key.
        return new DeviceKeyInfo(
                deviceKey,
                new KeyAuthorizations(issuerNameSpaces.stream()
                        .map(IssuerNameSpace::nameSpace)
                        .collect(Collectors.toList()))
        );
    }

    private static IssuerSigned buildIssuerSigned(List<IssuerNameSpace> issuerNameSpaces, MdocCose.Sign1 issuerAuth) {
        // IssuerSigned combines the namespace item arrays with IssuerAuth COSE_Sign1.
        Map<String, Object> nameSpaces = new LinkedHashMap<>();
        for (IssuerNameSpace issuerNameSpace : issuerNameSpaces) {
            nameSpaces.put(
                    issuerNameSpace.nameSpace(),
                    issuerNameSpace.itemBytesList().stream()
                            .map(IssuerSignedItemBytes::tagged)
                            .collect(Collectors.toList())
            );
        }
        return new IssuerSigned(nameSpaces, issuerAuth);
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
        return HashUtils.hash(JavaAlgorithm.SHA256, value);
    }

    private static List<MdocNamespacedClaims> toNamespacedClaims(Map<String, Object> claims) {
        Objects.requireNonNull(claims, "claims");
        List<MdocNamespacedClaims> nameSpaces = new ArrayList<>();
        for (Map.Entry<String, Object> nameSpaceEntry : claims.entrySet()) {
            nameSpaces.add(MdocNamespacedClaims.fromEntry(nameSpaceEntry.getKey(), nameSpaceEntry.getValue()));
        }
        return nameSpaces;
    }

    private static final class IssuerSigned {

        private final Map<String, Object> nameSpaces;
        private final MdocCose.Sign1 issuerAuth;

        IssuerSigned(Map<String, Object> nameSpaces, MdocCose.Sign1 issuerAuth) {
            this.nameSpaces = nameSpaces;
            this.issuerAuth = issuerAuth;
        }

        @JsonValue
        Map<String, Object> toCborMap() {
            Map<String, Object> issuerSigned = new LinkedHashMap<>();
            issuerSigned.put("nameSpaces", nameSpaces);
            issuerSigned.put("issuerAuth", issuerAuth);
            return issuerSigned;
        }
    }

    private static final class IssuerSignedItem {

        private final int digestId;
        private final byte[] random;
        private final String elementIdentifier;
        private final Object elementValue;

        IssuerSignedItem(int digestId, byte[] random, String elementIdentifier, Object elementValue) {
            this.digestId = digestId;
            this.random = random;
            this.elementIdentifier = elementIdentifier;
            this.elementValue = elementValue;
        }

        @JsonValue
        Map<String, Object> toCborMap() {
            Map<String, Object> issuerSignedItem = new LinkedHashMap<>();
            issuerSignedItem.put("digestID", digestId);
            issuerSignedItem.put("random", random);
            issuerSignedItem.put("elementIdentifier", elementIdentifier);
            issuerSignedItem.put("elementValue", elementValue);
            return issuerSignedItem;
        }
    }

    private static final class MobileSecurityObject {

        private final Map<String, Map<Integer, byte[]>> valueDigests;
        private final DeviceKeyInfo deviceKeyInfo;
        private final String docType;
        private final MdocValidityInfo validityInfo;

        MobileSecurityObject(Map<String, Map<Integer, byte[]>> valueDigests,
                             DeviceKeyInfo deviceKeyInfo,
                             String docType,
                             MdocValidityInfo validityInfo) {
            this.valueDigests = valueDigests;
            this.deviceKeyInfo = deviceKeyInfo;
            this.docType = docType;
            this.validityInfo = validityInfo;
        }

        @JsonValue
        Map<String, Object> toCborMap() {
            Map<String, Object> mobileSecurityObject = new LinkedHashMap<>();
            mobileSecurityObject.put("version", "1.0");
            mobileSecurityObject.put("digestAlgorithm", "SHA-256");
            mobileSecurityObject.put("valueDigests", valueDigests);
            if (deviceKeyInfo != null) {
                mobileSecurityObject.put("deviceKeyInfo", deviceKeyInfo);
            }
            mobileSecurityObject.put("docType", docType);
            mobileSecurityObject.put("validityInfo", validityInfo.toCborMap());
            return mobileSecurityObject;
        }
    }

    private static final class DeviceKeyInfo {

        private final MdocDeviceKey deviceKey;
        private final KeyAuthorizations keyAuthorizations;

        DeviceKeyInfo(MdocDeviceKey deviceKey, KeyAuthorizations keyAuthorizations) {
            this.deviceKey = deviceKey;
            this.keyAuthorizations = keyAuthorizations;
        }

        @JsonValue
        Map<String, Object> toCborMap() {
            Map<String, Object> deviceKeyInfo = new LinkedHashMap<>();
            deviceKeyInfo.put("deviceKey", deviceKey.toCoseKey());
            deviceKeyInfo.put("keyAuthorizations", keyAuthorizations);
            return deviceKeyInfo;
        }
    }

    private static final class KeyAuthorizations {

        private final List<String> nameSpaces;

        KeyAuthorizations(List<String> nameSpaces) {
            this.nameSpaces = nameSpaces;
        }

        @JsonValue
        Map<String, Object> toCborMap() {
            Map<String, Object> keyAuthorizations = new LinkedHashMap<>();
            keyAuthorizations.put("nameSpaces", nameSpaces);
            return keyAuthorizations;
        }
    }

    private static final class IssuerNameSpace {

        private final String nameSpace;
        private final List<IssuerSignedItemBytes> itemBytesList;

        IssuerNameSpace(String nameSpace, List<IssuerSignedItemBytes> itemBytesList) {
            this.nameSpace = nameSpace;
            this.itemBytesList = itemBytesList;
        }

        String nameSpace() {
            return nameSpace;
        }

        List<IssuerSignedItemBytes> itemBytesList() {
            return itemBytesList;
        }
    }

    private static final class IssuerSignedItemBytes {

        private final int digestId;
        private final CborUtil.Tagged tagged;
        private final byte[] encoded;

        IssuerSignedItemBytes(int digestId, CborUtil.Tagged tagged, byte[] encoded) {
            this.digestId = digestId;
            this.tagged = tagged;
            this.encoded = encoded;
        }

        int digestId() {
            return digestId;
        }

        CborUtil.Tagged tagged() {
            return tagged;
        }

        byte[] encoded() {
            return encoded;
        }
    }
}
