package org.keycloak.testframework.realm;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.injection.ManagedTestResource;
import org.keycloak.testframework.util.ApiUtil;

public class ManagedOrganization extends ManagedTestResource {

    private final OrganizationRepresentation createdRepresentation;
    private final OrganizationResource organizationResource;

    private ManagedOrganizationCleanup cleanup;

    public ManagedOrganization(OrganizationRepresentation createdRepresentation, OrganizationResource organizationResource) {
        this.createdRepresentation = createdRepresentation;
        this.organizationResource = organizationResource;
    }

    /**
     * The UUID of the organization
     *
     * @return organization UUID
     */
    public String getId() {
        return createdRepresentation.getId();
    }

    /**
     * The name of the organization
     *
     * @return organization name
     */
    public String getName() {
        return createdRepresentation.getName();
    }

    /**
     * The alias of the organization. This is <code>null</code> if no alias was configured, in which case the server
     * defaults the alias to the name of the organization.
     *
     * @return organization alias
     */
    public String getAlias() {
        return createdRepresentation.getAlias();
    }

    public ManagedOrganizationCleanup cleanup() {
        if (cleanup == null) {
            cleanup = new ManagedOrganizationCleanup();
        }
        return cleanup;
    }

    /**
     * Admin organization resource for the organization to view or update the configuration of the organization. Updates
     * should in general not be done directly through the organization resource as it will leave the organization in an
     * unexpected state for subsequent tests.
     *
     * @return organization resource
     */
    public OrganizationResource admin() {
        return organizationResource;
    }

    /**
     * Invite a user to the organization by email, which is automatically deleted once the test is completed. This
     * creates a pending invitation for a user that does not exist in the realm yet.
     * <p>
     * The server sends the invitation email itself and fails if it can not, so the realm requires a working SMTP
     * configuration. Use the mail server provided by <code>@InjectMailServer</code> to configure the realm and to read
     * the invitation email.
     *
     * @param email the email address to invite
     */
    public void inviteUser(String email) {
        inviteUser(email, null, null);
    }

    /**
     * Invite a user to the organization by email, which is automatically deleted once the test is completed.
     *
     * @param email the email address to invite
     * @param firstName the first name of the invited user, may be <code>null</code>
     * @param lastName the last name of the invited user, may be <code>null</code>
     * @see #inviteUser(String)
     */
    public void inviteUser(String email, String firstName, String lastName) {
        invite(() -> organizationResource.members().inviteUser(email, firstName, lastName));
    }

    /**
     * Invite an existing user of the realm to the organization, which is automatically deleted once the test is
     * completed. The user is required to have an email address.
     *
     * @param user the user to invite
     * @see #inviteUser(String)
     */
    public void inviteExistingUser(ManagedUser user) {
        inviteExistingUser(user.getId());
    }

    /**
     * Invite an existing user of the realm to the organization, which is automatically deleted once the test is
     * completed. The user is required to have an email address.
     *
     * @param userId the UUID of the user to invite
     * @see #inviteUser(String)
     */
    public void inviteExistingUser(String userId) {
        invite(() -> organizationResource.members().inviteExistingUser(userId));
    }

    /**
     * The pending invitation for the given email address.
     *
     * @param email the email address the invitation was sent to
     * @return the invitation, or <code>null</code> if the email address was not invited
     */
    public OrganizationInvitationRepresentation getInvitation(String email) {
        List<OrganizationInvitationRepresentation> invitations = organizationResource.invitations().list(null, email, null, null);
        return invitations.isEmpty() ? null : invitations.get(0);
    }

    private void invite(Supplier<Response> invitation) {
        Set<String> before = invitationIds();

        try (Response response = invitation.get()) {
            ApiUtil.expectStatus(response, "invite user to organization '%s'".formatted(getName()), Status.NO_CONTENT);
        }

        invitationIds().stream()
                .filter(id -> !before.contains(id))
                .forEach(id -> cleanup().add(o -> {
                    try (Response response = o.invitations().delete(id)) {
                        // the invitation may already have been deleted by the test itself
                        ApiUtil.expectStatus(response, "delete invitation '%s' of organization '%s'".formatted(id, getName()),
                                Status.NO_CONTENT, Status.NOT_FOUND);
                    }
                }));
    }

    private Set<String> invitationIds() {
        return organizationResource.invitations().list().stream()
                .map(OrganizationInvitationRepresentation::getId)
                .collect(Collectors.toSet());
    }

    public void updateWithCleanup(OrganizationUpdate... updates) {
        OrganizationRepresentation rep = admin().toRepresentation();
        cleanup().resetToOriginalRepresentation(rep);

        OrganizationBuilder configBuilder = OrganizationBuilder.update(rep);
        for (OrganizationUpdate update : updates) {
            configBuilder = update.update(configBuilder);
        }
        try (Response response = admin().update(configBuilder.build())) {
            ApiUtil.expectStatus(response, "update organization '%s'".formatted(getName()), Status.NO_CONTENT);
        }
    }

    @Override
    public void runCleanup() {
        if (cleanup != null) {
            cleanup.runCleanupTasks(organizationResource);
            cleanup = null;
        }
    }

    public interface OrganizationUpdate {

        OrganizationBuilder update(OrganizationBuilder organization);

    }
}
