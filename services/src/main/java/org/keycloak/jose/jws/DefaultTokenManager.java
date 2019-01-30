/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.jose.jws;

import org.jboss.logging.Logger;
import org.keycloak.Token;
import org.keycloak.TokenCategory;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.TokenManager;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;

public class DefaultTokenManager implements TokenManager {

    private static final Logger logger = Logger.getLogger(DefaultTokenManager.class);

    private static String DEFAULT_ALGORITHM_NAME = Algorithm.RS256;

    private final KeycloakSession session;

    public DefaultTokenManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String encode(Token token) {
        String signatureAlgorithm = signatureAlgorithm(token.getCategory());

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
        SignatureSignerContext signer = signatureProvider.signer();

        String encodedToken = new JWSBuilder().type("JWT").jsonContent(token).sign(signer);
        return encodedToken;
    }

    @Override
    public <T extends Token> T decode(String token, Class<T> clazz) {
        if (token == null) {
            return null;
        }

        try {
            JWSInput jws = new JWSInput(token);

            String signatureAlgorithm = jws.getHeader().getAlgorithm().name();

            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
            if (signatureProvider == null) {
                return null;
            }

            String kid = jws.getHeader().getKeyId();
            // Backwards compatibility. Old offline tokens and cookies didn't have KID in the header
            if (kid == null) {
                logger.debugf("KID is null in token. Using the realm active key to verify token signature.");
                kid = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, signatureAlgorithm).getKid();
            }

            boolean valid = signatureProvider.verifier(kid).verify(jws.getEncodedSignatureInput().getBytes("UTF-8"), jws.getSignature());
            return valid ? jws.readJsonContent(clazz) : null;
        } catch (Exception e) {
            logger.debug("Failed to decode token", e);
            return null;
        }
    }

    @Override
    public String signatureAlgorithm(TokenCategory category) {
        switch (category) {
            case INTERNAL:
                return Algorithm.HS256;
            case ADMIN:
                return getSignatureAlgorithm(null);
            case ACCESS:
                return getSignatureAlgorithm(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG);
            case ID:
                return getSignatureAlgorithm(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG);
            case USERINFO:
                return getSignatureAlgorithm(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG);
            default:
                throw new RuntimeException("Unknown token type");
        }
    }

    private String getSignatureAlgorithm(String clientAttribute) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.getContext().getClient();

        String algorithm = client != null && clientAttribute != null ? client.getAttribute(clientAttribute) : null;
        if (algorithm != null && !algorithm.equals("")) {
            return algorithm;
        }

        algorithm = realm.getDefaultSignatureAlgorithm();
        if (algorithm != null && !algorithm.equals("")) {
            return algorithm;
        }

        return DEFAULT_ALGORITHM_NAME;
    }

}
