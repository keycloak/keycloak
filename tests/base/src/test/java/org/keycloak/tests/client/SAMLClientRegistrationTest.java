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

package org.keycloak.tests.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.util.ArtifactBindingUtils;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class SAMLClientRegistrationTest extends AbstractClientRegistrationTest {

    @InjectOAuthClient(ref = "saml-oauth", lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectClient(config = OIDCClient.class)
    ManagedClient oidcClient;

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = managedRealm.admin().clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    @Test
    public void createClient() throws ClientRegistrationException, IOException {
        String entityDescriptor = IOUtils.toString(getClass().getResourceAsStream("/org/keycloak/tests/client/clientreg-test/saml-entity-descriptor.xml"), Charset.defaultCharset());
        assertClientCreation(entityDescriptor);
    }

    @Test
    public void testSAMLEndpointCreateWithOIDCClient() throws Exception {
        ClientsResource clientsResource = managedRealm.admin().clients();
        ClientRepresentation oidcClient = clientsResource.findByClientId("oidc-client").get(0);
        String oidcClientServiceId = clientsResource.get(oidcClient.getId()).getServiceAccountUser().getId();

        String realmManagementId = clientsResource.findByClientId("realm-management").get(0).getId();
        RoleRepresentation role = clientsResource.get(realmManagementId).roles().get("create-client").toRepresentation();

        managedRealm.admin().users().get(oidcClientServiceId).roles().clientLevel(realmManagementId).add(Arrays.asList(role));

        String accessToken = oauth.client("oidc-client", "secret").doClientCredentialsGrantAccessTokenRequest().getAccessToken();
        reg.auth(Auth.token(accessToken));

        String entityDescriptor = IOUtils.toString(getClass().getResourceAsStream("/org/keycloak/tests/client/clientreg-test/saml-entity-descriptor.xml"), Charset.defaultCharset());
        assertClientCreation(entityDescriptor);
    }

    private void assertClientCreation(String entityDescriptor) throws ClientRegistrationException {
        ClientRepresentation response = reg.saml().create(entityDescriptor);
        assertThat(response.getRegistrationAccessToken(), notNullValue());
        assertThat(response.getClientId(), is("loadbalancer-9.siroe.com"));
        assertThat(response.getRedirectUris(), containsInAnyOrder(
                "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/post",
                "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/soap",
                "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/paos",
                "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/redirect",
                "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp/artifact"
        ));

        assertThat(response.getAttributes().get(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE), is("https://LoadBalancer-9.siroe.com:3443/federation/SPSloRedirect/metaAlias/sp"));
        assertThat(response.getAttributes().get(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE), is("https://LoadBalancer-9.siroe.com:3443/federation/SPSloSoap/metaAlias/sp"));
        assertThat(response.getAttributes().get(SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER), is(ArtifactBindingUtils.computeArtifactBindingIdentifierString("loadbalancer-9.siroe.com")));

        Assertions.assertNotNull(response.getProtocolMappers());
        Assertions.assertEquals(1,response.getProtocolMappers().size());
        ProtocolMapperRepresentation mapper = response.getProtocolMappers().get(0);
        Assertions.assertEquals("saml-user-attribute-mapper",mapper.getProtocolMapper());
        Assertions.assertEquals("urn:oid:2.5.4.42",mapper.getConfig().get(AttributeStatementHelper.SAML_ATTRIBUTE_NAME));
        Assertions.assertEquals("givenName",mapper.getConfig().get(AttributeStatementHelper.FRIENDLY_NAME));
        Assertions.assertEquals(AttributeStatementHelper.URI_REFERENCE,mapper.getConfig().get(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT));

        managedRealm.admin().clients().get(response.getId()).remove();
    }

    private static class OIDCClient implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("oidc-client")
                    .fullScopeEnabled(true)
                    .secret("secret")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true);
        }
    }
}
