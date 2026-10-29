package org.keycloak.tests.broker.saml;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.crypto.dsig.XMLSignature;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.testsuite.util.AccountHelper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@KeycloakIntegrationTest
public class KcSamlBrokerTest extends AbstractKcSamlBrokerTest {

    @InjectPage
    ErrorPage errorPage;

    @InjectEvents(realmRef = "consumer")
    Events events;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectHttpServer
    HttpServer httpServer;

    private final AtomicBoolean signatureValidBeforeTampering = new AtomicBoolean();
    private final AtomicBoolean signatureValidAfterTampering = new AtomicBoolean();

    @Test
    public void testIdpCreatedWithoutSignatureSettings_LoginFailsWhenResponseIsUnsigned() {
        // The provider client sends an unsigned response by default
        recreateIdpWithoutSignatureSettings();
        assertLoginFailsWithSignatureError();

        // Control: once the provider signs the response and the assertion, the login succeeds, so the failure above is caused by the signature
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
        providerRealm.admin().clients().get(client.getId()).update(client);

        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        events.clear();
        loginUser();
    }

    // Recreate the fixture IdP with the same alias, omitting both signature settings and trusting the provider certificate.
    private void recreateIdpWithoutSignatureSettings() {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        consumerRealm.admin().identityProviders().get(getIdpAlias()).remove();

        idp.setInternalId(null);
        Map<String, String> config = idp.getConfig();
        config.remove(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE);
        config.remove(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED);
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, activeRs256SigCert(providerRealm));

        try (Response response = consumerRealm.admin().identityProviders().create(idp)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        Map<String, String> stored = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation().getConfig();
        Assertions.assertFalse(stored.containsKey(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE));
        Assertions.assertFalse(stored.containsKey(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED));
    }

