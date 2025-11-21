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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;

public interface OrganizationMembersResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response addMember(String userId);

    @Path("{member-id}")
    @DELETE
    Response removeMember(@PathParam("member-id") String memberId);

    /**
     * Return all members in the organization.
     *
     * @return a list containing the organization members.
     * @Deprecated Use {@link org.keycloak.admin.client.resource.OrganizationMembersResource#list} instead.
     */
    @Deprecated
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<MemberRepresentation> getAll();

    /**
     * Return members in the organization.
     *
     * @param first index of the first element (pagination offset).
     * @param max the maximum number of results.
     * @return a list containing organization members.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<MemberRepresentation> list(
            @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults
    );

    /**
     * Return all organization members that match the specified filters.
     *
     * @param search a {@code String} representing either a member's username, e-mail, first name, or last name.
     * @param exact if {@code true}, the members will be searched using exact match for the {@code search} param - i.e.
     *              at least one of the username main attributes must match exactly the {@code search} param. If false,
     *              the method returns all members with at least one main attribute partially matching the {@code search} param.
     * @param first index of the first element (pagination offset).
     * @param max the maximum number of results.
     * @return a list containing the matched organization members.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<MemberRepresentation> search(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max
    );

    /**
     * Return all organization members that match the specified filters.
     *
     * @param search a {@code String} representing either a member's username, e-mail, first name, or last name.
     * @param exact if {@code true}, the members will be searched using exact match for the {@code search} param - i.e.
     *              at least one of the username main attributes must match exactly the {@code search} param. If false,
     *              the method returns all members with at least one main attribute partially matching the {@code search} param.
     * @param membershipType The {@link org.keycloak.representations.idm.MembershipType}. The parameter is supported since Keycloak 26.1
     * @param first index of the first element (pagination offset).
     * @param max the maximum number of results.
     * @return a list containing the matched organization members.
     * @since Keycloak 26.1. Use method {@link #search(String, Boolean, Integer, Integer)} for the older versions of the Keycloak server
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<MemberRepresentation> search(
            @QueryParam("search") String search,
            @QueryParam("exact") Boolean exact,
            @QueryParam("membershipType") MembershipType membershipType,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max
    );

    @Path("{id}")
    OrganizationMemberResource member(@PathParam("id") String id);

    @POST
    @Path("invite-user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response inviteUser(@FormParam("email") String email,
                        @FormParam("firstName") String firstName,
                        @FormParam("lastName") String lastName);

    @POST
    @Path("invite-existing-user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response inviteExistingUser(@FormParam("id") String id);

    /**
     * @since Keycloak server 26
     * @return count of members of the organization
     */
    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Long count();

    @Path("{id}/organizations")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> getOrganizations(
            @PathParam("id") String id,
            @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);
}
