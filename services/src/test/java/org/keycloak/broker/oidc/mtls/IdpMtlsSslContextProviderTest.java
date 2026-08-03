package org.keycloak.broker.oidc.mtls;

import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.KeyWrapper;

import javax.net.ssl.SSLContext;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class IdpMtlsSslContextProviderTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    private static KeyWrapper keyWithCert(String cn) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(kp, cn);
        KeyWrapper key = new KeyWrapper();
        key.setPrivateKey(kp.getPrivate());
        key.setPublicKey(kp.getPublic());
        key.setCertificate(cert);
        key.setCertificateChain(List.<X509Certificate>of(cert));
        return key;
    }

    @Test
    public void buildsSslContextFromKeyWrapperWithChain() throws Exception {
        KeyWrapper key = keyWithCert("CN=idp-client");
        SSLContext ctx = IdpMtlsSslContextProvider.buildSslContext(key, null);
        assertNotNull(ctx);
        assertNotNull(ctx.getSocketFactory());
    }

    @Test
    public void buildsSslContextFromKeyWrapperWithSingleCert() throws Exception {
        KeyWrapper key = keyWithCert("CN=idp-client-2");
        key.setCertificateChain(null); // force the single-cert fallback path
        SSLContext ctx = IdpMtlsSslContextProvider.buildSslContext(key, null);
        assertNotNull(ctx);
    }

    @Test
    public void rejectsNonPrivateKey() throws Exception {
        // Build a KeyWrapper whose privateKey field holds a plain Key (not a PrivateKey).
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(kp, "CN=bad");

        // An anonymous Key implementation that is NOT a PrivateKey.
        Key nonPrivate = new Key() {
            public String getAlgorithm() { return "RAW"; }
            public String getFormat() { return "RAW"; }
            public byte[] getEncoded() { return new byte[0]; }
        };

        KeyWrapper key = new KeyWrapper();
        key.setPrivateKey(nonPrivate);
        key.setCertificate(cert);

        assertThrows(IllegalStateException.class,
                () -> IdpMtlsSslContextProvider.buildSslContext(key, null));
    }
}
