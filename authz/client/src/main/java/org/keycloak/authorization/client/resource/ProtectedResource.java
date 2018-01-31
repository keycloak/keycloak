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

import static org.keycloak.authorization.client.util.Throwables.handleAndWrapException;

import java.util.Set;
import java.util.concurrent.Callable;

import org.keycloak.authorization.client.representation.RegistrationResponse;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ProtectedResource {

    private final Http http;
    private final Callable<String> pat;

    public ProtectedResource(Http http, Callable<String> pat) {
        this.http = http;
        this.pat = pat;
    }

    public RegistrationResponse create(ResourceRepresentation resource) {
        try {
            return this.http.<RegistrationResponse>post("/authz/protection/resource_set")
                    .authorizationBearer(this.pat.call())
                    .json(JsonSerialization.writeValueAsBytes(resource))
                    .response().json(RegistrationResponse.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not create resource", cause);
        }
    }

    public void update(ResourceRepresentation resource) {
        try {
            this.http.<RegistrationResponse>put("/authz/protection/resource_set/" + resource.getId())
                    .authorizationBearer(this.pat.call())
                    .json(JsonSerialization.writeValueAsBytes(resource)).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not update resource", cause);
        }
    }

    public RegistrationResponse findById(String id) {
        try {
            return this.http.<RegistrationResponse>get("/authz/protection/resource_set/" + id)
                    .authorizationBearer(this.pat.call())
                    .response().json(RegistrationResponse.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not find resource", cause);
        }
    }

    public Set<String> findByFilter(String filter) {
        try {
            return this.http.<Set>get("/authz/protection/resource_set")
                    .authorizationBearer(this.pat.call())
                    .param("filter", filter)
                    .response().json(Set.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not find resource", cause);
        }
    }

    public Set<String> findAll() {
        try {
            return this.http.<Set>get("/authz/protection/resource_set")
                    .authorizationBearer(this.pat.call())
                    .response().json(Set.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not find resource", cause);
        }
    }

    public void delete(String id) {
        try {
            this.http.delete("/authz/protection/resource_set/" + id)
                    .authorizationBearer(this.pat.call())
                    .execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not delete resource", cause);
        }
    }
}