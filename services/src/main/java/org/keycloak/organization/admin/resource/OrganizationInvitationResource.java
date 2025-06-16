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
package org.keycloak.organization.admin.resource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationInvitationModel;
import org.keycloak.organization.OrganizationInvitationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.keycloak.representations.idm.OrganizationInvitationStatus;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.services.validation.Validation;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
public class OrganizationInvitationResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationModel organization;
    private final AdminEventBuilder adminEvent;
    private final int tokenExpiration;

    public OrganizationInvitationResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.organization = organization;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_MEMBERSHIP);
        this.tokenExpiration = getTokenExpiration();
    }

    public Response inviteUser(String email, String firstName, String lastName) {
        if (StringUtil.isBlank(email)) {
            throw ErrorResponse.error("Email is required to invite a member", Status.BAD_REQUEST);
        }
        
        email = email.trim().toLowerCase();
        if (!Validation.isEmailValid(email)) {
            throw ErrorResponse.error("Invalid email format", Status.BAD_REQUEST);
        }
        
        OrganizationInvitationProvider invitationProvider = session.getProvider(OrganizationInvitationProvider.class);
        OrganizationInvitationModel existingInvitation = invitationProvider.getByEmailAndOrganization(email, organization);
        if (existingInvitation != null) {
            if (!existingInvitation.isExpired()) {
                throw ErrorResponse.error("User already has a pending invitation", Status.CONFLICT);
            } else {
                invitationProvider.deleteInvitation(organization, existingInvitation.getId());
            }
        }

        UserModel user = session.users().getUserByEmail(realm, email);

        if (user != null) {
            if (organization.isMember(user)) {
                throw ErrorResponse.error("User already a member of the organization", Status.CONFLICT);
            }
            return sendInvitation(user);
        }

        // Create temporary user for new registrations
        user = new InMemoryUserAdapter(session, realm, null);
        user.setEmail(email);
        if (firstName != null && lastName != null) {
            user.setFirstName(firstName);
            user.setLastName(lastName);
        }

        return sendInvitation(user);
    }

    public Response inviteExistingUser(String id) {
        if (StringUtil.isBlank(id)) {
            throw new BadRequestException("To invite a member you need to provide the user id");
        }

        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Status.BAD_REQUEST);
        }

        return sendInvitation(user);
    }

    private Response sendInvitation(UserModel user) {
        OrganizationInvitationProvider invitationProvider = session.getProvider(OrganizationInvitationProvider.class);
        
        // Create persistent invitation record
        OrganizationInvitationModel invitation = invitationProvider.createInvitation(
            organization,
            user.getEmail(),
            user.getFirstName(),
            user.getLastName()
        );
        
        String link = user.getId() == null ? 
            createRegistrationLink(user, invitation.getId()) : 
            createInvitationLink(user, invitation.getId());
        invitation.setInviteLink(link);

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .sendOrgInviteEmail(organization, link, TimeUnit.SECONDS.toMinutes(getActionTokenLifespan()));
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
            // Clean up the invitation record if email fails
            invitationProvider.deleteInvitation(organization, invitation.getId());
            throw ErrorResponse.error("Failed to send invite email", Status.INTERNAL_SERVER_ERROR);
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();

        return Response.noContent().build();
    }

    private int getTokenExpiration() {
        return Time.currentTime() + getActionTokenLifespan();
    }

    private int getActionTokenLifespan() {
        return realm.getActionTokenGeneratedByAdminLifespan();
    }

    private String createInvitationLink(UserModel user, String invitationId) {
        return LoginActionsService.actionTokenProcessor(session.getContext().getUri())
                .queryParam("key", createToken(user, invitationId))
                .build(realm.getName()).toString();
    }

    private String createRegistrationLink(UserModel user, String invitationId) {
        return OIDCLoginProtocolService.registrationsUrl(session.getContext().getUri().getBaseUriBuilder())
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OIDCResponseType.CODE)
                .queryParam(Constants.CLIENT_ID, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .queryParam(Constants.TOKEN, createToken(user, invitationId))
                .buildFromMap(Map.of("realm", realm.getName(), "protocol", OIDCLoginProtocol.LOGIN_PROTOCOL)).toString();
    }

    private String createToken(UserModel user, String invitationId) {
        InviteOrgActionToken token = new InviteOrgActionToken(user.getId(), tokenExpiration, user.getEmail(), Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);

        token.setOrgId(organization.getId());
        token.setInvitationId(invitationId);

        if (organization.getRedirectUrl() == null || organization.getRedirectUrl().isBlank()) {
            token.setRedirectUri(Urls.accountBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName()).toString());
        } else {
            token.setRedirectUri(organization.getRedirectUrl());
        }

        return token.serialize(session, realm, session.getContext().getUri());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get invitations for the organization")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success")
    })
    public Stream<OrganizationInvitationRepresentation> getInvitations(
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("status") String status,
            @QueryParam("email") String email,
            @QueryParam("search") String search,
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName) {
        
        OrganizationInvitationProvider invitationProvider = session.getProvider(OrganizationInvitationProvider.class);
        
        Stream<OrganizationInvitationModel> invitations = invitationProvider.getAllInvitations(organization);
        
        if (status != null && !status.isBlank()) {
            try {
                OrganizationInvitationModel.InvitationStatus statusFilter = OrganizationInvitationModel.InvitationStatus.valueOf(status.toUpperCase());
                invitations = invitations.filter(inv -> {
                    boolean isExpired = inv.isExpired();
                    OrganizationInvitationModel.InvitationStatus dynamicStatus = isExpired ? 
                        OrganizationInvitationModel.InvitationStatus.EXPIRED : 
                        OrganizationInvitationModel.InvitationStatus.PENDING;
                    return statusFilter.equals(dynamicStatus);
                });
            } catch (IllegalArgumentException e) {
                // Invalid status value, return empty stream
                return Stream.empty();
            }
        }
        
        if (email != null && !email.isBlank()) {
            String emailLower = email.toLowerCase().trim();
            invitations = invitations.filter(inv -> emailLower.equals(inv.getEmail().toLowerCase()));
        }
        
        if (firstName != null && !firstName.isBlank()) {
            invitations = invitations.filter(inv -> firstName.equals(inv.getFirstName()));
        }
        
        if (lastName != null && !lastName.isBlank()) {
            invitations = invitations.filter(inv -> lastName.equals(inv.getLastName()));
        }
        
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase().trim();
            invitations = invitations.filter(inv -> {
                String invEmail = inv.getEmail() != null ? inv.getEmail().toLowerCase() : "";
                String invFirstName = inv.getFirstName() != null ? inv.getFirstName().toLowerCase() : "";
                String invLastName = inv.getLastName() != null ? inv.getLastName().toLowerCase() : "";
                
                return invEmail.contains(searchLower) || 
                       invFirstName.contains(searchLower) || 
                       invLastName.contains(searchLower);
            });
        }
        
        if (first != null || max != null) {
            invitations = invitations.skip(first != null ? first : 0);
            if (max != null) {
                invitations = invitations.limit(max);
            }
        }
        
        return invitations.map(this::toRepresentation);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get invitation by ID")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public OrganizationInvitationRepresentation getInvitation(@PathParam("id") String id) {
        OrganizationInvitationProvider invitationProvider = session.getProvider(OrganizationInvitationProvider.class);
        OrganizationInvitationModel invitation = invitationProvider.getById(id, organization);
        
        if (invitation == null) {
            throw ErrorResponse.error("Invitation not found", Status.NOT_FOUND);
        }
        
        return toRepresentation(invitation);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an invitation")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Response deleteInvitation(@PathParam("id") String id) {
        OrganizationInvitationProvider invitationProvider = session.getProvider(OrganizationInvitationProvider.class);
        
        boolean deleted = invitationProvider.deleteInvitation(organization, id);
        if (!deleted) {
            throw ErrorResponse.error("Invitation not found", Status.NOT_FOUND);
        }

        adminEvent.operation(OperationType.DELETE).resourcePath("invitations", id).success();
        
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/resend")
    @Operation(summary = "Resend an invitation")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public Response resendInvitation(@PathParam("id") String id) {
        OrganizationInvitationProvider invitationProvider = session.getProvider(OrganizationInvitationProvider.class);
        
        OrganizationInvitationModel invitation = invitationProvider.getById(id, organization);
        if (invitation == null) {
            throw ErrorResponse.error("Invitation not found", Status.NOT_FOUND);
        }
        
        // Update the invitation's expiration date to extend its validity
        LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(getActionTokenLifespan());
        invitation.setExpiresAt(newExpiresAt);
        
        // Create a temporary user to send the invitation
        UserModel user = session.users().getUserByEmail(realm, invitation.getEmail());
        if (user == null) {
            user = new InMemoryUserAdapter(session, realm, null);
            user.setEmail(invitation.getEmail());
            user.setFirstName(invitation.getFirstName());
            user.setLastName(invitation.getLastName());
        }
        
        String link = user.getId() == null ?
            createRegistrationLink(user, invitation.getId()) :
            createInvitationLink(user, invitation.getId());
        invitation.setInviteLink(link);

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .sendOrgInviteEmail(organization, link, TimeUnit.SECONDS.toMinutes(getActionTokenLifespan()));
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
            throw ErrorResponse.error("Failed to resend invite email", Status.INTERNAL_SERVER_ERROR);
        }

        adminEvent.operation(OperationType.ACTION).resourcePath("invitations", id, "resend").success();
        
        return Response.noContent().build();
    }

    // Helper method to convert model to representation
    private OrganizationInvitationRepresentation toRepresentation(OrganizationInvitationModel model) {
        if (model == null) return null;
        
        OrganizationInvitationRepresentation rep = new OrganizationInvitationRepresentation();
        rep.setId(model.getId());
        rep.setEmail(model.getEmail());
        rep.setFirstName(model.getFirstName());
        rep.setLastName(model.getLastName());
        rep.setOrganizationId(model.getOrganizationId());
        rep.setSentDate(model.getCreatedAt() != null ? 
            model.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null);
        rep.setExpiresAt(model.getExpiresAt() != null ? 
            model.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null);
        rep.setInviteLink(model.getInviteLink());
        
        OrganizationInvitationStatus dynamicStatus = model.isExpired() ? 
            OrganizationInvitationStatus.EXPIRED : 
            OrganizationInvitationStatus.PENDING;
        rep.setStatus(dynamicStatus);
        
        return rep;
    }

}
