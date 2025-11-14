package org.keycloak.tests.admin.identityprovider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.crypto.dsig.XMLSignature;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.KeyUtils;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.XMLDSIG_NSURI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class IdentityProviderSamlTest extends AbstractIdentityProviderTest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    // Certificate imported from
    private static final String SIGNING_CERT_1 = "MIICmzCCAYMCBgFUYnC0OjANBgkqhkiG9w0BAQsFADARMQ8wDQY"
            + "DVQQDDAZtYXN0ZXIwHhcNMTYwNDI5MTQzMjEzWhcNMjYwNDI5MTQzMzUzWjARMQ8wDQYDVQQDDAZtYXN0ZXI"
            + "wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCN25AW1poMEZRbuMAHG58AThZmCwMV6/Gcui4mjGa"
            + "cRFyudgqzLjQ2rxpoW41JAtLjbjeAhuWvirUcFVcOeS3gM/ZC27qCpYighAcylZz6MYocnEe1+e8rPPk4JlI"
            + "D6Wv62dgu+pL/vYsQpRhvD3Y2c/ytgr5D32xF+KnzDehUy5BSyzypvu12Wq9mS5vK5tzkN37EjkhpY2ZxaXP"
            + "ubjDIITCAL4Q8M/m5IlacBaUZbzI4AQrHnMP1O1IH2dHSWuMiBe+xSDTco72PmuYPJKTV4wQdeBUIkYbfLc4"
            + "RxVmXEvgkQgyW86EoMPxlWJpj7+mTIR+l+2thZPr/VgwTs82rAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAA/"
            + "Ip/Hi8RoVu5ouaFFlc5whT7ltuK8slfLGW4tM4vJXhInYwsqIRQKBNDYW/64xle3eII4u1yAH1OYRRwEs7Em"
            + "1pr4QuFuTY1at+aE0sE46XDlyESI0txJjWxYoT133vM0We2pj1b2nxgU30rwjKA3whnKEfTEYT/n3JBSqNgg"
            + "y6l8ZGw/oPSgvPaR4+xeB1tfQFC4VrLoYKoqH6hAL530nKxL+qV8AIfL64NDEE8ankIAEDAAFe8x3CPUfXR/"
            + "p4KOANKkpz8ieQaHDb1eITkAwUwjESj6UF9D1aePlhWls/HX0gujFXtWfWfrJ8CU/ogwlH8y1jgRuLjFQYZk6llc=";

    private static final String SIGNING_CERT_2 = "MIIBnDCCAQUCBgFYKXKsPTANBgkqhkiG9w0BAQsFADAUMRIwEAY"
            + "DVQQDDAlzYW1sLWRlbW8wHhcNMTYxMTAzMDkwNzEwWhcNMjYxMTAzMDkwODUwWjAUMRIwEAYDVQQDDAlzYW1"
            + "sLWRlbW8wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKtWsK5O0CtuBpnMvWG+HTG0vmZzujQ2o9WdheQ"
            + "u+BzCILcGMsbDW0YQaglpcO5JpGWWhubnckGGPHfdQ2/7nP9QwbiTK0FbGF41UqcvoaCqU1psxoV88s8IXyQ"
            + "CAqeyLv00yj6foqdJjxh5SZ5z+na+M7Y2OxIBVxYRAxWEnfUvAgMBAAEwDQYJKoZIhvcNAQELBQADgYEAhet"
            + "vOU8TyqfZF5jpv0IcrviLl/DoFrbjByeHR+pu/vClcAOjL/u7oQELuuTfNsBI4tpexUj5G8q/YbEz0gk7idf"
            + "LXrAUVcsR73oTngrhRfwUSmPrjjK0kjcRb6HL9V/+wh3R/6mEd59U08ExT8N38rhmn0CI3ehMdebReprP7U8=";

    @Test
    public void testRemove() {
        IdentityProviderRepresentation newIdentityProvider = createRep("remove-identity-provider", "saml");

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = managedRealm.admin().identityProviders().get("remove-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        identityProviderResource.remove();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.identityProviderPath("remove-identity-provider"), ResourceType.IDENTITY_PROVIDER);

        try {
            managedRealm.admin().identityProviders().get("remove-identity-provider").toRepresentation();
            Assertions.fail("Not expected to found");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    @Test
    public void testNotAvailableFromRealRepresentation() {
        IdentityProviderRepresentation newIdentityProvider = createRep("remove-identity-provider", "saml");

        String id = create(newIdentityProvider);

        RealmRepresentation rep = this.managedRealm.admin().toRepresentation();
        assertNull(rep.getIdentityProviders());
        assertNull(rep.getIdentityProviderMappers());

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void importShouldFailDueAliasWithSpace() {

        Map<String, Object> data = new HashMap<>();
        data.put("providerId", "saml");
        data.put("alias", "Alias With Space");
        data.put("fromUrl", "http://");

        assertThrows(BadRequestException.class, () -> {
            managedRealm.admin().identityProviders().importFrom(data);
        });

    }

    @Test
    public void testSamlImportAndExport() throws URISyntaxException, IOException, ParsingException {
        testSamlImport("saml-idp-metadata.xml", true);

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = managedRealm.admin().identityProviders().get("saml").export("xml");
        Assertions.assertEquals(200, response.getStatus());
        String body = response.readEntity(String.class);
        response.close();

        assertSamlExport(body, true);
    }

    @Test
    public void testSamlImportWithBom() throws URISyntaxException, IOException, ParsingException {
        testSamlImport("saml-idp-metadata_utf8_bom.xml", true);

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = managedRealm.admin().identityProviders().get("saml").export("xml");
        Assertions.assertEquals(200, response.getStatus());
        String body = response.readEntity(String.class);
        response.close();

        assertSamlExport(body, true);
    }

    @Test
    public void testSamlImportAndExportDifferentBindings() throws URISyntaxException, IOException, ParsingException {
        testSamlImport("saml-idp-metadata-different-bindings.xml", false);

        // Perform export, and make sure some of the values are like they're supposed to be
        try (Response response = managedRealm.admin().identityProviders().get("saml").export("xml")) {
            Assertions.assertEquals(200, response.getStatus());
            String body = response.readEntity(String.class);
            assertSamlExport(body, false);
        }
    }

    @Test
    public void testSamlImportWithAnyEncryptionMethod() throws URISyntaxException, IOException, ParsingException {
        testSamlImport("saml-idp-metadata-encryption-methods.xml", true);
    }

    @Test
    public void testSamlImportAndExportDisabled() throws URISyntaxException, IOException, ParsingException {

        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = IdentityProviderSamlTest.class.getResource("saml-idp-metadata-disabled.xml");
        byte[] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata-disabled.xml");

        Map<String, String> result = managedRealm.admin().identityProviders().importFrom(form);
        assertSamlImport(result, SIGNING_CERT_1, false, true);

        // Create new SAML identity provider using configuration retrieved from import-config
        String id = create(createRep("saml", "saml", false, result));

        IdentityProviderResource provider = managedRealm.admin().identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep, false, true);

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void testSamlImportAndExportMultipleSigningKeys() throws URISyntaxException, IOException, ParsingException {

        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = IdentityProviderSamlTest.class.getResource("saml-idp-metadata-two-signing-certs.xml");
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata-two-signing-certs");

        Map<String, String> result = managedRealm.admin().identityProviders().importFrom(form);
        assertSamlImport(result, SIGNING_CERT_1 + "," + SIGNING_CERT_2, true, true);

        // Create new SAML identity provider using configuration retrieved from import-config
        String id = create(createRep("saml", "saml",true, result));

        IdentityProviderResource provider = managedRealm.admin().identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep, true, true);

        // Now list the providers - we should see the one just created
        List<IdentityProviderRepresentation> providers = managedRealm.admin().identityProviders().findAll();
        Assertions.assertNotNull(providers, "identityProviders not null");
        Assertions.assertEquals(1, providers.size(), "identityProviders instance count");
        assertEqual(rep, providers.get(0));

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = managedRealm.admin().identityProviders().get("saml").export("xml");
        Assertions.assertEquals(200, response.getStatus());
        body = response.readEntity(String.class);
        response.close();

        assertSamlExport(body, true);

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void testSamlExportSignatureOff() throws URISyntaxException, IOException, ConfigurationException, ParsingException, ProcessingException {
        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = IdentityProviderSamlTest.class.getResource("saml-idp-metadata.xml");
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

        Map<String, String> result = managedRealm.admin().identityProviders().importFrom(form);

        // Explicitly disable SP Metadata Signature
        result.put(SAMLIdentityProviderConfig.SIGN_SP_METADATA, "false");

        // Create new SAML identity provider using configuration retrieved from import-config
        IdentityProviderRepresentation idpRep = createRep("saml", "saml", true, result);
        String id = create(idpRep);

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = managedRealm.admin().identityProviders().get("saml").export("xml");
        Assertions.assertEquals(200, response.getStatus());
        body = response.readEntity(String.class);
        response.close();

        Document document = DocumentUtil.getDocument(body);
        Element signatureElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), XMLDSIG_NSURI.get(), "Signature");
        Assertions.assertNull(signatureElement);

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void testSamlExportSignatureOn() throws URISyntaxException, IOException, ConfigurationException, ParsingException, ProcessingException, CertificateEncodingException {
        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = IdentityProviderSamlTest.class.getResource("saml-idp-metadata.xml");
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

        Map<String, String> result = managedRealm.admin().identityProviders().importFrom(form);

        // Explicitly enable SP Metadata Signature
        result.put(SAMLIdentityProviderConfig.SIGN_SP_METADATA, "true");
        result.put(SAMLIdentityProviderConfig.XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER, XmlKeyInfoKeyNameTransformer.CERT_SUBJECT.name());

        // Create new SAML identity provider using configuration retrieved from import-config
        IdentityProviderRepresentation idpRep = createRep("saml", "saml", true, result);
        String id = create(idpRep);

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = managedRealm.admin().identityProviders().get("saml").export("xml");
        Assertions.assertEquals(200, response.getStatus());
        body = response.readEntity(String.class);
        response.close();

        Document document = DocumentUtil.getDocument(body);

        Element signatureElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), XMLDSIG_NSURI.get(), "Signature");
        assertThat("Signature not null", signatureElement, notNullValue());

        Element keyInfoElement = DocumentUtil.getDirectChildElement(signatureElement, XMLDSIG_NSURI.get(), "KeyInfo");
        assertThat("KeyInfo not null", keyInfoElement, notNullValue());

        Element x509DataElement = DocumentUtil.getDirectChildElement(keyInfoElement, XMLDSIG_NSURI.get(), "X509Data");
        assertThat("X509Data not null", x509DataElement, notNullValue());

        Element x509CertificateElement = DocumentUtil.getDirectChildElement(x509DataElement, XMLDSIG_NSURI.get(), "X509Certificate");
        assertThat("X509Certificate not null", x509CertificateElement, notNullValue());

        Element keyNameElement = DocumentUtil.getDirectChildElement(keyInfoElement, XMLDSIG_NSURI.get(), "KeyName");
        assertThat("KeyName not null", keyNameElement, notNullValue());

        String activeSigCert = KeyUtils.findActiveSigningKey(managedRealm.admin(), Constants.DEFAULT_SIGNATURE_ALGORITHM).getCertificate();
        assertThat("activeSigCert not null", activeSigCert, notNullValue());

        X509Certificate activeX509SigCert = XMLSignatureUtil.getX509CertificateFromKeyInfoString(activeSigCert);
        assertThat("KeyName matches subject DN",
                keyNameElement.getTextContent().trim(), equalTo(activeX509SigCert.getSubjectX500Principal().getName()));

        assertThat("Signing cert matches active realm cert",
                x509CertificateElement.getTextContent().trim(), equalTo(Base64.getEncoder().encodeToString(activeX509SigCert.getEncoded())));

        PublicKey activePublicSigKey = activeX509SigCert.getPublicKey();
        assertThat("Metadata signature is valid",
                new SAML2Signature().validate(document, new HardcodedKeyLocator(activePublicSigKey)), is(true));

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    private void testSamlImport(String fileName, boolean postBindingResponse) throws URISyntaxException, IOException, ParsingException {
        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = IdentityProviderSamlTest.class.getResource(fileName);
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, fileName);

        Map<String, String> result = managedRealm.admin().identityProviders().importFrom(form);
        assertSamlImport(result, SIGNING_CERT_1, true, postBindingResponse);

        // Create new SAML identity provider using configuration retrieved from import-config
        String id = create(createRep("saml", "saml",true, result));

        IdentityProviderResource provider = managedRealm.admin().identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep, true, postBindingResponse);

        // Now list the providers - we should see the one just created
        List<IdentityProviderRepresentation> providers = managedRealm.admin().identityProviders().findAll();
        Assertions.assertNotNull(providers, "identityProviders not null");
        Assertions.assertEquals(1, providers.size(), "identityProviders instance count");
        assertEqual(rep, providers.get(0));

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    private void assertEqual(IdentityProviderRepresentation expected, IdentityProviderRepresentation actual) {
        //System.out.println("expected: " + expected);
        //System.out.println("actual: " + actual);
        Assertions.assertNotNull(expected, "expected IdentityProviderRepresentation not null");
        Assertions.assertNotNull(actual, "actual IdentityProviderRepresentation not null");
        Assertions.assertEquals(expected.getInternalId(), actual.getInternalId(), "internalId");
        Assertions.assertEquals(expected.getAlias(), actual.getAlias(), "alias");
        Assertions.assertEquals(expected.getProviderId(), actual.getProviderId(), "providerId");
        Assertions.assertEquals(expected.isEnabled(), actual.isEnabled(), "enabled");
        Assertions.assertEquals(expected.isHideOnLogin(), actual.isHideOnLogin(), "hideOnLogin");
        Assertions.assertEquals(expected.getFirstBrokerLoginFlowAlias(), actual.getFirstBrokerLoginFlowAlias(), "firstBrokerLoginFlowAlias");
        Assertions.assertEquals(expected.getConfig(), actual.getConfig(), "config");
    }

    private void assertCreatedSamlIdp(IdentityProviderRepresentation idp, boolean enabled, boolean postBindingResponse) {
        //System.out.println("idp: " + idp);
        Assertions.assertNotNull(idp, "IdentityProviderRepresentation not null");
        Assertions.assertNotNull(idp.getInternalId(), "internalId");
        Assertions.assertEquals("saml", idp.getAlias(), "alias");
        Assertions.assertEquals("saml", idp.getProviderId(), "providerId");
        Assertions.assertEquals(enabled, idp.isEnabled(), "enabled");
        Assertions.assertTrue(idp.isHideOnLogin(), "hideOnLogin");
        Assertions.assertNull(idp.getFirstBrokerLoginFlowAlias(), "firstBrokerLoginFlowAlias");
        assertSamlConfig(idp.getConfig(), postBindingResponse, false);
    }

    private void assertSamlConfig(Map<String, String> config, boolean postBindingResponse, boolean hasHideOnLoginPage) {
        // import endpoint simply converts IDPSSODescriptor into key value pairs.
        // check that saml-idp-metadata.xml was properly converted into key value pairs
        //System.out.println(config);
        List<String> keys = new ArrayList<>(List.of(
                "validateSignature",
                "singleLogoutServiceUrl",
                "postBindingLogout",
                "postBindingResponse",
                "artifactBindingResponse",
                "postBindingAuthnRequest",
                "singleSignOnServiceUrl",
                "artifactResolutionServiceUrl",
                "wantAuthnRequestsSigned",
                "nameIDPolicyFormat",
                "signingCertificate",
                "addExtensionsElementWithKeyInfo",
                "loginHint",
                "idpEntityId"
        ));
        if (hasHideOnLoginPage) {
            keys.add("hideOnLoginPage");
        }
        assertThat(config.keySet(), containsInAnyOrder(keys.toArray()));
        assertThat(config, hasEntry("validateSignature", "true"));
        assertThat(config, hasEntry("singleLogoutServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("artifactResolutionServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml/resolve"));
        assertThat(config, hasEntry("postBindingResponse", Boolean.toString(postBindingResponse)));
        assertThat(config, hasEntry("artifactBindingResponse", "false"));
        assertThat(config, hasEntry("postBindingAuthnRequest", Boolean.toString(postBindingResponse)));
        assertThat(config, hasEntry("singleSignOnServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("wantAuthnRequestsSigned", "true"));
        assertThat(config, hasEntry("addExtensionsElementWithKeyInfo", "false"));
        assertThat(config, hasEntry("nameIDPolicyFormat", "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        if (hasHideOnLoginPage) {
            assertThat(config, hasEntry("hideOnLoginPage", "true"));
        }
        assertThat(config, hasEntry("idpEntityId", "http://localhost:8080/auth/realms/master"));
        assertThat(config, hasEntry(is("signingCertificate"), notNullValue()));
    }

    private void assertSamlImport(Map<String, String> config, String expectedSigningCertificates, boolean enabled, boolean postBindingResponse) {
        //firtsly check and remove enabledFromMetadata from config
        boolean enabledFromMetadata = Boolean.valueOf(config.get(SAMLIdentityProviderConfig.ENABLED_FROM_METADATA));
        config.remove(SAMLIdentityProviderConfig.ENABLED_FROM_METADATA);
        Assert.assertEquals(enabledFromMetadata,enabled);
        assertSamlConfig(config, postBindingResponse, true);
        assertThat(config, hasEntry("signingCertificate", expectedSigningCertificates));
    }

    private void assertSamlExport(String body, boolean postBindingResponse) throws ParsingException, URISyntaxException {
        //System.out.println(body);

        Object entBody = SAMLParser.getInstance().parse(
                new ByteArrayInputStream(body.getBytes(Charset.forName("utf-8"))));

        Assertions.assertEquals(EntityDescriptorType.class, entBody.getClass(), "Parsed export type");
        EntityDescriptorType entity = (EntityDescriptorType) entBody;

        Assertions.assertEquals(keycloakUrls.getBaseUrl() + "/realms/default", entity.getEntityID(), "EntityID");

        Assertions.assertNotNull(entity.getChoiceType(), "ChoiceType not null");
        Assertions.assertEquals(1, entity.getChoiceType().size(), "ChoiceType.size");

        List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = entity.getChoiceType().get(0).getDescriptors();
        Assertions.assertNotNull(descriptors, "Descriptors not null");
        Assertions.assertEquals(1, descriptors.size(), "Descriptors.size");

        SPSSODescriptorType desc = descriptors.get(0).getSpDescriptor();
        Assertions.assertNotNull(desc, "SPSSODescriptor not null");

        Assertions.assertTrue(desc.isAuthnRequestsSigned(), "AuthnRequestsSigned");

        Set<String> expected = new HashSet<>(Arrays.asList(
                "urn:oasis:names:tc:SAML:2.0:protocol"));

        Set<String> actual = new HashSet<>(desc.getProtocolSupportEnumeration());

        Assertions.assertEquals(expected, actual, "ProtocolSupportEnumeration");

        Assertions.assertNotNull(desc.getAssertionConsumerService(), "AssertionConsumerService not null");
        Assertions.assertEquals(3, desc.getAssertionConsumerService().size(), "AssertionConsumerService.size");

        IndexedEndpointType endpoint = desc.getAssertionConsumerService().get(0);
        final URI samlUri = new URI(keycloakUrls.getBase() + "/realms/default/broker/saml/endpoint");

        Assertions.assertEquals(samlUri, endpoint.getLocation(), "AssertionConsumerService.Location");
        Assert.assertEquals(postBindingResponse ? JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri() : JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri(),
                endpoint.getBinding(), "AssertionConsumerService.Binding");
        Assertions.assertTrue(endpoint.isIsDefault(), "AssertionConsumerService.isDefault");

        endpoint = desc.getAssertionConsumerService().get(1);

        Assertions.assertEquals(samlUri, endpoint.getLocation(), "AssertionConsumerService.Location");
        Assert.assertEquals(postBindingResponse ? JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri() : JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri(),
                endpoint.getBinding(), "AssertionConsumerService.Binding");

        endpoint = desc.getAssertionConsumerService().get(2);

        Assertions.assertEquals(samlUri, endpoint.getLocation(), "AssertionConsumerService.Location");
        Assertions.assertEquals(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri(), endpoint.getBinding(), "AssertionConsumerService.Binding");

        Assertions.assertNotNull(desc.getSingleLogoutService(), "SingleLogoutService not null");
        Assertions.assertEquals(2, desc.getSingleLogoutService().size(), "SingleLogoutService.size");

        EndpointType sloEndpoint = desc.getSingleLogoutService().get(0);

        Assertions.assertEquals(samlUri, sloEndpoint.getLocation(), "SingleLogoutService.Location");
        Assertions.assertEquals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri(), sloEndpoint.getBinding(), "SingleLogoutService.Binding");

        sloEndpoint = desc.getSingleLogoutService().get(1);

        Assertions.assertEquals(samlUri, sloEndpoint.getLocation(), "SingleLogoutService.Location");
        Assertions.assertEquals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri(), sloEndpoint.getBinding(), "SingleLogoutService.Binding");

        Assertions.assertNotNull(desc.getKeyDescriptor(), "KeyDescriptor not null");
        Assertions.assertEquals(1, desc.getKeyDescriptor().size(), "KeyDescriptor.size");
        KeyDescriptorType keyDesc = desc.getKeyDescriptor().get(0);
        assertThat(keyDesc, notNullValue());
        assertThat(keyDesc.getUse(), equalTo(KeyTypes.SIGNING));
        NodeList cert = keyDesc.getKeyInfo().getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate");
        assertThat("KeyDescriptor.Signing.Cert existence", cert.getLength(), is(1));
    }
}
