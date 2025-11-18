/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.sdjwt.vp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.sdjwt.SdJwtVerificationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM;
import static org.keycloak.OID4VCConstants.SDJWT_DELIMITER;
import static org.keycloak.OID4VCConstants.SD_HASH;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwtVP {
    private final String sdJwtVpString;
    private final IssuerSignedJWT issuerSignedJWT;

    private final Map<String, ArrayNode> claims;
    private final Map<String, String> disclosures;
    private final Map<String, String> recursiveDigests;
    private final List<String> ghostDigests;
    private final String hashAlgorithm;

    private final Optional<KeyBindingJWT> keyBindingJWT;
    private final SdJwtVerificationContext sdJwtVerificationContext;

    public Map<String, ArrayNode> getClaims() {
        return claims;
    }

    public IssuerSignedJWT getIssuerSignedJWT() {
        return issuerSignedJWT;
    }

    public Map<String, String> getDisclosures() {
        return disclosures;
    }

    public Collection<String> getDisclosuresString() {
        return disclosures.values();
    }

    public Map<String, String> getRecursiveDigests() {
        return recursiveDigests;
    }

    public Collection<String> getGhostDigests() {
        return ghostDigests;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public Optional<KeyBindingJWT> getKeyBindingJWT() {
        return keyBindingJWT;
    }

    private SdJwtVP(String sdJwtVpString, String hashAlgorithm, IssuerSignedJWT issuerSignedJWT,
            Map<String, ArrayNode> claims, Map<String, String> disclosures, Map<String, String> recursiveDigests,
            List<String> ghostDigests, Optional<KeyBindingJWT> keyBindingJWT) {
        this.sdJwtVpString = sdJwtVpString;
        this.hashAlgorithm = hashAlgorithm;
        this.issuerSignedJWT = issuerSignedJWT;
        this.claims = Collections.unmodifiableMap(claims);
        this.disclosures = Collections.unmodifiableMap(disclosures);
        this.recursiveDigests = Collections.unmodifiableMap(recursiveDigests);
        this.ghostDigests = Collections.unmodifiableList(ghostDigests);
        this.keyBindingJWT = keyBindingJWT;

        // Instantiate context for verification
        this.sdJwtVerificationContext = new SdJwtVerificationContext(
                this.sdJwtVpString,
                this.issuerSignedJWT,
                this.disclosures,
                this.keyBindingJWT.orElse(null)
        );
    }

    public static SdJwtVP of(String sdJwtString) {
        int disclosureStart = sdJwtString.indexOf(SDJWT_DELIMITER);
        int disclosureEnd = sdJwtString.lastIndexOf(SDJWT_DELIMITER);

        if (disclosureStart == -1) {
            throw new IllegalArgumentException("SD-JWT is malformed, expected to contain a '" + SDJWT_DELIMITER + "'");
        }

        String issuerSignedJWTString = sdJwtString.substring(0, disclosureStart);
        String disclosuresString = "";

        if (disclosureEnd > disclosureStart) {
            disclosuresString = sdJwtString.substring(disclosureStart + 1, disclosureEnd);
        }

        IssuerSignedJWT issuerSignedJWT = IssuerSignedJWT.fromJws(issuerSignedJWTString);

        ObjectNode issuerPayload = (ObjectNode) issuerSignedJWT.getPayload();
        String hashAlgorithm = Optional.ofNullable(issuerPayload.get(CLAIM_NAME_SD_HASH_ALGORITHM))
                .map(JsonNode::asText)
                .orElse(JavaAlgorithm.SHA256.toLowerCase());

        Map<String, ArrayNode> claims = new HashMap<>();
        Map<String, String> disclosures = new HashMap<>();

        List<String> split = Arrays.stream(disclosuresString.split(SDJWT_DELIMITER))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        for (String disclosure : split) {
            String disclosureDigest = SdJwtUtils.hashAndBase64EncodeNoPad(disclosure.getBytes(), hashAlgorithm);
            if (disclosures.containsKey(disclosureDigest)) {
                throw new IllegalArgumentException("Duplicate disclosure digest");
            }
            disclosures.put(disclosureDigest, disclosure);
            ArrayNode disclosureData;
            try {
                disclosureData = (ArrayNode) SdJwtUtils.mapper.readTree(Base64Url.decode(disclosure));
                claims.put(disclosureDigest, disclosureData);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid disclosure data");
            }
        }
        Set<String> allDigests = claims.keySet();

        Map<String, String> recursiveDigests = new HashMap<>();
        List<String> ghostDigests = new ArrayList<>();
        allDigests.stream()
                .forEach(disclosureDigest -> {
                    JsonNode node = findNode(issuerPayload, disclosureDigest);
                    node = processDisclosureDigest(node, disclosureDigest, claims, recursiveDigests, ghostDigests);
                });

        Optional<KeyBindingJWT> keyBindingJWT = Optional.empty();
        if (sdJwtString.length() > disclosureEnd + 1) {
            String keyBindingJWTString = sdJwtString.substring(disclosureEnd + 1);
            keyBindingJWT = Optional.of(KeyBindingJWT.of(keyBindingJWTString));
        }

        // Drop the key binding String if any. As it is held by the keyBindingJwtObject
        String sdJWtVPString = sdJwtString.substring(0, disclosureEnd + 1);

        return new SdJwtVP(sdJWtVPString, hashAlgorithm, issuerSignedJWT, claims, disclosures, recursiveDigests,
                ghostDigests, keyBindingJWT);

    }

    private static JsonNode processDisclosureDigest(JsonNode node, String disclosureDigest,
            Map<String, ArrayNode> claims,
            Map<String, String> recursiveDigests,
            List<String> ghostDigests) {
        if (node == null) { // digest is nested in another disclosure
            Set<Entry<String, ArrayNode>> entrySet = claims.entrySet();
            for (Entry<String, ArrayNode> entry : entrySet) {
                if (entry.getKey().equals(disclosureDigest)) {
                    continue;
                }
                node = findNode(entry.getValue(), disclosureDigest);
                if (node != null) {
                    recursiveDigests.put(disclosureDigest, entry.getKey());
                    break;
                }
            }
        }
        if (node == null) { // No digest found for disclosure.
            ghostDigests.add(disclosureDigest);
        }
        return node;
    }

    public JsonNode getCnfClaim() {
        return issuerSignedJWT.getCnfClaim().orElse(null);
    }

    public String present(List<String> disclosureDigests, JsonNode keyBindingClaims,
            SignatureSignerContext holdSignatureSignerContext, String jwsType) {
        StringBuilder sb = new StringBuilder();
        if (disclosureDigests == null || disclosureDigests.isEmpty()) {
            // disclose everything
            sb.append(sdJwtVpString);
        } else {
            sb.append(issuerSignedJWT.toJws());
            sb.append(SDJWT_DELIMITER);
            for (String disclosureDigest : disclosureDigests) {
                sb.append(disclosures.get(disclosureDigest));
                sb.append(SDJWT_DELIMITER);
            }
        }
        String unboundPresentation = sb.toString();
        if (keyBindingClaims == null || holdSignatureSignerContext == null) {
            return unboundPresentation;
        }
        String sd_hash = SdJwtUtils.hashAndBase64EncodeNoPad(unboundPresentation.getBytes(), getHashAlgorithm());
        keyBindingClaims = ((ObjectNode) keyBindingClaims).put(SD_HASH, sd_hash);
        KeyBindingJWT keyBindingJWT = KeyBindingJWT.from(keyBindingClaims, holdSignatureSignerContext, jwsType);
        sb.append(keyBindingJWT.toJws());
        return sb.toString();
    }

    /**
     * Verifies SD-JWT presentation.
     *
     * @param issuerVerifyingKeys             Verifying keys for validating the Issuer-signed JWT. The caller
     *                                        is responsible for establishing trust in that the keys belong
     *                                        to the intended issuer.
     * @param issuerSignedJwtVerificationOpts Options to parameterize the Issuer-Signed JWT verification.
     * @param keyBindingJwtVerificationOpts   Options to parameterize the Key Binding JWT verification.
     *                                        Must, among others, specify the Verifier's policy whether
     *                                        to check Key Binding.
     * @throws VerificationException if verification failed
     */
    public void verify(
            List<SignatureVerifierContext> issuerVerifyingKeys,
            IssuerSignedJwtVerificationOpts issuerSignedJwtVerificationOpts,
            KeyBindingJwtVerificationOpts keyBindingJwtVerificationOpts
    ) throws VerificationException {
        sdJwtVerificationContext.verifyPresentation(
                issuerVerifyingKeys,
                issuerSignedJwtVerificationOpts,
                keyBindingJwtVerificationOpts,
                null
        );
    }

    /**
     * Retrieve verification context for advanced scenarios.
     */
    public SdJwtVerificationContext getSdJwtVerificationContext() {
        return sdJwtVerificationContext;
    }

    // Recursively searches the node with the given value.
    // Returns the node if found, null otherwise.
    private static JsonNode findNode(JsonNode node, String value) {
        if (node == null) {
            return null;
        }
        if (node.isValueNode()) {
            if (node.asText().equals(value)) {
                return node;
            } else {
                return null;
            }
        }
        if (node.isArray() || node.isObject()) {
            for (JsonNode child : node) {
                JsonNode found = findNode(child, value);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return sdJwtVpString;
    }

    public String verbose() {
        StringBuilder sb = new StringBuilder();
        sb.append("Issuer Signed JWT: ");
        sb.append(issuerSignedJWT.getPayload());
        sb.append("\n");
        disclosures.forEach((digest, disclosure) -> {
            sb.append("\n");
            sb.append("Digest: ");
            sb.append(digest);
            sb.append("\n");
            sb.append("Disclosure: ");
            sb.append(disclosure);
            sb.append("\n");
            sb.append("Content: ");
            sb.append(claims.get(digest));
            sb.append("\n");
        });
        sb.append("\n");
        sb.append("Recursive Digests: ");
        sb.append(recursiveDigests);
        sb.append("\n");
        sb.append("\n");
        sb.append("Ghost Digests: ");
        sb.append(ghostDigests);
        sb.append("\n");
        sb.append("\n");
        if (keyBindingJWT.isPresent()) {
            sb.append("Key Binding JWT: ");
            sb.append("\n");
            sb.append(keyBindingJWT.get().getPayload().toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
