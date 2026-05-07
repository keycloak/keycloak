package org.keycloak.tests.oid4vc.abca;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.keycloak.crypto.KeyWrapper;

public interface OIDCClientAttester {

    String getIssuer();

    PublicKey getPublicKey();

    X509Certificate getCertificate();

    String attestWalletKey(String clientId, KeyWrapper pubKey);

}
