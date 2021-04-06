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
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class ClientProfilesResource {
    protected static final Logger logger = Logger.getLogger(ClientProfilesResource.class);

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    @Context
    protected KeycloakSession session;

    protected RealmModel realm;
    private AdminPermissionEvaluator auth;

    public ClientProfilesResource(RealmModel realm, AdminPermissionEvaluator auth) {
        this.realm = realm;
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public String getProfiles() {
        auth.realm().requireViewRealm();

        return session.clientPolicy().getClientProfiles(realm);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProfiles(final String json) {
        auth.realm().requireManageRealm();

        try {
            session.clientPolicy().updateClientProfiles(realm, json);
        } catch (ClientPolicyException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getError()).build();
        }
        return Response.noContent().build();
    }
}
