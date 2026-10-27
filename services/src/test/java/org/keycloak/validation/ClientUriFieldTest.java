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
package org.keycloak.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutorFactory;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.DefaultClientRegistrationContext;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicyFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class ClientUriFieldTest {

    @Test
    public void absentAttributesAndNullListEntriesAreSafe() {
        ClientRepresentation client = new ClientRepresentation();
        client.setRedirectUris(Arrays.asList(null, "", "https://localhost/callback"));
        for (ClientUriField field : ClientUriField.values()) {
            assertEquals(field == ClientUriField.REDIRECT_URIS ? List.of("https://localhost/callback") : List.of(), field.getValues(client));
        }
    }

    @Test
    public void multivaluedAttributesPreserveAllUris() {
        ClientRepresentation client = new ClientRepresentation();
        client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##https://untrusted.example/logout##https://localhost/logout"));
        assertEquals(List.of("+", "https://untrusted.example/logout", "https://localhost/logout"),
                ClientUriField.POST_LOGOUT_REDIRECT_URIS.getValues(client));
        assertThrows(ClientRegistrationPolicyException.class,
                () -> ClientUriPolicyTest.trustedHostPolicy().beforeRegister(new DefaultClientRegistrationContext(null, client, null)));
    }

    @Test
    public void redirectReferencesAndDisabledPostLogoutRedirectsAreAccepted() throws Exception {
        ClientRepresentation client = new ClientRepresentation();
        client.setRedirectUris(List.of("https://localhost/callback"));
        client.setWebOrigins(List.of("+"));
        client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##-"));
        assertEquals(List.of("+", "-"), ClientUriField.POST_LOGOUT_REDIRECT_URIS.getValues(client));
        assertEquals(List.of("+"), ClientUriField.WEB_ORIGINS.getValues(client));
        ClientUriPolicyTest.trustedHostPolicy().beforeRegister(new DefaultClientRegistrationContext(null, client, null));
        new SecureClientUrisExecutor(null).executeOnEvent(new AdminClientRegisterContext(client, null));
    }

    @Test
    public void trustedHostsPreservesEmbeddedLogosAndLegacyOrigins() {
        ClientRepresentation client = new ClientRepresentation();
        client.setWebOrigins(List.of("*", "null", "localhost:8080"));
        client.setAttributes(Map.of(OIDCConfigAttributes.LOGO_URI, "data:image/png;base64,aW1hZ2U="));
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
        ClientUriPolicyTest.trustedHostPolicy().beforeRegister(context);
        ClientUriPolicyTest.trustedHostPolicy().beforeUpdate(context, null);
    }

    @Test
    public void legacyWebOriginsRemainSubjectToClientPolicies() {
        for (String origin : List.of(Constants.AUTH_ADMIN_URL_PROP, Constants.AUTH_BASE_URL_PROP, "file://", "FILE://")) {
            ClientRepresentation client = new ClientRepresentation();
            client.setWebOrigins(List.of(origin));
            DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
            assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeRegister(context));
            assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeUpdate(context, null));
            SecureClientUrisExecutor secureUris = new SecureClientUrisExecutor(null);
            assertThrows(ClientPolicyException.class, () -> secureUris.executeOnEvent(new AdminClientRegisterContext(client, null)));
            assertThrows(ClientPolicyException.class, () -> secureUris.executeOnEvent(new AdminClientUpdateContext(client, null, null)));
            SecureClientUrisPatternExecutor pattern = ClientUriPolicyTest.patternPolicy(List.of("webOrigins"));
            assertThrows(ClientPolicyException.class, () -> pattern.executeOnEvent(new AdminClientRegisterContext(client, null)));
            assertThrows(ClientPolicyException.class, () -> pattern.executeOnEvent(new AdminClientUpdateContext(client, null, null)));
        }
    }

    @Test
    public void trustedHostsChecksExplicitUrlsAlongsideEitherPostLogoutMarker() {
        ClientRepresentation client = new ClientRepresentation();
        client.setRedirectUris(List.of("https://localhost/callback"));
        for (String marker : List.of("+", "-")) {
            client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, marker + "##https://untrusted.example/logout"));
            DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
            assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeRegister(context));
            assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeUpdate(context, null));
        }
    }

    @Test
    public void trustedHostsDataLogoExceptionDoesNotApplyToEndpoints() {
        ClientRepresentation client = new ClientRepresentation();
        client.setAttributes(Map.of(OIDCConfigAttributes.JWKS_URL, "data:application/json;base64,e30="));
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
        assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeRegister(context));
        assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeUpdate(context, null));
    }

    @Test
    public void trustedHostsValidatesLegacyNumericOriginsAgainstTheirHost() {
        ComponentModel component = new ComponentModel();
        component.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "false");
        component.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, List.of("127.0.0.1", "[::1]"));
        TrustedHostClientRegistrationPolicy policy = new TrustedHostClientRegistrationPolicy(null, component);
        ClientRepresentation client = new ClientRepresentation();
        client.setWebOrigins(List.of("127.0.0.1:8080", "[::1]:8080"));
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
        policy.beforeRegister(context);
        policy.beforeUpdate(context, null);
        client.setWebOrigins(List.of("127.0.0.2:8080"));
        assertThrows(ClientRegistrationPolicyException.class, () -> policy.beforeRegister(context));
        assertThrows(ClientRegistrationPolicyException.class, () -> policy.beforeUpdate(context, null));
    }

    @Test
    public void relativeLogoutUrisUseTrustedRoot() {
        ClientRepresentation client = new ClientRepresentation();
        client.setRootUrl("https://localhost");
        client.setAttributes(Map.of(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "/logout"));
        ClientUriPolicyTest.trustedHostPolicy().beforeRegister(new DefaultClientRegistrationContext(null, client, null));
    }

    @Test
    public void schemeRelativeUrisCannotHideTheirAuthority() {
        for (String root : Arrays.asList(null, "https://localhost")) {
            ClientRepresentation client = new ClientRepresentation();
            client.setRootUrl(root);
            for (String attribute : List.of(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, OIDCConfigAttributes.LOGO_URI)) {
                client.setAttributes(Map.of(attribute, "//untrusted.example/logout"));
                DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
                assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeRegister(context));
                assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeUpdate(context, null));
            }
            client.setAttributes(Map.of());
            client.setBaseUrl("//untrusted.example/logout");
            DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
            assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeRegister(context));
            assertThrows(ClientRegistrationPolicyException.class, () -> ClientUriPolicyTest.trustedHostPolicy().beforeUpdate(context, null));
        }
    }

    @Test
    public void trustedSchemeRelativeUrisKeepTheirAuthority() {
        for (String root : Arrays.asList(null, "https://localhost")) {
            ClientRepresentation client = new ClientRepresentation();
            client.setRootUrl(root);
            client.setAttributes(Map.of(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "//localhost/logout"));
            ClientUriPolicyTest.trustedHostPolicy().beforeRegister(new DefaultClientRegistrationContext(null, client, null));
        }
    }

    @Test
    public void specialValuesAreSpecificToTheirField() {
        ClientRepresentation client = new ClientRepresentation();
        client.setAttributes(Map.of(OIDCConfigAttributes.REQUEST_URIS, "+"));
        assertThrows(ClientRegistrationPolicyException.class,
                () -> ClientUriPolicyTest.trustedHostPolicy().beforeRegister(new DefaultClientRegistrationContext(null, client, null)));
    }

    @Test
    public void doubleSlashRedirectPathsStillUseTheTrustedRoot() {
        ClientRepresentation client = new ClientRepresentation();
        client.setRootUrl("https://localhost");
        client.setRedirectUris(List.of("//callback/path"));
        client.setAttributes(Map.of(
                OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "//logout/path",
                OIDCConfigAttributes.REQUEST_URIS, "//request/path",
                OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "//backchannel/path",
                OIDCConfigAttributes.JWKS_URL, "//keys/path"));
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
        ClientUriPolicyTest.trustedHostPolicy().beforeRegister(context);
        ClientUriPolicyTest.trustedHostPolicy().beforeUpdate(context, null);
    }

    @Test
    public void trustedHostUpdatesUseValidationHook() {
        ComponentModel component = new ComponentModel();
        component.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "false");
        TrustedHostClientRegistrationPolicy policy = new TrustedHostClientRegistrationPolicy(null, component) {
            @Override
            protected void verifyClientUrls(ClientRegistrationContext context) {
                throw new ClientRegistrationPolicyException("Custom URI validation");
            }
        };
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, new ClientRepresentation(), null);
        assertEquals("Custom URI validation", assertThrows(ClientRegistrationPolicyException.class,
                () -> policy.beforeUpdate(context, null)).getMessage());
    }

    @Test
    public void trustedHostValidationOverrideCanAcceptCustomUris() {
        ComponentModel component = new ComponentModel();
        component.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "false");
        ClientRepresentation client = new ClientRepresentation();
        client.setBaseUrl("https://custom.example/home");
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
        AtomicInteger invocations = new AtomicInteger();
        TrustedHostClientRegistrationPolicy policy = new TrustedHostClientRegistrationPolicy(null, component) {
            @Override
            protected void verifyClientUrls(ClientRegistrationContext actualContext) {
                assertSame(context, actualContext);
                invocations.incrementAndGet();
            }
        };
        for (String protocol : Arrays.asList(null, "openid-connect", "saml")) {
            client.setProtocol(protocol);
            policy.beforeRegister(context);
            policy.beforeUpdate(context, ClientUriPolicyTest.existingClient("saml"));
            assertEquals(protocol, client.getProtocol());
        }
        assertEquals(6, invocations.get());
    }

    @Test
    public void patternSelectionLeavesOtherFieldsUnrestricted() throws Exception {
        ClientRepresentation client = new ClientRepresentation();
        client.setBaseUrl("https://untrusted.example/home");
        client.setAttributes(Map.of(OIDCConfigAttributes.LOGO_URI, "https://localhost/logo"));
        SecureClientUrisPatternExecutor executor = ClientUriPolicyTest.patternPolicy(List.of("logoUri"));
        assertEquals(SecureClientUrisPatternExecutorFactory.PROVIDER_ID, executor.getProviderId());
        executor.executeOnEvent(new AdminClientRegisterContext(client, null));
    }

    @Test
    public void localhostHttpExceptionOnlyAppliesToAuthorizationRequests() throws Exception {
        SecureClientUrisExecutor executor = new SecureClientUrisExecutor(null);
        SecureClientUrisExecutor.Configuration config = new SecureClientUrisExecutor.Configuration();
        config.setAllowHttpOnLocalhost(true);
        executor.setupConfiguration(config);
        ClientRepresentation client = new ClientRepresentation();
        client.setRedirectUris(List.of("http://localhost/callback"));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientRegisterContext(client, null)));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientUpdateContext(client, null, null)));
        executor.executeOnEvent(new AuthorizationRequestContext(null, null, "http://localhost/callback", null, null));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(
                new AuthorizationRequestContext(null, null, "http://localhost.example/callback", null, null)));
        config.setAllowHttpOnLocalhost(false);
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(
                new AuthorizationRequestContext(null, null, "http://localhost/callback", null, null)));
    }

    @Test
    public void patternUnknownFieldsAndInvalidExpressionsKeepTheirMeaning() throws Exception {
        ClientRepresentation client = new ClientRepresentation();
        client.setBaseUrl("/home");
        ClientUriPolicyTest.patternPolicy(List.of("unknown-field")).executeOnEvent(new AdminClientRegisterContext(client, null));
        SecureClientUrisPatternExecutor executor = new SecureClientUrisPatternExecutor(null);
        SecureClientUrisPatternExecutor.Configuration config = new SecureClientUrisPatternExecutor.Configuration();
        config.setClientUriFields(List.of("baseUrl"));
        config.setAllowedPatterns(List.of("[", "/home"));
        executor.setupConfiguration(config);
        executor.executeOnEvent(new AdminClientRegisterContext(client, null));
        config.setAllowedPatterns(List.of("["));
        executor.setupConfiguration(config);
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientRegisterContext(client, null)));
    }

    @Test
    public void patternPreservesConfiguredErrorOrder() {
        ClientRepresentation client = new ClientRepresentation();
        client.setRootUrl("https://untrusted.example");
        client.setAttributes(Map.of(OIDCConfigAttributes.LOGO_URI, "https://untrusted.example/logo"));
        SecureClientUrisPatternExecutor executor = ClientUriPolicyTest.patternPolicy(List.of("logoUri", "rootUrl"));
        ClientPolicyException error = assertThrows(ClientPolicyException.class,
                () -> executor.executeOnEvent(new AdminClientRegisterContext(client, null)));
        assertEquals("Invalid logoUri", error.getErrorDetail());
    }

    @Test
    public void patternMatchesLiteralWebOrigins() throws Exception {
        ClientRepresentation client = new ClientRepresentation();
        client.setRedirectUris(List.of("https://untrusted.example/callback"));
        client.setWebOrigins(List.of("+", "*", "null"));
        SecureClientUrisPatternExecutor executor = literalMarkerPattern(List.of("webOrigins"));
        executor.executeOnEvent(new AdminClientRegisterContext(client, null));
        executor.executeOnEvent(new AdminClientUpdateContext(client, null, null));
        client.setWebOrigins(List.of("+", "https://untrusted.example"));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientRegisterContext(client, null)));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientUpdateContext(client, null, null)));
    }

    @Test
    public void patternMatchesLiteralPostLogoutUris() throws Exception {
        ClientRepresentation client = new ClientRepresentation();
        client.setRedirectUris(List.of("https://untrusted.example/callback"));
        client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##-##*##https://localhost/logout"));
        SecureClientUrisPatternExecutor executor = literalMarkerPattern(List.of("postLogoutRedirectUris"));
        executor.executeOnEvent(new AdminClientRegisterContext(client, null));
        executor.executeOnEvent(new AdminClientUpdateContext(client, null, null));
        client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##-##https://untrusted.example/logout"));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientRegisterContext(client, null)));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientUpdateContext(client, null, null)));
    }

    @Test
    public void patternDoesNotResolveOrSkipLiteralMarkers() {
        ClientRepresentation client = new ClientRepresentation();
        client.setRedirectUris(List.of("https://localhost/callback"));
        client.setWebOrigins(List.of("+"));
        SecureClientUrisPatternExecutor origins = ClientUriPolicyTest.patternPolicy(List.of("webOrigins"));
        assertThrows(ClientPolicyException.class, () -> origins.executeOnEvent(new AdminClientRegisterContext(client, null)));
        assertThrows(ClientPolicyException.class, () -> origins.executeOnEvent(new AdminClientUpdateContext(client, null, null)));
        SecureClientUrisPatternExecutor logout = ClientUriPolicyTest.patternPolicy(List.of("postLogoutRedirectUris"));
        for (String marker : List.of("+", "-", "*")) {
            client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, marker));
            assertThrows(ClientPolicyException.class, () -> logout.executeOnEvent(new AdminClientRegisterContext(client, null)));
            assertThrows(ClientPolicyException.class, () -> logout.executeOnEvent(new AdminClientUpdateContext(client, null, null)));
        }
    }

    private static SecureClientUrisPatternExecutor literalMarkerPattern(List<String> fields) {
        SecureClientUrisPatternExecutor executor = new SecureClientUrisPatternExecutor(null);
        SecureClientUrisPatternExecutor.Configuration configuration = new SecureClientUrisPatternExecutor.Configuration();
        configuration.setAllowedPatterns(List.of("\\+", "-", "\\*", "null", "https://localhost/.*"));
        configuration.setClientUriFields(fields);
        executor.setupConfiguration(configuration);
        return executor;
    }
}
