package org.keycloak.sdjwt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSInput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handle verifiable credentials (SD-JWT VC), enabling the parsing
 * of existing VCs as well as the creation and signing of new ones.
 * It integrates with Keycloak's SignatureSignerContext to facilitate
 * the generation of issuer signature.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public class IssuerSignedJWT extends SdJws {

    public static IssuerSignedJWT fromJws(String jwsString) {
        return new IssuerSignedJWT(jwsString);
    }

    public IssuerSignedJWT toSignedJWT(SignatureSignerContext signer) {
        JWSInput jwsInput = sign(getPayload(), signer);
        return new IssuerSignedJWT(getPayload(), jwsInput);
    }

    private IssuerSignedJWT(String jwsString) {
        super(jwsString);
    }

    private IssuerSignedJWT(List<SdJwtClaim> claims, String hashAlg) {
        super(generatePayloadString(claims, hashAlg));
    }

    private IssuerSignedJWT(JsonNode payload, JWSInput jwsInput) {
        super(payload, jwsInput);
    }

    private IssuerSignedJWT(List<SdJwtClaim> claims, String hashAlg, SignatureSignerContext signer) {
        super(generatePayloadString(claims, hashAlg), signer);
    }

    /*
     * Generates the payload of the issuer signed jwt from the list
     * of claims.
     */
    private static JsonNode generatePayloadString(List<SdJwtClaim> claims, String hashAlg) {

        SdJwtUtils.requireNonEmpty(hashAlg, "hashAlg must not be null or empty");
        final List<SdJwtClaim> claimsInternal = claims == null ? Collections.emptyList()
                : Collections.unmodifiableList(claims);

        try {
            // Check no dupplicate claim names
            claimsInternal.stream()
                    .filter(Objects::nonNull)
                    // is any duplicate, toMap will throw IllegalStateException
                    .collect(Collectors.toMap(SdJwtClaim::getClaimName, claim -> claim));
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("claims must not contain duplicate claim names", e);
        }

        ArrayNode sdArray = SdJwtUtils.mapper.createArrayNode();
        // first filter all UndisclosedClaim
        // then sort by salt
        // then push digest into the sdArray
        claimsInternal.stream()
                .filter(claim -> claim instanceof UndisclosedClaim)
                .map(claim -> (UndisclosedClaim) claim)
                .collect(Collectors.toMap(UndisclosedClaim::getSalt, claim -> claim))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .map(od -> od.getDisclosureDigest(hashAlg))
                .sorted()
                .forEach(sdArray::add);

        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();

        if (sdArray.size() > 0) {
            // drop _sd claim if empty
            payload.set(CLAIM_NAME_SELECTIVE_DISCLOSURE, sdArray);
            // No need to keep hash alg in here as there is no deigest
            // in the payload. ToDo: raise issue with spec.
            payload.put(CLAIM_NAME_SD_HASH_ALGORITHM, hashAlg);
        }

        // then put all other claims in the paypload
        // Disclosure of array of elements is handled
        // by the corresponding claim object.
        claimsInternal.stream()
                .filter(Objects::nonNull)
                .filter(claim -> !(claim instanceof UndisclosedClaim))
                .forEach(nullableClaim -> {
                    SdJwtClaim claim = Objects.requireNonNull(nullableClaim);
                    payload.set(claim.getClaimNameAsString(), claim.getVisibleClaimValue(hashAlg));
                });

        return payload;
    }

    // SD JWT Claims
    private static final String CLAIM_NAME_SELECTIVE_DISCLOSURE = "_sd";
    private static final String CLAIM_NAME_SD_HASH_ALGORITHM = "_sd_alg";

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<SdJwtClaim> claims;
        private String hashAlg;
        private SignatureSignerContext signer;

        public Builder withClaims(List<SdJwtClaim> claims) {
            this.claims = claims;
            return this;
        }

        public Builder withHashAlg(String hashAlg) {
            this.hashAlg = hashAlg;
            return this;
        }

        public Builder withSigner(SignatureSignerContext signer) {
            this.signer = signer;
            return this;
        }

        public IssuerSignedJWT build() {
            // Preinitialize hashAlg to sha-256 if not provided
            hashAlg = hashAlg == null ? "sha-256" : hashAlg;
            // send an empty lise if claims not set.
            claims = claims == null ? Collections.emptyList() : claims;
            if (signer != null) {
                return new IssuerSignedJWT(claims, hashAlg, signer);
            } else {
                return new IssuerSignedJWT(claims, hashAlg);
            }
        }
    }

}
