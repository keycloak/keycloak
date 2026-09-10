package org.keycloak.tests.oid4vc.abca;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ClientAttestationJwt;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;

import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_JWT_TYPE;

public class OIDCMockClientAttester implements OIDCClientAttester {

    private final String issuer;
    private final KeyWrapper attesterKey;

    public OIDCMockClientAttester(KeyWrapper attesterKey) {
        this.issuer = "https://example.com/mock-attester";
        this.attesterKey = attesterKey;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    @Override
    public PublicKey getPublicKey() {
        return (PublicKey) attesterKey.getPublicKey();
    }

    @Override
    public X509Certificate getCertificate() {
        return attesterKey.getCertificate();
    }

    @Override
    public String attestWalletKey(String clientId, KeyWrapper key) {
        String algorithm = key.getAlgorithm();
        JWK jwk = JWKBuilder.create()
                .kid(key.getKid())
                .algorithm(algorithm)
                .rsa(key.getPublicKey(), key.getCertificate());
        ClientAttestationJwt body = new ClientAttestationJwt()
                .issuer(issuer)
                .subject(clientId)
                .confirmation(jwk)
                .issuedNowWithTTL(300); // 5min
        return new JWSBuilder()
                .type(OAUTH_CLIENT_ATTESTATION_JWT_TYPE)
                .kid(attesterKey.getKid())
                .jsonContent(body)
                .sign(new AsymmetricSignatureSignerContext(attesterKey));
    }
}
