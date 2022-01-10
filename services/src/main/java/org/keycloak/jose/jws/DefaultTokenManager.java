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
import org.keycloak.crypto.CekManagementProvider;
import org.keycloak.crypto.ClientSignatureVerifierProvider;
import org.keycloak.crypto.ContentEncryptionProvider;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.JOSEParser;
import org.keycloak.jose.JOSE;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.TokenManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.LogoutToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.PrivateKey;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DefaultTokenManager implements TokenManager {

    private static final Logger logger = Logger.getLogger(DefaultTokenManager.class);

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
    public <T> T decodeClientJWT(String jwt, ClientModel client, BiConsumer<JOSE, ClientModel> jwtValidator, Class<T> clazz) {
        if (jwt == null) {
            return null;
        }

        JOSE joseToken = JOSEParser.parse(jwt);

        jwtValidator.accept(joseToken, client);

        if (joseToken instanceof JWE) {
            try {
                Optional<KeyWrapper> activeKey;
                String kid = joseToken.getHeader().getKeyId();
                Stream<KeyWrapper> keys = session.keys().getKeysStream(session.getContext().getRealm());

                if (kid == null) {
                    activeKey = keys.filter(k -> KeyUse.ENC.equals(k.getUse()) && k.getPublicKey() != null)
                            .sorted(Comparator.comparingLong(KeyWrapper::getProviderPriority).reversed())
                            .findFirst();
                } else {
                    activeKey = keys
                            .filter(k -> KeyUse.ENC.equals(k.getUse()) && k.getKid().equals(kid)).findAny();
                }

                JWE jwe = JWE.class.cast(joseToken);
                Key privateKey = activeKey.map(KeyWrapper::getPrivateKey)
                        .orElseThrow(() -> new RuntimeException("Could not find private key for decrypting token"));

                jwe.getKeyStorage().setDecryptionKey(privateKey);

                byte[] content = jwe.verifyAndDecodeJwe().getContent();

                try {
                    JOSE jws = JOSEParser.parse(new String(content));

                    if (jws instanceof JWSInput) {
                        jwtValidator.accept(jws, client);
                        return verifyJWS(client, clazz, (JWSInput) jws);
                    }
                } catch (Exception ignore) {
                    // try to decrypt content as is
                }

                return JsonSerialization.readValue(content, clazz);
            } catch (IOException cause) {
                throw new RuntimeException("Failed to deserialize JWT", cause);
            } catch (JWEException cause) {
                throw new RuntimeException("Failed to decrypt JWT", cause);
            }
        }

        return verifyJWS(client, clazz, (JWSInput) joseToken);
    }

    private <T> T verifyJWS(ClientModel client, Class<T> clazz, JWSInput jws) {
        try {
            String signatureAlgorithm = jws.getHeader().getAlgorithm().name();
            ClientSignatureVerifierProvider signatureProvider = session.getProvider(ClientSignatureVerifierProvider.class, signatureAlgorithm);

            if (signatureProvider == null) {
                if (jws.getHeader().getAlgorithm().equals(org.keycloak.jose.jws.Algorithm.none)) {
                    return jws.readJsonContent(clazz);
                }
                return null;
            }

            boolean valid = signatureProvider.verifier(client, jws).verify(jws.getEncodedSignatureInput().getBytes("UTF-8"), jws.getSignature());
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
            case LOGOUT:
                return getSignatureAlgorithm(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG);
            case USERINFO:
                return getSignatureAlgorithm(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG);
            case AUTHORIZATION_RESPONSE:
                return getSignatureAlgorithm(OIDCConfigAttributes.AUTHORIZATION_SIGNED_RESPONSE_ALG);
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

        return Constants.DEFAULT_SIGNATURE_ALGORITHM;
    }

    @Override
    public String encodeAndEncrypt(Token token) {
        String encodedToken = encode(token);
        if (isTokenEncryptRequired(token.getCategory())) {
            encodedToken = getEncryptedToken(token.getCategory(), encodedToken);
        }
        return encodedToken;
    }

    private boolean isTokenEncryptRequired(TokenCategory category) {
        if (cekManagementAlgorithm(category) == null) return false;
        if (encryptAlgorithm(category) == null) return false;
        return true;
    }

    private String getEncryptedToken(TokenCategory category, String encodedToken) {
        String encryptedToken = null;

        String algAlgorithm = cekManagementAlgorithm(category);
        String encAlgorithm = encryptAlgorithm(category);

        CekManagementProvider cekManagementProvider = session.getProvider(CekManagementProvider.class, algAlgorithm);
        JWEAlgorithmProvider jweAlgorithmProvider = cekManagementProvider.jweAlgorithmProvider();

        ContentEncryptionProvider contentEncryptionProvider = session.getProvider(ContentEncryptionProvider.class, encAlgorithm);
        JWEEncryptionProvider jweEncryptionProvider = contentEncryptionProvider.jweEncryptionProvider();

        ClientModel client = session.getContext().getClient();

        KeyWrapper keyWrapper = PublicKeyStorageManager.getClientPublicKeyWrapper(session, client, JWK.Use.ENCRYPTION, algAlgorithm);
        if (keyWrapper == null) {
            throw new RuntimeException("can not get encryption KEK");
        }
        Key encryptionKek = keyWrapper.getPublicKey();
        String encryptionKekId = keyWrapper.getKid();
        try {
            encryptedToken = TokenUtil.jweKeyEncryptionEncode(encryptionKek, encodedToken.getBytes("UTF-8"), algAlgorithm, encAlgorithm, encryptionKekId, jweAlgorithmProvider, jweEncryptionProvider);
        } catch (JWEException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return encryptedToken;
    }

    @Override
    public String cekManagementAlgorithm(TokenCategory category) {
        if (category == null) return null;
        switch (category) {
            case ID:
            case LOGOUT:
                return getCekManagementAlgorithm(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG);
            case AUTHORIZATION_RESPONSE:
                return getCekManagementAlgorithm(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ALG);
            default:
                return null;
        }
    }

    private String getCekManagementAlgorithm(String clientAttribute) {
        ClientModel client = session.getContext().getClient();
        String algorithm = client != null && clientAttribute != null ? client.getAttribute(clientAttribute) : null;
        if (algorithm != null && !algorithm.equals("")) {
            return algorithm;
        }
        return null;
    }

    @Override
    public String encryptAlgorithm(TokenCategory category) {
        if (category == null) return null;
        switch (category) {
            case ID:
            case LOGOUT:
                return getEncryptAlgorithm(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC);
            case AUTHORIZATION_RESPONSE:
                return getEncryptAlgorithm(OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ENC);
            default:
                return null;
        }
    }

    private String getEncryptAlgorithm(String clientAttribute) {
        ClientModel client = session.getContext().getClient();
        String algorithm = client != null && clientAttribute != null ? client.getAttribute(clientAttribute) : null;
        if (algorithm != null && !algorithm.equals("")) {
            return algorithm;
        }
        return null;
    }

    public LogoutToken initLogoutToken(ClientModel client, UserModel user,
                                       AuthenticatedClientSessionModel clientSession) {
        LogoutToken token = new LogoutToken();
        token.id(KeycloakModelUtils.generateId());
        token.issuedNow();
        token.issuer(clientSession.getNote(OIDCLoginProtocol.ISSUER));
        token.putEvents(TokenUtil.TOKEN_BACKCHANNEL_LOGOUT_EVENT, JsonSerialization.createObjectNode());
        token.addAudience(client.getClientId());

        OIDCAdvancedConfigWrapper oidcAdvancedConfigWrapper = OIDCAdvancedConfigWrapper.fromClientModel(client);
        if (oidcAdvancedConfigWrapper.isBackchannelLogoutSessionRequired()){
            token.setSid(clientSession.getUserSession().getId());
        }
        if (oidcAdvancedConfigWrapper.getBackchannelLogoutRevokeOfflineTokens()){
            token.putEvents(TokenUtil.TOKEN_BACKCHANNEL_LOGOUT_EVENT_REVOKE_OFFLINE_TOKENS, true);
        }
        token.setSubject(user.getId());

        return token;
    }
}
