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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.sdjwt.consumer.PresentationRequirements;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_JWK;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_UNDISCLOSED_ARRAY;
import static org.keycloak.OID4VCConstants.SDJWT_DELIMITER;
import static org.keycloak.OID4VCConstants.SD_HASH;


/**
 * Runs SD-JWT verification in isolation with only essential properties.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class SdJwtVerificationContext {

    private static final Logger logger = Logger.getLogger(SdJwtVerificationContext.class.getName());

    private String sdJwtVpString;

    private final IssuerSignedJWT issuerSignedJwt;
    private final Map<String, String> disclosures;
    private KeyBindingJWT keyBindingJwt;

    public SdJwtVerificationContext(
            String sdJwtVpString,
            IssuerSignedJWT issuerSignedJwt,
            Map<String, String> disclosures,
            KeyBindingJWT keyBindingJwt) {
        this(issuerSignedJwt, disclosures);
        this.keyBindingJwt = keyBindingJwt;
        this.sdJwtVpString = sdJwtVpString;
    }

    public SdJwtVerificationContext(IssuerSignedJWT issuerSignedJwt, Map<String, String> disclosures) {
        this.issuerSignedJwt = issuerSignedJwt;
        this.disclosures = disclosures;
    }

    public SdJwtVerificationContext(IssuerSignedJWT issuerSignedJwt, List<String> disclosureStrings) {
        this.issuerSignedJwt = issuerSignedJwt;
        this.disclosures = computeDigestDisclosureMap(disclosureStrings);
    }

    private Map<String, String> computeDigestDisclosureMap(List<String> disclosureStrings) {
        return disclosureStrings.stream()
                .map(disclosureString -> {
                    String digest = SdJwtUtils.hashAndBase64EncodeNoPad(
                            disclosureString.getBytes(), issuerSignedJwt.getSdHashAlg());
                    return new AbstractMap.SimpleEntry<>(digest, disclosureString);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Verifies SD-JWT as to whether the Issuer-signed JWT's signature and disclosures are valid.
     *
     * <p>Upon receiving an SD-JWT, a Holder or a Verifier needs to ensure that:</p>
     * - the Issuer-signed JWT is valid, i.e., it is signed by the Issuer and the signature is valid, and
     * - all Disclosures are valid and correspond to a respective digest value in the Issuer-signed JWT
     * (directly in the payload or recursively included in the contents of other Disclosures).
     *
     * @param issuerVerifyingKeys             Verifying keys for validating the Issuer-signed JWT. The caller
     *                                        is responsible for establishing trust in that the keys belong
     *                                        to the intended issuer.
     * @param issuerSignedJwtVerificationOpts Options to parameterize the Issuer-Signed JWT verification.
     * @param presentationRequirements        If set, the presentation requirements will be enforced upon fully
     *                                        disclosing the Issuer-signed JWT during the verification.
     * @throws VerificationException if verification failed
     */
    public void verifyIssuance(
            List<SignatureVerifierContext> issuerVerifyingKeys,
            IssuerSignedJwtVerificationOpts issuerSignedJwtVerificationOpts,
            PresentationRequirements presentationRequirements
    ) throws VerificationException {
        // Validate the Issuer-signed JWT.
        validateIssuerSignedJwt(issuerVerifyingKeys);

        // Validate disclosures.
        JsonNode disclosedPayload = validateDisclosuresDigests();

        // Validate time claims.
        // Issuers will typically include claims controlling the validity of the SD-JWT in plaintext in the
        // SD-JWT payload, but there is no guarantee they would do so. Therefore, Verifiers cannot reliably
        // depend on that and need to operate as though security-critical claims might be selectively disclosable.
        validateIssuerSignedJwtTimeClaims(disclosedPayload, issuerSignedJwtVerificationOpts);

        // Enforce presentation requirements.
        if (presentationRequirements != null) {
            presentationRequirements.checkIfSatisfiedBy(disclosedPayload);
        }
    }

    /**
     * Verifies SD-JWT presentation.
     *
     * <p>
     * Upon receiving a Presentation, in addition to the checks in {@link #verifyIssuance}, Verifiers need
     * to ensure that if Key Binding is required, the Key Binding JWT is signed by the Holder and valid.
     * </p>
     *
     * @param issuerVerifyingKeys             Verifying keys for validating the Issuer-signed JWT. The caller
     *                                        is responsible for establishing trust in that the keys belong
     *                                        to the intended issuer.
     * @param issuerSignedJwtVerificationOpts Options to parameterize the Issuer-Signed JWT verification.
     * @param keyBindingJwtVerificationOpts   Options to parameterize the Key Binding JWT verification.
     *                                        Must, among others, specify the Verifier's policy whether
     *                                        to check Key Binding.
     * @param presentationRequirements        If set, the presentation requirements will be enforced upon fully
     *                                        disclosing the Issuer-signed JWT during the verification.
     * @throws VerificationException if verification failed
     */
    public void verifyPresentation(
            List<SignatureVerifierContext> issuerVerifyingKeys,
            IssuerSignedJwtVerificationOpts issuerSignedJwtVerificationOpts,
            KeyBindingJwtVerificationOpts keyBindingJwtVerificationOpts,
            PresentationRequirements presentationRequirements
    ) throws VerificationException {
        // If Key Binding is required and a Key Binding JWT is not provided,
        // the Verifier MUST reject the Presentation.
        if (keyBindingJwtVerificationOpts.isKeyBindingRequired() && keyBindingJwt == null) {
            throw new VerificationException("Missing Key Binding JWT");
        }

        // Upon receiving a Presentation, in addition to the checks in {@link #verifyIssuance}...
        verifyIssuance(issuerVerifyingKeys, issuerSignedJwtVerificationOpts, presentationRequirements);

        // Validate Key Binding JWT if required
        if (keyBindingJwtVerificationOpts.isKeyBindingRequired()) {
            validateKeyBindingJwt(keyBindingJwtVerificationOpts);
        }
    }

    /**
     * Validate Issuer-signed JWT
     *
     * <p>
     * Upon receiving an SD-JWT, a Holder or a Verifier needs to ensure that:
     * - the Issuer-signed JWT is valid, i.e., it is signed by the Issuer and the signature is valid
     * </p>
     *
     * @param verifiers Verifying keys for validating the Issuer-signed JWT.
     * @throws VerificationException if verification failed
     */
    private void validateIssuerSignedJwt(
            List<SignatureVerifierContext> verifiers
    ) throws VerificationException {
        // Check that the _sd_alg claim value is understood and the hash algorithm is deemed secure
        issuerSignedJwt.verifySdHashAlgorithm();

        // Validate the signature over the Issuer-signed JWT
        Iterator<SignatureVerifierContext> iterator = verifiers.iterator();
        while (iterator.hasNext()) {
            try {
                SignatureVerifierContext verifier = iterator.next();
                issuerSignedJwt.verifySignature(verifier);
                return;
            } catch (VerificationException e) {
                logger.debugf(e, "Issuer-signed JWT's signature verification failed against one potential verifying key");
                if (iterator.hasNext()) {
                    logger.debugf("Retrying Issuer-signed JWT's signature verification with next potential verifying key");
                }
            }
        }

        // No potential verifier could verify the JWT's signature
        throw new VerificationException("Invalid Issuer-Signed JWT: Signature could not be verified");
    }

    /**
     * Validate Key Binding JWT
     *
     * @throws VerificationException if verification failed
     */
    private void validateKeyBindingJwt(
            KeyBindingJwtVerificationOpts keyBindingJwtVerificationOpts
    ) throws VerificationException {
        // Check that the typ of the Key Binding JWT is kb+jwt
        validateKeyBindingJwtTyp();

        // Determine the public key for the Holder from the SD-JWT
        JsonNode cnf = issuerSignedJwt.getCnfClaim().orElseThrow(
                () -> new VerificationException("No cnf claim in Issuer-signed JWT for key binding")
        );

        // Ensure that a signing algorithm was used that was deemed secure for the application.
        // The none algorithm MUST NOT be accepted.
        SignatureVerifierContext holderVerifier = buildHolderVerifier(cnf);

        // Validate the signature over the Key Binding JWT
        try {
            keyBindingJwt.verifySignature(holderVerifier);
        } catch (VerificationException e) {
            throw new VerificationException("Key binding JWT invalid", e);
        }

        // Check that the creation time of the Key Binding JWT is within an acceptable window.
        validateKeyBindingJwtTimeClaims(keyBindingJwtVerificationOpts);

        // Determine that the Key Binding JWT is bound to the current transaction and was created
        // for this Verifier (replay protection) by validating nonce and aud claims.
        preventKeyBindingJwtReplay(keyBindingJwtVerificationOpts);

        // The same hash algorithm as for the Disclosures MUST be used (defined by the _sd_alg element
        // in the Issuer-signed JWT or the default value, as defined in Section 5.1.1).
        validateKeyBindingJwtSdHashIntegrity();

        // Check that the Key Binding JWT is a valid JWT in all other respects
        // -> Covered in part by `keyBindingJwt` being an instance of SdJws?
        // -> Time claims are checked above
    }

    /**
     * Validate Key Binding JWT's typ header attribute
     *
     * @throws VerificationException if verification failed
     */
    private void validateKeyBindingJwtTyp() throws VerificationException {
        String typ = keyBindingJwt.getHeader().getType();
        if (!typ.equals(KeyBindingJWT.TYP)) {
            throw new VerificationException("Key Binding JWT is not of declared typ " + KeyBindingJWT.TYP);
        }
    }

    /**
     * Build holder verifier from JWK node.
     *
     * @throws VerificationException if unable
     */
    private SignatureVerifierContext buildHolderVerifier(JsonNode cnf) throws VerificationException {
        Objects.requireNonNull(cnf);

        // Read JWK
        JsonNode cnfJwk = cnf.get(CLAIM_NAME_JWK);
        if (cnfJwk == null) {
            throw new UnsupportedOperationException("Only cnf/jwk claim supported");
        }

        // Convert JWK
        try {
            return JwkParsingUtils.convertJwkNodeToVerifierContext(cnfJwk);
        } catch (Exception e) {
            throw new VerificationException("Could not process cnf/jwk", e);
        }
    }

    /**
     * Validate Issuer-Signed JWT time claims.
     *
     * <p>
     * Check that the SD-JWT is valid using claims such as nbf, iat, and exp in the processed payload.
     * If a required validity-controlling claim is missing, the SD-JWT MUST be rejected.
     * </p>
     *
     * @throws VerificationException if verification failed
     */
    private void validateIssuerSignedJwtTimeClaims(
            JsonNode payload,
            IssuerSignedJwtVerificationOpts issuerSignedJwtVerificationOpts
    ) throws VerificationException {
        TimeClaimVerifier timeClaimVerifier = new TimeClaimVerifier(issuerSignedJwtVerificationOpts);

        try {
            timeClaimVerifier.verifyIssuedAtClaim(payload);
        } catch (VerificationException e) {
            throw new VerificationException("Issuer-Signed JWT: Invalid `iat` claim", e);
        }

        try {
            timeClaimVerifier.verifyExpirationClaim(payload);
        } catch (VerificationException e) {
            throw new VerificationException("Issuer-Signed JWT: Invalid `exp` claim", e);
        }

        try {
            timeClaimVerifier.verifyNotBeforeClaim(payload);
        } catch (VerificationException e) {
            throw new VerificationException("Issuer-Signed JWT: Invalid `nbf` claim", e);
        }
    }

    /**
     * Validate key binding JWT time claims.
     *
     * @throws VerificationException if verification failed
     */
    private void validateKeyBindingJwtTimeClaims(
            KeyBindingJwtVerificationOpts keyBindingJwtVerificationOpts
    ) throws VerificationException {
        JsonNode kbJwtPayload = keyBindingJwt.getPayload();
        TimeClaimVerifier timeClaimVerifier = new TimeClaimVerifier(keyBindingJwtVerificationOpts);

        // Check that the creation time of the Key Binding JWT, as determined by the iat claim,
        // is within an acceptable window

        try {
            timeClaimVerifier.verifyIssuedAtClaim(kbJwtPayload);
        } catch (VerificationException e) {
            throw new VerificationException("Key binding JWT: Invalid `iat` claim", e);
        }

        try {
            timeClaimVerifier.verifyAge(kbJwtPayload, keyBindingJwtVerificationOpts.getAllowedMaxAge());
        } catch (VerificationException e) {
            throw new VerificationException("Key binding JWT is too old");
        }

        // Check other time claims

        try {
            timeClaimVerifier.verifyExpirationClaim(kbJwtPayload);
        } catch (VerificationException e) {
            throw new VerificationException("Key binding JWT: Invalid `exp` claim", e);
        }

        try {
            timeClaimVerifier.verifyNotBeforeClaim(kbJwtPayload);
        } catch (VerificationException e) {
            throw new VerificationException("Key binding JWT: Invalid `nbf` claim", e);
        }
    }

    /**
     * Validate disclosures' digests
     *
     * <p>
     * Upon receiving an SD-JWT, a Holder or a Verifier needs to ensure that:
     * - all Disclosures are valid and correspond to a respective digest value in the Issuer-signed JWT
     * (directly in the payload or recursively included in the contents of other Disclosures)
     * </p>
     *
     * <p>
     * We additionally check that salt values are not reused:
     * The salt value MUST be unique for each claim that is to be selectively disclosed.
     * </p>
     *
     * @return the fully disclosed SdJwt payload
     * @throws VerificationException if verification failed
     */
    private JsonNode validateDisclosuresDigests() throws VerificationException {
        // Validate SdJwt digests by attempting full recursive disclosing.
        Set<String> visitedSalts = new HashSet<>();
        Set<String> visitedDigests = new HashSet<>();
        Set<String> visitedDisclosureStrings = new HashSet<>();
        JsonNode disclosedPayload = validateViaRecursiveDisclosing(
                SdJwtUtils.deepClone(issuerSignedJwt.getPayload()),
                visitedSalts, visitedDigests, visitedDisclosureStrings);

        // Validate all disclosures where visited
        validateDisclosuresVisits(visitedDisclosureStrings);

        return disclosedPayload;
    }

    /**
     * Validate SdJwt digests by attempting full recursive disclosing.
     *
     * <p>
     * By recursively disclosing all disclosable fields in the SdJwt payload, validation rules are
     * enforced regarding the conformance of linked disclosures. Additional rules should be enforced
     * after calling this method based on the visited data arguments.
     * </p>
     *
     * @return the fully disclosed SdJwt payload
     */
    private JsonNode validateViaRecursiveDisclosing(
            JsonNode currentNode,
            Set<String> visitedSalts,
            Set<String> visitedDigests,
            Set<String> visitedDisclosureStrings
    ) throws VerificationException {
        if (!currentNode.isObject() && !currentNode.isArray()) {
            return currentNode;
        }

        // Find all objects having an _sd key that refers to an array of strings.
        if (currentNode.isObject()) {
            ObjectNode currentObjectNode = ((ObjectNode) currentNode);

            JsonNode sdArray = currentObjectNode.get(CLAIM_NAME_SD);
            if (sdArray != null && sdArray.isArray()) {
                for (JsonNode el : sdArray) {
                    if (!el.isTextual()) {
                        throw new VerificationException(
                                "Unexpected non-string element inside _sd array: " + el
                        );
                    }

                    // Compare the value with the digests calculated previously and find the matching Disclosure.
                    // If no such Disclosure can be found, the digest MUST be ignored.

                    String digest = el.asText();
                    markDigestAsVisited(digest, visitedDigests);
                    String disclosure = disclosures.get(digest);

                    if (disclosure != null) {
                        // Mark disclosure as visited
                        visitedDisclosureStrings.add(disclosure);

                        // Validate disclosure format
                        DisclosureFields decodedDisclosure = validateSdArrayDigestDisclosureFormat(disclosure);

                        // Mark salt as visited
                        markSaltAsVisited(decodedDisclosure.getSaltValue(), visitedSalts);

                        // Insert, at the level of the _sd key, a new claim using the claim name
                        // and claim value from the Disclosure
                        currentObjectNode.set(
                                decodedDisclosure.getClaimName(),
                                decodedDisclosure.getClaimValue()
                        );
                    }
                }
            }

            // Remove all _sd keys and their contents from the Issuer-signed JWT payload.
            // If this results in an object with no properties, it should be represented as an empty object {}
            currentObjectNode.remove(CLAIM_NAME_SD);

            // Remove the claim _sd_alg from the SD-JWT payload.
            currentObjectNode.remove(CLAIM_NAME_SD_HASH_ALGORITHM);
        }

        // Find all array elements that are objects with one key, that key being ... and referring to a string
        if (currentNode.isArray()) {
            ArrayNode currentArrayNode = ((ArrayNode) currentNode);
            ArrayList<Integer> indexesToRemove = new ArrayList<>();

            for (int i = 0; i < currentArrayNode.size(); ++i) {
                JsonNode itemNode = currentArrayNode.get(i);
                if (itemNode.isObject() && itemNode.size() == 1) {
                    // Check single "..." field
                    Map.Entry<String, JsonNode> field = itemNode.fields().next();
                    if (field.getKey().equals(CLAIM_NAME_SD_UNDISCLOSED_ARRAY)
                            && field.getValue().isTextual()) {
                        // Compare the value with the digests calculated previously and find the matching Disclosure.
                        // If no such Disclosure can be found, the digest MUST be ignored.

                        String digest = field.getValue().asText();
                        markDigestAsVisited(digest, visitedDigests);
                        String disclosure = disclosures.get(digest);

                        if (disclosure != null) {
                            // Mark disclosure as visited
                            visitedDisclosureStrings.add(disclosure);

                            // Validate disclosure format
                            DisclosureFields decodedDisclosure = validateArrayElementDigestDisclosureFormat(disclosure);

                            // Mark salt as visited
                            markSaltAsVisited(decodedDisclosure.getSaltValue(), visitedSalts);

                            // Replace the array element with the value from the Disclosure.
                            // Removal is done below.
                            currentArrayNode.set(i, decodedDisclosure.getClaimValue());
                        } else {
                            // Remove all array elements for which the digest was not found in the previous step.
                            indexesToRemove.add(i);
                        }
                    }
                }
            }

            // Remove all array elements for which the digest was not found in the previous step.
            indexesToRemove.forEach(currentArrayNode::remove);
        }

        for (JsonNode childNode : currentNode) {
            validateViaRecursiveDisclosing(childNode, visitedSalts, visitedDigests, visitedDisclosureStrings);
        }

        return currentNode;
    }

    /**
     * Mark digest as visited.
     *
     * <p>
     * If any digest value is encountered more than once in the Issuer-signed JWT payload
     * (directly or recursively via other Disclosures), the SD-JWT MUST be rejected.
     * </p>
     *
     * @throws VerificationException if not first visit
     */
    private void markDigestAsVisited(String digest, Set<String> visitedDigests)
            throws VerificationException {
        if (!visitedDigests.add(digest)) {
            // If add returns false, then it is a duplicate
            throw new VerificationException("A digest was encountered more than once: " + digest);
        }
    }

    /**
     * Mark salt as visited.
     *
     * <p>
     * The salt value MUST be unique for each claim that is to be selectively disclosed.
     * </p>
     *
     * @throws VerificationException if not first visit
     */
    private void markSaltAsVisited(String salt, Set<String> visitedSalts)
            throws VerificationException {
        if (!visitedSalts.add(salt)) {
            // If add returns false, then it is a duplicate
            throw new VerificationException("A salt value was reused: " + salt);
        }
    }

    /**
     * Validate disclosure assuming digest was found in an object's _sd key.
     *
     * <p>
     * If the contents of the respective Disclosure is not a JSON-encoded array of three elements
     * (salt, claim name, claim value), the SD-JWT MUST be rejected.
     * </p>
     *
     * <p>
     * If the claim name is _sd or ..., the SD-JWT MUST be rejected.
     * </p>
     *
     * @return decoded disclosure (salt, claim name, claim value)
     */
    private DisclosureFields validateSdArrayDigestDisclosureFormat(String disclosure)
            throws VerificationException {
        ArrayNode arrayNode = SdJwtUtils.decodeDisclosureString(disclosure);

        // Check if the array has exactly three elements
        if (arrayNode.size() != 3) {
            throw new VerificationException("A field disclosure must contain exactly three elements");
        }

        // If the claim name is _sd or ..., the SD-JWT MUST be rejected.

        List<String> denylist = Arrays.asList(
                CLAIM_NAME_SD,
                CLAIM_NAME_SD_UNDISCLOSED_ARRAY
        );

        String claimName = arrayNode.get(1).asText();
        if (denylist.contains(claimName)) {
            throw new VerificationException("Disclosure claim name must not be '_sd' or '...'");
        }

        // Return decoded disclosure
        return new DisclosureFields(
                arrayNode.get(0).asText(),
                claimName,
                arrayNode.get(2)
        );
    }

    /**
     * Validate disclosure assuming digest was found as an undisclosed array element.
     *
     * <p>
     * If the contents of the respective Disclosure is not a JSON-encoded array of
     * two elements (salt, value), the SD-JWT MUST be rejected.
     * </p>
     *
     * @return decoded disclosure (salt, value)
     */
    private DisclosureFields validateArrayElementDigestDisclosureFormat(String disclosure)
            throws VerificationException {
        ArrayNode arrayNode = SdJwtUtils.decodeDisclosureString(disclosure);

        // Check if the array has exactly two elements
        if (arrayNode.size() != 2) {
            throw new VerificationException("An array element disclosure must contain exactly two elements");
        }

        // Return decoded disclosure
        return new DisclosureFields(
                arrayNode.get(0).asText(),
                null,
                arrayNode.get(1)
        );
    }

    /**
     * Validate all disclosures where visited
     *
     * <p>
     * If any Disclosure was not referenced by digest value in the Issuer-signed JWT (directly or recursively via
     * other Disclosures), the SD-JWT MUST be rejected.
     * </p>
     *
     * @throws VerificationException if not the case
     */
    private void validateDisclosuresVisits(Set<String> visitedDisclosureStrings)
            throws VerificationException {
        if (visitedDisclosureStrings.size() < disclosures.size()) {
            throw new VerificationException("At least one disclosure is not protected by digest");
        }
    }

    /**
     * Run checks for replay protection.
     *
     * <p>
     * Determine that the Key Binding JWT is bound to the current transaction and was created for this
     * Verifier (replay protection) by validating nonce and aud claims.
     * </p>
     *
     * @throws VerificationException if verification failed
     */
    private void preventKeyBindingJwtReplay(
            KeyBindingJwtVerificationOpts keyBindingJwtVerificationOpts
    ) throws VerificationException {
        JsonNode nonce = keyBindingJwt.getPayload().get("nonce");
        if (nonce == null || !nonce.isTextual()
                || !nonce.asText().equals(keyBindingJwtVerificationOpts.getNonce())) {
            throw new VerificationException("Key binding JWT: Unexpected `nonce` value");
        }

        JsonNode aud = keyBindingJwt.getPayload().get("aud");
        if (aud == null || !aud.isTextual()
                || !aud.asText().equals(keyBindingJwtVerificationOpts.getAud())) {
            throw new VerificationException("Key binding JWT: Unexpected `aud` value");
        }
    }

    /**
     * Validate integrity of Key Binding JWT's sd_hash.
     *
     * <p>
     * Calculate the digest over the Issuer-signed JWT and Disclosures and verify that it matches
     * the value of the sd_hash claim in the Key Binding JWT.
     * </p>
     *
     * @throws VerificationException if verification failed
     */
    private void validateKeyBindingJwtSdHashIntegrity() throws VerificationException {
        Objects.requireNonNull(sdJwtVpString);

        JsonNode sdHash = keyBindingJwt.getPayload().get(SD_HASH);
        if (sdHash == null || !sdHash.isTextual()) {
            throw new VerificationException("Key binding JWT: Claim `sd_hash` missing or not a string");
        }

        int lastDelimiterIndex = sdJwtVpString.lastIndexOf(SDJWT_DELIMITER);
        String toHash = sdJwtVpString.substring(0, lastDelimiterIndex + 1);

        String digest = SdJwtUtils.hashAndBase64EncodeNoPad(
                toHash.getBytes(), issuerSignedJwt.getSdHashAlg());

        if (!digest.equals(sdHash.asText())) {
            throw new VerificationException("Key binding JWT: Invalid `sd_hash` digest");
        }
    }

    /**
     * Plain record for disclosure fields.
     */
    private static class DisclosureFields {
        String saltValue;
        String claimName;
        JsonNode claimValue;

        public DisclosureFields(String saltValue, String claimName, JsonNode claimValue) {
            this.saltValue = saltValue;
            this.claimName = claimName;
            this.claimValue = claimValue;
        }

        public String getSaltValue() {
            return saltValue;
        }

        public String getClaimName() {
            return claimName;
        }

        public JsonNode getClaimValue() {
            return claimValue;
        }
    }
}
