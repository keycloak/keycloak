/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.resources.admin;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.cors.Cors;

public class RealmsAdminResourcePreflight extends RealmsAdminResource {

    private HttpRequest request;

    public RealmsAdminResourcePreflight(KeycloakSession session, AdminAuth auth, TokenManager tokenManager) {
        super(session, auth, tokenManager);
    }

    public RealmsAdminResourcePreflight(KeycloakSession session, AdminAuth auth, TokenManager tokenManager, HttpRequest request) {
        super(session, auth, tokenManager);
        this.request = request;
    }

    @Path("{any:.*}")
    @OPTIONS
    public Response preFlight() {
        return Cors.builder().preflight().allowedMethods("GET", "PUT", "POST", "DELETE").auth().add(Response.ok());
    }

}
