package org.keycloak.organization;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.OrganizationInvitationModel;
import org.keycloak.models.OrganizationInvitationModel.Filter;
import org.keycloak.models.OrganizationModel;

public interface InvitationManager {

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
    OrganizationInvitationModel create(OrganizationModel organization, String email,
                                       String firstName, String lastName);

    /**
     * Retrieves an invitation by its ID for a specific organization.
     *
     * @param id the invitation ID
     * @return the invitation, or null if not found
     */
    OrganizationInvitationModel getById(String id);

    /**
     * Retrieves an invitation by email and organization.
     *
     * @param email the email address
     * @param organization the organization
     * @return the invitation, or null if not found
     */
    default OrganizationInvitationModel getByEmail(OrganizationModel organization, String email) {
        return getAllStream(organization, Map.of(Filter.EMAIL, email), null, null)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all invitations for the given organization.
     *
     * @return a stream of all invitations for the organization
     */
    Stream<OrganizationInvitationModel> getAllStream(OrganizationModel organization, Map<Filter, String> attributes, Integer first, Integer max);

    /**
     * Deletes an invitation permanently.
     *
     * @param id the invitation ID
     * @return true if successful, false if invitation not found
     */
    boolean remove(String id);
}
