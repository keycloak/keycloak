package org.keycloak.authentication.authenticators.x509;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.KeyType;
import org.keycloak.rule.CryptoInitRule;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY_MODE_ALL;
import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY_MODE_ANY;

/**
 * author Pascal Knueppel <br>
 * created at: 07.11.2019 - 16:24 <br>
 * <br>
 *
 */
public class CertificateValidatorTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    /**
     * will validate that the certificate validation succeeds if the certificate is currently valid
     */
    @Test
    public void testValidityOfCertificatesSuccess() throws GeneralSecurityException {
        KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
        kpg.initialize(512);
        KeyPair keyPair = kpg.generateKeyPair();
        X509Certificate certificate = CryptoIntegration.getProvider().getCertificateUtils()
                .createServicesTestCertificate("CN=keycloak-test", new Date(),
                        new Date(System.currentTimeMillis() + 1000L * 60), keyPair);

        CertificateValidator.CertificateValidatorBuilder builder =
            new CertificateValidator.CertificateValidatorBuilder();
        CertificateValidator validator = builder
          .timestampValidation()
            .enabled(true)
          .build(new X509Certificate[] { certificate });
        try {
            validator.validateTimestamps();
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    /**
     * will validate that the certificate validation throws an exception if the certificate is not valid yet
     */
    @Test
    public void testValidityOfCertificatesNotValidYet() throws GeneralSecurityException {
        KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
        kpg.initialize(512);
        KeyPair keyPair = kpg.generateKeyPair();
        X509Certificate certificate = CryptoIntegration.getProvider().getCertificateUtils()
                .createServicesTestCertificate("CN=keycloak-test", new Date(System.currentTimeMillis() + 1000L * 60),
                        new Date(System.currentTimeMillis() + 1000L * 60), keyPair);

        CertificateValidator.CertificateValidatorBuilder builder =
            new CertificateValidator.CertificateValidatorBuilder();
        CertificateValidator validator = builder
          .timestampValidation()
            .enabled(true)
          .build(new X509Certificate[] { certificate });
        try {
            validator.validateTimestamps();
            Assert.fail("certificate validation must fail for certificate is not valid yet");
        } catch (Exception ex) {
            MatcherAssert.assertThat(ex.getMessage(), Matchers.containsString("not valid yet"));
            Assert.assertEquals(GeneralSecurityException.class, ex.getClass());
        }
    }

    /**
     * will validate that the certificate validation throws an exception if the certificate has expired
     */
    @Test
    public void testValidityOfCertificatesHasExpired() throws GeneralSecurityException {
        KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
        kpg.initialize(512);
        KeyPair keyPair = kpg.generateKeyPair();
        X509Certificate certificate = CryptoIntegration.getProvider().getCertificateUtils()
                .createServicesTestCertificate("CN=keycloak-test",
                        new Date(System.currentTimeMillis() - 1000L * 60 * 2),
                        new Date(System.currentTimeMillis() - 1000L * 60), keyPair);

        CertificateValidator.CertificateValidatorBuilder builder =
            new CertificateValidator.CertificateValidatorBuilder();
        CertificateValidator validator = builder
          .timestampValidation()
            .enabled(true)
          .build(new X509Certificate[] { certificate });
        try {
            validator.validateTimestamps();
            Assert.fail("certificate validation must fail for certificate has expired");
        } catch (Exception ex) {
            MatcherAssert.assertThat(ex.getMessage(), Matchers.containsString("has expired"));
            Assert.assertEquals(GeneralSecurityException.class, ex.getClass());
        }
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ALL, no policies are requested and the cert does not contain any policy
     */
    @Test
    public void testCertificatePolicyModeAllNotRequestedAndNotPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation(null, CERTIFICATE_POLICY_MODE_ALL);
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ALL, no policies are requested and the cert does contains a policy
     */
    @Test
    public void testCertificatePolicyModeAllNotRequestedAndOnePresent() throws GeneralSecurityException {
        testCertificatePolicyValidation(null, CERTIFICATE_POLICY_MODE_ALL, "1.3.76.16.2.1");
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ALL, no policies are requested and the cert contains two policies
     */
    @Test
    public void testCertificatePolicyModeAllNotRequestedAndTwoPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation(null, CERTIFICATE_POLICY_MODE_ALL, "1.3.76.16.2.1", "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ALL, one policy is requested and the cert does not contain any policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAllOneRequestedAndNotPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ALL);
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ALL, one policy is requested and the cert contains that policy
     */
    @Test
    public void testCertificatePolicyModeAllOneRequestedAndOnePresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ALL, "1.3.76.16.2.1");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ALL, one policy is requested and the cert contains a different policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAllOneRequestedAndOnePresentDifferent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ALL, "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ALL, one policy is requested and the cert contains that policy and one more
     */
    @Test
    public void testCertificatePolicyModeAllOneRequestedAndTwoPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ALL, "1.3.76.16.2.1", "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ALL, one policy is requested and the cert contains a different policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAllOneRequestedAndTwoPresentDifferent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ALL, "1.2.3.4.5", "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ALL, two policies are requested and the cert does not contain any policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAllTwoRequestedAndNotPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1,1.2.3.4.5.6", CERTIFICATE_POLICY_MODE_ALL);
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ALL, two policies are requested and the cert contains one different policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAllTwoRequestedAndOnePresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1,1.2.3.4.5.6", CERTIFICATE_POLICY_MODE_ALL, "1.3.76.16.2.1");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ALL, two policies are requested and the cert contains one different policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAllTwoRequestedAndOnePresentDifferent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1,1.2.3.4.5.6", CERTIFICATE_POLICY_MODE_ALL, "1.2.3.4");
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ALL, two policies are requested and the cert contains those two policies
     */
    @Test
    public void testCertificatePolicyModeAllTwoRequestedAndTwoPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1,1.2.3.4.5.6", CERTIFICATE_POLICY_MODE_ALL, "1.3.76.16.2.1", "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ANY, no policies are requested and the cert does not contain any policy
     */
    @Test
    public void testCertificatePolicyModeAnyNotRequestedAndNotPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation(null, CERTIFICATE_POLICY_MODE_ANY);
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ANY, no policies are requested and the cert does contains a policy
     */
    @Test
    public void testCertificatePolicyModeAnyNotRequestedAndOnePresent() throws GeneralSecurityException {
        testCertificatePolicyValidation(null, CERTIFICATE_POLICY_MODE_ANY, "1.3.76.16.2.1");
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ANY, no policies are requested and the cert contains two policies
     */
    @Test
    public void testCertificatePolicyModeAnyNotRequestedAndTwoPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation(null, CERTIFICATE_POLICY_MODE_ANY, "1.3.76.16.2.1", "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ANY, one policy is requested and the cert does not contain any policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAnyOneRequestedAndNotPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ANY);
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ANY, one policy is requested and the cert contains that policy
     */
    @Test
    public void testCertificatePolicyModeAnyOneRequestedAndOnePresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ANY, "1.3.76.16.2.1");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ANY, one policy is requested and the cert contains a different policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAnyOneRequestedAndOnePresentDifferent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ANY, "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ANY, one policy is requested and the cert contains that policy and one more
     */
    @Test
    public void testCertificatePolicyModeAnyOneRequestedAndTwoPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ANY, "1.3.76.16.2.1", "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ANY, one policy is requested and the cert contains a different policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAnyOneRequestedAndTwoPresentDifferent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1", CERTIFICATE_POLICY_MODE_ANY, "1.2.3.4.5", "1.2.3.4.5.6");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ANY, two policies are requested and the cert does not contain any policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAnyTwoRequestedAndNotPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1,1.2.3.4.5.6", CERTIFICATE_POLICY_MODE_ANY);
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ANY, two policies are requested and the cert contains one policy
     */
    @Test
    public void testCertificatePolicyModeAnyTwoRequestedAndOnePresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1,1.2.3.4.5.6", CERTIFICATE_POLICY_MODE_ANY, "1.3.76.16.2.1");
    }

    /**
     * will validate that the certificate policy validation WILL throw exceptions
     * if mode=ANY, two policies are requested and the cert contains one different policy
     */
    @Test(expected = GeneralSecurityException.class)
    public void testCertificatePolicyModeAnyTwoRequestedAndOnePresentDifferent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1,1.2.3.4.5.6", CERTIFICATE_POLICY_MODE_ANY, "1.2.3.4");
    }

    /**
     * will validate that the certificate policy validation won't throw exceptions
     * if mode=ANY, two policies are requested and the cert contains those two policies
     */
    @Test
    public void testCertificatePolicyModeAnyTwoRequestedAndTwoPresent() throws GeneralSecurityException {
        testCertificatePolicyValidation("1.3.76.16.2.1,1.2.3.4.5.6", CERTIFICATE_POLICY_MODE_ANY, "1.3.76.16.2.1", "1.2.3.4.5.6");
    }

    // Helper to test various certificate policy validation combinations
    private void testCertificatePolicyValidation(String expectedPolicy, String mode, String... certificatePolicyOid)
        throws GeneralSecurityException
    {
        KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
        kpg.initialize(512);
        KeyPair keyPair = kpg.generateKeyPair();
        X509Certificate certificate = CryptoIntegration.getProvider().getCertificateUtils()
                .createServicesTestCertificate("CN=keycloak-test",
                        new Date(System.currentTimeMillis() - 1000L * 60 * 2),
                        new Date(System.currentTimeMillis() - 1000L * 60), keyPair, certificatePolicyOid);

        CertificateValidator.CertificateValidatorBuilder builder =
            new CertificateValidator.CertificateValidatorBuilder();
        CertificateValidator validator = builder
            .certificatePolicy()
                .mode(mode)
                .parse(expectedPolicy)
            .build(new X509Certificate[] { certificate });

        validator.validatePolicy();
    }

}
