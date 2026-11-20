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
package org.keycloak.tests.broker.saml;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.xml.crypto.dsig.CanonicalizationMethod;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlBrowserlessBrokerClient;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.METADATA_SIGNING_CERTIFICATE;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.USE_METADATA_DESCRIPTOR_URL;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.VALIDATE_METADATA_SIGNATURE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the validation of the signature of the metadata that is downloaded from the metadata descriptor URL
 * of a SAML identity provider. The keys of the identity provider are only loaded from metadata that is signed
 * with one of the configured metadata signing certificates.
 */
@KeycloakIntegrationTest
public class KcSamlMetadataSignatureBrokerTest extends AbstractKcSamlBrokerTest {

    private static final String METADATA_PATH = "/saml-idp-metadata";
    private static final String METADATA_NS = JBossSAMLURIConstants.METADATA_NSURI.get();

    @InjectHttpServer
    HttpServer httpServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    private final AtomicReference<String> metadata = new AtomicReference<>();
    private final AtomicInteger metadataRequests = new AtomicInteger();

    private KeyPair metadataKey;
    private String userId;

    @BeforeEach
    void configureMetadataSignatureValidation() throws Exception {
        metadataKey = KeyUtils.generateRsaKeyPair(2048);
        metadata.set(sign(entityDescriptor(), metadataKey));
        httpServer.createContext(METADATA_PATH, exchange -> {
            metadataRequests.incrementAndGet();
            HttpServerUtil.sendResponse(exchange, 200, Map.of("Content-Type", List.of("application/xml")), metadata.get());
        });

        updateIdp(idp -> {
            idp.getConfig().put(USE_METADATA_DESCRIPTOR_URL, "true");
            idp.getConfig().put(IdentityProviderModel.METADATA_DESCRIPTOR_URL,
                    "http://127.0.0.1:" + httpServer.getAddress().getPort() + METADATA_PATH);
            idp.getConfig().put(VALIDATE_METADATA_SIGNATURE, "true");
            idp.getConfig().put(METADATA_SIGNING_CERTIFICATE, certificate(metadataKey));
        });
    }

    @AfterEach
    void removeMetadata() {
        httpServer.removeContext(METADATA_PATH);
    }

    @Test
    public void testSignedMetadataIsAccepted() {
        createLinkedUser();

        assertLoginSucceeds(1);
        assertThat(metadataRequests.get(), is(1));
    }

    @Test
    public void testUnsignedMetadataIsRejected() {
        createLinkedUser();
        metadata.set(entityDescriptor());

        assertLoginFailsUnlessSignatureValidationIsDisabled();
    }

    @Test
    public void testMetadataSignedWithAnotherCertificateIsRejected() throws Exception {
        createLinkedUser();
        metadata.set(sign(entityDescriptor(), KeyUtils.generateRsaKeyPair(2048)));

        assertLoginFailsUnlessSignatureValidationIsDisabled();
    }

    @Test
    public void testMetadataModifiedAfterSigningIsRejected() {
        createLinkedUser();
        String entityId = getProviderRealm().getBaseUrl();
        metadata.set(metadata.get().replace("entityID=\"" + entityId + "\"", "entityID=\"" + entityId + "/modified\""));

        assertLoginFailsUnlessSignatureValidationIsDisabled();
    }

    @Test
    public void testSignatureOfAnotherEntityIsRejected() throws Exception {
        createLinkedUser();
        // the keys are loaded from the first entity, that is not signed, and the signed entity is the second one
        Document doc = DocumentUtil.getDocument("<md:EntitiesDescriptor xmlns:md=\"" + METADATA_NS + "\">"
                + entityDescriptor()
                + entityDescriptor().replace("ID=\"ID_metadata\"", "ID=\"ID_other\"")
                + "</md:EntitiesDescriptor>");
        sign((Element) doc.getElementsByTagNameNS(METADATA_NS, "EntityDescriptor").item(1), metadataKey);
        metadata.set(DocumentUtil.getDocumentAsString(doc));

        assertLoginFailsUnlessSignatureValidationIsDisabled();
    }

    @Test
    public void testRejectedMetadataDoesNotReplaceLoadedKeys() {
        createLinkedUser();
        assertLoginSucceeds(1);
        assertThat(metadataRequests.get(), is(1));

        // the metadata is replaced with one that is not signed and publishes another key
        metadata.set(entityDescriptor("another-key", certificate(KeyUtils.generateRsaKeyPair(2048))));
        // move the time to allow another request to the metadata descriptor URL
        timeOffSet.set(60);
        assertThrows(WebApplicationException.class, () -> idpResource().reloadKeys());
        assertThat(metadataRequests.get(), is(2));

        // the keys that were loaded from the signed metadata are still used
        assertLoginSucceeds(2);
        assertThat(metadataRequests.get(), is(2));
    }

