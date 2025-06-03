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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.XMLDSIG_NSURI;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.crypto.dsig.XMLSignature;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
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
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.broker.OIDCIdentityProviderConfigRep;
import org.keycloak.testsuite.broker.oidc.OverwrittenMappersTestIdentityProviderFactory;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
    public void testFind() {
        create(createRep("twitter", "twitter idp","twitter", true, Collections.singletonMap("key1", "value1")));
        create(createRep("linkedin-openid-connect", "linkedin-openid-connect"));
        create(createRep("google", "google"));
        create(createRep("github", "github"));
        create(createRep("facebook", "facebook"));

        Assert.assertNames(realm.identityProviders().findAll(), "facebook", "github", "google", "linkedin-openid-connect", "twitter");

        Assert.assertNames(realm.identityProviders().find(null, true, 0, 2), "facebook", "github");
        Assert.assertNames(realm.identityProviders().find(null, true, 2, 2), "google", "linkedin-openid-connect");
        Assert.assertNames(realm.identityProviders().find(null, true, 4, 2), "twitter");

        Assert.assertNames(realm.identityProviders().find("g", true, 0, 5), "github", "google");

        Assert.assertNames(realm.identityProviders().find("g*", true, 0, 5), "github", "google");
        Assert.assertNames(realm.identityProviders().find("g*", true, 0, 1), "github");
        Assert.assertNames(realm.identityProviders().find("g*", true, 1, 1), "google");

        Assert.assertNames(realm.identityProviders().find("*oo*", true, 0, 5), "google", "facebook");

        //based on display name search
        Assert.assertNames(realm.identityProviders().find("*ter i*", true, 0, 5), "twitter");

        List<IdentityProviderRepresentation> results = realm.identityProviders().find("\"twitter\"", true, 0, 5);
        Assert.assertNames(results, "twitter");
        Assert.assertTrue("Result is not in brief representation", results.iterator().next().getConfig().isEmpty());
        results = realm.identityProviders().find("\"twitter\"", null, 0, 5);
        Assert.assertNames(results, "twitter");
        Assert.assertFalse("Config should be present in full representation", results.iterator().next().getConfig().isEmpty());
    }

    @Test
    public void testFindForLoginPreservesOrderByAlias() {

        create(createRep("twitter", "twitter"));
        create(createRep("linkedin-openid-connect", "linkedin-openid-connect"));
        create(createRep("google", "google"));
        create(createRep("github", "github"));
        create(createRep("facebook", "facebook"));
        create(createRep("stackoverflow", "stackoverflow"));
        create(createRep("openshift-v4", "openshift-v4"));

        getTestingClient().server(REALM_NAME).run(session -> {
            // fetch the list of idps available for login (should match all from above list) and ensure they come ordered by alias.
            List<String> aliases = session.identityProviders().getForLogin(IdentityProviderStorageProvider.FetchMode.ALL, null)
                    .map(IdentityProviderModel::getAlias).toList();
            assertThat(aliases, contains("facebook", "github", "google", "linkedin-openid-connect", "openshift-v4", "stackoverflow", "twitter"));
        });
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
        assertNull(representation.getFirstBrokerLoginFlowAlias());

        assertEquals("some secret value", testingClient.testing("admin-client-test").getIdentityProviderConfig("new-identity-provider").get("clientSecret"));

        IdentityProviderRepresentation rep = realm.identityProviders().findAll().stream().filter(i -> i.getAlias().equals("new-identity-provider")).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, rep.getConfig().get("clientSecret"));
    }

    @Test
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
    public void shouldFailWhenAliasHasSpaceDuringCreation() {
        IdentityProviderRepresentation newIdentityProvider = createRep("New Identity Provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "some secret value");
        newIdentityProvider.getConfig().put("clientAuthMethod",OIDCLoginProtocol.CLIENT_SECRET_BASIC);

        try (Response response = this.realm.identityProviders().create(newIdentityProvider)) {
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
        assertNull(representation.getConfig().get("jwtX509HeadersEnabled"));
        assertTrue(representation.isEnabled());
        assertFalse(representation.isStoreToken());
        assertFalse(representation.isTrustEmail());
    }

    @Test
    public void testCreateWithJWTAndX509Headers() {
        IdentityProviderRepresentation newIdentityProvider = createRep("new-identity-provider", "oidc");

        newIdentityProvider.getConfig().put(IdentityProviderModel.SYNC_MODE, "IMPORT");
        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);
        newIdentityProvider.getConfig().put("jwtX509HeadersEnabled", "true");

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
        assertEquals("true", representation.getConfig().get("jwtX509HeadersEnabled"));
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
            assertAdminEvents.poll(); // realm update
            IdentityProviderRepresentation representation = createRep(UUID.randomUUID().toString(), "oidc");

            representation.getConfig().put("clientId", "clientId");
            representation.getConfig().put("clientSecret", "some secret value");

            create(representation);

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

    @Test
    public void testNotAvailableFromRealRepresentation() {
        IdentityProviderRepresentation newIdentityProvider = createRep("remove-identity-provider", "saml");

        create(newIdentityProvider);

        RealmRepresentation rep = this.realm.toRepresentation();
        assertNull(rep.getIdentityProviders());
        assertNull(rep.getIdentityProviderMappers());
    }

    private void create(IdentityProviderRepresentation idpRep) {
        Response response = realm.identityProviders().create(idpRep);
        Assert.assertNotNull(ApiUtil.getCreatedId(response));
        response.close();

        getCleanup().addIdentityProviderAlias(idpRep.getAlias());

        String secret = idpRep.getConfig() != null ? idpRep.getConfig().get("clientSecret") : null;
        idpRep = StripSecretsUtils.stripSecrets(null, idpRep);
        // if legacy hide on login page attribute was used, the attr will be removed when converted to model
        idpRep.setHideOnLogin(Boolean.parseBoolean(idpRep.getConfig().remove(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR)));

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProviderPath(idpRep.getAlias()), idpRep, ResourceType.IDENTITY_PROVIDER);

        if (secret != null) {
            idpRep.getConfig().put("clientSecret", secret);
        }
    }

    private IdentityProviderRepresentation createRep(String alias, String providerId) {
        return createRep(alias, providerId,true, null);
    }

    private IdentityProviderRepresentation createRep(String alias, String providerId,boolean enabled, Map<String, String> config) {
        return createRep(alias, alias, providerId, enabled, config);
    }

    private IdentityProviderRepresentation createRep(String alias, String displayName, String providerId, boolean enabled, Map<String, String> config) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();

        idp.setAlias(alias);
        idp.setDisplayName(displayName);
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

        /*
        // disabled to prevent 429 rate limiting on GitHub actions for LinkedIn's
        // https://www.linkedin.com/oauth/.well-known/openid-configuration discovery URL
        create(createRep("linkedin-openid-connect", "linkedin-openid-connect"));
        provider = realm.identityProviders().get("linkedin-openid-connect");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "linkedin-user-attribute-mapper", "oidc-username-idp-mapper");
        */

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
        assertMapperTypes(mapperTypes, "keycloak-oidc-role-to-role-idp-mapper", "oidc-user-attribute-idp-mapper", "oidc-role-idp-mapper", "oidc-username-idp-mapper", "oidc-advanced-group-idp-mapper", "oidc-advanced-role-idp-mapper", "oidc-user-session-note-idp-mapper");

        create(createRep("oidc", "oidc"));
        provider = realm.identityProviders().get("oidc");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "oidc-user-attribute-idp-mapper", "oidc-role-idp-mapper", "oidc-username-idp-mapper", "oidc-advanced-group-idp-mapper", "oidc-advanced-role-idp-mapper", "oidc-user-session-note-idp-mapper");

        create(createRep("saml", "saml"));
        provider = realm.identityProviders().get("saml");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "saml-user-attribute-idp-mapper", "saml-role-idp-mapper", "saml-username-idp-mapper", "saml-advanced-role-idp-mapper", "saml-advanced-group-idp-mapper", "saml-xpath-attribute-idp-mapper");
    }

    @Test
    public void mapperTypesCanBeOverwritten() {
        String kcOidcProviderId = "keycloak-oidc";
        create(createRep(kcOidcProviderId, kcOidcProviderId));

        String testProviderId = OverwrittenMappersTestIdentityProviderFactory.PROVIDER_ID;
        create(createRep(testProviderId, testProviderId));

        /*
         * in the test provider, we have overwritten the mapper types to be the same as supported by "keycloak-oidc", so
         * the "keycloak-oidc" mappers are the expected mappers for the test provider
         */
        IdentityProviderResource kcOidcProvider = realm.identityProviders().get(kcOidcProviderId);
        Set<String> expectedMapperTypes = kcOidcProvider.getMapperTypes().keySet();

        IdentityProviderResource testProvider = realm.identityProviders().get(testProviderId);
        Set<String> actualMapperTypes = testProvider.getMapperTypes().keySet();

        assertThat(actualMapperTypes, equalTo(expectedMapperTypes));
    }

    private void assertMapperTypes(Map<String, IdentityProviderMapperTypeRepresentation> mapperTypes, String ... mapperIds) {
        Set<String> expected = new HashSet<>();
        expected.add("hardcoded-user-session-attribute-idp-mapper");
        expected.add("oidc-hardcoded-role-idp-mapper");
        expected.add("oidc-hardcoded-group-idp-mapper");
        expected.add("hardcoded-attribute-idp-mapper");
        expected.add("multi-valued-test-idp-mapper");
        expected.addAll(Arrays.asList(mapperIds));

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
    public void importShouldFailDueAliasWithSpace() {

        Map<String, Object> data = new HashMap<>();
        data.put("providerId", "saml");
        data.put("alias", "Alias With Space");
        data.put("fromUrl", "http://");

       assertThrows(BadRequestException.class, () -> {
            realm.identityProviders().importFrom(data);
        });

    }

    @Test
    public void testSamlImportAndExport() throws URISyntaxException, IOException, ParsingException {
        testSamlImport("saml-idp-metadata.xml", true);

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = realm.identityProviders().get("saml").export("xml");
        Assert.assertEquals(200, response.getStatus());
        String body = response.readEntity(String.class);
        response.close();

        assertSamlExport(body, true);
    }

   @Test
    public void testSamlImportWithBom() throws URISyntaxException, IOException, ParsingException {
        testSamlImport("saml-idp-metadata_utf8_bom.xml", true);

        // Perform export, and make sure some of the values are like they're supposed to be
        Response response = realm.identityProviders().get("saml").export("xml");
        Assert.assertEquals(200, response.getStatus());
        String body = response.readEntity(String.class);
        response.close();

        assertSamlExport(body, true);
    }

    @Test
    public void testSamlImportAndExportDifferentBindings() throws URISyntaxException, IOException, ParsingException {
        testSamlImport("saml-idp-metadata-different-bindings.xml", false);

        // Perform export, and make sure some of the values are like they're supposed to be
        try (Response response = realm.identityProviders().get("saml").export("xml")) {
            Assert.assertEquals(200, response.getStatus());
            String body = response.readEntity(String.class);
            assertSamlExport(body, false);
        }
    }

    @Test
    public void testSamlImportWithAnyEncryptionMethod() throws URISyntaxException, IOException, ParsingException {
        testSamlImport("saml-idp-metadata-encryption-methods.xml", true);
    }

    private void testSamlImport(String fileName, boolean postBindingResponse) throws URISyntaxException, IOException, ParsingException {
        // Use import-config to convert IDPSSODescriptor file into key value pairs
        // to use when creating a SAML Identity Provider
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = getClass().getClassLoader().getResource("admin-test/"+fileName);
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        String body = new String(content, Charset.forName("utf-8"));
        form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, fileName);

        Map<String, String> result = realm.identityProviders().importFrom(form);
        assertSamlImport(result, SIGNING_CERT_1, true, postBindingResponse);

        // Create new SAML identity provider using configuration retrieved from import-config
        create(createRep("saml", "saml",true, result));

        IdentityProviderResource provider = realm.identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep, true, postBindingResponse);

        // Now list the providers - we should see the one just created
        List<IdentityProviderRepresentation> providers = realm.identityProviders().findAll();
        Assert.assertNotNull("identityProviders not null", providers);
        Assert.assertEquals("identityProviders instance count", 1, providers.size());
        assertEqual(rep, providers.get(0));

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
        assertSamlImport(result, SIGNING_CERT_1, false, true);

        // Create new SAML identity provider using configuration retrieved from import-config
        create(createRep("saml", "saml", false, result));

        IdentityProviderResource provider = realm.identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep, false, true);

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
        assertSamlImport(result, SIGNING_CERT_1 + "," + SIGNING_CERT_2, true, true);

        // Create new SAML identity provider using configuration retrieved from import-config
        create(createRep("saml", "saml",true, result));

        IdentityProviderResource provider = realm.identityProviders().get("saml");
        IdentityProviderRepresentation rep = provider.toRepresentation();
        assertCreatedSamlIdp(rep, true, true);

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

        assertSamlExport(body, true);
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

        response = realm.identityProviders().getIdentityProviders("linkedin-openid-connect");
        Assert.assertEquals("Status", 200, response.getStatus());
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "linkedin-openid-connect", "LinkedIn");

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
        Assert.assertEquals("hideOnLogin", expected.isHideOnLogin(), actual.isHideOnLogin());
        Assert.assertEquals("firstBrokerLoginFlowAlias", expected.getFirstBrokerLoginFlowAlias(), actual.getFirstBrokerLoginFlowAlias());
        Assert.assertEquals("config", expected.getConfig(), actual.getConfig());
    }

    private void assertCreatedSamlIdp(IdentityProviderRepresentation idp, boolean enabled, boolean postBindingResponse) {
        //System.out.println("idp: " + idp);
        Assert.assertNotNull("IdentityProviderRepresentation not null", idp);
        Assert.assertNotNull("internalId", idp.getInternalId());
        Assert.assertEquals("alias", "saml", idp.getAlias());
        Assert.assertEquals("providerId", "saml", idp.getProviderId());
        Assert.assertEquals("enabled",enabled, idp.isEnabled());
        Assert.assertTrue("hideOnLogin", idp.isHideOnLogin());
        Assert.assertNull("firstBrokerLoginFlowAlias", idp.getFirstBrokerLoginFlowAlias());
        assertSamlConfig(idp.getConfig(), postBindingResponse, false);
    }

    private void assertSamlConfig(Map<String, String> config, boolean postBindingResponse, boolean hasHideOnLoginPage) {
        // import endpoint simply converts IDPSSODescriptor into key value pairs.
        // check that saml-idp-metadata.xml was properly converted into key value pairs
        //System.out.println(config);
        List<String> keys = new ArrayList<>(List.of("syncMode",
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
                "urn:oasis:names:tc:SAML:2.0:protocol"));

        Set<String> actual = new HashSet<>(desc.getProtocolSupportEnumeration());

        Assert.assertEquals("ProtocolSupportEnumeration", expected, actual);

        Assert.assertNotNull("AssertionConsumerService not null", desc.getAssertionConsumerService());
        Assert.assertEquals("AssertionConsumerService.size", 3, desc.getAssertionConsumerService().size());

        IndexedEndpointType endpoint = desc.getAssertionConsumerService().get(0);
        final URI samlUri = new URI(OAuthClient.AUTH_SERVER_ROOT + "/realms/admin-client-test/broker/saml/endpoint");

        Assert.assertEquals("AssertionConsumerService.Location", samlUri, endpoint.getLocation());
        Assert.assertEquals("AssertionConsumerService.Binding",
                postBindingResponse ? JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri() : JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri(),
                endpoint.getBinding());
        Assert.assertTrue("AssertionConsumerService.isDefault", endpoint.isIsDefault());

        endpoint = desc.getAssertionConsumerService().get(1);

        Assert.assertEquals("AssertionConsumerService.Location", samlUri, endpoint.getLocation());
        Assert.assertEquals("AssertionConsumerService.Binding",
                postBindingResponse ? JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri() : JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri(),
                endpoint.getBinding());

        endpoint = desc.getAssertionConsumerService().get(2);

        Assert.assertEquals("AssertionConsumerService.Location", samlUri, endpoint.getLocation());
        Assert.assertEquals("AssertionConsumerService.Binding", JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri(), endpoint.getBinding());

        Assert.assertNotNull("SingleLogoutService not null", desc.getSingleLogoutService());
        Assert.assertEquals("SingleLogoutService.size", 2, desc.getSingleLogoutService().size());

        EndpointType sloEndpoint = desc.getSingleLogoutService().get(0);

        Assert.assertEquals("SingleLogoutService.Location", samlUri, sloEndpoint.getLocation());
        Assert.assertEquals("SingleLogoutService.Binding", JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri(), sloEndpoint.getBinding());

        sloEndpoint = desc.getSingleLogoutService().get(1);

        Assert.assertEquals("SingleLogoutService.Location", samlUri, sloEndpoint.getLocation());
        Assert.assertEquals("SingleLogoutService.Binding", JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri(), sloEndpoint.getBinding());

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
    public void testSamlExportSignatureOn() throws URISyntaxException, IOException, ConfigurationException, ParsingException, ProcessingException, CertificateEncodingException {
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
        result.put(SAMLIdentityProviderConfig.XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER, XmlKeyInfoKeyNameTransformer.CERT_SUBJECT.name());

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
        assertThat("Signature not null", signatureElement, notNullValue());

        Element keyInfoElement = DocumentUtil.getDirectChildElement(signatureElement, XMLDSIG_NSURI.get(), "KeyInfo");
        assertThat("KeyInfo not null", keyInfoElement, notNullValue());

        Element x509DataElement = DocumentUtil.getDirectChildElement(keyInfoElement, XMLDSIG_NSURI.get(), "X509Data");
        assertThat("X509Data not null", x509DataElement, notNullValue());

        Element x509CertificateElement = DocumentUtil.getDirectChildElement(x509DataElement, XMLDSIG_NSURI.get(), "X509Certificate");
        assertThat("X509Certificate not null", x509CertificateElement, notNullValue());

        Element keyNameElement = DocumentUtil.getDirectChildElement(keyInfoElement, XMLDSIG_NSURI.get(), "KeyName");
        assertThat("KeyName not null", keyNameElement, notNullValue());

        String activeSigCert = KeyUtils.findActiveSigningKey(realm, Constants.DEFAULT_SIGNATURE_ALGORITHM).getCertificate();
        assertThat("activeSigCert not null", activeSigCert, notNullValue());

        X509Certificate activeX509SigCert = XMLSignatureUtil.getX509CertificateFromKeyInfoString(activeSigCert);
        assertThat("KeyName matches subject DN",
                keyNameElement.getTextContent().trim(), equalTo(activeX509SigCert.getSubjectX500Principal().getName()));

        assertThat("Signing cert matches active realm cert",
                x509CertificateElement.getTextContent().trim(), equalTo(Base64.getEncoder().encodeToString(activeX509SigCert.getEncoded())));

        PublicKey activePublicSigKey = activeX509SigCert.getPublicKey();
        assertThat("Metadata signature is valid",
                new SAML2Signature().validate(document, new HardcodedKeyLocator(activePublicSigKey)), is(true));
    }
}
