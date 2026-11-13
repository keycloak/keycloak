package org.keycloak.tests.x509;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.def.DefaultCryptoProvider;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

final class CrlGenerator {

    private static final long ONE_DAY_MILLIS = 86_400_000L;

    private CrlGenerator() {
    }

    static byte[] generateCrl(Date nextUpdate) throws Exception {
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "KC CRL Test CA");
        Date thisUpdate = new Date(nextUpdate.getTime() - ONE_DAY_MILLIS);
        X509v2CRLBuilder builder = new X509v2CRLBuilder(X500Name.getInstance(caCert.getSubjectX500Principal().getEncoded()), thisUpdate);
        builder.setNextUpdate(nextUpdate);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyIntegration.PROVIDER).build(keyPair.getPrivate());
        return builder.build(signer).getEncoded();
    }

    static byte[] generateValidCrl() throws Exception {
        return generateCrl(new Date(System.currentTimeMillis() + ONE_DAY_MILLIS));
    }

    static byte[] generateStaleCrl() throws Exception {
        return generateCrl(new Date(System.currentTimeMillis() - ONE_DAY_MILLIS));
    }
}
