package org.keycloak.services.x509;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.function.Consumer;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.http.HttpRequest;
import org.keycloak.services.resteasy.HttpRequestImpl;

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
import static org.junit.Assert.assertNotNull;

public class HaProxySslClientCertificateLookupTest {

    private static final String TEST_CLIENT_CERT_FILE = "/org/keycloak/test/services/x509/header_value_rfc_9440_client_cert";
    private static final String TEST_CLIENT_CHAIN_FILE = "/org/keycloak/test/services/x509/header_value_rfc_9440_client_chain";
    private static final String CLIENT_CERT_HEADER = "Client-Cert";
    private static final String CLIENT_CHAIN_HEADER = "Client-Cert-Chain";

    private static String clientCertBase64;
    private static String chainCert1Base64;
    private static String chainCert2Base64;
    private static String concatenatedChainBase64;

    @BeforeClass
    public static void init() throws IOException {
        CryptoIntegration.init(HaProxySslClientCertificateLookupTest.class.getClassLoader());

        URL certResource = HaProxySslClientCertificateLookupTest.class.getResource(TEST_CLIENT_CERT_FILE);
        assertNotNull(certResource);

        String rfc9440CertValue = IOUtils.toString(certResource, StandardCharsets.UTF_8);
        clientCertBase64 = stripByteSequenceDelimiters(rfc9440CertValue);

        URL chainResource = HaProxySslClientCertificateLookupTest.class.getResource(TEST_CLIENT_CHAIN_FILE);
        assertNotNull(chainResource);

        String rfc9440ChainValue = IOUtils.toString(chainResource, StandardCharsets.UTF_8);
        String[] chainEntries = rfc9440ChainValue.split(",\\s*");
        chainCert1Base64 = stripByteSequenceDelimiters(chainEntries[0]);
        chainCert2Base64 = stripByteSequenceDelimiters(chainEntries[1]);

        byte[] derCert1 = Base64.getMimeDecoder().decode(chainCert1Base64);
        byte[] derCert2 = Base64.getMimeDecoder().decode(chainCert2Base64);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(derCert1);
        baos.write(derCert2);
        concatenatedChainBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static String stripByteSequenceDelimiters(String byteSequence) {
        String trimmed = byteSequence.trim();
        return trimmed.substring(1, trimmed.length() - 1);
    }

    @Test
    public void testClientCertOnly() throws Exception {
        X509Certificate[] chain = lookupWithSingleChainHeader(1)
              .getCertificateChain(
                    createHttpRequest(headers -> headers.add(CLIENT_CERT_HEADER, clientCertBase64))
              );

        assertThat(chain, is(not(nullValue())));
        assertThat(chain, is(arrayWithSize(1)));
    }

    @Test
    public void testEmptyChainOnMissingClientCert() throws Exception {
        X509Certificate[] chain = lookupWithSingleChainHeader(1)
              .getCertificateChain(
                    createHttpRequest(headers -> {
                    })
              );
        assertThat(chain, is(not(nullValue())));
        assertThat(chain, is(emptyArray()));
    }

    @Test
    public void testSingleChainHeaderWithFullChain() throws Exception {
        X509Certificate[] chain = lookupWithSingleChainHeader(2)
              .getCertificateChain(
                    createHttpRequest(headers -> {
                              headers.add(CLIENT_CERT_HEADER, clientCertBase64);
                              headers.add(CLIENT_CHAIN_HEADER, concatenatedChainBase64);
                          }
                    )
              );
        assertThat(chain, is(not(nullValue())));
        assertThat(chain, is(arrayWithSize(3)));
    }

    @Test
    public void testSingleChainHeaderTruncatesToChainLength() throws Exception {
        X509Certificate[] chain = lookupWithSingleChainHeader(1)
              .getCertificateChain(
                    createHttpRequest(headers -> {
                              headers.add(CLIENT_CERT_HEADER, clientCertBase64);
                              headers.add(CLIENT_CHAIN_HEADER, concatenatedChainBase64);
                          }
                    )
              );
        assertThat(chain, is(not(nullValue())));
        assertThat(chain, is(arrayWithSize(2)));
    }

    @Test
    public void testSingleChainHeaderWithZeroChainLength() throws Exception {
        X509Certificate[] chain = lookupWithSingleChainHeader(0)
              .getCertificateChain(
                    createHttpRequest(headers -> {
                              headers.add(CLIENT_CERT_HEADER, clientCertBase64);
                              headers.add(CLIENT_CHAIN_HEADER, concatenatedChainBase64);
                          }
                    )
              );
        assertThat(chain, is(not(nullValue())));
        assertThat(chain, is(arrayWithSize(1)));
    }

    @Test
    public void testIndexedChainHeadersWithFullChain() throws Exception {
        X509Certificate[] chain = lookupWithIndexedChainHeaders(2)
              .getCertificateChain(
                    createHttpRequest(headers -> {
                              headers.add(CLIENT_CERT_HEADER, clientCertBase64);
                              headers.add(CLIENT_CHAIN_HEADER + "_0", chainCert1Base64);
                              headers.add(CLIENT_CHAIN_HEADER + "_1", chainCert2Base64);
                          }
                    )
              );
        assertThat(chain, is(not(nullValue())));
        assertThat(chain, is(arrayWithSize(3)));
    }

    @Test
    public void testIndexedChainHeadersTruncatesToChainLength() throws Exception {
        X509Certificate[] chain = lookupWithIndexedChainHeaders(0)
              .getCertificateChain(
                    createHttpRequest(headers -> {
                              headers.add(CLIENT_CERT_HEADER, clientCertBase64);
                              headers.add(CLIENT_CHAIN_HEADER + "_0", chainCert1Base64);
                              headers.add(CLIENT_CHAIN_HEADER + "_1", chainCert2Base64);
                          }
                    )
              );
        assertThat(chain, is(not(nullValue())));
        assertThat(chain, is(arrayWithSize(1)));
    }

    private static HaProxySslClientCertificateLookup lookupWithSingleChainHeader(int certificateChainLength) {
        return new HaProxySslClientCertificateLookup(CLIENT_CERT_HEADER, null, CLIENT_CHAIN_HEADER, certificateChainLength);
    }

    private static HaProxySslClientCertificateLookup lookupWithIndexedChainHeaders(int certificateChainLength) {
        return new HaProxySslClientCertificateLookup(CLIENT_CERT_HEADER, CLIENT_CHAIN_HEADER, null, certificateChainLength);
    }

    private static HttpRequest createHttpRequest(Consumer<MultivaluedMap<String, String>> configurer) throws URISyntaxException {
        MockHttpRequest requestMock = MockHttpRequest.get("foo");
        configurer.accept(requestMock.getMutableHeaders());
        return new HttpRequestImpl(requestMock);
    }
}
