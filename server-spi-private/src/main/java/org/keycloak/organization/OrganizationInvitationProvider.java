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
package org.keycloak.organization;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.keycloak.models.OrganizationModel;
import org.keycloak.provider.Provider;

/**
 * Provider for managing organization invitations.
 */
public interface OrganizationInvitationProvider extends Provider {

    /**
     * Creates a new invitation for a user to join an organization.
     * The invitation will use the realm's default action token lifespan.
     *
     * @param organization the organization
     * @param email the email address of the user to invite
     * @param firstName the first name of the user (optional)
     * @param lastName the last name of the user (optional)
     * @return the created invitation
     */
    OrganizationInvitationModel createInvitation(OrganizationModel organization, String email, 
                                                String firstName, String lastName);

    /**
     * Creates a new invitation for a user to join an organization with a specific expiration time.
     *
     * @param organization the organization
     * @param email the email address of the user to invite
     * @param firstName the first name of the user (optional)
     * @param lastName the last name of the user (optional)
     * @param expiresAt when the invitation expires
     * @return the created invitation
     */
    OrganizationInvitationModel createInvitation(OrganizationModel organization, String email, 
                                                String firstName, String lastName, 
                                                LocalDateTime expiresAt);

    /**
     * Retrieves an invitation by its ID for a specific organization.
     *
     * @param id the invitation ID
     * @param organization the organization
     * @return the invitation, or null if not found
     */
    OrganizationInvitationModel getById(String id, OrganizationModel organization);

    /**
     * Retrieves an invitation by email and organization.
     *
     * @param email the email address
     * @param organization the organization
     * @return the invitation, or null if not found
     */
    OrganizationInvitationModel getByEmailAndOrganization(String email, OrganizationModel organization);

    /**
     * Get all invitations for the given organization.
     *
     * @param organization the organization
     * @return a stream of all invitations for the organization
     */
    Stream<OrganizationInvitationModel> getAllInvitations(OrganizationModel organization);

    /**
     * Deletes an invitation permanently.
     *
     * @param organization the organization
     * @param invitationId the invitation ID
     * @return true if successful, false if invitation not found
     */
    boolean deleteInvitation(OrganizationModel organization, String invitationId);
}
