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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdatedContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;
import org.keycloak.services.clientpolicy.executor.SamlSecureClientUrisExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisPatternExecutor;
import org.keycloak.services.clientregistration.DefaultClientRegistrationContext;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicyFactory;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/** Exercise the same client metadata through the registration and client policy entry points. */
@RunWith(Parameterized.class)
public class ClientUriPolicyTest {

    private static final String TRUSTED = "https://localhost/callback";
    private static final String UNTRUSTED = "https://untrusted.example/callback";

    @Parameterized.Parameters(name = "{0} ({1})")
    public static List<Object[]> fields() {
        List<Object[]> fields = new ArrayList<>();
        fields.add(field("rootUrl", ClientRepresentation::setRootUrl));
        fields.add(field("baseUrl", ClientRepresentation::setBaseUrl));
        fields.add(field("adminUrl", ClientRepresentation::setAdminUrl));
        fields.add(field("redirectUris", (c, u) -> c.setRedirectUris(List.of(u))));
        fields.add(field("webOrigins", (c, u) -> c.setWebOrigins(List.of(u))));
        fields.add(attribute("jwksUri", OIDCConfigAttributes.JWKS_URL));
        fields.add(attribute("requestUris", OIDCConfigAttributes.REQUEST_URIS));
        fields.add(attribute("backchannelLogoutUrl", OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL));
        fields.add(attribute("frontChannelLogoutUrl", OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI));
        fields.add(attribute("postLogoutRedirectUris", OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS));
        fields.add(attribute("cibaClientNotificationEndpoint", CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT));
        fields.add(attribute("logoUri", OIDCConfigAttributes.LOGO_URI));
        fields.add(attribute("policyUri", OIDCConfigAttributes.POLICY_URI));
        fields.add(attribute("tosUri", OIDCConfigAttributes.TOS_URI));
        fields.add(attribute("sectorIdentifierUri", OIDCConfigAttributes.SECTOR_IDENTIFIER_URI));
        fields.add(new Object[]{"adminUrl", SamlProtocol.LOGIN_PROTOCOL, (BiConsumer<ClientRepresentation, String>) ClientRepresentation::setAdminUrl});
        for (String attribute : List.of(
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE,
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE,
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE,
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE,
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE,
                SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE,
                SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL)) {
            Object[] field = attribute(attribute, attribute);
            field[1] = SamlProtocol.LOGIN_PROTOCOL;
            fields.add(field);
        }
        return fields;
    }

    private static Object[] field(String name, BiConsumer<ClientRepresentation, String> setter) {
        return new Object[]{name, "openid-connect", setter};
    }

    private static Object[] attribute(String name, String attribute) {
        return field(name, (c, u) -> c.setAttributes(Map.of(attribute, u)));
    }

    private final String field;
    private final String protocol;
    private final BiConsumer<ClientRepresentation, String> setter;

    public ClientUriPolicyTest(String field, String protocol, BiConsumer<ClientRepresentation, String> setter) {
        this.field = field;
        this.protocol = protocol;
        this.setter = setter;
    }

    @Test
    public void catalogReadsTheSameLiteralValuesFromModelsAndRepresentations() {
        ClientRepresentation representation = client(TRUSTED);
        ClientModel model = clientModel(representation);
        ClientUriField selected = ClientUriField.fromName(field).orElseThrow();
        for (ClientUriField candidate : ClientUriField.values()) {
            List<String> expected = candidate == selected ? List.of(TRUSTED) : List.of();
            assertEquals(expected, candidate.getValues(representation));
            assertEquals(expected, candidate.getValues(model));
        }
    }

