package org.keycloak.tests.oid4vc.abca;

import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public interface OIDCClientAttester {

    String getIssuer();

    PublicKey getPublicKey();

    X509Certificate getCertificate();

    String attestWalletKey(String clientId, Key pubKey);

}
