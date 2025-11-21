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
package org.keycloak.authentication.actiontoken.inviteorg;

import java.net.URI;
import java.util.Objects;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Action token handler for handling invitation of an existing user to an organization. A new user is handled in registration {@link org.keycloak.services.resources.LoginActionsService}.
 */
public class InviteOrgActionTokenHandler extends AbstractActionTokenHandler<InviteOrgActionToken> {

    public InviteOrgActionTokenHandler() {
        super(
          InviteOrgActionToken.TOKEN_TYPE,
          InviteOrgActionToken.class,
          Messages.STALE_INVITE_ORG_LINK,
          EventType.INVITE_ORG,
          Errors.INVALID_TOKEN
        );
    }

    @Override
    public Predicate<? super InviteOrgActionToken>[] getVerifiers(ActionTokenContext<InviteOrgActionToken> tokenContext) {
        return TokenUtils.predicates(
          TokenUtils.checkThat(
            t -> Objects.equals(t.getEmail(), tokenContext.getAuthenticationSession().getAuthenticatedUser().getEmail()),
            Errors.INVALID_EMAIL, getDefaultErrorMessage()
          )
        );
    }

    @Override
    public Response preHandleToken(InviteOrgActionToken token, ActionTokenContext<InviteOrgActionToken> tokenContext) {
        KeycloakSession session = tokenContext.getSession();
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();

        OrganizationModel organization = orgProvider.getById(token.getOrgId());

        if (organization == null) {
            return session.getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(authSession)
                    .setInfo(Messages.ORG_NOT_FOUND, token.getOrgId())
                    .createInfoPage();
        }

        session.getContext().setOrganization(organization);

        return super.preHandleToken(token, tokenContext);
    }

    @Override
    public Response handleToken(InviteOrgActionToken token, ActionTokenContext<InviteOrgActionToken> tokenContext) {
        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        KeycloakSession session = tokenContext.getSession();
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
        EventBuilder event = tokenContext.getEvent();

        event.event(EventType.INVITE_ORG).detail(Details.USERNAME, user.getUsername());

        OrganizationModel organization = orgProvider.getById(token.getOrgId());

        if (organization == null) {
            event.user(user).error(Errors.ORG_NOT_FOUND);
            return session.getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(authSession)
                    .setInfo(Messages.ORG_NOT_FOUND, token.getOrgId())
                    .createInfoPage();
        }

        if (organization.isMember(user)) {
            event.user(user).error(Errors.USER_ORG_MEMBER_ALREADY);
            return session.getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(authSession)
                    .setAttribute("messageHeader", Messages.STALE_INVITE_ORG_LINK)
                    .setInfo(Messages.ORG_MEMBER_ALREADY, user.getUsername(), organization.getName())
                    .setAttribute("pageRedirectUri", organization.getRedirectUrl())
                    .createInfoPage();
        }

        final UriInfo uriInfo = tokenContext.getUriInfo();
        final RealmModel realm = tokenContext.getRealm();

        if (tokenContext.isAuthenticationSessionFresh()) {
            // Update the authentication session in the token
            String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
            token.setCompoundAuthenticationSessionId(authSessionEncodedId);
            UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                    authSession.getClient().getClientId(), authSession.getTabId(), AuthenticationProcessor.getClientData(session, authSession));
            String confirmUri = builder.build(realm.getName()).toString();

            return session.getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(authSession)
                    .setSuccess(Messages.CONFIRM_ORGANIZATION_MEMBERSHIP, organization.getName())
                    .setAttribute("messageHeader", Messages.CONFIRM_ORGANIZATION_MEMBERSHIP_TITLE)
                    .setAttribute(Constants.TEMPLATE_ATTR_ACTION_URI, confirmUri)
                    .setAttribute(OrganizationModel.ORGANIZATION_NAME_ATTRIBUTE, organization.getName())
                    .createInfoPage();
        }

        // if we made it this far then go ahead and add the user to the organization
        orgProvider.addMember(orgProvider.getById(token.getOrgId()), user);

        String redirectUri = token.getRedirectUri();

        if (redirectUri != null) {
            authSession.setAuthNote(AuthenticationManager.SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS, "true");
            authSession.setRedirectUri(redirectUri);
            authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        }

        event.success();

        tokenContext.setEvent(event.clone().removeDetail(Details.EMAIL).event(EventType.LOGIN));

        String nextAction = AuthenticationManager.nextRequiredAction(session, authSession, tokenContext.getRequest(), event);

        if (nextAction == null) {
            // do not show account updated page
            authSession.removeAuthNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS);

            if (redirectUri != null) {
                // always redirect to the expected URI if provided
                return Response.status(Status.FOUND).location(URI.create(redirectUri)).build();
            }
        }

        return AuthenticationManager.redirectToRequiredActions(session, realm, authSession, uriInfo, nextAction);
    }
}