    @Test
    public void samlExecutorChecksEveryConfiguredUriOnCreateAndUpdate() throws Exception {
        ClientRepresentation representation = client(TRUSTED);
        representation.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        ClientModel model = clientModel(representation);
        SamlSecureClientUrisExecutor executor = new SamlSecureClientUrisExecutor(null);
        executor.setupConfiguration(new SamlSecureClientUrisExecutor.Configuration(false));
        executor.executeOnEvent(new AdminClientRegisteredContext(model, null));
        executor.executeOnEvent(new AdminClientUpdatedContext(null, model, null));
        setter.accept(representation, "http://localhost/callback");
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientRegisteredContext(model, null)));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientUpdatedContext(null, model, null)));
    }

    private ClientModel clientModel(ClientRepresentation representation) {
        return (ClientModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ClientModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getProtocol" -> representation.getProtocol();
                    case "getRootUrl" -> representation.getRootUrl();
                    case "getManagementUrl" -> representation.getAdminUrl();
                    case "getBaseUrl" -> representation.getBaseUrl();
                    case "getRedirectUris" -> representation.getRedirectUris() == null ? null : Set.copyOf(representation.getRedirectUris());
                    case "getWebOrigins" -> representation.getWebOrigins() == null ? null : Set.copyOf(representation.getWebOrigins());
                    case "getAttribute" -> representation.getAttributes() == null ? null : representation.getAttributes().get(args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    public void trustedHostAcceptsTrustedUri() {
        ClientRepresentation client = client(TRUSTED);
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client, null);
        trustedHostPolicy().beforeRegister(context);
        trustedHostPolicy().beforeUpdate(context, null);
    }

    @Test
    public void trustedHostsAppliesItsFieldScopeOnCreateAndUpdate() {
        DefaultClientRegistrationContext context = new DefaultClientRegistrationContext(null, client(UNTRUSTED), null);
        assertTrustedHostViolation(() -> trustedHostPolicy().beforeRegister(context));
        assertTrustedHostViolation(() -> trustedHostPolicy().beforeUpdate(context, null));
    }

    @Test
    public void secureUrisAcceptsHttps() throws ClientPolicyException {
        SecureClientUrisExecutor executor = new SecureClientUrisExecutor(null);
        executor.executeOnEvent(new AdminClientRegisterContext(client(TRUSTED), null));
        executor.executeOnEvent(new AdminClientUpdateContext(client(TRUSTED), null, null));
    }

    @Test
    public void secureUrisRejectsHttpForEveryFieldOnCreateAndUpdate() {
        SecureClientUrisExecutor executor = new SecureClientUrisExecutor(null);
        assertSecureUriViolation(() -> executor.executeOnEvent(new AdminClientRegisterContext(client("http://localhost/callback"), null)));
        assertSecureUriViolation(() -> executor.executeOnEvent(new AdminClientUpdateContext(client("http://localhost/callback"), null, null)));
    }

    @Test
    public void secureUrisRejectsWildcardsForEveryField() {
        SecureClientUrisExecutor executor = new SecureClientUrisExecutor(null);
        assertSecureUriViolation(() -> executor.executeOnEvent(new AdminClientRegisterContext(client(TRUSTED + "/*"), null)));
    }

    @Test
    public void patternAcceptsMatchingUri() throws ClientPolicyException {
        SecureClientUrisPatternExecutor executor = patternPolicy(null);
        executor.executeOnEvent(new AdminClientRegisterContext(client(TRUSTED), null));
        executor.executeOnEvent(new AdminClientUpdateContext(client(TRUSTED), null, null));
    }

    @Test
    public void patternRejectsUnmatchedUriOnCreateAndUpdate() {
        SecureClientUrisPatternExecutor executor = patternPolicy(null);
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientRegisterContext(client(UNTRUSTED), null)));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientUpdateContext(client(UNTRUSTED), null, null)));
    }

    @Test
    public void patternHonorsConfiguredFieldNames() {
        SecureClientUrisPatternExecutor executor = patternPolicy(List.of(field));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientRegisterContext(client(UNTRUSTED), null)));
    }

    @Test
    public void updatesWithoutProtocolAcceptTrustedUris() throws Exception {
        ClientRepresentation proposed = client(TRUSTED);
        proposed.setProtocol(null);
        ClientModel target = existingClient(protocol);
        DefaultClientRegistrationContext registration = new DefaultClientRegistrationContext(null, proposed, null);
        trustedHostPolicy().beforeUpdate(registration, target);
        for (ClientCRUDContext context : List.of(new AdminClientUpdateContext(proposed, target, null),
                new DynamicClientUpdateContext(registration, target, null, null))) {
            new SecureClientUrisExecutor(null).executeOnEvent(context);
            patternPolicy(List.of(field)).executeOnEvent(context);
        }
        assertNull(proposed.getProtocol());
    }

    @Test
    public void updatesWithoutProtocolPreservePolicyScope() {
        ClientRepresentation proposed = client(UNTRUSTED);
        proposed.setProtocol(null);
        ClientModel target = existingClient(protocol);
        DefaultClientRegistrationContext registration = new DefaultClientRegistrationContext(null, proposed, null);
        assertTrustedHostViolation(() -> trustedHostPolicy().beforeUpdate(registration, target));
        for (ClientCRUDContext context : List.of(new AdminClientUpdateContext(proposed, target, null),
                new DynamicClientUpdateContext(registration, target, null, null))) {
            assertThrows(ClientPolicyException.class, () -> patternPolicy(List.of(field)).executeOnEvent(context));
            setter.accept(proposed, "http://localhost/callback");
            assertSecureUriViolation(() -> new SecureClientUrisExecutor(null).executeOnEvent(context));
            setter.accept(proposed, UNTRUSTED);
        }
        assertNull(proposed.getProtocol());
    }

    @Test
    public void protocolChangesPreservePolicyScope() {
        ClientRepresentation proposed = client(UNTRUSTED);
        ClientModel target = existingClient(SamlProtocol.LOGIN_PROTOCOL.equals(protocol) ? "openid-connect" : SamlProtocol.LOGIN_PROTOCOL);
        DefaultClientRegistrationContext registration = new DefaultClientRegistrationContext(null, proposed, null);
        assertTrustedHostViolation(() -> trustedHostPolicy().beforeUpdate(registration, target));
        for (ClientCRUDContext context : List.of(new AdminClientUpdateContext(proposed, target, null),
                new DynamicClientUpdateContext(registration, target, null, null))) {
            assertThrows(ClientPolicyException.class, () -> patternPolicy(List.of(field)).executeOnEvent(context));
            setter.accept(proposed, "http://localhost/callback");
            assertSecureUriViolation(() -> new SecureClientUrisExecutor(null).executeOnEvent(context));
            setter.accept(proposed, UNTRUSTED);
        }
        assertEquals(protocol, proposed.getProtocol());
    }

    @Test
    public void policiesValidateTheirSelectedFieldsRegardlessOfProtocol() {
        String otherProtocol = SamlProtocol.LOGIN_PROTOCOL.equals(protocol) ? "openid-connect" : SamlProtocol.LOGIN_PROTOCOL;
        for (String proposedProtocol : Arrays.asList(null, otherProtocol)) {
            ClientRepresentation proposed = client(UNTRUSTED);
            proposed.setProtocol(proposedProtocol);
            ClientModel target = existingClient(otherProtocol);
            DefaultClientRegistrationContext registration = new DefaultClientRegistrationContext(null, proposed, null);
            assertTrustedHostViolation(() -> trustedHostPolicy().beforeRegister(registration));
            assertTrustedHostViolation(() -> trustedHostPolicy().beforeUpdate(registration, target));
            for (ClientCRUDContext context : List.of(new AdminClientRegisterContext(proposed, null),
                    new DynamicClientRegisterContext(registration, null, null),
                    new AdminClientUpdateContext(proposed, target, null),
                    new DynamicClientUpdateContext(registration, target, null, null))) {
                assertThrows(ClientPolicyException.class, () -> patternPolicy(List.of(field)).executeOnEvent(context));
                setter.accept(proposed, "http://localhost/callback");
                assertSecureUriViolation(() -> new SecureClientUrisExecutor(null).executeOnEvent(context));
                setter.accept(proposed, UNTRUSTED);
            }
            assertEquals(proposedProtocol, proposed.getProtocol());
        }
    }

    private void assertTrustedHostViolation(ThrowingRunnable action) {
        assertThrows(ClientRegistrationPolicyException.class, action);
    }

    private void assertSecureUriViolation(ThrowingRunnable action) {
        assertThrows(ClientPolicyException.class, action);
    }

    static ClientModel existingClient(String protocol) {
        return (ClientModel) Proxy.newProxyInstance(ClientUriPolicyTest.class.getClassLoader(), new Class<?>[]{ClientModel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getProtocol")) {
                        return protocol;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private ClientRepresentation client(String uri) {
        ClientRepresentation client = new ClientRepresentation();
        client.setProtocol(protocol);
        setter.accept(client, uri);
        return client;
    }

    static TrustedHostClientRegistrationPolicy trustedHostPolicy() {
        ComponentModel component = new ComponentModel();
        component.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "false");
        component.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, "localhost");
        return new TrustedHostClientRegistrationPolicy(null, component);
    }

    static SecureClientUrisPatternExecutor patternPolicy(List<String> fields) {
        SecureClientUrisPatternExecutor executor = new SecureClientUrisPatternExecutor(null);
        SecureClientUrisPatternExecutor.Configuration configuration = new SecureClientUrisPatternExecutor.Configuration();
        configuration.setAllowedPatterns(List.of("https://localhost/.*"));
        configuration.setClientUriFields(fields);
        executor.setupConfiguration(configuration);
        return executor;
    }
}
