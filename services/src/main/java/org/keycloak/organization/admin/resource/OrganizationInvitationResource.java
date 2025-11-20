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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

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
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
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
            throw ErrorResponse.error("To invite a member you need to provide an email", Status.BAD_REQUEST);
        }

        UserModel user = session.users().getUserByEmail(realm, email);

        if (user != null) {
            if (organization.isMember(user)) {
                throw ErrorResponse.error("User already a member of the organization", Status.CONFLICT);
            }

            return sendInvitation(user);
        }

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
        String link = user.getId() == null ? createRegistrationLink(user) : createInvitationLink(user);

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .sendOrgInviteEmail(organization, link, TimeUnit.SECONDS.toMinutes(getActionTokenLifespan()));
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
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

    private String createInvitationLink(UserModel user) {
        return LoginActionsService.actionTokenProcessor(session.getContext().getUri())
                .queryParam("key", createToken(user))
                .build(realm.getName()).toString();
    }

    private String createRegistrationLink(UserModel user) {
        return OIDCLoginProtocolService.registrationsUrl(session.getContext().getUri().getBaseUriBuilder())
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OIDCResponseType.CODE)
                .queryParam(Constants.CLIENT_ID, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .queryParam(Constants.TOKEN, createToken(user))
                .buildFromMap(Map.of("realm", realm.getName(), "protocol", OIDCLoginProtocol.LOGIN_PROTOCOL)).toString();
    }

    private String createToken(UserModel user) {
        InviteOrgActionToken token = new InviteOrgActionToken(user.getId(), tokenExpiration, user.getEmail(), Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);

        token.setOrgId(organization.getId());

        if (organization.getRedirectUrl() == null || organization.getRedirectUrl().isBlank()) {
            token.setRedirectUri(Urls.accountBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName()).toString());
        } else {
            token.setRedirectUri(organization.getRedirectUrl());
        }

        return token.serialize(session, realm, session.getContext().getUri());
    }

}
