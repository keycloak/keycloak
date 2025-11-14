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

package org.keycloak.tests.admin.identityprovider;

import java.util.UUID;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.broker.OIDCIdentityProviderConfigRep;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class IdentityProviderOidcTest extends AbstractIdentityProviderTest {

    @Test
    public void testCreateWithReservedCharacterForAlias() {
        IdentityProviderRepresentation newIdentityProvider = createRep("ne$&w-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");

        Response response = managedRealm.admin().identityProviders().create(newIdentityProvider);
        Assertions.assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreate() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");

        String id = create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = managedRealm.admin().identityProviders().get("new-identity-provider");

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
        assertNull(representation.isStoreToken());
        assertNull(representation.isTrustEmail());
        assertNull(representation.getFirstBrokerLoginFlowAlias());

        assertEquals("some secret value", runOnServer.fetch(s -> s.identityProviders().getByAlias("new-identity-provider").getConfig().get("clientSecret"), String.class));

        IdentityProviderRepresentation rep = managedRealm.admin().identityProviders().findAll().stream().filter(i -> i.getAlias().equals("new-identity-provider")).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, rep.getConfig().get("clientSecret"));

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void failCreateInvalidUrl() throws Exception {
        managedRealm.updateWithCleanup(r -> r.sslRequired(SslRequired.ALL.name()));

        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");

        OIDCIdentityProviderConfigRep oidcConfig = new OIDCIdentityProviderConfigRep(newIdentityProvider);

        oidcConfig.setAuthorizationUrl("invalid://test");

        try (Response response = this.managedRealm.admin().identityProviders().create(newIdentityProvider)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("The url [authorization_url] is malformed", error.getErrorMessage());
        }

        oidcConfig.setAuthorizationUrl(null);
        oidcConfig.setTokenUrl("http://test");

        try (Response response = this.managedRealm.admin().identityProviders().create(newIdentityProvider)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("The url [token_url] requires secure connections", error.getErrorMessage());
        }

        oidcConfig.setAuthorizationUrl(null);
        oidcConfig.setTokenUrl(null);
        oidcConfig.setJwksUrl("http://test");

        try (Response response = this.managedRealm.admin().identityProviders().create(newIdentityProvider)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("The url [jwks_url] requires secure connections", error.getErrorMessage());
        }

        oidcConfig.setAuthorizationUrl(null);
        oidcConfig.setTokenUrl(null);
        oidcConfig.setJwksUrl(null);
        oidcConfig.setLogoutUrl("http://test");

        try (Response response = this.managedRealm.admin().identityProviders().create(newIdentityProvider)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("The url [logout_url] requires secure connections", error.getErrorMessage());
        }

        oidcConfig.setAuthorizationUrl(null);
        oidcConfig.setTokenUrl(null);
        oidcConfig.setJwksUrl(null);
        oidcConfig.setLogoutUrl(null);
        oidcConfig.setUserInfoUrl("http://test");

        try (Response response = this.managedRealm.admin().identityProviders().create(newIdentityProvider)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertEquals("The url [userinfo_url] requires secure connections", error.getErrorMessage());
        }
    }

    @Test
    public void shouldFailWhenAliasHasSpaceDuringCreation() {
        IdentityProviderRepresentation newIdentityProvider = createRep("New Identity Provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");
        newIdentityProvider.getConfig().put("clientAuthMethod",OIDCLoginProtocol.CLIENT_SECRET_BASIC);

        try (Response response = this.managedRealm.admin().identityProviders().create(newIdentityProvider)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            String error = response.readEntity(String.class);
            assertTrue(error.contains("Empty Space not allowed."));
        }
    }

    @Test
    public void testCreateWithBasicAuth() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");
        newIdentityProvider.getConfig().put("clientAuthMethod",OIDCLoginProtocol.CLIENT_SECRET_BASIC);

        String id = create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = managedRealm.admin().identityProviders().get("new-identity-provider");

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
        assertNull(representation.isStoreToken());
        assertNull(representation.isTrustEmail());

        assertEquals("some secret value", runOnServer.fetch(s -> s.identityProviders().getByAlias("new-identity-provider").getConfig().get("clientSecret"), String.class));

        IdentityProviderRepresentation rep = managedRealm.admin().identityProviders().findAll().stream().filter(i -> i.getAlias().equals("new-identity-provider")).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, rep.getConfig().get("clientSecret"));

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void testCreateWithJWT() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);

        String id = create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = managedRealm.admin().identityProviders().get("new-identity-provider");

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
        assertNull(representation.getConfig().get("jwtX509HeadersEnabled"));
        assertTrue(representation.isEnabled());
        assertNull(representation.isStoreToken());
        assertNull(representation.isTrustEmail());

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void testCreateWithJWTAndX509Headers() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);
        newIdentityProvider.getConfig().put("jwtX509HeadersEnabled", "true");

        String id = create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = managedRealm.admin().identityProviders().get("new-identity-provider");

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
        assertEquals("true", representation.getConfig().get("jwtX509HeadersEnabled"));
        assertTrue(representation.isEnabled());
        assertNull(representation.isStoreToken());
        assertNull(representation.isTrustEmail());

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void testUpdate() {
        IdentityProviderRepresentation newIdentityProvider = createRep("update-identity-provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = managedRealm.admin().identityProviders().get("update-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertEquals("update-identity-provider", representation.getAlias());

        representation.setAlias("changed-alias");
        representation.setEnabled(false);
        representation.setStoreToken(true);
        representation.getConfig().put("clientId", "changedClientId");

        identityProviderResource.update(representation);
        AdminEventRepresentation event = adminEvents.poll();
        AdminEventAssertion.assertEvent(event, OperationType.UPDATE, AdminEventPaths.identityProviderPath("update-identity-provider"), representation, ResourceType.IDENTITY_PROVIDER);
        assertFalse(event.getRepresentation().contains("some secret value"));
        assertTrue(event.getRepresentation().contains(ComponentRepresentation.SECRET_VALUE));

        identityProviderResource = managedRealm.admin().identityProviders().get(representation.getInternalId());

        assertNotNull(identityProviderResource);

        representation = identityProviderResource.toRepresentation();

        assertFalse(representation.isEnabled());
        assertTrue(representation.isStoreToken());
        assertEquals("changedClientId", representation.getConfig().get("clientId"));

        assertEquals("some secret value", runOnServer.fetch(s -> s.identityProviders().getByAlias("changed-alias").getConfig().get("clientSecret"), String.class));

        representation.getConfig().put("clientSecret", "${vault.key}");
        identityProviderResource.update(representation);
        event = adminEvents.poll();
        AdminEventAssertion.assertEvent(event, OperationType.UPDATE, AdminEventPaths.identityProviderPath(representation.getInternalId()), representation, ResourceType.IDENTITY_PROVIDER);
        assertThat(event.getRepresentation(), containsString("${vault.key}"));
        assertThat(event.getRepresentation(), not(containsString(ComponentRepresentation.SECRET_VALUE)));

        assertThat(identityProviderResource.toRepresentation().getConfig(), hasEntry("clientSecret", "${vault.key}"));
        assertEquals("${vault.key}", runOnServer.fetch(s -> s.identityProviders().getByAlias("changed-alias").getConfig().get("clientSecret"), String.class));
    }

    @Test
    public void failUpdateInvalidUrl() throws Exception {
        managedRealm.updateWithCleanup(r -> r.sslRequired(SslRequired.ALL.name()));
        adminEvents.poll(); // realm update
        IdentityProviderRepresentation representation = createRep(UUID.randomUUID().toString(), "oidc");

        representation.getConfig().put("clientId", "clientId");
        representation.getConfig().put("clientSecret", "some secret value");

        String id = create(representation);

        IdentityProviderResource resource = this.managedRealm.admin().identityProviders().get(representation.getAlias());
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

        managedRealm.updateWithCleanup(r -> r.sslRequired(SslRequired.EXTERNAL.name()));
        resource.update(representation);

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void testNoExport() {
        String id = create(createRep("keycloak-oidc", "keycloak-oidc"));

        Response response = managedRealm.admin().identityProviders().get("keycloak-oidc").export("json");
        Assertions.assertEquals(204, response.getStatus(), "status");
        String body = response.readEntity(String.class);
        Assertions.assertNull(body, "body");
        response.close();

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }
}
