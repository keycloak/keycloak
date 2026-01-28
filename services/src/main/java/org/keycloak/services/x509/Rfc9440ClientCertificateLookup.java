package org.keycloak.services.x509;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.keycloak.common.util.DerUtils;
import org.keycloak.http.HttpRequest;

import org.jboss.logging.Logger;

/**
 * The provider allows to extract a client certificate forwarded
 * to the keycloak middleware configured behind a reverse proxy that is
 * compliant with RFC 9440.
 *
 * @author <a href="mailto:seiferma.dev+kc@gmail.com">Stephan Seifermann</a>
 * @version $Revision: 1 $
 * @since 12/30/2024
 */
public class Rfc9440ClientCertificateLookup implements X509ClientCertificateLookup {

    public static class RfcViolationException extends Exception {
        public RfcViolationException(String rfc, String section, String details, Throwable cause) {
            super("Violation of RFC " + rfc + " (see section " + section + "): " + details, cause);
        }
    }

    public static class Rfc9440ViolationException extends RfcViolationException {
        public Rfc9440ViolationException(String section, String details) {
            this(section, details, null);
        }
        public Rfc9440ViolationException(String section, String details, Throwable cause) {
            super("9440", section, details, cause);
        }
    }

    public static class Rfc8941ViolationException extends RfcViolationException {
        public Rfc8941ViolationException(String section, String details) {
            this(section, details, null);
        }
        public Rfc8941ViolationException(String section, String details, Throwable cause) {
            super("8941", section, details, cause);
        }
    }

    private static final Logger log = Logger.getLogger(Rfc9440ClientCertificateLookup.class);
    protected final String sslClientCertHttpHeader;
    protected final String sslCertChainHttpHeader;
    protected final int certificateChainLength;

    public Rfc9440ClientCertificateLookup(String sslClientCertHttpHeader,
                                          String sslCertChainHttpHeader,
                                          int certificateChainLength) {
        this.sslClientCertHttpHeader = Optional.ofNullable(sslClientCertHttpHeader)
                .filter(s -> !s.isBlank())
                .orElseThrow(() ->  new IllegalArgumentException("sslClientCertHttpHeader"));

        this.sslCertChainHttpHeader = Optional.ofNullable(sslCertChainHttpHeader)
                .filter(s -> !s.isBlank())
                .orElseThrow(() ->  new IllegalArgumentException("sslCertChainHttpHeader"));

        this.certificateChainLength = certificateChainLength;
    }

    @Override
    public X509Certificate[] getCertificateChain(HttpRequest httpRequest) throws GeneralSecurityException {
        if (!httpRequest.isProxyTrusted()) {
            log.warnf("HTTP header \"%s\" is not trusted", sslClientCertHttpHeader);
            return null;
        }
        try {
            List<X509Certificate> chain = new ArrayList<>();
            X509Certificate clientCertificate = getClientCertificateFromHeader(httpRequest);
            if (clientCertificate != null) {
                chain.add(clientCertificate);
                chain.addAll(getClientCertificateChainFromHeader(httpRequest));
            }
            return chain.toArray(new X509Certificate[0]);
        } catch (RfcViolationException e) {
            throw new GeneralSecurityException(e);
        }
    }

    @Override
    public void close() {
        // intentionally left blank
    }

    /**
     * Extract the client certificate from the {@link #sslClientCertHttpHeader} header.
     *
     * @param httpRequest The request containing the headers.
     * @return The extracted certificate or null if no certificate was presented.
     * @throws RfcViolationException thrown if the header is missing or its value do not comply with the relevant RFCs.
     */
    protected X509Certificate getClientCertificateFromHeader(HttpRequest httpRequest) throws RfcViolationException {
        List<String> headerValues = httpRequest.getHttpHeaders().getRequestHeader(sslClientCertHttpHeader);
        if (headerValues.isEmpty()) {
            return null;
        }
        if (headerValues.size() > 1) {
            throw new Rfc9440ViolationException("2.2", "client cert header must occur at most once");
        }

        return parseCertificateFromHttpByteSequence(headerValues.get(0));
    }

    /**
     * Extract the certificate chain from the {@link #sslCertChainHttpHeader} header.
     *
     * @param httpRequest The request containing the headers.
     * @return A list of extracted certificates in the order of occurrence in the header.
     * @throws RfcViolationException thrown if the header values do not comply with the relevant RFCs.
     * @throws GeneralSecurityException thrown if the length of the chain is bigger than the configured maximum length (see {@link #certificateChainLength}).
     */
    protected List<X509Certificate> getClientCertificateChainFromHeader(HttpRequest httpRequest) throws RfcViolationException, GeneralSecurityException {
        List<String> chainHeaderValues = httpRequest.getHttpHeaders().getRequestHeader(sslCertChainHttpHeader);
        if (chainHeaderValues == null || chainHeaderValues.isEmpty()) {
            // header is optional as of sec. 2.3 of RFC 9440
            return Collections.emptyList();
        }

        // header may be split according to sec. 3.1 of RFC 8941
        List<String> encodedCerts = new ArrayList<>();
        for (String chainHeaderValue : chainHeaderValues) {
            // lists may contain multiple entries separated by comma followed by optional whitespace according to sec. 3.1 of RFC 8941
            String[] listEntries = chainHeaderValue.split(",\\s*");
            encodedCerts.addAll(Arrays.asList(listEntries));
        }

        // the chain might be bigger than the configured limit
        if (encodedCerts.size() > certificateChainLength) {
            throw new GeneralSecurityException(
                    "The amount of certificates in the chain header " + encodedCerts.size() +
                    " is bigger than the configured limit of " + certificateChainLength + "."
            );
        }

        // list entries are byte sequences encoded according to sec. 2.1 of RFC 9440
        List<X509Certificate> parsedCertificates = new ArrayList<>();
        for (String encodedCert : encodedCerts) {
            parsedCertificates.add(parseCertificateFromHttpByteSequence(encodedCert));
        }
        return parsedCertificates;
    }

    /**
     * Parses a X509 certificate from a byte sequence encoded according to sec. 2.1 of RFC 9440.
     *
     * @param byteSequence the byte sequence of a certificate encoded according to sec. 2.1 of RFC 9440
     * @return the extracted X509 certificate
     * @throws RfcViolationException thrown if input does not conform to RFC
     */
    protected static X509Certificate parseCertificateFromHttpByteSequence(String byteSequence) throws RfcViolationException {
        if (byteSequence.length() < 2 || !byteSequence.startsWith(":") || !byteSequence.endsWith(":")) {
            throw new Rfc8941ViolationException("3.3.5", "value is not encoded as byte sequence");
        }
        String base64EncodedByteSequence = byteSequence.substring(1, byteSequence.length() - 1);

        byte[] certificateBytes;
        try {
            certificateBytes = Base64.getMimeDecoder().decode(base64EncodedByteSequence);
        } catch (IllegalArgumentException e) {
            throw new Rfc9440ViolationException("2.1", "value does not contain base64 encoded content", e);
        }

        X509Certificate certificate;
        try (InputStream is = new ByteArrayInputStream(certificateBytes)) {
            certificate = DerUtils.decodeCertificate(is);
        } catch (Exception e) {
            throw new Rfc9440ViolationException("2.1", "value does not contain DER encoded certificate", e);
        }

        if (certificate == null) {
            throw new Rfc9440ViolationException("2.1", "value does not contain DER encoded certificate");
        }

        log.debugf("Parsed certificate : Subject DN=[%s]  SerialNumber=[%s]", certificate.getSubjectX500Principal(), certificate.getSerialNumber());
        return certificate;
    }

}
