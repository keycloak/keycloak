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

import java.util.concurrent.Callable;

import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.RegistrationResponse;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.util.JsonSerialization;

/**
 * An entry point for managing resources using the Protection API.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ProtectedResource {

    private final Http http;
    private Configuration configuration;
    private final Callable<String> pat;

    ProtectedResource(Http http, Configuration configuration, Callable<String> pat) {
        this.http = http;
        this.configuration = configuration;
        this.pat = pat;
    }

    /**
     * Creates a new resource.
     *
     * @param resource the resource data
     * @return a {@link RegistrationResponse}
     */
    public RegistrationResponse create(ResourceRepresentation resource) {
        try {
            return this.http.<RegistrationResponse>post("/authz/protection/" + configuration.getResource() + "/resource_set")
                    .authorizationBearer(this.pat.call())
                    .json(JsonSerialization.writeValueAsBytes(resource))
                    .response().json(RegistrationResponse.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not create resource", cause);
        }
    }

    /**
     * Updates a resource.
     *
     * @param resource the resource data
     * @return a {@link RegistrationResponse}
     */
    public void update(ResourceRepresentation resource) {
        if (resource.getId() == null) {
            throw new IllegalArgumentException("You must provide the resource id");
        }
        try {
            this.http.<RegistrationResponse>put("/authz/protection/" + configuration.getResource() + "/resource_set/" + resource.getId())
                    .authorizationBearer(this.pat.call())
                    .json(JsonSerialization.writeValueAsBytes(resource)).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not update resource", cause);
        }
    }

    /**
     * Query the server for a resource given its <code>id</code>.
     *
     * @param id the resource id
     * @return a {@link ResourceRepresentation}
     */
    public ResourceRepresentation findById(String id) {
        try {
            return this.http.<ResourceRepresentation>get("/authz/protection/" + configuration.getResource() + "/resource_set/" + id)
                    .authorizationBearer(this.pat.call())
                    .response().json(ResourceRepresentation.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not find resource", cause);
        }
    }

    /**
     * Query the server for a resource given its <code>name</code>.
     *
     * @param id the resource name
     * @return a {@link ResourceRepresentation}
     */
    public ResourceRepresentation findByName(String name) {
        String[] representations = find(null, name, null, null, null, null, null, null);

        if (representations.length == 0) {
            return null;
        }

        return findById(representations[0]);
    }

    /**
     * Query the server for any resource with the matching arguments.
     *
     * @param id the resource id
     * @param name the resource name
     * @param uri the resource uri
     * @param owner the resource owner
     * @param type the resource type
     * @param scope the resource scope
     * @param firstResult the position of the first resource to retrieve
     * @param maxResult the maximum number of resources to retrieve
     * @return an array of strings with the resource ids
     */
    public String[] find(String id, String name, String uri, String owner, String type, String scope, Integer firstResult, Integer maxResult) {
        try {
            return this.http.<String[]>get("/authz/protection/" + configuration.getResource() + "/resource_set")
                    .authorizationBearer(this.pat.call())
                    .param("_id", id)
                    .param("name", name)
                    .param("uri", uri)
                    .param("owner", owner)
                    .param("type", type)
                    .param("scope", scope)
                    .param("deep", Boolean.FALSE.toString())
                    .param("first", firstResult != null ? firstResult.toString() : null)
                    .param("max", maxResult != null ? maxResult.toString() : null)
                    .response().json(String[].class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not find resource", cause);
        }
    }

    /**
     * Query the server for all resources.
     *
     * @return @return an array of strings with the resource ids
     */
    public String[] findAll() {
        try {
            return find(null,null , null, null, null, null, null, null);
        } catch (Exception cause) {
            throw handleAndWrapException("Could not find resource", cause);
        }
    }

    /**
     * Deletes a resource with the given <code>id</code>.
     *
     * @param id the resource id
     */
    public void delete(String id) {
        try {
            this.http.delete("/authz/protection/" + configuration.getResource() + "/resource_set/" + id)
                    .authorizationBearer(this.pat.call())
                    .execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Could not delete resource", cause);
        }
    }

    /**
     * Query the server for all resources with the given uri.
     *
     * @param uri the resource uri
     */
    public List<ResourceRepresentation> findByUri(String uri) {
        String[] ids = find(null, null, uri, null, null, null, null, null);

        if (ids.length == 0) {
            return Collections.emptyList();
        }

        List<ResourceRepresentation> representations = new ArrayList<>();

        for (String id : ids) {
            representations.add(findById(id));
        }

        return representations;
    }
}