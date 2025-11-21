/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.installation.SamlSPDescriptorClientInstallation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.KeyUtils;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.METADATA_NSURI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Test getting the installation/configuration files for OIDC and SAML.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest(config = InstallationTest.InstallationTestServerConfig.class)
public class InstallationTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectClient(config = OidcClientConfig.class, ref = "oidc")
    ManagedClient oidcClient;

    @InjectClient(config = OidcBearerOnlyClientConfig.class, ref = "oidcBearerOnly")
    ManagedClient oidcBearerOnlyClient;

    @InjectClient(config = OidcBearerOnlyWithAuthzClientConfig.class, ref = "oidcBearerOnlyWithAuthz")
    ManagedClient oidcBearerOnlyWithAuthzClient;

    @InjectClient(config = SamlClientConfig.class, ref = "saml")
    ManagedClient samlClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectAdminEvents
    AdminEvents adminEvents;

    private static final String OIDC_NAME = "oidcInstallationClient";
    private static final String OIDC_NAME_BEARER_ONLY_NAME = "oidcInstallationClientBearerOnly";
    private static final String OIDC_NAME_BEARER_ONLY_WITH_AUTHZ_NAME = "oidcInstallationClientBearerOnlyWithAuthz";
    private static final String SAML_NAME = "samlInstallationClient";

    private String samlUrl() {
        return realm.getBaseUrl() + "/protocol/saml";
    }

    @Test
    public void testOidcJBossXml() {
        String xml = oidcClient.admin().getInstallationProvider("keycloak-oidc-jboss-subsystem");
        assertOidcInstallationConfig(xml);
        assertThat(xml, containsString("<secure-deployment"));
    }

    @Test
    public void testOidcJson() {
        String json = oidcClient.admin().getInstallationProvider("keycloak-oidc-keycloak-json");
        assertOidcInstallationConfig(json);
    }

    @Test
    public void testOidcJBossCli() {
        String cli = oidcClient.admin().getInstallationProvider("keycloak-oidc-jboss-subsystem-cli");
        assertOidcInstallationConfig(cli);
        assertThat(cli, containsString("/subsystem=keycloak/secure-deployment=\"WAR MODULE NAME.war\""));
    }

    @Test
    public void testOidcBearerOnlyJson() {
        String json = oidcBearerOnlyClient.admin().getInstallationProvider("keycloak-oidc-keycloak-json");
        assertOidcInstallationConfig(json);
        assertThat(json, containsString("bearer-only"));
        assertThat(json, not(containsString("public-client")));
        assertThat(json, not(containsString("credentials")));
        assertThat(json, not(containsString("verify-token-audience")));
    }

    @Test
    public void testOidcBearerOnlyJsonWithAudienceClientScope() {
        // Generate audience client scope
        String clientScopeId = createAudienceClientScope();

        String json = oidcBearerOnlyClient.admin().getInstallationProvider("keycloak-oidc-keycloak-json");
        assertOidcInstallationConfig(json);
        assertThat(json, containsString("bearer-only"));
        assertThat(json, not(containsString("public-client")));
        assertThat(json, not(containsString("credentials")));
        assertThat(json, containsString("verify-token-audience"));

        // Remove clientScope
        realm.admin().clientScopes().get(clientScopeId).remove();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientScopeResourcePath(clientScopeId), null, ResourceType.CLIENT_SCOPE);
    }

    private String createAudienceClientScope() {
        ClientScopeRepresentation clientScopeRepresentation = new ClientScopeRepresentation();
        clientScopeRepresentation.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        clientScopeRepresentation.setName(oidcBearerOnlyClient.getClientId());

        ProtocolMapperRepresentation mapper = createProtocolMapper();
        clientScopeRepresentation.setProtocolMappers(List.of(mapper));

        Response response = realm.admin().clientScopes().create(clientScopeRepresentation);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientScopeResourcePath(id), ResourceType.CLIENT_SCOPE);
        return id;
    }

    private ProtocolMapperRepresentation createProtocolMapper() {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("Audience for " + oidcBearerOnlyClient.getClientId());
        mapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Map<String, String> config = new HashMap<>();
        config.put(AudienceProtocolMapper.INCLUDED_CLIENT_AUDIENCE, oidcBearerOnlyClient.getClientId());
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        mapper.setConfig(config);
        return mapper;
    }

    @Test
    public void testOidcBearerOnlyWithAuthzJson() {
        String json = oidcBearerOnlyWithAuthzClient.admin().getInstallationProvider("keycloak-oidc-keycloak-json");
        assertOidcInstallationConfig(json);
        assertThat(json, not(containsString("bearer-only")));
        assertThat(json, not(containsString("public-client")));
        assertThat(json, containsString("credentials"));
        assertThat(json, containsString("secret"));
        assertThat(json, containsString("policy-enforcer"));
    }

    private void assertOidcInstallationConfig(String config) {
        assertThat(config, containsString("default"));
        assertThat(config, not(containsString(KeyUtils.findActiveSigningKey(realm.admin()).getPublicKey())));
        assertThat(config, containsString(keycloakUrls.getBase()));
    }

    @Test
    public void testSamlMetadataIdpDescriptor() {
        try {
            samlClient.admin().getInstallationProvider("saml-idp-descriptor");
            Assertions.fail("Successful saml-idp-descriptor not expected");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    @Test
    public void testSamlAdapterXml() {
        String xml = samlClient.admin().getInstallationProvider("keycloak-saml");
        assertThat(xml, containsString("<keycloak-saml-adapter>"));
        assertThat(xml, containsString("SPECIFY YOUR entityID!"));
        assertThat(xml, not(containsString(KeyUtils.findActiveSigningKey(realm.admin()).getCertificate())));
        assertThat(xml, containsString(samlUrl()));
    }

    @Test
    public void testSamlAdapterCli() {
        String cli = samlClient.admin().getInstallationProvider("keycloak-saml-subsystem-cli");
        assertThat(cli, containsString("/subsystem=keycloak-saml/secure-deployment=YOUR-WAR.war/"));
        assertThat(cli, containsString("SPECIFY YOUR entityID!"));
        assertThat(cli, not(containsString(KeyUtils.findActiveSigningKey(realm.admin()).getCertificate())));
        assertThat(cli, containsString(samlUrl()));
    }

    @Test
    public void testSamlMetadataSpDescriptor() throws Exception {
        String xml = samlClient.admin().getInstallationProvider(SamlSPDescriptorClientInstallation.SAML_CLIENT_INSTALATION_SP_DESCRIPTOR);
        Document doc = getDocumentFromXmlString(xml);
        assertElements(doc, METADATA_NSURI.get(), "EntityDescriptor", null);
        assertElements(doc, METADATA_NSURI.get(), "SPSSODescriptor", null);
        assertThat(xml, containsString(SAML_NAME));
    }

    @Test
    public void testSamlJBossXml() {
        String xml = samlClient.admin().getInstallationProvider("keycloak-saml-subsystem");
        assertThat(xml, containsString("<secure-deployment"));
        assertThat(xml, containsString("SPECIFY YOUR entityID!"));
        assertThat(xml, not(containsString(KeyUtils.findActiveSigningKey(realm.admin()).getCertificate())));
        assertThat(xml, containsString(samlUrl()));
    }

    @Test
    public void testSamlMetadataSpDescriptorPost() throws Exception {
        assertThat(samlClient.admin().toRepresentation().getAttributes().get(SamlConfigAttributes.SAML_FORCE_POST_BINDING), equalTo("true"));

        //error fallback
        String response = samlClient.admin().getInstallationProvider(SamlSPDescriptorClientInstallation.SAML_CLIENT_INSTALATION_SP_DESCRIPTOR);
        Document doc = getDocumentFromXmlString(response);
        Map<String, String> attrNamesAndValues = new HashMap<>();
        attrNamesAndValues.put("Binding", JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
        attrNamesAndValues.put("Location", "ERROR:ENDPOINT_NOT_SET");
        assertElements(doc, METADATA_NSURI.get(), "SingleLogoutService", attrNamesAndValues);
        assertElements(doc, METADATA_NSURI.get(), "AssertionConsumerService", attrNamesAndValues);
        attrNamesAndValues.clear();

        //fallback to adminUrl
        samlClient.updateWithCleanup(c -> c.adminUrl("https://admin-url"));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(samlClient.getId()), ResourceType.CLIENT);
        response = samlClient.admin().getInstallationProvider(SamlSPDescriptorClientInstallation.SAML_CLIENT_INSTALATION_SP_DESCRIPTOR);
        doc = getDocumentFromXmlString(response);
        attrNamesAndValues.put("Binding", JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
        attrNamesAndValues.put("Location", "https://admin-url");
        assertElements(doc, METADATA_NSURI.get(), "SingleLogoutService", attrNamesAndValues);
        assertElements(doc, METADATA_NSURI.get(), "AssertionConsumerService", attrNamesAndValues);
        attrNamesAndValues.clear();

        //fine grained
        samlClient.updateWithCleanup(c -> c
                .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "https://saml-assertion-post-url")
                .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "https://saml-logout-post-url")
                .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, "https://saml-assertion-redirect-url")
                .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "https://saml-logout-redirect-url")
        );
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(samlClient.getId()), ResourceType.CLIENT);

        response = samlClient.admin().getInstallationProvider(SamlSPDescriptorClientInstallation.SAML_CLIENT_INSTALATION_SP_DESCRIPTOR);
        doc = getDocumentFromXmlString(response);
        attrNamesAndValues.put("Binding", JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
        attrNamesAndValues.put("Location", "https://saml-logout-post-url");
        assertElements(doc, METADATA_NSURI.get(), "SingleLogoutService", attrNamesAndValues);
        attrNamesAndValues.clear();
        attrNamesAndValues.put("Binding", JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
        attrNamesAndValues.put("Location", "https://saml-assertion-post-url");
        assertElements(doc, METADATA_NSURI.get(), "AssertionConsumerService", attrNamesAndValues);
    }

    @Test
    public void testSamlMetadataSpDescriptorRedirect() throws Exception {
        samlClient.updateWithCleanup(c -> c.attribute(SamlConfigAttributes.SAML_FORCE_POST_BINDING, "false"));

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(samlClient.getId()), ResourceType.CLIENT);
        assertThat(samlClient.admin().toRepresentation().getAttributes().get(SamlConfigAttributes.SAML_FORCE_POST_BINDING), equalTo("false"));

        //error fallback
        String response = samlClient.admin().getInstallationProvider(SamlSPDescriptorClientInstallation.SAML_CLIENT_INSTALATION_SP_DESCRIPTOR);
        Document doc = getDocumentFromXmlString(response);
        Map<String, String> attrNamesAndValues = new HashMap<>();
        attrNamesAndValues.put("Binding", JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get());
        attrNamesAndValues.put("Location", "ERROR:ENDPOINT_NOT_SET");
        assertElements(doc, METADATA_NSURI.get(), "SingleLogoutService", attrNamesAndValues);
        assertElements(doc, METADATA_NSURI.get(), "AssertionConsumerService", attrNamesAndValues);
        attrNamesAndValues.clear();

        //fallback to adminUrl
        samlClient.updateWithCleanup(c -> c.adminUrl("https://admin-url"));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(samlClient.getId()), ResourceType.CLIENT);
        response = samlClient.admin().getInstallationProvider(SamlSPDescriptorClientInstallation.SAML_CLIENT_INSTALATION_SP_DESCRIPTOR);
        doc = getDocumentFromXmlString(response);
        attrNamesAndValues.put("Binding", JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get());
        attrNamesAndValues.put("Location", "https://admin-url");
        assertElements(doc, METADATA_NSURI.get(), "SingleLogoutService", attrNamesAndValues);
        assertElements(doc, METADATA_NSURI.get(), "AssertionConsumerService", attrNamesAndValues);
        attrNamesAndValues.clear();

        //fine-grained
        samlClient.updateWithCleanup(c -> c
                .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "https://saml-assertion-post-url")
                .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "https://saml-logout-post-url")
                .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, "https://saml-assertion-redirect-url")
                .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "https://saml-logout-redirect-url")
        );
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(samlClient.getId()), ResourceType.CLIENT);
        response = samlClient.admin().getInstallationProvider(SamlSPDescriptorClientInstallation.SAML_CLIENT_INSTALATION_SP_DESCRIPTOR);
        doc = getDocumentFromXmlString(response);
        attrNamesAndValues.put("Binding", JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get());
        attrNamesAndValues.put("Location", "https://saml-logout-redirect-url");
        assertElements(doc, METADATA_NSURI.get(), "SingleLogoutService", attrNamesAndValues);
        attrNamesAndValues.clear();
        attrNamesAndValues.put("Binding", JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get());
        attrNamesAndValues.put("Location", "https://saml-assertion-redirect-url");
        assertElements(doc, METADATA_NSURI.get(), "AssertionConsumerService", attrNamesAndValues);
    }

    @Test
    public void testPemsInModAuthMellonExportShouldBeFormattedInRfc7468() throws IOException {
        Response response = samlClient.admin().getInstallationProviderAsResponse("mod-auth-mellon");
        byte[] result = response.readEntity(byte[].class);

        String clientPrivateKey = null;
        String clientCert = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(result))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith("client-private-key.pem")) {
                    clientPrivateKey = new String(zis.readAllBytes(), StandardCharsets.US_ASCII);
                } else if (entry.getName().endsWith("client-cert.pem")) {
                    clientCert = new String(zis.readAllBytes(), StandardCharsets.US_ASCII);
                }
            }
        }

        Assertions.assertNotNull(clientPrivateKey);
        Assertions.assertNotNull(clientCert);
        assertRfc7468PrivateKey(clientPrivateKey);
        assertRfc7468Cert(clientCert);
    }

    private void assertRfc7468PrivateKey(String result) {
        Assertions.assertTrue(result.startsWith("-----BEGIN PRIVATE KEY-----"));
        Assertions.assertTrue(result.endsWith("-----END PRIVATE KEY-----"));
        result.lines().forEach(line -> Assertions.assertTrue(line.length() <= 64));
    }

    private void assertRfc7468Cert(String result) {
        Assertions.assertTrue(result.startsWith("-----BEGIN CERTIFICATE-----"));
        Assertions.assertTrue(result.endsWith("-----END CERTIFICATE-----"));
        result.lines().forEach(line -> Assertions.assertTrue(line.length() <= 64));
    }

    private Document getDocumentFromXmlString(String xml) throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        return db.parse(is);
    }

    private void assertElements(Document doc, String tagNamespace, String tagName, Map<String, String> attrNamesAndValues) {
        NodeList elementsByTagName = doc.getElementsByTagNameNS(tagNamespace, tagName);
        assertThat("Expected exactly one " + tagName + " element!", elementsByTagName.getLength(), is(equalTo(1)));
        Node element = elementsByTagName.item(0);

        if (attrNamesAndValues != null) {
            for (var entry : attrNamesAndValues.entrySet()) {
                assertThat(element.getAttributes().getNamedItem(entry.getKey()).getNodeValue(), containsString(entry.getValue()));
            }
        }
    }

    public static class InstallationTestServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(AUTHORIZATION);
        }
    }

    public static class OidcClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId(OIDC_NAME)
                    .name(OIDC_NAME)
                    .protocol("openid-connect");
        }
    }

    public static class OidcBearerOnlyClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId(OIDC_NAME_BEARER_ONLY_NAME)
                    .name(OIDC_NAME_BEARER_ONLY_NAME)
                    .protocol("openid-connect")
                    .bearerOnly(true)
                    .publicClient(false);
        }
    }

    public static class OidcBearerOnlyWithAuthzClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId(OIDC_NAME_BEARER_ONLY_WITH_AUTHZ_NAME)
                    .name(OIDC_NAME_BEARER_ONLY_WITH_AUTHZ_NAME)
                    .protocol("openid-connect")
                    .bearerOnly(false)
                    .publicClient(false)
                    .authorizationServicesEnabled(true)
                    .serviceAccountsEnabled(true);
        }
    }

    public static class SamlClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId(SAML_NAME)
                    .name(SAML_NAME)
                    .protocol("saml");
        }
    }
}
