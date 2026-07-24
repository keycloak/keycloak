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

import java.net.URI;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;

import org.jboss.logging.Logger;

import static org.keycloak.utils.StringUtil.isBlank;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(IdentityProviderAuthenticator.class);

    protected static final String ACCEPTS_PROMPT_NONE = "acceptsPromptNoneForwardFromClient";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (isErrorResponse(context)) {
            // error, do not redirect but move to the next step
            context.attempted();
            return;
        }

        String providerId = getIdentityProviderAlias(context);

        if (providerId == null) {
            LOG.tracef("No default provider set or %s query parameter provided", AdapterConstants.KC_IDP_HINT);
            context.attempted();
            return;
        }

        if (isBlank(providerId)) {
            LOG.tracef("Skipping: %s parameter is empty", AdapterConstants.KC_IDP_HINT);
            context.attempted();
        } else {
            redirect(context, providerId);
        }
    }

    private String getIdentityProviderAlias(AuthenticationFlowContext context) {
        String providerId = context.getUriInfo().getQueryParameters().containsKey(AdapterConstants.KC_IDP_HINT)
            ? context.getUriInfo().getQueryParameters().getFirst(AdapterConstants.KC_IDP_HINT)
            : context.getAuthenticationSession().getClientNote(AdapterConstants.KC_IDP_HINT);

        if (providerId == null) {
            AuthenticatorConfigModel config = context.getAuthenticatorConfig();

            if (config != null) {
                providerId = config.getConfig().get(IdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER);
            }
        }

        return providerId;
    }

    protected void redirect(AuthenticationFlowContext context, String providerId) {
        String loginHint = context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        redirect(context, providerId, loginHint);
    }

    protected void redirect(AuthenticationFlowContext context, String providerId, String loginHint) {
        IdentityProviderModel idp = context.getSession().identityProviders().getByAlias(providerId);
        if (idp != null && idp.isEnabled()) {
            String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getOrGenerateCode();
            String clientId = context.getAuthenticationSession().getClient().getClientId();
            String tabId = context.getAuthenticationSession().getTabId();
            String clientData = AuthenticationProcessor.getClientData(context.getSession(), context.getAuthenticationSession());
            URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId, context.getRealm().getName(), accessCode, clientId, tabId, clientData, loginHint);
            Response response = Response.seeOther(location)
                    .build();
            // will forward the request to the IDP with prompt=none if the IDP accepts forwards with prompt=none.
            if ("none".equals(context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.PROMPT_PARAM)) &&
                    Boolean.parseBoolean(idp.getConfig().get(ACCEPTS_PROMPT_NONE))) {
                context.getAuthenticationSession().setAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN, "true");
            }
            LOG.debugf("Redirecting to %s", providerId);
            context.forceChallenge(response);
            return;
        }

        LOG.warnf("Provider not found or not enabled for realm %s", providerId);
        context.attempted();
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

    private boolean isErrorResponse(AuthenticationFlowContext context) {
        return context.getForwardedErrorMessage() != null;
    }
}
