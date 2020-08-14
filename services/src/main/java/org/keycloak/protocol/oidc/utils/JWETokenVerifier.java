package org.keycloak.protocol.oidc.utils;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.crypto.CekManagementProvider;
import org.keycloak.crypto.ContentEncryptionProvider;
import org.keycloak.crypto.DecryptionKEKAccessor;
import org.keycloak.crypto.DecryptionVerifierContext;
import org.keycloak.crypto.DefaultDecryptionVerifierContext;
import org.keycloak.crypto.DelegatingSignatureVerfierContext;
import org.keycloak.crypto.JWEAlgorithmProviderAccessor;
import org.keycloak.crypto.JWEEncryptionProviderAccessor;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;

import java.security.KeyStore;
import java.security.PrivateKey;

public class JWETokenVerifier<T extends JsonWebToken> extends TokenVerifier<T> {

    public static <T extends JsonWebToken> TokenVerifier<T> createTokenVerifier(String token, Class<T> clazz, KeycloakSession session, RealmModel realm, ClientModel client) throws VerificationException {

        TokenVerifier<T> verifier;

        if (TokenVerifier.isJwe(token)) {
            verifier = new JWETokenVerifier<T>(TokenVerifier.create(token, clazz), session, (ClientModel) null);
        } else {
            verifier = TokenVerifier.create(token, clazz);
            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);
        }

        verifier.realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

        return verifier;
    }

    public JWETokenVerifier(TokenVerifier<T> tokenVerifier, KeycloakSession session, ClientModel client) {
        super(tokenVerifier);

        DecryptionKEKAccessor decryptionKeyAccessor = (kid, algo) -> {
            // TODO find a way to configure the encryption key from client
            try {
                KeyStore keystore = KeystoreUtil.loadKeyStore("/home/tom/dev/repos/gh/thomasdarimont/spring-training/spring-boot-keycloak-jwe-example/src/main/resources/keystore.jks", "geheim");
                return (PrivateKey) keystore.getKey(kid, "geheim".toCharArray());
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        };

        JWEAlgorithmProviderAccessor jweAlgorithmProviderAccessor = alg -> session.getProvider(CekManagementProvider.class, alg).jweAlgorithmProvider();
        JWEEncryptionProviderAccessor jweEncryptionProviderAccessor = enc -> session.getProvider(ContentEncryptionProvider.class, enc).jweEncryptionProvider();

        DecryptionVerifierContext decryptionVerifierContext = new DefaultDecryptionVerifierContext(decryptionKeyAccessor, jweAlgorithmProviderAccessor, jweEncryptionProviderAccessor);
        decrypterContext(decryptionVerifierContext);

        // we need to wrap the SignatureVerifierContext here, to postpone the actual signature validation until the nested JWS was extracted from the JWE.
        SignatureVerifierContext verifierContext = new DelegatingSignatureVerfierContext(
                this,
                (alg, kid) -> {
                    return session.getProvider(SignatureProvider.class, alg).verifier(kid);
                }
        );
        verifierContext(verifierContext);
    }
}
