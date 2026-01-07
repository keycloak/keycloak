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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.OrganizationInvitationRepresentation;

/**
 * Managing organization invitations.
 *
 * @since Keycloak server 26.5.0
 */
public interface OrganizationInvitationsResource {

    /**
     * Return all invitations in the organization.
     *
     * @return a list containing all organization invitations.
     */
    default List<OrganizationInvitationRepresentation> list() {
        return list(null, null, null, null, null, null, null);
    }

    /**
     * Return invitations in the organization with pagination.
     *
     * @param first index of the first element (pagination offset). If null, starts from 0.
     * @param max the maximum number of results. If null, returns all results.
     * @return a list containing organization invitations.
     */
    default List<OrganizationInvitationRepresentation> list(Integer first, Integer max) {
        return list(null, null, null, null, null, first, max);
    }

    /**
     * Return invitations in the organization filtered by status.
     *
     * @param first index of the first element (pagination offset). If null, starts from 0.
     * @param max the maximum number of results. If null, returns all results.
     * @param status filter by invitation status (PENDING, EXPIRED). If null, returns all statuses.
     * @param email filter by exact email match. If null, no email filtering is applied.
     * @return a list containing organization invitations matching the criteria.
     */
    default List<OrganizationInvitationRepresentation> list(String status, String email, Integer first, Integer max) {
        return list(status, email, null, null, null, first, max);
    }

    /**
     * Return invitations in the organization.
     *
     * @param first index of the first element (pagination offset). If null, starts from 0.
     * @param max the maximum number of results. If null, returns all results.
     * @param status filter by invitation status (PENDING, EXPIRED). If null, returns all statuses.
     * @param email filter by exact email match. If null, no email filtering is applied.
     * @param search search text applied across email, firstName, and lastName fields. If null, no search filtering is applied.
     * @param firstName filter by exact first name match. If null, no firstName filtering is applied.
     * @param lastName filter by exact last name match. If null, no lastName filtering is applied.
     * @return a list containing organization invitations matching the criteria.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationInvitationRepresentation> list(
            @QueryParam("status") String status,
            @QueryParam("email") String email,
            @QueryParam("search") String search,
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max);

    /**
     * Get invitation by ID.
     *
     * @param id the invitation ID.
     * @return the invitation representation.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    OrganizationInvitationRepresentation get(@PathParam("id") String id);

    /**
     * Delete an invitation permanently.
     * This action cannot be undone.
     *
     * @param id the invitation ID.
     * @return response indicating success or failure.
     */
    @DELETE
    @Path("/{id}")
    Response delete(@PathParam("id") String id);

    /**
     * Resend an invitation email.
     * This generates a new invitation token with fresh expiration time.
     *
     * @param id the invitation ID.
     * @return response indicating success or failure.
     */
    @POST
    @Path("/{id}/resend")
    Response resend(@PathParam("id") String id);
}
