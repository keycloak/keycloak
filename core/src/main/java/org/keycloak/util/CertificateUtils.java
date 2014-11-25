package org.keycloak.util;

import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CertificateUtils {
    static {
        BouncyIntegration.init();
    }
    public static X509Certificate generateV3Certificate(KeyPair keyPair, PrivateKey caPrivateKey, X509Certificate caCert, String subject) throws Exception {

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal subjectName = new X500Principal("CN=" + subject);

        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        certGen.setSerialNumber(serialNumber);
        certGen.setIssuerDN(caCert.getSubjectX500Principal());
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 100000));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 10);
        certGen.setNotAfter(calendar.getTime());
        certGen.setSubjectDN(subjectName);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(keyPair.getPublic()));

        X509Certificate cert = certGen.generate(caPrivateKey, "BC");   // note: private key of CA
        return cert;
    }

    public static X509Certificate generateV1SelfSignedCertificate(KeyPair keyPair, String subject) throws Exception {
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal subjectPrincipal = new X500Principal("CN=" + subject);
        certGen.setSerialNumber(serialNumber);
        certGen.setIssuerDN(subjectPrincipal);
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 100000));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 10);
        certGen.setNotAfter(calendar.getTime());
        certGen.setSubjectDN(subjectPrincipal);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        X509Certificate cert = certGen.generate(keyPair.getPrivate(), "BC");
        return cert;
    }
}
