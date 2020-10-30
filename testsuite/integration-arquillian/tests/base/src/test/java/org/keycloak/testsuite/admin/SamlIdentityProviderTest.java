package org.keycloak.testsuite.admin;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.crypto.dsig.XMLSignature;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.mdui.LogoType;
import org.keycloak.dom.saml.v2.mdui.UIInfoType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.ContactType;
import org.keycloak.dom.saml.v2.metadata.ContactTypeType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.events.log.JBossLoggingEventListenerProviderFactory;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.events.EventsListenerProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.w3c.dom.NodeList;

public class SamlIdentityProviderTest extends IdentityProviderTest {
    
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
    
    private static final Map<String, String> attributesMap =
        Arrays.stream(new String[][] { { "http://macedir.org/entity-category", "http://refeds.org/category/research-and-scholarship,http://www.geant.net/uri/dataprotection-code-of-conduct/v1" } }).collect(Collectors.toMap(st -> st[0], st -> st[1]));

    private static final String LANG="en";
    
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.debug("Adding test realm for import from testrealm.json");
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        testRealms.add(testRealm);

        configureTestRealm(testRealm);

        RealmRepresentation adminRealmRep = new RealmRepresentation();
        adminRealmRep.setId(REALM_NAME);
        adminRealmRep.setRealm(REALM_NAME);
        adminRealmRep.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put("from", "auto@keycloak.org");
        config.put("host", "localhost");
        config.put("port", "3025");
        adminRealmRep.setSmtpServer(config);
        
        // add realm metadata configurations used in SAML SP metadata
        Map<String,String> attributes = new HashMap<>();
        try {
            attributes.put("samlAttributes", JsonSerialization.writeValueAsPrettyString(attributesMap));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        attributes.put("mdrpiRegistrationAuthority", "http://www.surfconext.nl/");
        attributes.put("mdrpiRegistrationPolicy", "https://wiki.surfnet.nl/display/eduGAIN/EduGAIN");
        attributes.put("mduiDescription", "Keycloak test SP");
        attributes.put("mduiInformationURL", "https://www.keycloak.org/documentation.html");
        attributes.put("mduiPrivacyStatementURL", "https://www.keycloak.org/");
        attributes.put("mduiLogo", "https://github.com/keycloak/keycloak-misc/blob/master/logo/keycloak_icon_256px.png");
        attributes.put("mduiLogoHeight", "100");
        attributes.put("mduiLogoWidth", "110");
        attributes.put("mdOrganizationName", "Keycloak");
        attributes.put("mdOrganizationDisplayName", "Keycloak organization");
        attributes.put("mdOrganizationURL", "https://www.keycloak.org/");
        attributes.put("mdContactType", "SUPPORT");
        attributes.put("mdContactSurname", "Sub");
        attributes.put("mdContactGivenName", "Bob");
        attributes.put("mdContactCompany", "Keycloak");
        attributes.put("mdContactEmailAddress", "support@keycloak.com");
        attributes.put("mdContactTelephoneNumber", "2112113334,2112113335");
        adminRealmRep.setAttributes(attributes);

        List<String> eventListeners = new ArrayList<>();
        eventListeners.add(JBossLoggingEventListenerProviderFactory.ID);
        eventListeners.add(EventsListenerProviderFactory.PROVIDER_ID);
        adminRealmRep.setEventsListeners(eventListeners);

        testRealms.add(adminRealmRep);
    }
    
    @Test
    public void testSamlImportAndExport() throws URISyntaxException, IOException, ParsingException {

        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = getClass().getClassLoader().getResource("admin-test/saml-idp-metadata.xml");
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

        Map<String, String> result = realm.identityProviders().importFrom(form);
        assertSamlImport(result, SIGNING_CERT_1,true);

        // Create new SAML identity provider using configuration retrieved from import-config
        create(createRep("saml", "saml",true, result));

        IdentityProviderResource provider = realm.identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep,true);

