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
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.w3c.dom.NodeList;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.crypto.dsig.XMLSignature;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderTest extends AbstractAdminTest {

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
    public void testFindAll() {
        create(createRep("google", "google"));

        create(createRep("facebook", "facebook"));

        Assert.assertNames(realm.identityProviders().findAll(), "google", "facebook");
    }

    @Test
    public void testCreate() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = realm.identityProviders().get("new-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertNotNull(representation.getInternalId());
        assertEquals("new-identity-provider", representation.getAlias());
        assertEquals("oidc", representation.getProviderId());
        assertEquals("clientId", representation.getConfig().get("clientId"));
        assertEquals(ComponentRepresentation.SECRET_VALUE, representation.getConfig().get("clientSecret"));
        assertTrue(representation.isEnabled());
        assertFalse(representation.isStoreToken());
        assertFalse(representation.isTrustEmail());

        assertEquals("some secret value", testingClient.testing("admin-client-test").getIdentityProviderConfig("new-identity-provider").get("clientSecret"));

        IdentityProviderRepresentation rep = realm.identityProviders().findAll().stream().filter(i -> i.getAlias().equals("new-identity-provider")).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, rep.getConfig().get("clientSecret"));
    }


    @Test
    public void testCreateWithBasicAuth() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");
        newIdentityProvider.getConfig().put("clientAuthMethod",OIDCLoginProtocol.CLIENT_SECRET_BASIC);

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = realm.identityProviders().get("new-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertNotNull(representation.getInternalId());
        assertEquals("new-identity-provider", representation.getAlias());
        assertEquals("oidc", representation.getProviderId());
        assertEquals("clientId", representation.getConfig().get("clientId"));
        assertEquals(ComponentRepresentation.SECRET_VALUE, representation.getConfig().get("clientSecret"));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, representation.getConfig().get("clientAuthMethod"));

        assertTrue(representation.isEnabled());
        assertFalse(representation.isStoreToken());
        assertFalse(representation.isTrustEmail());

        assertEquals("some secret value", testingClient.testing("admin-client-test").getIdentityProviderConfig("new-identity-provider").get("clientSecret"));

        IdentityProviderRepresentation rep = realm.identityProviders().findAll().stream().filter(i -> i.getAlias().equals("new-identity-provider")).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, rep.getConfig().get("clientSecret"));
    }

    @Test
    public void testCreateWithJWT() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = realm.identityProviders().get("new-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertNotNull(representation.getInternalId());
        assertEquals("new-identity-provider", representation.getAlias());
        assertEquals("oidc", representation.getProviderId());
        assertEquals("clientId", representation.getConfig().get("clientId"));
        assertNull(representation.getConfig().get("clientSecret"));
        assertEquals(OIDCLoginProtocol.PRIVATE_KEY_JWT, representation.getConfig().get("clientAuthMethod"));
        assertTrue(representation.isEnabled());
        assertFalse(representation.isStoreToken());
        assertFalse(representation.isTrustEmail());
    }

    @Test
    public void testUpdate() {
        IdentityProviderRepresentation newIdentityProvider = createRep("update-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");

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
        AdminEventRepresentation event = assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.identityProviderPath("update-identity-provider"), representation, ResourceType.IDENTITY_PROVIDER);
        assertFalse(event.getRepresentation().contains("some secret value"));
        assertTrue(event.getRepresentation().contains(ComponentRepresentation.SECRET_VALUE));

        identityProviderResource = realm.identityProviders().get(representation.getInternalId());

        assertNotNull(identityProviderResource);

        representation = identityProviderResource.toRepresentation();

        assertFalse(representation.isEnabled());
        assertTrue(representation.isStoreToken());
        assertEquals("changedClientId", representation.getConfig().get("clientId"));

        assertEquals("some secret value", testingClient.testing("admin-client-test").getIdentityProviderConfig("changed-alias").get("clientSecret"));

        representation.getConfig().put("clientSecret", "${vault.key}");
        identityProviderResource.update(representation);
        event = assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.identityProviderPath(representation.getInternalId()), representation, ResourceType.IDENTITY_PROVIDER);
        assertThat(event.getRepresentation(), containsString("${vault.key}"));
        assertThat(event.getRepresentation(), not(containsString(ComponentRepresentation.SECRET_VALUE)));

        assertThat(identityProviderResource.toRepresentation().getConfig(), hasEntry("clientSecret", "${vault.key}"));
        assertEquals("${vault.key}", testingClient.testing("admin-client-test").getIdentityProviderConfig("changed-alias").get("clientSecret"));
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

        getCleanup().addIdentityProviderAlias(idpRep.getAlias());

        String secret = idpRep.getConfig() != null ? idpRep.getConfig().get("clientSecret") : null;
        idpRep = StripSecretsUtils.strip(idpRep);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProviderPath(idpRep.getAlias()), idpRep, ResourceType.IDENTITY_PROVIDER);

        if (secret != null) {
            idpRep.getConfig().put("clientSecret", secret);
        }
    }

    private IdentityProviderRepresentation createRep(String id, String providerId) {
        return createRep(id, providerId, null);
    }

    private IdentityProviderRepresentation createRep(String id, String providerId, Map<String, String> config) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();

        idp.setAlias(id);
        idp.setDisplayName(id);
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
        assertMapperTypes(mapperTypes, "google-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("facebook", "facebook"));
        provider = realm.identityProviders().get("facebook");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "facebook-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("github", "github"));
        provider = realm.identityProviders().get("github");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "github-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("twitter", "twitter"));
        provider = realm.identityProviders().get("twitter");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "oidc-username-idp-mapper");

        create(createRep("linkedin", "linkedin"));
        provider = realm.identityProviders().get("linkedin");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "linkedin-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("microsoft", "microsoft"));
        provider = realm.identityProviders().get("microsoft");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "microsoft-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("stackoverflow", "stackoverflow"));
        provider = realm.identityProviders().get("stackoverflow");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "stackoverflow-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("keycloak-oidc", "keycloak-oidc"));
        provider = realm.identityProviders().get("keycloak-oidc");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "keycloak-oidc-role-to-role-idp-mapper", "oidc-user-attribute-idp-mapper", "oidc-role-idp-mapper", "oidc-username-idp-mapper", "oidc-advanced-role-idp-mapper");

        create(createRep("oidc", "oidc"));
        provider = realm.identityProviders().get("oidc");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "oidc-user-attribute-idp-mapper", "oidc-role-idp-mapper", "oidc-username-idp-mapper", "oidc-advanced-role-idp-mapper");

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
        assertSamlImport(result, SIGNING_CERT_1);

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
    public void testSamlImportAndExportMultipleSigningKeys() throws URISyntaxException, IOException, ParsingException {

        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = getClass().getClassLoader().getResource("admin-test/saml-idp-metadata-two-signing-certs.xml");
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata-two-signing-certs");

        Map<String, String> result = realm.identityProviders().importFrom(form);
        assertSamlImport(result, SIGNING_CERT_1 + "," + SIGNING_CERT_2);

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

        // add duplicate mapper
        Response error = provider.addMapper(mapper);
        Assert.assertEquals("mapper unique name", 400, error.getStatus());
        error.close();

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

    // KEYCLOAK-4962
    @Test
    public void testUpdateProtocolMappers() {
        create(createRep("google2", "google"));

        IdentityProviderResource provider = realm.identityProviders().get("google2");

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias("google2");
        mapper.setName("my_mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("role", "");
        mapper.setConfig(config);

        Response response = provider.addMapper(mapper);
        String mapperId = ApiUtil.getCreatedId(response);


        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        assertEquals(1, mappers.size());
        assertEquals(0, mappers.get(0).getConfig().size());

        mapper = provider.getMapperById(mapperId);
        mapper.getConfig().put("role", "offline_access");

        provider.update(mapperId, mapper);

        mappers = provider.getMappers();
        assertEquals(1, mappers.size());
        assertEquals(1, mappers.get(0).getConfig().size());
        assertEquals("offline_access", mappers.get(0).getConfig().get("role"));
    }

    // KEYCLOAK-7872
    @Test
    public void testDeleteProtocolMappersAfterDeleteIdentityProvider() {
        create(createRep("google3", "google"));

        IdentityProviderResource provider = realm.identityProviders().get("google3");

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias("google3");
        mapper.setName("my_mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("role", "offline_access");
        mapper.setConfig(config);

        Response response = provider.addMapper(mapper);

        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        assertThat(mappers, hasSize(1));

        assertAdminEvents.clear();

        provider.remove();
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.identityProviderPath("google3"), ResourceType.IDENTITY_PROVIDER);

        create(createRep("google3", "google"));

        IdentityProviderResource newProvider = realm.identityProviders().get("google3");

        assertThat(newProvider.getMappers(), empty());
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
        assertThat(config.keySet(), containsInAnyOrder(
          "validateSignature",
          "singleLogoutServiceUrl",
          "postBindingLogout",
          "postBindingResponse",
          "postBindingAuthnRequest",
          "singleSignOnServiceUrl",
          "wantAuthnRequestsSigned",
          "signingCertificate",
          "addExtensionsElementWithKeyInfo"
        ));
        assertThat(config, hasEntry("validateSignature", "true"));
        assertThat(config, hasEntry("singleLogoutServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("postBindingResponse", "true"));
        assertThat(config, hasEntry("postBindingAuthnRequest", "true"));
        assertThat(config, hasEntry("singleSignOnServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("wantAuthnRequestsSigned", "true"));
        assertThat(config, hasEntry("addExtensionsElementWithKeyInfo", "false"));
        assertThat(config, hasEntry(is("signingCertificate"), notNullValue()));
    }

    private void assertSamlImport(Map<String, String> config, String expectedSigningCertificates) {
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

        Assert.assertNotNull("ChoiceType not null", entity.getChoiceType());
        Assert.assertEquals("ChoiceType.size", 1, entity.getChoiceType().size());

        List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = entity.getChoiceType().get(0).getDescriptors();
        Assert.assertNotNull("Descriptors not null", descriptors);
        Assert.assertEquals("Descriptors.size", 1, descriptors.size());

        SPSSODescriptorType desc = descriptors.get(0).getSpDescriptor();
        Assert.assertNotNull("SPSSODescriptor not null", desc);

        Assert.assertTrue("AuthnRequestsSigned", desc.isAuthnRequestsSigned());

        Set<String> expected = new HashSet<>(Arrays.asList(
                "urn:oasis:names:tc:SAML:2.0:protocol",
                "urn:oasis:names:tc:SAML:1.1:protocol",
                "http://schemas.xmlsoap.org/ws/2003/07/secext"));

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

        Assert.assertNotNull("KeyDescriptor not null", desc.getKeyDescriptor());
        Assert.assertEquals("KeyDescriptor.size", 1, desc.getKeyDescriptor().size());
        KeyDescriptorType keyDesc = desc.getKeyDescriptor().get(0);
        assertThat(keyDesc, notNullValue());
        assertThat(keyDesc.getUse(), equalTo(KeyTypes.SIGNING));
        NodeList cert = keyDesc.getKeyInfo().getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate");
        assertThat("KeyDescriptor.Signing.Cert existence", cert.getLength(), is(1));
    }

    private void assertProviderInfo(Map<String, String> info, String id, String name) {
        System.out.println(info);
        Assert.assertEquals("id", id, info.get("id"));
        Assert.assertEquals("name", name, info.get("name"));
    }
}
