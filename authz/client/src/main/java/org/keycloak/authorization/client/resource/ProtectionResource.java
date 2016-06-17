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
package org.keycloak.authorization.client.resource;

import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.authorization.client.util.Http;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ProtectionResource {

    private final String pat;
    private final Http http;

    public ProtectionResource(Http http, String pat) {
        if (pat == null) {
            throw new RuntimeException("No access token was provided when creating client for Protection API.");
        }

        this.http = http;
        this.pat = pat;
    }

    public ProtectedResource resource() {
        return new ProtectedResource(http, pat);
    }

    public PermissionResource permission() {
        return new PermissionResource(http, pat);
    }

    public TokenIntrospectionResponse introspectRequestingPartyToken(String rpt) {
        return this.http.<TokenIntrospectionResponse>post("/protocol/openid-connect/token/introspect")
                .authentication()
                    .oauth2ClientCredentials()
                .form()
                    .param("token_type_hint", "requesting_party_token")
                    .param("token", rpt)
                .response().json(TokenIntrospectionResponse.class).execute();
    }
}
