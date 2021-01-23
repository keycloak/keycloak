/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.keycloak.authorization.config.UmaConfiguration;
import org.keycloak.authorization.config.UmaWellKnownProviderFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.OAuthClient;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class UmaDiscoveryDocumentTest extends AbstractKeycloakTest {

    @ArquillianResource
    protected OAuthClient oauth;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void testFetchDiscoveryDocument() {
        Client client = AdminClientUtil.createResteasyClient();
        UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build("test", UmaWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        try (Response response = oidcDiscoveryTarget.request().get()) {
            assertEquals("no-cache, must-revalidate, no-transform, no-store", response.getHeaders().getFirst("Cache-Control"));


            UmaConfiguration configuration = response.readEntity(UmaConfiguration.class);


            assertEquals(configuration.getAuthorizationEndpoint(), OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            assertEquals(configuration.getTokenEndpoint(), oauth.getAccessTokenUrl());
            assertEquals(configuration.getJwksUri(), oauth.getCertsUrl("test"));
            assertEquals(configuration.getIntrospectionEndpoint(), oauth.getTokenIntrospectionUrl());

            String registrationUri = UriBuilder
                    .fromUri(OAuthClient.AUTH_SERVER_ROOT)
                    .path(RealmsResource.class).path(RealmsResource.class, "getRealmResource").build(realmsResouce().realm("test").toRepresentation().getRealm()).toString();

            assertEquals(registrationUri + "/authz/protection/permission", configuration.getPermissionEndpoint().toString());
            assertEquals(registrationUri + "/authz/protection/resource_set", configuration.getResourceRegistrationEndpoint().toString());
        }
    }
}
