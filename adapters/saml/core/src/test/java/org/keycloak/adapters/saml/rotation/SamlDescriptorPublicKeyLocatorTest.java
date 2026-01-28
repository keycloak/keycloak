/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adapters.saml.rotation;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.keycloak.common.util.Time;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;

/**
 *
 * @author rmartinc
 */
public class SamlDescriptorPublicKeyLocatorTest {

    private static final String DESCRIPTOR_PREFIX =
            "<EntityDescriptor ID=\"_46a4ff39-ad96-499d-91d9-040588865218\" entityID=\"http://adfs.server.url/adfs/services/trust\" xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">" +
            "<IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\" WantAuthnRequestsSigned=\"true\">" +
            "<KeyDescriptor use=\"signing\">";
    private static final String DESCRIPTOR_SUFFIX =
            "</KeyDescriptor>" +
            "<SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://adfs.server.url/adfs/ls/\"/>" +
            "<SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://adfs.server.url/adfs/ls/\"/>" +
            "<NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</NameIDFormat>" +
            "<NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</NameIDFormat>" +
            "<NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</NameIDFormat>" +
            "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://adfs.server.url/adfs/ls/\"/>" +
            "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://adfs.server.url/adfs/ls/\"/>" +
            "</IDPSSODescriptor>" +
            "<ContactPerson contactType=\"support\"/>" +
            "</EntityDescriptor>";
    private static final String SAMPLE_CERTIFICATE_RSA_1 = "MIICmzCCAYMCBgGGc0gbfzANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjMwMjIxMDkyMDUwWhcNMzMwMjIxMDkyMjMwWjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCfc15pW/NOT3CM92q7BUB3pyTdA1h0WFG+JM2JjrNyZEbxsycYXS84QlaaEl/qT0wshIFQnv6bD1jy604V9W+7luK6Q/cOoQyRCiI70CVy4kB73sqT8Lgrfux6zWJeZ0lMO14sPq6eJLhWNBGxbGvJtUgBAdv5TIjf8yaHCV+yo4rc83T6Pd1sfTlRrURnokPD+hy+BbCEVj9350vYiyTRSvUD+e1wG1BIyZ/IA572p15rS69PP+qAuBBE8QF42bI56ZTsU+tXxwSX2nPqVbLD61tb1BFXfrHkArRiLe/Dte7xAmArynWs62ZI1q52REVWik1dzzy+VpJ7lef7vgtJAgMBAAEwDQYJKoZIhvcNAQELBQADggEBADB5DXugTWEYrw/ic/Jqz+aKXlz+QJvP5JEOVMnfKQLfHx+6760ubCwqJstA8HL6z8DWQUWWylwhfFv15nW/tgawbYLGHiq0NfB3/T6u3hswAPff9ZNvviL0L8CtPXpgPE5MzUEyPRIl/ExW/a7oNlo3rOPE6vA2xEG5h24f9xVdT5hGT5wRTm/e64ZT+umpWs2HnGjRcvdEKZhQPGfKrfdzNn1DVobbGSuy7P64lPWRJ/DxrhMwVkOyfZ+XoIGavS/yLQt01KjIrqtmUZOwHE5FRM/B58doGZn/zNpxq0tb7t9sxWIcW6wyZyieTAO7D9D84Qw8EBwKlbtsfS0oSZw=";
    private static final String SAMPLE_CERTIFICATE_RSA_2 = "MIICmzCCAYMCBgGGc0gb+jANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjMwMjIxMDkyMDUxWhcNMzMwMjIxMDkyMjMxWjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDOgZKCSPgYFaBCLrhaX4jBjgTqdYemJPLyR3gAq3GhO/KVVj5i3lOJYLPE3TdyxowxpvnqJK5zIgLv954y7cbah5wbyfdcFf/qa/RvEDAVb1c3gs+7e5uEoiAWgvARQbcduuO8U/rerlgF3eN0WLjIjcz8yncLmMvd+AhjOAqs3AmKrlEADeABTRq454gXjrD8x3bZwRvC67ZOdK32WpfIG9u58WABDYHWavQ8aetcs1uuwbNl7Tmi0heEtgBd8q2y3BJmn31NXmRobLwNuILEN8sujMKf6iaISA50gh0TCUYSbzeeQ6DrqHBlOA8azpuwka4pQyr+R22MDdrItTc3AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAKuc82PlWzQbevzd/FvbutsEX5Tdf4Nojd+jOvcP6NiDtImWojzgN+SSAKTtmCz3ToBxjJbI4UjhovjWN4e4ygEWksBw6YYYR9ZGCJ7Z3EZzyREojvZeF/H0lQqB3BgnjI38HBpRgCpZm3H6+1UoJtMOW2sU8jorG/k1qvXrx2Y3bZvj/6wixVnzjiFzagb3cIUzv9c7ZWlexaR2Bg0k4kQ5TFwyzYCE136nl8xPqoDd8Nc4fQEPI7wLYMGglmbLFlGvdz3IJ7XRparYJRm4wlznQ43GL2x2KGBu8JipgbA7+u6F84oqf3vOC/PozWXzVCn08e6gqBY3YdZcs6sA3qY=";
    private static HttpServer server;
    private static final Map<String, String> signingCertificates = new HashMap<>();
    private static final X509Certificate cert1;
    private static final X509Certificate cert2;

