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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.broker.OIDCIdentityProviderConfigRep;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.sun.net.httpserver.HttpServer;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class IdentityProviderOidcTest extends AbstractIdentityProviderTest {

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectEvents
    Events events;

    @InjectHttpServer
    HttpServer httpServer;

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
        newIdentityProvider.getConfig().put("tokenUrl", "https://example.com/token");

        create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = managedRealm.admin().identityProviders().get("update-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertEquals("update-identity-provider", representation.getAlias());

        representation.setEnabled(false);
        representation.setStoreToken(true);
        // Changing non-sensitive fields keeps the masked secret reusable
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

        assertEquals("some secret value", runOnServer.fetch(s -> s.identityProviders().getByAlias("update-identity-provider").getConfig().get("clientSecret"), String.class));

        // Changing clientId requires providing a fresh secret (or secret is cleared)
        representation.getConfig().put("clientId", "changedClientId");
        representation.getConfig().put("clientSecret", "updated secret value");
        identityProviderResource.update(representation);
        adminEvents.poll();

        representation = identityProviderResource.toRepresentation();
        assertEquals("changedClientId", representation.getConfig().get("clientId"));
        assertEquals("updated secret value", runOnServer.fetch(s -> s.identityProviders().getByAlias("update-identity-provider").getConfig().get("clientSecret"), String.class));

        representation.getConfig().put("clientSecret", "${vault.key}");
        identityProviderResource.update(representation);
        event = adminEvents.poll();
        AdminEventAssertion.assertEvent(event, OperationType.UPDATE, AdminEventPaths.identityProviderPath(representation.getInternalId()), representation, ResourceType.IDENTITY_PROVIDER);
        assertThat(event.getRepresentation(), containsString("${vault.key}"));
        assertThat(event.getRepresentation(), not(containsString(ComponentRepresentation.SECRET_VALUE)));

        assertThat(identityProviderResource.toRepresentation().getConfig(), hasEntry("clientSecret", "${vault.key}"));
        assertEquals("${vault.key}", runOnServer.fetch(s -> s.identityProviders().getByAlias("update-identity-provider").getConfig().get("clientSecret"), String.class));
    }

    private static final String CLIENT_SECRET_REENTRY_REQUIRED =
            "Client secret must be re-entered when the token URL, client ID, authentication method, or related destination settings are changed";

    @Test
    public void maskedClientSecretNotReusedWhenTokenUrlChanges() {
        IdentityProviderRepresentation newIdentityProvider = createRep("masked-secret-idp", "oidc");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "real-partner-secret");
        newIdentityProvider.getConfig().put("tokenUrl", "https://idp.example.com/token");
        newIdentityProvider.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);
        create(newIdentityProvider);

        IdentityProviderResource resource = managedRealm.admin().identityProviders().get("masked-secret-idp");
        IdentityProviderRepresentation representation = resource.toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, representation.getConfig().get("clientSecret"));

        // Attack: change tokenUrl to attacker endpoint, leave masked secret — rejected
        representation.getConfig().put("tokenUrl", "https://attacker.example/token");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        try {
            resource.update(representation);
            fail("Should reject masked secret when token URL changes");
        } catch (Exception e) {
            assertError(e, CLIENT_SECRET_REENTRY_REQUIRED);
        }

        assertEquals("real-partner-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("clientSecret"), String.class));
        assertEquals("https://idp.example.com/token",
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("tokenUrl"), String.class));

        // Change clientId with masked secret — rejected
        representation = resource.toRepresentation();
        representation.getConfig().put("clientId", "attacker-client-id");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        try {
            resource.update(representation);
            fail("Should reject masked secret when clientId changes");
        } catch (Exception e) {
            assertError(e, CLIENT_SECRET_REENTRY_REQUIRED);
        }

        assertEquals("real-partner-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("clientSecret"), String.class));
        assertEquals("clientId",
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("clientId"), String.class));

        // Change clientAuthMethod with masked secret — rejected
        representation = resource.toRepresentation();
        representation.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_BASIC);
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        try {
            resource.update(representation);
            fail("Should reject masked secret when clientAuthMethod changes");
        } catch (Exception e) {
            assertError(e, CLIENT_SECRET_REENTRY_REQUIRED);
        }

        assertEquals("real-partner-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("clientSecret"), String.class));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_POST,
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("clientAuthMethod"), String.class));

        // Change tokenIntrospectionUrl with masked secret — rejected
        representation = resource.toRepresentation();
        representation.getConfig().put("tokenIntrospectionUrl", "https://idp.example.com/introspect");
        representation.getConfig().put("clientSecret", "real-partner-secret");
        resource.update(representation);
        adminEvents.poll();

        representation = resource.toRepresentation();
        representation.getConfig().put("tokenIntrospectionUrl", "https://attacker.example/introspect");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        try {
            resource.update(representation);
            fail("Should reject masked secret when tokenIntrospectionUrl changes");
        } catch (Exception e) {
            assertError(e, CLIENT_SECRET_REENTRY_REQUIRED);
        }

        assertEquals("real-partner-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("clientSecret"), String.class));
        assertEquals("https://idp.example.com/introspect",
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("tokenIntrospectionUrl"), String.class));

        // Unchanged sensitive fields: masked secret is reused
        representation = resource.toRepresentation();
        representation.setDisplayName("Still the same credentials");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        resource.update(representation);
        adminEvents.poll();

        assertEquals("real-partner-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-idp").getConfig().get("clientSecret"), String.class));
    }

    @Test
    public void maskedClientSecretNotReusedWhenDerivedTokenDestinationChanges() {
        // GitHub derives tokenUrl from baseUrl at provider construction time; stored config often
        // has no tokenUrl, so destination checks must include baseUrl itself.
        IdentityProviderRepresentation newIdentityProvider = createRep("masked-secret-github", "github");
        newIdentityProvider.getConfig().put("clientId", "github-client");
        newIdentityProvider.getConfig().put("clientSecret", "real-github-secret");
        newIdentityProvider.getConfig().put("baseUrl", "https://github.com");
        create(newIdentityProvider);

        IdentityProviderResource resource = managedRealm.admin().identityProviders().get("masked-secret-github");
        IdentityProviderRepresentation representation = resource.toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, representation.getConfig().get("clientSecret"));

        representation.getConfig().put("baseUrl", "https://attacker.example");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        try {
            resource.update(representation);
            fail("Should reject masked secret when baseUrl changes");
        } catch (Exception e) {
            assertError(e, CLIENT_SECRET_REENTRY_REQUIRED);
        }

        assertEquals("real-github-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-github").getConfig().get("clientSecret"), String.class));
        assertEquals("https://github.com",
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-github").getConfig().get("baseUrl"), String.class));

        // Unchanged baseUrl: masked secret is reused
        representation = resource.toRepresentation();
        representation.setDisplayName("Same GitHub destination");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        resource.update(representation);
        adminEvents.poll();

        assertEquals("real-github-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-github").getConfig().get("clientSecret"), String.class));

        // Optional fields absent in storage (null) but sent as "" by the UI must not look like a change
        IdentityProviderRepresentation noBaseUrl = createRep("masked-secret-github-no-base", "github");
        noBaseUrl.getConfig().put("clientId", "github-client");
        noBaseUrl.getConfig().put("clientSecret", "real-github-secret");
        create(noBaseUrl);

        IdentityProviderResource noBaseUrlResource = managedRealm.admin().identityProviders().get("masked-secret-github-no-base");
        IdentityProviderRepresentation noBaseUrlRep = noBaseUrlResource.toRepresentation();
        noBaseUrlRep.setDisplayName("Optional baseUrl unchanged");
        noBaseUrlRep.getConfig().put("baseUrl", "");
        noBaseUrlRep.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        noBaseUrlResource.update(noBaseUrlRep);
        adminEvents.poll();

        assertEquals("real-github-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-github-no-base").getConfig().get("clientSecret"), String.class));
    }

    @Test
    public void maskedClientSecretNotReusedWhenMicrosoftTenantIdChanges() {
        // Microsoft derives the token URL from tenantId at provider construction time.
        IdentityProviderRepresentation newIdentityProvider = createRep("masked-secret-microsoft", "microsoft");
        newIdentityProvider.getConfig().put("clientId", "microsoft-client");
        newIdentityProvider.getConfig().put("clientSecret", "real-microsoft-secret");
        newIdentityProvider.getConfig().put("tenantId", "common");
        create(newIdentityProvider);

        IdentityProviderResource resource = managedRealm.admin().identityProviders().get("masked-secret-microsoft");
        IdentityProviderRepresentation representation = resource.toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, representation.getConfig().get("clientSecret"));

        representation.getConfig().put("tenantId", "attacker-tenant");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        try {
            resource.update(representation);
            fail("Should reject masked secret when tenantId changes");
        } catch (Exception e) {
            assertError(e, CLIENT_SECRET_REENTRY_REQUIRED);
        }

        assertEquals("real-microsoft-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-microsoft").getConfig().get("clientSecret"), String.class));
        assertEquals("common",
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-microsoft").getConfig().get("tenantId"), String.class));

        representation = resource.toRepresentation();
        representation.setDisplayName("Same Microsoft destination");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        resource.update(representation);
        adminEvents.poll();

        assertEquals("real-microsoft-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-microsoft").getConfig().get("clientSecret"), String.class));
    }

    @Test
    public void maskedClientSecretNotReusedWhenPayPalSandboxChanges() {
        // PayPal switches the token host based on the sandbox flag.
        IdentityProviderRepresentation newIdentityProvider = createRep("masked-secret-paypal", "paypal");
        newIdentityProvider.getConfig().put("clientId", "paypal-client");
        newIdentityProvider.getConfig().put("clientSecret", "real-paypal-secret");
        newIdentityProvider.getConfig().put("sandbox", "false");
        create(newIdentityProvider);

        IdentityProviderResource resource = managedRealm.admin().identityProviders().get("masked-secret-paypal");
        IdentityProviderRepresentation representation = resource.toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, representation.getConfig().get("clientSecret"));

        representation.getConfig().put("sandbox", "true");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        try {
            resource.update(representation);
            fail("Should reject masked secret when sandbox changes");
        } catch (Exception e) {
            assertError(e, CLIENT_SECRET_REENTRY_REQUIRED);
        }

        assertEquals("real-paypal-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-paypal").getConfig().get("clientSecret"), String.class));
        assertEquals("false",
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-paypal").getConfig().get("sandbox"), String.class));

        representation = resource.toRepresentation();
        representation.setDisplayName("Same PayPal destination");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        resource.update(representation);
        adminEvents.poll();

        assertEquals("real-paypal-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-paypal").getConfig().get("clientSecret"), String.class));
    }

    @Test
    public void maskedClientSecretNotReusedWhenProviderIdMismatched() {
        // Spoofing providerId (e.g. saml) would otherwise select IdentityProviderModel's default
        // canReuseMaskedClientSecret() which always allows reuse, while persistence ignores providerId.
        IdentityProviderRepresentation newIdentityProvider = createRep("masked-secret-providerid", "oidc");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "real-partner-secret");
        newIdentityProvider.getConfig().put("tokenUrl", "https://idp.example.com/token");
        newIdentityProvider.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);
        create(newIdentityProvider);

        IdentityProviderResource resource = managedRealm.admin().identityProviders().get("masked-secret-providerid");
        IdentityProviderRepresentation representation = resource.toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, representation.getConfig().get("clientSecret"));

        representation.setProviderId("saml");
        representation.getConfig().put("tokenUrl", "https://attacker.example/token");
        representation.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        try {
            resource.update(representation);
            fail("Should reject providerId mismatch when updating with masked secret");
        } catch (Exception e) {
            assertError(e, "Identity Provider providerId cannot be changed");
        }

        assertEquals("oidc",
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-providerid").getProviderId(), String.class));
        assertEquals("real-partner-secret", runOnServer.fetch(
                s -> s.identityProviders().getByAlias("masked-secret-providerid").getConfig().get("clientSecret"), String.class));
        assertEquals("https://idp.example.com/token",
                runOnServer.fetch(s -> s.identityProviders().getByAlias("masked-secret-providerid").getConfig().get("tokenUrl"), String.class));
    }

    @Test
    public void failUpdateAlias() {
        IdentityProviderRepresentation newIdentityProvider = createRep("fail-update-alias", "oidc");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        String id = create(newIdentityProvider);

        IdentityProviderResource identityProviderResource = managedRealm.admin().identityProviders().get("fail-update-alias");
        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        representation.setAlias("changed-alias");

        try {
            identityProviderResource.update(representation);
            fail("Should not be able to change the alias");
        } catch (Exception e) {
            assertError(e, "Identity Provider alias cannot be changed");
        }

        // Verify alias was not changed
        representation = managedRealm.admin().identityProviders().get("fail-update-alias").toRepresentation();
        assertEquals("fail-update-alias", representation.getAlias());

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
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
            assertError(e, "The url [authorization_url] is malformed");
        }

        oidcConfig.setAuthorizationUrl(null);
        oidcConfig.setTokenUrl("http://test");

        try {
            resource.update(representation);
            fail("Invalid URL");
        } catch (Exception e) {
            assertError(e, "The url [token_url] requires secure connections");
        }

        oidcConfig.setAuthorizationUrl(null);
        oidcConfig.setTokenUrl(null);
        oidcConfig.setJwksUrl("http://test");
        try {
            resource.update(representation);
            fail("Invalid URL");
        } catch (Exception e) {
            assertError(e, "The url [jwks_url] requires secure connections");
        }

        oidcConfig.setAuthorizationUrl(null);
        oidcConfig.setTokenUrl(null);
        oidcConfig.setJwksUrl(null);
        oidcConfig.setLogoutUrl("http://test");
        try {
            resource.update(representation);
            fail("Invalid URL");
        } catch (Exception e) {
            assertError(e, "The url [logout_url] requires secure connections");
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
            assertError(e, "The url [userinfo_url] requires secure connections");
        }

        managedRealm.updateWithCleanup(r -> r.sslRequired(SslRequired.EXTERNAL.name()));
        resource.update(representation);

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    @Test
    public void testOIDCKeysRequiredForVariousConfigs() {
        String id = create(createRep("keycloak-oidc", "keycloak-oidc"));

        IdentityProviderResource resource = this.managedRealm.admin().identityProviders().get("keycloak-oidc");
        IdentityProviderRepresentation representation = resource.toRepresentation();
        OIDCIdentityProviderConfigRep oidcConfig = new OIDCIdentityProviderConfigRep(representation);

        // OIDC Keys required when "validate signature" is ON
        oidcConfig.setValidateSignature(true);
        try {
            resource.update(representation);
            fail("Not expected to update identity provider");
        } catch (Exception e) {
            assertError(e, "The 'Validating public key' is required when 'Validate signatures' enabled and 'Use JWKS URL' disabled");
        }

        // OIDC Keys (set by JWKS URL) required when "validate signature" is ON
        oidcConfig.setUseJwksUrl(true);
        try {
            resource.update(representation);
            fail("JWKS URL is required when 'Validate signatures' enabled and 'Use JWKS URL' enabled");
        } catch (Exception e) {
            assertError(e, "JWKS URL is required when 'Validate signatures' enabled and 'Use JWKS URL' enabled");
        }

        // OIDC Keys (set by JWKS URL) required when "authorization grant" is ON
        oidcConfig.setValidateSignature(false);
        oidcConfig.setJWTAuthorizationGrantEnabled(true);
        try {
            resource.update(representation);
            fail("JWKS URL is required when 'Validate signatures' enabled and 'Use JWKS URL' enabled");
        } catch (Exception e) {
            assertError(e, "JWKS URL is required when 'JWT Authorization Grant' enabled and 'Use JWKS URL' enabled");
        }

        // OIDC Keys (set by JWKS URL) required when "federated client authentication" is ON
        oidcConfig.setJWTAuthorizationGrantEnabled(false);
        oidcConfig.setSupportsClientAssertions(true);
        try {
            resource.update(representation);
            fail("JWKS URL is required when 'Validate signatures' enabled and 'Use JWKS URL' enabled");
        } catch (Exception e) {
            assertError(e, "JWKS URL is required when 'Supports client assertions' enabled and 'Use JWKS URL' enabled");
        }

        // Successful update when JWKS URL set
        oidcConfig.setJwksUrl("https://foo");
        oidcConfig.setIssuer("https://foo");
        resource.update(representation);

        managedRealm.cleanup().add(r -> r.identityProviders().get(id).remove());
    }

    private void assertError(Exception e, String expectedError) {
        assertTrue(e instanceof  ClientErrorException);
        Response response = ClientErrorException.class.cast(e).getResponse();
        assertEquals( Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ErrorRepresentation error = ((ClientErrorException) e).getResponse().readEntity(ErrorRepresentation.class);
        assertEquals(expectedError, error.getErrorMessage());
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

    @Test
    public void testOIDCIdentityProviderLoginIssuerValidation() {
        IdentityProviderRepresentation newIdentityProvider = createRep("external-idp", "oidc");
        newIdentityProvider.getConfig().put(OIDCIdentityProviderConfig.ISSUER, "bad-issuer");
        newIdentityProvider.getConfig().put("clientId", "test-client");
        newIdentityProvider.getConfig().put("clientSecret", "password");
        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, "http://localhost:8080/realms/external-realm/protocol/openid-connect/token");
        newIdentityProvider.getConfig().put("authorizationUrl", "http://localhost:8080/realms/external-realm/protocol/openid-connect/auth");
        newIdentityProvider.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, "http://localhost:8080/realms/external-realm/protocol/openid-connect/certs");
        newIdentityProvider.getConfig().put(OIDCIdentityProviderConfig.USE_JWKS_URL, "true");
        create(newIdentityProvider);

        events.skipAll();
        oauth.openLoginForm();
        loginPage.clickSocial("external-idp");
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();
        EventAssertion.assertError(events.poll())
                .type(EventType.IDENTITY_PROVIDER_LOGIN_ERROR)
                .sessionId(null)
                .error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);

        //test correct issuer
        managedRealm.updateIdentityProvider("external-idp", rep -> {
            rep.getConfig().put(OIDCIdentityProviderConfig.ISSUER, "http://localhost:8080/realms/external-realm");
        });

        oauth.openLoginForm();
        loginPage.clickSocial("external-idp");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        oauth.logoutRequest().idTokenHint(tokenResponse.getIdToken()).send();
        oauth.logoutRequest().send();
    }

    @Test
    public void importConfigShouldReportAnUnreachableMetadataUrl() {
        assertImportConfigFails("http://localhost:1/.well-known/openid-configuration", "Cannot fetch identity provider metadata");
    }

    @Test
    public void importConfigShouldReportAMalformedMetadataUrl() {
        assertImportConfigFails("http://localhost:1/ .well-known", "Cannot fetch identity provider metadata");
    }

    @Test
    public void importConfigShouldReportTheStatusOfAFailedMetadataRequest() {
        assertImportConfigFails(404, "not found", "Cannot fetch identity provider metadata: HTTP 404");
    }

    @Test
    public void importConfigShouldReportAUrlThatIsNotMetadata() {
        assertImportConfigFails(200, "<html>not metadata</html>", "Cannot parse identity provider metadata");
    }

    private void assertImportConfigFails(int status, String body, String expectedMessage) {
        String path = "/import-config";
        httpServer.createContext(path, exchange -> HttpServerUtil.sendResponse(exchange, status, null, body));
        try {
            assertImportConfigFails("http://127.0.0.1:" + httpServer.getAddress().getPort() + path, expectedMessage);
        } finally {
            httpServer.removeContext(path);
        }
    }

    private void assertImportConfigFails(String fromUrl, String expectedMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("providerId", "oidc");
        data.put("fromUrl", fromUrl);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> managedRealm.admin().identityProviders().importFrom(data));

        assertEquals(expectedMessage, error.getResponse().readEntity(ErrorRepresentation.class).getErrorMessage());
    }

    public static class ExternalRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("external-realm");

            realm.clients(ClientBuilder.create("test-client")
                    .secret("password")
                    .redirectUris("*"));

            realm.users(UserBuilder.create("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password"));

            return realm;
        }
    }

}
