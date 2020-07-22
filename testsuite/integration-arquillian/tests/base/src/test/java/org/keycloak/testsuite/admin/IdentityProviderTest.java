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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.XMLDSIG_NSURI;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.broker.OIDCIdentityProviderConfigRep;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


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
    public void testCreateWithReservedCharacterForAlias() {
        IdentityProviderRepresentation newIdentityProvider = createRep("ne$&w-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");

        Response response = realm.identityProviders().create(newIdentityProvider);
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreate() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
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
        assertEquals("IMPORT", representation.getConfig().get(IdentityProviderMapperModel.SYNC_MODE));
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
    @AuthServerContainerExclude(REMOTE)
    public void failCreateInvalidUrl() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(realmsResouce().realm("test"))
                .updateWith(r -> r.setSslRequired(SslRequired.ALL.name()))
                .update()
        ) {
            IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

            newIdentityProvider.getConfig().put("clientId", "clientId");
            newIdentityProvider.getConfig().put("clientSecret", "some secret value");

            OIDCIdentityProviderConfigRep oidcConfig = new OIDCIdentityProviderConfigRep(newIdentityProvider);

            oidcConfig.setAuthorizationUrl("invalid://test");

            try (Response response = this.realm.identityProviders().create(newIdentityProvider)) {
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
                assertEquals("The url [authorization_url] is malformed", error.getErrorMessage());
            }

            oidcConfig.setAuthorizationUrl(null);
            oidcConfig.setTokenUrl("http://test");

            try (Response response = this.realm.identityProviders().create(newIdentityProvider)) {
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
                assertEquals("The url [token_url] requires secure connections", error.getErrorMessage());
            }

            oidcConfig.setAuthorizationUrl(null);
            oidcConfig.setTokenUrl(null);
            oidcConfig.setJwksUrl("http://test");

            try (Response response = this.realm.identityProviders().create(newIdentityProvider)) {
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
                assertEquals("The url [jwks_url] requires secure connections", error.getErrorMessage());
            }

            oidcConfig.setAuthorizationUrl(null);
            oidcConfig.setTokenUrl(null);
            oidcConfig.setJwksUrl(null);
            oidcConfig.setLogoutUrl("http://test");

            try (Response response = this.realm.identityProviders().create(newIdentityProvider)) {
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
                assertEquals("The url [logout_url] requires secure connections", error.getErrorMessage());
            }

            oidcConfig.setAuthorizationUrl(null);
            oidcConfig.setTokenUrl(null);
            oidcConfig.setJwksUrl(null);
            oidcConfig.setLogoutUrl(null);
            oidcConfig.setUserInfoUrl("http://test");

            try (Response response = this.realm.identityProviders().create(newIdentityProvider)) {
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
                assertEquals("The url [userinfo_url] requires secure connections", error.getErrorMessage());
            }
        }
    }

    @Test
    public void testCreateWithBasicAuth() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
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
        assertEquals("IMPORT", representation.getConfig().get(IdentityProviderMapperModel.SYNC_MODE));
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

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
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
        assertEquals("IMPORT", representation.getConfig().get(IdentityProviderMapperModel.SYNC_MODE));
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

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
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
    public void failUpdateInvalidUrl() throws Exception {
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(realm)
                .updateWith(r -> r.setSslRequired(SslRequired.ALL.name()))
                .update()
        ) {
            IdentityProviderRepresentation representation = createRep(UUID.randomUUID().toString(), "oidc");

            representation.getConfig().put("clientId", "clientId");
            representation.getConfig().put("clientSecret", "some secret value");

            try (Response response = realm.identityProviders().create(representation)) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            }

            IdentityProviderResource resource = this.realm.identityProviders().get(representation.getAlias());
            representation = resource.toRepresentation();

            OIDCIdentityProviderConfigRep oidcConfig = new OIDCIdentityProviderConfigRep(representation);

            oidcConfig.setAuthorizationUrl("invalid://test");
            try {
                resource.update(representation);
                fail("Invalid URL");
            } catch (Exception e) {
                assertTrue(e instanceof  ClientErrorException);
                Response response = ClientErrorException.class.cast(e).getResponse();
                assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = ((ClientErrorException) e).getResponse().readEntity(ErrorRepresentation.class);
                assertEquals("The url [authorization_url] is malformed", error.getErrorMessage());
            }

            oidcConfig.setAuthorizationUrl(null);
            oidcConfig.setTokenUrl("http://test");

            try {
                resource.update(representation);
                fail("Invalid URL");
            } catch (Exception e) {
                assertTrue(e instanceof  ClientErrorException);
                Response response = ClientErrorException.class.cast(e).getResponse();
                assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = ((ClientErrorException) e).getResponse().readEntity(ErrorRepresentation.class);
                assertEquals("The url [token_url] requires secure connections", error.getErrorMessage());
            }

            oidcConfig.setAuthorizationUrl(null);
            oidcConfig.setTokenUrl(null);
            oidcConfig.setJwksUrl("http://test");
            try {
                resource.update(representation);
                fail("Invalid URL");
            } catch (Exception e) {
                assertTrue(e instanceof  ClientErrorException);
                Response response = ClientErrorException.class.cast(e).getResponse();
                assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = ((ClientErrorException) e).getResponse().readEntity(ErrorRepresentation.class);
                assertEquals("The url [jwks_url] requires secure connections", error.getErrorMessage());
            }

            oidcConfig.setAuthorizationUrl(null);
            oidcConfig.setTokenUrl(null);
            oidcConfig.setJwksUrl(null);
            oidcConfig.setLogoutUrl("http://test");
            try {
                resource.update(representation);
                fail("Invalid URL");
            } catch (Exception e) {
                assertTrue(e instanceof  ClientErrorException);
                Response response = ClientErrorException.class.cast(e).getResponse();
                assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = ((ClientErrorException) e).getResponse().readEntity(ErrorRepresentation.class);
                assertEquals("The url [logout_url] requires secure connections", error.getErrorMessage());
            }

            oidcConfig.setAuthorizationUrl(null);
            oidcConfig.setTokenUrl(null);
            oidcConfig.setJwksUrl(null);
            oidcConfig.setLogoutUrl(null);
            oidcConfig.setUserInfoUrl("http://localhost");

            try {
                resource.update(representation);
                fail("Invalid URL");
            } catch (Exception e) {
                assertTrue(e instanceof  ClientErrorException);
                Response response = ClientErrorException.class.cast(e).getResponse();
                assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
                ErrorRepresentation error = ((ClientErrorException) e).getResponse().readEntity(ErrorRepresentation.class);
                assertEquals("The url [userinfo_url] requires secure connections", error.getErrorMessage());
            }

            rau.updateWith(r -> r.setSslRequired(SslRequired.EXTERNAL.name())).update();
            resource.update(representation);
        }
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

    protected void create(IdentityProviderRepresentation idpRep) {
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

    protected IdentityProviderRepresentation createRep(String id, String providerId) {
        return createRep(id, providerId,true, null);
    }

    protected IdentityProviderRepresentation createRep(String id, String providerId,boolean enabled, Map<String, String> config) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();

        idp.setAlias(id);
        idp.setDisplayName(id);
        idp.setProviderId(providerId);
        idp.setEnabled(enabled);
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
        assertMapperTypes(mapperTypes, "saml-user-attribute-idp-mapper", "saml-role-idp-mapper", "saml-username-idp-mapper", "saml-advanced-role-idp-mapper");
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
    public void testMappers() {
        create(createRep("google", "google"));

        IdentityProviderResource provider = realm.identityProviders().get("google");

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias("google");
        mapper.setName("my_mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("role", "offline_access");
        config.put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString());
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
        Assert.assertEquals("INHERIT", mappers.get(0).getConfig().get(IdentityProviderMapperModel.SYNC_MODE));
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
        config.put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString());
        config.put("role", "");
        mapper.setConfig(config);

        Response response = provider.addMapper(mapper);
        String mapperId = ApiUtil.getCreatedId(response);


        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        assertEquals(1, mappers.size());
        assertEquals(1, mappers.get(0).getConfig().size());

        mapper = provider.getMapperById(mapperId);
        mapper.getConfig().put("role", "offline_access");

        provider.update(mapperId, mapper);

        mappers = provider.getMappers();
        assertEquals("INHERIT", mappers.get(0).getConfig().get(IdentityProviderMapperModel.SYNC_MODE));
        assertEquals(1, mappers.size());
        assertEquals(2, mappers.get(0).getConfig().size());
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
        config.put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString());
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


    private void assertProviderInfo(Map<String, String> info, String id, String name) {
        System.out.println(info);
        Assert.assertEquals("id", id, info.get("id"));
        Assert.assertEquals("name", name, info.get("name"));
    }

    @Test
    public void testSamlExportSignatureOff() throws URISyntaxException, IOException, ConfigurationException, ParsingException, ProcessingException {
        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = getClass().getClassLoader().getResource("admin-test/saml-idp-metadata.xml");
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

        Map<String, String> result = realm.identityProviders().importFrom(form);

        // Explicitly disable SP Metadata Signature
        result.put(SAMLIdentityProviderConfig.SIGN_SP_METADATA, "false");

        // Create new SAML identity provider using configuration retrieved from import-config
        IdentityProviderRepresentation idpRep = createRep("saml", "saml", true, result);
        create(idpRep);

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = realm.identityProviders().get("saml").export("xml");
        Assert.assertEquals(200, response.getStatus());
        body = response.readEntity(String.class);
        response.close();

        Document document = DocumentUtil.getDocument(body);
        Element signatureElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), XMLDSIG_NSURI.get(), "Signature");
        Assert.assertNull(signatureElement);
    }

    @Test
    public void testSamlExportSignatureOn() throws URISyntaxException, IOException, ConfigurationException, ParsingException, ProcessingException {
        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = getClass().getClassLoader().getResource("admin-test/saml-idp-metadata.xml");
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

        Map<String, String> result = realm.identityProviders().importFrom(form);

        // Explicitly enable SP Metadata Signature
        result.put(SAMLIdentityProviderConfig.SIGN_SP_METADATA, "true");

        // Create new SAML identity provider using configuration retrieved from import-config
        IdentityProviderRepresentation idpRep = createRep("saml", "saml", true, result);
        create(idpRep);

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = realm.identityProviders().get("saml").export("xml");
        Assert.assertEquals(200, response.getStatus());
        body = response.readEntity(String.class);
        response.close();

        Document document = DocumentUtil.getDocument(body);

        Element signatureElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), XMLDSIG_NSURI.get(), "Signature");
        Assert.assertNotNull(signatureElement);
    }
}
