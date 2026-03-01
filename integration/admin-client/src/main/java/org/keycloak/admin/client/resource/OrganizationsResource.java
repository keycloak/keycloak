/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.OrganizationRepresentation;

/**
 * @since Keycloak 25. All the child endpoints are also available since that version<p>
 *
 * This endpoint including all the child endpoints requires feature {@link org.keycloak.common.Profile.Feature#ORGANIZATION} to be enabled<p>
 */
public interface OrganizationsResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(OrganizationRepresentation organization);

    @Path("{id}")
    OrganizationResource get(@PathParam("id") String id);

    /**
     * Returns all organizations in the realm.
     *
     * @return a list containing the organizations.
     * @Deprecated Use {@link org.keycloak.admin.client.resource.OrganizationsResource#list} instead.
     */
    @Deprecated
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> getAll();

    /**
     * Returns organizations in the realm.
     *
     * @param first index of the first element (pagination offset).
     * @param max the maximum number of results.
     * @return a list containing the organizations.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> list(
            @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults
    );

    /**
     * Returns all organizations that match the specified filter.
     *
     * @param search a {@code String} representing either an organization name or domain.
     * @return a list containing the matched organizations.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> search(@QueryParam("search") String search);

    /**
     * Returns all organizations that match the specified filters.
     *
     * @param search a {@code String} representing either an organization name or domain.
     * @param exact if {@code true}, the organizations will be searched using exact match for the {@code search} param - i.e.
     *              either the organization name or one of its domains must match exactly the {@code search} param. If false,
     *              the method returns all organizations whose name or (domains) partially match the {@code search} param.
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a list containing the matched organizations.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> search(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max
    );

    /**
     * Returns all organizations that match the specified filters.
     *
     * @param search a {@code String} representing either an organization name or domain.
     * @param exact if {@code true}, the organizations will be searched using exact match for the {@code search} param - i.e.
     *              either the organization name or one of its domains must match exactly the {@code search} param. If false,
     *              the method returns all organizations whose name or (domains) partially match the {@code search} param.
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @param briefRepresentation if {@code false} the full representation is to be returned. Otherwise, only the basic fields are returned. The parameter is supported since Keycloak 26.1
     * @return a list containing the matched organizations.
     * @since Keycloak 26.1. Use method {@link #search(String, Boolean, Integer, Integer)} for the older versions of the Keycloak server
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> search(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("briefRepresentation") Boolean briefRepresentation
    );

    /**
     * Returns all organizations that contain attributes matching the specified query.
     *
     * @param searchQuery a query to search for organization attributes, in the format 'key1:value2 key2:value2'.
     * @return a list containing the organizations that match the attribute query.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> searchByAttribute(
            @QueryParam("q") String searchQuery
    );

    /**
     * Returns all organizations that contain attributes matching the specified query.
     *
     * @param searchQuery a query to search for organization attributes, in the format 'key1:value2 key2:value2'.
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a list containing the organizations that match the attribute query.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> searchByAttribute(
            @QueryParam("q") String searchQuery,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max
    );

    /**
     * Counts organizations by search.
     * @param search text to look for.
     * @return the number of organizations that match the search.
     * @since Keycloak 26.3
     */
    @GET
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    long count(@QueryParam("search") String search);

    /**
     * Counts organizations by search.
     * @param search text to look for.
     * @param exact if {@code true}, the organizations will be searched using exact match for the {@code search} param - i.e.
     *              either the organization name or one of its domains must match exactly the {@code search} param. If false,
     *              the method returns all organizations whose name or (domains) partially match the {@code search} param.
     * @return the number of organizations that match the search.
     * @since Keycloak 26.3
     */
    @GET
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    long count(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact
    );

    /**
     * Counts all organizations that contain attributes matching the specified query.
     * @param searchQuery a query to search for organization attributes, in the format 'key1:value2 key2:value2'.
     * @return the number of the organizations that match the attribute query.
     * @since Keycloak 26.3
     */
    @GET
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    long countByAttribute(
            @QueryParam("q") String searchQuery
    );


    @Path("members")
    OrganizationsMembersResource members();
}
