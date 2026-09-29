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

package org.keycloak.organization.authentication.authenticators.broker;

import java.util.List;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationIdentityProviderLinkModel;
import org.keycloak.models.OrganizationInvitationModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

import static org.keycloak.organization.utils.Organizations.isEnabledAndOrganizationsPresent;

public class IdpAddOrganizationMemberAuthenticator extends AbstractIdpAuthenticator {

    private static final Logger logger = Logger.getLogger(IdpAddOrganizationMemberAuthenticator.class);

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        KeycloakSession session = context.getSession();
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        UserModel user = context.getUser();
        InviteOrgActionToken invitation = getPendingInvitation(session, user);

        if (invitation != null) {
            // The invitation decides which organization is joined, whichever broker was used to get
            // here, including one belonging to another organization. Unmanaged, as when an existing
            // user accepts an invitation.
            OrganizationModel invited = provider.getById(invitation.getOrgId());

            provider.addMember(invited, user);
            provider.getInvitationManager().remove(invitation.getId());
            context.getAuthenticationSession().setRedirectUri(invitation.getRedirectUri());

            context.getEvent().clone()
                    .event(EventType.INVITE_ORG)
                    .user(user)
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(Details.ORG_ID, invited.getId())
                    .success();

            context.success();
            return;
        }

        OrganizationModel organization = Organizations.resolveOrganization(session);
        IdentityProviderModel idpConfig = brokerContext.getIdpConfig();
        String emailDomain = Organizations.getEmailDomain(user.getEmail());

        for (String orgId : idpConfig.getOrganizationIds()) {
            OrganizationModel org = provider.getById(orgId);
            if (org == null || !org.isEnabled()) continue;
            if (organization != null && !organization.equals(org)) continue;

            OrganizationIdentityProviderLinkModel link = provider.getIdentityProviderLink(org, idpConfig);
            if (link == null || !link.isAutoMembership()) continue;

            if (!passesDomainGate(org, emailDomain)) continue;

            try {
                if (link.getMembershipType() == MembershipType.MANAGED) {
                    provider.addManagedMember(org, user);
                } else {
                    provider.addMember(org, user);
                }
            } catch (ModelException e) {
                logger.debugf("Could not add member to org %s: %s", orgId, e.getMessage());
            }
        }

        context.success();
    }

    /**
     * Returns the invitation that started this authentication, or {@code null} if there is none or
     * it does not authorize this user. The token is read from the authentication session rather than
     * looked up by email, so a link superseded by a resend cannot be used to consume its replacement.
     */
    private static InviteOrgActionToken getPendingInvitation(KeycloakSession session, UserModel user) {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();

        if (authSession == null || user == null || user.getEmail() == null) {
            return null;
        }

        InviteOrgActionToken token;

        try {
            token = Organizations.parseInvitationToken(session, authSession.getAuthNote(Organizations.INVITATION_TOKEN_NOTE));
        } catch (VerificationException e) {
            return null;
        }

        if (token == null || !user.getEmail().equalsIgnoreCase(token.getEmail())) {
            return null;
        }

        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        OrganizationModel organization = provider.getById(token.getOrgId());

        if (organization == null || !organization.isEnabled()) {
            return null;
        }

        OrganizationInvitationModel invitation = provider.getInvitationManager().getById(token.getId());

        return invitation == null || invitation.isExpired() ? null : token;
    }

    private boolean passesDomainGate(OrganizationModel org, String emailDomain) {
        List<OrganizationDomainModel> domains = org.getDomains().toList();
        if (domains.isEmpty()) return true;
        if (emailDomain == null) return false;
        return Organizations.getMatchingDomain(emailDomain, org) != null;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);

        if (!isEnabledAndOrganizationsPresent(provider)) {
            return false;
        }

        if (getPendingInvitation(session, user) != null) {
            return true;
        }

        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext)
                session.getAttribute(BrokeredIdentityContext.class.getName());
        return brokerContext != null && brokerContext.getIdpConfig().hasOrganization();
    }
}
