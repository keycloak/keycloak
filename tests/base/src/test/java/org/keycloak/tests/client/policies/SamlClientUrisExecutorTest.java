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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SamlSecureClientUrisExecutor;
import org.keycloak.services.clientpolicy.executor.SamlSecureClientUrisExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class SamlClientUrisExecutorTest extends AbstractClientPoliciesTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void metadataDescriptorRequiresHttpsOnCreateAndUpdate() throws Exception {
        setupPolicy(false);
        assertThrows(ClientPolicyException.class, () -> createClientByAdmin(realm,
                generateSuffixedName("http-metadata"), SamlProtocol.LOGIN_PROTOCOL, client -> {
                    client.setRedirectUris(List.of("https://localhost/callback"));
                    client.getAttributes().put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "http://localhost/metadata");
                }));
        String id = createClientByAdmin(realm, generateSuffixedName("https-metadata"), SamlProtocol.LOGIN_PROTOCOL, client -> {
            client.setRedirectUris(List.of("https://localhost/callback"));
            client.getAttributes().put(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "https://localhost/metadata");
        });
        updateClientByAdmin(realm, id, client -> client.setDescription("An unrelated update"));
        ClientResource resource = realm.admin().clients().get(id);
        ClientRepresentation update = new ClientRepresentation();
        update.setAttributes(Map.of(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "http://localhost/metadata"));
        assertThrows(BadRequestException.class, () -> resource.update(update));
        assertEquals("https://localhost/metadata", resource.toRepresentation().getAttributes().get(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL));
    }

    @Test
    public void samlEndpointsRequireHttpsOnCreateAndPartialUpdate() throws Exception {
        setupPolicy(false);
        assertThrows(ClientPolicyException.class, () -> createClientByAdmin(realm,
                generateSuffixedName("http-acs"), SamlProtocol.LOGIN_PROTOCOL, client -> {
                    client.setRedirectUris(List.of("https://localhost/callback"));
                    client.getAttributes().put(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "http://localhost/acs");
                }));
        String id = createClientByAdmin(realm, generateSuffixedName("https-acs"), SamlProtocol.LOGIN_PROTOCOL,
                client -> client.setRedirectUris(List.of("https://localhost/callback")));
        ClientResource resource = realm.admin().clients().get(id);
        ClientRepresentation update = new ClientRepresentation();
        update.setAttributes(Map.of(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "http://localhost/acs"));
        assertThrows(BadRequestException.class, () -> resource.update(update));
        assertNotEquals("http://localhost/acs", resource.toRepresentation().getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE));
        update.setAttributes(Map.of(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "https://localhost/acs"));
        resource.update(update);
        assertEquals("https://localhost/acs", resource.toRepresentation().getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE));
    }

    @Test
    public void wildcardRedirectsRemainConfigurable() throws Exception {
        setupPolicy(false);
        assertThrows(ClientPolicyException.class, () -> createClientByAdmin(realm,
                generateSuffixedName("wildcard-rejected"), SamlProtocol.LOGIN_PROTOCOL,
                client -> client.setRedirectUris(List.of("https://localhost/*"))));
        setupPolicy(true);
        String id = createClientByAdmin(realm, generateSuffixedName("wildcard-allowed"), SamlProtocol.LOGIN_PROTOCOL,
                client -> client.setRedirectUris(List.of("https://localhost/*")));
        updateClientByAdmin(realm, id, client -> client.setRedirectUris(List.of("https://localhost/updated/*")));
    }

    @Test
    public void genericHttpsPolicyValidatesAllAdditionalUriAttributes() throws Exception {
        setupPolicy(realm, SecureClientUrisExecutorFactory.PROVIDER_ID, new SecureClientUrisExecutor.Configuration(),
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
        for (String protocol : List.of(OIDCLoginProtocol.LOGIN_PROTOCOL, SamlProtocol.LOGIN_PROTOCOL)) {
            for (String attribute : List.of(
                    OIDCConfigAttributes.SECTOR_IDENTIFIER_URI,
                    SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL,
                    SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                    SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE,
                    SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE,
                    SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                    SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE,
                    SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE,
                    SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE,
                    SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE)) {
                ClientPolicyException error = assertThrows(ClientPolicyException.class, () -> createClientByAdmin(realm,
                        generateSuffixedName("http-additional-uri"), protocol, client -> {
                            client.setRedirectUris(List.of("https://localhost/callback"));
                            client.setAttributes(Map.of(attribute, "http://localhost/endpoint"));
                        }));
                assertEquals("Invalid " + attribute, error.getErrorDetail());
                String id = createClientByAdmin(realm, generateSuffixedName("https-additional-uri"), protocol, client -> {
                    client.setRedirectUris(List.of("https://localhost/callback"));
                    client.setAttributes(Map.of(attribute, "https://localhost/endpoint"));
                });
                ClientResource resource = realm.admin().clients().get(id);
                ClientRepresentation update = new ClientRepresentation();
                update.setAttributes(Map.of(attribute, "http://localhost/updated"));
                assertThrows(BadRequestException.class, () -> resource.update(update));
                assertEquals("https://localhost/endpoint", resource.toRepresentation().getAttributes().get(attribute));
                update.setAttributes(Map.of(attribute, "https://localhost/*"));
                assertThrows(BadRequestException.class, () -> resource.update(update));
                assertEquals("https://localhost/endpoint", resource.toRepresentation().getAttributes().get(attribute));
                update.setAttributes(Map.of(attribute, "https://localhost/updated"));
                resource.update(update);
                assertEquals("https://localhost/updated", resource.toRepresentation().getAttributes().get(attribute));
                assertEquals(protocol, resource.toRepresentation().getProtocol());
            }
        }
    }

    private void setupPolicy(boolean allowWildcards) throws Exception {
        setupPolicy(realm, SamlSecureClientUrisExecutorFactory.PROVIDER_ID,
                new SamlSecureClientUrisExecutor.Configuration(allowWildcards),
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }
}
