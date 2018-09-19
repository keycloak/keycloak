/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.client.resource;

import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.authorization.client.util.Throwables;
import org.keycloak.authorization.client.util.TokenCallable;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * An entry point for managing user-managed permissions for a particular resource
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PolicyResource {

    private String resourceId;
    private final Http http;
    private final ServerConfiguration serverConfiguration;
    private final TokenCallable pat;

    public PolicyResource(String resourceId, Http http, ServerConfiguration serverConfiguration, TokenCallable pat) {
        this.resourceId = resourceId;
        this.http = http;
        this.serverConfiguration = serverConfiguration;
        this.pat = pat;
    }

    /**
     * Creates a new user-managed permission as represented by the given {@code permission}.
     *
     * @param permission the permission to create
     * @return if successful, the permission created
     */
    public UmaPermissionRepresentation create(final UmaPermissionRepresentation permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission must not be null");
        }

        Callable<UmaPermissionRepresentation> callable = new Callable<UmaPermissionRepresentation>() {
            @Override
            public UmaPermissionRepresentation call() throws Exception {
                return http.<UmaPermissionRepresentation>post(serverConfiguration.getPolicyEndpoint() + "/" + resourceId)
                        .authorizationBearer(pat.call())
                        .json(JsonSerialization.writeValueAsBytes(permission))
                        .response().json(UmaPermissionRepresentation.class).execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error creating policy for resource [" + resourceId + "]", cause);
        }
    }

    /**
     * Updates an existing user-managed permission
     *
     * @param permission the permission to update
     */
    public void update(final UmaPermissionRepresentation permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission must not be null");
        }

        if (permission.getId() == null) {
            throw new IllegalArgumentException("Permission id must not be null");
        }

        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                http.<Void>put(serverConfiguration.getPolicyEndpoint() + "/"+ permission.getId())
                        .authorizationBearer(pat.call())
                        .json(JsonSerialization.writeValueAsBytes(permission)).execute();
                return null;
            }
        };
        try {
            callable.call();
        } catch (Exception cause) {
            Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error updating policy for resource [" + resourceId + "]", cause);
        }
    }

    /**
     * Deletes an existing user-managed permission
     *
     * @param id the permission id
     */
    public void delete(final String id) {
        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() {
                http.<UmaPermissionRepresentation>delete(serverConfiguration.getPolicyEndpoint() + "/" + id)
                        .authorizationBearer(pat.call())
                        .response().execute();
                return null;
            }
        };
        try {
            callable.call();
        } catch (Exception cause) {
            Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error updating policy for resource [" + resourceId + "]", cause);
        }
    }

    /**
     * Queries the server for permission matching the given parameters.
     *
     * @param id the permission id
     * @param name the name of the permission
     * @param scope the scope associated with the permission
     * @param firstResult the position of the first resource to retrieve
     * @param maxResult the maximum number of resources to retrieve
     * @return the permissions matching the given parameters
     */
    public List<UmaPermissionRepresentation> find(final String name,
                                                  final String scope,
                                                  final Integer firstResult,
                                                  final Integer maxResult) {
        Callable<List<UmaPermissionRepresentation>> callable = new Callable<List<UmaPermissionRepresentation>>() {
            @Override
            public List<UmaPermissionRepresentation> call() {
                return http.<List<UmaPermissionRepresentation>>get(serverConfiguration.getPolicyEndpoint())
                        .authorizationBearer(pat.call())
                        .param("name", name)
                        .param("resource", resourceId)
                        .param("scope", scope)
                        .param("first", firstResult == null ? null : firstResult.toString())
                        .param("max", maxResult == null ? null : maxResult.toString())
                        .response().json(new TypeReference<List<UmaPermissionRepresentation>>(){}).execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error querying policies for resource [" + resourceId + "]", cause);
        }
    }

    /**
     * Queries the server for a permission with the given {@code id}.
     *
     * @param id the permission id
     * @return the permission with the given id
     */
    public UmaPermissionRepresentation findById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("Permission id must not be null");
        }

        Callable<UmaPermissionRepresentation> callable = new Callable<UmaPermissionRepresentation>() {
            @Override
            public UmaPermissionRepresentation call() {
                return http.<UmaPermissionRepresentation>get(serverConfiguration.getPolicyEndpoint() + "/" + id)
                        .authorizationBearer(pat.call())
                        .response().json(UmaPermissionRepresentation.class).execute();
            }
        };
        try {
            return callable.call();
        } catch (Exception cause) {
            return Throwables.retryAndWrapExceptionIfNecessary(callable, pat, "Error creating policy for resource [" + resourceId + "]", cause);
        }
    }
}
