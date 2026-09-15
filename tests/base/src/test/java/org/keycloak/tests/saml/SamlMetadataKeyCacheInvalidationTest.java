/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.saml;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.tests.suites.DatabaseTest;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that a client update invalidates metadata signing keys in Keycloak's public-key cache
 * by submitting signed AuthnRequests through the SAML endpoint (issue #52106).
 */
@KeycloakIntegrationTest
@DatabaseTest
public class SamlMetadataKeyCacheInvalidationTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = SamlUserConfig.class)
    ManagedUser user;

    @InjectEvents
    Events events;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectHttpServer
    HttpServer httpServer;

    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates certificates;

    private static final String CLIENT_ID = "saml-metadata-key-client";
    private static final String ACS_PATH = "/saml/acs";
    private static final String LOGIN_PATH = "/saml-sp-login";

    private final AtomicReference<String> loginRequest = new AtomicReference<>();

    @Test
    public void metadataKeyCacheInvalidatedOnClientUpdate() throws Exception {
        CryptoIntegration.init(getClass().getClassLoader());
        KeyPair keyA = KeyUtils.generateRsaKeyPair(2048);
        KeyPair keyB = KeyUtils.generateRsaKeyPair(2048);
        String metadataA = spMetadata("key-a", keyA);
        String metadataB = spMetadata("key-b", keyB);
        AtomicReference<String> metadata = new AtomicReference<>(metadataA);
        AtomicInteger metadataRequests = new AtomicInteger();

        String path = "/saml-sp-metadata";
        String url = "http://" + httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort() + path;
        httpServer.createContext(path, exchange -> {
            metadataRequests.incrementAndGet();
            HttpServerUtil.sendResponse(exchange, 200,
                    Map.of("Content-Type", List.of("application/xml")), metadata.get());
        });

        AtomicReference<String> samlResponse = new AtomicReference<>();
        String uuid = null;
        try {
            httpServer.createContext(LOGIN_PATH, exchange -> HttpServerUtil.sendResponse(exchange, 200,
                    Map.of("Content-Type", List.of("text/html")), loginRequest.get()));
            httpServer.createContext(ACS_PATH, exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                URLEncodedUtils.parse(body, StandardCharsets.UTF_8).stream()
                        .filter(parameter -> "SAMLResponse".equals(parameter.getName()))
                        .findFirst().ifPresent(parameter -> samlResponse.set(parameter.getValue()));
                HttpServerUtil.sendResponse(exchange, 200, Map.of("Content-Type", List.of("text/html")),
                        "<html><body>SAML response received</body></html>");
            });
            ClientRepresentation rep = ClientBuilder.create(CLIENT_ID)
                .protocol("saml")
                .redirectUris(acsUrl())
                .attributes(Map.of(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.TRUE.toString(),
                    SamlConfigAttributes.SAML_USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString(),
                    SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, url))
                .build();
            try (Response response = realm.admin().clients().create(rep)) {
                uuid = ApiUtil.getCreatedId(response);
            }
            assertNotEquals(CLIENT_ID, uuid, "clientId and internal UUID must differ to reproduce the bug");
            AdminEventAssertion.assertSuccess(adminEvents.poll())
                    .operationType(OperationType.CREATE)
                    .resourceType(ResourceType.CLIENT)
                    .resourcePath("clients", uuid);

            assertLoginPage("key-a", keyA);
            assertEquals(1, metadataRequests.get(), "initial request must load metadata");

            metadata.set(metadataB);
            assertLoginPage("key-a", keyA);
            assertEquals(1, metadataRequests.get(), "key A must still be accepted from the cache before the update");

            ClientRepresentation current = realm.admin().clients().get(uuid).toRepresentation();
            current.setDescription("rotated metadata key");
            realm.admin().clients().get(uuid).update(current);
            AdminEventAssertion.assertSuccess(adminEvents.poll())
                    .operationType(OperationType.UPDATE)
                    .resourceType(ResourceType.CLIENT)
                    .resourcePath("clients", uuid)
                    .representation(Map.of("description", "rotated metadata key"));

            // Check the retired key first: requesting unknown key B could itself trigger a reload.
            sendAuthnRequest("key-a", keyA);
            errorPage.assertCurrent();
            assertEquals("Invalid requester", errorPage.getError());
            EventAssertion.assertError(events.poll())
                    .type(EventType.LOGIN_ERROR)
                    .error(Errors.INVALID_SIGNATURE);
            assertEquals(2, metadataRequests.get(), "client update must cause metadata to be fetched again");

            assertNull(samlResponse.get(), "a rejected request must not return a SAML response");
            assertTrue(user.admin().getUserSessions().isEmpty(), "the rejected request must not create a user session");

            assertLoginPage("key-b", keyB);
            assertEquals(2, metadataRequests.get(), "replacement key must resolve from the refreshed cache");
            loginPage.fillLogin(user.getUsername(), user.getPassword());
            loginPage.submit();
            driver.waiting().until(webDriver -> samlResponse.get() != null);
            ResponseType response = (ResponseType) SAMLRequestParser.parseResponsePostBinding(samlResponse.get()).getSamlObject();
            assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), response.getStatus().getStatusCode().getValue().toString());
            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .clientId(CLIENT_ID)
                    .userId(user.getId());

            var sessions = user.admin().getUserSessions();
            assertEquals(1, sessions.size(), "successful SAML login must create a user session");
            assertEquals(CLIENT_ID, sessions.get(0).getClients().get(uuid));
        } finally {
            httpServer.removeContext(path);
            httpServer.removeContext(LOGIN_PATH);
            httpServer.removeContext(ACS_PATH);
            if (uuid != null) {
                realm.admin().clients().get(uuid).remove();
            }
        }
    }

    private void assertLoginPage(String kid, KeyPair keyPair) throws Exception {
        sendAuthnRequest(kid, keyPair);
        loginPage.assertCurrent();
    }

    private void sendAuthnRequest(String kid, KeyPair keyPair) throws Exception {
        URI endpoint = URI.create(keycloakUrls.getBase() + "/realms/" + realm.getName() + "/protocol/saml");
        Document request = SAML2Request.convert(SamlClient.createLoginRequestDocument(CLIENT_ID, acsUrl(), endpoint));
        loginRequest.set(new BaseSAML2BindingBuilder<>()
                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                .signWith(kid, keyPair)
                .signDocument()
                .postBinding(request)
                .getHtmlRequest(endpoint.toString()));
        // The browser submits the signed SAML form and retains the authentication cookies across redirects.
        driver.open("http://" + httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort() + LOGIN_PATH);
    }

    private String spMetadata(String kid, KeyPair keyPair) throws Exception {
        String certificate = PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(keyPair, CLIENT_ID));
        // No validUntil/cacheDuration: the test must rely on client-update invalidation, not metadata expiry.
        return "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" entityID=\"" + CLIENT_ID + "\">"
                + "<md:SPSSODescriptor AuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<md:KeyDescriptor use=\"signing\"><ds:KeyInfo><ds:KeyName>" + kid + "</ds:KeyName>"
                + "<ds:X509Data><ds:X509Certificate>" + certificate + "</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>"
                + "<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"" + acsUrl() + "\" index=\"0\"/>"
                + "</md:SPSSODescriptor></md:EntityDescriptor>";
    }

    private String acsUrl() {
        return "http://" + httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort() + ACS_PATH;
    }

    public static class SamlUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("saml-user").password("password").firstName("SAML").lastName("User").email("saml-user@example.org");
        }
    }

    public static class TlsEnabledConfig implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }
}