    @Test
    public void testSignatureValidationRequiresCertificate() {
        IdentityProviderRepresentation idp = idpResource().toRepresentation();
        idp.getConfig().remove(METADATA_SIGNING_CERTIFICATE);

        BadRequestException e = assertThrows(BadRequestException.class, () -> idpResource().update(idp));
        assertThat(e.getResponse().readEntity(ErrorRepresentation.class).getErrorMessage(),
                is("validateMetadataSignature needs a non-empty certificate for metadataSigningCertificate"));
    }

    private void assertLoginFailsUnlessSignatureValidationIsDisabled() {
        // the keys to validate the response of the identity provider cannot be loaded
        assertThat(login(), greaterThanOrEqualTo(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(userSessions(), empty());

        // the same metadata is accepted, as before, if the signature is not validated
        updateIdp(idp -> idp.getConfig().put(VALIDATE_METADATA_SIGNATURE, "false"));
        assertLoginSucceeds(1);
    }

    private void assertLoginSucceeds(int expectedUserSessions) {
        assertThat(login(), is(Response.Status.OK.getStatusCode()));
        assertThat(userSessions(), hasSize(expectedUserSessions));
    }

    private int login() {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            return broker.login(getUserLogin(), getUserPassword()).submit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // the user is already linked to the identity provider, so a brokered login creates a session for the user
    private void createLinkedUser() {
        try (Response response = getConsumerRealm().admin().users().create(UserBuilder.create(getUserLogin())
                .enabled(true)
                .email(getUserEmail())
                .emailVerified(true)
                .name("Firstname", "Lastname")
                .federatedLink(getIdpAlias(), getUserLogin(), getUserLogin())
                .build())) {
            userId = ApiUtil.getCreatedId(response);
        }
    }

    private List<UserSessionRepresentation> userSessions() {
        return getConsumerRealm().admin().users().get(userId).getUserSessions();
    }

    private IdentityProviderResource idpResource() {
        return getConsumerRealm().admin().identityProviders().get(getIdpAlias());
    }

    private void updateIdp(Consumer<IdentityProviderRepresentation> update) {
        IdentityProviderRepresentation idp = idpResource().toRepresentation();
        update.accept(idp);
        idpResource().update(idp);
    }

    // metadata of the provider realm with the key that the realm uses to sign
    private String entityDescriptor() {
        KeyMetadataRepresentation key = getProviderRealm().admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> k.getCertificate() != null && KeyUse.SIG.equals(k.getUse()) && Algorithm.RS256.equals(k.getAlgorithm()))
                .findFirst().orElseThrow();
        return entityDescriptor(key.getKid(), key.getCertificate());
    }

    private String entityDescriptor(String keyName, String certificate) {
        return "<md:EntityDescriptor xmlns:md=\"" + METADATA_NS + "\" xmlns:ds=\"" + JBossSAMLURIConstants.XMLDSIG_NSURI.get() + "\""
                + " ID=\"ID_metadata\" entityID=\"" + getProviderRealm().getBaseUrl() + "\">"
                + "<md:IDPSSODescriptor protocolSupportEnumeration=\"" + JBossSAMLURIConstants.PROTOCOL_NSURI.get() + "\">"
                + "<md:KeyDescriptor use=\"signing\"><ds:KeyInfo><ds:KeyName>" + keyName + "</ds:KeyName>"
                + "<ds:X509Data><ds:X509Certificate>" + certificate + "</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>"
                + "<md:SingleSignOnService Binding=\"" + JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get() + "\""
                + " Location=\"" + getProviderRealm().getBaseUrl() + "/protocol/saml\"/>"
                + "</md:IDPSSODescriptor></md:EntityDescriptor>";
    }

    private static String sign(String entityDescriptor, KeyPair keyPair) throws Exception {
        Document doc = DocumentUtil.getDocument(entityDescriptor);
        sign(doc.getDocumentElement(), keyPair);
        return DocumentUtil.getDocumentAsString(doc);
    }

    private static void sign(Element element, KeyPair keyPair) throws Exception {
        element.setIdAttribute("ID", true);
        XMLSignatureUtil.sign(element, element.getFirstChild(), "metadata", keyPair,
                SignatureAlgorithm.RSA_SHA256.getXmlSignatureDigestMethod(), SignatureAlgorithm.RSA_SHA256.getXmlSignatureMethod(),
                "#" + element.getAttribute("ID"), CanonicalizationMethod.EXCLUSIVE);
    }

    private static String certificate(KeyPair keyPair) {
        return PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(keyPair, "metadata"));
    }
}
