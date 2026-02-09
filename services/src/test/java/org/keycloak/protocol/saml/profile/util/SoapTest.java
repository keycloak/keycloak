/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml.profile.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;
import org.keycloak.utils.ScopeUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.w3c.dom.Document;

/**
 * <p>Test class for Soap utility class.</p>
 *
 * @author rmartinc
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SoapTest {

    private static HttpServer server;

    private static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // just return the same data received, headers inclusive
            if ("POST".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().putAll(exchange.getRequestHeaders());
                try ( InputStream is = exchange.getRequestBody();
                        OutputStream os = exchange.getResponseBody()) {
                    exchange.sendResponseHeaders(200, Long.parseLong(exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_LENGTH)));
                    IOUtils.copy(is, os);
                }
            }
            exchange.sendResponseHeaders(400, 0);
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

    private LogoutRequestType createLogoutRequestType() throws ConfigurationException {
        NameIDType nameId = new NameIDType();
        nameId.setFormat(URI.create(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get()));
        nameId.setValue("user1");
        return new SAML2LogoutRequestBuilder().assertionExpiration(60).issuer("http://sample.com")
                .nameId(nameId).destination("http://sample.com/logout")
                .sessionIndex("idx")
                .createLogoutRequest();
    }

    @Test
    public void test1ResponseOK() throws Exception {
        LogoutRequestType request = createLogoutRequestType();
        Document doc = SAML2Request.convert(request);
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        KeycloakSession session = new ResteasyKeycloakSession(sessionFactory);

        SOAPMessage soapResponse = Soap.createMessage()
                .addMimeHeader("SOAPAction", "http://www.oasis-open.org/committees/security")
                .addMimeHeader("custom-header", "custom-value")
                .addToBody(doc)
                .call("http://localhost:8280", session);
        // check the headers are set back
        Assert.assertArrayEquals(new String[]{"no-cache, no-store"}, soapResponse.getMimeHeaders().getHeader(HttpHeaders.CACHE_CONTROL));
        Assert.assertArrayEquals(new String[]{"http://www.oasis-open.org/committees/security"}, soapResponse.getMimeHeaders().getHeader("SOAPAction"));
        Assert.assertArrayEquals(new String[]{"custom-value"}, soapResponse.getMimeHeaders().getHeader("custom-header"));
        // check response is the LogoutResponseType sent
        Document responseDoc = Soap.extractSoapMessage(soapResponse);
        SAMLDocumentHolder samlDocResponse = SAML2Request.getSAML2ObjectFromDocument(responseDoc);
        SAML2Object samlObject = samlDocResponse.getSamlObject();
        MatcherAssert.assertThat(samlObject, CoreMatchers.instanceOf(LogoutRequestType.class));
        LogoutRequestType response = (LogoutRequestType) samlObject;
        Assert.assertEquals(request.getNameID().getValue(), response.getNameID().getValue());
    }

    @Test
    public void test2ConfigurationUsed() throws Exception {
        LogoutRequestType request = createLogoutRequestType();
        Document doc = SAML2Request.convert(request);
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        Config.init(new Config.ConfigProvider() {
            @Override
            public String getProvider(String spi) {
                return null;
            }

            @Override
            public String getDefaultProvider(String spi) {
                return null;
            }

            @Override
            public Config.Scope scope(String... scope) {
                if (scope.length == 2 && "connectionsHttpClient".equals(scope[0]) && "default".equals(scope[1])) {
                    return ScopeUtil.createScope(Collections.singletonMap("proxy-mappings", "localhost;http://localhost:8281"));
                }
                return ScopeUtil.createScope(new HashMap<>());
            }
        });
        ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        KeycloakSession session = new ResteasyKeycloakSession(sessionFactory);

        SOAPException ex = Assert.assertThrows(SOAPException.class, () -> {
            Soap.createMessage()
                .addToBody(doc)
                .call("http://localhost:8280", session);
        });
        MatcherAssert.assertThat(ex.getMessage(), CoreMatchers.containsString("localhost:8281"));
        MatcherAssert.assertThat(ex.getMessage(), CoreMatchers.containsString("Connection refused"));
    }
}