        //add one mapper and test that there is exactly one mapper
        provider.addMapper(createMapper("saml"));
        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        Assert.assertEquals("IdentityProviderMappers instance count", 1, mappers.size());
        Assert.assertEquals("saml", mappers.get(0).getIdentityProviderAlias());
        Assert.assertEquals("saml-user-attribute-idp-mapper", mappers.get(0).getIdentityProviderMapper());

        // Now list the providers - we should see the one just created
        List<IdentityProviderRepresentation> providers = realm.identityProviders().findAll();
        Assert.assertNotNull("identityProviders not null", providers);
        Assert.assertEquals("identityProviders instance count", 1, providers.size());
        assertEqual(rep, providers.get(0));

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = realm.identityProviders().get("saml").export("xml");
        Assert.assertEquals(200, response.getStatus());
        body = response.readEntity(String.class);
        response.close();

        assertSamlExport(body);
    }

    @Test
    public void testSamlImportAndExportDisabled() throws URISyntaxException, IOException, ParsingException {

        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = getClass().getClassLoader().getResource("admin-test/saml-idp-metadata-disabled.xml");
        byte[] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata-disabled.xml");

        Map<String, String> result = realm.identityProviders().importFrom(form);
        assertSamlImport(result, SIGNING_CERT_1, false);

        // Create new SAML identity provider using configuration retrieved from import-config
        create(createRep("saml", "saml", false, result));

        IdentityProviderResource provider = realm.identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep, false);

    }


    @Test
    public void testSamlImportAndExportMultipleSigningKeys() throws Exception {
           
        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = getClass().getClassLoader().getResource("admin-test/saml-idp-metadata-two-signing-certs.xml");
        byte[] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata-two-signing-certs");

        Map<String, String> result = realm.identityProviders().importFrom(form);
        assertSamlImport(result, SIGNING_CERT_1 + "," + SIGNING_CERT_2, true);

        // Create new SAML identity provider using configuration retrieved from import-config
        create(createRep("saml", "saml", true, result));

        IdentityProviderResource provider = realm.identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep, true);

        // add one mapper and test that there is exactly one mapper
        provider.addMapper(createMapper("saml"));
        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        Assert.assertEquals("IdentityProviderMappers instance count", 1, mappers.size());
        Assert.assertEquals("saml", mappers.get(0).getIdentityProviderAlias());
        Assert.assertEquals("saml-user-attribute-idp-mapper", mappers.get(0).getIdentityProviderMapper());

        // Now list the providers - we should see the one just created
        List<IdentityProviderRepresentation> providers = realm.identityProviders().findAll();
        Assert.assertNotNull("identityProviders not null", providers);
        Assert.assertEquals("identityProviders instance count", 1, providers.size());
        assertEqual(rep, providers.get(0));

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = realm.identityProviders().get("saml").export("xml");
        Assert.assertEquals(200, response.getStatus());
        body = response.readEntity(String.class);
        response.close();

        assertSamlExport(body);
    }
    
    private void assertEqual(IdentityProviderRepresentation expected, IdentityProviderRepresentation actual) {
        //System.out.println("expected: " + expected);
        //System.out.println("actual: " + actual);
        Assert.assertNotNull("expected IdentityProviderRepresentation not null", expected);
        Assert.assertNotNull("actual IdentityProviderRepresentation not null", actual);
        Assert.assertEquals("internalId", expected.getInternalId(), actual.getInternalId());
        Assert.assertEquals("alias", expected.getAlias(), actual.getAlias());
        Assert.assertEquals("providerId", expected.getProviderId(), actual.getProviderId());
        Assert.assertEquals("enabled", expected.isEnabled(), actual.isEnabled());
        Assert.assertEquals("firstBrokerLoginFlowAlias", expected.getFirstBrokerLoginFlowAlias(), actual.getFirstBrokerLoginFlowAlias());
        Assert.assertEquals("config", expected.getConfig(), actual.getConfig());
    }

    private void assertCreatedSamlIdp(IdentityProviderRepresentation idp,boolean enabled) {
        //System.out.println("idp: " + idp);
        Assert.assertNotNull("IdentityProviderRepresentation not null", idp);
        Assert.assertNotNull("internalId", idp.getInternalId());
        Assert.assertEquals("alias", "saml", idp.getAlias());
        Assert.assertEquals("providerId", "saml", idp.getProviderId());
        Assert.assertEquals("enabled",enabled, idp.isEnabled());
        Assert.assertEquals("firstBrokerLoginFlowAlias", "first broker login",idp.getFirstBrokerLoginFlowAlias());
        assertSamlConfig(idp.getConfig());
    }

    private void assertSamlConfig(Map<String, String> config) {
        // import endpoint simply converts IDPSSODescriptor into key value pairs.
        // check that saml-idp-metadata.xml was properly converted into key value pairs
        //System.out.println(config.get("samlAttributes"));
        assertThat(config.keySet(), containsInAnyOrder(
          "validateSignature",
          "singleLogoutServiceUrl",
          "postBindingLogout",
          "postBindingResponse",
          "postBindingAuthnRequest",
          "singleSignOnServiceUrl",
          "wantAuthnRequestsSigned",
          "nameIDPolicyFormat",
          "signingCertificate",
          "addExtensionsElementWithKeyInfo",
          "loginHint",
          "hideOnLoginPage"
        ));
        assertThat(config, hasEntry("validateSignature", "true"));
        assertThat(config, hasEntry("singleLogoutServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("postBindingResponse", "true"));
        assertThat(config, hasEntry("postBindingAuthnRequest", "true"));
        assertThat(config, hasEntry("singleSignOnServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("wantAuthnRequestsSigned", "true"));
        assertThat(config, hasEntry("addExtensionsElementWithKeyInfo", "false"));
        assertThat(config, hasEntry("nameIDPolicyFormat", "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        assertThat(config, hasEntry("hideOnLoginPage", "true"));
        assertThat(config, hasEntry(is("signingCertificate"), notNullValue()));
    }

    private void assertSamlImport(Map<String, String> config, String expectedSigningCertificates,boolean enabled) {
        //firtsly check and remove enabledFromMetadata from config
        boolean enabledFromMetadata = Boolean.valueOf(config.get(SAMLIdentityProviderConfig.ENABLED_FROM_METADATA));
        config.remove(SAMLIdentityProviderConfig.ENABLED_FROM_METADATA);
        Assert.assertEquals(enabledFromMetadata,enabled);
        assertSamlConfig(config);
        assertThat(config, hasEntry("signingCertificate", expectedSigningCertificates));

    }

    private void assertSamlExport(String body) throws ParsingException, URISyntaxException {
        //System.out.println(body);
        
        Object entBody = SAMLParser.getInstance().parse(
                new ByteArrayInputStream(body.getBytes(Charset.forName("utf-8"))));

        Assert.assertEquals("Parsed export type", EntityDescriptorType.class, entBody.getClass());
        EntityDescriptorType entity = (EntityDescriptorType) entBody;

        Assert.assertEquals("EntityID", oauth.AUTH_SERVER_ROOT + "/realms/admin-client-test", entity.getEntityID());

        Assert.assertNotNull("Extensions not null", entity.getExtensions());
        Assert.assertNotNull("EntityAttributes not null", entity.getExtensions().getEntityAttributes());
        Assert.assertNotNull("Saml Attributes not null", entity.getExtensions().getEntityAttributes().getAttribute());
        Assert.assertEquals("Saml Attributes size equal to 1", 1,
            entity.getExtensions().getEntityAttributes().getAttribute().size());
        AttributeType attribute = entity.getExtensions().getEntityAttributes().getAttribute().get(0);
        Assert.assertEquals("http://macedir.org/entity-category", attribute.getName());
        Assert.assertNotNull("Attribute Value not null", attribute.getAttributeValue());
        Assert.assertEquals("Attribute Value size equal to 2", 2, attribute.getAttributeValue().size());
        assertThat(attribute.getAttributeValue(), containsInAnyOrder("http://refeds.org/category/research-and-scholarship",
            "http://www.geant.net/uri/dataprotection-code-of-conduct/v1"));
        Assert.assertEquals("http://www.surfconext.nl/",
            entity.getExtensions().getRegistrationInfo().getRegistrationAuthority().toString());
        Assert.assertNotNull("RegistrationInfo not null", entity.getExtensions().getRegistrationInfo());
        Assert.assertEquals("http://www.surfconext.nl/",
            entity.getExtensions().getRegistrationInfo().getRegistrationAuthority().toString());

        List<LocalizedURIType> registrationPolicies = entity.getExtensions().getRegistrationInfo().getRegistrationPolicy();
        Assert.assertNotNull("RegistrationPolicy not null", registrationPolicies);
        Assert.assertEquals("RegistrationPolicy size", 1, registrationPolicies.size());
        Assert.assertEquals("https://wiki.surfnet.nl/display/eduGAIN/EduGAIN",
            registrationPolicies.get(0).getValue().toString());

        Assert.assertNotNull("ChoiceType not null", entity.getChoiceType());
        Assert.assertEquals("ChoiceType.size", 1, entity.getChoiceType().size());

        List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = entity.getChoiceType().get(0).getDescriptors();
        Assert.assertNotNull("Descriptors not null", descriptors);
        Assert.assertEquals("Descriptors.size", 1, descriptors.size());

        SPSSODescriptorType desc = descriptors.get(0).getSpDescriptor();
        Assert.assertNotNull("SPSSODescriptor not null", desc);

        Assert.assertNotNull("Descriptors Extensions not null", desc.getExtensions());
        UIInfoType info = desc.getExtensions().getUIInfo();
        Assert.assertNotNull("Descriptors UIInfo not null", desc.getExtensions().getUIInfo());

        Assert.assertNotNull("DisplayName not null", info.getDisplayName());
        Assert.assertEquals("DisplayName size equal to 1", 1, info.getDisplayName().size());
        LocalizedNameType displayName = info.getDisplayName().get(0);
        Assert.assertEquals("DisplayName language  equal to en", LANG, displayName.getLang());
        Assert.assertEquals(REALM_NAME, displayName.getValue());

        Assert.assertNotNull("description not null", info.getDescription());
        Assert.assertEquals("description size equal to 1", 1, info.getDescription().size());
        LocalizedNameType description = info.getDescription().get(0);
        Assert.assertEquals("description language  equal to en", LANG, description.getLang());
        Assert.assertEquals("Keycloak test SP", description.getValue());

        Assert.assertNotNull("InformationURL not null", info.getInformationURL());
        Assert.assertEquals("InformationURL size equal to 1", 1, info.getInformationURL().size());
        LocalizedURIType information = info.getInformationURL().get(0);
        Assert.assertEquals("InformationURL language  equal to en", LANG, information.getLang());
        Assert.assertNotNull("InformationURL value not null", information.getValue());
        Assert.assertEquals("https://www.keycloak.org/documentation.html", information.getValue().toString());

        Assert.assertNotNull("privacyStatement not null", info.getPrivacyStatementURL());
        Assert.assertEquals("privacyStatement size equal to 1", 1, info.getPrivacyStatementURL().size());
        LocalizedURIType privacyStatement = info.getPrivacyStatementURL().get(0);
        Assert.assertEquals("privacyStatement language  equal to en", LANG, privacyStatement.getLang());
        Assert.assertNotNull("privacyStatement value not null", privacyStatement.getValue());
        Assert.assertEquals("https://www.keycloak.org/", privacyStatement.getValue().toString());

        List<LogoType> logos = info.getLogo();
        Assert.assertNotNull("Logo not null", logos);
        Assert.assertEquals("Logo size equal to 1", 1, logos.size());
        Assert.assertEquals("https://github.com/keycloak/keycloak-misc/blob/master/logo/keycloak_icon_256px.png",
            logos.get(0).getValue().toString());
        Assert.assertEquals(100, logos.get(0).getHeight());
        Assert.assertEquals(110, logos.get(0).getWidth());

        Assert.assertTrue("AuthnRequestsSigned", desc.isAuthnRequestsSigned());

        Set<String> expected = new HashSet<>(Arrays.asList(
                "urn:oasis:names:tc:SAML:2.0:protocol"));

        Set<String> actual = new HashSet<>(desc.getProtocolSupportEnumeration());

        Assert.assertEquals("ProtocolSupportEnumeration", expected, actual);

        Assert.assertNotNull("AssertionConsumerService not null", desc.getAssertionConsumerService());
        Assert.assertEquals("AssertionConsumerService.size", 1, desc.getAssertionConsumerService().size());

        IndexedEndpointType endpoint = desc.getAssertionConsumerService().get(0);

        Assert.assertEquals("AssertionConsumerService.Location",
                new URI(oauth.AUTH_SERVER_ROOT + "/realms/admin-client-test/broker/saml/endpoint"), endpoint.getLocation());
        Assert.assertEquals("AssertionConsumerService.Binding",
                new URI("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"), endpoint.getBinding());
        Assert.assertTrue("AssertionConsumerService.isDefault", endpoint.isIsDefault());


        Assert.assertNotNull("SingleLogoutService not null", desc.getSingleLogoutService());
        Assert.assertEquals("SingleLogoutService.size", 1, desc.getSingleLogoutService().size());

        EndpointType sloEndpoint = desc.getSingleLogoutService().get(0);

        Assert.assertEquals("SingleLogoutService.Location",
                new URI(oauth.AUTH_SERVER_ROOT + "/realms/admin-client-test/broker/saml/endpoint"), sloEndpoint.getLocation());
        Assert.assertEquals("SingleLogoutService.Binding",
                new URI("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"), sloEndpoint.getBinding());

        Assert.assertNotNull("AttributeConsumingService not null", desc.getAttributeConsumingService());
        Assert.assertEquals("AttributeConsumingService.size", 1, desc.getAttributeConsumingService().size());
        AttributeConsumingServiceType attributeConsuming = desc.getAttributeConsumingService().get(0);

        Assert.assertNotNull("ServiceName not null", attributeConsuming.getServiceName());
        Assert.assertEquals("ServiceName size equal to 1", 1, attributeConsuming.getServiceName().size());
        LocalizedNameType displayNameMd = attributeConsuming.getServiceName().get(0);
        Assert.assertEquals("ServiceName language  equal to en", LANG, displayNameMd.getLang());
        Assert.assertEquals(REALM_NAME, displayNameMd.getValue());

        Assert.assertNotNull("ServiceDescription not null", attributeConsuming.getServiceDescription());
        Assert.assertEquals("ServiceDescription size equal to 1", 1, attributeConsuming.getServiceDescription().size());
        LocalizedNameType descriptionMd = attributeConsuming.getServiceDescription().get(0);
        Assert.assertEquals("ServiceDescription language  equal to en", LANG, descriptionMd.getLang());
        Assert.assertEquals("Keycloak test SP", descriptionMd.getValue());

        Assert.assertNotNull("RequestedAttribute not null", attributeConsuming.getRequestedAttribute());
        Assert.assertEquals("RequestedAttribute size equal to 1", 1, attributeConsuming.getRequestedAttribute().size());
        RequestedAttributeType requestedAttribute = attributeConsuming.getRequestedAttribute().get(0);
        Assert.assertEquals("urn:oid:0.9.2342.19200300.100.1.3", requestedAttribute.getName());
        Assert.assertEquals("email", requestedAttribute.getFriendlyName());
        
        Assert.assertNotNull("KeyDescriptor not null", desc.getKeyDescriptor());
        Assert.assertEquals("KeyDescriptor.size", 1, desc.getKeyDescriptor().size());
        KeyDescriptorType keyDesc = desc.getKeyDescriptor().get(0);
        assertThat(keyDesc, notNullValue());
        assertThat(keyDesc.getUse(), equalTo(KeyTypes.SIGNING));
        NodeList cert = keyDesc.getKeyInfo().getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate");
        assertThat("KeyDescriptor.Signing.Cert existence", cert.getLength(), is(1));
        
        Assert.assertNotNull("Organization not null", entity.getOrganization());
        Assert.assertNotNull("Organization name not null", entity.getOrganization().getOrganizationName());
        Assert.assertEquals("Organization name size equal to 1", 1, entity.getOrganization().getOrganizationName().size());
        LocalizedNameType organizationName = entity.getOrganization().getOrganizationName().get(0);
        Assert.assertEquals("Organization name language  equal to en", LANG, organizationName.getLang());
        Assert.assertEquals("Keycloak", organizationName.getValue());
        Assert.assertNotNull("Organization display name not null", entity.getOrganization().getOrganizationDisplayName());
        Assert.assertEquals("Organization display name size equal to 1", 1,
            entity.getOrganization().getOrganizationDisplayName().size());
        LocalizedNameType organizationDisplayName = entity.getOrganization().getOrganizationDisplayName().get(0);
        Assert.assertEquals("Organization display name language  equal to en", LANG, organizationDisplayName.getLang());
        Assert.assertEquals("Keycloak organization", organizationDisplayName.getValue());
        Assert.assertNotNull("Organization URL not null", entity.getOrganization().getOrganizationURL());
        Assert.assertEquals("Organization URL size equal to 1", 1, entity.getOrganization().getOrganizationURL().size());
        LocalizedURIType organizationURL = entity.getOrganization().getOrganizationURL().get(0);
        Assert.assertEquals("Organization URL language  equal to en", LANG, organizationURL.getLang());
        Assert.assertNotNull("Organization URL value not null", organizationURL.getValue());
        Assert.assertEquals("https://www.keycloak.org/", organizationURL.getValue().toString());

        Assert.assertNotNull("Contact person not null", entity.getContactPerson());
        Assert.assertEquals("Contact person size equal to 1", 1, entity.getContactPerson().size());
        ContactType contact = entity.getContactPerson().get(0);
        Assert.assertEquals(ContactTypeType.SUPPORT, contact.getContactType());
        Assert.assertEquals("Bob", contact.getGivenName());
        Assert.assertEquals("Sub", contact.getSurName());
        Assert.assertEquals("Keycloak", contact.getCompany());
        Assert.assertNotNull("Contact person email not null", contact.getEmailAddress());
        Assert.assertEquals("Contact person email size equal to 1", 1, contact.getEmailAddress().size());
        Assert.assertEquals("support@keycloak.com", contact.getEmailAddress().get(0));
        Assert.assertNotNull("Contact person telephone number not null", contact.getTelephoneNumber());
        Assert.assertEquals("Contact person telephone number size equal to 2", 2, contact.getTelephoneNumber().size());
        assertThat(contact.getTelephoneNumber(), containsInAnyOrder("2112113334", "2112113335"));
        
    }
    
    private IdentityProviderMapperRepresentation createMapper(String idpAlias) {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias(idpAlias);
        mapper.setIdentityProviderMapper("saml-user-attribute-idp-mapper");
        mapper.setName("mapper");
        Map<String,String> config = new HashMap<>();
        config.put("attribute.name", "urn:oid:0.9.2342.19200300.100.1.3");
        config.put("attribute.friendly.name", "email");
        mapper.setConfig(config);

        return mapper;
    }

}
