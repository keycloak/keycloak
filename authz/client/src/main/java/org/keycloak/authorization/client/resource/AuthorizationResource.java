/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.client.resource;


import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.representation.AuthorizationRequest;
import org.keycloak.authorization.client.representation.AuthorizationResponse;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationResource {

    private final Http http;
    private final String accessToken;

    public AuthorizationResource(Http http, String aat) {
        this.http = http;
        this.accessToken = aat;
    }

    public AuthorizationResponse authorize(AuthorizationRequest request) {
        try {
            return this.http.<AuthorizationResponse>post("/authz/authorize")
                    .authorizationBearer(this.accessToken)
                    .json(JsonSerialization.writeValueAsBytes(request))
                    .response().json(AuthorizationResponse.class).execute();
        } catch (HttpResponseException e) {
            if (403 == e.getStatusCode()) {
                throw new AuthorizationDeniedException(e);
            }
            throw new RuntimeException("Failed to obtain authorization data.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain authorization data.", e);
        }
    }
}
