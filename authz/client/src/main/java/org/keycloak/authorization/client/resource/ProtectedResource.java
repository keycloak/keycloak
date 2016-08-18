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

import org.keycloak.authorization.client.representation.RegistrationResponse;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.util.JsonSerialization;

import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ProtectedResource {

    private final Http http;
    private final String pat;

    public ProtectedResource(Http http, String pat) {
        this.http = http;
        this.pat = pat;
    }

    public RegistrationResponse create(ResourceRepresentation resource) {
        try {
            return this.http.<RegistrationResponse>post("/authz/protection/resource_set")
                    .authorizationBearer(this.pat)
                    .json(JsonSerialization.writeValueAsBytes(resource))
                    .response().json(RegistrationResponse.class).execute();
        } catch (Exception e) {
            throw new RuntimeException("Could not create resource.", e);
        }
    }

    public RegistrationResponse findById(String id) {
        try {
            return this.http.<RegistrationResponse>get("/authz/protection/resource_set/" + id)
                    .authorizationBearer(this.pat)
                    .response().json(RegistrationResponse.class).execute();
        } catch (Exception e) {
            throw new RuntimeException("Could not find resource.", e);
        }
    }

    public Set<String> findByFilter(String filter) {
        try {
            return this.http.<Set>get("/authz/protection/resource_set")
                    .authorizationBearer(this.pat)
                    .param("filter", filter)
                    .response().json(Set.class).execute();
        } catch (Exception e) {
            throw new RuntimeException("Could not find resource.", e);
        }
    }

    public Set<String> findAll() {
        try {
            return this.http.<Set>get("/authz/protection/resource_set")
                    .authorizationBearer(this.pat)
                    .response().json(Set.class).execute();
        } catch (Exception e) {
            throw new RuntimeException("Could not find resource.", e);
        }
    }

    public void delete(String id) {
        try {
            this.http.delete("/authz/protection/resource_set/" + id)
                    .authorizationBearer(this.pat)
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("Could not delete resource.", e);
        }
    }
}
