package org.keycloak.authentication.authenticators.x509;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;

import org.junit.ClassRule;
import org.keycloak.rule.CryptoInitRule;
import org.junit.Test;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.StreamUtil;

public class CertificatePemIdentityExtractorTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testExtractsCertInPemFormat() throws Exception {
        InputStream is = getClass().getResourceAsStream("/certs/UPN-cert.pem");
        X509Certificate x509Certificate = PemUtils.decodeCertificate(StreamUtil.readString(is, Charset.defaultCharset()));
        String certificatePem = PemUtils.encodeCertificate(x509Certificate);

        X509AuthenticatorConfigModel config = new X509AuthenticatorConfigModel();
        UserIdentityExtractor extractor = UserIdentityExtractor.getCertificatePemIdentityExtractor(config);

        String userIdentity = (String) extractor.extractUserIdentity(new X509Certificate[]{x509Certificate});

        assertEquals(certificatePem, userIdentity);
    }

}
