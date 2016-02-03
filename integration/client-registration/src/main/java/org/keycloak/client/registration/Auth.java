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

package org.keycloak.client.registration;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.keycloak.common.util.Base64;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class Auth {

    public abstract void addAuth(HttpRequest request);

    public static Auth token(String token) {
        return new BearerTokenAuth(token);
    }

    public static Auth token(ClientInitialAccessPresentation initialAccess) {
        return new BearerTokenAuth(initialAccess.getToken());
    }

    public static Auth token(ClientRepresentation client) {
        return new BearerTokenAuth(client.getRegistrationAccessToken());
    }

    public static Auth token(OIDCClientRepresentation client) {
        return new BearerTokenAuth(client.getRegistrationAccessToken());
    }

    public static Auth client(String clientId, String clientSecret) {
        return new BasicAuth(clientId, clientSecret);
    }

    private static class BearerTokenAuth extends Auth {

        private String token;

        public BearerTokenAuth(String token) {
            this.token = token;
        }

        @Override
        public void addAuth(HttpRequest request) {
            request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
    }

    private static class BasicAuth extends Auth {

        private String username;
        private String password;

        public BasicAuth(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void addAuth(HttpRequest request) {
            String val = Base64.encodeBytes((username + ":" + password).getBytes());
            request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + val);
        }
    }

}
