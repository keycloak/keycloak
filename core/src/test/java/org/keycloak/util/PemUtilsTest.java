package org.keycloak.util;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.rule.CryptoInitRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public abstract class PemUtilsTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testGenerateThumbprintSha1() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        String encoded = org.keycloak.common.util.PemUtils.generateThumbprint(test, "SHA-1");
        assertEquals(27, encoded.length());
    }

    @Test
    public void testGenerateThumbprintSha256() throws NoSuchAlgorithmException {
        String[] test = new String[] {"abcdefg"};
        String encoded = org.keycloak.common.util.PemUtils.generateThumbprint(test, "SHA-256");
        assertEquals(43, encoded.length());
    }

    @Test
    public void testEncodeAndDecodeGeneratedObjects() {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "FooBar");

        // Test encoding/decoding private key
        String encodedPrivateKey = PemUtils.encodeKey(keyPair.getPrivate());
        PrivateKey decodedPrivateKey = PemUtils.decodePrivateKey(encodedPrivateKey);
        assertEquals(decodedPrivateKey, keyPair.getPrivate());

        // Test encoding/decoding public key
        String encodedPublicKey = PemUtils.encodeKey(keyPair.getPublic());
        PublicKey decodedPublicKey = PemUtils.decodePublicKey(encodedPublicKey);
        assertEquals(decodedPublicKey, keyPair.getPublic());

        // Test encoding/decoding certificate
        String encodedCertificate = PemUtils.encodeCertificate(certificate);
        Certificate decodedCertificate = PemUtils.decodeCertificate(encodedCertificate);
        assertEquals(decodedCertificate, certificate);
    }

    @Test
    public void testDecodeObjectsInPEMFormat() {
        String privateKey1 = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
        String publicKey1 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
        String publicKeyEC = "-----BEGIN PUBLIC KEY-----\n"
                + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAElyCs9XI47lFR5l4WafsZZrAiUmEr\n"
                + "+kYeStgx3tyPntt3YNfs6kAVNozI4aJqdqDjITJWatHm6boJ0BRLPNphRA==\n"
                + "-----END PUBLIC KEY-----";
        String cert1 = "MIICnTCCAYUCBgFPPLDaTzANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdjbGllbnQxMB4XDTE1MDgxNzE3MjI0N1oXDTI1MDgxNzE3MjQyN1owEjEQMA4GA1UEAwwHY2xpZW50MTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIUjjgv+V3s96O+Za9002Lp/trtGuHBeaeVL9dFKMKzO2MPqdRmHB4PqNlDdd28Rwf5Xn6iWdFpyUKOnI/yXDLhdcuFpR0sMNK/C9Lt+hSpPFLuzDqgtPgDotlMxiHIWDOZ7g9/gPYNXbNvjv8nSiyqoguoCQiiafW90bPHsiVLdP7ZIUwCcfi1qQm7FhxRJ1NiW5dvUkuCnnWEf0XR+Wzc5eC9EgB0taLFiPsSEIlWMm5xlahYyXkPdNOqZjiRnrTWm5Y4uk8ZcsD/KbPTf/7t7cQXipVaswgjdYi1kK2/zRwOhg1QwWFX/qmvdd+fLxV0R6VqRDhn7Qep2cxwMxLsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAKE6OA46sf20bz8LZPoiNsqRwBUDkaMGXfnob7s/hJZIIwDEx0IAQ3uKsG7q9wb+aA6s+v7S340zb2k3IxuhFaHaZpAd4CyR5cn1FHylbzoZ7rI/3ASqHDqpljdJaFqPH+m7nZWtyDvtZf+gkZ8OjsndwsSBK1d/jMZPp29qYbl1+XfO7RCp/jDqro/R3saYFaIFiEZPeKn1hUJn6BO48vxH1xspSu9FmlvDOEAOz4AuM58z4zRMP49GcFdCWr1wkonJUHaSptJaQwmBwLFUkCbE5I1ixGMb7mjEud6Y5jhfzJiZMo2U8RfcjNbrN0diZl3jB6LQIwESnhYSghaTjNQ==";
        String cert2 = "MIICnTCCAYUCBgFPPQDGxTANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdjbGllbnQxMB4XDTE1MDgxNzE4NTAwNVoXDTI1MDgxNzE4NTE0NVowEjEQMA4GA1UEAwwHY2xpZW50MTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMMw3PaBffWxgS2PYSDDBp6As+cNvv9kt2C4f/RDAGmvSIHPFev9kuQiKs3Oaws3ZsV4JG3qHEuYgnh9W4vfe3DwNwtD1bjL5FYBhPBFTw0lAQECYxaBHnkjHwUKp957FqdSPPICm3LjmTcEdlH+9dpp9xHCMbbiNiWDzWI1xSxC8Fs2d0hwz1sd+Q4QeTBPIBWcPM+ICZtNG5MN+ORfayu4X+Me5d0tXG2fQO//rAevk1i5IFjKZuOjTwyKB5SJIY4b8QTeg0g/50IU7Ht00Pxw6CK02dHS+FvXHasZlD3ckomqCDjStTBWdhJo5dST0CbOqalkkpLlCCbGA1yEQRsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAUIMeJ+EAo8eNpCG/nXImacjrKakbFnZYBGD/gqeTGaZynkX+jgBSructTHR83zSH+yELEhsAy+3BfK4EEihp+PEcRnK2fASVkHste8AQ7rlzC+HGGirlwrVhWCdizNUCGK80DE537IZ7nmZw6LFG9P5/Q2MvCsOCYjRUvMkukq6TdXBXR9tETwZ+0gpSfsOxjj0ZF7ftTRUSzx4rFfcbM9fRNdVizdOuKGc8HJPA5lLOxV6CyaYIvi3y5RlQI1OHeS34lE4w9CNPRFa/vdxXvN7ClyzA0HMFNWxBN7pC/Ht/FbhSvaAagJBHg+vCrcY5C26Oli7lAglf/zZrwUPs0w==";

        // RSA key in the "traditional" PKCS1 format
        String privateKey2 = "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIICXAIBAAKBgQCjcGqTkOq0CR3rTx0ZSQSIdTrDrFAYl29611xN8aVgMQIWtDB/\n" +
                "lD0W5TpKPuU9iaiG/sSn/VYt6EzN7Sr332jj7cyl2WrrHI6ujRswNy4HojMuqtfa\n" +
                "b5FFDpRmCuvl35fge18OvoQTJELhhJ1EvJ5KUeZiuJ3u3YyMnxxXzLuKbQIDAQAB\n" +
                "AoGAPrNDz7TKtaLBvaIuMaMXgBopHyQd3jFKbT/tg2Fu5kYm3PrnmCoQfZYXFKCo\n" +
                "ZUFIS/G1FBVWWGpD/MQ9tbYZkKpwuH+t2rGndMnLXiTC296/s9uix7gsjnT4Naci\n" +
                "5N6EN9pVUBwQmGrYUTHFc58ThtelSiPARX7LSU2ibtJSv8ECQQDWBRrrAYmbCUN7\n" +
                "ra0DFT6SppaDtvvuKtb+mUeKbg0B8U4y4wCIK5GH8EyQSwUWcXnNBO05rlUPbifs\n" +
                "DLv/u82lAkEAw39sTJ0KmJJyaChqvqAJ8guulKlgucQJ0Et9ppZyet9iVwNKX/aW\n" +
                "9UlwGBMQdafQ36nd1QMEA8AbAw4D+hw/KQJBANJbHDUGQtk2hrSmZNoV5HXB9Uiq\n" +
                "7v4N71k5ER8XwgM5yVGs2tX8dMM3RhnBEtQXXs9LW1uJZSOQcv7JGXNnhN0CQBZe\n" +
                "nzrJAWxh3XtznHtBfsHWelyCYRIAj4rpCHCmaGUM6IjCVKFUawOYKp5mmAyObkUZ\n" +
                "f8ue87emJLEdynC1CLkCQHduNjP1hemAGWrd6v8BHhE3kKtcK6KHsPvJR5dOfzbd\n" +
                "HAqVePERhISfN6cwZt5p8B3/JUwSR8el66DF7Jm57BM=\n" +
                "-----END RSA PRIVATE KEY-----";

        testPrivateKeyEncodeDecode(privateKey1);
        testPublicKeyEncodeDecode(publicKey1);
        testPublicKeyEncodeDecode(publicKeyEC);
        testPrivateKeyEncodeDecode(PemUtils.removeBeginEnd(privateKey2).replace("\n", ""));
        testCertificateEncodeDecode(cert1);
        testCertificateEncodeDecode(cert2);
    }

    @Test
    public void testPrivateKeyInPKCS8Format() {
        String privateKeyPkcs8 = "-----BEGIN PRIVATE KEY-----\n" +
                "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKNwapOQ6rQJHetP\n" +
                "HRlJBIh1OsOsUBiXb3rXXE3xpWAxAha0MH+UPRblOko+5T2JqIb+xKf9Vi3oTM3t\n" +
                "KvffaOPtzKXZauscjq6NGzA3LgeiMy6q19pvkUUOlGYK6+Xfl+B7Xw6+hBMkQuGE\n" +
                "nUS8nkpR5mK4ne7djIyfHFfMu4ptAgMBAAECgYA+s0PPtMq1osG9oi4xoxeAGikf\n" +
                "JB3eMUptP+2DYW7mRibc+ueYKhB9lhcUoKhlQUhL8bUUFVZYakP8xD21thmQqnC4\n" +
                "f63asad0ycteJMLb3r+z26LHuCyOdPg1pyLk3oQ32lVQHBCYathRMcVznxOG16VK\n" +
                "I8BFfstJTaJu0lK/wQJBANYFGusBiZsJQ3utrQMVPpKmloO2++4q1v6ZR4puDQHx\n" +
                "TjLjAIgrkYfwTJBLBRZxec0E7TmuVQ9uJ+wMu/+7zaUCQQDDf2xMnQqYknJoKGq+\n" +
                "oAnyC66UqWC5xAnQS32mlnJ632JXA0pf9pb1SXAYExB1p9Dfqd3VAwQDwBsDDgP6\n" +
                "HD8pAkEA0lscNQZC2TaGtKZk2hXkdcH1SKru/g3vWTkRHxfCAznJUaza1fx0wzdG\n" +
                "GcES1Bdez0tbW4llI5By/skZc2eE3QJAFl6fOskBbGHde3Oce0F+wdZ6XIJhEgCP\n" +
                "iukIcKZoZQzoiMJUoVRrA5gqnmaYDI5uRRl/y57zt6YksR3KcLUIuQJAd242M/WF\n" +
                "6YAZat3q/wEeETeQq1wrooew+8lHl05/Nt0cCpV48RGEhJ83pzBm3mnwHf8lTBJH\n" +
                "x6XroMXsmbnsEw==\n" +
                "-----END PRIVATE KEY-----";

        PrivateKey decodedPrivateKey1 = PemUtils.decodePrivateKey(privateKeyPkcs8);

        // Assert it works also when the "begin/end" section is removed
        String pk = PemUtils.removeBeginEnd(privateKeyPkcs8).replace("\n", "");
        PrivateKey decodedPrivateKey2 = PemUtils.decodePrivateKey(pk);
        Assert.assertEquals(decodedPrivateKey1, decodedPrivateKey2);

        String ecPrivateKeyPkcs8 = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgO1oavi4kqVFc/rxj\n" +
                "24SJivHXq7buWX58U0tswYikPwyhRANCAASCIp6nVvOk9flbUrMW7JPDmyaXCnDc\n" +
                "Q2uMfvxVWIJzBuhG6VDoeFPk3yf2EN5t7Q8FU5jPSp6gJz9xbaFYYLL6\n" +
                "-----END PRIVATE KEY-----";

        PrivateKey decodedEcPrivateKey = PemUtils.decodePrivateKey(ecPrivateKeyPkcs8);
        Assert.assertEquals("EC", decodedEcPrivateKey.getAlgorithm());
    }

    @Test
    public void testDecodeCertificateBundle() {
        String certBundleEC = "-----BEGIN CERTIFICATE-----\n" +
                "MIIBUTCB96ADAgECAggYMJVpV/BvyTAKBggqhkjOPQQDAjARMQ8wDQYDVQQDEwZz\n" +
                "dWItY2EwIBcNMDAwMTAxMDkwMDAwWhgPMjEwMDAxMDEwOTAwMDBaMBUxEzARBgNV\n" +
                "BAMTCmVuZC1lbnRpdHkwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAASCIp6nVvOk\n" +
                "9flbUrMW7JPDmyaXCnDcQ2uMfvxVWIJzBuhG6VDoeFPk3yf2EN5t7Q8FU5jPSp6g\n" +
                "Jz9xbaFYYLL6ozMwMTAOBgNVHQ8BAf8EBAMCBaAwHwYDVR0jBBgwFoAU3etTPCDC\n" +
                "f31HxBuYWWjF9ImW4ccwCgYIKoZIzj0EAwIDSQAwRgIhAKpP+HBEvUWEfjdr2qD2\n" +
                "sw/bVLtW1HnpqVnQm2i/kDp2AiEA6F+kKyMNu+jGKmzj0Pf6v0cj0c+f00bqoJdk\n" +
                "h+GXGnM=\n" +
                "-----END CERTIFICATE-----\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIBejCCAR+gAwIBAgIIGDCVaVflNG8wCgYIKoZIzj0EAwIwDTELMAkGA1UEAxMC\n" +
                "Y2EwIBcNMDAwMTAxMDkwMDAwWhgPMjEwMDAxMDEwOTAwMDBaMBExDzANBgNVBAMT\n" +
                "BnN1Yi1jYTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABI4bNe/0VXXojhjdh76p\n" +
                "89esSheOT5WEBVQnJUvDBDSRoxRiFx2BEdPaVn8L4cCbaZIxLsoJusOJadm7Eltc\n" +
                "h3qjYzBhMA4GA1UdDwEB/wQEAwIBBjAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQW\n" +
                "BBTd61M8IMJ/fUfEG5hZaMX0iZbhxzAfBgNVHSMEGDAWgBQ9q0KnjYuFWTSXf4YM\n" +
                "Taz6vbNVRTAKBggqhkjOPQQDAgNJADBGAiEA3y9pa2JMhtM898f6NOZhezoHzj1a\n" +
                "2JQIZRLQbOTjk0wCIQCg9A8414teP9whzRGSxM4eJNExdfHeJBYjDD345EW0vg==\n" +
                "-----END CERTIFICATE-----";

        X509Certificate[] certs = PemUtils.decodeCertificates(certBundleEC);
        Assert.assertEquals(2, certs.length);
        Assert.assertEquals("CN=end-entity", certs[0].getSubjectX500Principal().getName());
        Assert.assertEquals("CN=sub-ca", certs[1].getSubjectX500Principal().getName());

        String invalidCertBundle = "foo\n";
        Assert.assertThrows(PemException.class, () -> {
            PemUtils.decodeCertificates(invalidCertBundle);
        });

    }

    private void testPrivateKeyEncodeDecode(String origPrivateKeyPem) {
        PrivateKey decodedPrivateKey = PemUtils.decodePrivateKey(origPrivateKeyPem);
        String encodedPrivateKey = PemUtils.encodeKey(decodedPrivateKey);
        assertEquals(origPrivateKeyPem, encodedPrivateKey);
    }

    private void testPublicKeyEncodeDecode(String origPublicKeyPem) {
        PublicKey decodedPublicKey = PemUtils.decodePublicKey(origPublicKeyPem);
        String encodedPublicKey = PemUtils.encodeKey(decodedPublicKey);
        assertEquals(PemUtils.removeBeginEnd(origPublicKeyPem), encodedPublicKey);
    }

    private void testCertificateEncodeDecode(String origCertPem) {
        X509Certificate decodedCert = PemUtils.decodeCertificate(origCertPem);
        String encodedCert = PemUtils.encodeCertificate(decodedCert);
        assertEquals(origCertPem, encodedCert);
    }
}
