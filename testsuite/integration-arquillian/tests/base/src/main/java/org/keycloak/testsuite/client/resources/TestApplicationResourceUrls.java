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

package org.keycloak.testsuite.client.resources;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.testsuite.util.oauth.OAuthClient;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestApplicationResourceUrls {

    private static UriBuilder oidcClientEndpoints() {
        return UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)
                .path(TestApplicationResource.class)
                .path(TestApplicationResource.class, "oidcClientEndpoints");
    }

    public static String clientRequestUri() {
        UriBuilder builder = oidcClientEndpoints()
                .path(TestOIDCEndpointsApplicationResource.class, "getOIDCRequest");

        return builder.build().toString();
    }

    public static String clientJwksUri() {
        UriBuilder builder = oidcClientEndpoints()
                .path(TestOIDCEndpointsApplicationResource.class, "getJwks");

        return builder.build().toString();
    }

    public static String pairwiseSectorIdentifierUri() {
        UriBuilder builder = oidcClientEndpoints()
                .path(TestOIDCEndpointsApplicationResource.class, "getSectorIdentifierRedirectUris");
        return builder.build().toString();
    }

    public static String cibaClientNotificationEndpointUri() {
        UriBuilder builder = oidcClientEndpoints()
                .path(TestOIDCEndpointsApplicationResource.class, "cibaClientNotificationEndpoint");

        return builder.build().toString();
    }

    public static String checkIntentClientBoundUri() {
        UriBuilder builder = oidcClientEndpoints()
                .path(TestOIDCEndpointsApplicationResource.class, "checkIntentClientBound");

        return builder.build().toString();
    }
}
