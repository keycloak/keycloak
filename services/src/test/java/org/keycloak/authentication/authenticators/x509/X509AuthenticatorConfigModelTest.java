package org.keycloak.authentication.authenticators.x509;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;

import org.keycloak.common.crypto.UserIdentityExtractor;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.rule.CryptoInitRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SERIALNUMBER;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTALTNAME_OTHERNAME;

/**
 * author Pascal Knueppel <br>
 * created at: 02.12.2019 - 10:59 <br>
 * <br>
 *
 */
public class X509AuthenticatorConfigModelTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    /**
     * this test will verify that no exception occurs if no settings are stored for the timestamp validation
     */
    @Test
    public void testTimestampValidationAttributeReturnsNull() {
        X509AuthenticatorConfigModel configModel = new X509AuthenticatorConfigModel();
        Assert.assertNull(configModel.getConfig().get(AbstractX509ClientCertificateAuthenticator.TIMESTAMP_VALIDATION));
        Assert.assertFalse(configModel.isCertValidationEnabled());
    }

    /**
     * this test will verify that no exception occurs if no settings are stored for the certificate policy validation
     */
    @Test
    public void testCertificatePolicyValidationAttributeReturnsNull() {
        X509AuthenticatorConfigModel configModel = new X509AuthenticatorConfigModel();
        Assert.assertNull(configModel.getConfig().get(AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY));
        Assert.assertNull(configModel.getCertificatePolicy());
    }

    /**
     * this test will verify that no exception occurs and ALL will be returned if no settings are stored for the certificate policy mode setting
     */
    @Test
    public void testCertificatePolicyModeValidationAttributeReturnsAll() {
        X509AuthenticatorConfigModel configModel = new X509AuthenticatorConfigModel();
        Assert.assertNull(configModel.getConfig().get(AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY_MODE));
        Assert.assertEquals(AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY_MODE_ALL, configModel.getCertificatePolicyMode().getMode());
    }

    @Test
    public void testSubjectAltNameOtherNameUsesRegularExpression() throws Exception {
        X509AuthenticatorConfigModel configModel = new X509AuthenticatorConfigModel()
                .setMappingSourceType(SUBJECTALTNAME_OTHERNAME)
                .setRegularExpression("(.*?)(?:@|$)");

        UserIdentityExtractor extractor = AbstractX509ClientCertificateAuthenticator.UserIdentityExtractorBuilder.fromConfig(configModel);
        String userIdentity = (String) extractor.extractUserIdentity(new X509Certificate[] { getCertificate("/certs/UPN-cert.pem") });

        assertEquals("test-user", userIdentity);
    }

    @Test
    public void testSerialNumberIgnoresRegularExpression() throws Exception {
        X509Certificate certificate = getCertificate("/certs/UPN-cert.pem");
        X509AuthenticatorConfigModel configModel = new X509AuthenticatorConfigModel()
                .setMappingSourceType(SERIALNUMBER)
                .setRegularExpression("(nomatch)");

        UserIdentityExtractor extractor = AbstractX509ClientCertificateAuthenticator.UserIdentityExtractorBuilder.fromConfig(configModel);
        String userIdentity = (String) extractor.extractUserIdentity(new X509Certificate[] { certificate });

        assertEquals(certificate.getSerialNumber().toString(), userIdentity);
    }

    private X509Certificate getCertificate(String resourceFilename) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourceFilename)) {
            Assert.assertNotNull("Test certificate resource not found: " + resourceFilename, is);
            String certificate = StreamUtil.readString(is, Charset.defaultCharset());
            return PemUtils.decodeCertificate(certificate);
        }
    }
}
