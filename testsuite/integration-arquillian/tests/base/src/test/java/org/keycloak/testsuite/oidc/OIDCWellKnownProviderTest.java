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

package org.keycloak.testsuite.oidc;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.services.resources.ServerMetadataResource;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCWellKnownProviderTest extends AbstractWellKnownProviderTest {

    protected String getWellKnownProviderId() {
        return OIDCWellKnownProviderFactory.PROVIDER_ID;
    }

    /**
     * Ensures that the OpenID Connect configuration is not exposed by default via the server metadata
     * root endpoint. This test verifies that accessing the `.well-known/openid-configuration`
     * endpoint results in an HTTP 404 response, indicating the resource is not available.
     */
    @Test
    public void openIdConfigurationShouldNotBeExposedViaServerMetadataRoot() {

        UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
        URI serverMetadataWellKnownUri = ServerMetadataResource.wellKnownProviderUrl(builder).build(getWellKnownProviderId(), "test");

        try (Client client = AdminClientUtil.createResteasyClient()) {
            try (Response response = client.target(serverMetadataWellKnownUri).request().get()) {
                assertEquals(404, response.getStatus());
            }
        }
    }
}
