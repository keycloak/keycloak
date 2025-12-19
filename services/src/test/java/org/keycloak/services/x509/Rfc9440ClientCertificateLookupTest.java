package org.keycloak.services.x509;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.http.HttpRequest;
import org.keycloak.services.resteasy.HttpRequestImpl;

import com.google.common.base.Splitter;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
import static org.junit.Assert.assertThrows;

public class Rfc9440ClientCertificateLookupTest {

    private static final String TEST_CLIENT_CERT_FILE = "/org/keycloak/test/services/x509/header_value_rfc_9440_client_cert";
    private static final String TEST_CLIENT_CHAIN_FILE = "/org/keycloak/test/services/x509/header_value_rfc_9440_client_chain";
    private static final String CLIENT_CERT_HEADER = "SSL_CLIENT_CERT ";
    private static final String CLIENT_CHAIN_HEADER = "CERT_CHAIN ";

    private static String clientCertHeaderValue;
    private static String clientChainHeaderValue;

    private static class UntrustedHttpRequestImpl extends HttpRequestImpl {
        public UntrustedHttpRequestImpl(MockHttpRequest delegate) {
            super(delegate);
        }

        @Override
        public boolean isProxyTrusted() {
            return false;
        }
    }

    @BeforeClass
    public static void init() throws IOException {
        CryptoIntegration.init(Rfc9440ClientCertificateLookupTest.class.getClassLoader());

        URL certHeaderValueResource = Rfc9440ClientCertificateLookupTest.class.getResource(TEST_CLIENT_CERT_FILE);
        assert certHeaderValueResource != null;
        clientCertHeaderValue = IOUtils.toString(certHeaderValueResource, StandardCharsets.UTF_8);

        URL chainHeaderValueResource = Rfc9440ClientCertificateLookupTest.class.getResource(TEST_CLIENT_CHAIN_FILE);
        assert chainHeaderValueResource != null;
        clientChainHeaderValue = IOUtils.toString(chainHeaderValueResource, StandardCharsets.UTF_8);
    }

   @Test
    public void testRequestFromUntrustedProxyIsDiscarded() throws GeneralSecurityException {
        Rfc9440ClientCertificateLookup subject = createSubject(1);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue);
        }, false);

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(nullValue()));
    }

    @Test
    public void testClientCertOnly() throws GeneralSecurityException {
        Rfc9440ClientCertificateLookup subject = createSubject(1);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue);
        });

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(not(nullValue())));
        assertThat(actualChain, is(arrayWithSize(1)));
    }

    @Test
    public void testClientCertAndChain() throws GeneralSecurityException {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue);
            headers.add(CLIENT_CHAIN_HEADER, clientChainHeaderValue);
        });

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(not(nullValue())));
        assertThat(actualChain, is(arrayWithSize(3)));
    }

    @Test
    public void testClientCertAndChainInMultipleIndividualFields() throws GeneralSecurityException {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue);
            List<String> chainCerts = Splitter.on(", ").splitToList(clientChainHeaderValue);
            chainCerts.forEach(cert -> headers.add(CLIENT_CHAIN_HEADER, cert));
        });

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(not(nullValue())));
        assertThat(actualChain, is(arrayWithSize(3)));
    }

    @Test
    public void testClientCertAndChainInMultipleMixedFields() throws GeneralSecurityException {
        Rfc9440ClientCertificateLookup subject = createSubject(3);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue);
            List<String> chainCerts = Splitter.on(", ").splitToList(clientChainHeaderValue);
            headers.add(CLIENT_CHAIN_HEADER, chainCerts.get(0));
            headers.add(CLIENT_CHAIN_HEADER, chainCerts.get(0) + "," + chainCerts.get(1));
        });

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(not(nullValue())));
        assertThat(actualChain, is(arrayWithSize(4)));
    }

    @Test
    public void testEmptyChainOnMissingClientCert() throws GeneralSecurityException {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {});

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(not(nullValue())));
        assertThat(actualChain, is(emptyArray()));
    }

    @Test
    public void testErrorOnEmptyClientCertByteArray() {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, "::");
        });

        assertThrows(GeneralSecurityException.class, () -> {
            subject.getCertificateChain(httpRequest);
        });
    }

    @Test
    public void testErrorOnIncorrectlyStartedClientCert() {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue.substring(1));
        });

        assertThrows(GeneralSecurityException.class, () -> {
            subject.getCertificateChain(httpRequest);
        });
    }

    @Test
    public void testErrorOnIncorrectlyTerminatedClientCert() {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue.substring(0, clientCertHeaderValue.length() - 1));
        });

        assertThrows(GeneralSecurityException.class, () -> {
            subject.getCertificateChain(httpRequest);
        });
    }

    @Test
    public void testErrorOnInvalidClientCertBase64Payload() {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, ":Zm9_:");
        });

        assertThrows(GeneralSecurityException.class, () -> {
            subject.getCertificateChain(httpRequest);
        });
    }

    @Test
    public void testErrorOnInvalidClientCertDerPayload() {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, ":Zm9v:");
        });

        assertThrows(GeneralSecurityException.class, () -> {
            subject.getCertificateChain(httpRequest);
        });
    }

    @Test
    public void testErrorOnMultipleClientCerts() {
        Rfc9440ClientCertificateLookup subject = createSubject(2);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue);
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue);
        });

        assertThrows(GeneralSecurityException.class, () -> {
            subject.getCertificateChain(httpRequest);
        });
    }

    @Test
    public void testErrorOnTooLongChain() {
        Rfc9440ClientCertificateLookup subject = createSubject(1);
        HttpRequest httpRequest = createHttpRequest(headers -> {
            headers.add(CLIENT_CERT_HEADER, clientCertHeaderValue);
            headers.add(CLIENT_CHAIN_HEADER, clientChainHeaderValue);
        });

        assertThrows(GeneralSecurityException.class, () -> {
            subject.getCertificateChain(httpRequest);
        });
    }

    private static Rfc9440ClientCertificateLookup createSubject(int certificateChainLength) {
        return new Rfc9440ClientCertificateLookup(CLIENT_CERT_HEADER, CLIENT_CHAIN_HEADER, certificateChainLength);
    }

    private static HttpRequest createHttpRequest(Consumer<MultivaluedMap<String, String>> configurer) {
        return createHttpRequest(configurer, true);
    }

    private static HttpRequest createHttpRequest(Consumer<MultivaluedMap<String, String>> configurer, boolean fromTrustedProxy) {
        MockHttpRequest requestMock;
        try {
            requestMock = MockHttpRequest.get("foo");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        configurer.accept(requestMock.getMutableHeaders());
        if (fromTrustedProxy) {
            return new HttpRequestImpl(requestMock);
        } else {
            return new UntrustedHttpRequestImpl(requestMock);
        }
    }

}
