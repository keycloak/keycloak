package org.keycloak.protocol.docker.installation.compose;

import org.keycloak.common.util.PemUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;

public final class DockerCertFileUtils {
    public static final String BEGIN_CERT = PemUtils.BEGIN_CERT;
    public static final String END_CERT = PemUtils.END_CERT;
    public static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    public static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    public static final String LINE_SEPERATOR = System.getProperty("line.separator");

    private DockerCertFileUtils() {
    }

    public static String formatCrtFileContents(final Certificate certificate) throws CertificateEncodingException {
        return encodeAndPrettify(BEGIN_CERT, certificate.getEncoded(), END_CERT);
    }

    public static String formatPrivateKeyContents(final PrivateKey privateKey) {
        return encodeAndPrettify(BEGIN_PRIVATE_KEY, privateKey.getEncoded(), END_PRIVATE_KEY);
    }

    public static String formatPublicKeyContents(final PublicKey publicKey) {
        return encodeAndPrettify(BEGIN_CERT, publicKey.getEncoded(), END_CERT);
    }

    private static String encodeAndPrettify(final String header, final byte[] rawCrtText, final String footer) {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPERATOR.getBytes());
        final String encodedCertText = new String(encoder.encode(rawCrtText));
        final String prettified_cert = header + LINE_SEPERATOR + encodedCertText + LINE_SEPERATOR + footer;
        return prettified_cert;
    }
}
