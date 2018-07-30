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

import java.util.Arrays;
import java.util.Set;

import org.apache.http.Header;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.client.ClientAuthenticator;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationRequest.Metadata;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionTicketToken;

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
        this.method.params.put(OAuth2Constants.GRANT_TYPE, Arrays.asList(OAuth2Constants.CLIENT_CREDENTIALS));
        authenticator.configureClientCredentials(this.method.params, this.method.headers);
        return this.method;
    }

    public HttpMethod<R> oauth2ResourceOwnerPassword(String userName, String password) {
        client();
        this.method.params.put(OAuth2Constants.GRANT_TYPE, Arrays.asList(OAuth2Constants.PASSWORD));
        this.method.params.put("username", Arrays.asList(userName));
        this.method.params.put("password", Arrays.asList(password));
        return this.method;
    }

    public HttpMethod<R> uma() {
        // if there is an authorization bearer header authenticate using bearer token
        Header authorizationHeader = method.builder.getFirstHeader("Authorization");

        if (!(authorizationHeader != null && authorizationHeader.getValue().toLowerCase().startsWith("bearer"))) {
            client();
        }

        method.params.put(OAuth2Constants.GRANT_TYPE, Arrays.asList(OAuth2Constants.UMA_GRANT_TYPE));
        return method;
    }

    public HttpMethod<R> uma(AuthorizationRequest request) {
        String ticket = request.getTicket();
        PermissionTicketToken permissions = request.getPermissions();

        if (ticket == null && permissions == null) {
            throw new IllegalArgumentException("You must either provide a permission ticket or the permissions you want to request.");
        }

        uma();
        method.param("ticket", ticket);
        method.param("claim_token", request.getClaimToken());
        method.param("claim_token_format", request.getClaimTokenFormat());
        method.param("pct", request.getPct());
        method.param("rpt", request.getRptToken());
        method.param("scope", request.getScope());
        method.param("audience", request.getAudience());
        method.param("subject_token", request.getSubjectToken());

        if (permissions != null) {
            for (Permission permission : permissions.getPermissions()) {
                String resourceId = permission.getResourceId();
                Set<String> scopes = permission.getScopes();
                StringBuilder value = new StringBuilder();

                if (resourceId != null) {
                    value.append(resourceId);
                }

                if (scopes != null && !scopes.isEmpty()) {
                    value.append("#");
                    for (String scope : scopes) {
                        if (!value.toString().endsWith("#")) {
                            value.append(",");
                        }
                        value.append(scope);
                    }
                }

                method.params("permission", value.toString());
            }
        }

        Metadata metadata = request.getMetadata();

        if (metadata != null) {
            if (metadata.getIncludeResourceName() != null) {
                method.param("response_include_resource_name", metadata.getIncludeResourceName().toString());
            }

            if (metadata.getLimit() != null) {
                method.param("response_permissions_limit", metadata.getLimit().toString());
            }
        }

        return method;
    }
}
