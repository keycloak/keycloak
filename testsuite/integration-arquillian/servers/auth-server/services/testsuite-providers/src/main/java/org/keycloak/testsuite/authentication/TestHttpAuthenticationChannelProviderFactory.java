/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.testsuite.authentication;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelProvider;
import org.keycloak.protocol.oidc.grants.ciba.channel.HttpAuthenticationChannelProvider;
import org.keycloak.protocol.oidc.grants.ciba.channel.HttpAuthenticationChannelProviderFactory;
import org.keycloak.testsuite.util.ServerURLs;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TestHttpAuthenticationChannelProviderFactory extends HttpAuthenticationChannelProviderFactory {

    private static final String TEST_HTTP_AUTH_CHANNEL =
            String.format("%s://%s:%s/auth/realms/master/app/oidc-client-endpoints/request-authentication-channel",
                    ServerURLs.AUTH_SERVER_SCHEME, ServerURLs.AUTH_SERVER_HOST, ServerURLs.AUTH_SERVER_PORT);

    @Override
    public AuthenticationChannelProvider create(KeycloakSession session) {
        return new HttpAuthenticationChannelProvider(session, TEST_HTTP_AUTH_CHANNEL);
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public String getId() {
        return "test-http-auth-channel";
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
