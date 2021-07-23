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

import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public final class UsernameOrIdpRedirectForm extends UsernamePasswordForm {

    private static final Logger LOG = Logger.getLogger(UsernameOrIdpRedirectForm.class);

    protected static final String ACCEPTS_PROMPT_NONE = "acceptsPromptNoneForwardFromClient";

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        }
        if (!validateForm(context, formData)) {
            String domain = emailDomainFor(context);
            if (domain == null) {
                return;
            }

            // Is alias okay to use as convention?
            // Already enforces uniqueness.
            // Already exists.
            // Decoupled from Authenticator.
            // But what if you want to reuse existing idp for domain? Can't rename. Export realm configs
            // and search-replace?
            IdentityProviderModel idp = context.getRealm().getIdentityProviderByAlias(domain);

            if (idp == null || !idp.isEnabled()) {
                return;
            }

            // if idp.isMatchDomainByAlias() ?
            redirect(context, idp);
            return;
        }

        context.success();
    }

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        context.clearUser();
        UserModel user = getUser(context, formData);
        return user != null && validateUser(context, user, formData);
    }

    private UserModel getUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_USERNAME);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return null;
        }

        // remove leading and trailing whitespace
        username = username.trim();

        context.getEvent().detail(Details.USERNAME, username);
        context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

        UserModel user = null;
        try {
            // FIXME: user can block another user's username by changing their email to their username
            //   if username is an email.
            // use verified email only?
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        } catch (ModelDuplicateException mde) {
            ServicesLogger.LOGGER.modelDuplicateException(mde);

            // Could happen during federation import
            if (mde.getDuplicateFieldName() != null && mde.getDuplicateFieldName().equals(UserModel.EMAIL)) {
                setDuplicateUserChallenge(context, Errors.EMAIL_IN_USE, Messages.EMAIL_EXISTS, AuthenticationFlowError.INVALID_USER);
            } else {
                setDuplicateUserChallenge(context, Errors.USERNAME_IN_USE, Messages.USERNAME_EXISTS, AuthenticationFlowError.INVALID_USER);
            }
            return user;
        }

        testInvalidUser(context, user);
        return user;
    }

    public void testInvalidUser(AuthenticationFlowContext context, UserModel user) {
        if (user == null) {
//            dummyHash(context);
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_USERNAME);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
        }
    }

    private boolean validateUser(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData) {
        if (!enabledUser(context, user)) {
            return false;
        }
        String rememberMe = inputData.getFirst("rememberMe");
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");
        if (remember) {
            context.getAuthenticationSession().setAuthNote(Details.REMEMBER_ME, "true");
            context.getEvent().detail(Details.REMEMBER_ME, "true");
        } else {
            context.getAuthenticationSession().removeAuthNote(Details.REMEMBER_ME);
        }
        context.setUser(user);
        return true;
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        LoginFormsProvider forms = context.form();

        if (!formData.isEmpty()) forms.setFormData(formData);

        return forms.createLoginUsername();
    }

    @Override
    protected Response createLoginForm(LoginFormsProvider form) {
        return form.createLoginUsername();
    }

    @Override
    protected String getDefaultChallengeMessage(AuthenticationFlowContext context) {
        if (context.getRealm().isLoginWithEmailAllowed())
            return Messages.INVALID_USERNAME_OR_EMAIL;
        return Messages.INVALID_USERNAME;
    }

    private String emailDomainFor(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String domain;

        if (user == null) {
            if (!context.getRealm().isLoginWithEmailAllowed()) {
                return null;
            }

            String attempted = attemptedUsername(context);

            if (attempted == null) {
                return null;
            }

            // extract domain
            int at = attempted.indexOf('@');
            if (at == -1) {
                return null;
            }

            domain = attempted.substring(at + 1); // trim?
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

            domain = email.substring(at + 1);
        }

        return StringUtils.trimToNull(domain);
    }

    protected String attemptedUsername(AuthenticationFlowContext context) {
        return context.getAuthenticationSession().getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
    }

    private void redirect(AuthenticationFlowContext context, IdentityProviderModel idp) {
        String alias = idp.getAlias();
        String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getOrGenerateCode();
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        String tabId = context.getAuthenticationSession().getTabId();
        URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), alias, context.getRealm().getName(), accessCode, clientId, tabId);
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
        LOG.debugf("Redirecting to %s", alias);
        // TODO: challenge or force challenge?
        context.challenge(response);
    }
}
