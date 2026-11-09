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
package org.keycloak.services.clientpolicy.executor;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.OAuthErrorException;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdatedContext;
import org.keycloak.services.clientpolicy.context.SamlAuthnRequestContext;
import org.keycloak.services.clientpolicy.context.SamlLogoutRequestContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SamlSecureClientUrisExecutorTest {

    @Test
    public void metadataUrlIsCheckedOnRegistrationAndUpdate() throws Exception {
        TestClient client = new TestClient();
        SamlSecureClientUrisExecutor executor = executor(false);
        client.attributes.put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "https://localhost/metadata");
        executor.executeOnEvent(new AdminClientRegisteredContext(client.model(), null));
        executor.executeOnEvent(new AdminClientUpdatedContext(null, client.model(), null));
        client.attributes.put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "http://localhost/metadata");
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientRegisteredContext(client.model(), null)));
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(new AdminClientUpdatedContext(null, client.model(), null)));
    }

    @Test
    public void wildcardConfigurationIsPreserved() throws Exception {
        TestClient client = new TestClient();
        client.redirects = Set.of("https://localhost/*");
        ClientPolicyException error = assertThrows(ClientPolicyException.class,
                () -> executor(false).executeOnEvent(new AdminClientRegisteredContext(client.model(), null)));
        assertEquals("Unsecure wildcard redirect https://localhost/* for Valid redirect URIs", error.getErrorDetail());
        executor(true).executeOnEvent(new AdminClientRegisteredContext(client.model(), null));
        executor(true).executeOnEvent(new AdminClientUpdatedContext(null, client.model(), null));
    }

    @Test
    public void loginChecksAllConfiguredUris() throws Exception {
        TestClient client = new TestClient();
        client.attributes.put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "https://localhost/metadata");
        SamlAuthnRequestContext context = new SamlAuthnRequestContext(new AuthnRequestType("request-id", null), client.model(), SamlProtocol.SAML_POST_BINDING);
        SamlSecureClientUrisExecutor executor = executor(false);
        executor.executeOnEvent(context);
        client.attributes.put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "http://localhost/metadata");
        ClientPolicyException error = assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(context));
        assertEquals(OAuthErrorException.INVALID_REQUEST, error.getError());
        assertEquals("Non secure scheme for Metadata descriptor URL", error.getErrorDetail());
    }

    @Test
    public void explicitLoginEndpointDoesNotBypassConfiguredUriChecks() throws Exception {
        TestClient client = new TestClient();
        client.attributes.put(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "http://localhost/fallback");
        AuthnRequestType request = new AuthnRequestType("request-id", null);
        request.setAssertionConsumerServiceURL(URI.create("https://localhost/login"));
        SamlAuthnRequestContext context = new SamlAuthnRequestContext(request, client.model(), SamlProtocol.SAML_POST_BINDING);
        SamlSecureClientUrisExecutor executor = executor(false);
        assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(context));
        client.attributes.clear();
        executor.executeOnEvent(context);
        request.setAssertionConsumerServiceURL(URI.create("http://localhost/login"));
        ClientPolicyException error = assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(context));
        assertEquals("Non secure scheme for AssertionConsumerServiceURL", error.getErrorDetail());
    }

    @Test
    public void logoutChecksAllConfiguredUris() throws Exception {
        TestClient client = new TestClient();
        client.attributes.put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "https://localhost/metadata");
        SamlLogoutRequestContext context = new SamlLogoutRequestContext(null, client.model(), SamlProtocol.SAML_POST_BINDING);
        SamlSecureClientUrisExecutor executor = executor(false);
        executor.executeOnEvent(context);
        client.attributes.put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "http://localhost/metadata");
        ClientPolicyException error = assertThrows(ClientPolicyException.class, () -> executor.executeOnEvent(context));
        assertEquals(OAuthErrorException.INVALID_REQUEST, error.getError());
        assertEquals("Non secure scheme for Metadata descriptor URL", error.getErrorDetail());
    }

    private static SamlSecureClientUrisExecutor executor(boolean allowWildcards) {
        SamlSecureClientUrisExecutor executor = new SamlSecureClientUrisExecutor(null);
        executor.setupConfiguration(new SamlSecureClientUrisExecutor.Configuration(allowWildcards));
        return executor;
    }

    private static class TestClient {
        final Map<String, String> attributes = new HashMap<>();
        Set<String> redirects = Set.of("https://localhost/callback");

        ClientModel model() {
            return (ClientModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ClientModel.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getProtocol" -> SamlProtocol.LOGIN_PROTOCOL;
                        case "getRootUrl", "getBaseUrl", "getManagementUrl" -> null;
                        case "getRedirectUris" -> redirects;
                        case "getWebOrigins" -> Set.of();
                        case "getAttributes" -> attributes;
                        case "getAttribute" -> attributes.get(args[0]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
