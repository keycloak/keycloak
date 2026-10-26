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
package org.keycloak.tests.client.policies;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.models.CibaConfig;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutorFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicyFactory;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.SectorIdentifierRedirectUrisProvider;
import org.keycloak.testframework.oauth.annotations.InjectSectorIdentifierRedirectUrisProvider;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class ClientUrisPatternExecutorTest extends AbstractClientPoliciesTest {

    private static final String SAFE_PATTERN = "^https://trusted\\.com.*";

    private static final String VALID_URL = "https://trusted.com/callback";
    private static final String INVALID_URL = "http://untrusted.com/callback";

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectRealm
    protected ManagedRealm realm;

    @InjectSectorIdentifierRedirectUrisProvider("http://localhost:8080/app")
    protected SectorIdentifierRedirectUrisProvider sectorIdentifierRedirectUris;

    @Test
    public void testFieldsByAdmin() throws Exception {
        testFieldByAdmin("rootUrl", OIDCLoginProtocol.LOGIN_PROTOCOL, ClientRepresentation::setRootUrl);
        testFieldByAdmin("adminUrl", OIDCLoginProtocol.LOGIN_PROTOCOL, ClientRepresentation::setAdminUrl);
        testFieldByAdmin("baseUrl", OIDCLoginProtocol.LOGIN_PROTOCOL, ClientRepresentation::setBaseUrl);
        testFieldByAdmin("redirectUris", OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.setRedirectUris(Collections.singletonList(val)));
        testFieldByAdmin("webOrigins", OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.setWebOrigins(Collections.singletonList(val)));

        testFieldByAdmin("jwksUri", OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.JWKS_URL, val));
        testFieldByAdmin("requestUris", OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.REQUEST_URIS, val));
        testFieldByAdmin("backchannelLogoutUrl", OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, val));
        testFieldByAdmin("frontChannelLogoutUrl", OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, val));
        testFieldByAdmin("postLogoutRedirectUris", OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, val));
        testFieldByAdmin("cibaClientNotificationEndpoint", OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, val));
        testFieldByAdmin(OIDCConfigAttributes.LOGO_URI, OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.LOGO_URI, val));
        testFieldByAdmin(OIDCConfigAttributes.POLICY_URI, OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.POLICY_URI, val));
        testFieldByAdmin(OIDCConfigAttributes.TOS_URI, OIDCLoginProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(OIDCConfigAttributes.TOS_URI, val));
    }

    @Test
    public void testFieldsByAdminSaml() throws Exception {
        testFieldByAdmin("rootUrl", SamlProtocol.LOGIN_PROTOCOL, ClientRepresentation::setRootUrl);
        testFieldByAdmin("adminUrl", SamlProtocol.LOGIN_PROTOCOL, ClientRepresentation::setAdminUrl);
        testFieldByAdmin("baseUrl", SamlProtocol.LOGIN_PROTOCOL, ClientRepresentation::setBaseUrl);
        testFieldByAdmin("redirectUris", SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.setRedirectUris(Collections.singletonList(val)));

        testFieldByAdmin(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, val));
        testFieldByAdmin(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, val));
        testFieldByAdmin(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, val));
        testFieldByAdmin(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, val));
        testFieldByAdmin(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, val));
        testFieldByAdmin(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, val));
        testFieldByAdmin(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE, val));
        testFieldByAdmin(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, val));
        testFieldByAdmin(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, SamlProtocol.LOGIN_PROTOCOL, (c, val) -> c.getAttributes().put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, val));
    }

    @Test
    public void testSamlPartialUpdateByAdmin() throws Exception {
        setupPolicy(List.of(SAFE_PATTERN), null);
        String id = createClientByAdmin(realm, generateSuffixedName("partial-saml"), SamlProtocol.LOGIN_PROTOCOL, client -> {
            client.setRedirectUris(List.of(VALID_URL));
            client.setAttributes(Map.of(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, VALID_URL));
        });
        ClientResource resource = realm.admin().clients().get(id);
        for (String attribute : List.of(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE)) {
            ClientRepresentation update = new ClientRepresentation();
            update.setAttributes(Map.of(attribute, INVALID_URL));
            Assertions.assertThrows(BadRequestException.class, () -> resource.update(update));
            Assertions.assertNotEquals(INVALID_URL, resource.toRepresentation().getAttributes().get(attribute));

            update.setAttributes(Map.of(attribute, VALID_URL));
            resource.update(update);
            Assertions.assertEquals(VALID_URL, resource.toRepresentation().getAttributes().get(attribute));
            Assertions.assertEquals(SamlProtocol.LOGIN_PROTOCOL, resource.toRepresentation().getProtocol());
        }
        ClientRepresentation update = new ClientRepresentation();
        update.setName("Updated without URI changes");
        resource.update(update);
        Assertions.assertEquals(update.getName(), resource.toRepresentation().getName());
    }

    @Test
    public void testSamlUrisAreValidatedBeforeSwitchingProtocol() throws Exception {
        setupPolicy(List.of(SAFE_PATTERN), null);
        for (String attribute : List.of(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE)) {
            ClientPolicyException error = Assertions.assertThrows(ClientPolicyException.class, () ->
                    createClientByAdmin(realm, generateSuffixedName("invalid-inactive-saml"), OIDCLoginProtocol.LOGIN_PROTOCOL,
                            client -> {
                                client.setRedirectUris(List.of(VALID_URL));
                                client.setAttributes(Map.of(attribute, INVALID_URL));
                            }));
            Assertions.assertEquals("Invalid " + attribute, error.getErrorDetail());

            String id = createClientByAdmin(realm, generateSuffixedName("inactive-saml"), OIDCLoginProtocol.LOGIN_PROTOCOL, client -> {
                client.setRedirectUris(List.of(VALID_URL));
                client.setAttributes(Map.of(attribute, VALID_URL));
            });
            ClientResource resource = realm.admin().clients().get(id);
            ClientRepresentation update = new ClientRepresentation();
            update.setAttributes(Map.of(attribute, INVALID_URL));
            Assertions.assertThrows(BadRequestException.class, () -> resource.update(update));
            Assertions.assertEquals(VALID_URL, resource.toRepresentation().getAttributes().get(attribute));

            ClientRepresentation protocolUpdate = new ClientRepresentation();
            protocolUpdate.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
            resource.update(protocolUpdate);
            ClientRepresentation stored = resource.toRepresentation();
            Assertions.assertEquals(SamlProtocol.LOGIN_PROTOCOL, stored.getProtocol());
            Assertions.assertEquals(VALID_URL, stored.getAttributes().get(attribute));
        }
    }

    @Test
    public void testFieldsDynamically() throws Exception {
        //remove trust registration policy
        List<ComponentRepresentation> components = realm.admin().components().query(null, ClientRegistrationPolicy.class.getCanonicalName())
                .stream()
                .filter(c -> c.getProviderId().equals(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID))
                .toList();
        for (ComponentRepresentation component : components) {
            realm.admin().components().removeComponent(component.getId());
        }

        ClientRegistration reg = ClientRegistration.create().url(keycloakUrls.getBase(), realm.getName()).build();
        ClientInitialAccessPresentation token = realm.admin().clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));

        testFieldDynamically(reg, "baseUrl", OIDCClientRepresentation::setClientUri);
        testFieldDynamically(reg, "redirectUris", (c, val) -> c.setRedirectUris(Collections.singletonList(val)));

        testFieldDynamically(reg, "jwksUri", OIDCClientRepresentation::setJwksUri);
        testFieldDynamically(reg, "logoUri", OIDCClientRepresentation::setLogoUri);
        testFieldDynamically(reg, "policyUri", OIDCClientRepresentation::setPolicyUri);
        testFieldDynamically(reg, "backchannelLogoutUrl", OIDCClientRepresentation::setBackchannelLogoutUri);
        testFieldDynamically(reg, "frontChannelLogoutUrl", OIDCClientRepresentation::setFrontChannelLogoutUri);

        //test sectorIdentifierUri
        String sectorIdentifierUriPattern = "^http://localhost.*/sector-identifier-redirect-uris$";
        String redirectUri = "http://localhost:8080/app";

        testFieldDynamically(reg, "sectorIdentifierUri", ((c, s) -> {
            c.setRedirectUris(Collections.singletonList(redirectUri));
            c.setSubjectType("pairwise");
            c.setSectorIdentifierUri(s);
        }), sectorIdentifierUriPattern, "http://localhost:8500/sector-identifier-redirect-uris", INVALID_URL);
    }

    @Test
    public void testLiteralMarkersByAdmin() throws Exception {
        setupPolicy(List.of("\\+", "-", "\\*", "null", SAFE_PATTERN), List.of("webOrigins", "postLogoutRedirectUris"));

        String clientId = generateSuffixedName("literal-markers");
        String id = createClientByAdmin(realm, clientId, OIDCLoginProtocol.LOGIN_PROTOCOL, client -> {
            client.setRedirectUris(List.of("https://untrusted.com/callback"));
            client.setWebOrigins(List.of("+", "*", "null"));
            client.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##-##*##" + VALID_URL);
        });
        updateClientByAdmin(realm, id, client -> {
            client.setWebOrigins(List.of("+"));
            client.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "-");
        });

        ClientPolicyException origins = Assertions.assertThrows(ClientPolicyException.class, () ->
                updateClientByAdmin(realm, id, client -> client.setWebOrigins(List.of("+", INVALID_URL))));
        Assertions.assertEquals("Invalid webOrigins", origins.getErrorDetail());
        ClientPolicyException logout = Assertions.assertThrows(ClientPolicyException.class, () ->
                updateClientByAdmin(realm, id, client -> client.getAttributes().put(
                        OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##-##" + INVALID_URL)));
        Assertions.assertEquals("Invalid postLogoutRedirectUris", logout.getErrorDetail());
    }

    @Test
    public void testReferencesRequireMatchingLiteralPatterns() throws Exception {
        setupPolicy(List.of(SAFE_PATTERN), List.of("webOrigins", "postLogoutRedirectUris"));
        String clientId = generateSuffixedName("explicit-values");
        String id = createClientByAdmin(realm, clientId, OIDCLoginProtocol.LOGIN_PROTOCOL, client -> {
            client.setRedirectUris(List.of(VALID_URL));
            client.setWebOrigins(List.of("https://trusted.com"));
            client.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, VALID_URL);
        });
        for (String field : List.of("webOrigins", "postLogoutRedirectUris")) {
            BiConsumer<ClientRepresentation, String> setter = field.equals("webOrigins")
                    ? (client, value) -> client.setWebOrigins(List.of(value))
                    : (client, value) -> client.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, value);
            ClientPolicyException create = Assertions.assertThrows(ClientPolicyException.class, () ->
                    createClientByAdmin(realm, generateSuffixedName("unmatched-marker"), OIDCLoginProtocol.LOGIN_PROTOCOL, client -> {
                        client.setRedirectUris(List.of(VALID_URL));
                        client.setWebOrigins(List.of("https://trusted.com"));
                        client.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, VALID_URL);
                        setter.accept(client, "+");
                    }));
            Assertions.assertEquals("Invalid " + field, create.getErrorDetail());
            ClientPolicyException update = Assertions.assertThrows(ClientPolicyException.class, () ->
                    updateClientByAdmin(realm, id, client -> setter.accept(client, "+")));
            Assertions.assertEquals("Invalid " + field, update.getErrorDetail());
        }
    }

    @Test
    public void testInvalidPatternConfiguration() throws Exception {
        setupPolicy(List.of("("), null);

        String allFieldsClient = generateSuffixedName("invalid-config");

        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class, ()
                -> createClientByAdmin(realm, allFieldsClient, OIDCLoginProtocol.LOGIN_PROTOCOL, (ClientRepresentation c) -> {
                    c.setRootUrl("invalid-url");
                }));
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, cpe.getError());
    }

    @Test
    public void testEmptyPatternConfiguration() throws Exception {
        setupPolicy(Collections.emptyList(), null);

        String allFieldsClient = generateSuffixedName("invalid-config");

        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class, ()
                -> createClientByAdmin(realm, allFieldsClient, OIDCLoginProtocol.LOGIN_PROTOCOL, (ClientRepresentation c) -> {
                    c.setRootUrl("invalid-url");
                }));
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, cpe.getError());
    }

    public void testFieldDynamically(ClientRegistration reg, String fieldName, BiConsumer<OIDCClientRepresentation, String> setter) throws Exception {
        testFieldDynamically(reg, fieldName, setter, SAFE_PATTERN, VALID_URL, INVALID_URL);
    }

    public void testFieldDynamically(ClientRegistration reg, String fieldName, BiConsumer<OIDCClientRepresentation, String> setter, String pattern, String validUrl, String invalidUrl) throws Exception {
        setupPolicy(List.of(pattern), List.of(fieldName));

        //create with valid field
        String validClientId = createClientDynamically(realm, reg, generateSuffixedName("valid-" + fieldName), (OIDCClientRepresentation c) -> setter.accept(c, validUrl));

        //create invalid field
        ClientRegistrationException cre = Assertions.assertThrows(ClientRegistrationException.class, ()
                -> createClientDynamically(realm, reg, generateSuffixedName("invalid-" + fieldName), (OIDCClientRepresentation c) -> setter.accept(c, invalidUrl)));
        Assertions.assertEquals("Failed to send request", cre.getMessage());

        //try to update with invalid field
        cre = Assertions.assertThrows(ClientRegistrationException.class, ()
                -> updateClientDynamically(reg, validClientId, (OIDCClientRepresentation c) -> setter.accept(c, invalidUrl)));
        Assertions.assertEquals("Failed to send request", cre.getMessage());
    }

    public void testFieldByAdmin(String fieldName, String protocol, BiConsumer<ClientRepresentation, String> setter) throws Exception {
        setupPolicy(List.of(SAFE_PATTERN), List.of(fieldName));

        //create with valid field
        String validClientId = generateSuffixedName("valid-" + fieldName);
        createClientByAdmin(realm, validClientId, protocol, (ClientRepresentation c) -> setter.accept(c, VALID_URL));

        //create invalid field
        ClientPolicyException cpe = Assertions.assertThrows(ClientPolicyException.class, ()
                -> createClientByAdmin(realm, generateSuffixedName("invalid-" + fieldName), protocol, (ClientRepresentation c) ->  setter.accept(c, INVALID_URL)));
        Assertions.assertEquals("Invalid " + fieldName, cpe.getErrorDetail());

        //try to update with invalid field
        ClientRepresentation cRep = realm.admin().clients().findByClientId(validClientId).get(0);
        cpe = Assertions.assertThrows(ClientPolicyException.class, ()
                -> updateClientByAdmin(realm, cRep.getId(), (ClientRepresentation c) -> setter.accept(c, INVALID_URL)));
        Assertions.assertEquals("Invalid " + fieldName, cpe.getErrorDetail());
    }

    private void setupPolicy(List<String> allowedPatterns, List<String> fieldsToValidate) throws Exception {
        SecureClientUrisPatternExecutor.Configuration executorConfig = new SecureClientUrisPatternExecutor.Configuration();
        executorConfig.setAllowedPatterns(allowedPatterns);
        executorConfig.setClientUriFields(fieldsToValidate);
        setupPolicy(realm, SecureClientUrisPatternExecutorFactory.PROVIDER_ID, executorConfig,
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }

}