    static {
        try {
            cert1 = XMLSignatureUtil.getX509CertificateFromKeyInfoString(SAMPLE_CERTIFICATE_RSA_1);
            cert2 = XMLSignatureUtil.getX509CertificateFromKeyInfoString(SAMPLE_CERTIFICATE_RSA_2);
        } catch (ProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeClass
    public static void startHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8280), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    @AfterClass
    public static void stopHttpServer() {
        server.stop(0);
    }

    @After
    public void resetTest() {
        signingCertificates.clear();
        Time.setOffset(0);
    }

    @Test
    public void testKeyName() throws Exception {
        signingCertificates.put("cert1", SAMPLE_CERTIFICATE_RSA_1);
        KeyLocator locator = new SamlDescriptorPublicKeyLocator("http://localhost:8280", 10, 30, HttpClients.createDefault());
        Assert.assertEquals(cert1.getPublicKey(), locator.getKey("cert1"));
        Assert.assertNull(locator.getKey("cert2"));

        signingCertificates.put("cert2", SAMPLE_CERTIFICATE_RSA_2);
        Assert.assertNull(locator.getKey("cert2")); // not allowed to refresh
        Time.setOffset(11);
        signingCertificates.put("cert2", SAMPLE_CERTIFICATE_RSA_2);
        Assert.assertEquals(cert2.getPublicKey(), locator.getKey("cert2"));
    }

    @Test
    public void testCertificateKey() throws Exception {
        signingCertificates.put("cert1", SAMPLE_CERTIFICATE_RSA_1);
        KeyLocator locator = new SamlDescriptorPublicKeyLocator("http://localhost:8280", 10, 30, HttpClients.createDefault());
        Assert.assertEquals(cert1.getPublicKey(), locator.getKey(cert1.getPublicKey()));
        Assert.assertNull(locator.getKey("cert2"));

        signingCertificates.put("cert2", SAMPLE_CERTIFICATE_RSA_2);
        Assert.assertNull(locator.getKey(cert2.getPublicKey())); // not allowed to refresh
        Time.setOffset(11);
        signingCertificates.put("cert2", SAMPLE_CERTIFICATE_RSA_2);
        Assert.assertEquals(cert2.getPublicKey(), locator.getKey(cert2.getPublicKey()));
    }

    @Test
    public void testIteration() throws Exception {
        signingCertificates.put("cert1", SAMPLE_CERTIFICATE_RSA_1);
        KeyLocator locator = new SamlDescriptorPublicKeyLocator("http://localhost:8280", 10, 30, HttpClients.createDefault());
        Set<Key> keys = StreamSupport.stream(locator.spliterator(), false).collect(Collectors.toSet());
        Assert.assertTrue(keys.contains(cert1.getPublicKey()));

        signingCertificates.put("cert2", SAMPLE_CERTIFICATE_RSA_2);
        // not refreshed
        keys = StreamSupport.stream(locator.spliterator(), false).collect(Collectors.toSet());
        Assert.assertFalse(keys.contains(cert2.getPublicKey()));
        Time.setOffset(11);
        // still not refreshed, iterator waits for ttl
        keys = StreamSupport.stream(locator.spliterator(), false).collect(Collectors.toSet());
        Assert.assertFalse(keys.contains(cert2.getPublicKey()));
        Time.setOffset(31);
        // now should be refreshed
        keys = StreamSupport.stream(locator.spliterator(), false).collect(Collectors.toSet());
        Assert.assertTrue(keys.contains(cert2.getPublicKey()));
    }

    private static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                StringBuilder sb = new StringBuilder(DESCRIPTOR_PREFIX);
                for (Map.Entry<String, String> entry : signingCertificates.entrySet()) {
                    StringWriter sw = new StringWriter();
                    XMLStreamWriter writer = StaxUtil.getXMLStreamWriter(sw);
                    Element e = SPMetadataDescriptor.buildKeyInfoElement(entry.getKey(), entry.getValue());
                    StaxUtil.writeDOMElement(writer, e);
                    writer.close();
                    sb.append(sw.toString());
                }
                sb.append(DESCRIPTOR_SUFFIX);
                byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                t.getResponseHeaders().add("Content-Type", "application/xml;charset=UTF-8");
                t.sendResponseHeaders(200, bytes.length);
                try ( OutputStream os = t.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (ParserConfigurationException | XMLStreamException | ProcessingException e) {
                throw new IOException(e);
            }
        }
    }
}
