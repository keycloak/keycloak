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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORizer;
import com.authlete.cose.COSEException;
import com.authlete.cose.COSEProtectedHeader;
import com.authlete.cose.COSEProtectedHeaderBuilder;
import com.authlete.cose.COSESign1;
import com.authlete.cose.COSESign1Builder;
import com.authlete.cose.COSEUnprotectedHeader;
import com.authlete.cose.COSEUnprotectedHeaderBuilder;
import com.authlete.cose.SigStructure;
import com.authlete.cose.SigStructureBuilder;
import com.authlete.cose.SigStructureSigner;
import com.authlete.mdoc.AuthorizedNameSpaces;
import com.authlete.mdoc.DeviceKeyInfo;
import com.authlete.mdoc.DigestIDs;
import com.authlete.mdoc.DigestIDsEntry;
import com.authlete.mdoc.IssuerNameSpaces;
import com.authlete.mdoc.IssuerNameSpacesEntry;
import com.authlete.mdoc.IssuerSigned;
import com.authlete.mdoc.IssuerSignedItem;
import com.authlete.mdoc.IssuerSignedItemBytes;
import com.authlete.mdoc.KeyAuthorizations;
import com.authlete.mdoc.MobileSecurityObject;
import com.authlete.mdoc.MobileSecurityObjectBytes;
import com.authlete.mdoc.ValueDigests;
import com.authlete.mdoc.ValueDigestsEntry;

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
     * Signs the credential and returns the OID4VCI base64url-encoded IssuerSigned payload.
     *
     * @param signerContext signer for the IssuerAuth COSE_Sign1 structure
     * @return base64url-encoded IssuerSigned CBOR
     */
    public String sign(SignatureSignerContext signerContext) {
        return signAsIssuerSignedDocument(signerContext).getEncodedIssuerSigned();
    }

    /**
     * Signs the credential and returns a decoded view of the IssuerSigned structure.
     *
     * @param signerContext signer for the IssuerAuth COSE_Sign1 structure
     * @return decoded view of the signed IssuerSigned structure
     */
    public MdocIssuerSignedDocument signAsIssuerSignedDocument(SignatureSignerContext signerContext) {
        try {
            IssuerNameSpaces issuerNameSpaces = buildIssuerNameSpaces();
            MdocAlgorithm algorithm = resolveSigningAlgorithm(signerContext);
            // OID4VCI 1.0 Appendix A.2.4 returns a base64url-encoded ISO mdoc IssuerSigned structure.
            // IssuerAuth is the COSE_Sign1 over the Mobile Security Object inside that structure.
            COSESign1 issuerAuth = buildIssuerAuth(
                    issuerNameSpaces,
                    signerContext,
                    signerContext.getCertificateChain(),
                    algorithm
            );
            IssuerSigned issuerSigned = new IssuerSigned(issuerNameSpaces, issuerAuth);

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

    private IssuerNameSpaces buildIssuerNameSpaces() {
        List<IssuerNameSpacesEntry> entries = new ArrayList<>();
        CBORizer cborizer = new CBORizer();
        int digestId = 0;

        // OID4VCI 1.0 Appendix C.2 maps mDoc paths as namespace -> data element identifier -> optional nested value.
        // ISO mdoc signs each data element as IssuerSignedItemBytes and references it from the MSO by digest ID.
        for (Map.Entry<String, Object> namespaceEntry : claims.entrySet()) {
            List<IssuerSignedItemBytes> itemBytesList = buildItemBytesList(
                    namespaceEntry.getKey(),
                    namespaceEntry.getValue(),
                    cborizer,
                    digestId
            );
            entries.add(new IssuerNameSpacesEntry(namespaceEntry.getKey(), itemBytesList));
            digestId += itemBytesList.size();
        }

        return new IssuerNameSpaces(entries);
    }

    private List<IssuerSignedItemBytes> buildItemBytesList(String namespace, Object value, CBORizer cborizer, int startingDigestId) {
        if (!(value instanceof Map<?, ?> namespaceClaims)) {
            throw new MdocException("The value for the name space '" + namespace + "' is not a JSON object.");
        }

        List<IssuerSignedItemBytes> itemBytes = new ArrayList<>();
        int digestId = startingDigestId;
        for (Map.Entry<?, ?> claimEntry : namespaceClaims.entrySet()) {
            if (!(claimEntry.getKey() instanceof String elementIdentifier)) {
                throw new MdocException("The element identifier for the name space '" + namespace + "' is not a string.");
            }

            CBORItem elementValue = cborizer.cborize(claimEntry.getValue());
            IssuerSignedItem issuerSignedItem = new IssuerSignedItem(
                    digestId++,
                    generateRandom(),
                    elementIdentifier,
                    elementValue
            );
            itemBytes.add(new IssuerSignedItemBytes(issuerSignedItem));
        }
        return itemBytes;
    }

    private COSESign1 buildIssuerAuth(IssuerNameSpaces issuerNameSpaces,
                                      SignatureSignerContext signerContext,
                                      List<X509Certificate> certificateChain,
                                      MdocAlgorithm algorithm) throws Exception {
        int coseAlgorithm = algorithm.getCoseAlgorithmIdentifier();
        COSEProtectedHeader protectedHeader = new COSEProtectedHeaderBuilder()
                .alg(coseAlgorithm)
                .build();
        // ISO mdoc verifiers resolve the issuer authentication key from the COSE x5chain header.
        // Without it, the IssuerAuth signature may be well-formed COSE but is not usable as an mDoc issuer signature.
        COSEUnprotectedHeader unprotectedHeader = new COSEUnprotectedHeaderBuilder()
                .x5chain(requireCertificateChain(certificateChain))
                .build();
        CBORByteArray payload = prepareIssuerAuthPayload(issuerNameSpaces);
        SigStructure sigStructure = new SigStructureBuilder()
                .signature1()
                .bodyAttributes(protectedHeader)
                .payload(payload)
                .build();
        byte[] signature = new KeycloakSigStructureSigner(signerContext).sign(sigStructure, coseAlgorithm);

        return new COSESign1Builder()
                .protectedHeader(protectedHeader)
                .unprotectedHeader(unprotectedHeader)
                .payload(payload)
                .signature(signature)
                .build();
    }

    private CBORByteArray prepareIssuerAuthPayload(IssuerNameSpaces issuerNameSpaces) {
        MobileSecurityObjectBytes mobileSecurityObjectBytes = buildMobileSecurityObjectBytes(issuerNameSpaces);
        return new CBORByteArray(mobileSecurityObjectBytes.encode(), mobileSecurityObjectBytes);
    }

    private MobileSecurityObjectBytes buildMobileSecurityObjectBytes(IssuerNameSpaces issuerNameSpaces) {
        return new MobileSecurityObjectBytes(buildMobileSecurityObject(issuerNameSpaces));
    }

    private MobileSecurityObject buildMobileSecurityObject(IssuerNameSpaces issuerNameSpaces) {
        return new MobileSecurityObject(
                buildValueDigests(issuerNameSpaces),
                buildDeviceKeyInfo(issuerNameSpaces),
                docType,
                validityInfo.toAuthleteValidityInfo()
        );
    }

    private ValueDigests buildValueDigests(IssuerNameSpaces issuerNameSpaces) {
        List<ValueDigestsEntry> entries = new ArrayList<>();
        for (CBORPair pair : issuerNameSpaces.getPairs()) {
            IssuerNameSpacesEntry namespaceEntry = (IssuerNameSpacesEntry) pair;
            entries.add(new ValueDigestsEntry(
                    namespaceEntry.getNameSpace(),
                    buildDigestIds(namespaceEntry.getIssuerSignedItemBytesList())
            ));
        }
        return new ValueDigests(entries);
    }

    private DigestIDs buildDigestIds(List<? extends IssuerSignedItemBytes> itemBytesList) {
        List<DigestIDsEntry> entries = new ArrayList<>();
        for (IssuerSignedItemBytes itemBytes : itemBytesList) {
            entries.add(new DigestIDsEntry(
                    itemBytes.getIssuerSignedItem().getDigestID(),
                    computeDigest(itemBytes.encode())
            ));
        }
        return new DigestIDs(entries);
    }

    private DeviceKeyInfo buildDeviceKeyInfo(IssuerNameSpaces issuerNameSpaces) {
        if (deviceKey == null) {
            return null;
        }

        // OID4VCI 1.0 Section 12.2.4 advertises "cose_key" for credentials bound to COSE_Key. The JWT proof
        // from Appendix F.1 can still carry the wallet key as JOSE; mDoc stores it here as DeviceKeyInfo.COSE_Key.
        return new DeviceKeyInfo(
                deviceKey.toAuthleteCoseKey(),
                new KeyAuthorizations(
                        new AuthorizedNameSpaces(extractNamespaceNames(issuerNameSpaces)),
                        null
                ),
                null
        );
    }

    private static Collection<String> extractNamespaceNames(IssuerNameSpaces issuerNameSpaces) {
        List<String> namespaces = new ArrayList<>();
        for (CBORPair pair : issuerNameSpaces.getPairs()) {
            namespaces.add(((IssuerNameSpacesEntry) pair).getNameSpace().parse());
        }
        return namespaces;
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

    private static final class KeycloakSigStructureSigner implements SigStructureSigner {

        private final SignatureSignerContext signerContext;

        private KeycloakSigStructureSigner(SignatureSignerContext signerContext) {
            this.signerContext = signerContext;
        }

        @Override
        public byte[] sign(SigStructure sigStructure, int algorithm) throws COSEException {
            try {
                return signerContext.sign(sigStructure.encode());
            } catch (Exception e) {
                throw new COSEException(e.getMessage(), e);
            }
        }
    }
}
