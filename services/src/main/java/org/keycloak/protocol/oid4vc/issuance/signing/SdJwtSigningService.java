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

package org.keycloak.protocol.oid4vc.issuance.signing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * {@link VerifiableCredentialsSigningService} implementing the SD_JWT_VC format. It returns a String, containing
 * the signed SD-JWT
 * <p>
 * {@see https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html}
 * {@see https://www.ietf.org/archive/id/draft-fett-oauth-selective-disclosure-jwt-02.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class SdJwtSigningService extends SigningService<String> {

    private static final Logger LOGGER = Logger.getLogger(SdJwtSigningService.class);

    private static final String ISSUER_CLAIM ="iss";
    private static final String NOT_BEFORE_CLAIM ="nbf";
    private static final String VERIFIABLE_CREDENTIAL_TYPE_CLAIM = "vct";
    private static final String CREDENTIAL_ID_CLAIM = "jti";

    private final ObjectMapper objectMapper;
    private final SignatureSignerContext signatureSignerContext;
    private final TimeProvider timeProvider;
    private final String tokenType;
    private final String hashAlgorithm;
    private final int decoys;
    private final List<String> visibleClaims;
    protected final String issuerDid;

    public SdJwtSigningService(KeycloakSession keycloakSession, ObjectMapper objectMapper, String keyId, String algorithmType, String tokenType, String hashAlgorithm, String issuerDid, int decoys, List<String> visibleClaims, TimeProvider timeProvider, Optional<String> kid) {
        super(keycloakSession, keyId, algorithmType);
        this.objectMapper = objectMapper;
        this.issuerDid = issuerDid;
        this.timeProvider = timeProvider;
        this.tokenType = tokenType;
        this.hashAlgorithm = hashAlgorithm;
        this.decoys = decoys;
        this.visibleClaims = visibleClaims;
        KeyWrapper signingKey = getKey(keyId, algorithmType);
        if (signingKey == null) {
            throw new SigningServiceException(String.format("No key for id %s and algorithm %s available.", keyId, algorithmType));
        }
        // set the configured kid if present.
        if (kid.isPresent()) {
            // we need to clone the key first, to not change the kid of the original key so that the next request still can find it.
            signingKey = signingKey.cloneKey();
            signingKey.setKid(keyId);
        }
        kid.ifPresent(signingKey::setKid);
        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, algorithmType);
        signatureSignerContext = signatureProvider.signer(signingKey);

        LOGGER.debugf("Successfully initiated the SD-JWT Signing Service with algorithm %s.", algorithmType);
    }

    @Override
    public String signCredential(VerifiableCredential verifiableCredential) {

        DisclosureSpec.Builder disclosureSpecBuilder = DisclosureSpec.builder();
        CredentialSubject credentialSubject = verifiableCredential.getCredentialSubject();
        JsonNode claimSet = objectMapper.valueToTree(credentialSubject);
        // put all claims into the disclosure spec, except the one to be kept visible
        credentialSubject.getClaims()
                .entrySet()
                .stream()
                .filter(entry -> !visibleClaims.contains(entry.getKey()))
                .forEach(entry -> {
                    if (entry instanceof List<?> listValue) {
                        IntStream.range(0, listValue.size())
                                .forEach(i -> disclosureSpecBuilder.withUndisclosedArrayElt(entry.getKey(), i, SdJwtUtils.randomSalt()));
                    } else {
                        disclosureSpecBuilder.withUndisclosedClaim(entry.getKey(), SdJwtUtils.randomSalt());
                    }
                });

        // add the configured number of decoys
        if (decoys != 0) {
            IntStream.range(0, decoys)
                    .forEach(i -> disclosureSpecBuilder.withDecoyClaim(SdJwtUtils.randomSalt()));
        }

        ObjectNode rootNode = claimSet.withObject("");
        rootNode.put(ISSUER_CLAIM, issuerDid);

        // Get the issuance date from the credential. Since nbf is mandatory, we set it to the current time if not
        // provided
        long iat = Optional.ofNullable(verifiableCredential.getIssuanceDate())
                .map(issuanceDate -> issuanceDate.toInstant().getEpochSecond())
                .orElse((long) timeProvider.currentTimeSeconds());
        rootNode.put(NOT_BEFORE_CLAIM, iat);
        if (verifiableCredential.getType() == null || verifiableCredential.getType().size() != 1) {
            throw new SigningServiceException("SD-JWT only supports single type credentials.");
        }
        rootNode.put(VERIFIABLE_CREDENTIAL_TYPE_CLAIM, verifiableCredential.getType().get(0));
        rootNode.put(CREDENTIAL_ID_CLAIM, JwtSigningService.createCredentialId(verifiableCredential));

        SdJwt sdJwt = SdJwt.builder()
                .withDisclosureSpec(disclosureSpecBuilder.build())
                .withClaimSet(claimSet)
                .withSigner(signatureSignerContext)
                .withHashAlgorithm(hashAlgorithm)
                .withJwsType(tokenType)
                .build();

        return sdJwt.toSdJwtString();
    }

}