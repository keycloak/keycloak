/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class UserProfileResource {

    @Context
    protected KeycloakSession session;

    protected RealmModel realm;
    private AdminPermissionEvaluator auth;

    public UserProfileResource(RealmModel realm, AdminPermissionEvaluator auth) {
        this.realm = realm;
        this.auth = auth;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getConfiguration() {
        auth.realm().requireViewRealm();
        return session.getProvider(UserProfileProvider.class).getConfiguration();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(String text) {
        auth.realm().requireManageRealm();
        UserProfileProvider t = session.getProvider(UserProfileProvider.class);

        try {
            t.setConfiguration(text);
        } catch (ComponentValidationException e) {
            //show validation result containing details about error
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

        return Response.ok(t.getConfiguration()).type(MediaType.APPLICATION_JSON).build();
    }

}
