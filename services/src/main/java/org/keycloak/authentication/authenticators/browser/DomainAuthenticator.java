/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class DomainAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(DomainAuthenticator.class);

    protected static final String ACCEPTS_PROMPT_NONE = "acceptsPromptNoneForwardFromClient";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // What if a user changes their email domain?
        // Or email is not mapped and registers with a different domain?
        // Maybe integrate with user profile email validator?

        String domain = emailDomainFor(context);
        if (domain == null) {
            context.attempted();
            return;
        }

        // Is alias okay to use as convention?
        // Already enforces uniqueness.
        // Already exists.
        // Decoupled from Authenticator.
        // But what if you want to reuse existing idp for domain? Can't rename. Export realm configs
        // and search-replace?
        IdentityProviderModel idp = context.getRealm().getIdentityProviderByAlias(domain);

        if (idp == null) {
            context.attempted();
            return;
        }

        // if idp.isMatchDomainByAlias() ?
        redirect(context, idp);
    }

    private String emailDomainFor(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String domain;

        if (user == null) {
            if (!context.getRealm().isLoginWithEmailAllowed()) {
                return null;
            }

            String attempted = attemptedUsername(context);

            // extract domain
            int at = attempted.indexOf('@');
            if (at == -1) {
                return null;
            }

            domain = attempted.substring(at); // trim?
        } else {
            String email = user.getEmail();
            if (email == null) {
                return null;
            }

            int at = email.indexOf('@');
            if (at == -1) {
                // wot?
                return null;
            }

            domain = email.substring(at);
        }

        return domain;
    }

    protected String attemptedUsername(AuthenticationFlowContext context) {
        return context.getAuthenticationSession().getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

    private void redirect(AuthenticationFlowContext context, IdentityProviderModel idp) {
        String providerId = idp.getProviderId();
        String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getOrGenerateCode();
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        String tabId = context.getAuthenticationSession().getTabId();
        URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId, context.getRealm().getName(), accessCode, clientId, tabId);
        if (context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY) != null) {
            location = UriBuilder.fromUri(location).queryParam(OAuth2Constants.DISPLAY, context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY)).build();
        }
        Response response = Response.seeOther(location)
                .build();
        // will forward the request to the IDP with prompt=none if the IDP accepts forwards with prompt=none.
        if ("none".equals(context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.PROMPT_PARAM)) &&
                Boolean.parseBoolean(idp.getConfig().get(ACCEPTS_PROMPT_NONE))) {
            context.getAuthenticationSession().setAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN, "true");
        }
        LOG.debugf("Redirecting to %s", providerId);
        // TODO: challenge or force challenge?
        context.challenge(response);
    }

}