    private IdentityProviderRepresentation fixtureIdpWithoutSignatureSettings(String cert) {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.setInternalId(null);
        Map<String, String> config = idp.getConfig();
        config.remove(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE);
        config.remove(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED);
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, cert);
        return idp;
    }

    private RealmRepresentation realmWithSamlIdpWithoutSignatureSettings(String realmName, String cert) {
        String importedBaseUrl = consumerRealm.getBaseUrl().replace("/realms/" + CONSUMER_REALM, "/realms/" + realmName);
        String consumerServerRoot = importedBaseUrl.substring(0, importedBaseUrl.indexOf("/realms/"));
        String salesPostBaseUrl = consumerServerRoot + "/sales-post";

        return RealmBuilder.create()
                .name(realmName)
                .eventsEnabled(true)
                .eventsListeners("jboss-logging")
                .enabledEventTypes("LOGIN", "LOGIN_ERROR",
                        "IDENTITY_PROVIDER_LOGIN", "IDENTITY_PROVIDER_LOGIN_ERROR",
                        "IDENTITY_PROVIDER_RESPONSE", "IDENTITY_PROVIDER_RESPONSE_ERROR",
                        "IDENTITY_PROVIDER_FIRST_LOGIN", "IDENTITY_PROVIDER_FIRST_LOGIN_ERROR")
                .clients(
                        ClientBuilder.create(CONSUMER_BROKER_APP_CLIENT_ID)
                                .secret(CONSUMER_BROKER_APP_SECRET)
                                .redirectUris(oauth.getRedirectUri()),
                        ClientBuilder.create(SAML_CLIENT_ID_SALES_POST)
                                .enabled(true)
                                .fullScopeEnabled(true)
                                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                                .baseUrl(salesPostBaseUrl)
                                .redirectUris(salesPostBaseUrl + "/*")
                                .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
                                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE))
                .identityProviders(fixtureIdpWithoutSignatureSettings(cert))
                .build();
    }

    @Test
    public void testRealmImportWithoutSignatureSettings_LoginFailsWhenResponseIsUnsigned() {
        String realmName = "imported-saml-consumer";
        String providerCert = activeRs256SigCert(providerRealm);

        String importedBaseUrl = consumerRealm.getBaseUrl().replace("/realms/" + CONSUMER_REALM, "/realms/" + realmName);
        String importedBrokerEndpoint = importedBaseUrl + "/broker/" + getIdpAlias() + "/endpoint";

        ClientRepresentation providerClient = providerRealm.admin().clients().findByClientId(consumerRealm.getBaseUrl()).get(0);
        ClientRepresentation importedClient = new ClientRepresentation();
        importedClient.setClientId(importedBaseUrl);
        importedClient.setEnabled(true);
        importedClient.setProtocol(IDP_SAML_PROVIDER_ID);
        importedClient.setRedirectUris(List.of(importedBrokerEndpoint));
        importedClient.setAttributes(new HashMap<>(providerClient.getAttributes()));
        importedClient.getAttributes().put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, importedBrokerEndpoint);
        importedClient.getAttributes().put(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, importedBrokerEndpoint);
        importedClient.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        importedClient.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");
        try (Response response = providerRealm.admin().clients().create(importedClient)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        adminClient.realms().create(realmWithSamlIdpWithoutSignatureSettings(realmName, providerCert));
        try {
            IdentityProviderRepresentation imported = adminClient.realm(realmName).identityProviders().get(getIdpAlias()).toRepresentation();
            Assertions.assertFalse(imported.getConfig().containsKey(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE));
            Assertions.assertFalse(imported.getConfig().containsKey(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED));

            oauth.realm(realmName);
            oauth.openLoginForm();
            logInWithBroker();
            loginPage.fillLogin(getUserLogin(), getUserPassword());
            loginPage.submit();

            errorPage.assertCurrent();

            // Control: once the provider signs the response and assertions, login succeeds
            ClientRepresentation createdClient = providerRealm.admin().clients().findByClientId(importedBaseUrl).get(0);
            createdClient.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
            createdClient.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
            createdClient.getAttributes().put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
            providerRealm.admin().clients().get(createdClient.getId()).update(createdClient);

            AccountHelper.logout(providerRealm.admin(), getUserLogin());

            oauth.openLoginForm();
            logInWithBroker();
            loginPage.fillLogin(getUserLogin(), getUserPassword());
            loginPage.submit();
            updateAccountInformation();
        } finally {
            adminClient.realm(realmName).remove();
            List<ClientRepresentation> clientList = providerRealm.admin().clients().findByClientId(importedBaseUrl);
            if (!clientList.isEmpty()) {
                providerRealm.admin().clients().get(clientList.get(0).getId()).remove();
            }
        }
    }

    @Test
    public void testLoginFailsWhenResponse_UnsignedANDValidateSignatureIsExplicitlyTrue() {
        // Configure the provider
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, activeRs256SigCert(providerRealm));

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
        // Attempt to log in should fail due to missing signature
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginFailsWhenAssertionHasInvalidSignatureAndWantAssertionsSignedIsTrue() {
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");

        providerRealm.admin().clients().get(client.getId()).update(client);

        String consumerSigningCert = activeRs256SigCert(consumerRealm);

        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true");
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, consumerSigningCert);
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginFailsWhenWantAssertionsSignedIsExplicitlyTrue() {
        // wantAssertionsSigned defaults to true when omitted.
        configureSignedResponseWithUnsignedAssertion(true, true);
        assertLoginFailsWithSignatureError();
    }

    @Test
    public void testLoginFailsWhenWantAssertionsSignedIsOmitted() {
        // The response is signed and validated, so only the omitted wantAssertionsSigned can cause the rejection of the unsigned assertion
        configureSignedResponseWithUnsignedAssertion(null, true);
        Assertions.assertFalse(consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation().getConfig().containsKey(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED));
        assertLoginFailsWithSignatureError();

        // Control: the same unsigned assertion is accepted once wantAssertionsSigned is explicitly false
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "false");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        events.clear();
        loginUser();
    }

    @Test
    public void testLoginSucceedsWhenWantAssertionsSignedIsFalse() {
        configureSignedResponseWithUnsignedAssertion(false,true);
        loginUser();
    }

    @Test
    public void testLoginFailsWhenWantAssertionsSignedIsTrueAndValidateSignatureIsFalse() {
        configureSignedResponseWithUnsignedAssertion(true, false);
        assertLoginFailsWithSignatureError();
    }

    private void assertLoginFailsWithSignatureError() {
        oauth.openLoginForm();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        errorPage.assertCurrent();
        EventAssertion.assertError(events.poll())
                .type(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR)
                .error(Errors.INVALID_SIGNATURE);
    }


    private void configureSignedResponseWithUnsignedAssertion(
            Boolean wantAssertionsSigned,
            boolean validateSignature) {
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients()
                .findByClientId(samlClientId).get(0);

        client.getAttributes().put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp =
                consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();

        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE,
                Boolean.toString(validateSignature));
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY,
                activeRs256SigCert(providerRealm));

        if (wantAssertionsSigned == null) {
            config.remove(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED);
        } else {
            config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED,
                    Boolean.toString(wantAssertionsSigned));
        }

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }

    @Test
    public void testLoginFailsWhenAssertionSignatureIsTampered() {
        CryptoIntegration.init(getClass().getClassLoader());
        KeyPair idpKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate idpCert = CertificateUtils.generateV1SelfSignedCertificate(idpKeyPair, "test-idp");

        String mockIdpUrl = setupMockIdp(idpKeyPair);
        configureIdpWithCorrectCert(mockIdpUrl, idpCert);

        try {
            getConsumerRealm().admin().users().list().forEach(u -> getConsumerRealm().admin().users().get(u.getId()).logout());

            oauth.openLoginForm();
            logInWithBroker();

            Assertions.assertTrue(signatureValidBeforeTampering.get(), "The assertion must have a valid signature before tampering");
            Assertions.assertFalse(signatureValidAfterTampering.get(), "Tampering must invalidate the assertion signature");

            errorPage.assertCurrent();
            EventAssertion.assertError(events.poll())
                    .type(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR)
                    .error(Errors.INVALID_SIGNATURE);

        } finally {
            httpServer.removeContext("/mock-idp");
        }
    }

    private String setupMockIdp(KeyPair idpKeyPair) {
        String path = "/mock-idp";
        String url = "http://" + httpServer.getAddress().getHostString() + ":"
                + httpServer.getAddress().getPort() + path;

        httpServer.createContext(path, ex -> handleMockIdpRequest(ex, idpKeyPair));
        return url;
    }

    private void handleMockIdpRequest(HttpExchange ex, KeyPair idpKeyPair) {
        try {
            String query = ex.getRequestURI().getQuery();
            String samlRequest = query.split("SAMLRequest=")[1].split("&")[0];
            String relayState = query.contains("RelayState=") ? query.split("RelayState=")[1].split("&")[0] : null;

            SAMLDocumentHolder req = SAMLRequestParser.parseRequestRedirectBinding(samlRequest);
            String requestId = ((org.keycloak.dom.saml.v2.protocol.AuthnRequestType) req.getSamlObject()).getID();

            Document signedDoc = buildSignedResponse(requestId, idpKeyPair);
            signatureValidBeforeTampering.set(isAssertionSignatureValid(signedDoc, idpKeyPair.getPublic()));
            tamperDigestValue(signedDoc);
            signatureValidAfterTampering.set(isAssertionSignatureValid(signedDoc, idpKeyPair.getPublic()));
            sendResponse(ex, signedDoc, relayState);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ex.sendResponseHeaders(500, 0);
                ex.getResponseBody().close();
            } catch (Exception ignored) {
            }
        }
    }

    private Document buildSignedResponse(String requestId, KeyPair idpKeyPair) throws Exception {
        String destination = getConsumerRealm().getBaseUrl() + "/broker/" + IDP_SAML_ALIAS + "/endpoint";

        ResponseType resp = new SAML2LoginResponseBuilder()
                .requestID(requestId)
                .destination(destination)
                .issuer("test-idp")
                .requestIssuer(getConsumerRealm().getBaseUrl())
                .assertionExpiration(300)
                .subjectExpiration(300)
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), getUserEmail())
                .buildModel();


        AssertionType assertion = resp.getAssertions().get(0).getAssertion();

        AttributeStatementType attrStatement = new AttributeStatementType();
        AttributeType attr = new AttributeType("username");
        attr.addAttributeValue(getUserLogin());
        attrStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attr));
        assertion.addStatement(attrStatement);

        Document doc = new SAML2Response().convert(resp);

        new BaseSAML2BindingBuilder<>()
                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                .signWith("key", idpKeyPair)
                .signAssertion(doc);

        return doc;
    }

    private boolean isAssertionSignatureValid(Document doc, PublicKey publicKey) {
        Element assertion = (Element) doc.getDocumentElement().getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion").item(0);
        return AssertionUtil.isSignatureValid(assertion, new HardcodedKeyLocator(publicKey));
    }

    private void tamperDigestValue(Document doc) {
        Element digestValue = (Element) doc.getElementsByTagNameNS(XMLSignature.XMLNS, "DigestValue").item(0);

        Assertions.assertNotNull(digestValue, "Signed assertion must contain a DigestValue");

        String original = digestValue.getTextContent();
        Assertions.assertFalse(original.isEmpty(), "DigestValue must not be empty");

        String tamperedSignature = (original.charAt(0) == 'A' ? "B" : "A") + original.substring(1);
        digestValue.setTextContent(tamperedSignature);
    }

    private void sendResponse(HttpExchange ex, Document doc, String relayState) throws Exception {
        String destination = getConsumerRealm().getBaseUrl() + "/broker/" + IDP_SAML_ALIAS + "/endpoint";
        String encoded = Base64.getEncoder().encodeToString(DocumentUtil.asString(doc).getBytes(StandardCharsets.UTF_8));

        String html = "<html><body onload='document.forms[0].submit()'>"
                + "<form method='POST' action='" + destination + "'>"
                + "<input type='hidden' name='SAMLResponse' value='" + encoded + "'/>"
                + (relayState != null ? "<input type='hidden' name='RelayState' value='" + relayState + "'/>" : "")
                + "</form></body></html>";

        ex.getResponseHeaders().add("Content-Type", "text/html");
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private void configureIdpWithCorrectCert(String mockIdpUrl, X509Certificate cert) {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders()
                .get(getIdpAlias()).toRepresentation();
        Map<String, String> config = idp.getConfig();
        config.put(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, mockIdpUrl);
        config.put(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false");
        config.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true");
        config.put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "false");
        config.put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, PemUtils.encodeCertificate(cert));
        config.put(SAMLIdentityProviderConfig.IDP_ENTITY_ID, "test-idp");

        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}
