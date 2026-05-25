package org.keycloak.services.x509;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.security.auth.x500.X500Principal;

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

    private static final String TEST_CLIENT_CERT_FILE = "/org/keycloak/test/services/x509/header_value_haproxy_client_cert";
    private static final String TEST_CLIENT_CHAIN_FILE = "/org/keycloak/test/services/x509/header_value_haproxy_client_chain";
    private static final String CLIENT_CERT_HEADER = "Client-Cert";
    private static final String CLIENT_CHAIN_HEADER = "Client-Cert-Chain";

    private static final X500Principal CLIENT_CERT_DN = new X500Principal("CN=test-client");
    private static final X500Principal CHAIN_CERT_1_DN = new X500Principal("CN=Client Intermediate CA 2");
    private static final X500Principal CHAIN_CERT_2_DN = new X500Principal("CN=Client Intermediate CA 1");

    private static String clientCertBase64;
    private static String chainCert1Base64;
    private static String chainCert2Base64;
    private static String concatenatedChainBase64;

    @BeforeClass
    public static void init() throws Exception {
        CryptoIntegration.init(HaProxySslClientCertificateLookupTest.class.getClassLoader());

        URL certResource = HaProxySslClientCertificateLookupTest.class.getResource(TEST_CLIENT_CERT_FILE);
        assertNotNull(certResource);
        clientCertBase64 = IOUtils.toString(certResource, StandardCharsets.UTF_8).trim();

        URL chainResource = HaProxySslClientCertificateLookupTest.class.getResource(TEST_CLIENT_CHAIN_FILE);
        assertNotNull(chainResource);
        concatenatedChainBase64 = IOUtils.toString(chainResource, StandardCharsets.UTF_8).trim();

        byte[] chainDerBytes = Base64.getMimeDecoder().decode(concatenatedChainBase64);
        CertificateFactory cf = CryptoIntegration.getProvider().getX509CertFactory();
        Collection<? extends Certificate> certs = cf.generateCertificates(new ByteArrayInputStream(chainDerBytes));
        List<Certificate> certList = new ArrayList<>(certs);
        chainCert1Base64 = Base64.getEncoder().encodeToString(certList.get(0).getEncoded());
        chainCert2Base64 = Base64.getEncoder().encodeToString(certList.get(1).getEncoded());
    }

    @Test
    public void testClientCertOnly() throws Exception {
        X509Certificate[] chain = lookupWithSingleChainHeader(1)
              .getCertificateChain(
                    createHttpRequest(headers -> headers.add(CLIENT_CERT_HEADER, clientCertBase64))
              );

        assertThat(chain, is(not(nullValue())));
        assertThat(chain, is(arrayWithSize(1)));
        assertThat(chain[0].getSubjectX500Principal(), is(CLIENT_CERT_DN));
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
        assertThat(chain[0].getSubjectX500Principal(), is(CLIENT_CERT_DN));
        assertThat(chain[1].getSubjectX500Principal(), is(CHAIN_CERT_1_DN));
        assertThat(chain[2].getSubjectX500Principal(), is(CHAIN_CERT_2_DN));
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
        assertThat(chain[0].getSubjectX500Principal(), is(CLIENT_CERT_DN));
        assertThat(chain[1].getSubjectX500Principal(), is(CHAIN_CERT_1_DN));
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
        assertThat(chain[0].getSubjectX500Principal(), is(CLIENT_CERT_DN));
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
        assertThat(chain[0].getSubjectX500Principal(), is(CLIENT_CERT_DN));
        assertThat(chain[1].getSubjectX500Principal(), is(CHAIN_CERT_1_DN));
        assertThat(chain[2].getSubjectX500Principal(), is(CHAIN_CERT_2_DN));
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
        assertThat(chain[0].getSubjectX500Principal(), is(CLIENT_CERT_DN));
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
