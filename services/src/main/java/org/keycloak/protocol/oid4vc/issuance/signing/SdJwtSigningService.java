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
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.CredentialConfigId;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.model.VerifiableCredentialType;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
public class SdJwtSigningService extends JwtProofBasedSigningService<String> {

    private static final Logger LOGGER = Logger.getLogger(SdJwtSigningService.class);

    private static final String ISSUER_CLAIM = "iss";
    private static final String VERIFIABLE_CREDENTIAL_TYPE_CLAIM = "vct";
    private static final String CREDENTIAL_ID_CLAIM = "jti";
    private static final String CNF_CLAIM = "cnf";
    private static final String JWK_CLAIM = "jwk";

    private final ObjectMapper objectMapper;
    private final SignatureSignerContext signatureSignerContext;
    private final String tokenType;
    private final String hashAlgorithm;
    private final int decoys;
    private final List<String> visibleClaims;
    protected final String issuerDid;

    private final CredentialConfigId vcConfigId;

    // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request-6
    // vct sort of additional category for sd-jwt.
    private final VerifiableCredentialType vct;

    public SdJwtSigningService(KeycloakSession keycloakSession, ObjectMapper objectMapper, String keyId, String algorithmType, String tokenType, String hashAlgorithm, String issuerDid, int decoys, List<String> visibleClaims, Optional<String> kid, VerifiableCredentialType credentialType, CredentialConfigId vcConfigId) {
        super(keycloakSession, keyId, Format.SD_JWT_VC, algorithmType);
        this.objectMapper = objectMapper;
        this.issuerDid = issuerDid;
        this.tokenType = tokenType;
        this.hashAlgorithm = hashAlgorithm;
        this.decoys = decoys;
        this.visibleClaims = visibleClaims;
        this.vcConfigId = vcConfigId;
        this.vct = credentialType;

        // If a config id is defined, a vct must be defined.
        // Also validated in: org.keycloak.protocol.oid4vc.issuance.signing.SdJwtSigningServiceProviderFactory.validateSpecificConfiguration
        if (this.vcConfigId != null && this.vct == null) {
            throw new SigningServiceException(String.format("Missing vct for credential config id %s.", vcConfigId));
        }

        // Will return the active key if key id is null.
        KeyWrapper signingKey = getKey(keyId, algorithmType);
        if (signingKey == null) {
            throw new SigningServiceException(String.format("No key for id %s and algorithm %s available.", keyId, algorithmType));
        }
        // keyId header can be confusing if there is any key rotation, as key ids have to be immutable. It can lead
        // to different keys being exposed under the same id.
        // set the configured kid if present.
        if (kid.isPresent()) {
            // we need to clone the key first, to not change the kid of the original key so that the next request still can find it.
            signingKey = signingKey.cloneKey();
            signingKey.setKid(keyId);
        }

        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, algorithmType);
        signatureSignerContext = signatureProvider.signer(signingKey);

        LOGGER.debugf("Successfully initiated the SD-JWT Signing Service with algorithm %s.", algorithmType);
    }

    @Override
    public String signCredential(VCIssuanceContext vcIssuanceContext) throws VCIssuerException {

        JWK jwk = null;
        try {
            // null returned is a valid result. Means no key binding will be included.
            jwk = validateProof(vcIssuanceContext);
        } catch (JWSInputException | VerificationException | IOException e) {
            throw new VCIssuerException("Can not verify proof", e);
        }

        VerifiableCredential verifiableCredential = vcIssuanceContext.getVerifiableCredential();
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

        // nbf, iat and exp are all optional. So need to be set by a protocol mapper if needed
        // see: https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-03.html#name-registered-jwt-claims

        // Use vct as type for sd-jwt.
        rootNode.put(VERIFIABLE_CREDENTIAL_TYPE_CLAIM, vct.getValue());
        rootNode.put(CREDENTIAL_ID_CLAIM, JwtSigningService.createCredentialId(verifiableCredential));

        // add the key binding if any
        if (jwk != null) {
            rootNode.putPOJO(CNF_CLAIM, Map.of(JWK_CLAIM, jwk));
        }

        SdJwt sdJwt = SdJwt.builder()
                .withDisclosureSpec(disclosureSpecBuilder.build())
                .withClaimSet(claimSet)
                .withSigner(signatureSignerContext)
                .withHashAlgorithm(hashAlgorithm)
                .withJwsType(tokenType)
                .build();

        return sdJwt.toSdJwtString();
    }

    @Override
    public String locator() {
        return VerifiableCredentialsSigningService.locator(format, vct, vcConfigId);
    }
}
