/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.client.util;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.client.ClientAuthenticator;
import org.keycloak.authorization.client.representation.AuthorizationRequestMetadata;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpMethodAuthenticator<R> {

    private final HttpMethod<R> method;
    private final ClientAuthenticator authenticator;

    public HttpMethodAuthenticator(HttpMethod<R> method, ClientAuthenticator authenticator) {
        this.method = method;
        this.authenticator = authenticator;
    }

    public HttpMethod<R> client() {
        this.method.params.put(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS);
        authenticator.configureClientCredentials(this.method.params, this.method.headers);
        return this.method;
    }

    public HttpMethod<R> oauth2ResourceOwnerPassword(String userName, String password) {
        client();
        this.method.params.put(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
        this.method.params.put("username", userName);
        this.method.params.put("password", password);
        return this.method;
    }

    public HttpMethod<R> uma() {
        client();
        method.params.put(OAuth2Constants.GRANT_TYPE, OAuth2Constants.UMA_GRANT_TYPE);
        return method;
    }

    public HttpMethod<R> uma(String ticket, String claimToken, String claimTokenFormat, String pct, String rpt, String scope, PermissionTicketToken permissions, AuthorizationRequestMetadata metadata) {
        if (ticket == null && permissions == null) {
            throw new IllegalArgumentException("You must either provide a permission ticket or the permissions you want to request.");
        }
        uma();
        method.param("ticket", ticket);
        method.param("claim_token", claimToken);
        method.param("claim_token_format", claimTokenFormat);
        method.param("pct", pct);
        method.param("rpt", rpt);
        method.param("scope", scope);
        try {
            method.param("permissions", permissions != null ? JsonSerialization.writeValueAsString(permissions) : null);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to marshal permissions", cause);
        }
        try {
            method.param("metadata", metadata != null ? JsonSerialization.writeValueAsString(metadata) : null);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to marshal metadata", cause);
        }
        return method;
    }
}
