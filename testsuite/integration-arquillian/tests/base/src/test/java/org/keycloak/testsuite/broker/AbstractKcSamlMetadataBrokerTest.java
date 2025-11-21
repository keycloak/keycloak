/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.saml.SamlDocumentStepBuilder;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.keycloak.testsuite.util.Matchers.bodyHC;

/**
 *
 * @author rmartinc
 */
public abstract class AbstractKcSamlMetadataBrokerTest extends AbstractBrokerTest {

    protected Document identityDocument(Document doc) {
        return doc;
    }

    protected Document removeKeyNameFromSignature(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS(JBossSAMLURIConstants.XMLDSIG_NSURI.get(), JBossSAMLConstants.KEY_INFO.get());
        if (nodes != null && nodes.getLength() > 0) {
            Element keyInfo = (Element) nodes.item(0);
            nodes = keyInfo.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (JBossSAMLURIConstants.XMLDSIG_NSURI.get().equals(node.getNamespaceURI())
                        && "KeyName".equals(node.getLocalName())) {
                    keyInfo.removeChild(node);
                    break;
                }
            }
        }
        return doc;
    }

    protected void doSamlPostLogin() throws ProcessingException, ConfigurationException, ParsingException {
        doSamlPostLogin(Response.Status.OK.getStatusCode(), "Update Account Information", this::identityDocument);
    }

    protected void doSamlPostLogin(int statusCode, String expectedString, SamlDocumentStepBuilder.Saml2DocumentTransformer transformer)
            throws ProcessingException, ConfigurationException, ParsingException {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                BrokerTestTools.getConsumerRoot() + "/sales-post/saml", null);
        Document doc = SAML2Request.convert(loginRep);
        new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST)
                .build() // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(SamlClient.Binding.POST).build() // AuthnRequest to producer IdP
                .login().user(bc.getUserLogin(), bc.getUserPassword()).build()
                .processSamlResponse(SamlClient.Binding.POST) // Response from producer IdP
                .transformDocument(transformer)
                .build()
                // first-broker flow: if valid request, it displays an update profile page on consumer realm
                .execute(currentResponse -> {
                    Assert.assertEquals(statusCode, currentResponse.getStatusLine().getStatusCode());
                    if (expectedString != null) {
                        MatcherAssert.assertThat(currentResponse, bodyHC(Matchers.containsString(expectedString)));
                    }
                });
    }

    protected void doSamlRedirectLogin() throws ProcessingException, ConfigurationException, ParsingException {
        doSamlRedirectLogin(Response.Status.OK.getStatusCode(), "Update Account Information");
    }

    protected void doSamlRedirectLogin(int statusCode, String expectedString) throws ProcessingException, ConfigurationException, ParsingException {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST,
                BrokerTestTools.getConsumerRoot() + "/sales-post/saml", null);
        Document doc = SAML2Request.convert(loginRep);
        new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.REDIRECT)
                .build() // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(SamlClient.Binding.REDIRECT).build() // AuthnRequest to producer IdP
                .login().user(bc.getUserLogin(), bc.getUserPassword()).build()
                .processSamlResponse(SamlClient.Binding.REDIRECT) // Response from producer IdP
                .build()
                // first-broker flow: if valid request, it displays an update profile page on consumer realm
                .execute(currentResponse -> {
                    Assert.assertEquals(statusCode, currentResponse.getStatusLine().getStatusCode());
                    MatcherAssert.assertThat(currentResponse, bodyHC(Matchers.containsString(expectedString)));
                });
    }

    protected ComponentRepresentation createComponentRep(String algorithm, String providerId, String realmId) {
        ComponentRepresentation keys = new ComponentRepresentation();
        keys.setName("generated");
        keys.setProviderType(KeyProvider.class.getName());
        keys.setProviderId(providerId);
        keys.setParentId(realmId);
        keys.setConfig(new MultivaluedHashMap<>());
        keys.getConfig().putSingle("priority", Long.toString(System.currentTimeMillis()));
        keys.getConfig().putSingle("algorithm", algorithm);
        return keys;
    }

    protected void rotateKeys(String realmName, String algorithm, String providerId) {
        RealmResource realm = adminClient.realm(realmName);
        String activeKid = realm.keys().getKeyMetadata().getActive().get(algorithm);

        // Rotate public keys on the parent broker
        String realmId = realm.toRepresentation().getId();
        ComponentRepresentation keys = createComponentRep(algorithm, providerId, realmId);
        try (Response response = realm.components().add(keys)) {
            Assert.assertEquals(201, response.getStatus());
        }

        String updatedActiveKid = realm.keys().getKeyMetadata().getActive().get(algorithm);
        Assert.assertNotEquals(activeKid, updatedActiveKid);
    }

    protected void updateKeyProvider(String realmName, String providerId, boolean active, boolean enabled) {
        RealmResource realm = adminClient.realm(realmName);
        List<ComponentRepresentation> components = realm.components().query(null, KeyProvider.class.getName(), providerId);
        Assert.assertEquals("Key provider " + providerId + " not found.", 1, components.size());
        ComponentRepresentation key = components.iterator().next();
        MultivaluedHashMap<String, String> config = key.getConfig();
        if (config == null) {
            config = new MultivaluedHashMap<>();
            key.setConfig(config);
        }
        config.putSingle(Attributes.ACTIVE_KEY, Boolean.toString(active));
        config.putSingle(Attributes.ENABLED_KEY, Boolean.toString(enabled));
        realm.components().component(key.getId()).update(key);
    }
}
