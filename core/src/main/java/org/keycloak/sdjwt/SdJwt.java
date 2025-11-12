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
package org.keycloak.sdjwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.sdjwt.vp.KeyBindingJWT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM;

/**
 * Main entry class for selective disclosure jwt (SD-JWT).
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwt {

    private final IssuerSignedJWT issuerSignedJWT;
    private final List<SdJwtClaim> claims;
    private final List<String> disclosures = new ArrayList<>();
    private final SdJwtVerificationContext sdJwtVerificationContext;

    private SdJwt(DisclosureSpec disclosureSpec, JsonNode claimSet, List<SdJwt> nesteSdJwts,
                  Optional<KeyBindingJWT> keyBindingJWT,
                  SignatureSignerContext signer,
                  String hashAlgorithm,
                  String jwsType) {
        claims = new ArrayList<>();
        claimSet.fields()
                .forEachRemaining(entry -> claims.add(createClaim(entry.getKey(), entry.getValue(), disclosureSpec)));

        this.issuerSignedJWT = IssuerSignedJWT.builder()
                .withClaims(claims)
                .withDecoyClaims(createdDecoyClaims(disclosureSpec))
                .withNestedDisclosures(!nesteSdJwts.isEmpty())
                .withSigner(signer)
                .withHashAlg(hashAlgorithm)
                .withJwsType(jwsType)
                .build();

        nesteSdJwts.stream().forEach(nestedJwt -> this.disclosures.addAll(nestedJwt.getDisclosures()));
        this.disclosures.addAll(getDisclosureStrings(claims));

        // Instantiate context for verification
        this.sdJwtVerificationContext = new SdJwtVerificationContext(
                this.issuerSignedJWT,
                this.disclosures
        );
    }

    private Optional<String> sdJwtString = Optional.empty();

    private List<DecoyClaim> createdDecoyClaims(DisclosureSpec disclosureSpec) {
        return disclosureSpec.getDecoyClaims().stream()
                .map(disclosureData -> DecoyClaim.builder().withSalt(disclosureData.getSalt()).build())
                .collect(Collectors.toList());
    }

    /**
     * Prepare to a nested payload to this SD-JWT.
     * <p>
     * dropping the algo claim.
     *
     * @param nestedSdJwt
     * @return
     */
    public JsonNode asNestedPayload() {
        JsonNode nestedPayload = issuerSignedJWT.getPayload();
        ((ObjectNode) nestedPayload).remove(CLAIM_NAME_SD_HASH_ALGORITHM);
        return nestedPayload;
    }

    public String toSdJwtString() {
        List<String> parts = new ArrayList<>();

        parts.add(issuerSignedJWT.toJws());
        parts.addAll(disclosures);
        parts.add("");

        return String.join(OID4VCConstants.SDJWT_DELIMITER, parts);
    }

    private static List<String> getDisclosureStrings(List<SdJwtClaim> claims) {
        List<String> disclosureStrings = new ArrayList<>();
        claims.stream()
                .map(SdJwtClaim::getDisclosureStrings)
                .forEach(disclosureStrings::addAll);
        return Collections.unmodifiableList(disclosureStrings);
    }

    @Override
    public String toString() {
        return sdJwtString.orElseGet(() -> {
            String sdString = toSdJwtString();
            sdJwtString = Optional.of(sdString);
            return sdString;
        });
    }

    private SdJwtClaim createClaim(String claimName, JsonNode claimValue, DisclosureSpec disclosureSpec) {
        DisclosureSpec.DisclosureData disclosureData = disclosureSpec.getUndisclosedClaim(SdJwtClaimName.of(claimName));

        if (disclosureData != null) {
            return createUndisclosedClaim(claimName, claimValue, disclosureData.getSalt());
        } else {
            return createArrayOrVisibleClaim(claimName, claimValue, disclosureSpec);
        }
    }

    private SdJwtClaim createUndisclosedClaim(String claimName, JsonNode claimValue, SdJwtSalt salt) {
        return UndisclosedClaim.builder()
                .withClaimName(claimName)
                .withClaimValue(claimValue)
                .withSalt(salt)
                .build();
    }

    private SdJwtClaim createArrayOrVisibleClaim(String claimName, JsonNode claimValue, DisclosureSpec disclosureSpec) {
        SdJwtClaimName sdJwtClaimName = SdJwtClaimName.of(claimName);
        Map<Integer, DisclosureSpec.DisclosureData> undisclosedArrayElts = disclosureSpec
                .getUndisclosedArrayElts(sdJwtClaimName);
        Map<Integer, DisclosureSpec.DisclosureData> decoyArrayElts = disclosureSpec.getDecoyArrayElts(sdJwtClaimName);

        if (undisclosedArrayElts != null || decoyArrayElts != null) {
            return createArrayDisclosure(claimName, claimValue, undisclosedArrayElts, decoyArrayElts);
        } else {
            return VisibleSdJwtClaim.builder()
                    .withClaimName(claimName)
                    .withClaimValue(claimValue)
                    .build();
        }
    }

    private SdJwtClaim createArrayDisclosure(String claimName, JsonNode claimValue,
                                             Map<Integer, DisclosureSpec.DisclosureData> undisclosedArrayElts,
                                             Map<Integer, DisclosureSpec.DisclosureData> decoyArrayElts) {
        ArrayNode arrayNode = validateArrayNode(claimName, claimValue);
        ArrayDisclosure.Builder arrayDisclosureBuilder = ArrayDisclosure.builder().withClaimName(claimName);

        if (undisclosedArrayElts != null) {
            IntStream.range(0, arrayNode.size())
                    .forEach(i -> processArrayElement(arrayDisclosureBuilder, arrayNode.get(i),
                            undisclosedArrayElts.get(i)));
        }

        if (decoyArrayElts != null) {
            decoyArrayElts.entrySet().stream()
                    .forEach(e -> arrayDisclosureBuilder.withDecoyElt(e.getKey(), e.getValue().getSalt()));
        }

        return arrayDisclosureBuilder.build();
    }

    private ArrayNode validateArrayNode(String claimName, JsonNode claimValue) {
        return Optional.of(claimValue)
                .filter(v -> v.getNodeType() == JsonNodeType.ARRAY)
                .map(v -> (ArrayNode) v)
                .orElseThrow(
                        () -> new IllegalArgumentException("Expected array for claim with name: " + claimName));
    }

    private void processArrayElement(ArrayDisclosure.Builder builder, JsonNode elementValue,
                                     DisclosureSpec.DisclosureData disclosureData) {
        if (disclosureData != null) {
            builder.withUndisclosedElement(disclosureData.getSalt(), elementValue);
        } else {
            builder.withVisibleElement(elementValue);
        }
    }

    public IssuerSignedJWT getIssuerSignedJWT() {
        return issuerSignedJWT;
    }

    public List<String> getDisclosures() {
        return disclosures;
    }

    /**
     * Verifies SD-JWT as to whether the Issuer-signed JWT's signature and disclosures are valid.
     *
     * @param issuerVerifyingKeys Verifying keys for validating the Issuer-signed JWT. The caller
     *                            is responsible for establishing trust in that the keys belong
     *                            to the intended issuer.
     * @param verificationOpts    Options to parameterize the Issuer-Signed JWT verification.
     * @throws VerificationException if verification failed
     */
    public void verify(
            List<SignatureVerifierContext> issuerVerifyingKeys,
            IssuerSignedJwtVerificationOpts verificationOpts
    ) throws VerificationException {
        sdJwtVerificationContext.verifyIssuance(
                issuerVerifyingKeys,
                verificationOpts,
                null
        );
    }

    // builder for SdJwt
    public static class Builder {
        private DisclosureSpec disclosureSpec;
        private JsonNode claimSet;
        private Optional<KeyBindingJWT> keyBindingJWT = Optional.empty();
        private SignatureSignerContext signer;
        private final List<SdJwt> nestedSdJwts = new ArrayList<>();
        private String hashAlgorithm;
        private String jwsType;

        public Builder withDisclosureSpec(DisclosureSpec disclosureSpec) {
            this.disclosureSpec = disclosureSpec;
            return this;
        }

        public Builder withClaimSet(JsonNode claimSet) {
            this.claimSet = claimSet;
            return this;
        }

        public Builder withKeyBindingJWT(KeyBindingJWT keyBindingJWT) {
            this.keyBindingJWT = Optional.of(keyBindingJWT);
            return this;
        }

        public Builder withSigner(SignatureSignerContext signer) {
            this.signer = signer;
            return this;
        }

        public Builder withNestedSdJwt(SdJwt nestedSdJwt) {
            nestedSdJwts.add(nestedSdJwt);
            return this;
        }

        public Builder withHashAlgorithm(String hashAlgorithm) {
            this.hashAlgorithm = hashAlgorithm;
            return this;
        }

        public Builder withJwsType(String jwsType) {
            this.jwsType = jwsType;
            return this;
        }

        public SdJwt build() {
            return new SdJwt(disclosureSpec, claimSet, nestedSdJwts, keyBindingJWT, signer, hashAlgorithm, jwsType);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
