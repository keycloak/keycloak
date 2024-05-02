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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.utils.StringUtil;

@Provider
public class OrganizationMemberResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationProvider provider;
    private final OrganizationModel organization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public OrganizationMemberResource() {
        this.session = null;
        this.realm = null;
        this.provider = null;
        this.organization = null;
        this.auth = null;
        this.adminEvent = null;
    }

    public OrganizationMemberResource(KeycloakSession session, OrganizationModel organization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMember(String id) {
        auth.realm().requireManageRealm();
        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Status.BAD_REQUEST);
        }

        try {
            if (provider.addMember(organization, user)) {
                return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
            }
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Status.BAD_REQUEST);
        }

        throw ErrorResponse.error("User is already a member of the organization.", Status.CONFLICT);
    }

    @POST
    @Path("invite")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response inviteMember(UserRepresentation rep) {
        if (rep == null || StringUtil.isBlank(rep.getEmail())) {
            throw new BadRequestException("To invite a member you need to provide an email and/or username");
        }

        UserModel user = session.users().getUserByEmail(realm, rep.getEmail());

        InviteOrgActionToken token = null;
        String link = null;
        int tokenExpiration = Time.currentTime() + realm.getActionTokenGeneratedByAdminLifespan();
        if (user != null) {
           token = new InviteOrgActionToken(user.getId(), tokenExpiration, user.getEmail(), Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
           token.setOrgId(organization.getId());
           link = LoginActionsService.actionTokenProcessor(session.getContext().getUri())
                   .queryParam("key", token.serialize(session, realm, session.getContext().getUri()))
                   .build(realm.getName()).toString();
        } else {
            // TODO this link really only works with implicit token grants enabled for the given client
            // this path lets us invite a user that doesn't exist yet, letting them register into the organization
            token = new InviteOrgActionToken(null, tokenExpiration, rep.getEmail(), Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            token.setOrgId(organization.getId());
            Map<String, String> params = Map.of("realm", realm.getName(), "protocol", "openid-connect");
            link = OIDCLoginProtocolService.registrationsUrl(session.getContext().getUri().getBaseUriBuilder())
                    .queryParam(OAuth2Constants.RESPONSE_TYPE, OIDCResponseType.TOKEN)
                    .queryParam(Constants.CLIENT_ID, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                    .queryParam(Constants.ORG_TOKEN, token.serialize(session, realm, session.getContext().getUri()))
                    .buildFromMap(params).toString();
        }


        if (user == null ) {
            user = new InMemoryUserAdapter(session, realm, null);
            user.setEmail(rep.getEmail());
        }

        try {
            session
                    .getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .sendOrgInviteEmail(link, TimeUnit.SECONDS.toMinutes(token.getExp()));
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
            throw ErrorResponse.error("Failed to send invite email", Status.INTERNAL_SERVER_ERROR);
        }
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation( summary = "Return a paginated list of organization members filtered according to the specified parameters")
    public Stream<UserRepresentation> search(
            @Parameter(description = "A String representing either a member's username, e-mail, first name, or last name.") @QueryParam("search") String search,
            @Parameter(description = "Boolean which defines whether the param 'search' must match exactly or not") @QueryParam("exact") Boolean exact,
            @Parameter(description = "The position of the first result to be processed (pagination offset)") @QueryParam("first") @DefaultValue("0") Integer first,
            @Parameter(description = "The maximum number of results to be returned. Defaults to 10") @QueryParam("max") @DefaultValue("10") Integer max
    ) {
        auth.realm().requireManageRealm();
        return provider.getMembersStream(organization, search, exact, first, max).map(this::toRepresentation);
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation get(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        if (StringUtil.isBlank(id)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        return toRepresentation(getMember(id));
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        if (StringUtil.isBlank(id)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        UserModel member = getMember(id);

        if (provider.removeMember(organization, member)) {
            return Response.noContent().build();
        }

        throw ErrorResponse.error("Not a member of the organization", Status.BAD_REQUEST);
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") String id, UserRepresentation user) {
        auth.realm().requireManageRealm();
        return new UserResource(session, getMember(id), auth, adminEvent).updateUser(user);
    }

    @Path("{id}/organization")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OrganizationRepresentation getOrganization(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        if (StringUtil.isBlank(id)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        UserModel member = getMember(id);
        OrganizationModel organization = provider.getByMember(member);

        if (organization == null) {
            throw ErrorResponse.error("Not associated with an organization", Status.NOT_FOUND);
        }

        OrganizationRepresentation rep = new OrganizationRepresentation();

        rep.setId(organization.getId());

        return rep;
    }

    private UserModel getMember(String id) {
        UserModel member = provider.getMemberById(organization, id);

        if (member == null) {
            throw new NotFoundException();
        }

        return member;
    }

    private UserRepresentation toRepresentation(UserModel member) {
        return ModelToRepresentation.toRepresentation(session, realm, member);
    }
}
