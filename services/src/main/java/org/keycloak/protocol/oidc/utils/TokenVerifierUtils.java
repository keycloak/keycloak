package org.keycloak.protocol.oidc.utils;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.crypto.CekManagementProvider;
import org.keycloak.crypto.ContentEncryptionProvider;
import org.keycloak.crypto.DecryptionKEKAccessor;
import org.keycloak.crypto.DefaultDecryptionContext;
import org.keycloak.crypto.DelegatingSignatureVerfierContext;
import org.keycloak.crypto.JWEAlgorithmProviderAccessor;
import org.keycloak.crypto.JWEEncryptionProviderAccessor;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;
import org.keycloak.services.validation.Validation;

import java.security.KeyStore;
import java.security.PrivateKey;

public class TokenVerifierUtils {

    public static <T extends JsonWebToken> TokenVerifier<T> createTokenVerifier(String token, Class<T> clazz, KeycloakSession session) throws VerificationException {

        TokenVerifier<T> verifier;

        KeycloakContext context = session.getContext();

        RealmModel realm = context.getRealm();
        ClientModel client = context.getClient();

        if (TokenVerifier.isJwe(token)) {
            verifier = new JWETokenVerifier<T>(TokenVerifier.create(token, clazz), session, client);
        } else {
            verifier = TokenVerifier.create(token, clazz);
            JWSHeader jwsHeader = verifier.getHeader();
            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, jwsHeader.getAlgorithm().name()).verifier(jwsHeader.getKeyId());
            verifier.verifierContext(verifierContext);
        }

        verifier.realmUrl(Urls.realmIssuer(context.getUri().getBaseUri(), realm.getName()));

        return verifier;
    }

    private static class JWETokenVerifier<T extends JsonWebToken> extends TokenVerifier<T> {

        public JWETokenVerifier(TokenVerifier<T> tokenVerifier, KeycloakSession session, ClientModel client) throws VerificationException{
            super(tokenVerifier);

            if (client == null) {
                throw new VerificationException("Client missing for JWETokenVerifier setup.");
            }

            OIDCAdvancedConfigWrapper oidcConfig = OIDCAdvancedConfigWrapper.fromClientModel(client);

            DecryptionKEKAccessor decryptionKeyAccessor = (kid, algo) -> {

                if (Validation.isBlank(oidcConfig.getJweTokenKeystoreLocation())) {
                    throw new JWEException("Could not verify JWE due to missing configuration.");
                }

                try {
                    KeyStore keystore = KeystoreUtil.loadKeyStore(oidcConfig.getJweTokenKeystoreLocation(), oidcConfig.getJweTokenKeystorePassword());
                    char[] keyPassword = oidcConfig.getJweTokenKeystoreKeyPassword() != null ? oidcConfig.getJweTokenKeystoreKeyPassword().toCharArray() : null;
                    return (PrivateKey) keystore.getKey(kid, keyPassword);
                } catch (Exception ex) {
                    throw new JWEException("Error during JWE decryption key lookup.", ex);
                }
            };

            // we need to delay the algorithm provider resolving here, since we need to parse the token first
            JWEAlgorithmProviderAccessor jweAlgorithmProviderAccessor = alg -> session.getProvider(CekManagementProvider.class, alg).jweAlgorithmProvider();

            // we need to delay the encryption provider resolving here, since we need to parse the token first
            JWEEncryptionProviderAccessor jweEncryptionProviderAccessor = enc -> session.getProvider(ContentEncryptionProvider.class, enc).jweEncryptionProvider();

            decryptionContext(new DefaultDecryptionContext(decryptionKeyAccessor, jweAlgorithmProviderAccessor, jweEncryptionProviderAccessor));

            // we need to wrap the SignatureVerifierContext here, to postpone the actual signature validation until the nested JWS was extracted from the JWE.
            verifierContext(new DelegatingSignatureVerfierContext(
                    this,
                    (alg, kid) -> session.getProvider(SignatureProvider.class, alg).verifier(kid)
            ));
        }

    }
}
