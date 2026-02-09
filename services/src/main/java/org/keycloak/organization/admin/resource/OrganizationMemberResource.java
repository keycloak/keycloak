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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.utils.StringUtil;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class OrganizationMemberResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationProvider provider;
    private final OrganizationModel organization;
    private final AdminEventBuilder adminEvent;

    public OrganizationMemberResource(KeycloakSession session, OrganizationModel organization, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.adminEvent = adminEvent.resource(ResourceType.ORGANIZATION_MEMBERSHIP);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Adds the user with the specified id as a member of the organization", description = "Adds, or associates, " +
            "an existing user with the organization. If no user is found, or if it is already associated with the organization, " +
            "an error response is returned")
    @RequestBody(description = "Payload should contain only id of the user to be added to the organization (UUID with or without quotes). " +
            "Surrounding whitespace characters will be trimmed.", required = true)
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response addMember(String id) {
        id = id.trim().replaceAll("^\"|\"$", ""); // fixes https://github.com/keycloak/keycloak/issues/34401
        
        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Status.BAD_REQUEST);
        }

        try {
            if (provider.addMember(organization, user)) {
                adminEvent.operation(OperationType.CREATE).resource(ResourceType.ORGANIZATION_MEMBERSHIP)
                        .representation(ModelToRepresentation.toRepresentation(organization))
                        .resourcePath(session.getContext().getUri())
                        .detail(UserModel.USERNAME, user.getUsername())
                        .detail(UserModel.EMAIL, user.getEmail())
                        .success();
                return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
            }
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Status.BAD_REQUEST);
        }

        throw ErrorResponse.error("User is already a member of the organization.", Status.CONFLICT);
    }

    @Path("invite-user")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Invites an existing user or sends a registration link to a new user, based on the provided e-mail address.",
            description = "If the user with the given e-mail address exists, it sends an invitation link, otherwise it sends a registration link.")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "409", description = "Conflict"),
        @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    public Response inviteUser(@FormParam("email") String email,
                               @FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName) {
        return new OrganizationInvitationResource(session, organization, adminEvent).inviteUser(email, firstName, lastName);
    }

    @POST
    @Path("invite-existing-user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Invites an existing user to the organization, using the specified user id")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    public Response inviteExistingUser(@FormParam("id") String id) {
        return new OrganizationInvitationResource(session, organization, adminEvent).inviteExistingUser(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation( summary = "Returns a paginated list of organization members filtered according to the specified parameters")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MemberRepresentation.class, type = SchemaType.ARRAY)))
    })
    public Stream<MemberRepresentation> search(
            @Parameter(description = "A String representing either a member's username, e-mail, first name, or last name.") @QueryParam("search") String search,
            @Parameter(description = "Boolean which defines whether the param 'search' must match exactly or not") @QueryParam("exact") Boolean exact,
            @Parameter(description = "The position of the first result to be processed (pagination offset)") @QueryParam("first") @DefaultValue("0") Integer first,
            @Parameter(description = "The maximum number of results to be returned. Defaults to 10") @QueryParam("max") @DefaultValue("10") Integer max,
            @Parameter(description = "The membership type") @QueryParam("membershipType") String membershipType
    ) {
        Map<String, String> filters = new HashMap<>();

        if (search != null) {
            filters.put(UserModel.SEARCH, search);
        }

        if (membershipType != null) {
            filters.put(MembershipType.NAME, MembershipType.valueOf(membershipType.toUpperCase()).name());
        }

        return provider.getMembersStream(organization, filters, exact, first, max).map(this::toRepresentation);
    }

    @Path("{member-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation( summary = "Returns the member of the organization with the specified id", description = "Searches for a" +
            "user with the given id. If one is found, and is currently a member of the organization, returns it. Otherwise," +
            "an error response with status NOT_FOUND is returned")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = MemberRepresentation.class))),
        @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public MemberRepresentation get(@PathParam("member-id") String memberId) {
        if (StringUtil.isBlank(memberId)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        return toRepresentation(getMember(memberId));
    }

    @Path("{member-id}")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Removes the user with the specified id from the organization", description = "Breaks the association " +
            "between the user and organization. The user itself is deleted in case the membership is managed, otherwise the user is not deleted. " +
            "If no user is found, or if they are not a member of the organization, an error response is returned")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public Response delete(@PathParam("member-id") String memberId) {
        if (StringUtil.isBlank(memberId)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        UserModel member = getMember(memberId);

        if (provider.removeMember(organization, member)) {
            adminEvent.operation(OperationType.DELETE).resource(ResourceType.ORGANIZATION_MEMBERSHIP)
                    .representation(ModelToRepresentation.toRepresentation(organization))
                    .resourcePath(session.getContext().getUri())
                    .detail(UserModel.USERNAME, member.getUsername())
                    .detail(UserModel.EMAIL, member.getEmail())
                    .success();
            return Response.noContent().build();
        }

        throw ErrorResponse.error("Not a member of the organization", Status.BAD_REQUEST);
    }

    @Path("{member-id}/organizations")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Returns the organizations associated with the user that has the specified id")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = OrganizationRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "400", description = "Bad Request")
    })
    public Stream<OrganizationRepresentation> getOrganizations(
            @PathParam("member-id") String memberId,
            @Parameter(description = "if false, return the full representation. Otherwise, only the basic fields are returned.")
            @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        if (StringUtil.isBlank(memberId)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        UserModel member = getUser(memberId);

        return provider.getByMember(member)
                .map(model -> ModelToRepresentation.toRepresentation(model, briefRepresentation));
    }

    @Path("count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation( summary = "Returns number of members in the organization.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "", content = @Content(schema = @Schema(implementation = Long.class)))
    })
    public Long count() {
        return provider.getMembersCount(organization);
    }

    private UserModel getMember(String id) {
        UserModel member = provider.getMemberById(organization, id);

        if (member == null) {
            throw new NotFoundException();
        }

        return member;
    }

    private UserModel getUser(String id) {
        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            throw new NotFoundException();
        }

        return user;
    }

    private MemberRepresentation toRepresentation(UserModel member) {
        MemberRepresentation result = new MemberRepresentation(ModelToRepresentation.toRepresentation(session, realm, member));
        result.setMembershipType(provider.isManagedMember(organization, member) ? MembershipType.MANAGED : MembershipType.UNMANAGED);
        return result;
    }
}
