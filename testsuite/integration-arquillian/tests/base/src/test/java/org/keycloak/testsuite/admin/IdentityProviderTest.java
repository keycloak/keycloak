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

package org.keycloak.testsuite.admin;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLEntityDescriptorParser;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminEventPaths;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderTest extends AbstractAdminTest {

    @Test
    public void testFindAll() {
        create(createRep("google", "google"));

        create(createRep("facebook", "facebook"));

        Assert.assertNames(realm.identityProviders().findAll(), "google", "facebook");
    }

    @Test
    public void testCreate() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "clientSecret");

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = realm.identityProviders().get("new-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertNotNull(representation.getInternalId());
        assertEquals("new-identity-provider", representation.getAlias());
        assertEquals("oidc", representation.getProviderId());
        assertEquals("clientId", representation.getConfig().get("clientId"));
        assertEquals("clientSecret", representation.getConfig().get("clientSecret"));
        assertTrue(representation.isEnabled());
        assertFalse(representation.isStoreToken());
        assertFalse(representation.isTrustEmail());
    }

    @Test
    public void testUpdate() {
        IdentityProviderRepresentation newIdentityProvider = createRep("update-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "clientSecret");

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = realm.identityProviders().get("update-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertEquals("update-identity-provider", representation.getAlias());

        representation.setAlias("changed-alias");
        representation.setEnabled(false);
        representation.setStoreToken(true);
        representation.getConfig().put("clientId", "changedClientId");

        identityProviderResource.update(representation);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.identityProviderPath("update-identity-provider"), representation, ResourceType.IDENTITY_PROVIDER);

        identityProviderResource = realm.identityProviders().get(representation.getInternalId());

        assertNotNull(identityProviderResource);

        representation = identityProviderResource.toRepresentation();

        assertFalse(representation.isEnabled());
        assertTrue(representation.isStoreToken());
        assertEquals("changedClientId", representation.getConfig().get("clientId"));
    }

    @Test
    public void testRemove() {
        IdentityProviderRepresentation newIdentityProvider = createRep("remove-identity-provider", "saml");

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = realm.identityProviders().get("remove-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        identityProviderResource.remove();
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.identityProviderPath("remove-identity-provider"), ResourceType.IDENTITY_PROVIDER);

        try {
            realm.identityProviders().get("remove-identity-provider").toRepresentation();
            Assert.fail("Not expected to found");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    private void create(IdentityProviderRepresentation idpRep) {
        Response response = realm.identityProviders().create(idpRep);
        Assert.assertNotNull(ApiUtil.getCreatedId(response));
        response.close();

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProviderPath(idpRep.getAlias()), idpRep, ResourceType.IDENTITY_PROVIDER);
    }

    private IdentityProviderRepresentation createRep(String id, String providerId) {
        return createRep(id, providerId, null);
    }

    private IdentityProviderRepresentation createRep(String id, String providerId, Map<String, String> config) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();

        idp.setAlias(id);
        idp.setProviderId(providerId);
        idp.setEnabled(true);
        if (config != null) {
            idp.setConfig(config);
        }
        return idp;
    }

    @Test
    public void testMapperTypes() {

        IdentityProviderResource provider;
        Map<String, IdentityProviderMapperTypeRepresentation> mapperTypes;

        create(createRep("google", "google"));
        provider = realm.identityProviders().get("google");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "google-user-attribute-mapper");

        create(createRep("facebook", "facebook"));
        provider = realm.identityProviders().get("facebook");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "facebook-user-attribute-mapper");

        create(createRep("github", "github"));
        provider = realm.identityProviders().get("github");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "github-user-attribute-mapper");

        create(createRep("twitter", "twitter"));
        provider = realm.identityProviders().get("twitter");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes);

        create(createRep("linkedin", "linkedin"));
        provider = realm.identityProviders().get("linkedin");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "linkedin-user-attribute-mapper");

        create(createRep("microsoft", "microsoft"));
        provider = realm.identityProviders().get("microsoft");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "microsoft-user-attribute-mapper");

        create(createRep("stackoverflow", "stackoverflow"));
        provider = realm.identityProviders().get("stackoverflow");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "stackoverflow-user-attribute-mapper");

        create(createRep("keycloak-oidc", "keycloak-oidc"));
        provider = realm.identityProviders().get("keycloak-oidc");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "keycloak-oidc-role-to-role-idp-mapper", "oidc-user-attribute-idp-mapper", "oidc-role-idp-mapper", "oidc-username-idp-mapper");

        create(createRep("oidc", "oidc"));
        provider = realm.identityProviders().get("oidc");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "oidc-user-attribute-idp-mapper", "oidc-role-idp-mapper", "oidc-username-idp-mapper");

        create(createRep("saml", "saml"));
        provider = realm.identityProviders().get("saml");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "saml-user-attribute-idp-mapper", "saml-role-idp-mapper", "saml-username-idp-mapper");
    }

    private void assertMapperTypes(Map<String, IdentityProviderMapperTypeRepresentation> mapperTypes, String ... mapperIds) {
        Set<String> expected = new HashSet<>();
        expected.add("hardcoded-user-session-attribute-idp-mapper");
        expected.add("oidc-hardcoded-role-idp-mapper");
        expected.add("hardcoded-attribute-idp-mapper");
        for (String id: mapperIds) {
            expected.add(id);
        }
        Assert.assertEquals("mapperTypes", expected, mapperTypes.keySet());
    }

    @Test
    public void testNoExport() {
        create(createRep("keycloak-oidc", "keycloak-oidc"));

        Response response = realm.identityProviders().get("keycloak-oidc").export("json");
        Assert.assertEquals("status", 204, response.getStatus());
        String body = response.readEntity(String.class);
        Assert.assertNull("body", body);
        response.close();
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
        assertSamlImport(result);

        // Create new SAML identity provider using configuration retrieved from import-config
        create(createRep("saml", "saml", result));

        IdentityProviderResource provider = realm.identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep);

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
    public void testMappers() {
        create(createRep("google", "google"));

        IdentityProviderResource provider = realm.identityProviders().get("google");

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias("google");
        mapper.setName("my_mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("role", "offline_access");
        mapper.setConfig(config);

        // createRep and add mapper
        Response response = provider.addMapper(mapper);
        String id = ApiUtil.getCreatedId(response);
        Assert.assertNotNull(id);
        response.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProviderMapperPath("google", id), mapper, ResourceType.IDENTITY_PROVIDER_MAPPER);

        // list mappers
        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        Assert.assertEquals("mappers count", 1, mappers.size());
        Assert.assertEquals("newly created mapper id", id, mappers.get(0).getId());

        // get mapper
        mapper = provider.getMapperById(id);
        Assert.assertNotNull("mapperById not null", mapper);
        Assert.assertEquals("mapper id", id, mapper.getId());
        Assert.assertNotNull("mapper.config exists", mapper.getConfig());
        Assert.assertEquals("config retained", "offline_access", mapper.getConfig().get("role"));

        // update mapper
        mapper.getConfig().put("role", "master-realm.manage-realm");
        provider.update(id, mapper);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.identityProviderMapperPath("google", id), mapper, ResourceType.IDENTITY_PROVIDER_MAPPER);

        mapper = provider.getMapperById(id);
        Assert.assertNotNull("mapperById not null", mapper);
        Assert.assertEquals("config changed", "master-realm.manage-realm", mapper.getConfig().get("role"));

        // delete mapper
        provider.delete(id);
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.identityProviderMapperPath("google", id), ResourceType.IDENTITY_PROVIDER_MAPPER);
        try {
            provider.getMapperById(id);
            Assert.fail("Should fail with NotFoundException");
        } catch (NotFoundException e) {
            // Expected
        }
    }

    @Test
    public void testInstalledIdentityProviders() {
        Response response = realm.identityProviders().getIdentityProviders("oidc");
        Assert.assertEquals("Status", 200, response.getStatus());
        Map<String, String> body = response.readEntity(Map.class);
        assertProviderInfo(body, "oidc", "OpenID Connect v1.0");

        response = realm.identityProviders().getIdentityProviders("keycloak-oidc");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "keycloak-oidc", "Keycloak OpenID Connect");

        response = realm.identityProviders().getIdentityProviders("saml");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "saml", "SAML v2.0");

        response = realm.identityProviders().getIdentityProviders("google");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "google", "Google");

        response = realm.identityProviders().getIdentityProviders("facebook");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "facebook", "Facebook");

        response = realm.identityProviders().getIdentityProviders("github");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "github", "GitHub");

        response = realm.identityProviders().getIdentityProviders("twitter");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "twitter", "Twitter");

        response = realm.identityProviders().getIdentityProviders("linkedin");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "linkedin", "LinkedIn");

        response = realm.identityProviders().getIdentityProviders("microsoft");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "microsoft", "Microsoft");

        response = realm.identityProviders().getIdentityProviders("stackoverflow");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "stackoverflow", "StackOverflow");

        response = realm.identityProviders().getIdentityProviders("nonexistent");
        Assert.assertEquals("Status", 400, response.getStatus());
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

    private void assertCreatedSamlIdp(IdentityProviderRepresentation idp) {
        //System.out.println("idp: " + idp);
        Assert.assertNotNull("IdentityProviderRepresentation not null", idp);
        Assert.assertNotNull("internalId", idp.getInternalId());
        Assert.assertEquals("alias", "saml", idp.getAlias());
        Assert.assertEquals("providerId", "saml", idp.getProviderId());
        Assert.assertTrue("enabled", idp.isEnabled());
        Assert.assertEquals("firstBrokerLoginFlowAlias", "first broker login",idp.getFirstBrokerLoginFlowAlias());
        assertSamlConfig(idp.getConfig());
    }

    private void assertSamlConfig(Map<String, String> config) {
        // import endpoint simply converts IDPSSODescriptor into key value pairs.
        // check that saml-idp-metadata.xml was properly converted into key value pairs
        //System.out.println(config);
        Assert.assertEquals("Config size", 7, config.size());
        Assert.assertEquals("validateSignature", "true", config.get("validateSignature"));
        Assert.assertEquals("singleLogoutServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml", config.get("singleLogoutServiceUrl"));
        Assert.assertEquals("postBindingResponse", "true", config.get("postBindingResponse"));
        Assert.assertEquals("postBindingAuthnRequest", "true", config.get("postBindingAuthnRequest"));
        Assert.assertEquals("singleSignOnServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml", config.get("singleSignOnServiceUrl"));
        Assert.assertEquals("wantAuthnRequestsSigned", "true", config.get("wantAuthnRequestsSigned"));
        Assert.assertNotNull("signingCertificate not null", config.get("signingCertificate"));
    }

    private void assertSamlImport(Map<String, String> config) {
        assertSamlConfig(config);
    }

    private void assertSamlExport(String body) throws ParsingException, URISyntaxException {
        //System.out.println(body);

        Object entBody = new SAMLEntityDescriptorParser().parse(StaxParserUtil.getXMLEventReader(
                new ByteArrayInputStream(body.getBytes(Charset.forName("utf-8")))));

        Assert.assertEquals("Parsed export type", EntityDescriptorType.class, entBody.getClass());
        EntityDescriptorType entity = (EntityDescriptorType) entBody;

        Assert.assertEquals("EntityID", "http://localhost:8180/auth/realms/admin-client-test", entity.getEntityID());

        Assert.assertNotNull("ChoiceType not null", entity.getChoiceType());
        Assert.assertEquals("ChoiceType.size", 1, entity.getChoiceType().size());

        List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = entity.getChoiceType().get(0).getDescriptors();
        Assert.assertNotNull("Descriptors not null", descriptors);
        Assert.assertEquals("Descriptors.size", 1, descriptors.size());

        SPSSODescriptorType desc = descriptors.get(0).getSpDescriptor();
        Assert.assertNotNull("SPSSODescriptor not null", desc);

        Assert.assertTrue("AuthnRequestsSigned", desc.isAuthnRequestsSigned());

        Set<String> expected = new HashSet(Arrays.asList(
                "urn:oasis:names:tc:SAML:2.0:protocol",
                "urn:oasis:names:tc:SAML:1.1:protocol",
                "http://schemas.xmlsoap.org/ws/2003/07/secext"));

        Set<String> actual = new HashSet(desc.getProtocolSupportEnumeration());

        Assert.assertEquals("ProtocolSupportEnumeration", expected, actual);

        Assert.assertNotNull("AssertionConsumerService not null", desc.getAssertionConsumerService());
        Assert.assertEquals("AssertionConsumerService.size", 1, desc.getAssertionConsumerService().size());

        IndexedEndpointType endpoint = desc.getAssertionConsumerService().get(0);

        Assert.assertEquals("AssertionConsumerService.Location",
                new URI("http://localhost:8180/auth/realms/admin-client-test/broker/saml/endpoint"), endpoint.getLocation());
        Assert.assertEquals("AssertionConsumerService.Binding",
                new URI("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"), endpoint.getBinding());
        Assert.assertTrue("AssertionConsumerService.isDefault", endpoint.isIsDefault());


        Assert.assertNotNull("SingleLogoutService not null", desc.getSingleLogoutService());
        Assert.assertEquals("SingleLogoutService.size", 1, desc.getSingleLogoutService().size());

        EndpointType sloEndpoint = desc.getSingleLogoutService().get(0);

        Assert.assertEquals("SingleLogoutService.Location",
                new URI("http://localhost:8180/auth/realms/admin-client-test/broker/saml/endpoint"), sloEndpoint.getLocation());
        Assert.assertEquals("SingleLogoutService.Binding",
                new URI("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"), sloEndpoint.getBinding());

        Assert.assertNotNull("KeyDescriptor not null", desc.getKeyDescriptor());
        Assert.assertEquals("KeyDescriptor.size", 1, desc.getKeyDescriptor().size());
        Assert.assertEquals("KeyDescriptor.Use", KeyTypes.SIGNING, desc.getKeyDescriptor().get(0).getUse());
    }

    private void assertProviderInfo(Map<String, String> info, String id, String name) {
        System.out.println(info);
        Assert.assertEquals("id", id, info.get("id"));
        Assert.assertEquals("name", name, info.get("name"));
    }
}
