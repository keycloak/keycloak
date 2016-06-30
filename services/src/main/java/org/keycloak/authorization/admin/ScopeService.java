/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.keycloak.authorization.admin;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.RealmAuth;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.keycloak.authorization.admin.util.Models.toModel;
import static org.keycloak.authorization.admin.util.Models.toRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopeService {

    private final AuthorizationProvider authorization;
    private final RealmAuth auth;
    private ResourceServer resourceServer;

    public ScopeService(ResourceServer resourceServer, AuthorizationProvider authorization, RealmAuth auth) {
        this.resourceServer = resourceServer;
        this.authorization = authorization;
        this.auth = auth;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response create(ScopeRepresentation scope) {
        this.auth.requireManage();
        Scope model = toModel(scope, this.resourceServer, authorization);

        scope.setId(model.getId());

        return Response.status(Status.CREATED).entity(scope).build();
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("id") String id, ScopeRepresentation scope) {
        this.auth.requireManage();
        scope.setId(id);
        StoreFactory storeFactory = authorization.getStoreFactory();
        Scope model = storeFactory.getScopeStore().findById(scope.getId());

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        model.setName(scope.getName());
        model.setIconUri(scope.getIconUri());

        return Response.noContent().build();
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        this.auth.requireManage();
        StoreFactory storeFactory = authorization.getStoreFactory();
        List<Resource> resources = storeFactory.getResourceStore().findByScope(id);

        if (!resources.isEmpty()) {
            return ErrorResponse.exists("Scopes can not be removed while associated with resources.");
        }

        Scope scope = storeFactory.getScopeStore().findById(id);
        PolicyStore policyStore = storeFactory.getPolicyStore();
        List<Policy> policies = policyStore.findByScopeIds(Arrays.asList(scope.getId()), resourceServer.getId());

        for (Policy policyModel : policies) {
            if (policyModel.getScopes().size() == 1) {
                policyStore.delete(policyModel.getId());
            } else {
                policyModel.removeScope(scope);
            }
        }

        storeFactory.getScopeStore().delete(id);

        return Response.noContent().build();
    }

    @Path("{id}")
    @GET
    @Produces("application/json")
    public Response findById(@PathParam("id") String id) {
        this.auth.requireView();
        Scope model = this.authorization.getStoreFactory().getScopeStore().findById(id);

        if (model == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(toRepresentation(model, this.authorization)).build();
    }

    @GET
    @Produces("application/json")
    public Response findAll() {
        this.auth.requireView();
        return Response.ok(
                this.authorization.getStoreFactory().getScopeStore().findByResourceServer(this.resourceServer.getId()).stream()
                        .map(scope -> toRepresentation(scope, this.authorization))
                        .collect(Collectors.toList()))
                .build();
    }
}
