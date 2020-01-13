package org.keycloak.authentication.authenticators.x509;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * author Pascal Knueppel <br>
 * created at: 07.11.2019 - 16:24 <br>
 * <br>
 *
 */
public class CertificateValidatorTest {

    private static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();

    /**
     * will validate that the certificate validation succeeds if the certificate is currently valid
     */
    @Test
    public void testValidityOfCertificatesSuccess() throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair keyPair = kpg.generateKeyPair();
        X509Certificate certificate =
            createCertificate("CN=keycloak-test", new Date(),
                new Date(System.currentTimeMillis() + 1000L * 60), keyPair);

        CertificateValidator.CertificateValidatorBuilder builder =
            new CertificateValidator.CertificateValidatorBuilder();
        CertificateValidator validator = builder.build(new X509Certificate[] { certificate });
        try {
            validator.validateTimestamps(true);
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
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair keyPair = kpg.generateKeyPair();
        X509Certificate certificate =
            createCertificate("CN=keycloak-test", new Date(System.currentTimeMillis() + 1000L * 60),
                new Date(System.currentTimeMillis() + 1000L * 60), keyPair);

        CertificateValidator.CertificateValidatorBuilder builder =
            new CertificateValidator.CertificateValidatorBuilder();
        CertificateValidator validator = builder.build(new X509Certificate[] { certificate });
        try {
            validator.validateTimestamps(true);
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
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair keyPair = kpg.generateKeyPair();
        X509Certificate certificate =
            createCertificate("CN=keycloak-test", new Date(System.currentTimeMillis() - 1000L * 60 * 2),
                new Date(System.currentTimeMillis() - 1000L * 60), keyPair);

        CertificateValidator.CertificateValidatorBuilder builder =
            new CertificateValidator.CertificateValidatorBuilder();
        CertificateValidator validator = builder.build(new X509Certificate[] { certificate });
        try {
            validator.validateTimestamps(true);
            Assert.fail("certificate validation must fail for certificate has expired");
        } catch (Exception ex) {
            MatcherAssert.assertThat(ex.getMessage(), Matchers.containsString("has expired"));
            Assert.assertEquals(GeneralSecurityException.class, ex.getClass());
        }
    }


    /**
     * will create a self-signed certificate
     *
     * @param dn the DN of the subject and issuer
     * @param startDate startdate of the validity of the created certificate
     * @param expiryDate expiration date of the created certificate
     * @param keyPair the keypair that is used to create the certificate
     * @return a X509-Certificate in version 3
     */
    public X509Certificate createCertificate(String dn,
                                             Date startDate,
                                             Date expiryDate,
                                             KeyPair keyPair) {
        X500Name subjectDN = new X500Name(dn);
        X500Name issuerDN = new X500Name(dn);
        // @formatter:off
    SubjectPublicKeyInfo subjPubKeyInfo = SubjectPublicKeyInfo.getInstance(
                                                        ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()));
    // @formatter:on
        BigInteger serialNumber = new BigInteger(130, new SecureRandom());

        X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(issuerDN, serialNumber, startDate, expiryDate,
            subjectDN, subjPubKeyInfo);
        ContentSigner contentSigner = null;
        try {
            // @formatter:off
      contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                                                              .setProvider(BOUNCY_CASTLE_PROVIDER)
                                                              .build(keyPair.getPrivate());
      X509Certificate x509Certificate = new JcaX509CertificateConverter()
                                                              .setProvider(BOUNCY_CASTLE_PROVIDER)
                                                              .getCertificate(certGen.build(contentSigner));
      // @formatter:on
            return x509Certificate;
        } catch (CertificateException | OperatorCreationException e) {
            throw new IllegalStateException(e);
        }
    }

}
